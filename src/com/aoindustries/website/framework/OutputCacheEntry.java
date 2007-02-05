package com.aoindustries.website.framework;

/*
 * Copyright 2004-2007 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */

/**
 * @see  WebPage#doGet(WebSiteRequest,HttpServletResponse)
 *
 * @author  AO Industries, Inc.
 */
final public class OutputCacheEntry {

    final public Object outputCacheKey;

    final public long lastModified;
    
    final public byte[] bytes;
    
    public OutputCacheEntry(Object outputCacheKey, long lastModified, byte[] bytes) {
        this.outputCacheKey=outputCacheKey;
        this.lastModified=lastModified;
        this.bytes=bytes;
    }
}
