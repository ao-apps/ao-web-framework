/*
 * Copyright 2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import java.util.logging.Logger;
import javax.servlet.ServletContext;

/**
 * Given a ServletContext and a name should return an appropriate Logger.
 *
 * @author  AO Industries, Inc.
 */
public interface LoggerAccessor {

	Logger getLogger(ServletContext servletContext, String name);
}
