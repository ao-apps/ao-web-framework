/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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

import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Document;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically generates a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AutoListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	/**
	 * The number of columns in each row.
	 */
	public static final int NUM_COLS = 3;

	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		Document document,
		WebPageLayout layout
	) throws ServletException, IOException {
		if(req != null) {
			layout.startContent(document, req, resp, 1, getPreferredContentWidth(req));
			layout.printContentTitle(document, req, resp, this, 1);
			layout.printContentHorizontalDivider(document, req, resp, 1, false);
			layout.startContentLine(document, req, resp, 1, null, null);
			printContentStart(document, req, resp);
			document.out.write("<table cellpadding='0' cellspacing='10'>\n"
					+ "  <tbody>\n");
			printPageList(document, req, resp, this, layout);
			document.out.write("  </tbody>\n"
					+ "</table>\n");
			layout.endContentLine(document, req, resp, 1, false);
			layout.endContent(this, document, req, resp, 1);
		}
	}

	/**
	 * Prints the content that will be put before the auto-generated list.
	 */
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void printContentStart(
		Document document,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException {
	}

	/**
	 * Prints a list of pages.
	 */
	public static void printPageList(Document document, WebSiteRequest req, HttpServletResponse resp, WebPage[] pages, WebPageLayout layout) throws ServletException, IOException {
		int len = pages.length;
		for (int c = 0; c < len; c++) {
			WebPage page = pages[c];
			document.out.write("    <tr>\n"
					+ "      <td style='white-space:nowrap'><a class='aoLightLink' href='");
			if(req != null) encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), document.out);
			document.out.write("'>"); document.text(page.getShortTitle()).out.write("</a>\n"
					+ "      </td>\n"
					+ "      <td style='width:12px; white-space:nowrap'>&#160;</td>\n"
					+ "      <td style='white-space:nowrap'>"); document.text(page.getDescription()).out.write("</td>\n"
					+ "    </tr>\n");
		}
	}

	/**
	 * Prints an unordered list of the available pages.
	 */
	public static void printPageList(Document document, WebSiteRequest req, HttpServletResponse resp, WebPage parent, WebPageLayout layout) throws ServletException, IOException {
		if(req != null) printPageList(document, req, resp, parent.getCachedChildren(req, resp), layout);
	}
}
