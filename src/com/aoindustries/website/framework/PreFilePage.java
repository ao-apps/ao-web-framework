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
 * Pulls the page contents from a file while wrapping it with a PRE block.
 *
 * @author  AO Industries, Inc.
 */
public abstract class PreFilePage extends FilePage {

    public PreFilePage() {
    }

    public PreFilePage(WebSiteRequest req) {
	super(req);
    }

    public PreFilePage(Object param) {
	super(param);
    }

    @Override
    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        out.println("<pre>");
        super.doGet(out, req, resp);
        out.println("</pre>");
    }
}
