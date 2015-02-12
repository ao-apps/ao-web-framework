/*
 * Copyright 2004-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

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
