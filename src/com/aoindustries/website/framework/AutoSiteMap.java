/*
 * Copyright 2000-2009, 2015, 2016 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.encoding.ChainWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically builds a site map of the entire site.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AutoSiteMap extends TreePage {

	private static final long serialVersionUID = 1L;

	public AutoSiteMap(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public AutoSiteMap(WebSiteRequest req) {
		super(req);
	}

	public AutoSiteMap(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
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
					req.getContextPath()+req.getURL(page),
					page.getDescription()
				)
			);
			for(int c=0; c<len; c++) buildData(path, pages[c], data, req);
		}
	}

	/**
	 * The content of this page will not be included in the interal search engine.
	 */
	@Override
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		if(req!=null) super.doGet(out, req, resp);
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
