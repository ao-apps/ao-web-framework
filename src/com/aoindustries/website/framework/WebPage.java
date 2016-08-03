/*
 * Copyright 2000-2013, 2015, 2016 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.encoding.ChainWriter;
import com.aoindustries.io.AoByteArrayOutputStream;
import com.aoindustries.security.LoginException;
import com.aoindustries.servlet.http.ServletUtil;
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
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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

	public WebPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public WebPage(WebSiteRequest req) {
		super(req.sourcePage.getLoggerAccessor());
	}

	public WebPage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor);
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

	/**
	 * First, resolves correct instance of <code>WebPage</code>.  Then, if security mode
	 * mismatches, authentication failed, or the page is a redirect, returns <code>-1</code>
	 * for unknown.  Otherwise, call <code>getLastModified(WebSiteRequest)</code>
	 *
	 * @see  #getLastModified(WebSiteRequest)
	 */
	@Override
	final public long reportingGetLastModified(HttpServletRequest httpReq) throws IOException, SQLException {
		WebSiteRequest req=getWebSiteRequest(httpReq);
		Class<? extends WebPage> thisClass = getClass();
		WebPage page=getWebPage(thisClass, req);

		// Check authentication first
		try {
			WebSiteUser user=req.getWebSiteUser(null);
			if(!page.canAccess(user)) return -1;
		} catch(LoginException err) {
			return -1;
		}

		// If redirected
		if(page.getRedirectURL(req)!=null) return -1;

		return page.getLastModified(req);
	}

	/**
	 * The <code>getLastModified</code> defaults to <code>-1</code>.
	 */
	public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
		return -1;
	}

	/**
	 * Gets the last modified time of the java class file.  If the class file is
	 * unavailable, it defaults to the time the servlets were loaded.
	 *
	 * @see  WebSiteFrameworkConfiguration#getServletDirectory
	 * @see  ErrorReportingServlet#getUptime()
	 */
	protected final long getClassLastModified() throws IOException, SQLException {
		String dir=WebSiteFrameworkConfiguration.getServletDirectory();
		if(dir!=null && dir.length()>0) {
			// Try to get from the class file
			long lastMod=new File(dir, getClass().getName().replace('.', File.separatorChar) + ".class").lastModified();
			if(lastMod!=0 && lastMod!=-1) return lastMod;
		}
		return getClassLoaderUptime();
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

	/**
	 * First, resolves correct instance of <code>WebPage</code>.  Next, handles security
	 * mode, authentication check, and redirects.  Anything left goes on to <code>doGet</code>.
	 *
	 * @see  #doGet(WebSiteRequest,HttpServletResponse)
	 */
	@Override
	final protected void reportingDoGet(HttpServletRequest httpReq, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		WebSiteRequest req=getWebSiteRequest(httpReq);
		WebPage page=getWebPage(getClass(), req);
		// Logout when requested
		boolean isLogout="true".equals(req.getParameter("logout_requested"));
		if(isLogout) req.logout(resp);

		// Check authentication first and return HTTP error code
		boolean alreadyDone=false;
		if("true".equals(req.getParameter("login_requested"))) {
			page.printLoginForm(page, new LoginException("Please Login"), req, resp);
			alreadyDone = true;
		}
		if(!alreadyDone) {
			try {
				WebSiteUser user=req.getWebSiteUser(resp);
				if(!page.canAccess(user)) {
					page.printUnauthorizedPage(page, req, resp);
					alreadyDone=true;
				}
			} catch(LoginException err) {
				page.printLoginForm(page, err, req, resp);
				alreadyDone=true;
			}
		}
		if(!alreadyDone) {
			String redirect=page.getRedirectURL(req);
			if(redirect!=null) {
				ServletUtil.sendRedirect(req, resp, redirect, HttpServletResponse.SC_MOVED_TEMPORARILY);
			} else {
				page.doGet(req, resp);
			}
		}
	}

	/**
	 * The layout is automatically applied to the page, then <code>doGet</code> is called.  To not have this automatically applied,
	 * override this method.  By the time this method is called, security checks, authentication, and redirects have been done.<br />
	 * <br />
	 * The first thing this method does is print the frameset if needed.  Second, it uses the output cache to quickly print
	 * the output if possible.  And third, it will call doGet(ChainWriter,WebSiteRequest,HttpServletResponse) with a stream
	 * directly out if the first two actions were not taken.
	 *
	 * @see #doGet(ChainWriter,WebSiteRequest,HttpServletResponse)
	 */
	protected void doGet(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		WebPageLayout layout=getWebPageLayout(req);

		ChainWriter out=getHTMLChainWriter(req, resp);
		try {
			layout.startHTML(this, req, resp, out, null);
			doGet(out, req, resp);
			layout.endHTML(this, req, resp, out);
		} finally {
			out.flush();
			out.close();
		}
	}

	/**
	 * By default, GET provides no content.
	 *
	 * @param  out   the <code>ChainWriter</code> to send output to
	 * @param  req   the current <code>WebSiteRequest</code>
	 * @param  resp  the <code>HttpServletResponse</code> for this request, is <code>null</code> when searching
	 */
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException, SQLException {
	}

	/**
	 * First, resolves correct instance of <code>WebPage</code>.  Then, handles
	 * security mode, authentication check, and redirects.  Anything left goes
	 * on to <code>doPostWithSearch</code>.
	 *
	 * @see  #doPostWithSearch(WebSiteRequest,HttpServletResponse)
	 */
	@Override
	protected final void reportingDoPost(HttpServletRequest httpReq, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		WebSiteRequest req=getWebSiteRequest(httpReq);
		WebPage page=getWebPage(getClass(), req);
		// Logout when requested
		boolean isLogout="true".equals(req.getParameter("logout_requested"));
		if(isLogout) req.logout(resp);

		// Check authentication first and return HTTP error code
		boolean alreadyDone=false;
		if("true".equals(req.getParameter("login_requested"))) {
			page.printLoginForm(page, new LoginException("Please Login"), req, resp);
			alreadyDone = true;
		}
		if(!alreadyDone) {
			try {
				WebSiteUser user=req.getWebSiteUser(resp);
				if(!page.canAccess(user)) {
					page.printUnauthorizedPage(page, req, resp);
					alreadyDone=true;
				}
			} catch(LoginException err) {
				page.printLoginForm(page, err, req, resp);
				alreadyDone=true;
			}
		}
		if(!alreadyDone) {
			String redirect=page.getRedirectURL(req);
			if(redirect!=null) {
				ServletUtil.sendRedirect(req, resp, redirect, HttpServletResponse.SC_MOVED_TEMPORARILY);
			} else {
				if(isLogout || (req.getParameter("login_username")!=null && req.getParameter("login_password")!=null)) page.doGet(req, resp);
				else page.doPostWithSearch(req, resp);
			}
		}
	}

	/**
	 * Handles any search posts, sends everything else on to <code>doPost(WebSiteRequest,HttpServletResponse)</code>.
	 * The search assumes the search parameters of <code>search_query</code> and <code>search_target</code>.  Both
	 * these values must be present for a search to be performed.  Search target may be either <code>"this_area"</code>
	 * or <code>"entire_site"</code>, defaulting to <code>"area"</code> for any other value.
	 *
	 * @see  #doPost(WebSiteRequest,HttpServletResponse)
	 */
	protected void doPostWithSearch(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		String query=req.getParameter("search_query");
		String searchTarget=req.getParameter("search_target");
		WebPageLayout layout=getWebPageLayout(req);
		if(query!=null && searchTarget!=null) {
			try (ChainWriter out = getHTMLChainWriter(req, resp)) {
				layout.startHTML(this, req, resp, out, "document.forms['search_two'].search_query.select(); document.forms['search_two'].search_query.focus();");
				boolean entire_site=searchTarget.equals("entire_site");
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
			}
		} else {
			doPost(req, resp);
		}
	}

	/**
	 * By default, a post request is just sets up the content beginning and calls <code>doPost</code>
	 *
	 * @param  req       the current <code>WebSiteRequest</code>
	 * @param  resp      the <code>HttpServletResponse</code> for this request
	 *
	 * @see  #doPost(ChainWriter,WebSiteRequest,HttpServletResponse)
	 */
	protected void doPost(
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException, SQLException {
		ChainWriter out=getHTMLChainWriter(req, resp);
		try {
			WebPageLayout layout=getWebPageLayout(req);
			layout.startHTML(this, req, resp, out, null);

			doPost(out, req, resp);

			layout.endHTML(this, req, resp, out);
		} finally {
			out.flush();
			out.close();
		}
	}

	/**
	 * By default, a post request just calls <code>doGet</code>
	 *
	 * @param  out  the <code>ChainWriter</code> to write to
	 * @param  req  the current <code>WebSiteRequest</code>
	 * @param  resp  the <code>HttpServletResponse</code> for this request
	 *
	 * @see  #doGet(ChainWriter,WebSiteRequest,HttpServletResponse)
	 */
	protected void doPost(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException, SQLException {
		doGet(out, req, resp);
	}


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
	 * Sets the content type, encoding to UTF-8, sets the additional headers, then returns the <code>ChainWriter</code>.
	 *
	 * @see  #getAdditionalHeaders
	 */
	protected final ChainWriter getHTMLChainWriter(WebSiteRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
		String[] headers=getAdditionalHeaders(req);
		if(headers!=null) {
			int len=headers.length;
			for(int c=0; c<len; c+=2) resp.setHeader(headers[c], headers[c+1]);
		}
		return new ChainWriter(resp.getWriter());
	}

	/**
	 * Sets the content type, encoding to UTF-8, sets the additional headers, then returns the <code>OutputStream</code>.
	 *
	 * @see  #getAdditionalHeaders
	 */
	protected final OutputStream getHTMLOutputStream(WebSiteRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
		String[] headers=getAdditionalHeaders(req);
		if(headers!=null) {
			int len=headers.length;
			for(int c=0; c<len; c+=2) resp.setHeader(headers[c], headers[c+1]);
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
	public String getKeywords() throws IOException, SQLException {
		return getParent().getKeywords();
	}

	/**
	 * Gets the text for the navigation image to use to represent this page.  Defaults to <code>getShortTitle</code>.
	 *
	 * @return  the alt text of the navigation image
	 *
	 * @see  #getShortTitle
	 * @see  #getNavImageSuffix
	 * @see  #getNavImageURL
	 */
	public String getNavImageAlt(WebSiteRequest req) throws IOException, SQLException {
		return getShortTitle();
	}

	/**
	 * Gets the text that will be placed in to the right of the navigation image.  If the
	 * image is not large enough to hold both <code>getNavImageAlt</code> and <code>getNavImageSuffix</code>,
	 * the beginning is truncated and <code>...</code> appended so that both fit the image.
	 *
	 * @see  #getNavImageAlt
	 * @see  #getNavImageURL
	 */
	public String getNavImageSuffix(WebSiteRequest req) throws IOException, SQLException {
		return null;
	}

	/**
	 * Gets the URL associated with a nav image.
	 *
	 * @see  #getNavImageAlt
	 * @see  #getNavImageSuffix
	 */
	public String getNavImageURL(WebSiteRequest req, HttpServletResponse resp, Object params) throws IOException, SQLException {
		return resp.encodeURL(req.getContextPath()+req.getURL(this, params));
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
	 * @see  WebSiteFrameworkConfiguration#useWebSiteCaching
	 * @see  #getWebPages(WebSiteRequest)
	 */
	synchronized public WebPage[] getCachedPages(WebSiteRequest req) throws IOException, SQLException {
		if(WebSiteFrameworkConfiguration.useWebSiteCaching()) {
			WebPage[] myPages=this.pages;
			if(myPages==null) myPages=this.pages=getWebPages(req);
			return myPages;
		} else return getWebPages(req);
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
	 * @see  WebSiteFrameworkConfiguration#useWebSiteCaching()
	 * @see  #isHandler(WebSiteRequest)
	 */
	public static WebPage getWebPage(ServletContext context, Class<? extends WebPage> clazz, WebSiteRequest req) throws IOException {
		if(context==null) throw new IllegalArgumentException("context is null");
		String classname=clazz.getName();
		boolean use_caching=WebSiteFrameworkConfiguration.useWebSiteCaching();
		if(use_caching) {
			// First look for a match in the cache
			List<WebPage> list=webPageCache.get(classname);
			if(list!=null) {
				int size=list.size();
				for(int c=0;c<size;c++) {
					WebPage page=list.get(c);
					if(page.getClass()==clazz && page.isHandler(req)) return page;
				}
			}
		}

		// Make a new instance and store in cache
		try {
			if(!use_caching) clazz=loadClass(classname);
			Constructor<? extends WebPage> con=clazz.getConstructor(getWebPageRequestParams);
			WebPage page=con.newInstance(new Object[] {req});
			page.setServletContext(context);
			if(use_caching) {
				List<WebPage> list=webPageCache.get(classname);
				if(list==null) webPageCache.put(classname, list=new ArrayList<>());
				list.add(page);
			}
			return page;
		} catch (ClassNotFoundException | ClassCastException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
			throw new IOException("Unable to getWebPage: "+clazz.getName()+", classname="+classname+", req="+req, e);
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
	 * @see  WebSiteFrameworkConfiguration#useWebSiteCaching()
	 * @see  #isHandler(Object)
	 */
	public static WebPage getWebPage(ServletContext context, Class<? extends WebPage> clazz, Object params) throws IOException {
		String classname=clazz.getName();
		boolean use_caching=WebSiteFrameworkConfiguration.useWebSiteCaching();
		if(use_caching) {
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
		}

		// Make a new instance and store in cache
		try {
			if(!use_caching) clazz=loadClass(classname);
			Constructor<? extends WebPage> con=clazz.getConstructor(getWebPageObjectParams);
			WebPage page=con.newInstance(new Object[] {params});
			page.setServletContext(context);
			if(use_caching) {
				List<WebPage> list=webPageCache.get(classname);
				if(list==null) webPageCache.put(classname, list=new ArrayList<>());
				list.add(page);
			}
			return page;
		} catch (ClassNotFoundException | ClassCastException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
			throw new IOException("Unable to getWebPage: "+clazz.getName()+", classname="+classname+", params="+params, e);
		}
	}

	/**
	 * Gets the time that the classloader was instantiated.  This will indicate
	 * the modified time of classes that are dynamically loaded.
	 */
	public static long getClassLoaderUptime() {
		WebPageClassLoader loader=webPageClassLoader;
		if(loader!=null) return WebPageClassLoader.getUptime();
		return getUptime();
	}

	private static WebPageClassLoader webPageClassLoader;
	private static Map<String,Long> classnameCache=new HashMap<>();

	/**
	 * Dynamically loads new classes based on the source .class file's modified time.
	 */
	synchronized public static Class<? extends WebPage> loadClass(String className) throws ClassNotFoundException {
		try {
			if(WebSiteFrameworkConfiguration.useWebSiteCaching()) {
				return Class.forName(className).asSubclass(WebPage.class);
			} else {
				// Find the directory to work in
				File dir=new File(WebSiteFrameworkConfiguration.getServletDirectory());
				File file=new File(dir, className.replace('.', '/') + ".class");
				long lastModified=file.lastModified();

				// Look in the cache for an existing class
				Long modified=classnameCache.get(className);
				if(
					webPageClassLoader==null
					|| (
						modified!=null
						&& modified!=lastModified
					)
				) {
					webPageClassLoader=new WebPageClassLoader();
					classnameCache.clear();
					modified=null;
				}
				Class<? extends WebPage> clazz=webPageClassLoader.loadClass(className).asSubclass(WebPage.class);
				if(modified==null) {
					classnameCache.put(className, lastModified);
				}
				return clazz;
			}
		} catch(IOException err) {
			throw new ClassNotFoundException("Unable to loadClass: "+className, err);
		}
	}

	/**
	 * Gets the current layout for this page.  By default, takes the layout
	 * of the parent page.
	 *
	 * @return  the <code>WebPageLayout</code>
	 */
	public WebPageLayout getWebPageLayout(WebSiteRequest req) throws IOException, SQLException {
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

			// The counted matches will go here
			int totalMatches = 0;
			int size;

			// Search the byte data only if not able to index
			long mySearchLastModified = getSearchLastModified();
			if (mySearchLastModified == -1) {
				title = getTitle();
				description = getDescription();
				author = getAuthor();
				String keywords = getKeywords();

				// Get the HTML content
				bytes.reset();
				ChainWriter out = new ChainWriter(bytes);
				try {
					doGet(out, null, response);
				} finally {
					out.flush();
					out.close();
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
							String keywords = getKeywords();

							// Get the HTML content
							bytes.reset();
							ChainWriter out = new ChainWriter(bytes);
							try {
								doGet(out, null, null);
							} catch(NullPointerException err) {
								getLogger().log(Level.WARNING, null, err);
							} finally {
								out.flush();
								out.close();
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
						req.getContextPath()+req.getURL(this),
						probability,
						title == null ? getTitle() : title,
						description == null ? getDescription() : description,
						author == null ? getAuthor() : author
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
