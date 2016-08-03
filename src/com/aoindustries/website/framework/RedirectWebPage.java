/*
 * Copyright 2007-2009, 2015, 2016 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Redirects to the configured URL.
 *
 * @author  AO Industries, Inc.
 */
public class RedirectWebPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private final WebPage parent;
	private final String path;
	private final String description;
	private final String keywords;
	private final String navImageAlt;
	private final String title;

	/**
	 * Performs a redirect.
	 *
	 * @param  path  the context-relative path, with a preceeding slash (/)
	 */
	public RedirectWebPage(LoggerAccessor logAccessor, ServletContext context, WebPage parent, String path, String description, String keywords, String navImageAlt, String title) {
		super(logAccessor);
		setServletContext(context);
		this.parent = parent;
		this.path = path;
		this.description = description;
		this.keywords = keywords;
		this.navImageAlt = navImageAlt;
		this.title = title;
	}

	@Override
	protected WebSiteRequest getWebSiteRequest(HttpServletRequest req) throws IOException, SQLException {
		return new WebSiteRequest(this, req);
	}

	@Override
	public WebPage getParent() {
		return parent;
	}

	@Override
	public String getRedirectURL(WebSiteRequest req) throws IOException {
		String lowerPath = path.toLowerCase();
		if(lowerPath.startsWith("http:") || lowerPath.startsWith("https:")) return path;
		return path;
	}

	@Override
	public String getURLPath() {
		return path;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getKeywords() {
		return keywords;
	}

	@Override
	public String getNavImageAlt(WebSiteRequest req) {
		return navImageAlt;
	}

	@Override
	public String getTitle() {
		return title;
	}
}
