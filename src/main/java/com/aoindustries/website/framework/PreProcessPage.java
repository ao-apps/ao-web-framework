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
import javax.servlet.http.HttpServletResponse;

/**
 * Takes the output of a native process and puts it in a PRE block
 *
 * @author  AO Industries, Inc.
 */
public abstract class PreProcessPage extends ProcessPage {

	private static final long serialVersionUID = 1L;

	public PreProcessPage() {
		super();
	}

	public PreProcessPage(WebSiteRequest req) {
		super(req);
	}

	public PreProcessPage(Object param) {
		super(param);
	}

	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		WebPageLayout layout
	) throws IOException, SQLException {
		out.write("<pre>\n"); // all println to write with \n, all print to write?
		super.doGet(req, resp, out, layout);
		out.write("</pre>\n");
	}
}
