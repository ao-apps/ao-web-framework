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

import com.aoapps.html.any.Content;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.lang.io.FileUtils;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Pulls information from a file to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
public abstract class FilePage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Override
	public <__ extends FlowContent<__>> void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		__ flow
	) throws ServletException, IOException {
		printFile(flow, getFile());
	}

	/**
	 * Gets the file that the text should be read from.
	 */
	public abstract File getFile() throws IOException;

	@Override
	public long getLastModified(WebSiteRequest req, HttpServletResponse resp) throws ServletException {
		try {
			return Math.max(super.getLastModified(req, resp), getFile().lastModified());
		} catch(IOException e) {
			throw new ServletException(e);
		}
	}

	@SuppressWarnings("deprecation")
	public static void printFile(Content<?, ?> content, File file) throws IOException {
		FileUtils.copy(file, content.getRawUnsafe());
	}
}
