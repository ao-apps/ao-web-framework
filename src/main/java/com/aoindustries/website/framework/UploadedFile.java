/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aoweb-framework.
 *
 * aoweb-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aoweb-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aoweb-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.website.framework;

import com.aoindustries.security.Identifier;
import java.io.File;

/**
 * An <code>UploadedFile</code> is a file that has been uploaded by a client request.
 *
 * @author  AO Industries, Inc.
 */
final public class UploadedFile {

	final private Identifier id;
	final private String filename;
	final private File storageFile;
	final private long create_time;
	final private WebSiteUser owner;
	final private String contentType;
	private static class LastAccessLock {}
	final private LastAccessLock lastAccessLock=new LastAccessLock();
	private long lastAccessed;

	UploadedFile(String filename, File storageFile, WebSiteUser owner, String contentType) {
		this.id = new Identifier(storageFile.getName());
		this.filename=filename;
		this.storageFile=storageFile;
		this.create_time=this.lastAccessed=System.currentTimeMillis();
		this.owner=owner;
		this.contentType=contentType;
	}

	public Identifier getID() {
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
