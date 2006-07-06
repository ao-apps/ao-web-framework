package com.aoindustries.website.framework;

/*
 * Copyright 2000-2006 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import com.aoindustries.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Pulls information from a file to be used as the content.
 *
 * @author  AO Industries, Inc.
 */
abstract public class FilePage extends WebPage {

    public FilePage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, FilePage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public FilePage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, FilePage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public FilePage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, FilePage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, FilePage.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            printFile(out, getFile());
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the file that the text should be read from.
     */
    public abstract File getFile() throws IOException;

    public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, FilePage.class, "getLastModified(WebSiteRequest)", null);
        try {
            return Math.max(super.getLastModified(req), getFile().lastModified());
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static void printFile(ChainWriter out, File file) throws IOException {
        Profiler.startProfile(Profiler.IO, FilePage.class, "printFile(ChainWriter,File)", null);
        try {
            InputStream in = new FileInputStream(file);
            try {
                byte[] bytes = BufferManager.getBytes();
                try {
                    char[] chars = BufferManager.getChars();
                    try {
                        int ret;
                        while ((ret = in.read(bytes, 0, BufferManager.BUFFER_SIZE)) != -1) {
                            for (int c = 0; c < ret; c++) chars[c] = (char) bytes[c];
                            out.write(chars, 0, ret);
                        }
                    } finally {
                        BufferManager.release(chars);
                    }
                } finally {
                    BufferManager.release(bytes);
                }
            } finally {
                in.close();
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}