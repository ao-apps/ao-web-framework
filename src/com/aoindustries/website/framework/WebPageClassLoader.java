package com.aoindustries.website.framework;

/*
 * Copyright 2002-2010 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author  AO Industries, Inc.
 */
public final class WebPageClassLoader extends ClassLoader {

    /**
     * The time that the classloader was instantiated.
     */
    private static long uptime=System.currentTimeMillis();

    /**
     * Gets the time the classloader was loaded.
     */
    public static long getUptime() {
        return uptime;
    }

    // The list of prefixes to not be dynamic first
    private final String[] staticPrefixes;
    
    // Then the list of dynamicly loaded prefixes
    private final String[] dynamicPrefixes;
    
    public WebPageClassLoader() throws IOException {
        staticPrefixes=WebSiteFrameworkConfiguration.getStaticClassPrefixes();
        dynamicPrefixes=WebSiteFrameworkConfiguration.getDynamicClassPrefixes();
    }

    // This instance is reused for speed
    private final ByteArrayOutputStream bytesOut=new ByteArrayOutputStream();

    @Override
    protected URL findResource(String name) {
        return getSystemResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return getSystemResources(name);
    }

    @Override
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name);
        if(c==null) {
            // Determine if it should be loaded dynamically
            boolean dynamic=false;
            boolean found=false;
            for(int d=0;d<staticPrefixes.length;d++) {
                if(name.startsWith(staticPrefixes[d])) {
                    found=true;
                    break;
                }
            }
            if(!found) {
                for(int d=0;d<dynamicPrefixes.length;d++) {
                    if(name.startsWith(dynamicPrefixes[d])) {
                        dynamic=true;
                        found=true;
                        break;
                    }
                }
            }
            if(dynamic) {
                try {
                    String resourceName=name.replace('.', '/')+".class";
                    InputStream in=getSystemResourceAsStream(resourceName);
                    try {
                        if(in==null) throw new IllegalArgumentException("Unable to find SystemResource: "+resourceName);
                        bytesOut.reset();

                        byte[] buffer=BufferManager.getBytes();
                        try {
                            int ret;
                            while((ret=in.read(buffer, 0, BufferManager.BUFFER_SIZE))!=-1) bytesOut.write(buffer, 0, ret);
                        } finally {
                            BufferManager.release(buffer);
                        }
                    } finally {
                        if(in!=null) in.close();
                    }

                    byte[] bytes=bytesOut.toByteArray();
                    c=defineClass(name, bytes, 0, bytes.length);
                } catch (IOException err) {
                    throw new ClassNotFoundException("Unable to load class: "+name, err);
                }
            } else c = findSystemClass(name);
        }

        if (resolve) resolveClass(c);
        return c;
    }
}
