/*
 * Copyright 2000-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.io.ChainWriter;
import com.aoindustries.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

/**
 * Pulls information from a file to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class FilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public FilePage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public FilePage(WebSiteRequest req) {
		super(req);
	}

	public FilePage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		printFile(out, getFile());
	}

	/**
	 * Gets the file that the text should be read from.
	 */
	public abstract File getFile() throws IOException;

	@Override
	public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
		return Math.max(super.getLastModified(req), getFile().lastModified());
	}

	public static void printFile(ChainWriter out, File file) throws IOException {
		FileUtils.copy(file, out);
	}
}
