/*
 * Copyright 2000-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import java.io.File;

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
		this.id=Long.parseLong(storageFile.getName());
		this.filename=filename;
		this.storageFile=storageFile;
		this.create_time=this.lastAccessed=System.currentTimeMillis();
		this.owner=owner;
		this.contentType=contentType;
	}

	public long getID() {
		synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
		return id;
	}

	public String getFilename() {
		synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
		return filename;
	}

	public File getStorageFile() {
		synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
		return storageFile;
	}

	public long getCreateTime() {
		synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
		return create_time;
	}

	public WebSiteUser getOwner() {
		synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
		return owner;
	}

	public String getContentType() {
		synchronized(lastAccessLock) {lastAccessed=System.currentTimeMillis();}
		return contentType;
	}

	public long getLastAccessed() {
		synchronized(lastAccessLock) {
			return lastAccessed;
		}
	}
}
