package com.aoindustries.website.framework;

/*
 * Copyright 2004-2011 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
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
