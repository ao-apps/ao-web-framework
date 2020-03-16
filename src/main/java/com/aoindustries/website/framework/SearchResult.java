/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2006-2009, 2015, 2016  AO Industries, Inc.
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

/**
 * Automatically generates a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
public class SearchResult implements Comparable<SearchResult> {

	private final String url;
	private final float probability;
	private final String title;
	private final String description;
	private final String author;

	public SearchResult(
		String url,
		float probability,
		String title,
		String description,
		String author
	) {
		this.url=url;
		this.probability=probability;
		this.title=title;
		this.description=description;
		this.author=author;
	}

	public String getUrl() {
		return url;
	}

	public float getProbability() {
		return probability;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getAuthor() {
		return author;
	}

	@Override
	public int compareTo(SearchResult other) {
		return Float.compare(other.probability, probability);
	}
}
