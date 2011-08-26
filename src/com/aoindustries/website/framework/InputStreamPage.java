package com.aoindustries.website.framework;

/*
 * Copyright 2000-2011 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Reads everything from an input stream and puts it into a page.
 *
 * @author  AO Industries, Inc.
 */
abstract public class InputStreamPage extends WebPage {

    public InputStreamPage(LoggerAccessor loggerAccessor) {
        super(loggerAccessor);
    }

    public InputStreamPage(WebSiteRequest req) {
    	super(req);
    }

    public InputStreamPage(LoggerAccessor loggerAccessor, Object param) {
        super(loggerAccessor, param);
    }

    @Override
    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        WebPageLayout layout=getWebPageLayout(req);
        layout.startContent(out, req, resp, 1, getPreferredContentWidth(req));
        try {
            layout.printContentTitle(out, req, resp, this, 1);
            layout.printContentHorizontalDivider(out, req, resp, 1, false);
            layout.startContentLine(out, req, resp, 1, null, null);
            try {
                InputStream in=getInputStream();
                try {
                    printStream(out, req, resp, in);
                } finally {
                    in.close();
                }
            } finally {
                layout.endContentLine(out, req, resp, 1, false);
            }
        } finally {
            layout.endContent(this, out, req, resp, 1);
        }
    }

    /**
     * Gets the stream that the text should be read from.
     */
    public abstract InputStream getInputStream() throws IOException;

    public void printStream(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, InputStream in) throws IOException, SQLException {
        printStreamStatic(out, in);
    }

    /**
     * @deprecated  This doesn't do any character conversion - assumes ISO8859-1.
     */
    @Deprecated
    public static void printStreamStatic(ChainWriter out, InputStream in) throws IOException {
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
