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

import java.io.File;
import javax.activation.FileTypeMap;
import javax.servlet.ServletContext;

/**
 * @author  AO Industries, Inc.
 */
final public class UploadedFileTypeMap extends FileTypeMap {

	final private WebSiteUser owner;
	final private ServletContext context;
	final private LoggerAccessor loggerAccessor;

	public UploadedFileTypeMap(WebSiteUser owner, ServletContext context, LoggerAccessor loggerAccessor) {
		this.owner=owner;
		this.context=context;
		this.loggerAccessor = loggerAccessor;
	}

	@Override
	public String getContentType(File file) {
		return getContentType(file.getName());
	}

	@Override
	public String getContentType(String filename) {
		int pos=filename.lastIndexOf('/');
		if(pos==-1) pos=filename.lastIndexOf('\\');
		if(pos!=-1) filename=filename.substring(pos+1);
		long id=Long.parseLong(filename);
		UploadedFile uf=WebSiteRequest.getUploadedFile(owner, id, context, loggerAccessor);
		if(uf==null) throw new NullPointerException("Unable to find uploaded file: "+id);
		return uf.getContentType();
	}
}
