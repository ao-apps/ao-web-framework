/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2007-2009, 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.net.UrlUtils;
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
	private final int redirectType;
	private final String description;
	private final String keywords;
	private final String navImageAlt;
	private final String title;

	/**
	 * Performs a redirect.
	 *
	 * @param  path  the context-relative path, with a preceeding slash (/)
	 */
	public RedirectWebPage(LoggerAccessor logAccessor, ServletContext context, WebPage parent, String path, int redirectType, String description, String keywords, String navImageAlt, String title) {
		super(logAccessor);
		setServletContext(context);
		this.parent = parent;
		this.path = path;
		this.redirectType = redirectType;
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
		// TODO: These both return path, what was the intent here?
		if(UrlUtils.isScheme(path, "http") || UrlUtils.isScheme(path, "https")) return path;
		return path;
	}

	@Override
	public int getRedirectType() {
		return redirectType;
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
