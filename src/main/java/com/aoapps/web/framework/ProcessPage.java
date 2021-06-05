/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2020, 2021  AO Industries, Inc.
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
 * along with ao-web-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.web.framework;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Pulls information from a native process to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class ProcessPage extends InputStreamPage {

	private static final long serialVersionUID = 1L;

	@Override
	public InputStream getInputStream() throws IOException {
		return getProcess().getInputStream();
	}

	@Override
	public long getLastModified(WebSiteRequest req, HttpServletResponse resp) {
		return -1;
	}

	/**
	 * Gets the process that the contents should be read from
	 */
	public abstract Process getProcess() throws IOException;

	/**
	 * The search format of this page is indexed.
	 */
	@Override
	public long getSearchLastModified() throws ServletException {
		return super.getLastModified(null, null);
	}
}
