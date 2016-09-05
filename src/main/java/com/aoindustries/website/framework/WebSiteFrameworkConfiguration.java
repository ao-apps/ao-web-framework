/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016  AO Industries, Inc.
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
}
