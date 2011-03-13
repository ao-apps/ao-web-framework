package com.aoindustries.website.framework;

/*
 * Copyright 2000-2011 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.ChainWriter;
import com.aoindustries.security.LoginException;
import com.aoindustries.util.SortedArrayList;
import com.aoindustries.util.StringUtility;
import com.aoindustries.util.WrappedException;
import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.FileRenamePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * A <code>WebSiteSettings</code> contains all the values that a user may customize while they view the web site.
 *
 * @author  AO Industries, Inc.
 */
public class WebSiteRequest extends HttpServletRequestWrapper implements FileRenamePolicy {

    /**
     * Gets the random number generator used for this request.
     */
    public Random getRandom() throws IOException {
        return ErrorReportingServlet.getRandom();
    }

    private static String getExtension(String filename) {
        int pos=filename.lastIndexOf('.');
        if(pos==-1 || pos==(filename.length()-1)) return filename;
        else return filename.substring(pos+1);
    }

    private static final Object mimeTypeLock=new Object();
    private static Map<String,String> mimeTypes;
    private static String getContentType(MultipartRequest mreq, String filename) throws IOException {
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
    }

    private static final Map<Long,UploadedFile> uploadedFiles=new HashMap<Long,UploadedFile>();
    private long getNextID() throws IOException {
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
    }

