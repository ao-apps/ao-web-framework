/*
 * Copyright 2000-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.encoding.ChainWriter;
import com.aoindustries.io.IoUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

/**
 * Reads everything from an input stream and puts it into a page.
 *
 * @author  AO Industries, Inc.
 */
abstract public class InputStreamPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public InputStreamPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public InputStreamPage(WebSiteRequest req) {
		super(req);
	}

	public InputStreamPage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		WebPageLayout layout=getWebPageLayout(req);
		layout.startContent(out, req, resp, 1, getPreferredContentWidth(req));
		try {
			layout.printContentTitle(out, req, resp, this, 1);
			layout.printContentHorizontalDivider(out, req, resp, 1, false);
			layout.startContentLine(out, req, resp, 1, null, null);
			try {
				try (InputStream in = getInputStream()) {
					printStream(out, req, resp, in);
				}
			} finally {
				layout.endContentLine(out, req, resp, 1, false);
			}
		} finally {
			layout.endContent(this, out, req, resp, 1);
		}
	}

	/**
	 * Gets the stream that the text should be read from.
	 */
	public abstract InputStream getInputStream() throws IOException;

	public void printStream(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, InputStream in) throws IOException, SQLException {
		printStreamStatic(out, in);
	}

	public static void printStreamStatic(ChainWriter out, InputStream in) throws IOException {
		IoUtils.copy(new InputStreamReader(in), out);
	}
}
