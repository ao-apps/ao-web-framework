/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aoweb-framework.
 *
 * aoweb-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aoweb-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aoweb-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.website.framework;

import com.aoindustries.collections.SortedArrayList;
import com.aoindustries.exception.WrappedException;
import com.aoindustries.html.Html;
import com.aoindustries.lang.Strings;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.net.URIParser;
import com.aoindustries.security.Identifier;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.FileRenamePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * A <code>WebSiteSettings</code> contains all the values that a user may customize while they view the web site.
 *
 * @author  AO Industries, Inc.
 */
public class WebSiteRequest extends HttpServletRequestWrapper implements FileRenamePolicy {

	private static final Logger logger = Logger.getLogger(WebSiteRequest.class.getName());

	private static final int MAX_UPLOAD_SIZE = 1073741824;

	/**
	 * Gets the upload directory.
	 */
	private static File getFileUploadDirectory(ServletContext servletContext) throws FileNotFoundException {
		File uploadDir = new File(
			(File)servletContext.getAttribute(ServletContext.TEMPDIR),
			"uploads"
		);
		if(
			!uploadDir.exists()
			&& !uploadDir.mkdirs()
			// Check exists again, another thread may have created it and interfered with mkdirs
			&& !uploadDir.exists()
		) {
			throw new FileNotFoundException(uploadDir.getPath());
		}
		return uploadDir;
	}

	/**
	 * Gets the random number generator used for this request.
	 */
	public SecureRandom getSecureRandom() {
		return ErrorReportingServlet.getSecureRandom();
	}

	/**
	 * A fast pseudo-random number generated seeded by secure random.
	 */
	public Random getFastRandom() {
		return ErrorReportingServlet.getFastRandom();
	}

	private static String getExtension(String filename) {
		int pos=filename.lastIndexOf('.');
		if(pos==-1 || pos==(filename.length()-1)) return filename;
		else return filename.substring(pos+1);
	}

