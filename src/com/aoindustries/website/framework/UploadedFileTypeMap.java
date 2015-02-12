/*
 * Copyright 2000-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
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
