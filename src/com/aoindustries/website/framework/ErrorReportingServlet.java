package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 2200 Dogwood Ct N, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.*;
import com.aoindustries.email.*;
import com.aoindustries.sql.*;
import com.aoindustries.util.*;
import java.io.*;
import java.security.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Any error occuring during servlet execution is reported to <code>System.err</code>.
 * Also keeps track of hit stastistics for doGet, doPost, and getLastModified methods.
 *
 * @author  AO Industries, Inc.
 */
public abstract class ErrorReportingServlet extends HttpServlet {

    /**
     * The time that the servlet environment started.
     */
    private static long uptime=System.currentTimeMillis();
    
    /**
     * Gets the time the servlet environment was loaded.
     */
    public static long getUptime() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, ErrorReportingServlet.class, "getUptime()", null);
        try {
            return uptime;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
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

    /**
     * Any error that occurs during a <code>doGet</code> is caught and reported here.
     */
    final protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "doGet(HttpServletRequest,HttpServletResponse)", null);
        try {
            synchronized (getLock) {
                getCount++;
            }
            try {
                reportingDoGet(req, resp);
            } catch (ThreadDeath t) {
                throw t;
            } catch (RuntimeException e) {
                log(null, e);
                throw e;
            } catch (ServletException e) {
                log(null, e);
                throw e;
            } catch (IOException e) {
                log(null, e);
                throw e;
            } catch (Throwable t) {
                log(null, t);
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Any error that occurs during a <code>doPost</code> is caught and reported here.
     */
    final protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "doPost(HttpServletRequest,HttpServletResponse)", null);
        try {
            synchronized (postLock) {
                postCount++;
            }
            try {
                reportingDoPost(req, resp);
            } catch (ThreadDeath t) {
                throw t;
            } catch (RuntimeException e) {
                log(null, e);
                throw e;
            } catch (ServletException e) {
                log(null, e);
                throw e;
            } catch (IOException e) {
                log(null, e);
                throw e;
            } catch (Throwable t) {
                log(null, t);
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the number of GET requests that have been placed.
     */
    public static long getGetCount() {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "getGetCount()", null);
        try {
            synchronized (getLock) {
                return getCount;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Any error that occurs during a <code>getLastModified</code> call is caught here.
     */
    final protected long getLastModified(HttpServletRequest req) {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "getLastModified(HttpServletRequest)", null);
        try {
            synchronized (lastModifiedLock) {
                lastModifiedCount++;
            }
            try {
                return reportingGetLastModified(req);
            } catch (RuntimeException err) {
                log(null, err);
                throw err;
            } catch (ThreadDeath err) {
                throw err;
            } catch (Throwable err) {
                log(null, err);
                throw new WrappedException(err, new Object[] {"req="+req});
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the number of calls to <code>getLastModified</code>.
     */
    public static long getLastModifiedCount() {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "getLastModifiedCount()", null);
        try {
            synchronized (lastModifiedLock) {
                return lastModifiedCount;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the number of POST requests that have been made.
     */
    public static long getPostCount() {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "getPostCount()", null);
        try {
            synchronized(postLock) {
                return postCount;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest,HttpServletResponse)
     */
    protected void reportingDoGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "reportingDoGet(HttpServletRequest,HttpServletResponse)", null);
        try {
            super.doGet(req, resp);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest,HttpServletResponse)
     */
    protected void reportingDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "reportingDoPost(HttpServletRequest,HttpServletResponse)", null);
        try {
            super.doPost(req, resp);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)
     */
    protected long reportingGetLastModified(HttpServletRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "reportingGetLastModified(HttpServletRequest)", null);
        try {
            return super.getLastModified(req);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public void log(String message) {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "log(String)", null);
        try {
            log(getServletContext(), message, null, null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public void log(String message, Throwable err) {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "log(String,Throwable)", null);
        try {
            log(getServletContext(), message, err, null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public void log(String message, Throwable err, Object[] extraInfo) {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "log(String,Throwable,Object[])", null);
        try {
            log(getServletContext(), message, err, extraInfo);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static void log(ServletContext context, String message) {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "log(ServletContext,String)", null);
        try {
            log(context, message, null, null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static void log(ServletContext context, String message, Throwable err, Object[] extraInfo) {
        Profiler.startProfile(Profiler.IO, ErrorReportingServlet.class, "log(ServletContext,String,Throwable,Object[])", null);
        try {
            log0(context, message, err, extraInfo);

            // Optionally email the error
            try {
                String smtp=WebSiteFrameworkConfiguration.getErrorSmtpServer();
                if(smtp!=null && smtp.length()>0) {
                    String from=WebSiteFrameworkConfiguration.getErrorFromAddress();
                    List<String> tos=WebSiteFrameworkConfiguration.getErrorToAddresses();
                    for(int c=0;c<tos.size();c++) {
                        String to=tos.get(c);
                        ErrorMailer.emailError(
                            getRandom(),
                            message==null
                                ?(err==null?"null":err.getMessage())
                                :(err==null?message:(message+" - "+err.getMessage())),
                            smtp,
                            from,
                            to,
                            WebSiteFrameworkConfiguration.getErrorSubject()
                        );
                    }
                }
            } catch(IOException ioErr) {
                log0(context, "Unable to email error", ioErr, null);
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    /**
     * Errors are logged one at a time.
     */
    private static final Object logLock=new Object();

    private static void log0(ServletContext context, String message, Throwable err, Object[] extraInfo) {
        Profiler.startProfile(Profiler.IO, ErrorReportingServlet.class, "log0(ServletContext,String,Throwable,Object[])", null);
        try {
            try {
                synchronized(logLock) {
                    if(context==null || WebSiteFrameworkConfiguration.getLogToSystemErr()) {
                        if(message!=null) System.err.println(message);
                        if(err!=null) ErrorPrinter.printStackTraces(err, extraInfo);
                        System.err.flush();
                    } else context.log(message, err);
                }
            } catch(IOException err2) {
                synchronized(logLock) {
                    if(err!=null) ErrorPrinter.printStackTraces(err, extraInfo);
                    System.err.println("Unable to access web site framework configuration: log_to_system_err");
                    ErrorPrinter.printStackTraces(err2);
                    System.err.flush();
                }
                if(context!=null) {
                    context.log(message, err);
                    context.log("Unable to access web site framework configuration: log_to_system_err", err2);
                }
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    private static ErrorHandler errorHandler;
    public synchronized static ErrorHandler getErrorHandler() {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "getErrorHandler()", null);
        try {
            if(errorHandler==null) {
                errorHandler=new ErrorHandler() {
                    public final void reportError(Throwable T, Object[] extraInfo) {
                        ErrorReportingServlet.log(null, null, T, extraInfo);
                    }

                    public final void reportWarning(Throwable T, Object[] extraInfo) {
                        ErrorReportingServlet.log(null, null, T, extraInfo);
                    }
                };
            }
            return errorHandler;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    private static final Object randomLock=new Object();
    private static Random random;

    /**
     * @see  WebSiteRequest#getRandom()
     */
    static Random getRandom() {
        Profiler.startProfile(Profiler.FAST, ErrorReportingServlet.class, "getRandom()", null);
        try {
	    synchronized(randomLock) {
                String algorithm="SHA1PRNG";
		try {
		    if(random==null) random=SecureRandom.getInstance(algorithm);
		    return random;
		} catch(NoSuchAlgorithmException err) {
		    throw new WrappedException(err, new Object[] {"algorithm="+algorithm});
		}
	    }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}
