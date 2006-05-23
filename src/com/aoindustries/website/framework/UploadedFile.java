package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 2200 Dogwood Ct N, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.*;
import java.io.*;

/**
 * An <code>UploadedFile</code> is a file that has been uploaded by a client request.
 *
 * @author  AO Industries, Inc.
 */
final public class UploadedFile {

    final private long id;
    final private String filename;
    final private File storageFile;
    final private long create_time;
    final private WebSiteUser owner;
    final private String contentType;
    final private Object lastAccessLock=new Object();
    private long lastAccessed;

    UploadedFile(String filename, File storageFile, WebSiteUser owner, String contentType) {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "<init>(String,File,WebSiteUser,String)", null);
        try {
            this.id=Long.parseLong(storageFile.getName());
            this.filename=filename;
            this.storageFile=storageFile;
            this.create_time=this.lastAccessed=System.currentTimeMillis();
            this.owner=owner;
            this.contentType=contentType;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public long getID() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getID()", null);
        try {
            synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
            return id;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public String getFilename() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getFilename()", null);
        try {
            synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
            return filename;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public File getStorageFile() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getStorageFile()", null);
        try {
            synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
            return storageFile;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public long getCreateTime() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getCreateTime()", null);
        try {
            synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
            return create_time;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public WebSiteUser getOwner() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getOwner()", null);
        try {
            synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
            return owner;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public String getContentType() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getContentType()", null);
        try {
            synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
            return contentType;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public long getLastAccessed() {
        Profiler.startProfile(Profiler.FAST, UploadedFile.class, "getLastAccessed()", null);
        try {
            synchronized(lastAccessLock) {
                return lastAccessed;
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}