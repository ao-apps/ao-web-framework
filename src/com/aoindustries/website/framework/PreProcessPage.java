/*
 * Copyright 2000-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.io.ChainWriter;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;

/**
 * Takes the output of a native process and puts it in a PRE block
 *
 * @author  AO Industries, Inc.
 */
public abstract class PreProcessPage extends ProcessPage {

	private static final long serialVersionUID = 1L;

	public PreProcessPage(LoggerAccessor loggerAccessor) {
		super(loggerAccessor);
	}

	public PreProcessPage(WebSiteRequest req) {
		super(req);
	}

	public PreProcessPage(LoggerAccessor loggerAccessor, Object param) {
		super(loggerAccessor, param);
	}

	@Override
	public void doGet(
		ChainWriter out,
		WebSiteRequest req,
		HttpServletResponse resp
	) throws IOException, SQLException {
		out.println("<pre>");
		super.doGet(out, req, resp);
		out.println("</pre>");
	}
}
