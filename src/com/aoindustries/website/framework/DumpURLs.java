/*
 * Copyright 2000-2013, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.io.AoByteArrayOutputStream;
import com.aoindustries.io.ChainWriter;
import com.aoindustries.util.SortedArrayList;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * Generates a list of all URLs in the site.  This is useful for adding this site to
 * search engines.
 *
 * @author  AO Industries, Inc.
 */
abstract public class DumpURLs extends WebPage {

	private static final long serialVersionUID = 1L;

	public DumpURLs(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public DumpURLs(WebSiteRequest req) {
		super(req);
	}

	public DumpURLs(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		out.print("The following is a list of all unique servlet URLs in the site and may be used to add this site to\n"
				+ "search engines:\n"
				+ "<pre>\n");
		WebPage page = getRootPage();
		printURLs(req, resp, out, page, new SortedArrayList<WebPage>());
		out.print("</pre>\n");
	}

	@Override
	public String getDescription() {
		return "Lists all of the URLs in the site, useful for adding to search engines.";
	}

	@Override
	public String getKeywords() {
		return "search, engine, URL, list, add, hit, hits, adding";
	}

	/**
	 * The last modified time is -1 to always reload the list.
	 */
	@Override
	public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
		return -1;
	}

	/**
	 * Do not include this in the search results.
	 */
    @Override
    public void search(
        String[] words,
        WebSiteRequest req,
        HttpServletResponse response,
        List<SearchResult> results,
        AoByteArrayOutputStream bytes,
        List<WebPage> finishedPages
    ) {
    }

	@Override
	public long getSearchLastModified() throws IOException, SQLException {
		return getClassLastModified();
	}

	@Override
	public String getTitle() {
		return "List URLs";
	}

	private void printURLs(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, WebPage page, List<WebPage> finishedPages) throws IOException, SQLException {
		if(!finishedPages.contains(page)) {
			out.print("<a class='aoLightLink' href='").encodeXmlAttribute(resp.encodeURL(req.getURL(page))).print("'>").encodeXhtml(req.getURL(page)).print("</a>\n");

			finishedPages.add(page);

			WebPage[] pages = page.getCachedPages(req);
			int len = pages.length;
			for (int c = 0; c < len; c++) printURLs(req, resp, out, pages[c], finishedPages);
		}
	}
}
