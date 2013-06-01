package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Pulls information from a file to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class FilePage extends WebPage {

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
		Reader in = new FileReader(file);
		try {
			IoUtils.copy(in, out);
		} finally {
			in.close();
		}
	}
}