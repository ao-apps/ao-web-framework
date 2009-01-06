package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import com.aoindustries.security.*;
import com.aoindustries.sql.*;
import com.aoindustries.util.*;
import com.aoindustries.util.sort.AutoSort;
import gnu.regexp.*;
import java.awt.*;
import java.awt.font.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * The main web page provides the overall layout of the site.  The rest of
 * the site overrides methods of this class, but cannot override the
 * <code>reportingDoGet</code>, <code>reportingDoPost</code>, or
 * <code>reportingGetLastModified</code> methods.
 *
 * @author  AO Industries, Inc.
 */
abstract public class WebPage extends ErrorReportingServlet {

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
    private static final Map<String,List<WebPage>> webPageCache=new HashMap<String,List<WebPage>>();

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
    private final List<String> searchWords=new SortedArrayList<String>();

    /**
     * The number times each word appears in the document.
     */
    private final List<int[]> searchCounts=new ArrayList<int[]>();

    public static RE reHTMLPattern;
    //private static RE reWordPattern;
    static {
        try {
            reHTMLPattern = new RE("<.*?>");
            //reWordPattern = new RE("(\\w*)");
        } catch (REException e) {
            log(null, "Unable to load regular expression", e, null);
        }
    }

    private static final Class[] getWebPageRequestParams={
        WebSiteRequest.class
    };

    private static final Class[] getWebPageObjectParams={
        Object.class
    };

    /**
     * The output may be cached for greater throughput.
     * @see  #doGet(WebSiteRequest,HttpServletResponse)
     */
    private Map<Object,OutputCacheEntry> outputCache;