    private static Thread uploadedFileCleanup;
    private static void addUploadedFile(UploadedFile uf, final ServletContext servletContext, final LoggerAccessor loggerAccessor) {
        synchronized(uploadedFiles) {
            uploadedFiles.put(Long.valueOf(uf.getID()), uf);

            if(uploadedFileCleanup==null) {
                uploadedFileCleanup=new Thread() {
                    @Override
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
                                                    if(file.exists() && !file.delete()) {
                                                        loggerAccessor.getLogger(servletContext, getClass().getName()).log(Level.SEVERE, "file.getPath()="+file.getPath(), new IOException("Unable to delete file"));
                                                    }
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
                                                    if(!found && !file.delete()) {
                                                        loggerAccessor.getLogger(servletContext, getClass().getName()).log(Level.SEVERE, "file.getPath()="+file.getPath(), new IOException("Unable to delete file"));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch(ThreadDeath TD) {
                                throw TD;
                            } catch(Throwable T) {
                                loggerAccessor.getLogger(servletContext, getClass().getName()).log(Level.SEVERE, null, T);
                                try {
                                    sleep(60*1000);
                                } catch(InterruptedException err) {
                                    loggerAccessor.getLogger(servletContext, getClass().getName()).log(Level.WARNING, null, err);
                                }
                            }
                        }
                    }
                };
                uploadedFileCleanup.start();
            }
        }
    }

    final protected WebPage sourcePage;
    final private HttpServletRequest req;
    private MultipartRequest mreq;
    private List<UploadedFile> reqUploadedFiles;

    private boolean isLynx;
    private boolean isLynxDone;

    private boolean isBlackBerry;
    private boolean isBlackBerryDone;

    private boolean isLinux;
    private boolean isLinuxDone;

    public WebSiteRequest(WebPage sourcePage, HttpServletRequest req) throws IOException, SQLException {
        super(req);
        this.sourcePage=sourcePage;
        this.req=req;
        String contentType=req.getHeader("Content-Type");
        if (contentType!=null && contentType.length()>=19 && contentType.substring(0,19).equals("multipart/form-data")) {
            boolean keepFiles=false;
            try {
                mreq = new MultipartRequest(req, WebSiteFrameworkConfiguration.getFileUploadDirectory().getPath(), WebSiteFrameworkConfiguration.getMaxFileUploadSize(), this);
                try {
                    // Determine the authentication info
                    WebSiteUser user=getWebSiteUser(null);
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
                                addUploadedFile(uf, sourcePage.getServletContext(), sourcePage.getLoggerAccessor());
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
    }

    /**
     * Appends the parameters to a URL.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    protected static boolean appendParams(StringBuilder SB, Object optParam, List<String> finishedParams, boolean alreadyAppended) {
        if (optParam != null) {
            if (optParam instanceof String) {
                String[] nameValuePairs=StringUtility.splitString((String)optParam, '&');
                int len=nameValuePairs.length;
                for(int c=0;c<len;c++) {
                    SB.append(alreadyAppended?'&':'?');
                    String S=nameValuePairs[c];
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
    }

    /**
     * Gets a relative URL from a String containing a classname and optional parameters.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    public String getURL(String classAndParams) throws IOException, SQLException {
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
    }

    /**
     * Gets a relative URL given its classname and optional parameters.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    public String getURL(String classname, String params) throws IOException, SQLException {
        try {
            Class<? extends WebPage> clazz=Class.forName(classname).asSubclass(WebPage.class);
            return getURL(clazz, params);
        } catch(ClassNotFoundException err) {
            IOException ioErr=new IOException("Unable to load class: "+classname);
            ioErr.initCause(err);
            throw ioErr;
        }
    }

    /**
     * Gets the absolute URL String, optionally with the settings embedded.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    public String getURL(String url, boolean useEncryption, Object optParam, boolean keepSettings) throws IOException {
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
    }

    protected boolean appendSettings(List<String> finishedParams, boolean alreadyAppended, StringBuilder SB) {
        boolean searchEngine="true".equals(getParameter("search_engine"));
        if(searchEngine) alreadyAppended=appendParams(SB, new String[] {"search_engine", "true"}, finishedParams, alreadyAppended);
        return alreadyAppended;
    }

    @Override
    public String getParameter(String name) {
        if(mreq==null) return req.getParameter(name);
        else {
            String param=mreq.getParameter(name);
            if(param==null) param=req.getParameter(name);
            return param;
        }
    }

    @Override
    public Enumeration getParameterNames() {
        if (mreq==null) return req.getParameterNames();
        return mreq.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        if (mreq==null) return req.getParameterValues(name);
        return mreq.getParameterValues(name);
    }

    /**
     * Gets the absolute URL to a web page.
     */
    public String getURL(WebPage page) throws IOException, SQLException {
        return getURL(page, page.useEncryption(), null);
    }

    /**
     * Gets the absolute URL to a web page.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    public String getURL(WebPage page, Object optParam) throws IOException, SQLException {
        return getURL(page, page.useEncryption(), optParam);
    }

    /**
     * Gets the absolute URL to a web page.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    public String getURL(WebPage page, boolean useEncryption, Object optParam) throws IOException, SQLException {
        List<String> finishedParams=new SortedArrayList<String>();
        StringBuilder SB = new StringBuilder();
        String path = page.getURLPath();
        String lowerPath = path.toLowerCase();
        if(!lowerPath.startsWith("http:") && !lowerPath.startsWith("https:")) {
            SB.append(
                useEncryption
                ?WebSiteFrameworkConfiguration.getHttpsBase()
                :WebSiteFrameworkConfiguration.getHttpBase()
            );
        }
        SB.append(path);
        boolean alreadyAppended=appendParams(SB, optParam, finishedParams, false);
        alreadyAppended=appendParams(SB, page.getURLParams(this), finishedParams, alreadyAppended);

        alreadyAppended=appendSettings(finishedParams, alreadyAppended, SB);

        return SB.toString();
    }

    /**
     * Gets the absolute URL to a web page.
     * Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
     */
    public String getURL(Class<? extends WebPage> clazz, Object param) throws IOException, SQLException {
        WebPage page=WebPage.getWebPage(sourcePage.getServletContext(), clazz, param);
        return getURL(page, page.useEncryption(), param);
    }

    public String getURL(Class<? extends WebPage> clazz) throws IOException, SQLException {
        return getURL(clazz, null);
    }

    /**
     * Gets the URL String with the given parameters embedded, keeping the current settings.
     *
     * @param  url            the URL from the top of the webapp, without any beginning slash
     * @param  useEncryption  if <code>true</code> the link refers to an page that should be served over encryption
     * @param  optParam       any number of additional parameters.  This parameter can accept several types of
     *                        objects.  The following is a list of supported objects and a brief description of its
     *                        behavior.
     *                        Parameters should already be URL encoded and have a single ampersand (&amp;) as separator.
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
        return getURL(url, useEncryption, optParam, true);
    }

    /**
     * Determines if the request is for a Lynx browser
     */
    public boolean isLynx() {
        if(!isLynxDone) {
            String agent = req.getHeader("user-agent");
            isLynx=agent != null && agent.toLowerCase().indexOf("lynx") != -1;
            isLynxDone=true;
        }
        return isLynx;
    }

    /**
     * Determines if the request is for a BlackBerry browser
     */
    public boolean isBlackBerry() {
        if(!isBlackBerryDone) {
            String agent = req.getHeader("user-agent");
            isBlackBerry=agent != null && agent.startsWith("BlackBerry");
            isBlackBerryDone=true;
        }
        return isBlackBerry;
    }

    /**
     * Determines if the request is for a Linux browser
     */
    public boolean isLinux() {
        if(!isLinuxDone) {
            String agent = req.getHeader("user-agent");
            isLinux=agent == null || agent.toLowerCase().indexOf("linux") != -1;
            isLinuxDone=true;
        }
        return isLinux;
    }

    @Override
    public boolean isSecure() {
        return req.isSecure() || req.getServerPort()==443 || req.getRequestURI().indexOf("/https/")!=-1;
    }

    /**
     * Prints the hidden variables that contain all of the current settings.
     */
    public void printFormFields(ChainWriter out, int indent) throws IOException {
        if("true".equals(req.getParameter("search_engine"))) printHiddenField(out, indent, "search_engine", "true");
    }

    /**
     * Prints the hidden variables that contain all of the current settings.
     */
    protected static void printHiddenField(ChainWriter out, int indent, String name, String value) throws IOException {
        for(int c=0;c<indent;c++) out.print("  ");
        out.print("<input type='hidden' name='").encodeXmlAttribute(name).print("' value='").encodeXmlAttribute(value).print("' />\n");
    }

    public List<UploadedFile> getUploadedFiles() {
        if(reqUploadedFiles==null) Collections.emptyList();
        return Collections.unmodifiableList(reqUploadedFiles);
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
    public static UploadedFile getUploadedFile(WebSiteUser owner, long id, ServletContext context, LoggerAccessor loggerAccessor) throws SecurityException {
        synchronized(uploadedFiles) {
            UploadedFile uf=uploadedFiles.get(Long.valueOf(id));
            if(uf!=null) {
                if(uf.getOwner().equals(owner)) return uf;
                else {
                    loggerAccessor.getLogger(context, WebSiteRequest.class.getName()).severe("UploadedFile found, but owner doesn't match: uf.getOwner()=\""+uf.getOwner()+"\", owner=\""+owner+"\".");
                }
            }
            return null;
        }
    }
    
    public File rename(File file) {
        try {
            while(true) {
                File newFile=new File(WebSiteFrameworkConfiguration.getFileUploadDirectory(), String.valueOf(getNextID()));
                if(!newFile.exists()) return newFile;
            }
        } catch(IOException err) {
            throw new WrappedException(err, new Object[] {"file.getPath()="+file.getPath()});
        }
    }

    /**
     * Gets the person who is logged in or <code>null</code> if no login is performed for this request.
     *
     * @exception LoginException if an invalid login attempt is made or the user credentials are not found
     */
    public WebSiteUser getWebSiteUser(HttpServletResponse resp) throws IOException, SQLException, LoginException {
        return null;
    }

    /**
     * Determines if the user is currently logged in.
     */
    public boolean isLoggedIn() throws IOException, SQLException {
        try {
            return getWebSiteUser(null)!=null;
        } catch(LoginException err) {
            return false;
        }
    }

    /**
     * Logs out the current user or does nothing if not logged in.
     */
    public void logout(HttpServletResponse resp) {
    }
}
