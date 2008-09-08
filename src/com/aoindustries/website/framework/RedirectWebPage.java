package com.aoindustries.website.framework;

/*
 * Copyright 2007-2008 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Redirects to the configured URL.
 *
 * @author  AO Industries, Inc.
 */
public class RedirectWebPage extends WebPage {

    private WebPage parent;
    private String path;
    private boolean useEncryption;
    private String description;
    private String keywords;
    private String navImageAlt;
    private String title;

    /**
     * Performs a redirect.
     *
     * @param  path  the path relative to the top of the application, without a preceeding slash (/)
     */
    public RedirectWebPage(ServletContext context, WebPage parent, String path, boolean useEncryption, String description, String keywords, String navImageAlt, String title) {
        setServletContext(context);
        this.parent = parent;
        this.path = path;
        this.useEncryption = useEncryption;
        this.description = description;
        this.keywords = keywords;
        this.navImageAlt = navImageAlt;
        this.title = title;
    }

    protected WebSiteRequest getWebSiteRequest(HttpServletRequest req) throws IOException, SQLException {
        return new WebSiteRequest(this, req);
    }

    public WebPage getParent() {
        return parent;
    }

    public boolean useEncryption() {
        return useEncryption;
    }

    public String getRedirectURL(WebSiteRequest req) throws IOException {
        String lowerPath = path.toLowerCase();
        if(lowerPath.startsWith("http:") || lowerPath.startsWith("https:")) return path;
        return
            (
                useEncryption
                ? WebSiteFrameworkConfiguration.getHttpsBase()
                : WebSiteFrameworkConfiguration.getHttpBase()
            )+path;
    }

    public String getURLPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getNavImageAlt(WebSiteRequest req) {
        return navImageAlt;
    }

    public String getTitle() {
        return title;
    }
}
