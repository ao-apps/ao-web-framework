/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
import com.aoindustries.encoding.Doctype;
import com.aoindustries.encoding.EncodingContext;
import com.aoindustries.encoding.Serialization;
import com.aoindustries.encoding.servlet.DoctypeEE;
import com.aoindustries.encoding.servlet.SerializationEE;
import com.aoindustries.html.Html;
import com.aoindustries.io.AoByteArrayOutputStream;
import com.aoindustries.net.EmptyURIParameters;
import com.aoindustries.security.LoginException;
import com.aoindustries.servlet.ServletUtil;
import com.aoindustries.servlet.http.HttpServletUtil;
import com.aoindustries.util.SortedArrayList;
import com.aoindustries.util.StringUtility;
import com.aoindustries.util.WrappedException;
import gnu.regexp.RE;
import gnu.regexp.REException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The main web page provides the overall layout of the site.  The rest of
 * the site overrides methods of this class, but cannot override the
 * <code>reportingDoGet</code>, <code>reportingDoPost</code>, or
 * <code>reportingGetLastModified</code> methods.
 *
 * @author  AO Industries, Inc.
 */
abstract public class WebPage extends ErrorReportingServlet {

	private static final Logger logger = Logger.getLogger(WebPage.class.getName());

	private static final long serialVersionUID = 1L;

	/**
	 * An empty array of <code>WebPage</code> objects to be used in returning no web pages.
	 */
	protected static final WebPage[] emptyWebPageArray=new WebPage[0];

	/**
	 * Caches instances of <code>WebPage</code> for reuse.  The storage is a
	 * <code>HashMap</code> of <code>ArrayList</code>s, keyed on classname.
	 *
	 * @see  #getWebPage(ServletContext,Class,WebSiteRequest)
	 * @see  #getWebPage(ServletContext,Class,Object)
	 */
	private static final Map<String,List<WebPage>> webPageCache=new HashMap<>();

	/**
	 * Stores a cache of the list of child pages, once created.
	 *
	 * @see  #getCachedPages
	 */
	private WebPage[] pages;

	/**
	 * The last modified time of the content in the search index or <code>-1</code> if not indexed.
	 */
	private long searchLastModified=-1;

	/**
	 * The number of bytes in the document at last index time, used to properly weight the search results.
	 */
	private int searchByteCount;

	/**
	 * The words that are indexed, sorted.
	 */
	private final List<String> searchWords=new SortedArrayList<>();

	/**
	 * The number times each word appears in the document.
	 */
	private final List<int[]> searchCounts=new ArrayList<>();

	public static final RE reHTMLPattern;
	//private static RE reWordPattern;
	static {
		try {
			reHTMLPattern = new RE("<.*?>");
			//reWordPattern = new RE("(\\w*)");
		} catch (REException e) {
			throw new WrappedException(e);
		}
	}

	private static final Class<?>[] getWebPageRequestParams={
		WebSiteRequest.class
	};

	private static final Class<?>[] getWebPageObjectParams={
		Object.class
	};

	/**
	 * The output may be cached for greater throughput.
	 * @see  #doGet(WebSiteRequest,HttpServletResponse)
	 */
	private Map<Object,OutputCacheEntry> outputCache;

	public WebPage() {
		super();
	}

	public WebPage(WebSiteRequest req) {
		super();
	}

	public WebPage(Object param) {
		super();
	}

	private void addSearchWords(String words, int weight) {
		// Remove HTML
		words=reHTMLPattern.substituteAll(words, " ");

		// Iterate through all the words in the content
		StringTokenizer st=new StringTokenizer(words, " ");
		while(st.hasMoreTokens()) {
			String word=st.nextToken().toLowerCase(); //reWordPattern.getMatch(st.nextToken()).toString(1);

			// Find the index of the word
			int index=searchWords.indexOf(word);
			if(index==-1) {
				// Add to the word list
				searchWords.add(word);
				index=searchWords.indexOf(word);
				searchCounts.add(index, new int[] {weight});
			} else {
				// Increment the existing count
				searchCounts.get(index)[0]+=weight;
			}
		}
	}

	/**
	 * Determines if the provided user can access the page.  Defaults
	 * to inheriting the behavior of the parent page.
	 */
	public boolean canAccess(WebSiteUser user) throws IOException, SQLException {
		return getParent().canAccess(user);
	}

	/**
	 * Prints the form that is used to login.
	 */
	public void printLoginForm(WebPage page, LoginException loginException, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		getParent().printLoginForm(page, loginException, req, resp);
	}

	/**
	 * Prints the unauthorized page message.
	 */
	public void printUnauthorizedPage(WebPage page, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		getParent().printUnauthorizedPage(page, req, resp);
	}

	// <editor-fold defaultstate="collapsed" desc="getLastModified() Requests">
	
	/**
	 * The main entry point for <code>getLastModified()</code> requests.
	 * Prepares the request and performs initial actions:
	 * <ol>
	 *   <li>Wraps the request in {@link WebSiteRequest}
	 *       via {@link #getWebSiteRequest(javax.servlet.http.HttpServletRequest)}.</li>
	 *   <li>Resolves the current instance of {@link WebPage}
	 *       via {@link #getWebPage(java.lang.Class, com.aoindustries.website.framework.WebSiteRequest)}.</li>
	 *   <li>Handles any login request (parameter "login_requested"="true")
	 *       by returning {@code -1} for unknown.</li>
	 *   <li>Resolves the current {@link WebSiteUser}
	 *       via {@link WebSiteRequest#getWebSiteUser(javax.servlet.http.HttpServletResponse)} (if any).
	 *       When {@linkplain LoginException login required and failed},
	 *       returns {@code -1} for unknown.</li>
	 *   <li>Ensures the {@linkplain WebPage#canAccess(com.aoindustries.website.framework.WebSiteUser) user can access the page},
	 *       returns {@code -1} for unknown
	 *       when not authorized.</li>
	 *   <li>If {@linkplain #getRedirectURL(com.aoindustries.website.framework.WebSiteRequest) is a redirect},
	 *       returns {@code -1} for unknown.</li>
	 *   <li>Finally, dispatches the request to {@link #getLastModified(com.aoindustries.website.framework.WebSiteRequest)}.</li>
	 * </ol>
	 *
	 * @see #getLastModified(com.aoindustries.website.framework.WebSiteRequest)
	 */
	@Override
	final protected long reportingGetLastModified(HttpServletRequest httpReq) throws IOException, SQLException {
		WebSiteRequest req = getWebSiteRequest(httpReq);
		WebPage page = getWebPage(getClass(), req);

		if("true".equals(req.getParameter("login_requested"))) {
			return -1;
		}
		WebSiteUser user;
		try {
			user = req.getWebSiteUser(null);
		} catch(LoginException err) {
			return -1;
		}
		if(!page.canAccess(user)) return -1;

		// If redirected
		if(page.getRedirectURL(req) != null) return -1;

		return page.getLastModified(req);
	}

