package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.Profiler;
import com.aoindustries.util.*;
import java.io.*;
import java.util.*;

/**
 * The configuration parameters for the web site are stored in a properties file.
 * These properties are not reloaded if changed.
 *
 * @author  AO Industries, Inc.
 */
public final class WebSiteFrameworkConfiguration {

    private static Properties props;

    private static String getProperty(String name) throws IOException {
        if (props == null) {
            Properties newProps = new Properties();
            InputStream in = WebSiteFrameworkConfiguration.class.getResourceAsStream("website-framework.properties");
            try {
                newProps.load(in);
            } finally {
                in.close();
            }
            props = newProps;

            // Startup the profiling the first time the configuration is accessed
            Profiler.setProfilerLevel(newProps.getProperty("com.aoindustries.website.profiler.level"));
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
        return StringUtility.splitString(getProperty("com.aoindustries.website.framework.classloader.static"), ',');
    }

    public static String[] getDynamicClassPrefixes() throws IOException {
        return StringUtility.splitString(getProperty("com.aoindustries.website.framework.classloader.dynamic"), ',');
    }

    public static File getFileUploadDirectory() throws IOException {
        return new File(getProperty("com.aoindustries.website.framework.file_upload.directory"));
    }

    public static int getMaxFileUploadSize() throws IOException {
        return Integer.parseInt(getProperty("com.aoindustries.website.framework.file_upload.max_size"));
    }

    public static String getHttpBase() throws IOException {
        return getProperty("com.aoindustries.website.framework.http.base");
    }

    public static String getHttpsBase() throws IOException {
        return getProperty("com.aoindustries.website.framework.https.base");
    }
    
    public static boolean getEnforceSecureMode() throws IOException {
        return !"false".equals(getProperty("com.aoindustries.website.framework.enforce_secure_mode"));
    }
}
