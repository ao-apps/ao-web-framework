/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically builds a site map of the entire site.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AutoSiteMap extends TreePage {

	private static final long serialVersionUID = 1L;

	public AutoSiteMap() {
		super();
	}

	public AutoSiteMap(WebSiteRequest req) {
		super(req);
	}

	public AutoSiteMap(Object param) {
		super(param);
	}

	/**
	 * Recursively builds the list of all sites.
	 */
	private void buildData(String path, WebPage page, List<TreePageData> data, WebSiteRequest req) throws IOException, SQLException {
		if(isVisible(page)) {
			if(path.length()>0) path=path+'/'+page.getShortTitle();
			else path=page.getShortTitle();
			WebPage[] pages=page.getCachedPages(req);
			int len=pages.length;
			data.add(
				new TreePageData(
					len>0 ? (path+'/') : path,
					req.getURL(page),
					page.getDescription()
				)
			);
			for(int c=0; c<len; c++) buildData(path, pages[c], data, req);
		}
	}

	/**
	 * The content of this page will not be included in the internal search engine.
	 */
	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out
	) throws ServletException, IOException, SQLException {
		if(req != null) super.doGet(req, resp, out); // TODO: A search layout that does almost nothing
	}

	@Override
	final protected List<? extends TreePageData> getTree(WebSiteRequest req) throws IOException, SQLException {
		WebPage home=getRootPage();
		List<TreePageData> data=new ArrayList<>();
		buildData("", home, data, req);
		//int size=data.size();
		return data;
	}

	/**
	 * Determines if a page should be visible in the generated maps.
	 */
	abstract protected boolean isVisible(WebPage page);
}
