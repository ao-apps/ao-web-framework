/*
 * Copyright 2006-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

/**
 * @author  AO Industries, Inc.
 */
public class TreePageData {

	private final String path;
	private final String url;
	private final String description;

	public TreePageData(String path, String url, String description) {
		this.path=path;
		this.url=url;
		this.description=description;
	}

	public String getPath() {
		return path;
	}

	public String getUrl() {
		return url;
	}

	public String getDescription() {
		return description;
	}
}
