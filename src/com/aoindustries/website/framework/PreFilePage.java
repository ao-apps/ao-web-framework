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
 * Pulls the page contents from a file while wrapping it with a PRE block.
 *
 * @author  AO Industries, Inc.
 */
public abstract class PreFilePage extends FilePage {

    public PreFilePage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, PreFilePage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public PreFilePage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, PreFilePage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public PreFilePage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, PreFilePage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, PreFilePage.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            out.println("<pre>");
            super.doGet(out, req, resp);
            out.println("</pre>");
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }
}
