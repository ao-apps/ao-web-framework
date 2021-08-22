/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2019, 2020, 2021  AO Industries, Inc.
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
 * along with ao-web-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.web.framework;

import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.ScriptSupportingContent;
import com.aoapps.lang.EmptyArrays;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The layout to be used during search sub-requests.
 * Performs minimal writes of content only.
 *
 * @author  AO Industries, Inc.
 */
public class SearchLayout extends WebPageLayout {

	private static final SearchLayout instance = new SearchLayout();

	public static SearchLayout getInstance() {
		return instance;
	}

	protected SearchLayout() {
		super(EmptyArrays.EMPTY_STRING_ARRAY);
	}

	@Override
	public <__ extends FlowContent<__>> __ startPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		DocumentEE document,
		String onload
	) {
		// Do nothing
		@SuppressWarnings("unchecked") __ flow = (__)document;
		return flow;
	}

	@Override
	public void endPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> flow
	) {
		// Do nothing
	}

	@Override
	final public <__ extends FlowContent<__>> void printSearchOutput(WebPage page, __ flow, WebSiteRequest req, HttpServletResponse resp, String query, boolean isEntireSite, List<SearchResult> results, String[] words) {
		throw new AssertionError("This should never be called within a search sub-request");
	}

	@Override
	public void startContent(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) {
		// Do nothing
	}

	@Override
	public void printContentHorizontalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) {
		// Do nothing
	}

	@Override
	public void printContentTitle(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) {
		// Do nothing
	}

	@Override
	public void startContentLine(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) {
		// Do nothing
	}

	@Override
	public void printContentVerticalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) {
		// Do nothing
	}

	@Override
	public void endContentLine(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) {
		// Do nothing
	}

	@Override
	public void endContent(WebPage page, DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) {
		// Do nothing
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException {
		// Do nothing
		@SuppressWarnings("unchecked")
		__ lightArea = (__)pc;
		return lightArea;
	}

	@Override
	public void endLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> lightArea
	) throws ServletException, IOException {
		// Do nothing
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException {
		// Do nothing
		@SuppressWarnings("unchecked")
		__ whiteArea = (__)pc;
		return whiteArea;
	}

	@Override
	public void endWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> whiteArea
	) throws ServletException, IOException {
		// Do nothing
	}

	@Override
	public String getName() {
		return "Search";
	}

	@Override
	final public <__ extends FlowContent<__>> boolean printWebPageLayoutSelector(WebPage page, __ flow, WebSiteRequest req, HttpServletResponse resp) {
		throw new AssertionError("This should never be called within a search sub-request");
	}

	@Override
	protected <__ extends ScriptSupportingContent<__>> void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, __ content, WebPage page) {
		// Do nothing
	}
}
