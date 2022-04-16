/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2005-2009, 2015, 2016, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.html.servlet.ContentEE;
import com.aoapps.html.servlet.FlowContent;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically generates the description along with a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
public abstract class DescriptionAutoListPage extends AutoListPage {

	private static final long serialVersionUID = 1L;

	/**
	 * Prints the content that will be put before the auto-generated list.
	 */
	@Override
	public <__ extends FlowContent<__>> __ printContentStart(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		ContentEE<?> content,
		__ contentLine
	) throws ServletException, IOException {
		contentLine.text(getDescription(req));
		return contentLine;
	}
}
