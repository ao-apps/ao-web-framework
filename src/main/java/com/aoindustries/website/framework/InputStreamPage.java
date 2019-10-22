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
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		WebPageLayout layout
	) throws IOException, SQLException {
		layout.startContent(out, req, resp, 1, getPreferredContentWidth(req));
		try {
			layout.printContentTitle(out, req, resp, this, 1);
			layout.printContentHorizontalDivider(out, req, resp, 1, false);
			layout.startContentLine(out, req, resp, 1, null, null);
			try {
				// TODO: getEncoding (getCharacterEncoding) method on WebPage, with default Html.Encoding
				// TODO: Encoding here, read as Reader
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