	private static class MimeTypeLock {}
	private static final MimeTypeLock mimeTypeLock=new MimeTypeLock();
	private static Map<String,String> mimeTypes;
	private static String getContentType(MultipartRequest mreq, String filename) throws IOException {
		synchronized(mimeTypeLock) {
			if(mimeTypes==null) {
				Map<String,String> newMap=new HashMap<>();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(WebSiteRequest.class.getResourceAsStream("mime.types")))) {
					String line;
					while((line=in.readLine())!=null) {
						if(line.length()>0) {
							if(line.charAt(0)!='#') {
								String[] words=Strings.split(line);
								if(words.length>0) {
									String type=words[0];
									for(int c=1;c<words.length;c++) {
										newMap.put(words[1], type);
									}
								}
							}
						}
					}
				}
				mimeTypes=newMap;
			}
			String type=mimeTypes.get(getExtension(filename).toLowerCase());
			if(type!=null) return type;
			return mreq.getContentType(filename);
		}
	}

	// TODO: One ConcurrentMap per ServletContext
	private static final Map<Identifier,UploadedFile> uploadedFiles = new HashMap<>();
	private Identifier getNextID() throws IOException {
		synchronized(uploadedFiles) {
			while(true) {
				Identifier id = new Identifier(getSecureRandom());
				if(!uploadedFiles.containsKey(id)) {
					uploadedFiles.put(id, null);
					return id;
				}
			}
		}
	}

	// TODO: Start and stop with ServletContextListener.
	// TODO: Consider using ao-concurrent to avoid keeping a thread sleeping.
	private static Thread uploadedFileCleanup;
	private static void addUploadedFile(UploadedFile uf, final ServletContext servletContext) {
		synchronized(uploadedFiles) {
			uploadedFiles.put(uf.getID(), uf);

			if(uploadedFileCleanup==null) {
				uploadedFileCleanup=new Thread() {
					@Override
					@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch", "SleepWhileInLoop"})
					public void run() {
						while(true) {
							try {
								while(true) {
									sleep(10*60*1000);

									// Remove the expired entries
									synchronized(uploadedFiles) {
										Iterator<Identifier> I = uploadedFiles.keySet().iterator();
										while(I.hasNext()) {
											Identifier id = I.next();
											UploadedFile uf=uploadedFiles.get(id);
											if(uf==null) {
												I.remove();
											} else {
												long timeSince=System.currentTimeMillis()-uf.getLastAccessed();
												if(timeSince<0 || timeSince>=((long)60*60*1000)) {
													File file=uf.getStorageFile();
													if(file.exists()) {
														try {
															Files.delete(file.toPath());
														} catch(IOException e) {
															logger.log(
																Level.SEVERE,
																"file.getPath()="+file.getPath(),
																e
															);
														}
													}
													I.remove();
												}
											}
										}
										// Delete the files that do not have an uploadedFile entry and are at least two hours old
										File dir=getFileUploadDirectory(servletContext);
										String[] list=dir.list();
										if(list!=null) {
											for(String filename : list) {
												File file = new File(dir, filename);
												long fileAge=System.currentTimeMillis()-file.lastModified();
												if(fileAge<((long)-2*60*60*1000) || fileAge>((long)2*60*60*1000)) {
													boolean found=false;
													I=uploadedFiles.keySet().iterator();
													while(I.hasNext()) {
														UploadedFile uf=uploadedFiles.get(I.next());
														if(uf.getStorageFile().getAbsolutePath().equals(file.getAbsolutePath())) {
															found=true;
															break;
														}
													}
													if(!found) {
														try {
															Files.delete(file.toPath());
														} catch(IOException e) {
															logger.log(
																Level.SEVERE,
																"file.getPath()="+file.getPath(),
																e
															);
														}
													}
												}
											}
										}
									}
								}
							} catch(ThreadDeath TD) {
								throw TD;
							} catch(InterruptedException err) {
								logger.log(Level.WARNING, null, err);
							} catch(Throwable t) {
								logger.log(Level.SEVERE, null, t);
								try {
									sleep(60*1000);
								} catch(InterruptedException err) {
									logger.log(Level.WARNING, null, err);
								}
							}
						}
					}
				};
				uploadedFileCleanup.start();
			}
		}
	}

	final protected WebPage sourcePage;
	final private HttpServletRequest req;
	private MultipartRequest mreq;
	private List<UploadedFile> reqUploadedFiles;

	private boolean isLynx;
	private boolean isLynxDone;

	private boolean isBlackBerry;
	private boolean isBlackBerryDone;

	private boolean isLinux;
	private boolean isLinuxDone;

	@SuppressWarnings("OverridableMethodCallInConstructor")
	public WebSiteRequest(WebPage sourcePage, HttpServletRequest req) throws IOException, SQLException {
		super(req);
		this.sourcePage=sourcePage;
		this.req=req;
		String contentType=req.getHeader("content-type");
		if (contentType!=null && contentType.length()>=19 && contentType.substring(0,19).equalsIgnoreCase("multipart/form-data")) {
			boolean keepFiles=false;
			try {
				mreq = new MultipartRequest(req, getFileUploadDirectory(getServletContext()).getPath(), MAX_UPLOAD_SIZE, this);
				try {
					// Determine the authentication info
					WebSiteUser user=getWebSiteUser(null);
					if(user!=null) {
						keepFiles=true;
						// Create an UploadedFile for each file in the MultipartRequest
						reqUploadedFiles=new ArrayList<>();
						@SuppressWarnings("unchecked")
						Enumeration<String> E=mreq.getFileNames();
						while(E.hasMoreElements()) {
							String filename=E.nextElement();
							File file=mreq.getFile(filename);
							if(file!=null) {
								// Not necessary since there is a clean-up thread: file.deleteOnExit(); // JDK implementation builds an ever-growing set
								UploadedFile uf=new UploadedFile(
									mreq.getOriginalFileName(filename),
									file,
									user,
									getContentType(mreq, filename)
								);
								addUploadedFile(uf, sourcePage.getServletContext());
								reqUploadedFiles.add(uf);
							}
						}
					}
				} catch(LoginException err) {
					// Ignore the error, just allow the files to be cleaned up because keepFiles is still false
				}
			} finally {
				if(!keepFiles && mreq!=null) {
					@SuppressWarnings("unchecked")
					Enumeration<String> E=mreq.getFileNames();
					while(E.hasMoreElements()) {
						String filename=E.nextElement();
						File file=mreq.getFile(filename);
						if(file!=null && file.exists()) {
							Files.delete(file.toPath());
						}
					}
				}
			}
		} else this.mreq = null;
	}

	/**
	 * Appends the parameters to a URL.
	 * Parameters should already be URL encoded but not XML encoded.
	 */
	protected static boolean appendParams(StringBuilder SB, Object optParam, List<String> finishedParams, boolean alreadyAppended) {
		if (optParam != null) {
			if (optParam instanceof String) {
				List<String> nameValuePairs=Strings.split((String)optParam, '&');
				int len=nameValuePairs.size();
				for(int i=0;i<len;i++) {
					SB.append(alreadyAppended?'&':'?');
					String S=nameValuePairs.get(i);
					int pos=S.indexOf('=');
					if(pos==-1) {
						SB.append(S);
						alreadyAppended=true;
					} else {
						String name=S.substring(0, pos);
						if(!finishedParams.contains(name)) {
							SB.append(S);
							finishedParams.add(name);
							alreadyAppended=true;
						}
					}
				}
			} else if (optParam instanceof String[]) {
				String[] SA = (String[]) optParam;
				int len = SA.length;
				for (int c = 0; c < len; c += 2) {
					String name=SA[c];
					if(!finishedParams.contains(name)) {
						SB.append(alreadyAppended?'&':'?').append(name).append('=').append(SA[c + 1]);
						finishedParams.add(name);
						alreadyAppended=true;
					}
				}
			} else throw new IllegalArgumentException("Unsupported type for optParam: " + optParam.getClass().getName());
		}
		return alreadyAppended;
	}

	/**
	 * Gets a context-relative URL given its classname and optional parameters/fragment.
	 * Parameters should already be URL encoded but not XML encoded.
	 */
	public String getURLForClass(String classname, String params, String fragment) throws IOException, SQLException {
		try {
			Class<? extends WebPage> clazz=Class.forName(classname).asSubclass(WebPage.class);
			String url = getURL(clazz, params);
			if(fragment != null) url += fragment;
			return url;
		} catch(ClassNotFoundException err) {
			throw new IOException("Unable to load class: "+classname, err);
		}
	}

	/**
	 * {@linkplain #getURLForClass(java.lang.String, java.lang.String, java.lang.String) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURLForClass(String classname, String params, String fragment, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURLForClass(classname, params, fragment)
			)
		);
	}

	/**
	 * Gets a context-relative URL given its classname and optional parameters.
	 * Parameters should already be URL encoded but not XML encoded.
	 */
	public String getURLForClass(String classname, String params) throws IOException, SQLException {
		return getURLForClass(classname, params, null);
	}

	/**
	 * {@linkplain #getURLForClass(java.lang.String, java.lang.String) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURLForClass(String classname, String params, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURLForClass(classname, params)
			)
		);
	}

	/**
	 * Gets a context-relative URL from a String containing a classname and optional parameters/fragment.
	 * Parameters and fragment should already be URL encoded but not XML encoded.
	 */
	public String getURLForClass(String classAndParamsFragment) throws IOException, SQLException {
		String className, params, fragment;
		int pos = URIParser.getPathEnd(classAndParamsFragment);
		if(pos >= classAndParamsFragment.length()) {
			className = classAndParamsFragment;
			params = null;
			fragment = null;
		} else {
			className = classAndParamsFragment.substring(0, pos);
			if(classAndParamsFragment.charAt(pos) == '?') {
				int hashPos = classAndParamsFragment.indexOf('#', pos + 1);
				if(hashPos == -1) {
					params = classAndParamsFragment.substring(pos + 1);
					fragment = null;
				} else {
					params = classAndParamsFragment.substring(pos + 1, hashPos);
					fragment = classAndParamsFragment.substring(hashPos + 1);
				}
			} else {
				assert classAndParamsFragment.charAt(pos) == '#';
				params = null;
				fragment = classAndParamsFragment.substring(pos + 1);
			}
		}
		return getURLForClass(className, params, fragment);
	}

	/**
	 * {@linkplain #getURLForClass(java.lang.String) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURLForClass(String classAndParamsFragment, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURLForClass(classAndParamsFragment)
			)
		);
	}

	/**
	 * Gets the context-relative URL, optionally with the settings embedded.
	 * Parameters should already be URL encoded but not XML encoded.
	 * 
	 * @param  path  the context-relative path
	 */
	public String getURLForPath(String path, Object optParam, boolean keepSettings) throws IOException {
		StringBuilder SB=new StringBuilder();
		SB.append(path);
		List<String> finishedParams=new SortedArrayList<>();
		boolean alreadyAppended=appendParams(SB, optParam, finishedParams, false);
		if(keepSettings) appendSettings(finishedParams, alreadyAppended, SB);
		return SB.toString();
	}

	/**
	 * {@linkplain #getURLForPath(java.lang.String, java.lang.Object, boolean) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURLForPath(String path, Object optParam, boolean keepSettings, HttpServletResponse resp) throws IOException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURLForPath(path, optParam, keepSettings)
			)
		);
	}

	protected boolean appendSettings(List<String> finishedParams, boolean alreadyAppended, StringBuilder SB) {
		boolean searchEngine="true".equals(getParameter("search_engine"));
		if(searchEngine) alreadyAppended=appendParams(SB, new String[] {"search_engine", "true"}, finishedParams, alreadyAppended);
		return alreadyAppended;
	}

	@Override
	public String getParameter(String name) {
		if(mreq==null) return req.getParameter(name);
		else {
			String param=mreq.getParameter(name);
			if(param==null) param=req.getParameter(name);
			return param;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Enumeration<String> getParameterNames() {
		if (mreq==null) return req.getParameterNames();
		return (Enumeration<String>)mreq.getParameterNames();
	}

	@Override
	public String[] getParameterValues(String name) {
		if (mreq==null) return req.getParameterValues(name);
		return mreq.getParameterValues(name);
	}

	/**
	 * Gets the context-relative URL to a web page.
	 */
	public String getURL(WebPage page) throws IOException, SQLException {
		return getURL(page, (Object)null);
	}

	/**
	 * {@linkplain #getURL(com.aoindustries.website.framework.WebPage) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURL(WebPage page, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURL(page)
			)
		);
	}

	/**
	 * Gets the context-relative URL to a web page.
	 * Parameters should already be URL encoded but not yet XML encoded.
	 */
	public String getURL(WebPage page, Object optParam) throws IOException, SQLException {
		List<String> finishedParams=new SortedArrayList<>();
		StringBuilder SB = new StringBuilder();
		SB.append(page.getURLPath());
		boolean alreadyAppended=appendParams(SB, optParam, finishedParams, false);
		alreadyAppended=appendParams(SB, page.getURLParams(this), finishedParams, alreadyAppended);

		/*alreadyAppended=*/appendSettings(finishedParams, alreadyAppended, SB);

		return SB.toString();
	}

	/**
	 * {@linkplain #getURL(com.aoindustries.website.framework.WebPage, java.lang.Object) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURL(WebPage page, Object optParam, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURL(page, optParam)
			)
		);
	}

	/**
	 * Gets the context-relative URL to a web page.
	 * Parameters should already be URL encoded but not yet XML encoded.
	 */
	public String getURL(Class<? extends WebPage> clazz, Object param) throws IOException, SQLException {
		return getURL(
			WebPage.getWebPage(sourcePage.getServletContext(), clazz, param),
			param
		);
	}

	/**
	 * {@linkplain #getURL(java.lang.Class, java.lang.Object) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURL(Class<? extends WebPage> clazz, Object param, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURL(clazz, param)
			)
		);
	}

	public String getURL(Class<? extends WebPage> clazz) throws IOException, SQLException {
		return getURL(clazz, (Object)null);
	}

	/**
	 * {@linkplain #getURL(java.lang.Class) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURL(Class<? extends WebPage> clazz, HttpServletResponse resp) throws IOException, SQLException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURL(clazz)
			)
		);
	}

	/**
	 * Gets the URL String with the given parameters embedded, keeping the current settings.
	 *
	 * @param  path            the context-relative URL, with a beginning slash
	 * @param  optParam       any number of additional parameters.  This parameter can accept several types of
	 *                        objects.  The following is a list of supported objects and a brief description of its
	 *                        behavior.
	 *                        Parameters should already be URL encoded but not yet XML encoded.
	 *                        <ul>
	 *                          <li>
	 *                            <code>String</code> - appended to the end of the parameters, assumed to be in the
	 *                            format name=value
	 *                          </li>
	 *                          <li>
	 *                            <code>String[]</code> - name and value pairs, the first element of each pair is the
	 *                            name, the second is the value
	 *                          </li>
	 *                        </ul>
	 * @exception  IllegalArgumentException  if <code>optParam</code> is not a supported object
	 */
	public String getURLForPath(String path, Object optParam) throws IOException {
		return getURLForPath(path, optParam, true);
	}

	/**
	 * {@linkplain #getURLForPath(java.lang.String, java.lang.Object) Gets the URL}, including:
	 * <ol>
	 * <li>Prefixing {@linkplain HttpServletRequest#getContextPath() context path}</li>
	 * <li>Encoded to ASCII-only <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> format</li>
	 * <li>Then {@linkplain HttpServletResponse#encodeURL(java.lang.String) response encoding}</li>
	 * </ol>
	 */
	public String getEncodedURLForPath(String path, Object optParam, HttpServletResponse resp) throws IOException {
		return resp.encodeURL(
			URIEncoder.encodeURI(
				getContextPath() + getURLForPath(path, optParam)
			)
		);
	}

	/**
	 * Determines if the request is for a Lynx browser
	 */
	public boolean isLynx() {
		if(!isLynxDone) {
			String agent = req.getHeader("user-agent");
			isLynx=agent != null && agent.toLowerCase(Locale.ROOT).contains("lynx");
			isLynxDone=true;
		}
		return isLynx;
	}

	/**
	 * Determines if the request is for a BlackBerry browser
	 */
	public boolean isBlackBerry() {
		if(!isBlackBerryDone) {
			String agent = req.getHeader("user-agent");
			isBlackBerry=agent != null && agent.startsWith("BlackBerry");
			isBlackBerryDone=true;
		}
		return isBlackBerry;
	}

	/**
	 * Determines if the request is for a Linux browser
	 */
	public boolean isLinux() {
		if(!isLinuxDone) {
			String agent = req.getHeader("user-agent");
			isLinux=agent == null || agent.toLowerCase(Locale.ROOT).contains("linux");
			isLinuxDone=true;
		}
		return isLinux;
	}

	@Override
	public boolean isSecure() {
		return req.isSecure() || req.getServerPort()==443 || req.getRequestURI().contains("/https/");
	}

	/**
	 * Prints the hidden variables that contain all of the current settings.
	 */
	public void printFormFields(Html html) throws IOException {
		if("true".equals(req.getParameter("search_engine"))) printHiddenField(html, "search_engine", "true");
	}

	/**
	 * Prints the hidden variables that contain all of the current settings.
	 */
	protected static void printHiddenField(Html html, String name, String value) throws IOException {
		html.input.hidden().name(name).value(value).__().nl();
	}

	public List<UploadedFile> getUploadedFiles() {
		if(reqUploadedFiles==null) Collections.emptyList();
		return Collections.unmodifiableList(reqUploadedFiles);
	}

	/**
	 * Gets a file that was uploaded given its ID.  The authentication
	 * credentials for this request must match those of the provided ID.
	 *
	 * @param  owner  the owner of the object
	 *
	 * @return  the {@link UploadedFile} or <code>null</code> if not found
	 *
	 * @exception  SecurityException  if the ID is not assigned to the person logged in
	 */
	public static UploadedFile getUploadedFile(WebSiteUser owner, Identifier id, ServletContext context) throws SecurityException {
		synchronized(uploadedFiles) {
			UploadedFile uf=uploadedFiles.get(id);
			if(uf!=null) {
				if(uf.getOwner().equals(owner)) return uf;
				else {
					logger.log(
						Level.SEVERE,
						"UploadedFile found, but owner doesn''t match: uf.getOwner()=\"{0}\", owner=\"{1}\".",
						new Object[] {
							uf.getOwner(),
							owner
						}
					);
				}
			}
			return null;
		}
	}

	@Override
	public File rename(File file) {
		try {
			while(true) {
				File newFile=new File(getFileUploadDirectory(getServletContext()), String.valueOf(getNextID()));
				if(!newFile.exists()) return newFile;
			}
		} catch(IOException err) {
			throw new WrappedException(err, new Object[] {"file.getPath()="+file.getPath()});
		}
	}

	/**
	 * Gets the person who is logged in or <code>null</code> if no login is performed for this request.
	 *
	 * @param  resp  The current response or {@code null} when invoked from {@link WebPage#reportingGetLastModified(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
	 *
	 * @exception LoginException if an invalid login attempt is made or the user credentials are not found
	 */
	public WebSiteUser getWebSiteUser(HttpServletResponse resp) throws IOException, SQLException, LoginException {
		return null;
	}

	/**
	 * Determines if the user is currently logged in.
	 */
	public boolean isLoggedIn() throws IOException, SQLException {
		try {
			return getWebSiteUser(null)!=null;
		} catch(LoginException err) {
			return false;
		}
	}

	/**
	 * Logs out the current user or does nothing if not logged in.
	 */
	public void logout(HttpServletResponse resp) {
	}
}
