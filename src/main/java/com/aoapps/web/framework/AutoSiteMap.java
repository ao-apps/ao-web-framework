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

import com.aoapps.html.servlet.DocumentEE;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically builds a site map of the entire site.
 *
 * @author  AO Industries, Inc.
 */
public abstract class AutoSiteMap extends TreePage {

	private static final long serialVersionUID = 1L;

	/**
	 * Recursively builds the list of all sites.
	 */
	private void buildData(Deque<String> path, WebPage page, List<TreePageData> data, WebSiteRequest req, HttpServletResponse resp) throws ServletException {
		if(isVisible(page)) {
			path.add(page.getShortTitle(req));
			WebPage[] children = page.getCachedChildren(req, resp);
			data.add(
				new TreePageData(
					req.getURL(page),
					page.getDescription(req),
					children.length > 0,
					path
				)
			);
			for(WebPage child : children) {
				buildData(path, child, data, req, resp);
			}
			path.removeLast();
		}
	}

	/**
	 * The content of this page will not be included in the internal search engine.
	 */
	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		DocumentEE document
	) throws ServletException, IOException {
		if(req != null) super.doGet(req, resp, document);
	}

	@Override
	protected final List<? extends TreePageData> getTree(WebSiteRequest req, HttpServletResponse resp) throws ServletException {
		WebPage home=getRootPage();
		List<TreePageData> data=new ArrayList<>();
		buildData(new ArrayDeque<>(), home, data, req, resp);
		return data;
	}

	/**
	 * Determines if a page should be visible in the generated maps.
	 */
	protected abstract boolean isVisible(WebPage page);
}
