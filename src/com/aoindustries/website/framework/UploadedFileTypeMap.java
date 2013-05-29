package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import javax.activation.*;
import javax.servlet.*;
import java.io.*;

/**
 * @author  AO Industries, Inc.
 */
final public class UploadedFileTypeMap extends FileTypeMap {

    final private WebSiteUser owner;
    final private ServletContext context;
    final private LoggerAccessor loggerAccessor;

    public UploadedFileTypeMap(WebSiteUser owner, ServletContext context, LoggerAccessor loggerAccessor) {
        this.owner=owner;
        this.context=context;
        this.loggerAccessor = loggerAccessor;
    }
    
    public String getContentType(File file) {
        return getContentType(file.getName());
    }
    
    public String getContentType(String filename) {
        int pos=filename.lastIndexOf('/');
        if(pos==-1) pos=filename.lastIndexOf('\\');
        if(pos!=-1) filename=filename.substring(pos+1);
        long id=Long.parseLong(filename);
        UploadedFile uf=WebSiteRequest.getUploadedFile(owner, id, context, loggerAccessor);
        if(uf==null) throw new NullPointerException("Unable to find uploaded file: "+id);
        return uf.getContentType();
    }
}