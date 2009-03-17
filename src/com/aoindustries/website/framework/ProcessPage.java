package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Pulls information from a native process to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class ProcessPage extends InputStreamPage {

    public ProcessPage() {
    }

    public ProcessPage(WebSiteRequest req) {
	super(req);
    }

    public ProcessPage(Object param) {
	super(param);
    }

    public InputStream getInputStream() throws IOException {
        return getProcess().getInputStream();
    }

    @Override
    public long getLastModified(WebSiteRequest req) {
        return -1;
    }

    /**
     * Gets the process that the contents should be read from
     */
    public abstract Process getProcess() throws IOException;

    /**
     * The search format of this page is indexed.
     */
    @Override
    public long getSearchLastModified() throws IOException, SQLException {
        return super.getLastModified(null);
    }
}