	/**
	 * The <code>getLastModified</code> defaults to {@code -1}.
	 */
	public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
		return -1;
	}

	/**
	 * Gets the last modified time of the java class file.  If the class file is
	 * unavailable, it defaults to the time the servlets were loaded.
	 *
	 * @see  ErrorReportingServlet#getUptime()
	 */
	final protected long getClassLastModified() throws IOException, SQLException {
		String dir=getServletContext().getRealPath("/WEB-INF/classes");
		if(dir!=null && dir.length()>0) {
			// Try to get from the class file
			long lastMod=new File(dir, getClass().getName().replace('.', File.separatorChar) + ".class").lastModified();
			if(lastMod!=0 && lastMod!=-1) return lastMod;
		}
		return getUptime();
	}

	/**
	 * Gets the most recent last modified time of this page and its immediate children.
	 */
	public long getWebPageAndChildrenLastModified(WebSiteRequest req) throws IOException, SQLException {
		WebPage[] myPages = getCachedPages(req);
		int len = myPages.length;
		long mostRecent = getClassLastModified();
		if(mostRecent==-1) return -1;
		for (int c = 0; c < len; c++) {
			long time = myPages[c].getLastModified(req);
			if(time==-1) return -1;
			if (time > mostRecent) mostRecent = time;
		}
		return mostRecent;
	}

	/**
	 * Recursively gets the most recent modification time.
	 */
	final public long getLastModifiedRecursive(WebSiteRequest req) throws IOException, SQLException {
		long time=getLastModified(req);
		WebPage[] myPages=getCachedPages(req);
		int len=myPages.length;
		for(int c=0; c<len; c++) {
			long time2=myPages[c].getLastModifiedRecursive(req);
			if(time2>time) time=time2;
		}
		return time;
	}

	/**
	 * Recursively gets the most recent modification time of a file or directory.
	 */
	public static long getLastModifiedRecursive(File file) {
		long time=file.lastModified();
		if(file.isDirectory()) {
			String[] list=file.list();
			if(list!=null) {
				int len=list.length;
				for(int c=0; c<len; c++) {
					long time2=getLastModifiedRecursive(new File(file, list[c]));
					if (time2 > time) time=time2;
				}
			}
		}
		return time;
	}

	/**
	 * Gets the last modified time for search indexing.  The index will be recreated if
	 * the search last modified time is changed.  If this method returns <code>-1</code>,
	 * no search index is built.  This defaults to be a call to <code>getLastModified</code>
	 * with a null <code>WebSiteRequest</code>.
	 */
	public long getSearchLastModified() throws IOException, SQLException {
		return getLastModified(null);
	}

	// </editor-fold>

	/**
	 * Gets the {@link Serialization} to use for this page.
	 *
	 * @param req  {@code null} during search
	 *
	 * @see EncodingContext#DEFAULT_SERIALIZATION
	 * @see SerializationEE#getDefault(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 */
	protected Serialization getSerialization(WebSiteRequest req) {
		return (req == null) ? EncodingContext.DEFAULT_SERIALIZATION : SerializationEE.getDefault(getServletContext(), req);
	}

	/**
	 * Gets the {@link Doctype} to use for this page.
	 *
	 * @param req  {@code null} during search
	 *
	 * @see EncodingContext#DEFAULT_DOCTYPE
	 */
	protected Doctype getDoctype(WebSiteRequest req) {
		return EncodingContext.DEFAULT_DOCTYPE;
	}

	/**
	 * Gets the default HTML writer for this page.
	 *
	 * @see #getSerialization(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #getDoctype(com.aoindustries.website.framework.WebSiteRequest)
	 */
	public Html getHtml(WebSiteRequest req, ChainWriter out) {
		return new Html(
			getSerialization(req),
			getDoctype(req),
			out.getPrintWriter()
		);
	}

	// <editor-fold defaultstate="collapsed" desc="GET Requests">

	/**
	 * The main entry point for <code>GET</code> requests.
	 * Prepares the request and performs initial actions:
	 * <ol>
	 *   <li>Wraps the request in {@link WebSiteRequest}
	 *       via {@link #getWebSiteRequest(javax.servlet.http.HttpServletRequest)}.</li>
	 *   <li>Resolves the current instance of {@link WebPage}
	 *       via {@link #getWebPage(java.lang.Class, com.aoindustries.website.framework.WebSiteRequest)}.</li>
	 *   <li>Handles any logout request (parameter "logout_request"="true")
	 *       via {@link WebSiteRequest#logout()}.</li>
	 *   <li>Handles any login request (parameter "login_requested"="true")
	 *       by invoking {@link WebPage#printLoginForm(com.aoindustries.website.framework.WebPage, com.aoindustries.security.LoginException, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       and stops here.</li>
	 *   <li>Resolves the current {@link WebSiteUser}
	 *       via {@link WebSiteRequest#getWebSiteUser(javax.servlet.http.HttpServletResponse)} (if any).
	 *       When {@linkplain LoginException login required and failed},
	 *       invokes {@link WebPage#printLoginForm(com.aoindustries.website.framework.WebPage, com.aoindustries.security.LoginException, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       and stops here.</li>
	 *   <li>Ensures the {@linkplain WebPage#canAccess(com.aoindustries.website.framework.WebSiteUser) user can access the page},
	 *       invokes {@link WebPage#printUnauthorizedPage(com.aoindustries.website.framework.WebPage, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       when not authorized and stops here.</li>
	 *   <li>If {@linkplain #getRedirectURL(com.aoindustries.website.framework.WebSiteRequest) is a redirect},
	 *       {@linkplain HttpServletUtil#sendRedirect(int, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoindustries.net.URIParameters, boolean, boolean) sends the redirect}
	 *       of the {@linkplain #getRedirectType() correct type} and stops here.</li>
	 *   <li>Finally, dispatches the request to {@link #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}.</li>
	 * </ol>
	 *
	 * @see #doGet(WebSiteRequest,HttpServletResponse)
	 */
	@Override
	final protected void reportingDoGet(HttpServletRequest httpReq, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		WebSiteRequest req = getWebSiteRequest(httpReq);
		WebPage page = getWebPage(getClass(), req);
		// Logout when requested
		boolean isLogout = "true".equals(req.getParameter("logout_requested")); // TODO: No magic value here, constant where best
		if(isLogout) req.logout(resp);

		if("true".equals(req.getParameter("login_requested"))) {
			// TODO: robots header on "login_requested"
			page.printLoginForm(page, new LoginException("Please Login"), req, resp);
			return;
		}
		WebSiteUser user;
		try {
			user = req.getWebSiteUser(resp);
		} catch(LoginException err) {
			page.printLoginForm(page, err, req, resp);
			return;
		}
		if(!page.canAccess(user)) {
			page.printUnauthorizedPage(page, req, resp);
			return;
		}
		String redirect = page.getRedirectURL(req);
		if(redirect != null) {
			HttpServletUtil.sendRedirect(
				page.getRedirectType(),
				req,
				resp,
				redirect,
				EmptyURIParameters.getInstance(),
				true,
				false
			);
			return;
		}
		page.doGet(req, resp);
	}

	/**
	 * Prepares the request then invokes {@link #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)}.
	 * To not have these steps automatically applied, override this method.
	 * By the time this method is called, security checks, authentication, and redirects have been done.
	 * <ol>
	 *   <li>Sets the {@linkplain Serialization serialization}.</li>
	 *   <li>Sets the {@linkplain Doctype DOCTYPE}.</li>
	 *   <li>Gets the {@linkplain #getHTMLChainWriter(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse) response writer}.</li>
	 *   <li>Invokes {@link #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)}.</li>
	 * </ol>
	 *
	 * @see #reportingDoGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * @see #getSerialization(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #getDoctype(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)
	 */
	public void doGet(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		Serialization serialization = getSerialization(req);
		Serialization oldSerialization = SerializationEE.replace(req, serialization);
		try {
			Doctype oldDoctype = DoctypeEE.replace(req, getDoctype(req));
			try {
				try (ChainWriter out = getHTMLChainWriter(req, resp)) {
					doGet(req, resp, out);
					out.flush();
				}
			} finally {
				DoctypeEE.set(req, oldDoctype);
			}
		} finally {
			SerializationEE.set(req, oldSerialization);
		}
	}

	/**
	 * The layout is automatically applied to the page, then {@link #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, com.aoindustries.website.framework.WebPageLayout)}
	 * is called.  To not have the layout automatically applied, override this method.
	 * By the time this method is called, security checks, authentication, redirects, doctype, and serialization have been done.
	 *
	 * @param  req   the {@link WebSiteRequest} for this request, or {@code null} when searching
	 * @param  resp  the {@link HttpServletResponse} for this request, or {@code null} when searching
	 * @param  out   the {@link ChainWriter} to send output to
	 *
	 * @see #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 * @see #getWebPageLayout(com.aoindustries.website.framework.WebSiteRequest)
	 * @see WebPageLayout#startHTML(com.aoindustries.website.framework.WebPage, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, java.lang.String)
	 * @see #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, com.aoindustries.website.framework.WebPageLayout)
	 * @see WebPageLayout#endHTML(com.aoindustries.website.framework.WebPage, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)
	 */
	// TODO: We could have a NullHtmlWriter that does not write any HTML tags or attributes, but just the text body.
	// TODO: Then there could be a search-specific request object, instead of null, which is used during searches.
	// TODO: This NullHtmlWriter could wrap something that skips HTML tags (in case of direct writes - is possible through Html abstraction)
	// TODO: Finally, this could all go to a writer that builds word indexes on-the-fly.
	// TODO: This could support deferred attributes (at least in a servlet context), to avoid processing attributes that will be discarded
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out
	) throws ServletException, IOException, SQLException {
		WebPageLayout layout = getWebPageLayout(req);
		layout.startHTML(this, req, resp, out, null);
		doGet(req, resp, out, layout);
		layout.endHTML(this, req, resp, out);
	}

	final public void doGet(ChainWriter out, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
		throw new AssertionError("TODO: Delete this method after all subclasses upgraded");
	}

	/**
	 * By default, GET provides no content.
	 *
	 * @param  req     the {@link WebSiteRequest} for this request, or {@code null} when searching
	 * @param  resp    the {@link HttpServletResponse} for this request, or {@code null} when searching
	 * @param  out     the {@link ChainWriter} to send output to
	 * @param  layout  the {@link WebPageLayout} that has been applied
	 *
	 * @see #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)
	 */
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		WebPageLayout layout
	) throws ServletException, IOException, SQLException {
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="POST Requests">

	/**
	 * The main entry point for <code>GET</code> requests.
	 * Prepares the request and performs initial actions:
	 * <ol>
	 *   <li>Wraps the request in {@link WebSiteRequest}
	 *       via {@link #getWebSiteRequest(javax.servlet.http.HttpServletRequest)}.</li>
	 *   <li>Resolves the current instance of {@link WebPage}
	 *       via {@link #getWebPage(java.lang.Class, com.aoindustries.website.framework.WebSiteRequest)}.</li>
	 *   <li>Handles any logout request (parameter "logout_request"="true")
	 *       via {@link WebSiteRequest#logout()}.</li>
	 *   <li>Handles any login request (parameter "login_requested"="true")
	 *       by invoking {@link WebPage#printLoginForm(com.aoindustries.website.framework.WebPage, com.aoindustries.security.LoginException, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       and stops here.</li>
	 *   <li>Resolves the current {@link WebSiteUser}
	 *       via {@link WebSiteRequest#getWebSiteUser(javax.servlet.http.HttpServletResponse)} (if any).
	 *       When {@linkplain LoginException login required and failed},
	 *       invokes {@link WebPage#printLoginForm(com.aoindustries.website.framework.WebPage, com.aoindustries.security.LoginException, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       and stops here.</li>
	 *   <li>Ensures the {@linkplain WebPage#canAccess(com.aoindustries.website.framework.WebSiteUser) user can access the page},
	 *       invokes {@link WebPage#printUnauthorizedPage(com.aoindustries.website.framework.WebPage, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       when not authorized and stops here.</li>
	 *   <li>If {@linkplain #getRedirectURL(com.aoindustries.website.framework.WebSiteRequest) is a redirect},
	 *       {@linkplain HttpServletUtil#sendRedirect(int, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoindustries.net.URIParameters, boolean, boolean) sends the redirect}
	 *       of the {@linkplain #getRedirectType() correct type} and stops here.</li>
	 *   <li>Avoid unexpected POST action after a (re)login: If has parameteter "login_requested"="true"
	 *       or both "login_username" and "login_password" parameters, dispatch to
	 *       {@link #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}
	 *       and stop here.</li>
	 *   <li>Finally, dispatches the request to {@link #doPostWithSearch(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}.</li>
	 * </ol>
	 *
	 * @see #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 * @see #doPostWithSearch(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	final protected void reportingDoPost(HttpServletRequest httpReq, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		WebSiteRequest req = getWebSiteRequest(httpReq);
		WebPage page = getWebPage(getClass(), req);
		// Logout when requested
		boolean isLogout = "true".equals(req.getParameter("logout_requested")); // TODO: No magic value here, constant where best
		if(isLogout) req.logout(resp);

		if("true".equals(req.getParameter("login_requested"))) {
			page.printLoginForm(page, new LoginException("Please Login"), req, resp);
			return;
		}
		WebSiteUser user;
		try {
			user = req.getWebSiteUser(resp);
		} catch(LoginException err) {
			page.printLoginForm(page, err, req, resp);
			return;
		}
		if(!page.canAccess(user)) {
			page.printUnauthorizedPage(page, req, resp);
			return;
		}
		String redirect = page.getRedirectURL(req);
		if(redirect != null) {
			HttpServletUtil.sendRedirect(
				page.getRedirectType(),
				req,
				resp,
				redirect,
				EmptyURIParameters.getInstance(),
				true,
				false
			);
			return;
		}
		if(
			isLogout
			|| (
				req.getParameter("login_username") != null // TODO: No magic values
				&& req.getParameter("login_password") != null // TODO: No magic values
			)
		) {
			page.doGet(req, resp);
		} else {
			page.doPostWithSearch(req, resp);
		}
	}

	/**
	 * Handles any search posts, sends everything else on to {@link #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)}.
	 * The search assumes the search parameters of <code>search_query</code> and <code>search_target</code>.  Both
	 * these values must be present for a search to be performed.  Search target may be either <code>"this_area"</code>
	 * or <code>"entire_site"</code>, defaulting to <code>"area"</code> for any other value.
	 *
	 * @see #reportingDoPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * @see #doPost(WebSiteRequest,HttpServletResponse)
	 */
	protected void doPostWithSearch(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		String query = req.getParameter("search_query");
		String searchTarget = req.getParameter("search_target");
		if(query != null && searchTarget != null) {
			Serialization serialization = getSerialization(req);
			Serialization oldSerialization = SerializationEE.replace(req, serialization);
			try {
				Doctype oldDoctype = DoctypeEE.replace(req, getDoctype(req));
				try {
					try (ChainWriter out = getHTMLChainWriter(req, resp)) {
						WebPageLayout layout = getWebPageLayout(req);
						layout.startHTML(this, req, resp, out, "document.forms.search_two.search_query.select(); document.forms.search_two.search_query.focus();");

						boolean entire_site = searchTarget.equals("entire_site");
						WebPage target = entire_site ? getRootPage() : this;

						// If the target contains no pages, use its parent
						if(target.getCachedPages(req).length==0) target=target.getParent();

						// Get the list of words to search for
						String[] words=StringUtility.splitString(query.replace('.', ' '));

						List<SearchResult> results=new ArrayList<>();
						if(words.length>0) {
							// Perform the search
							target.search(words, req, resp, results, new AoByteArrayOutputStream(), new SortedArrayList<>());
							Collections.sort(results);
							//StringUtility.sortObjectsAndFloatDescending(results, 1, 5);
						}

						layout.printSearchOutput(this, out, req, resp, query, entire_site, results, words);

						layout.endHTML(this, req, resp, out);
						out.flush();
					}
				} finally {
					DoctypeEE.set(req, oldDoctype);
				}
			} finally {
				SerializationEE.set(req, oldSerialization);
			}
		} else {
			doPost(req, resp);
		}
	}

	/**
	 * Prepares the request then invokes {@link #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)}.
	 * To not have these steps automatically applied, override this method.
	 * By the time this method is called, security checks, authentication, and redirects have been done.
	 * <ol>
	 *   <li>Sets the {@linkplain Serialization serialization}.</li>
	 *   <li>Sets the {@linkplain Doctype DOCTYPE}.</li>
	 *   <li>Gets the {@linkplain #getHTMLChainWriter(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse) response writer}.</li>
	 *   <li>Invokes {@link #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)}.</li>
	 * </ol>
	 *
	 * @see #doPostWithSearch(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 * @see #getSerialization(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #getDoctype(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)
	 */
	public void doPost(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		Serialization serialization = getSerialization(req);
		Serialization oldSerialization = SerializationEE.replace(req, serialization);
		try {
			Doctype oldDoctype = DoctypeEE.replace(req, getDoctype(req));
			try {
				try (ChainWriter out = getHTMLChainWriter(req, resp)) {
					doPost(req, resp, out);
					out.flush();
				}
			} finally {
				DoctypeEE.set(req, oldDoctype);
			}
		} finally {
			SerializationEE.set(req, oldSerialization);
		}
	}

	final public void doPost(ChainWriter out, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
		throw new AssertionError("TODO: Delete this method after all subclasses upgraded");
	}

	/**
	 * The layout is automatically applied to the page, then {@link #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, com.aoindustries.website.framework.WebPageLayout)}
	 * is called.  To not have the layout automatically applied, override this method.
	 * By the time this method is called, security checks, authentication, redirects, doctype, and serialization have been done.
	 *
	 * @param  req   the {@link WebSiteRequest} for this request, or {@code null} when searching
	 * @param  resp  the {@link HttpServletResponse} for this request, or {@code null} when searching
	 * @param  out   the {@link ChainWriter} to send output to
	 *
	 * @see #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 * @see #getWebPageLayout(com.aoindustries.website.framework.WebSiteRequest)
	 * @see WebPageLayout#startHTML(com.aoindustries.website.framework.WebPage, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, java.lang.String)
	 * @see #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, com.aoindustries.website.framework.WebPageLayout)
	 * @see WebPageLayout#endHTML(com.aoindustries.website.framework.WebPage, com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)
	 */
	public void doPost(
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out
	) throws ServletException, IOException, SQLException {
		WebPageLayout layout = getWebPageLayout(req);
		layout.startHTML(this, req, resp, out, null);
		doPost(req, resp, out, layout);
		layout.endHTML(this, req, resp, out);
	}

	/**
	 * By default, a POST request just calls {@link #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, com.aoindustries.website.framework.WebPageLayout)}.
	 *
	 * @param  req     the current {@link WebSiteRequest}
	 * @param  resp    the {@link HttpServletResponse} for this request
	 * @param  out     the {@link ChainWriter} to send output to
	 * @param  layout  the {@link WebPageLayout} that has been applied
	 *
	 * @see #doPost(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter)
	 * @see #doGet(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoindustries.encoding.ChainWriter, com.aoindustries.website.framework.WebPageLayout)
	 */
	public void doPost(
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		WebPageLayout layout
	) throws ServletException, IOException, SQLException {
		doGet(req, resp, out, layout);
	}

	// </editor-fold>

	/**
	 * Determines if this page equals another page.
	 *
	 * @see  #equals(WebPage)
	 */
	@Override
	final public boolean equals(Object O) {
		return
			(O instanceof WebPage)
			&& equals((WebPage)O)
		;
	}

	/**
	 * Determines if this page equals another page.  By default, two pages
	 * of the same classname are considered equal.
	 *
	 * @see  #hashCode
	 */
	public boolean equals(WebPage other) {
		return other.getClass().getName().equals(getClass().getName());
	}

	/**
	 * The default hashcode for a page is the hashcode of its
	 * classname.
	 *
	 * @see  #equals(WebPage)
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	/**
	 * Gets additional headers for this page.  The format must be in a String[] of name/value pairs, two elements each, name and then value.
	 */
	// TODO: Return a Map<String,? extend Iterable<String>> ?
	public String[] getAdditionalHeaders(WebSiteRequest req) {
		return null;
	}

	/**
	 * Gets the author of this page.  By default, the author of the parent page is used.
	 */
	public String getAuthor() throws IOException, SQLException {
		return getParent().getAuthor();
	}

	/**
	 * Gets the URL for the author of this page.  By default, the URL of the author of the parent page is used.
	 */
	public String getAuthorHref(WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
		return getParent().getAuthorHref(req, resp);
	}

	/**
	 * Gets the preferred width of this content in pixels or <code>-1</code> for no preference.
	 * It is up to the <code>WebPageLayout</code> to make use of this value.  The preferred width
	 * defaults to the preferred width of the parent page.
	 *
	 * @see  WebPageLayout
	 */
	public int getPreferredContentWidth(WebSiteRequest req) throws IOException, SQLException {
		return getParent().getPreferredContentWidth(req);
	}

	/**
	 * Gets the vertical alignment of the content area.  Defaults to <code>"top"</code>.
	 * It is up to the <code>WebPageLayout</code> to make use of this value.
	 *
	 * @see  WebPageLayout
	 */
	public String getContentVAlign(WebSiteRequest req) {
		return "top";
	}

	/**
	 * Gets the description of this page.  By default, the description of the parent page is used.
	 */
	public String getDescription() throws IOException, SQLException {
		return getParent().getDescription();
	}

	/**
	 * Gets the root page in the web page hierarchy.  The root page has no parent.
	 */
	public final WebPage getRootPage() throws IOException, SQLException {
		WebPage page = this;
		WebPage parent;
		while ((parent = page.getParent()) != null) page = parent;
		return page;
	}

	/**
	 * Prepares for output and returns the {@link ChainWriter}.
	 * <ol>
	 *   <li>{@linkplain ServletResponse#resetBuffer() clears the output buffer}.</li>
	 *   <li>Sets the {@linkplain ServletResponse#setContentType(java.lang.String) response content type}.</li>
	 *   <li>Sets the {@linkplain ServletResponse#setCharacterEncoding(java.lang.String) response character encoding}
	 *       to {@linkplain Html#ENCODING the default <code>UTF-8</code>}.</li>
	 *   <li>Sets any {@linkplain #getAdditionalHeaders(com.aoindustries.website.framework.WebSiteRequest) additional headers}.</li>
	 * </ol>
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the content type.
	 * </p>
	 *
	 * @see SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 * @see #getAdditionalHeaders(com.aoindustries.website.framework.WebSiteRequest)
	 */
	protected ChainWriter getHTMLChainWriter(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Clear the output buffer
		resp.resetBuffer();
		// Set the content type
		ServletUtil.setContentType(
			resp,
			SerializationEE.get(req.getServletContext(), req).getContentType(),
			Html.ENCODING
		);
		// Set additional headers
		String[] headers = getAdditionalHeaders(req);
		if(headers != null) {
			int len = headers.length;
			for(int c=0; c<len; c += 2) resp.setHeader(headers[c], headers[c + 1]);
		}
		return new ChainWriter(resp.getWriter());
	}

	/**
	 * Prepares for output and returns the {@link OutputStream}.
	 * <ol>
	 *   <li>{@linkplain ServletResponse#resetBuffer() clears the output buffer}.</li>
	 *   <li>Sets the {@linkplain ServletResponse#setContentType(java.lang.String) response content type}.</li>
	 *   <li>Sets the {@linkplain ServletResponse#setCharacterEncoding(java.lang.String) response character encoding}
	 *       to {@linkplain Html#ENCODING the default <code>UTF-8</code>}.</li>
	 *   <li>Sets any {@linkplain #getAdditionalHeaders(com.aoindustries.website.framework.WebSiteRequest) additional headers}.</li>
	 * </ol>
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the content type.
	 * </p>
	 *
	 * @see SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 * @see #getAdditionalHeaders(com.aoindustries.website.framework.WebSiteRequest)
	 */
	protected OutputStream getHTMLOutputStream(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Clear the output buffer
		resp.resetBuffer();
		// Set the content type
		ServletUtil.setContentType(
			resp,
			// TODO: request-only variant
			SerializationEE.get(req.getServletContext(), req).getContentType(),
			Html.ENCODING
		);
		// Set additional headers
		String[] headers = getAdditionalHeaders(req);
		if(headers != null) {
			int len = headers.length;
			for(int c=0; c<len; c += 2) resp.setHeader(headers[c], headers[c + 1]);
		}
		return resp.getOutputStream();
	}

	/**
	 * Gets the JavaScript's that should have script src= tags generated, urls relative to top of context <code>path/to/javascript.js</code>.
	 *
	 * @param  req  the current <code>WebSiteRequest</code>
	 *
	 * @return  a <code>String[]</code> for multiple includes,
	 *          a <code>String</code> for one,
	 *          or <code>null</code> for none
	 */
	public Object getJavaScriptSrc(WebSiteRequest req) throws IOException, SQLException {
		return null;
	}

	/**
	 * Gets the keywords for this page.  By default, the keywords of the parent page are used.
	 */
	// TODO: Is it correct to use keywords of parent?
	public String getKeywords() throws IOException, SQLException {
		return getParent().getKeywords();
	}

	/**
	 * Gets the text for the navigation image to use to represent this page.  Defaults to <code>getShortTitle</code>.
	 *
	 * @return  the alt text of the navigation image
	 *
	 * @see #getShortTitle()
	 * @see #getNavImageSuffix(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #getNavImageURL(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, java.lang.Object)
	 */
	public String getNavImageAlt(WebSiteRequest req) throws IOException, SQLException {
		return getShortTitle();
	}

	/**
	 * Gets the text that will be placed in to the right of the navigation image.  If the
	 * image is not large enough to hold both <code>getNavImageAlt</code> and <code>getNavImageSuffix</code>,
	 * the beginning is truncated and <code>...</code> appended so that both fit the image.
	 *
	 * @see #getNavImageAlt(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #getNavImageURL(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, java.lang.Object)
	 */
	public String getNavImageSuffix(WebSiteRequest req) throws IOException, SQLException {
		return null;
	}

	/**
	 * Gets the URL associated with a nav image, fully encoded.
	 *
	 * @see #getNavImageAlt(com.aoindustries.website.framework.WebSiteRequest)
	 * @see #getNavImageSuffix(com.aoindustries.website.framework.WebSiteRequest)
	 */
	public String getNavImageURL(WebSiteRequest req, HttpServletResponse resp, Object params) throws IOException, SQLException {
		return req.getEncodedURL(this, params, resp);
	}

	/**
	 * Gets the index of this page in the parents list of children pages.
	 */
	final public int getPageIndexInParent(WebSiteRequest req) throws IOException, SQLException {
		WebPage[] myPages=getParent().getCachedPages(req);
		int len=myPages.length;
		for(int c=0;c<len;c++) if(myPages[c].equals(this)) return c;
		throw new RuntimeException("Unable to find page index in parent.");
	}

	/**
	 * Gets the <code>WebPage</code> that follows this one in the parents
	 * list of pages.
	 *
	 * @return  the <code>WebPage</code> or <code>null</code> if not found
	 */
	final public WebPage getNextPage(WebSiteRequest req) throws IOException, SQLException {
		WebPage parent=getParent();
		if (parent!=null) {
			WebPage[] myPages=parent.getCachedPages(req);
			int len=myPages.length;
			for(int c=0; c<len; c++) {
				if(myPages[c].getClass() == getClass()) {
					if (c < (len - 1)) return myPages[c + 1];
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the <code>WebPage</code> that proceeds this one in the parents
	 * list of pages.
	 *
	 * @return  the <code>WebPage</code> or <code>null</code> if not found
	 */
	final public WebPage getPreviousPage(WebSiteRequest req) throws IOException, SQLException {
		WebPage parent = getParent();
		if (parent != null) {
			WebPage[] myPages = parent.getCachedPages(req);
			int len = myPages.length;
			for (int c = 0; c < len; c++) {
				if (myPages[c].getClass() == getClass()) {
					if (c > 0) return myPages[c - 1];
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the JavaScript that should be executed with the onload event of the body tag
	 *
	 * @param  req  the current <code>WebSiteRequest</code>
	 *
	 * @return  a <code>String</code> or <code>null</code> for none
	 */
	public String getOnloadScript(WebSiteRequest req) {
		return null;
	}

	/**
	 * Gets all of the pages that are children of this one in the page hierarchy.
	 * Unless overridden, the pages are cached in a <code>WebPage[]</code> for
	 * faster access.  The actual list of pages is obtained from <code>getWebPages</code>.
	 * <p>
	 * Pages will also not be cached if the configuration property is set to anything
	 * other than <code>"true"</code>
	 *
	 * @return a <code>WebPage[]</code> of all of the lower-level pages
	 *
	 * @see  #getWebPages(WebSiteRequest)
	 */
	synchronized public WebPage[] getCachedPages(WebSiteRequest req) throws IOException, SQLException {
		WebPage[] myPages=this.pages;
		if(myPages==null) myPages=this.pages=getWebPages(req);
		return myPages;
	}

	/**
	 * Gets the parent of this page or <code>null</code> for none.
	 */
	public abstract WebPage getParent() throws IOException, SQLException;

	/**
	 * Gets the absolute or context-relative URL to direct to.
	 * Redirection happens before specific frameset actions thus allowing one to redirect various frames to different places.
	 *
	 * @return  the context-relative or absolute URL to redirect to or <code>null</code> for
	 *          no redirect.
	 */
	public String getRedirectURL(WebSiteRequest req) throws IOException, SQLException {
		return null;
	}

	/**
	 * Gets the redirect type, defaults to 302 (temporary).
	 */
	public int getRedirectType() {
		return HttpServletResponse.SC_MOVED_TEMPORARILY;
	}

	/**
	 * A short title is used showing a path to the current location in the site.  The short title
	 * defaults to <code>getTitle</code>.
	 *
	 * @return  the short page title
	 *
	 * @see  #getTitle
	 */
	public String getShortTitle() throws IOException, SQLException{
		return getTitle();
	}

	/**
	 * Gets the title of the web page in order to generate the HTML.  The
	 * title defaults to that of the parent page.
	 *
	 * @return  the page title
	 */
	public String getTitle() throws IOException, SQLException {
		return getParent().getTitle();
	}

	/**
	 * Gets parameters that are added to the query string of URLs generated for this page.
	 */
	public Object getURLParams(WebSiteRequest req) throws IOException, SQLException {
		return null;
	}

	/**
	 * @see  #getWebPage(ServletContext,Class,WebSiteRequest)
	 */
	public WebPage getWebPage(Class<? extends WebPage> clazz, WebSiteRequest req) throws IOException {
		return getWebPage(getServletContext(), clazz, req);
	}

	/**
	 * Gets an instance of <code>WebPage</code> given the <code>Class</code>.
	 * Instances returned should never have the <code>init</code> method
	 * called and should allocate a minimal set of resources.
	 * <p>
	 * Unless caching is disabled, the generated pages are stored in a
	 * cache and resolved using the pages <code>isHandler</code> method.
	 *
	 * @param  context  the context the servlet will be run in
	 * @param  clazz  the <code>Class</code> to get an instance of
	 * @param  req  the request details are used to select the right instance
	 *
	 * @return  a <code>WebPage</code> object of the given class that matches the request settings
	 *
	 * @exception  IllegalArgumentException if unable to create the instance
	 *
	 * @see  #isHandler(WebSiteRequest)
	 */
	public static WebPage getWebPage(ServletContext context, Class<? extends WebPage> clazz, WebSiteRequest req) throws IOException {
		synchronized(webPageCache) {
			if(context==null) throw new IllegalArgumentException("context is null");
			String classname=clazz.getName();
			// First look for a match in the cache
			List<WebPage> list=webPageCache.get(classname);
			if(list!=null) {
				int size=list.size();
				for(int c=0;c<size;c++) {
					WebPage page=list.get(c);
					if(page.getClass()==clazz && page.isHandler(req)) return page;
				}
			}

			// Make a new instance and store in cache
			try {
				Constructor<? extends WebPage> con=clazz.getConstructor(getWebPageRequestParams);
				WebPage page=con.newInstance(new Object[] {req});
				page.setServletContext(context);
				if(list==null) webPageCache.put(classname, list=new ArrayList<>());
				list.add(page);
				return page;
			} catch (ClassCastException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
				throw new IOException("Unable to getWebPage: "+clazz.getName()+", classname="+classname+", req="+req, e);
			}
		}
	}

	/**
	 * @see  #getWebPage(ServletContext,Class,Object)
	 */
	public WebPage getWebPage(Class<? extends WebPage> clazz, Object param) throws IOException {
		return getWebPage(getServletContext(), clazz, param);
	}

	/**
	 * Gets an instance of <code>WebPage</code> given the <code>Class</code>.
	 * Instances returned should never have the <code>init</code> method
	 * called and should allocate a minimal set of resources.
	 * <p>
	 * Unless caching is disabled, the generated pages are stored in a
	 * cache and resolved using the pages <code>isHander</code> method.
	 *
	 * @param  context  the context the servlet will be run in
	 * @param  clazz  the <code>Class</code> to get an instance of
	 * @param  params  the parameters used to select the right instance
	 *
	 * @return  a <code>WebPage</code> object of the given class that matches the request settings
	 *
	 * @exception  IllegalArgumentException if unable to create the instance
	 *
	 * @see  #isHandler(Object)
	 */
	public static WebPage getWebPage(ServletContext context, Class<? extends WebPage> clazz, Object params) throws IOException {
		synchronized(webPageCache) {
			String classname=clazz.getName();
			// First look for a match in the cache
			List<WebPage> list=webPageCache.get(classname);
			if(list!=null) {
				int size=list.size();
				for(int c=0;c<size;c++) {
					WebPage page=list.get(c);
					if(page==null) throw new NullPointerException("page is null");
					if(page.getClass()==clazz && page.isHandler(params)) return page;
				}
			}

			// Make a new instance and store in cache
			try {
				Constructor<? extends WebPage> con=clazz.getConstructor(getWebPageObjectParams);
				WebPage page=con.newInstance(new Object[] {params});
				page.setServletContext(context);
				if(list==null) webPageCache.put(classname, list=new ArrayList<>());
				list.add(page);
				return page;
			} catch (ClassCastException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
				throw new IOException("Unable to getWebPage: "+clazz.getName()+", classname="+classname+", params="+params, e);
			}
		}
	}

	/**
	 * Dynamically loads new classes based on the source .class file's modified time.
	 */
	public static Class<? extends WebPage> loadClass(String className) throws ClassNotFoundException {
		return Class.forName(className).asSubclass(WebPage.class);
	}

	/**
	 * Gets the current layout for this page.
	 * When req is null, should return {@link SearchLayout} or equivalent.
	 * <p>
	 * This default implementation returns {@link SearchLayout#getInstance()} for
	 * a search request (req is null), or inherits the layout of the
	 * {@linkplain #getParent() parent}.
	 * </p>
	 *
	 * @param  req  the {@link WebSiteRequest} for this request, or {@code null} when searching
	 *
	 * @return  the <code>WebPageLayout</code>
	 */
	// TODO: Review uses, should be much fewer now (only from this class?)
	public WebPageLayout getWebPageLayout(WebSiteRequest req) throws IOException, SQLException {
		// Search index building
		if(req == null) return SearchLayout.getInstance();
		return getParent().getWebPageLayout(req);
	}

	/**
	 * Gets all of the children pages of this one in the page hierarchy.  Defaults to none.
	 * The results of this call are never cached.  For efficiency, please call
	 * <code>getCachedWebPages</code>.  Subclasses will override and disable the caching
	 * provided by <code>getCachedWebPages</code> when appropriate.
	 *
	 * @return a <code>WebPage[]</code> of all of the lower-level pages
	 *
	 * @see  #getCachedPages
	 * @see  #emptyWebPageArray
	 */
	protected WebPage[] getWebPages(WebSiteRequest req) throws IOException, SQLException {
		return emptyWebPageArray;
	}

	/**
	 * Determines if this page is the instance that should handle a particular request.
	 * By default returns <code>true</code>, meaning it is a handler for all requests
	 * for this <code>Class</code>.
	 *
	 * @see  #getWebPage(ServletContext,Class,WebSiteRequest)
	 */
	public boolean isHandler(WebSiteRequest req) {
		return true;
	}

	/**
	 * Determines if this page is the instance that represents a certain set of parameters.
	 * By default returns <code>true</code>, meaning it is a handler for any parameters
	 * for this <code>Class</code>.
	 *
	 * @see  #getWebPage(ServletContext,Class,Object)
	 */
	public boolean isHandler(Object O) {
		return true;
	}

	/**
	 * Gets the <code>WebSiteRequest</code> that handles authentication and other details
	 * of this site.
	 */
	abstract protected WebSiteRequest getWebSiteRequest(HttpServletRequest req) throws IOException, SQLException;

	/**
	 * Searches this WebPage and all of its subordinate pages, returning the matches
	 * in a <code>ArrayList</code> with five elements per match.
	 * <ol>
	 *   <li>A <code>String</code> for the absolute URL (including settings)</li>
	 *   <li>The probability as a <code>Float</code></li>
	 *   <li>The title of the page</li>
	 *   <li>The description of the page</li>
	 *   <li>The author of the page</li>
	 * </ol>
	 * Defaults to <code>standardSearch</code>
	 *
	 * @param  words     all of the words that must match
	 * @param  req       the <code>WebSiteRequest</code> containing the users preferences
	 * @param  results   the <code>ArrayList</code> that contains the results
	 * @param  bytes     the <code>SearchOutputStream</code> to use for internal processing
	 *
	 * @see  #standardSearch
	 */
	public void search(
		String[] words,
		WebSiteRequest req,
		HttpServletResponse response,
		List<SearchResult> results,
		AoByteArrayOutputStream bytes,
		List<WebPage> finishedPages
	) throws ServletException, IOException, SQLException {
		standardSearch(words, req, response, results, bytes, finishedPages);
	}

	/**
	 * The standard implementation of the search functionality.
	 *
	 * @see  #search
	 */
	final public void standardSearch(
		String[] words,
		WebSiteRequest req,
		HttpServletResponse response,
		List<SearchResult> results,
		AoByteArrayOutputStream bytes,
		List<WebPage> finishedPages
	) throws ServletException, IOException, SQLException {
		if(!finishedPages.contains(this)) {
			String title = null;
			String description = null;
			String author = null;
			String authorHref = null;

			// The counted matches will go here
			int totalMatches = 0;
			int size;

			// Search the byte data only if not able to index
			long mySearchLastModified = getSearchLastModified();
			if (mySearchLastModified == -1) {
				title = getTitle();
				description = getDescription();
				author = getAuthor();
				authorHref = getAuthorHref(req, response);
				String keywords = getKeywords();

				// Get the HTML content
				bytes.reset();
				try (ChainWriter out = new ChainWriter(bytes)) {
					doGet(null, null, out);
					out.flush();
				}
				byte[] content = bytes.getInternalByteArray();
				size = bytes.size();

				int len = words.length;
				for (int c = 0; c < len; c++) {
					String word = words[c];
					int wordMatch =
						// Add the keywords with weight 10
						StringUtility.countOccurrences(keywords, word) * 10

						// Add the description with weight 5
						+StringUtility.countOccurrences(description, word) * 5

						// Add the title with weight 5
						+StringUtility.countOccurrences(title, word) * 5

						// Add the content with weight 1
						+StringUtility.countOccurrences(content, size, word)

						// Add the author with weight 1
						+StringUtility.countOccurrences(author, word);

					if (wordMatch == 0) {
						totalMatches = 0;
						break;
					}
					totalMatches += wordMatch;
				}

				if (totalMatches > 0) {
					size += keywords.length() + description.length() + title.length() + author.length();
				}
			} else {
				// Rebuild the search index if no longer valid
				if (mySearchLastModified != this.searchLastModified) {
					// Only synchronize for index rebuild
					synchronized (this) {
						if (mySearchLastModified != this.searchLastModified) {
							title = getTitle();
							description = getDescription();
							author = getAuthor();
							authorHref = getAuthorHref(req, response);
							String keywords = getKeywords();

							// Get the HTML content
							bytes.reset();
							try (ChainWriter out = new ChainWriter(bytes)) {
								doGet(null, null, out);
								out.flush();
							} catch(NullPointerException err) {
								logger.log(Level.WARNING, null, err);
							}
							byte[] bcontent = bytes.getInternalByteArray();
							size = bytes.size();
							String content = new String(bcontent, 0, size);

							// Remove all the indexed words
							searchWords.clear();
							searchCounts.clear();

							// Add the keywords with weight 10
							addSearchWords(keywords, 10);

							// Add the description with weight 5
							addSearchWords(description, 5);

							// Add the title with weight 5
							addSearchWords(title, 5);

							// Add the content with weight 1
							addSearchWords(content, 1);

							// Add the author with weight 1
							addSearchWords(author, 1);

							searchByteCount = size + keywords.length() + description.length() + title.length() + author.length();
							//searchWords.trimToSize();
							//searchCounts.trimToSize();
							this.searchLastModified = mySearchLastModified;
						}
					}
				}

				// Count the words from the index
				int searchWordsSize = searchWords.size();

				int len = words.length;
				for (int c = 0; c < len; c++) {
					String word = words[c];

					// Count through each word
					int wordMatch = 0;
					for (int d = 0; d < searchWordsSize; d++) {
						String searchWord = searchWords.get(d);
						int count = StringUtility.countOccurrences(searchWord, word);
						if (count > 0) wordMatch += count * searchCounts.get(d)[0];
					}

					if (wordMatch == 0) {
						totalMatches = 0;
						break;
					}
					totalMatches += wordMatch;
				}

				// Use the cached size
				size = searchByteCount;
			}

			if (totalMatches > 0) {
				float probability=
					totalMatches
					/ (
						size <= 0
						? 1.0f :
						((float)Math.log(size))
					)
				;
				results.add(
					new SearchResult(
						req.getURL(this),
						probability,
						title == null ? getTitle() : title,
						description == null ? getDescription() : description,
						author == null ? getAuthor() : author,
						authorHref == null ? getAuthorHref(req, response) : authorHref
					)
				);
			}

			// Flag as done
			finishedPages.add(this);

			// Search recursively
			WebPage[] myPages = getCachedPages(req);
			int len = myPages.length;
			for (int c = 0; c < len; c++) myPages[c].search(words, req, response, results, bytes, finishedPages);
		}
	}

	/**
	 * Determine if the nav image for this page should remain visible, even when
	 * its children are displayed.  The default is <code>false</code>.
	 */
	public boolean includeNavImageAsParent() {
		return false;
	}

	/**
	 * Determines whether or not to display the page in the left navigation.
	 */
	public boolean useNavImage() throws IOException, SQLException {
		return true;
	}

	/**
	 * Determines if this page will be displayed in the standard site map.
	 */
	public boolean useSiteMap() {
		return true;
	}

	/**
	 * Determines if this page will be displayed in the location bar.
	 */
	public boolean showInLocationPath(WebSiteRequest req) {
		return true;
	}

	private ServletContext context;

	@Override
	public ServletContext getServletContext() {
		if(context!=null) return context;
		ServletContext sc=super.getServletContext();
		if(sc==null) throw new NullPointerException("ServletContext is null");
		return sc;
	}

	void setServletContext(ServletContext context) {
		this.context=context;
	}

	/**
	 * Gets the copyright information for this page.  Defaults to the copyright of the parent page.
	 *
	 * // TODO: Use dcterms:
	 *          http://stackoverflow.com/questions/6665312/is-the-copyright-meta-tag-valid-in-html5
	 *          https://wiki.whatwg.org/wiki/MetaExtensions
	 *          http://dublincore.org/documents/dcmi-terms/
	 */
	public String getCopyright(WebSiteRequest req, HttpServletResponse resp, WebPage requestPage) throws IOException, SQLException {
		return getParent().getCopyright(req, resp, requestPage);
	}

	/**
	 * Gets the context-relative path for the URL
	 */
	public String getURLPath() throws IOException, SQLException {
		return '/'+generateURLPath(this);
	}

	/**
	 * Generates a URL path for this or another page, please call getURLPath() instead.
	 * The default behavior is to ask the parent to generate the URL.  Therefore the
	 * top-level <code>WebPage</code> of a site must implement this method.
	 */
	public String generateURLPath(WebPage page) throws IOException, SQLException {
		return getParent().generateURLPath(page);
	}

	/**
	 * Gets the URL pattern for this page as used in <code>web.xml</code>.
	 */
	public String getURLPattern() throws IOException, SQLException {
		return getURLPath();
	}
}
