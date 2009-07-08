package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Any error occuring during servlet execution is reported to <code>System.err</code>.
 * Also keeps track of hit stastistics for doGet, doPost, and getLastModified methods.
 *
 * @author  AO Industries, Inc.
 */
public abstract class ErrorReportingServlet extends HttpServlet {

    /**
     * The response buffer is set to this size.
     */
    public static final int BUFFER_SIZE = 256 * 1024;
    
    /**
     * The time that the servlet environment started.
     */
    private static long uptime=System.currentTimeMillis();
    
    /**
     * Gets the time the servlet environment was loaded.
     */
    public static long getUptime() {
        return uptime;
    }

    /**
     * The GET requests are counted in a thread safe way.
     */
    private static final Object getLock=new Object();
    private static long getCount=0;

    /**
     * The POST requests are counted in a thread safe way.
     */
    private static final Object postLock=new Object();
    private static long postCount=0;

    /**
     * The getLastModified calls are counted in a thread safe way.
     */
    private static final Object lastModifiedLock=new Object();
    private static long lastModifiedCount=0;

    private final LoggerAccessor loggerAccessor;

    protected ErrorReportingServlet(LoggerAccessor loggerAccessor) {
        this.loggerAccessor = loggerAccessor;
    }

    /**
     * Gets the loggerAccess for this page.
     */
    protected LoggerAccessor getLoggerAccessor() {
        return loggerAccessor;
    }

    /**
     * Gets the logger for this servlet.
     */
    public Logger getLogger() {
        return getLogger(getClass());
    }

    /**
     * Gets the logger for the provided class.
     */
    protected Logger getLogger(Class clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Gets the provided named logger.
     */
    protected Logger getLogger(String name) {
        return loggerAccessor.getLogger(getServletContext(), name);
    }

    /**
     * Any error that occurs during a <code>doGet</code> is caught and reported here.
     */
    @Override
    final protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setBufferSize(BUFFER_SIZE);
        synchronized (getLock) {
            getCount++;
        }
        try {
            reportingDoGet(req, resp);
        } catch (ThreadDeath t) {
            throw t;
        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, null, e);
            throw e;
        } catch (ServletException e) {
            getLogger().log(Level.SEVERE, null, e);
            throw e;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, null, e);
            throw e;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, null, t);
        }
    }

    /**
     * Any error that occurs during a <code>doPost</code> is caught and reported here.
     */
    @Override
    final protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setBufferSize(BUFFER_SIZE);
        synchronized (postLock) {
            postCount++;
        }
        try {
            reportingDoPost(req, resp);
        } catch (ThreadDeath t) {
            throw t;
        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, null, e);
            throw e;
        } catch (ServletException e) {
            getLogger().log(Level.SEVERE, null, e);
            throw e;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, null, e);
            throw e;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, null, t);
        }
    }

    /**
     * Gets the number of GET requests that have been placed.
     */
    public static long getGetCount() {
        synchronized (getLock) {
            return getCount;
        }
    }

    /**
     * Any error that occurs during a <code>getLastModified</code> call is caught here.
     */
    @Override
    final protected long getLastModified(HttpServletRequest req) {
        synchronized (lastModifiedLock) {
            lastModifiedCount++;
        }
        try {
            return reportingGetLastModified(req);
        } catch (RuntimeException err) {
            getLogger().log(Level.SEVERE, null, err);
            throw err;
        } catch (ThreadDeath err) {
            throw err;
        } catch (Throwable err) {
            getLogger().log(Level.SEVERE, null, err);
            throw new WrappedException(err, new Object[] {"req="+req});
        }
    }

    /**
     * Gets the number of calls to <code>getLastModified</code>.
     */
    public static long getLastModifiedCount() {
        synchronized (lastModifiedLock) {
            return lastModifiedCount;
        }
    }

    /**
     * Gets the number of POST requests that have been made.
     */
    public static long getPostCount() {
        synchronized(postLock) {
            return postCount;
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest,HttpServletResponse)
     */
    protected void reportingDoGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        super.doGet(req, resp);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest,HttpServletResponse)
     */
    protected void reportingDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        super.doPost(req, resp);
    }

    /**
     * @see javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)
     */
    protected long reportingGetLastModified(HttpServletRequest req) throws IOException, SQLException {
        return super.getLastModified(req);
    }

    /**
     * @deprecated  Please call logger directly for accurate class and method
     */
    @Override
    @Deprecated
    final public void log(String message) {
        getLogger().log(Level.SEVERE, message);
    }

    /**
     * @deprecated  Please call logger directly for accurate class and method
     */
    @Override
    @Deprecated
    final public void log(String message, Throwable err) {
        getLogger().log(Level.SEVERE, message, err);
    }

    private static final Random random = new SecureRandom();

    /**
     * @see  WebSiteRequest#getRandom()
     */
    static Random getRandom() {
        return random;
    }
}
