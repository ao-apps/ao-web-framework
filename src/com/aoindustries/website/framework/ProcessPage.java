/*
 * Copyright 2000-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Pulls information from a native process to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class ProcessPage extends InputStreamPage {

	private static final long serialVersionUID = 1L;

	public ProcessPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public ProcessPage(WebSiteRequest req) {
		super(req);
	}

	public ProcessPage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return getProcess().getInputStream();
	}

	@Override
	public long getLastModified(WebSiteRequest req) {
		return -1;
	}

	/**
	 * Gets the process that the contents should be read from
	 */
	public abstract Process getProcess() throws IOException;

	/**
	 * The search format of this page is indexed.
	 */
	@Override
	public long getSearchLastModified() throws IOException, SQLException {
		return super.getLastModified(null);
	}
}
