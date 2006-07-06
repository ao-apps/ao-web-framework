package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import com.aoindustries.security.*;
import com.aoindustries.util.*;
import com.oreilly.servlet.*;
import com.oreilly.servlet.multipart.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * A <code>WebSiteSettings</code> contains all the values that a user may customize while they view the web site.
 *
 * @author  AO Industries, Inc.
 */
abstract public class WebSiteRequest implements HttpServletRequest, FileRenamePolicy {

    /**
     * Gets the random number generator used for this request.
     */
    public Random getRandom() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getRandom()", null);
        try {
            return ErrorReportingServlet.getRandom();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    private static String getExtension(String filename) {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getExtension(String)", null);
        try {
            int pos=filename.lastIndexOf('.');
            if(pos==-1 || pos==(filename.length()-1)) return filename;
            else return filename.substring(pos+1);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    private static final Object mimeTypeLock=new Object();
    private static Map<String,String> mimeTypes;
    private static String getContentType(MultipartRequest mreq, String filename) throws IOException {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getContentType(MultipartRequest,String)", null);
        try {
            synchronized(mimeTypeLock) {
                if(mimeTypes==null) {
		    Map<String,String> newMap=new HashMap<String,String>();
                    BufferedReader in=new BufferedReader(new InputStreamReader(WebSiteRequest.class.getResourceAsStream("mime.types")));
		    try {
			String line;
			while((line=in.readLine())!=null) {
			    if(line.length()>0) {
				if(line.charAt(0)!='#') {
				    String[] words=StringUtility.splitString(line);
				    if(words.length>0) {
					String type=words[0];
					for(int c=1;c<words.length;c++) newMap.put(words[1], type);
				    }
				}
			    }
			}
		    } finally {
			in.close();
		    }
                    mimeTypes=newMap;
                }
                String type=mimeTypes.get(getExtension(filename).toLowerCase());
                if(type!=null) return type;
                return mreq.getContentType(filename);
            }
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    private static final Map<Long,UploadedFile> uploadedFiles=new HashMap<Long,UploadedFile>();
    private long getNextID() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getNextID()", null);
        try {
            Random random=getRandom();
            synchronized(uploadedFiles) {
                while(true) {
                    long id=random.nextLong()&0x7fffffffffffffffL;
                    Long ID=Long.valueOf(id);
                    if(!uploadedFiles.containsKey(ID)) {
                        uploadedFiles.put(ID, null);
                        return id;
                    }
                }
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    private static Thread uploadedFileCleanup;
    private static void addUploadedFile(UploadedFile uf) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "addUploadedFile(UploadedFile)", null);
        try {
            synchronized(uploadedFiles) {
                uploadedFiles.put(Long.valueOf(uf.getID()), uf);

                if(uploadedFileCleanup==null) {
                    uploadedFileCleanup=new Thread() {
                        public void run() {
                            while(true) {
                                try {
                                    while(true) {
                                        sleep(10*60*1000);

                                        // Remove the expired entries
                                        synchronized(uploadedFiles) {
                                            Iterator<Long> I=uploadedFiles.keySet().iterator();
                                            while(I.hasNext()) {
                                                Long ID=I.next();
                                                UploadedFile uf=uploadedFiles.get(ID);
                                                if(uf!=null) {
                                                    I.remove();
                                                } else {
                                                    long timeSince=System.currentTimeMillis()-uf.getLastAccessed();
                                                    if(timeSince<0 || timeSince>=((long)60*60*1000)) {
                                                        File file=uf.getStorageFile();
                                                        if(file.exists() && !file.delete()) ErrorReportingServlet.log(null, null, new IOException("Unable to delete file"), new Object[] {"file.getPath()="+file.getPath()});
                                                        I.remove();
                                                    }
                                                }
                                            }
                                            // Delete the files that do not have an uploadedFile entry and are at least two hours old
                                            File dir=WebSiteFrameworkConfiguration.getFileUploadDirectory();
                                            String[] list=dir.list();
                                            if(list!=null) {
                                                for(int c=0;c<list.length;c++) {
                                                    File file=new File(dir, list[c]);
                                                    long fileAge=System.currentTimeMillis()-file.lastModified();
                                                    if(fileAge<((long)-2*60*60*1000) || fileAge>((long)2*60*60*1000)) {
                                                        boolean found=false;
                                                        I=uploadedFiles.keySet().iterator();
                                                        while(I.hasNext()) {
                                                            UploadedFile uf=uploadedFiles.get(I.next());
                                                            if(uf.getStorageFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                                                                found=true;
                                                                break;
                                                            }
                                                        }
                                                        if(!found && !file.delete()) ErrorReportingServlet.log(null, null, new IOException("Unable to delete file"), new Object[] {"file.getPath()="+file.getPath()});
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch(ThreadDeath TD) {
                                    throw TD;
                                } catch(Throwable T) {
                                    ErrorReportingServlet.log(null, null, T, null);
                                    try {
                                        sleep(60*1000);
                                    } catch(InterruptedException err) {
                                        ErrorReportingServlet.log(null, null, err, null);
                                    }
                                }
                            }
                        }
                    };
                    uploadedFileCleanup.start();
                }
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    final protected HttpServlet sourcePage;
    final private HttpServletRequest req;
    private MultipartRequest mreq;
    private List<UploadedFile> reqUploadedFiles;

    private int fontSize=-1;

    private boolean isLynx;
    private boolean isLynxDone;

    private boolean isLinux;
    private boolean isLinuxDone;

    private boolean isNetscape;
    private boolean isNetscapeDone;

    private boolean isNetscape4;
    private boolean isNetscape4Done;

    private boolean isSearchEngine;
    private boolean isSearchEngineDone;

    private boolean isFramed;

    public WebSiteRequest(HttpServlet sourcePage, HttpServletRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "<init>(HttpServlet,HttpServletRequest)", null);
        try {
            this.sourcePage=sourcePage;
            this.req=req;
            String contentType=req.getHeader("Content-Type");
            if (contentType!=null && contentType.length()>=19 && contentType.substring(0,19).equals("multipart/form-data")) {
                boolean keepFiles=false;
                try {
                    mreq = new MultipartRequest(req, WebSiteFrameworkConfiguration.getFileUploadDirectory().getPath(), WebSiteFrameworkConfiguration.getMaxFileUploadSize(), this);
                    try {
                        // Determine the authentication info
                        WebSiteUser user=getWebSiteUser();
                        if(user!=null) {
                            keepFiles=true;
                            // Create an UploadedFile for each file in the MultipartRequest
                            reqUploadedFiles=new ArrayList<UploadedFile>();
                            Enumeration E=mreq.getFileNames();
                            while(E.hasMoreElements()) {
                                String filename=(String)E.nextElement();
                                File file=mreq.getFile(filename);
                                if(file!=null) {
                                    file.deleteOnExit();
                                    UploadedFile uf=new UploadedFile(
                                        mreq.getOriginalFileName(filename),
                                        file,
                                        user,
                                        getContentType(mreq, filename)
                                    );
                                    addUploadedFile(uf);
                                    reqUploadedFiles.add(uf);
                                }
                            }
                        }
                    } catch(LoginException err) {
                        // Ignore the error, just allow the files to be cleaned up because keepFiles is still false
                    }
                } finally {
                    if(!keepFiles && mreq!=null) {
                        Enumeration E=mreq.getFileNames();
                        while(E.hasMoreElements()) {
                            String filename=(String)E.nextElement();
                            File file=mreq.getFile(filename);
                            if(file!=null && file.exists() && !file.delete()) throw new IOException("Unable to delete file: "+file.getPath());
                        }
                    }
                }
            } else this.mreq = null;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Appends the parameters to a URL.
     */
    protected static boolean appendParams(StringBuilder SB, Object optParam, List<String> finishedParams, boolean alreadyAppended) {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "appendParams(StringBuilder,Object,List<String>,boolean)", null);
        try {
            if (optParam != null) {
                if (optParam instanceof String) {
                    String[] SA=StringUtility.splitString((String)optParam, '&');
                    int len=SA.length;
                    for(int c=0;c<len;c++) {
                        SB.append(alreadyAppended?'&':'?');
                        String S=SA[c];
                        int pos=S.indexOf('=');
                        if(pos==-1) {
                            SB.append(S);
                            alreadyAppended=true;
                        } else {
                            String name=S.substring(0, pos);
                            if(!finishedParams.contains(name)) {
                                SB.append(S);
                                finishedParams.add(name);
                                alreadyAppended=true;
                            }
                        }
                    }
                } else if (optParam instanceof String[]) {
                    String[] SA = (String[]) optParam;
                    int len = SA.length;
                    for (int c = 0; c < len; c += 2) {
                        String name=SA[c];
                        if(!finishedParams.contains(name)) {
                            SB.append(alreadyAppended?'&':'?').append(name).append('=').append(SA[c + 1]);
                            finishedParams.add(name);
                            alreadyAppended=true;
                        }
                    }
                } else throw new IllegalArgumentException("Unsupported type for optParam: " + optParam.getClass().getName());
            }
            return alreadyAppended;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets a relative URL from a String containing a classname and optional parameters.
     */
    public String getURL(String classAndParams) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(String)", null);
        try {
            String className, params;
            int pos=classAndParams.indexOf('?');
            if(pos==-1) {
                className=classAndParams;
                params=null;
            } else {
                className=classAndParams.substring(0, pos);
                params=classAndParams.substring(pos+1);
            }
            return getURL(className, params);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets a relative URL given its classname and optional parameters.
     */
    public String getURL(String classname, String params) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(String,String)", null);
        try {
            try {
                Class clazz=Class.forName(classname);
                return getURL(clazz, params);
            } catch(ClassNotFoundException err) {
                IOException ioErr=new IOException("Unable to load class: "+classname);
                ioErr.initCause(err);
                throw ioErr;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the absolute URL String, optionally with the settings embedded.
     */
    public String getURL(String url, boolean useEncryption, Object optParam, boolean keepSettings) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(String,boolean,Object,boolean)", null);
        try {
            StringBuilder SB=new StringBuilder();
            SB.append(
                useEncryption
                ?WebSiteFrameworkConfiguration.getHttpsBase()
                :WebSiteFrameworkConfiguration.getHttpBase()
            );
            SB.append(url);
            List<String> finishedParams=new SortedArrayList<String>();
            boolean alreadyAppended=appendParams(SB, optParam, finishedParams, false);
            if(keepSettings) appendSettings(finishedParams, alreadyAppended, SB);
            return SB.toString();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    protected boolean appendSettings(List<String> finishedParams, boolean alreadyAppended, StringBuilder SB) {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "appendSettings(List<String>,boolean,StringBuilder)", null);
        try {
            boolean searchEngine="true".equals(getParameter("search_engine"));
            if(searchEngine) alreadyAppended=appendParams(SB, new String[] {"search_engine", "true"}, finishedParams, alreadyAppended);
            String layout=getLayoutName();
            if(layout!=null && (!layout.equalsIgnoreCase("default") || isLynx())) alreadyAppended=appendParams(SB, new String[] {"layout", layout}, finishedParams, alreadyAppended);
            return alreadyAppended;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public Object getAttribute(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getAttribute(String)", null);
        try {
            return req.getAttribute(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Enumeration getAttributeNames() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getAttributeNames()", null);
        try {
            return req.getAttributeNames();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getAuthType() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getAuthType()", null);
        try {
            return req.getAuthType();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getCharacterEncoding() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getCharacterEncoding()", null);
        try {
            return req.getCharacterEncoding();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public int getContentLength() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getContentLength()", null);
        try {
            return req.getContentLength();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getContentType() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getContentType()", null);
        try {
            return req.getContentType();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getContextPath() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getContextPath()", null);
        try {
            return req.getContextPath();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Cookie[] getCookies() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getCookies()", null);
        try {
            return req.getCookies();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public long getDateHeader(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getDateHeader(String)", null);
        try {
            return req.getDateHeader(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getHeader(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getHeader(String)", null);
        try {
            return req.getHeader(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Enumeration getHeaderNames() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getHeaderNames()", null);
        try {
            return req.getHeaderNames();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Enumeration getHeaders(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getHeaders(String)", null);
        try {
            return req.getHeaders(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public ServletInputStream getInputStream() throws IOException {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getInputStream()", null);
        try {
            return req.getInputStream();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public int getIntHeader(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getIntHeader(String)", null);
        try {
            return req.getIntHeader(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Locale getLocale() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getLocale()", null);
        try {
            return req.getLocale();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Enumeration getLocales() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getLocales()", null);
        try {
            return req.getLocales();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Gets the local address or <code>null</code> if unavailable.
     */
    public String getLocalAddr() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getLocalAddr()", null);
        try {
            try {
                Method method=HttpServletRequest.class.getMethod("getLocalAddr", new Class[0]);
                if(method!=null) {
                    Object ret=method.invoke(req, new Object[0]);
                    if(ret!=null && (ret instanceof String)) {
                        return (String)ret;
                    }
                }
            } catch(NoSuchMethodException err) {
                // Normal if running in older version of servlet specifications
            } catch(IllegalAccessException err) {
                throw new WrappedException(err);
            } catch(InvocationTargetException err) {
                throw new WrappedException(err);
            }
            return null;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the local name or <code>null</code> if unavailable.
     */
    public String getLocalName() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getLocalName()", null);
        try {
            try {
                Method method=HttpServletRequest.class.getMethod("getLocalName", new Class[0]);
                if(method!=null) {
                    Object ret=method.invoke(req, new Object[0]);
                    if(ret!=null && (ret instanceof String)) {
                        return (String)ret;
                    }
                }
            } catch(NoSuchMethodException err) {
                // Normal if running in older version of servlet specifications
            } catch(IllegalAccessException err) {
                throw new WrappedException(err);
            } catch(InvocationTargetException err) {
                throw new WrappedException(err);
            }
            return null;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the local port or <code>-1</code> if unavailable.
     */
    public int getLocalPort() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getLocalPort()", null);
        try {
            try {
                Method method=HttpServletRequest.class.getMethod("getLocalPort", new Class[0]);
                if(method!=null) {
                    Object ret=method.invoke(req, new Object[0]);
                    if(ret!=null && (ret instanceof Integer)) {
                        return ((Integer)ret).intValue();
                    }
                }
            } catch(NoSuchMethodException err) {
                // Normal if running in older version of servlet specifications
            } catch(IllegalAccessException err) {
                throw new WrappedException(err);
            } catch(InvocationTargetException err) {
                throw new WrappedException(err);
            }
            return -1;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public String getMethod() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getMethod()", null);
        try {
            return req.getMethod();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getParameter(String name) {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getParameter(String)", null);
        try {
            if(mreq==null) return req.getParameter(name);
            else {
                String param=mreq.getParameter(name);
                if(param==null) param=req.getParameter(name);
                return param;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public Map getParameterMap() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getParameterMap()", null);
        try {
            return req.getParameterMap();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public Enumeration getParameterNames() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getParameterNames()", null);
        try {
            if (mreq==null) return req.getParameterNames();
            return mreq.getParameterNames();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String[] getParameterValues(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getParameterValues(String)", null);
        try {
            if (mreq==null) return req.getParameterValues(name);
            return mreq.getParameterValues(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getPathInfo() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getPathInfo()", null);
        try {
            return req.getPathInfo();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getPathTranslated() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getPathTranslated()", null);
        try {
            return req.getPathTranslated();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getProtocol() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getProtocol()", null);
        try {
            return req.getProtocol();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getQueryString() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getQueryString()", null);
        try {
            return req.getQueryString();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public BufferedReader getReader() throws IOException {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getReader()", null);
        try {
            return req.getReader();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * @deprecated
     *
     * @see  HttpServletRequest#getRealPath
     */
    public String getRealPath(String path) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRealPath(String)", null);
        try {
            return req.getRealPath(path);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getRemoteAddr() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRemoteAddr()", null);
        try {
            return req.getRemoteAddr();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getRemoteHost() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRemoteHost()", null);
        try {
            return req.getRemoteHost();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Gets the remote port or <code>-1</code> if unavailable.
     */
    public int getRemotePort() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getRemotePort()", null);
        try {
            try {
                Method method=HttpServletRequest.class.getMethod("getRemotePort", new Class[0]);
                if(method!=null) {
                    Object ret=method.invoke(req, new Object[0]);
                    if(ret!=null && (ret instanceof Integer)) {
                        return ((Integer)ret).intValue();
                    }
                }
            } catch(NoSuchMethodException err) {
                // Normal if running in older version of servlet specifications
            } catch(IllegalAccessException err) {
                throw new WrappedException(err);
            } catch(InvocationTargetException err) {
                throw new WrappedException(err);
            }
            return -1;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public String getRemoteUser() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRemoteUser()", null);
        try {
            return req.getRemoteUser();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRequestDispatcher(String)", null);
        try {
            return req.getRequestDispatcher(path);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getRequestedSessionId() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRequestedSessionId()", null);
        try {
            return req.getRequestedSessionId();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getRequestURI() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRequestURI()", null);
        try {
            return req.getRequestURI();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public StringBuffer getRequestURL() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getRequestURL()", null);
        try {
            return req.getRequestURL();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getScheme() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getScheme()", null);
        try {
            return req.getScheme();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getServerName() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getServerName()", null);
        try {
            return req.getServerName();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public int getServerPort() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getServerPort()", null);
        try {
            return req.getServerPort();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public String getServletPath() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getServletPath()", null);
        try {
            return req.getServletPath();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public HttpSession getSession() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getSession()", null);
        try {
            return req.getSession();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public HttpSession getSession(boolean create) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getSession(boolean)", null);
        try {
            return req.getSession(create);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Gets the absolute URL to a web page.
     */
    public String getURL(WebPage page) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(WebPage)", null);
        try {
            return getURL(page, page.useEncryption(), null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the absolute URL to a web page.
     */
    public String getURL(WebPage page, Object optParam) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(WebPage,Object)", null);
        try {
            return getURL(page, page.useEncryption(), optParam);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the absolute URL to a web page.
     */
    public String getURL(WebPage page, boolean useEncryption, Object optParam) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(WebPage,boolean,Object)", null);
        try {
            List<String> finishedParams=new SortedArrayList<String>();
            StringBuilder SB = new StringBuilder();
            SB.append(
                useEncryption
                ?WebSiteFrameworkConfiguration.getHttpsBase()
                :WebSiteFrameworkConfiguration.getHttpBase()
            );
            SB.append(page.getURLPath());
            boolean alreadyAppended=appendParams(SB, optParam, finishedParams, false);
            alreadyAppended=appendParams(SB, page.getURLParams(this), finishedParams, alreadyAppended);

            alreadyAppended=appendSettings(finishedParams, alreadyAppended, SB);

            return SB.toString();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the absolute URL to a web page.
     */
    public String getURL(Class clazz, Object param) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(Class,Object)", null);
        try {
            WebPage page=WebPage.getWebPage(sourcePage.getServletContext(), clazz, param);
            return getURL(page, page.useEncryption(), param);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public String getURL(Class clazz) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(Class)", null);
        try {
            return getURL(clazz, null);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the URL String with the given parameters embedded.
     *
     * @param  url            the URL from the top of the webapp
     * @param  useEncryption  if <code>true</code> the link refers to an page that should be served over encryption
     * @param  optParam       any number of additional parameters.  This parameter can accept several types of
     *                        objects.  The following is a list of supported objects and a brief description of its
     *                        behavior.
     *                        <ul>
     *                          <li>
     *                            <code>String</code> - appended to the end of the parameters, assumed to be in the
     *                            format name=value
     *                          </li>
     *                          <li>
     *                            <code>String[]</code> - name and value pairs, the first element of each pair is the
     *                            name, the second is the value
     *                          </li>
     *                        </ul>
     * @exception  IllegalArgumentException  if <code>optParam</code> is not a supported object
     */
    public String getURL(String url, boolean useEncryption, Object optParam) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getURL(String,boolean,Object)", null);
        try {
            return getURL(url, useEncryption, optParam, true);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public Principal getUserPrincipal() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "getUserPrincipal()", null);
        try {
            return req.getUserPrincipal();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public boolean isUsingFrames() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebSiteRequest.class, "isUsingFrames()", null);
        try {
            return isFramed;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the name of the currently selected WebPageLayout
     */
    public String getLayoutName() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getLayoutName()", null);
        try {
            String layout=req.getParameter("layout");
            if(layout==null || layout.length()==0) layout=null;
            return layout;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
        
    /**
     * Determines if the request is for a Lynx browser
     */
    public boolean isLynx() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "isLynx()", null);
        try {
            if(!isLynxDone) {
                String agent = req.getHeader("user-agent");
                isLynx=agent == null || agent.toLowerCase().indexOf("lynx") != -1;
                isLynxDone=true;
            }
            return isLynx;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Determines if the request is for a Linux browser
     */
    public boolean isLinux() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "isLinux()", null);
        try {
            if(!isLinuxDone) {
                String agent = req.getHeader("user-agent");
                isLinux=agent == null || agent.toLowerCase().indexOf("linux") != -1;
                isLinuxDone=true;
            }
            return isLinux;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Determines if the request is for a Netscape browser.
     */
    public boolean isNetscape() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "isNetscape()", null);
        try {
            if(!isNetscapeDone) {
                String agent=req.getHeader("user-agent");
                isNetscape=agent!=null && agent.indexOf("Mozilla")!=-1 && agent.indexOf("MSIE")==-1;
                isNetscapeDone=true;
            }
            return isNetscape;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Determines if the request is for a Netscape browser.
     */
    public boolean isNetscape4() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "isNetscape4()", null);
        try {
            if(!isNetscape4Done) {
                String agent=req.getHeader("user-agent");
                isNetscape4=agent!=null && agent.indexOf("Mozilla/4")!=-1 && agent.indexOf("MSIE")==-1;
                isNetscape4Done=true;
            }
            return isNetscape4;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public boolean isRequestedSessionIdFromCookie() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "isRequestedSessionIdFromCookie()", null);
        try {
            return req.isRequestedSessionIdFromCookie();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * @deprecated
     *
     * @see  HttpServletRequest#isRequestedSessionIdFromUrl
     */
    public boolean isRequestedSessionIdFromUrl() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "isRequestedSessionIdFromUrl()", null);
        try {
            return req.isRequestedSessionIdFromUrl();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public boolean isRequestedSessionIdFromURL() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "isRequestedSessionIdFromURL()", null);
        try {
            return req.isRequestedSessionIdFromURL();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public boolean isRequestedSessionIdValid() {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "isRequestedSessionIdValid()", null);
        try {
            return req.isRequestedSessionIdValid();
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Determines if the request is for a search engine.
     */
    public boolean isSearchEngine() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "isSearchEngine()", null);
        try {
            if(!isSearchEngineDone) {
                if("true".equals(getParameter("search_engine"))) {
                    isSearchEngine=true;
                    isSearchEngineDone=true;
                    return true;
                }
                String agent = req.getHeader("user-agent");
                if (agent == null) isSearchEngine=true;
                else {
                    agent = agent.toLowerCase();
                    isSearchEngine=
                        agent.indexOf("mozilla") == -1
                        && agent.indexOf("msie") == -1
                        && agent.indexOf("lynx") == -1
                    ;
                }
                isSearchEngineDone=true;
            }
            return isSearchEngine;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public boolean isSecure() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "isSecure()", null);
        try {
            return req.isSecure() || req.getServerPort()==443 || req.getRequestURI().indexOf("/https/")!=-1;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public boolean isUserInRole(String role) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "isUserInRole(String)", null);
        try {
            return req.isUserInRole(role);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    /**
     * Prints the hidden variables that contain all of the current settings.
     */
    public void printFormFields(ChainWriter out, int indent) {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "printFormFields(ChainWriter,int)", null);
        try {
            if("true".equals(req.getParameter("search_engine"))) printHiddenField(out, indent, "search_engine", "true");
            String layout=getLayoutName();
            if(layout!=null && (!layout.equalsIgnoreCase("default") || isLynx())) printHiddenField(out, indent, "layout", layout);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Prints the hidden variables that contain all of the current settings.
     */
    protected static void printHiddenField(ChainWriter out, int indent, String name, String value) {
        Profiler.startProfile(Profiler.IO, WebSiteRequest.class, "printHiddenField(ChainWriter,int,String,String)", null);
        try {
            for(int c=0;c<indent;c++) out.print("  ");
            out.print("<INPUT type='hidden' name='").printEI(name).print("' value='").printEI(value).print("'>\n");
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    public void removeAttribute(String name) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "removeAttribute(String)", null);
        try {
            req.removeAttribute(name);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public void setAttribute(String name, Object o) {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "setAttribute(String,Object)", null);
        try {
            req.setAttribute(name, o);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        Profiler.startProfile(Profiler.UNKNOWN, WebSiteRequest.class, "setCharacterEncoding(String)", null);
        try {
            req.setCharacterEncoding(encoding);
        } finally {
            Profiler.endProfile(Profiler.UNKNOWN);
        }
    }

    void setUsingFrames(boolean isFramed) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebSiteRequest.class, "setUsingFrames(boolean)", null);
        try {
            this.isFramed=isFramed;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
    
    public List<UploadedFile> getUploadedFiles() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getUploadedFiles()", null);
        try {
            if(reqUploadedFiles==null) Collections.emptyList();
            return Collections.unmodifiableList(reqUploadedFiles);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets a file that was uploaded given its ID.  The authentication
     * credentials for this request must match those of the provided ID.
     *
     * @return  the owner of the object
     * @return  the <code>UploadedFile</code> or <code>null</code> if not found
     *
     * @exception  SecurityException  if the ID is not assigned to the person logged in
     */
    public static UploadedFile getUploadedFile(WebSiteUser owner, long id, ServletContext context) throws SecurityException {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getUploadedFile(WebSiteUser,long,ServletContext)", null);
        try {
            synchronized(uploadedFiles) {
                UploadedFile uf=uploadedFiles.get(Long.valueOf(id));
                if(uf!=null) {
                    if(uf.getOwner().equals(owner)) return uf;
                    else WebPage.log(context, "UploadedFile found, but owner doesn't match: uf.getOwner()=\""+uf.getOwner()+"\", owner=\""+owner+"\".");
                }
                return null;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public File rename(File file) {
        Profiler.startProfile(Profiler.IO, WebSiteRequest.class, "rename(File)", null);
        try {
            try {
                while(true) {
                    File newFile=new File(WebSiteFrameworkConfiguration.getFileUploadDirectory(), String.valueOf(getNextID()));
                    if(!newFile.exists()) return newFile;
                }
            } catch(IOException err) {
                throw new WrappedException(err, new Object[] {"file.getPath()="+file.getPath()});
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    /**
     * Gets the person who is logged in or <code>null</code> if no login is performed for this request.
     *
     * @exception LoginException if an invalid login attempt is made or the user credentials are not found
     */
    abstract public WebSiteUser getWebSiteUser() throws IOException, SQLException, LoginException;

    /**
     * Determines if the user is currently logged in.
     */
    public boolean isLoggedIn() throws IOException, SQLException {
        try {
            return getWebSiteUser()!=null;
        } catch(LoginException err) {
            return false;
        }
    }

    public Object getOutputCacheKey() {
        Profiler.startProfile(Profiler.FAST, WebSiteRequest.class, "getOutputCacheKey()", null);
        try {
            return new WebSiteRequestCacheKey(this);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    /**
     * Logs out the current user or does nothing if not logged in.
     */
    abstract public void logout();
}
