package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.*;
import javax.activation.*;
import javax.servlet.*;
import java.io.*;

/**
 * @author  AO Industries, Inc.
 */
final public class UploadedFileTypeMap extends FileTypeMap {

    private WebSiteUser owner;
    private ServletContext context;

    public UploadedFileTypeMap(WebSiteUser owner, ServletContext context) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, UploadedFileTypeMap.class, "<init>(WebSiteUser,ServletContext)", null);
        try {
            this.owner=owner;
            this.context=context;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
    
    public String getContentType(File file) {
        Profiler.startProfile(Profiler.FAST, UploadedFileTypeMap.class, "getContentType(File)", null);
        try {
            return getContentType(file.getName());
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public String getContentType(String filename) {
        Profiler.startProfile(Profiler.FAST, UploadedFileTypeMap.class, "getContentType(String)", null);
        try {
            int pos=filename.lastIndexOf('/');
            if(pos==-1) pos=filename.lastIndexOf('\\');
            if(pos!=-1) filename=filename.substring(pos+1);
            long id=Long.parseLong(filename);
            UploadedFile uf=WebSiteRequest.getUploadedFile(owner, id, context);
            if(uf==null) throw new NullPointerException("Unable to find uploaded file: "+id);
            return uf.getContentType();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}