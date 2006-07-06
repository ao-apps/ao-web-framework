package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.util.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Generates a list of all URLs in the site.  This is useful for adding this site to
 * search engines.
 *
 * @author  AO Industries, Inc.
 */
abstract public class DumpURLs extends WebPage {

    public DumpURLs() {}

    public DumpURLs(WebSiteRequest req) {
        super(req);
    }

    public DumpURLs(Object param) {
        super(param);
    }

    public void doGet(
        ChainWriter out,
        WebSiteRequest req,
        HttpServletResponse resp
    ) throws IOException, SQLException {
        out.print("The following is a list of all unique servlet URLs in the site and may be used to add this site to\n"
                + "search engines:\n"
                + "<pre>\n");
        WebPage page = getRootPage();
        printURLs(req, out, page, new SortedArrayList<WebPage>());
        out.print("</pre>\n");
    }

    public String getDescription() {
        return "Lists all of the URLs in the site, useful for adding to search engines.";
    }

    public String getKeywords() {
        return "search, engine, URL, list, add, hit, hits, adding";
    }

    /**
     * The last modified time is -1 to always reload the list.
     */
    public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
        return -1;
    }

    /**
     * Do not include this in the search results.
     */
    public void search(String[] words, WebSiteRequest req, ArrayList results, BetterByteArrayOutputStream bytes, SortedArrayList finishedPages) {
    }

    public long getSearchLastModified() throws IOException, SQLException {
        return getClassLastModified();
    }

    public String getTitle() {
        return "List URLs";
    }

    private void printURLs(WebSiteRequest req, ChainWriter out, WebPage page, List<WebPage> finishedPages) throws IOException, SQLException {
        if(!finishedPages.contains(page)) {
            out.print("<A class='ao_light_link' href='").printEI(req.getURL(page)).print("'>").printEH(req.getURL(page, page.useEncryption(), null)).print("</A>\n");

            finishedPages.add(page);

            WebPage[] pages = page.getCachedPages(req);
            int len = pages.length;
            for (int c = 0; c < len; c++) printURLs(req, out, pages[c], finishedPages);
        }
    }
}