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

import com.aoapps.security.Identifier;
import java.io.File;
import javax.activation.FileTypeMap;
import javax.servlet.ServletContext;

/**
 * @author  AO Industries, Inc.
 */
public final class UploadedFileTypeMap extends FileTypeMap {

  private final WebSiteUser owner;
  private final ServletContext context;

  public UploadedFileTypeMap(WebSiteUser owner, ServletContext context) {
    this.owner=owner;
    this.context=context;
  }

  @Override
  public String getContentType(File file) {
    return getContentType(file.getName());
  }

  @Override
  public String getContentType(String filename) {
    int pos=filename.lastIndexOf('/');
    if (pos == -1) {
      pos=filename.lastIndexOf('\\');
    }
    if (pos != -1) {
      filename=filename.substring(pos+1);
    }
    Identifier id = new Identifier(filename);
    UploadedFile uf = WebSiteRequest.getUploadedFile(owner, id, context);
    if (uf == null) {
      throw new NullPointerException("Unable to find uploaded file: " + id);
    }
    return uf.getContentType();
  }
}
