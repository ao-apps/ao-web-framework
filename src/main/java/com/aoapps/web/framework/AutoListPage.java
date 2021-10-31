/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-framework.
 *
 * ao-web-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-framework.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.aoapps.web.framework;

import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.Union_TBODY_THEAD_TFOOT;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically generates a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
public abstract class AutoListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	/**
	 * The number of columns in each row.
	 */
	public static final int NUM_COLS = 3;

	@Override
	public <__ extends FlowContent<__>> void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		__ flow
	) throws ServletException, IOException {
		if(req != null) {
			DocumentEE document = flow.getDocument();
			layout.startContent(document, req, resp, 1, getPreferredContentWidth(req));
			layout.printContentTitle(document, req, resp, this, 1);
			layout.printContentHorizontalDivider(document, req, resp, 1, false);
			layout.startContentLine(document, req, resp, 1, null, null);
			printContentStart(document, req, resp);
			flow.table().cellpadding(0).cellspacing(10).__(table -> table
				.tbody__(tbody ->
					printPageList(tbody, req, resp, this, layout)
				)
			);
			layout.endContentLine(document, req, resp, 1, false);
			layout.endContent(this, document, req, resp, 1);
		}
	}

	/**
	 * Prints the content that will be put before the auto-generated list.
	 */
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void printContentStart(
		DocumentEE document,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException {
	}

	/**
	 * Prints a list of pages.
	 */
	public static void printPageList(Union_TBODY_THEAD_TFOOT<?> tbody, WebSiteRequest req, HttpServletResponse resp, WebPage[] pages, WebPageLayout layout) throws ServletException, IOException {
		int len = pages.length;
		for (int c = 0; c < len; c++) {
			WebPage page = pages[c];
			tbody.tr__(tr -> tr
				.td().style("white-space:nowrap").__(td -> td
					.a().clazz("aoLightLink").href(req == null ? null : req.getEncodedURL(page, resp)).__(page.getShortTitle(req))
				)
				.td().style("width:12px", "white-space:nowrap").__("\u00A0")
				.td().style("white-space:nowrap").__(page.getDescription(req))
			);
		}
	}

	/**
	 * Prints an unordered list of the available pages.
	 */
	public static void printPageList(Union_TBODY_THEAD_TFOOT<?> tbody, WebSiteRequest req, HttpServletResponse resp, WebPage parent, WebPageLayout layout) throws ServletException, IOException {
		if(req != null) printPageList(tbody, req, resp, parent.getCachedChildren(req, resp), layout);
	}
}
