/*
 * Copyright 2000-2013, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
package com.aoindustries.website.framework;

import com.aoindustries.util.PropertiesUtils;
import com.aoindustries.util.StringUtility;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * The configuration parameters for the web site are stored in a properties file.
 * These properties are not reloaded if changed.
 *
 * @author  AO Industries, Inc.
 */
public final class WebSiteFrameworkConfiguration {

	private WebSiteFrameworkConfiguration() {}

	private static Properties props;

	private static String getProperty(String name) throws IOException {
		if (props == null) {
			props = PropertiesUtils.loadFromResource(WebSiteFrameworkConfiguration.class, "website-framework.properties");
		}
		return props.getProperty(name);
	}

	public static String getServletDirectory() throws IOException {
		return getProperty("com.aoindustries.website.framework.directory.servlet");
	}

	public static boolean useWebSiteCaching() throws IOException {
		return "true".equals(getProperty("com.aoindustries.website.framework.use_website_caching"));
	}

	public static String[] getStaticClassPrefixes() throws IOException {
		List<String> split = StringUtility.splitString(getProperty("com.aoindustries.website.framework.classloader.static"), ',');
		return split.toArray(new String[split.size()]);
	}

	public static String[] getDynamicClassPrefixes() throws IOException {
		List<String> split = StringUtility.splitString(getProperty("com.aoindustries.website.framework.classloader.dynamic"), ',');
		return split.toArray(new String[split.size()]);
	}

	public static File getFileUploadDirectory() throws IOException {
		return new File(getProperty("com.aoindustries.website.framework.file_upload.directory"));
	}

	public static int getMaxFileUploadSize() throws IOException {
		return Integer.parseInt(getProperty("com.aoindustries.website.framework.file_upload.max_size"));
	}

	public static String getBase() throws IOException {
		return getProperty("com.aoindustries.website.framework.base");
	}
}
