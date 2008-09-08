package com.aoindustries.website.framework;

/*
 * Copyright 2004-2008 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.*;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Redirects to the configured URL.
 *
 * @author  AO Industries, Inc.
 */
public class Redirect extends ErrorReportingServlet {

    protected void reportingDoGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        Profiler.startProfile(Profiler.FAST, Redirect.class, "reportingDoGet(HttpServletRequest,HttpServletResponse)", null);
        try {
            resp.sendRedirect(resp.encodeRedirectURL((req.isSecure()?WebSiteFrameworkConfiguration.getHttpsBase():WebSiteFrameworkConfiguration.getHttpBase())+getServletConfig().getInitParameter("url")));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    protected void reportingDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        Profiler.startProfile(Profiler.FAST, Redirect.class, "reportingDoPost(HttpServletRequest,HttpServletResponse)", null);
        try {
            resp.sendRedirect(resp.encodeRedirectURL((req.isSecure()?WebSiteFrameworkConfiguration.getHttpsBase():WebSiteFrameworkConfiguration.getHttpBase())+getServletConfig().getInitParameter("url")));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}
