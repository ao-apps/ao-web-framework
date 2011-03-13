package com.aoindustries.website.framework;

/*
 * Copyright 2007-2011 by AO Industries, Inc.,
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

    private static final long serialVersionUID = 1L;
    
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
    public RedirectWebPage(LoggerAccessor logAccessor, ServletContext context, WebPage parent, String path, boolean useEncryption, String description, String keywords, String navImageAlt, String title) {
        super(logAccessor);
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

    @Override
    public boolean useEncryption() {
        return useEncryption;
    }

    @Override
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

    @Override
    public String getURLPath() {
        return path;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getKeywords() {
        return keywords;
    }

    @Override
    public String getNavImageAlt(WebSiteRequest req) {
        return navImageAlt;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
