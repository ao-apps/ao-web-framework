package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 2200 Dogwood Ct N, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;

/**
 * Automatically builds a site map of the entire site.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AutoSiteMap extends TreePage {

    public AutoSiteMap() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoSiteMap.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public AutoSiteMap(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoSiteMap.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public AutoSiteMap(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, AutoSiteMap.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    /**
     * Recursively builds the list of all sites.
     */
    private void buildData(String path, WebPage page, List<TreePageData> data, WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.UNKNOWN, AutoSiteMap.class, "buildData(String,List<TreePageData>,WebSiteRequest)", null);
        try {
            if(isVisible(page)) {
                if(path.length()>0) path=path+'/'+page.getShortTitle();
                else path=page.getShortTitle();
                WebPage[] pages=page.getCachedPages(req);
                int len=pages.length;
                data.add(
                    new TreePageData(
                        len>0 ? (path+'/') : path,
                        req.getURL(page),
                        page.getDescription()
                    )
                );
                for(int c=0; c<len; c++) buildData(path, pages[c], data, req);
            }
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * The content of this page will not be included in the interal search engine.
     */
    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, AutoSiteMap.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            if(req!=null) super.doGet(out, req, resp);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    final protected List<TreePageData> getTree(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.UNKNOWN, AutoSiteMap.class, "getTree(WebSiteRequest)", null);
        try {
            WebPage home=getRootPage();
            List<TreePageData> data=new ArrayList<TreePageData>();
            buildData("", home, data, req);
            int size=data.size();
            return data;
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Determines if a page should be visible in the generated maps.
     */
    abstract protected boolean isVisible(WebPage page);
}
