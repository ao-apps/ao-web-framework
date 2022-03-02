/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2019, 2020, 2021, 2022  AO Industries, Inc.
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
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		DocumentEE document,
		String onload
	) {
		// Do nothing
		@SuppressWarnings("unchecked") __ flow = (__)document;
		return flow;
	}

	@Override
	public void endPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		FlowContent<?> flow
	) {
		// Do nothing
	}

	@Override
	public final <__ extends FlowContent<__>> void printSearchOutput(WebSiteRequest req, HttpServletResponse resp, WebPage page, __ flow, String query, boolean isEntireSite, List<SearchResult> results, String[] words) {
		throw new AssertionError("This should never be called within a search sub-request");
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>
	> __ startContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int[] contentColumnSpans,
		String width
	) {
		// Do nothing
		@SuppressWarnings("unchecked")
		__ content = (__)pc;
		return content;
	}

	@Override
	public void contentTitle(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		String title,
		int contentColumns
	) {
		// Do nothing
	}

	@Override
	public <__ extends FlowContent<__>> __ startContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int colspan,
		String align,
		String width
	) {
		// Do nothing
		@SuppressWarnings("unchecked")
		__ contentLine = (__)content;
		return contentLine;
	}

	@Override
	public <__ extends FlowContent<__>> __ contentVerticalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine,
		int direction,
		int colspan,
		int rowspan,
		String align,
		String width
	) {
		// Do nothing
		@SuppressWarnings("unchecked")
		__ newContentLine = (__)contentLine;
		return newContentLine;
	}

	@Override
	public void endContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine,
		int rowspan,
		boolean endsInternal
	) {
		// Do nothing
	}

	@Override
	public void contentHorizontalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int[] colspansAndDirections,
		boolean endsInternal
	) {
		// Do nothing
	}

	@Override
	public void endContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content,
		int[] contentColumnSpans
	) {
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
	public final <__ extends FlowContent<__>> boolean printWebPageLayoutSelector(WebSiteRequest req, HttpServletResponse resp, WebPage page, __ flow) {
		throw new AssertionError("This should never be called within a search sub-request");
	}

	@Override
	protected <__ extends ScriptSupportingContent<__>> void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, WebPage page, __ content) {
		// Do nothing
	}
}
