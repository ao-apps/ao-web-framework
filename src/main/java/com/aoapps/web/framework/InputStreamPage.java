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

import com.aoapps.html.any.AnyDocument;
import com.aoapps.html.any.Content;
import com.aoapps.html.servlet.ContentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.lang.io.IoUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Reads everything from an input stream and puts it into a page.
 * The input stream must be encoded as {@link AnyDocument#ENCODING}.
 *
 * @author  AO Industries, Inc.
 */
public abstract class InputStreamPage extends WebPage {

  private static final long serialVersionUID = 1L;

  @Override
  @SuppressWarnings("unchecked")
  public <__ extends FlowContent<__>> void doGet(
    WebSiteRequest req,
    HttpServletResponse resp,
    WebPageLayout layout,
    __ flow
  ) throws ServletException, IOException {
    layout.content(req, resp, this, flow, content -> {
      layout.contentTitle(req, resp, this, content);
      layout.contentHorizontalDivider(req, resp, content);
      FlowContent<?> contentLine = layout.startContentLine(req, resp, content);
      try (InputStream in = getInputStream()) {
        contentLine = printStream(req, resp, layout, content, (FlowContent)contentLine, in);
      }
      layout.endContentLine(req, resp, contentLine);
    });
  }

  /**
   * Gets the stream that the text should be read from.
   */
  public abstract InputStream getInputStream() throws IOException;

  /**
   * @return  The current {@code contentLine}, which may have been replaced by a call to
   *          {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
   *          or {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
   */
  public <__ extends FlowContent<__>> __ printStream(
    WebSiteRequest req,
    HttpServletResponse resp,
    WebPageLayout layout,
    ContentEE<?> content,
    __ contentLine,
    InputStream in
  ) throws ServletException, IOException {
    printStreamStatic(contentLine, in);
    return contentLine;
  }

  @SuppressWarnings("deprecation")
  public static void printStreamStatic(Content<?, ?> content, InputStream in) throws IOException {
    IoUtils.copy(new InputStreamReader(in, AnyDocument.ENCODING), content.getRawUnsafe());
  }
}
