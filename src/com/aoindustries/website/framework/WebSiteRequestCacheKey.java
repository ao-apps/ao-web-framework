package com.aoindustries.website.framework;

/*
 * Copyright 2004-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */

/**
 * @see  WebSiteRequest#getOutputCacheKey()
 *
 * @author  AO Industries, Inc.
 */
public class WebSiteRequestCacheKey {

    public final String layout;

    public WebSiteRequestCacheKey(WebSiteRequest req) {
        String myLayout=(String)req.getSession().getAttribute("layout");
        if(myLayout==null) myLayout=req.isLynx() || req.isBlackBerry() ? "Text" : "Default";
        this.layout=myLayout;
    }
    
    @Override
    public int hashCode() {
        return layout.hashCode();
    }
    
    @Override
    public boolean equals(Object O) {
        if(O==null) return false;
        if(!(O instanceof WebSiteRequestCacheKey)) return false;
        WebSiteRequestCacheKey otherKey=(WebSiteRequestCacheKey)O;
        return
            layout.equals(otherKey.layout)
        ;
    }
}
