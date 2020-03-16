/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016  AO Industries, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Pulls information from a native process to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class ProcessPage extends InputStreamPage {

	private static final long serialVersionUID = 1L;

	public ProcessPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public ProcessPage(WebSiteRequest req) {
		super(req);
	}

	public ProcessPage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return getProcess().getInputStream();
	}

	@Override
	public long getLastModified(WebSiteRequest req) {
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
	public long getSearchLastModified() throws IOException, SQLException {
		return super.getLastModified(null);
	}
}
