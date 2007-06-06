package com.aoindustries.website.framework;

/*
 * Copyright 2000-2007 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
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
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoListPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public AutoListPage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoListPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public AutoListPage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoListPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, AutoListPage.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            WebPageLayout layout=getWebPageLayout(req);
            layout.startContent(out, req, resp, 1, getPreferredContentWidth(req));
            layout.printContentTitle(out, req, resp, this, 1);
            layout.printContentHorizontalDivider(out, req, resp, 1, false);
            layout.startContentLine(out, req, resp, 1, null);
            printContentStart(out, req, resp);
            try {
                out.print("      <table cellpadding=0 cellspacing=10 border=0>\n");
                printPageList(out, req, resp, this, layout);
                out.print("      </table>\n");
            } finally {
                layout.endContentLine(out, req, resp, 1, false);
                layout.endContent(this, out, req, resp, 1);
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
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
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoListPage.class, "printContentStart(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }
        
    /**
     * Prints a list of pages.
     */
    public static void printPageList(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage[] pages, WebPageLayout layout) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, AutoListPage.class, "printPageList(ChainWriter,WebSiteRequest,HttpServletResponse,WebPage[],WebPageLayout)", null);
        try {
            int len = pages.length;
            for (int c = 0; c < len; c++) {
                WebPage page = pages[c];
                out.print("  <TR>\n"
                        + "    <TD nowrap><A class='ao_light_link' href='").writeHtmlAttribute(req==null?"":resp.encodeURL(req.getURL(page))).print("'>").print(page.getShortTitle()).print("</A>\n"
                        + "    </TD>\n"
                        + "    <TD width=12 nowrap>&nbsp;</TD>\n"
                        + "    <TD nowrap>").print(page.getDescription()).print("</TD>\n"
                        + "  </TR>\n")
                ;
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    /**
     * Prints an unordered list of the available pages.
     */
    public static void printPageList(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage parent, WebPageLayout layout) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, AutoListPage.class, "printPageList(ChainWriter,WebSiteRequest,HttpServletResponse,WebPageLayout)", null);
        try {
            printPageList(out, req, resp, parent.getCachedPages(req), layout);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public Object getOutputCacheKey(WebSiteRequest req) {
        Profiler.startProfile(Profiler.FAST, AutoListPage.class, "getOutputCacheKey(WebSiteRequest)", null);
        try {
            return req.getOutputCacheKey();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}