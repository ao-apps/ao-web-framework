package com.aoindustries.website.framework;

/*
 * Copyright 2005-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Automatically generates the description along with a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
abstract public class DescriptionAutoListPage extends AutoListPage {

    public DescriptionAutoListPage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, DescriptionAutoListPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public DescriptionAutoListPage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, DescriptionAutoListPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public DescriptionAutoListPage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, DescriptionAutoListPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    /**
     * Prints the content that will be put before the auto-generated list.
     */
    public void printContentStart(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.INSTANTANEOUS, DescriptionAutoListPage.class, "printContentStart(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            out.print(getDescription());
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
}
