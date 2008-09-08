package com.aoindustries.website.framework;

/*
 * Copyright 2000-2008 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Takes the output of a native process and puts it in a PRE block
 *
 * @author  AO Industries, Inc.
 */
public abstract class PreProcessPage extends ProcessPage {

    public PreProcessPage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, PreProcessPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public PreProcessPage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, PreProcessPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public PreProcessPage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, PreProcessPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, PreProcessPage.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            out.println("<pre>");
            super.doGet(out, req, resp);
            out.println("</pre>");
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }
}
