/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2021, 2022  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-framework.
 *
 * ao-web-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-framework.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoapps.web.framework;

import com.aoapps.security.Identifier;
import java.io.File;

/**
 * An <code>UploadedFile</code> is a file that has been uploaded by a client request.
 *
 * @author  AO Industries, Inc.
 */
// TODO: This is not Serializable.  How is it persisted through reboots?
public final class UploadedFile {

  private final Identifier id;
  private final String filename;
  private final File storageFile;
  private final long createTime;
  private final WebSiteUser owner;
  private final String contentType;

  private static class LastAccessLock {
    // Empty lock class to help heap profile
  }

  private final LastAccessLock lastAccessLock = new LastAccessLock();
  private long lastAccessed;

  /**
   * Creates a new upload file.
   */
  UploadedFile(String filename, File storageFile, WebSiteUser owner, String contentType) {
    this.id = new Identifier(storageFile.getName());
    this.filename = filename;
    this.storageFile = storageFile;
    this.createTime = this.lastAccessed = System.currentTimeMillis();
    this.owner = owner;
    this.contentType = contentType;
  }

  /**
   * Gets the ID for the upload file.
   */
  public Identifier getId() {
    synchronized (lastAccessLock) {
      lastAccessed = System.currentTimeMillis();
    }
    return id;
  }

  /**
   * Gets the filename for the upload file.
   */
  public String getFilename() {
    synchronized (lastAccessLock) {
      lastAccessed = System.currentTimeMillis();
    }
    return filename;
  }

  /**
   * Gets the storage file for the upload file.
   */
  public File getStorageFile() {
    synchronized (lastAccessLock) {
      lastAccessed = System.currentTimeMillis();
    }
    return storageFile;
  }

  /**
   * Gets the create time for the upload file.
   */
  public long getCreateTime() {
    synchronized (lastAccessLock) {
      lastAccessed = System.currentTimeMillis();
    }
    return createTime;
  }

  /**
   * Gets the owner for the upload file.
   */
  public WebSiteUser getOwner() {
    synchronized (lastAccessLock) {
      lastAccessed = System.currentTimeMillis();
    }
    return owner;
  }

  /**
   * Gets the content type for the upload file.
   */
  public String getContentType() {
    synchronized (lastAccessLock) {
      lastAccessed = System.currentTimeMillis();
    }
    return contentType;
  }

  /**
   * Gets the last access time for the upload file.
   */
  public long getLastAccessed() {
    synchronized (lastAccessLock) {
      return lastAccessed;
    }
  }
}
