package com.aoindustries.website.framework;

/*
 * Copyright 2006-2007 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.Profiler;

/**
 * Automatically generates a list of all pages.
 *
 * @author  AO Industries, Inc.
 */
public class SearchResult implements Comparable<SearchResult> {

    private String url;
    private float probability;
    private String title;
    private String description;
    private String author;

    public SearchResult(
        String url,
        float probability,
        String title,
        String description,
        String author
    ) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, SearchResult.class, "<init>(String,float,String,String,String)", null);
        try {
            this.url=url;
            this.probability=probability;
            this.title=title;
            this.description=description;
            this.author=author;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getUrl() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, SearchResult.class, "getUrl()", null);
        try {
            return url;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public float getProbability() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, SearchResult.class, "getProbability()", null);
        try {
            return probability;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getTitle() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, SearchResult.class, "getTitle()", null);
        try {
            return title;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getDescription() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, SearchResult.class, "getDescription()", null);
        try {
            return description;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getAuthor() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, SearchResult.class, "getAuthor()", null);
        try {
            return author;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
    
    public int compareTo(SearchResult other) {
        return Float.compare(other.probability, probability);
    }
}
