/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2019  AO Industries, Inc.
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
import com.aoindustries.util.AoArrays;
import java.util.List;
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
		super(AoArrays.EMPTY_STRING_ARRAY);
	}

	@Override
	public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		String onload
	) {
		// Do nothing
	}

	@Override
	public void endHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out
	) {
		// Do nothing
	}

	@Override
	final public void printSearchOutput(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String query, boolean isEntireSite, List<SearchResult> results, String[] words) {
		throw new AssertionError("This should never be called within a search sub-request");
	}

	@Override
	public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) {
		// Do nothing
	}
	
	@Override
	public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) {
		// Do nothing
	}

	@Override
	public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) {
		// Do nothing
		// TODO: Should we write the title, encoded?  Or is the title used directly by the search indexing anyway?
	}

	@Override
	public void startContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) {
		// Do nothing
	}

	@Override
	public void printContentVerticalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) {
		// Do nothing
	}

	@Override
	public void endContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) {
		// Do nothing
	}

	@Override
	public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) {
		// Do nothing
	}

	@Override
	public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String align, String width, boolean nowrap) {
		// Do nothing
	}

	@Override
	public void endLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
		// Do nothing
	}

	@Override
	public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String align, String width, boolean nowrap) {
		// Do nothing
	}

	@Override
	public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
		// Do nothing
	}

	@Override
	public String getName() {
		return "Search";
	}

	@Override
	final public boolean printWebPageLayoutSelector(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp) {
		throw new AssertionError("This should never be called within a search sub-request");
	}

	@Override
	protected void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, WebPage page) {
		// Do nothing
	}
}
