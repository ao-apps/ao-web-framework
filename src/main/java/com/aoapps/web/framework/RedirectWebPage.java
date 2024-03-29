/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2007-2009, 2015, 2016, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects to the configured URL.
 *
 * @author  AO Industries, Inc.
 */
public class RedirectWebPage extends WebPage {

  private static final long serialVersionUID = 1L;

  private final WebPage parent;
  private final String path;
  private final int redirectType;
  private final String description;
  private final String keywords;
  private final String navImageAlt;
  private final String title;

  /**
   * Performs a redirect.
   *
   * @param  path  the context-relative path, with a preceding slash (/)
   */
  public RedirectWebPage(ServletContext context, WebPage parent, String path, int redirectType, String description, String keywords, String navImageAlt, String title) {
    super();
    setServletContext(context);
    this.parent = parent;
    this.path = path;
    this.redirectType = redirectType;
    this.description = description;
    this.keywords = keywords;
    this.navImageAlt = navImageAlt;
    this.title = title;
  }

  @Override
  protected WebSiteRequest getWebSiteRequest(HttpServletRequest req) throws ServletException {
    return new WebSiteRequest(this, req);
  }

  @Override
  public WebPage getParent() {
    return parent;
  }

  /**
   * Never do GET, redirect-only.
   */
  @Override
  public final void doGet(
      WebSiteRequest req,
      HttpServletResponse resp,
      DocumentEE document
  ) throws ServletException, IOException {
    // resp null during search
    if (resp != null) {
      resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
  }

  /**
   * Never do GET, redirect-only.
   */
  @Override
  public final <__ extends FlowContent<__>> void doGet(
      WebSiteRequest req,
      HttpServletResponse resp,
      WebPageLayout layout,
      __ flow
  ) throws ServletException, IOException {
    // resp null during search
    if (resp != null) {
      resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
  }

  /**
   * Never do POST, redirect-only.
   */
  @Override
  public final void doPost(
      WebSiteRequest req,
      HttpServletResponse resp,
      DocumentEE document
  ) throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  /**
   * Never do POST, redirect-only.
   */
  @Override
  public final <__ extends FlowContent<__>> void doPost(
      WebSiteRequest req,
      HttpServletResponse resp,
      WebPageLayout layout,
      __ flow
  ) throws ServletException, IOException {
    resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
  }

  @Override
  public String getRedirectUrl(WebSiteRequest req) throws ServletException {
    return path;
  }

  @Override
  public int getRedirectType() {
    return redirectType;
  }

  @Override
  public String getUrlPath() {
    return path;
  }

  @Override
  public String getDescription(WebSiteRequest req) {
    return description;
  }

  @Override
  public String getKeywords(WebSiteRequest req) {
    return keywords;
  }

  @Override
  public String getNavImageAlt(WebSiteRequest req) {
    return navImageAlt;
  }

  @Override
  public String getTitle(WebSiteRequest req) {
    return title;
  }
}
