package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.email.ErrorMailer;
import com.aoindustries.util.ErrorHandler;
import com.aoindustries.util.ErrorPrinter;
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import javax.servlet.ServletContext;
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

    /**
     * Any error that occurs during a <code>doGet</code> is caught and reported here.
     */
    @Override
    final protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
    }

    /**
     * Any error that occurs during a <code>doPost</code> is caught and reported here.
     */
    @Override
    final protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            log(null, err);
            throw err;
        } catch (ThreadDeath err) {
            throw err;
        } catch (Throwable err) {
            log(null, err);
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

    @Override
    public void log(String message) {
        log(getServletContext(), message, null, null);
    }
    
    @Override
    public void log(String message, Throwable err) {
        log(getServletContext(), message, err, null);
    }

    public void log(String message, Throwable err, Object[] extraInfo) {
        log(getServletContext(), message, err, extraInfo);
    }

    public static void log(ServletContext context, String message) {
        log(context, message, null, null);
    }

    public static void log(ServletContext context, String message, Throwable err, Object[] extraInfo) {
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
    }

    /**
     * Errors are logged one at a time.
     */
    private static final Object logLock=new Object();

    private static void log0(ServletContext context, String message, Throwable err, Object[] extraInfo) {
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
    }

    private static ErrorHandler errorHandler;
    public synchronized static ErrorHandler getErrorHandler() {
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
    }

    private static final Random random = new SecureRandom();

    /**
     * @see  WebSiteRequest#getRandom()
     */
    static Random getRandom() {
        return random;
    }
}
