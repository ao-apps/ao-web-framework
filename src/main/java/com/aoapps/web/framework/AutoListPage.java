/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.html.servlet.ContentEE;
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
	@SuppressWarnings("unchecked")
	public <__ extends FlowContent<__>> void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		__ flow
	) throws ServletException, IOException {
		if(req != null) {
			layout.content(req, resp, this, flow, content -> {
				layout.contentTitle(req, resp, this, content);
				layout.contentHorizontalDivider(req, resp, content);
				FlowContent<?> contentLine = layout.startContentLine(req, resp, content);
				contentLine = printContentStart(req, resp, layout, content, (FlowContent)contentLine);
				{
					contentLine.table().cellpadding(0).cellspacing(10).__(table -> table
						.tbody__(tbody ->
							printPageList(tbody, req, resp, this, layout)
						)
					);
				}
				layout.endContentLine(req, resp, contentLine);
			});
		}
	}

	/**
	 * Prints the content that will be put before the auto-generated list.
	 *
	 * @return  The current {@code contentLine}, which may have been replaced by a call to
	 *          {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *          or {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
	 */
	@SuppressWarnings("NoopMethodInAbstractClass")
	public <__ extends FlowContent<__>> __ printContentStart(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		ContentEE<?> content,
		__ contentLine
	) throws ServletException, IOException {
		// Do nothing
		return contentLine;
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
