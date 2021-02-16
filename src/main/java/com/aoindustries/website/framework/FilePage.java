/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoindustries.html.Html;
import com.aoindustries.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

/**
 * Pulls information from a file to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class FilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		Html html,
		WebPageLayout layout
	) throws IOException, SQLException {
		printFile(html, getFile());
	}

	/**
	 * Gets the file that the text should be read from.
	 */
	public abstract File getFile() throws IOException;

	@Override
	public long getLastModified(WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
		return Math.max(super.getLastModified(req, resp), getFile().lastModified());
	}

	public static void printFile(Html html, File file) throws IOException {
		FileUtils.copy(file, html.out);
	}
}
