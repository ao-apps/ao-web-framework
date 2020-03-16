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

import com.aoindustries.encoding.ChainWriter;
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

	public FilePage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public FilePage(WebSiteRequest req) {
		super(req);
	}

	public FilePage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		printFile(out, getFile());
	}

	/**
	 * Gets the file that the text should be read from.
	 */
	public abstract File getFile() throws IOException;

	@Override
	public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
		return Math.max(super.getLastModified(req), getFile().lastModified());
	}

	public static void printFile(ChainWriter out, File file) throws IOException {
		FileUtils.copy(file, out);
	}
}
