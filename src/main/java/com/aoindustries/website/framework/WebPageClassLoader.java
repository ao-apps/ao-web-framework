/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2002-2013, 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.io.AoByteArrayOutputStream;
import com.aoindustries.io.IoUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author  AO Industries, Inc.
 */
public final class WebPageClassLoader extends ClassLoader {

	/**
	 * The time that the classloader was instantiated.
	 */
	private static final long uptime = System.currentTimeMillis();

	/**
	 * Gets the time the classloader was loaded.
	 */
	public static long getUptime() {
		return uptime;
	}

	// The list of prefixes to not be dynamic first
	private final String[] staticPrefixes;

	// Then the list of dynamicly loaded prefixes
	private final String[] dynamicPrefixes;

	public WebPageClassLoader() throws IOException {
		staticPrefixes = WebSiteFrameworkConfiguration.getStaticClassPrefixes();
		dynamicPrefixes = WebSiteFrameworkConfiguration.getDynamicClassPrefixes();
	}

	// This instance is reused for speed
	private final AoByteArrayOutputStream bytesOut=new AoByteArrayOutputStream();

	@Override
	protected URL findResource(String name) {
		return getSystemResource(name);
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		return getSystemResources(name);
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);
		if(c==null) {
			// Determine if it should be loaded dynamically
			boolean isStatic=false;
			for (String staticPrefix : staticPrefixes) {
				if (name.startsWith(staticPrefix)) {
					isStatic=true;
					break;
				}
			}
			boolean isDynamic=false;
			if(!isStatic) {
				for (String dynamicPrefix : dynamicPrefixes) {
					if (name.startsWith(dynamicPrefix)) {
						isDynamic=true;
						//found=true;
						break;
					}
				}
			}
			if(isDynamic) {
				try {
					String resourceName=name.replace('.', '/')+".class";
					InputStream in=getSystemResourceAsStream(resourceName);
					if(in==null) throw new IllegalArgumentException("Unable to find SystemResource: "+resourceName);
					try {
						bytesOut.reset();
						IoUtils.copy(in, bytesOut);
					} finally {
						in.close();
					}

					c=defineClass(name, bytesOut.getInternalByteArray(), 0, bytesOut.size());
				} catch (IOException err) {
					throw new ClassNotFoundException("Unable to load class: "+name, err);
				}
			} else c = findSystemClass(name);
		}

		if (resolve) resolveClass(c);
		return c;
	}
}
