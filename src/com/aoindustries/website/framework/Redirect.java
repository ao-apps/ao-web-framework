package com.aoindustries.website.framework;

/*
 * Copyright 2004-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
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

    @Override
    protected void reportingDoGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        resp.sendRedirect(resp.encodeRedirectURL((req.isSecure()?WebSiteFrameworkConfiguration.getHttpsBase():WebSiteFrameworkConfiguration.getHttpBase())+getServletConfig().getInitParameter("url")));
    }

    @Override
    protected void reportingDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        resp.sendRedirect(resp.encodeRedirectURL((req.isSecure()?WebSiteFrameworkConfiguration.getHttpsBase():WebSiteFrameworkConfiguration.getHttpBase())+getServletConfig().getInitParameter("url")));
    }
}
