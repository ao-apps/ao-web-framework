/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2006-2009, 2015, 2016, 2021  AO Industries, Inc.
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
 * along with ao-web-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.web.framework;

import java.util.Collection;

/**
 * @author  AO Industries, Inc.
 */
public class TreePageData {

	private final String url;
	private final String description;
	private final boolean hasChilren;
	private final String[] path;

	/**
	 * @param path The path of display names.
	 */
	public TreePageData(String url, String description, boolean hasChildren, String ... path) {
		this.url = url;
		this.description = description;
		this.hasChilren = hasChildren;
		this.path = path;
	}

	/**
	 * @param path The path of display names, extracted via {@link Collection#toArray(java.lang.Object[])}
	 */
	public TreePageData(String url, String description, boolean hasChildren, Collection<? extends String> path) {
		this(
			url,
			description,
			hasChildren,
			path.toArray(new String[path.size()])
		);
	}

	public String getUrl() {
		return url;
	}

	public String getDescription() {
		return description;
	}

	public boolean hasChildren() {
		return hasChilren;
	}

	public String[] getPath() {
		return path;
	}
}
