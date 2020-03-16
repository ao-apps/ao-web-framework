/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016  AO Industries, Inc.
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
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically generates a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AutoListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public AutoListPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public AutoListPage(WebSiteRequest req) {
		super(req);
	}

	public AutoListPage(LoggerAccessor loggerAccessor, Object param) {
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
		layout.printContentTitle(out, req, resp, this, 1);
		layout.printContentHorizontalDivider(out, req, resp, 1, false);
		layout.startContentLine(out, req, resp, 1, null, null);
		printContentStart(out, req, resp);
		try {
			out.print("      <table cellpadding='0' cellspacing='10'>\n");
			printPageList(out, req, resp, this, layout);
			out.print("      </table>\n");
		} finally {
			layout.endContentLine(out, req, resp, 1, false);
			layout.endContent(this, out, req, resp, 1);
		}
	}

	/**
	 * Prints the content that will be put before the auto-generated list.
	 */
	public void printContentStart(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
	}

	/**
	 * Prints a list of pages.
	 */
	public static void printPageList(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage[] pages, WebPageLayout layout) throws IOException, SQLException {
		int len = pages.length;
		for (int c = 0; c < len; c++) {
			WebPage page = pages[c];
			out.print("  <tr>\n"
					+ "    <td style='white-space:nowrap'><a class='aoLightLink' href='").encodeXmlAttribute(req==null?"":resp.encodeURL(req.getContextPath()+req.getURL(page))).print("'>").encodeXhtml(page.getShortTitle()).print("</a>\n"
					+ "    </td>\n"
					+ "    <td style='width:12px; white-space:nowrap'>&#160;</td>\n"
					+ "    <td style='white-space:nowrap'>").encodeXhtml(page.getDescription()).print("</td>\n"
					+ "  </tr>\n");
		}
	}

	/**
	 * Prints an unordered list of the available pages.
	 */
	public static void printPageList(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage parent, WebPageLayout layout) throws IOException, SQLException {
		printPageList(out, req, resp, parent.getCachedPages(req), layout);
	}
}
