/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2014, 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.encoding.ChainWriter;
import com.aoindustries.io.FileUtils;
import com.aoindustries.security.LoginException;
import com.aoindustries.util.SortedArrayList;
import com.aoindustries.util.StringUtility;
import com.aoindustries.util.WrappedException;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.FileRenamePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

	/**
	 * Gets the random number generator used for this request.
	 */
	public Random getRandom() {
		return ErrorReportingServlet.getRandom();
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
								String[] words=StringUtility.splitString(line);
								if(words.length>0) {
									String type=words[0];
									for(int c=1;c<words.length;c++) newMap.put(words[1], type);
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

	private static final Map<Long,UploadedFile> uploadedFiles=new HashMap<>();
	private long getNextID() throws IOException {
		Random random=getRandom();
		synchronized(uploadedFiles) {
			while(true) {
				long id=random.nextLong()&0x7fffffffffffffffL;
				Long ID=id;
				if(!uploadedFiles.containsKey(ID)) {
					uploadedFiles.put(ID, null);
					return id;
				}
			}
		}
	}

	private static Thread uploadedFileCleanup;
	private static void addUploadedFile(UploadedFile uf, final ServletContext servletContext, final LoggerAccessor loggerAccessor) {
		synchronized(uploadedFiles) {
			uploadedFiles.put(uf.getID(), uf);

			if(uploadedFileCleanup==null) {
				uploadedFileCleanup=new Thread() {
					@Override
					public void run() {
						while(true) {
							try {
								while(true) {
									sleep(10*60*1000);

									// Remove the expired entries
									synchronized(uploadedFiles) {
										Iterator<Long> I=uploadedFiles.keySet().iterator();
										while(I.hasNext()) {
											Long ID=I.next();
											UploadedFile uf=uploadedFiles.get(ID);
											if(uf==null) {
												I.remove();
											} else {
												long timeSince=System.currentTimeMillis()-uf.getLastAccessed();
												if(timeSince<0 || timeSince>=((long)60*60*1000)) {
													File file=uf.getStorageFile();
													if(file.exists()) {
														try {
															FileUtils.delete(file);
														} catch(IOException e) {
															loggerAccessor.getLogger(
																servletContext,
																getClass().getName()
															).log(
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
										File dir=WebSiteFrameworkConfiguration.getFileUploadDirectory();
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
															FileUtils.delete(file);
														} catch(IOException e) {
															loggerAccessor.getLogger(
																servletContext,
																getClass().getName()
															).log(
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
							} catch(RuntimeException | InterruptedException | IOException T) {
								loggerAccessor.getLogger(servletContext, getClass().getName()).log(Level.SEVERE, null, T);
								try {
									sleep(60*1000);
								} catch(InterruptedException err) {
									loggerAccessor.getLogger(servletContext, getClass().getName()).log(Level.WARNING, null, err);
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

	public WebSiteRequest(WebPage sourcePage, HttpServletRequest req) throws IOException, SQLException {
		super(req);
		this.sourcePage=sourcePage;
		this.req=req;
		String contentType=req.getHeader("Content-Type");
		if (contentType!=null && contentType.length()>=19 && contentType.substring(0,19).equals("multipart/form-data")) {
			boolean keepFiles=false;
			try {
				mreq = new MultipartRequest(req, WebSiteFrameworkConfiguration.getFileUploadDirectory().getPath(), WebSiteFrameworkConfiguration.getMaxFileUploadSize(), this);
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
								file.deleteOnExit();
								UploadedFile uf=new UploadedFile(
									mreq.getOriginalFileName(filename),
									file,
									user,
									getContentType(mreq, filename)
								);
								addUploadedFile(uf, sourcePage.getServletContext(), sourcePage.getLoggerAccessor());
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
							FileUtils.delete(file);
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
				List<String> nameValuePairs=StringUtility.splitString((String)optParam, '&');
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
	 * Gets a relative URL from a String containing a classname and optional parameters.
	 * Parameters should already be URL encoded but not XML encoded.
	 */
	public String getURL(String classAndParams) throws IOException, SQLException {
		String className, params;
		int pos=classAndParams.indexOf('?');
		if(pos==-1) {
			className=classAndParams;
			params=null;
		} else {
			className=classAndParams.substring(0, pos);
			params=classAndParams.substring(pos+1);
		}
		return getURL(className, params);
	}

	/**
	 * Gets a relative URL given its classname and optional parameters.
	 * Parameters should already be URL encoded but not XML encoded.
	 */
	public String getURL(String classname, String params) throws IOException, SQLException {
		try {
			Class<? extends WebPage> clazz=Class.forName(classname).asSubclass(WebPage.class);
			return getURL(clazz, params);
		} catch(ClassNotFoundException err) {
			throw new IOException("Unable to load class: "+classname, err);
		}
	}

	/**
	 * Gets the context-relative URL, optionally with the settings embedded.
	 * Parameters should already be URL encoded but not XML encoded.
	 * 
	 * @param  url  the context-relative URL
	 */
	public String getURL(String url, Object optParam, boolean keepSettings) throws IOException {
		StringBuilder SB=new StringBuilder();
		SB.append(url);
		List<String> finishedParams=new SortedArrayList<>();
		boolean alreadyAppended=appendParams(SB, optParam, finishedParams, false);
		if(keepSettings) appendSettings(finishedParams, alreadyAppended, SB);
		return SB.toString();
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
		if (mreq==null) return (Enumeration<String>)req.getParameterNames();
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
	 * Gets the context-relative URL to a web page.
	 * Parameters should already be URL encoded but not yet XML encoded.
	 */
	public String getURL(Class<? extends WebPage> clazz, Object param) throws IOException, SQLException {
		WebPage page=WebPage.getWebPage(sourcePage.getServletContext(), clazz, param);
		return getURL(page, param);
	}

	public String getURL(Class<? extends WebPage> clazz) throws IOException, SQLException {
		return getURL(clazz, (Object)null);
	}

	/**
	 * Gets the URL String with the given parameters embedded, keeping the current settings.
	 *
	 * @param  url            the context-relative URL, with a beginning slash
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
	public String getURL(String url, Object optParam) throws IOException {
		return getURL(url, optParam, true);
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
	public void printFormFields(ChainWriter out, int indent) throws IOException {
		if("true".equals(req.getParameter("search_engine"))) printHiddenField(out, indent, "search_engine", "true");
	}

	/**
	 * Prints the hidden variables that contain all of the current settings.
	 */
	protected static void printHiddenField(ChainWriter out, int indent, String name, String value) throws IOException {
		for(int c=0;c<indent;c++) out.print("  ");
		out.print("<input type='hidden' name='").encodeXmlAttribute(name).print("' value='").encodeXmlAttribute(value).print("' />\n");
	}

	public List<UploadedFile> getUploadedFiles() {
		if(reqUploadedFiles==null) Collections.emptyList();
		return Collections.unmodifiableList(reqUploadedFiles);
	}

	/**
	 * Gets a file that was uploaded given its ID.  The authentication
	 * credentials for this request must match those of the provided ID.
	 *
	 * @return  the owner of the object
	 * @return  the <code>UploadedFile</code> or <code>null</code> if not found
	 *
	 * @exception  SecurityException  if the ID is not assigned to the person logged in
	 */
	public static UploadedFile getUploadedFile(WebSiteUser owner, long id, ServletContext context, LoggerAccessor loggerAccessor) throws SecurityException {
		synchronized(uploadedFiles) {
			UploadedFile uf=uploadedFiles.get(Long.valueOf(id));
			if(uf!=null) {
				if(uf.getOwner().equals(owner)) return uf;
				else {
					loggerAccessor.getLogger(
						context,
						WebSiteRequest.class.getName()
					).log(
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
				File newFile=new File(WebSiteFrameworkConfiguration.getFileUploadDirectory(), String.valueOf(getNextID()));
				if(!newFile.exists()) return newFile;
			}
		} catch(IOException err) {
			throw new WrappedException(err, new Object[] {"file.getPath()="+file.getPath()});
		}
	}

	/**
	 * Gets the person who is logged in or <code>null</code> if no login is performed for this request.
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
