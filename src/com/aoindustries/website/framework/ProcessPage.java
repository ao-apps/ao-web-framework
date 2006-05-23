package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 2200 Dogwood Ct N, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
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
        Profiler.startProfile(Profiler.INSTANTANEOUS, ProcessPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public ProcessPage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, ProcessPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public ProcessPage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, ProcessPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public InputStream getInputStream() throws IOException {
        Profiler.startProfile(Profiler.FAST, ProcessPage.class, "getInputStream()", null);
        try {
            return getProcess().getInputStream();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public long getLastModified(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, ProcessPage.class, "getLastModified(WebSiteRequest)", null);
        try {
            return -1;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the process that the contents should be read from
     */
    public abstract Process getProcess() throws IOException;

    /**
     * The search format of this page is indexed.
     */
    public long getSearchLastModified() throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, ProcessPage.class, "getSearchLastModified()", null);
        try {
            return super.getLastModified(null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}