    public WebPage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public WebPage(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public WebPage(Object param) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    private void addSearchWords(String words, int weight) {
        Profiler.startProfile(Profiler.UNKNOWN, WebPage.class, "addSearchWords(String,int)", null);
        try {
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
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Determines if the provided user can access the page.  Defaults
     * to inheriting the behavior of the parent page.
     */
    public boolean canAccess(WebSiteUser user) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "canAccess(WebSiteUser)", null);
        try {
            return getParent().canAccess(user);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Prints the form that is used to login.
     */
    public void printLoginForm(WebPage page, LoginException loginException, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "printLoginForm(WebPage,LoginException,WebSiteRequest,HttpServletResponse)", null);
        try {
            getParent().printLoginForm(page, loginException, req, resp);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Prints the unauthorized page message.
     */
    public void printUnauthorizedPage(WebPage page, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "printUnauthorizedPage(WebPage,WebSiteRequest,HttpServletResponse)", null);
        try {
            getParent().printUnauthorizedPage(page, req, resp);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * First, resolves correct instance of <code>WebPage</code>.  Then, if security mode
     * mismatches, authentication failed, or the page is a redirect, returns <code>-1</code>
     * for unknown.  Otherwise, call <code>getLastModified(WebSiteRequest)</code>
     *
     * @see  #getLastModified(WebSiteRequest)
     */
    final public long reportingGetLastModified(HttpServletRequest httpReq) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "reportingGetLastModified(HttpServletRequest)", null);
        try {
            WebSiteRequest req=getWebSiteRequest(httpReq);
            Class<? extends WebPage> thisClass = getClass();
            WebPage page=getWebPage(thisClass, req);
            if(
                WebSiteFrameworkConfiguration.getEnforceSecureMode()
                && page.enforceEncryption()
                && req.isSecure()!=page.useEncryption()
            ) return -1;

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
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getWebPageAndChildrenLastModified(WebSiteRequest)", null);
        try {
            WebPage[] pages = getCachedPages(req);
            int len = pages.length;
            long mostRecent = getClassLastModified();
            if(mostRecent==-1) return -1;
            for (int c = 0; c < len; c++) {
                long time = pages[c].getLastModified(req);
                if(time==-1) return -1;
                if (time > mostRecent) mostRecent = time;
            }
            return mostRecent;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Recursively gets the most recent modification time.
     */
    final public long getLastModifiedRecursive(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getLastModifiedRecursive(WebSiteRequest)", null);
        try {
            long time=getLastModified(req);
            WebPage[] pages=getCachedPages(req);
            int len=pages.length;
            for(int c=0; c<len; c++) {
                long time2=pages[c].getLastModifiedRecursive(req);
                if(time2>time) time=time2;
            }
            return time;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Recursively gets the most recent modification time of a file or directory.
     */
    public static long getLastModifiedRecursive(File file) {
        Profiler.startProfile(Profiler.IO, WebPage.class, "getLastModifiedRecursive(File)", null);
        try {
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
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    /**
     * Gets the last modified time for search indexing.  The index will be recreated if
     * the search last modified time is changed.  If this method returns <code>-1</code>,
     * no search index is built.  This defaults to be a call to <code>getLastModified</code>
     * with a null <code>WebSiteRequest</code>.
     */
    public long getSearchLastModified() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getSearchLastModified()", null);
        try {
            return getLastModified(null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * First, resolves correct instance of <code>WebPage</code>.  Next, handles security
     * mode, authentication check, and redirects.  Anything left goes on to <code>doGet</code>.
     *
     * @see  #doGet(WebSiteRequest,HttpServletResponse)
     */
    final protected void reportingDoGet(HttpServletRequest httpReq, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "reportingDoGet(HttpServletRequest,HttpServletResponse)", null);
        try {
            WebSiteRequest req=getWebSiteRequest(httpReq);
            WebPage page=getWebPage(getClass(), req);
            if(
                WebSiteFrameworkConfiguration.getEnforceSecureMode()
                && page.enforceEncryption()
                && req.isSecure()!=page.useEncryption()
                && req.getParameter("login_requested")==null
                && req.getParameter("login_username")==null
            ) {
                // Redirect to use proper encryption mode
                resp.sendRedirect(resp.encodeRedirectURL(req.getURL(page, page.useEncryption(), null)));
            } else {
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
                        resp.sendRedirect(resp.encodeRedirectURL(redirect));
                    } else {
                        page.doGet(req, resp);
                    }
                }
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * The layout is automatically applied to the page, then <code>doGet</code> is called.  To not have this automatically applied,
     * override this method.  By the time this method is called, security checks, authentication, and redirects have been done.<BR>
     * <BR>
     * The first thing this method does is print the frameset if needed.  Second, it uses the output cache to quickly print
     * the output if possible.  And third, it will call doGet(ChainWriter,WebSiteRequest,HttpServletResponse) with a stream
     * directly out if the first two actions were not taken.
     *
     * @see #getOutputCacheKey(WebSiteRequest)
     * @see #doGet(ChainWriter,WebSiteRequest,HttpServletResponse)
     */
    protected void doGet(WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "doGet(WebSiteRequest,HttpServletResponse)", null);
        try {
            WebPageLayout layout=getWebPageLayout(req);

            boolean doRegular=true;
            if(layout.useFrames(this, req)) {
                ChainWriter out=getHTMLChainWriter(req, resp);
		try {
		    req.setUsingFrames(true);
		    String frame=req.getParameter("frame");
		    if(
		       frame==null
		       || (frame=frame.trim()).length()==0
		       || !layout.printFrame(this, req, resp, out, null, frame)
		       ) layout.printFrameSet(this, req, resp, out);
		} finally {
		    out.flush();
		    out.close();
		}
                doRegular=false;
            }
            if(doRegular) {
                req.setUsingFrames(false);
                if(WebSiteFrameworkConfiguration.useWebSiteCaching() && req.getParameter("login_requested")==null && req.getParameter("login_username")==null) {
                    // Try to use the cache if the last modified time is available
                    long pageLastModified=getLastModified(req);
                    if(pageLastModified!=-1) {
                        Object outputCacheKey=getOutputCacheKey(req);
                        if(outputCacheKey!=null) {
                            OutputCacheEntry existingCache;
                            synchronized(this) {
                                if(outputCache==null) {
                                    outputCache=new HashMap<Object,OutputCacheEntry>();
                                    existingCache=null;
                                } else existingCache=outputCache.get(outputCacheKey);

                                if(existingCache==null || existingCache.lastModified!=pageLastModified) {
                                    ByteArrayOutputStream bout=new ByteArrayOutputStream();
                                    ChainWriter out=new ChainWriter(bout);
				    try {
					layout.startHTML(this, req, resp, out, null);
					doGet(out, req, resp);
					layout.endHTML(this, req, out);
				    } finally {
					out.flush();
					out.close();
				    }
                                    outputCache.put(outputCacheKey, existingCache=new OutputCacheEntry(outputCacheKey, pageLastModified, bout.toByteArray()));
                                }
                            }
                            OutputStream out=getHTMLOutputStream(req, resp);
			    try {
				out.write(existingCache.bytes);
			    } finally {
				out.flush();
				out.close();
			    }
                            doRegular=false;
                        }
                    }
                }
                if(doRegular) {
                    ChainWriter out=getHTMLChainWriter(req, resp);
		    try {
			layout.startHTML(this, req, resp, out, null);
			doGet(out, req, resp);
			layout.endHTML(this, req, out);
		    } finally {
			out.flush();
			out.close();
		    }
                }
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
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
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    /**
     * First, resolves correct instance of <code>WebPage</code>.  Then, handles
     * security mode, authentication check, and redirects.  Anything left goes
     * on to <code>doPostWithSearch</code>.
     *
     * @see  #doPostWithSearch(WebSiteRequest,HttpServletResponse)
     */
    protected final void reportingDoPost(HttpServletRequest httpReq, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.UNKNOWN, WebPage.class, "reportingDoPost(HttpServletRequest,HttpServletResponse)", null);
        try {
            WebSiteRequest req=getWebSiteRequest(httpReq);
            WebPage page=getWebPage(getClass(), req);
            if(
                WebSiteFrameworkConfiguration.getEnforceSecureMode()
                && page.enforceEncryption()
                && req.isSecure()!=page.useEncryption()
                && req.getParameter("login_requested")==null
                && req.getParameter("login_username")==null
            ) {
                // Redirect to use proper encryption mode
                resp.sendRedirect(resp.encodeRedirectURL(req.getURL(page, page.useEncryption(), null)));
            } else {
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
                    if(redirect!=null) resp.sendRedirect(resp.encodeRedirectURL(redirect));
                    else {
                        if(isLogout || (req.getParameter("login_username")!=null && req.getParameter("login_password")!=null)) page.doGet(req, resp);
                        else page.doPostWithSearch(req, resp);
                    }
                }
            }
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
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
    protected void doPostWithSearch(WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.UNKNOWN, WebPage.class, "doPostWithSearch(WebSiteRequest,HttpServletResponse)", null);
        try {
            String query=req.getParameter("search_query");
            String searchTarget=req.getParameter("search_target");
            WebPageLayout layout=getWebPageLayout(req);
            if(query!=null && searchTarget!=null) {
                // Search request
                ChainWriter out=getHTMLChainWriter(req, resp);
		try {
		    req.setUsingFrames(false);
		    layout.startHTML(this, req, resp, out, "document.forms['search_two'].search_query.select(); document.forms['search_two'].search_query.focus();");
		    boolean entire_site=searchTarget.equals("entire_site");
		    WebPage target = entire_site ? getRootPage() : this;

		    // If the target contains no pages, use its parent
		    if(target.getCachedPages(req).length==0) target=target.getParent();

		    // Get the list of words to search for
		    String[] words=StringUtility.splitString(query.replace('.', ' '));

		    List<SearchResult> results=new ArrayList<SearchResult>();
		    if(words.length>0) {
			// Perform the search
			target.search(words, req, results, new BetterByteArrayOutputStream(), new SortedArrayList<WebPage>());
                        AutoSort.sortStatic(results);
			//StringUtility.sortObjectsAndFloatDescending(results, 1, 5);
		    }

		    layout.printSearchOutput(this, out, req, resp, query, entire_site, results, words);

		    layout.endHTML(this, req, out);
		} finally {
		    out.close();
		}
            } else {
                boolean useFrames=layout.useFrames(this, req);
                req.setUsingFrames(useFrames);
                doPost(req, resp);
            }
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
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
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "doPost(WebSiteRequest,HttpServletResponse)", null);
        try {
            ChainWriter out=getHTMLChainWriter(req, resp);
	    try {
		WebPageLayout layout=getWebPageLayout(req);
		layout.startHTML(this, req, resp, out, null);

		doPost(out, req, resp);

		layout.endHTML(this, req, out);
	    } finally {
		out.flush();
		out.close();
	    }
        } finally {
            Profiler.endProfile(Profiler.FAST);
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
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "doPost(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            doGet(out, req, resp);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets whether this page should enforce the current encrpyption requirement or not.
     */
    public boolean enforceEncryption() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "enforceEncryption()", null);
        try {
            return getParent().enforceEncryption();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }


    /**
     * Determines if this page equals another page.
     *
     * @see  #equals(WebPage)
     */
    final public boolean equals(Object O) {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "equals(Object)", null);
        try {
            return
                (O instanceof WebPage)
                && equals((WebPage)O)
            ;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Determines if this page equals another page.  By default, two pages
     * of the same classname are considered equal.
     *
     * @see  #hashCode
     */
    public boolean equals(WebPage other) {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "equals(WebPage)", null);
        try {
            return other.getClass().getName().equals(getClass().getName());
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * The default hashcode for a page is the hashcode of its
     * classname.
     *
     * @see  #equals(WebPage)
     */
    public int hashCode() {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "hashCode()", null);
        try {
            return getClass().getName().hashCode();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets additional headers for this page.  The format must be in a String[] of name/value pairs, two elements each, name and then value.
     */
    public String[] getAdditionalHeaders(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getAdditionalHeaders(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the author of this page.  By default, the author of the parent page is used.
     */
    public String getAuthor() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getAuthor()", null);
        try {
            return getParent().getAuthor();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the preferred width of this content in pixels or <code>-1</code> for no preference.
     * It is up to the <code>WebPageLayout</code> to make use of this value.  The preferred width
     * defaults to the preferred width of the parent page.
     *
     * @see  WebPageLayout
     */
    public int getPreferredContentWidth(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getPreferredContentWidth(WebSiteRequest)", null);
        try {
            return getParent().getPreferredContentWidth(req);
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the vertical alignment of the content area.  Defaults to <code>"top"</code>.
     * It is up to the <code>WebPageLayout</code> to make use of this value.
     *
     * @see  WebPageLayout
     */
    public String getContentVAlign(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getContentVAlign(WebSiteRequest)", null);
        try {
            return "top";
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the description of this page.  By default, the description of the parent page is used.
     */
    public String getDescription() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getDescription()", null);
        try {
            return getParent().getDescription();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the root page in the web page hierarchy.  The root page has no parent.
     */
    public final WebPage getRootPage() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getRootPage()", null);
        try {
            WebPage page = this;
            WebPage parent;
            while ((parent = page.getParent()) != null) page = parent;
            return page;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Sets the content type, sets the additional headers, then returns the <code>ChainWriter</code>.
     *
     * @see  #getAdditionalHeaders
     */
    protected final ChainWriter getHTMLChainWriter(WebSiteRequest req, HttpServletResponse resp) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getHTMLChainWriter(WebSiteRequest,HttpServletResponse)", null);
        try {
            resp.setContentType("text/html");
            String[] headers=getAdditionalHeaders(req);
            if(headers!=null) {
                int len=headers.length;
                for(int c=0; c<len; c+=2) resp.setHeader(headers[c], headers[c+1]);
            }
            return new ChainWriter(resp.getWriter());
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Sets the content type, sets the additional headers, then returns the <code>OutputStream</code>.
     *
     * @see  #getAdditionalHeaders
     */
    protected final OutputStream getHTMLOutputStream(WebSiteRequest req, HttpServletResponse resp) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getHTMLOutputStream(WebSiteRequest,HttpServletResponse)", null);
        try {
            resp.setContentType("text/html");
            String[] headers=getAdditionalHeaders(req);
            if(headers!=null) {
                int len=headers.length;
                for(int c=0; c<len; c+=2) resp.setHeader(headers[c], headers[c+1]);
            }
            return resp.getOutputStream();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getJavaScriptSrc(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the keywords for this page.  By default, the keywords of the parent page are used.
     */
    public String getKeywords() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getKeywords()", null);
        try {
            return getParent().getKeywords();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getNavImageAlt(WebSiteRequest)", null);
        try {
            return getShortTitle();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getNavImageSuffix(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the URL associated with a nav image.
     *
     * @see  #getNavImageAlt
     * @see  #getNavImageSuffix
     */
    public String getNavImageURL(WebSiteRequest req, Object params) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getNavImageURL(WebSiteRequest,Object)", null);
        try {
            return req.getURL(this, params);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the index of this page in the parents list of children pages.
     */
    final public int getPageIndexInParent(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getPageIndexInParent(WebSiteRequest)", null);
        try {
            WebPage[] pages=getParent().getCachedPages(req);
            int len=pages.length;
            for(int c=0;c<len;c++) if(pages[c].equals(this)) return c;
            throw new RuntimeException("Unable to find page index in parent.");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the <code>WebPage</code> that follows this one in the parents
     * list of pages.
     *
     * @return  the <code>WebPage</code> or <code>null</code> if not found
     */
    final public WebPage getNextPage(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getNextPage(WebSiteRequest)", null);
        try {
            WebPage parent=getParent();
            if (parent!=null) {
		WebPage[] pages=parent.getCachedPages(req);
		int len=pages.length;
		for(int c=0; c<len; c++) {
                    if(pages[c].getClass() == getClass()) {
                        if (c < (len - 1)) return pages[c + 1];
                        return null;
                    }
		}
            }
            return null;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the <code>WebPage</code> that proceeds this one in the parents
     * list of pages.
     *
     * @return  the <code>WebPage</code> or <code>null</code> if not found
     */
    final public WebPage getPreviousPage(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getPreviousPage(WebSiteRequest)", null);
        try {
            WebPage parent = getParent();
            if (parent != null) {
                WebPage[] pages = parent.getCachedPages(req);
                int len = pages.length;
                for (int c = 0; c < len; c++) {
                    if (pages[c].getClass() == getClass()) {
                        if (c > 0) return pages[c - 1];
                        return null;
                    }
                }
            }
            return null;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the JavaScript that should be executed with the onLoad event of the body tag
     *
     * @param  req  the current <code>WebSiteRequest</code>
     *
     * @return  a <code>String</code> or <code>null</code> for none
     */
    public String getOnLoadScript(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getOnLoadScript(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
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
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getCachedPages(WebSiteRequest)", null);
        try {
            if(WebSiteFrameworkConfiguration.useWebSiteCaching()) {
                WebPage[] pages=this.pages;
                if(pages==null) pages=this.pages=getWebPages(req);
                return pages;
            } else return getWebPages(req);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the parent of this page or <code>null</code> for none.
     */
    public abstract WebPage getParent() throws IOException, SQLException;

    /**
     * Gets the URL to direct to.  Redirection happens before specific frameset actions,
     * thus allowing one to redirect various frames to different places.
     *
     * @return  the relative or absolute URL to redirect to or <code>null</code> for
     *          no redirect.
     */
    public String getRedirectURL(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getRedirectURL(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
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
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getShortTitle()", null);
        try {
            return getTitle();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the title of the web page in order to generate the HTML.  The
     * title defaults to that of the parent page.
     *
     * @return  the page title
     */
    public String getTitle() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getTitle()", null);
        try {
            return getParent().getTitle();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets parameters that are added to the query string of URLs generated for this page.
     */
    public Object getURLParams(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getURLParams(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * @see  #getWebPage(ServletContext,Class,WebSiteRequest)
     */
    public WebPage getWebPage(Class<? extends WebPage> clazz, WebSiteRequest req) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getWebPage(Class<? extends WebPage>,WebSiteRequest)", null);
        try {
            return getWebPage(getServletContext(), clazz, req);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getWebPage(ServletContext,Class<? extends WebPage>,WebSiteRequest)", null);
        try {
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
                    if(list==null) webPageCache.put(classname, list=new ArrayList<WebPage>());
                    list.add(page);
                }
                return page;
            } catch (ClassNotFoundException e) {
                log(context, null, e, new Object[] {"classname="+classname, "req="+req});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (ClassCastException e) {
                log(context, null, e, new Object[] {"classname="+classname, "req="+req});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (IllegalAccessException e) {
                log(context, null, e, new Object[] {"classname="+classname, "req="+req});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (InstantiationException e) {
                log(context, null, e, new Object[] {"classname="+classname, "req="+req});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (InvocationTargetException e) {
                log(context, null, e, new Object[] {"classname="+classname, "req="+req});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (NoSuchMethodException e) {
                log(context, null, e, new Object[] {"classname="+classname, "req="+req});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * @see  #getWebPage(ServletContext,Class,Object)
     */
    public WebPage getWebPage(Class<? extends WebPage> clazz, Object param) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getWebPage(Class<? extends WebPage>,Object)", null);
        try {
            return getWebPage(getServletContext(), clazz, param);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getWebPage(ServletContext,Class<? extends WebPage>,Object)", null);
        try {
            String classname=clazz.getName();
            boolean use_caching=WebSiteFrameworkConfiguration.useWebSiteCaching();
            if(use_caching) {
                // First look for a match in the cache
                List<WebPage> list=webPageCache.get(classname);
                if(list!=null) {
                    int size=list.size();
                    for(int c=0;c<size;c++) {
                        WebPage page=list.get(c);
                        if(page.getClass()==clazz && page.isHandler(params)) return page;
                    }
                }
            }

            // Make a new instance and store in cache
            try {
                if(!use_caching) clazz=loadClass(classname);
                Constructor con=clazz.getConstructor(getWebPageObjectParams);
                WebPage page=(WebPage)con.newInstance(new Object[] {params});
                page.setServletContext(context);
                if(use_caching) {
                    List<WebPage> list=webPageCache.get(classname);
                    if(list==null) webPageCache.put(classname, list=new ArrayList<WebPage>());
                    list.add(page);
                }
                return page;
            } catch (ClassNotFoundException e) {
                log(context, null, e, new Object[] {"classname="+classname, "params="+params});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (ClassCastException e) {
                log(context, null, e, new Object[] {"classname="+classname, "params="+params});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (IllegalAccessException e) {
                log(context, null, e, new Object[] {"classname="+classname, "params="+params});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (InstantiationException e) {
                log(context, null, e, new Object[] {"classname="+classname, "params="+params});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (InvocationTargetException e) {
                log(context, null, e, new Object[] {"classname="+classname, "params="+params});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            } catch (NoSuchMethodException e) {
                log(context, null, e, new Object[] {"classname="+classname, "params="+params});
                IOException ioExc=new IOException("Unable to getWebPage: "+clazz.getName());
                ioExc.initCause(e);
                throw ioExc;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the time that the classloader was instantiated.  This will indicate
     * the modified time of classes that are dynamically loaded.
     */
    public static long getClassLoaderUptime() {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getClassloaderUptime()", null);
        try {
            WebPageClassLoader loader=webPageClassLoader;
            if(loader!=null) return loader.getUptime();
            return getUptime();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    private static WebPageClassLoader webPageClassLoader;
    private static Map<String,Long> classnameCache=new HashMap<String,Long>();

    /**
     * Dynamically loads new classes based on the source .class file's modified time.
     */
    synchronized public static Class<? extends WebPage> loadClass(String className) throws ClassNotFoundException {
        Profiler.startProfile(Profiler.UNKNOWN, WebPage.class, "loadClass(String)", null);
        try {
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
                            && modified.longValue()!=lastModified
                        )
                    ) {
                        webPageClassLoader=new WebPageClassLoader();
                        classnameCache.clear();
                        modified=null;
                    }
                    Class<? extends WebPage> clazz=webPageClassLoader.loadClass(className).asSubclass(WebPage.class);
                    if(modified==null) {
                        classnameCache.put(className, Long.valueOf(lastModified));
                    }
                    return clazz;
                }
            } catch(IOException err) {
                throw new ClassNotFoundException("Unable to loadClass: "+className, err);
            }
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Gets the current layout for this page.  By default, takes the layout
     * of the parent page.
     *
     * @return  the <code>WebPageLayout</code>
     */
    public WebPageLayout getWebPageLayout(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getWebPageLayout(WebSiteRequest)", null);
        try {
            return getParent().getWebPageLayout(req);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
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
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getWebPages(WebSiteRequest)", null);
        try {
            return emptyWebPageArray;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
    
    /**
     * Determines if this page is the instance that should handle a particular request.
     * By default returns <code>true</code>, meaning it is a handler for all requests
     * for this <code>Class</code>.
     *
     * @see  #getWebPage(ServletContext,Class,WebSiteRequest)
     */
    public boolean isHandler(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "isHandler(WebSiteRequest)", null);
        try {
            return true;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Determines if this page is the instance that represents a certain set of parameters.
     * By default returns <code>true</code>, meaning it is a handler for any parameters
     * for this <code>Class</code>.
     *
     * @see  #getWebPage(ServletContext,Class,Object)
     */
    public boolean isHandler(Object O) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "isHandler(Object)", null);
        try {
            return true;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
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
	List<SearchResult> results,
	BetterByteArrayOutputStream bytes,
        List<WebPage> finishedPages
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "search(String[],WebSiteRequest,List<SearchResult>,BetterByteArrayOutputStream,List<WebPage>)", null);
        try {
            standardSearch(words, req, results, bytes, finishedPages);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * The standard implementation of the search functionality.
     *
     * @see  #search
     */
    final public void standardSearch(
        String[] words,
        WebSiteRequest req,
        List<SearchResult> results,
        BetterByteArrayOutputStream bytes,
        List<WebPage> finishedPages
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.UNKNOWN, WebPage.class, "standardSearch(String[],WebSiteRequest,List<SearchResult>,BetterByteArrayOutputStream,List<WebPage>)", null);
        try {
            if(!finishedPages.contains(this)) {
                String title = null;
                String description = null;
                String author = null;

                // The counted matches will go here
                int totalMatches = 0;
                int size;

                // Search the byte data only if not able to index
                long searchLastModified = getSearchLastModified();
                if (searchLastModified == -1) {
                    title = getTitle();
                    description = getDescription();
                    author = getAuthor();
                    String keywords = getKeywords();

                    // Get the HTML content
                    bytes.reset();
                    ChainWriter out = new ChainWriter(bytes);
		    try {
			doGet(out, null, null);
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
                            StringUtility.countOccurances(keywords, word) * 10

                            // Add the description with weight 5
                            +StringUtility.countOccurances(description, word) * 5

                            // Add the title with weight 5
                            +StringUtility.countOccurances(title, word) * 5

                            // Add the content with weight 1
                            +StringUtility.countOccurances(content, size, word)

                            // Add the author with weight 1
                            +StringUtility.countOccurances(author, word);

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
                    if (searchLastModified != this.searchLastModified) {
                        // Only synchronize for index rebuild
                        synchronized (this) {
                            if (searchLastModified != this.searchLastModified) {
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
                                    getErrorHandler().reportWarning(err, null);
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
                                this.searchLastModified = searchLastModified;
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
                            int count = StringUtility.countOccurances(searchWord, word);
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
                            author == null ? getAuthor() : author
                        )
                    );
                }

                // Flag as done
                finishedPages.add(this);

                // Search recursively
                WebPage[] pages = getCachedPages(req);
                int len = pages.length;
                for (int c = 0; c < len; c++) pages[c].search(words, req, results, bytes, finishedPages);
            }
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Determines if this page should be sent encrypted.  By default, the encryption mode
     * of the parent page is used.
     */
    public boolean useEncryption() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "useEncryption()", null);
        try {
            return getParent().useEncryption();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Determines if this page wants to be placed in a frame set.
     * A page will not necessarily be placed in a frameset, however.
     * <code>WebPageLayout.useFrames</code> determines if frames are actually
     * used, and <code>WebSiteRequest.isUsingFrames</code> indicates if
     * frames are actually being used.  In no event should a page
     * the return <code>false</code> to <code>useFrames</code> be
     * put into a frameset.
     *
     * @see  WebPageLayout#useFrames
     * @see  WebSiteRequest#isUsingFrames
     */
    public boolean useFrames(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "useFrames(WebSiteRequest)", null);
        try {
            return false;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Determine if the nav image for this page should remain visible, even when
     * its children are displayed.  The default is <code>false</code>.
     */
    public boolean includeNavImageAsParent() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "includeNavImageAsParent()", null);
        try {
            return false;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Determines whether or not to display the page in the left navigation.
     */
    public boolean useNavImage() throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "useNavImage()", null);
        try {
            return true;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Determines if this page will be displayed in the standard site map.
     */
    public boolean useSiteMap() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "useSiteMap()", null);
        try {
            return true;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Determines if this page will be displayed in the location bar.
     */
    public boolean showInLocationPath(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "showInLocationPath(WebSiteRequest)", null);
        try {
            return true;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
    
    private ServletContext context;

    public ServletContext getServletContext() {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getServletContext()", null);
        try {
            if(context!=null) return context;
            ServletContext sc=super.getServletContext();
            if(sc==null) throw new NullPointerException("ServletContext is null");
            return sc;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    void setServletContext(ServletContext context) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "setServletContext(ServletContext)", null);
        try {
            this.context=context;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the copyright information for this page.  Defaults to the copyright of the parent page.
     */
    public String getCopyright(WebSiteRequest req, WebPage requestPage) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getCopyright(WebSiteRequest,WebPage)", null);
        try {
            return getParent().getCopyright(req, requestPage);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    /**
     * Gets the object that represents the output cache key.  The output cache is used if the returned key is not null,
     * the last modified time is not <code>-1</code>, and the last modified time of the page is the same as the last
     * modified time at cache time.
     *
     * @see #doGet(WebSiteRequest,HttpServletResponse)
     * @see #getLastModified(WebSiteRequest)
     */
    public Object getOutputCacheKey(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getOutputCacheKey(WebSiteRequest)", null);
        try {
            return null;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
    
    /**
     * Gets the path of for the URL relative to the top of the site.
     */
    public String getURLPath() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "getURLPath()", null);
        try {
            return generateURLPath(this);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    /**
     * Generates a URL path for this or another page, please call getURLPath() instead.
     * The default behavior is to ask the parent to generate the URL.  Therefore the
     * top-level <code>WebPage</code> of a site must implement this method.
     */
    public String generateURLPath(WebPage page) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPage.class, "generateURLPath(WebPage)", null);
        try {
            return getParent().generateURLPath(page);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the URL pattern for this page as used in <code>web.xml</code>.
     */
    public String getURLPattern() throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPage.class, "getURLPattern()", null);
        try {
            return "/"+getURLPath();
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
}
