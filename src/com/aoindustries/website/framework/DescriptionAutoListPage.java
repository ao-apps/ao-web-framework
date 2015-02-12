/*
 * Copyright 2005-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.io.ChainWriter;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

/**
 * Automatically generates the description along with a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
abstract public class DescriptionAutoListPage extends AutoListPage {

	private static final long serialVersionUID = 1L;

	public DescriptionAutoListPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public DescriptionAutoListPage(WebSiteRequest req) {
		super(req);
	}

	public DescriptionAutoListPage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	/**
	 * Prints the content that will be put before the auto-generated list.
	 */
	@Override
	public void printContentStart(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		out.print(getDescription());
	}
}
