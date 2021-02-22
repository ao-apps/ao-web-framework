/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoindustries.collections.SortedArrayList;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Document;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Generates a list of all URLs in the site.  This is useful for adding this site to
 * search engines.
 *
 * @author  AO Industries, Inc.
 */
abstract public class DumpURLs extends WebPage {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		Document document,
		WebPageLayout layout
	) throws ServletException, IOException {
		document.out.write("The following is a list of all unique servlet URLs in the site and may be used to add this site to\n"
				+ "search engines:\n"
				+ "<pre>\n");
		WebPage page = getRootPage();
		printURLs(req, resp, document, page, new SortedArrayList<>());
		document.out.write("</pre>\n");
	}

	@Override
	public String getDescription() {
		return "Lists all of the URLs in the site, useful for adding to search engines.";
	}

	@Override
	public String getKeywords() {
		return "search, engine, URL, list, add, hit, hits, adding";
	}

	/**
	 * The last modified time is -1 to always reload the list.
	 */
	@Override
	public long getLastModified(WebSiteRequest req, HttpServletResponse resp) throws ServletException {
		return -1;
	}

	/**
	 * Do not include this in the search results.
	 */
	@Override
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void search(
		String[] words,
		WebSiteRequest req,
		HttpServletResponse response,
		List<SearchResult> results,
		CharArrayWriter buffer,
		Set<WebPage> finishedPages
	) {
	}

	@Override
	public long getSearchLastModified() throws ServletException {
		return getClassLastModified();
	}

	@Override
	public String getTitle() {
		return "List URLs";
	}

	private void printURLs(WebSiteRequest req, HttpServletResponse resp, Document document, WebPage page, List<WebPage> finishedPages) throws ServletException, IOException {
		if(!finishedPages.contains(page)) {
			document.out.write("<a class='aoLightLink' href='");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), document.out);
			document.out.write("'>");
			document.text(req.getURL(page));
			document.out.write("</a>\n");

			finishedPages.add(page);

			WebPage[] children = page.getCachedChildren(req, resp);
			int len = children.length;
			for (int c = 0; c < len; c++) {
				printURLs(req, resp, document, children[c], finishedPages);
			}
		}
	}
}
