package com.aoindustries.website.framework;

/*
 * Copyright 2000-2010 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
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

    public FilePage(LoggerAccessor loggerAccessor) {
        super(loggerAccessor);
    }

    public FilePage(WebSiteRequest req) {
	super(req);
    }

    public FilePage(LoggerAccessor loggerAccessor, Object param) {
	super(loggerAccessor, param);
    }

    @Override
    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        printFile(out, getFile());
    }

    /**
     * Gets the file that the text should be read from.
     */
    public abstract File getFile() throws IOException;

    @Override
    public long getLastModified(WebSiteRequest req) throws IOException, SQLException {
        return Math.max(super.getLastModified(req), getFile().lastModified());
    }

    public static void printFile(ChainWriter out, File file) throws IOException {
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
    }
}