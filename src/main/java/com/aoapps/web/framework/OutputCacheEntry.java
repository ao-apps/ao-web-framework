/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2004-2009, 2015, 2016, 2021, 2022  AO Industries, Inc.
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

/**
 * One entry in the output cache.
 *
 * @see  WebPage#doGet(WebSiteRequest, HttpServletResponse)
 *
 * @author  AO Industries, Inc.
 */
// TODO: This is no longer used?
public final class OutputCacheEntry {

  public final Object outputCacheKey;

  public final long lastModified;

  public final byte[] bytes;

  /**
   * Creates a new output cache entry.
   */
  public OutputCacheEntry(Object outputCacheKey, long lastModified, byte[] bytes) {
    this.outputCacheKey = outputCacheKey;
    this.lastModified = lastModified;
    this.bytes = bytes;
  }
}
