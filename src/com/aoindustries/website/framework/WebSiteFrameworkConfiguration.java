package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.profiler.*;
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
        Profiler.startProfile(Profiler.IO, WebSiteFrameworkConfiguration.class, "getProperty(String)", null);
        try {
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
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }
    
    public static boolean getLogToSystemErr() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getLogToSystemErr()", null);
        try {
            return "true".equals(getProperty("com.aoindustries.website.framework.log_to_system_err"));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String getErrorSmtpServer() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getErrorSmtpServer()", null);
        try {
            return getProperty("com.aoindustries.website.framework.error.smtp.server");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String getErrorFromAddress() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getErrorFromAddress()", null);
        try {
            return getProperty("com.aoindustries.website.framework.error.email.from");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String getErrorSubject() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getErrorSubject()", null);
        try {
            return getProperty("com.aoindustries.website.framework.error.email.subject");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static List<String> getErrorToAddresses() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getErrorToAddresses()", null);
        try {
            return StringUtility.splitStringCommaSpace(getProperty("com.aoindustries.website.framework.error.email.to"));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String getServletDirectory() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getServletDirectory()", null);
        try {
            return getProperty("com.aoindustries.website.framework.directory.servlet");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static boolean useWebSiteCaching() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "useWebSiteCaching()", null);
        try {
            return "true".equals(getProperty("com.aoindustries.website.framework.use_website_caching"));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String[] getStaticClassPrefixes() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getStaticClassPrefixes()", null);
        try {
            return StringUtility.splitString(getProperty("com.aoindustries.website.framework.classloader.static"), ',');
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String[] getDynamicClassPrefixes() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getDynamicClassPrefixes()", null);
        try {
            return StringUtility.splitString(getProperty("com.aoindustries.website.framework.classloader.dynamic"), ',');
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static File getFileUploadDirectory() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getFileUploadDirectory()", null);
        try {
            return new File(getProperty("com.aoindustries.website.framework.file_upload.directory"));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static int getMaxFileUploadSize() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getMaxFileUploadSize()", null);
        try {
            return Integer.parseInt(getProperty("com.aoindustries.website.framework.file_upload.max_size"));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String getHttpBase() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getHttpBase()", null);
        try {
            return getProperty("com.aoindustries.website.framework.http.base");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static String getHttpsBase() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getHttpsBase()", null);
        try {
            return getProperty("com.aoindustries.website.framework.https.base");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
    
    public static boolean getEnforceSecureMode() throws IOException {
        Profiler.startProfile(Profiler.FAST, WebSiteFrameworkConfiguration.class, "getEnforceSecureMode()", null);
        try {
            return !"false".equals(getProperty("com.aoindustries.website.framework.enforce_secure_mode"));
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}
