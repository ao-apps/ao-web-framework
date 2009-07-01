package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Automatically generates a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AutoListPage extends WebPage {

    public AutoListPage() {
    }

    public AutoListPage(WebSiteRequest req) {
	super(req);
    }

    public AutoListPage(Object param) {
	super(param);
    }

    @Override
    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        WebPageLayout layout=getWebPageLayout(req);
        layout.startContent(out, req, resp, 1, getPreferredContentWidth(req));
        layout.printContentTitle(out, req, resp, this, 1);
        layout.printContentHorizontalDivider(out, req, resp, 1, false);
        layout.startContentLine(out, req, resp, 1, null);
        printContentStart(out, req, resp);
        try {
            out.print("      <table cellpadding='0' cellspacing='10'>\n");
            printPageList(out, req, resp, this, layout);
            out.print("      </table>\n");
        } finally {
            layout.endContentLine(out, req, resp, 1, false);
            layout.endContent(this, out, req, resp, 1);
        }
    }

    /**
     * Prints the content that will be put before the auto-generated list.
     */
    public void printContentStart(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
    }
        
    /**
     * Prints a list of pages.
     */
    public static void printPageList(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage[] pages, WebPageLayout layout) throws IOException, SQLException {
        int len = pages.length;
        for (int c = 0; c < len; c++) {
            WebPage page = pages[c];
            out.print("  <tr>\n"
                    + "    <td nowrap><a class='aoLightLink' href='").print(req==null?"":resp.encodeURL(req.getURL(page))).print("'>").print(page.getShortTitle()).print("</a>\n"
                    + "    </td>\n"
                    + "    <td width=12 nowrap>&#160;</td>\n"
                    + "    <td nowrap>").print(page.getDescription()).print("</td>\n"
                    + "  </tr>\n")
            ;
        }
    }

    /**
     * Prints an unordered list of the available pages.
     */
    public static void printPageList(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage parent, WebPageLayout layout) throws IOException, SQLException {
        printPageList(out, req, resp, parent.getCachedPages(req), layout);
    }

    @Override
    public Object getOutputCacheKey(WebSiteRequest req) {
        return req.getOutputCacheKey();
    }
}