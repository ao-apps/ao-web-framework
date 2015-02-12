/*
 * Copyright 2006-2009, 2015 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
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
