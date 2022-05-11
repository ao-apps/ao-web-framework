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

import com.aoapps.html.servlet.ContentEE;
import com.aoapps.html.servlet.FlowContent;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Takes the output of a native process and puts it in a PRE block.
 *
 * @author  AO Industries, Inc.
 */
public abstract class PreProcessPage extends ProcessPage {

  private static final long serialVersionUID = 1L;

  @Override
  public <__ extends FlowContent<__>> __ printStream(
      WebSiteRequest req,
      HttpServletResponse resp,
      WebPageLayout layout,
      ContentEE<?> content,
      __ contentLine,
      InputStream in
  ) throws ServletException, IOException {
    contentLine.pre__(pre -> printStreamStatic(pre, in));
    return contentLine;
  }
}
