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

import com.aoindustries.html.Content;
import com.aoindustries.html.Document;
import com.aoindustries.html.FlowContent;
import com.aoindustries.io.IoUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Reads everything from an input stream and puts it into a page.
 * The input stream must be encoded as {@link Document#ENCODING}.
 *
 * @author  AO Industries, Inc.
 */
abstract public class InputStreamPage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		FlowContent<?> flow
	) throws ServletException, IOException {
		Document document = flow.getDocument();
		layout.startContent(document, req, resp, 1, getPreferredContentWidth(req));
		layout.printContentTitle(document, req, resp, this, 1);
		layout.printContentHorizontalDivider(document, req, resp, 1, false);
		layout.startContentLine(document, req, resp, 1, null, null);
		try (InputStream in = getInputStream()) {
			printStream(document, req, resp, in);
		}
		layout.endContentLine(document, req, resp, 1, false);
		layout.endContent(this, document, req, resp, 1);
	}

	/**
	 * Gets the stream that the text should be read from.
	 */
	public abstract InputStream getInputStream() throws IOException;

	public void printStream(FlowContent<?> flow, WebSiteRequest req, HttpServletResponse resp, InputStream in) throws ServletException, IOException {
		printStreamStatic(flow, in);
	}

	public static void printStreamStatic(Content<?> content, InputStream in) throws IOException {
		IoUtils.copy(new InputStreamReader(in, Document.ENCODING), content.getDocument().out);
	}
}
