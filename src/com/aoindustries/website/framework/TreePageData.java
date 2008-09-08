package com.aoindustries.website.framework;

/*
 * Copyright 2006-2008 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.Profiler;

/**
 * @author  AO Industries, Inc.
 */
public class TreePageData {

    private String path;
    private String url;
    private String description;

    public TreePageData(String path, String url, String description) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, TreePageData.class, "<init>(String,String,String)", null);
        try {
            this.path=path;
            this.url=url;
            this.description=description;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getPath() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, TreePageData.class, "getPath()", null);
        try {
            return path;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getUrl() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, TreePageData.class, "getUrl()", null);
        try {
            return url;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    public String getDescription() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, TreePageData.class, "getDescription()", null);
        try {
            return description;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }
}
