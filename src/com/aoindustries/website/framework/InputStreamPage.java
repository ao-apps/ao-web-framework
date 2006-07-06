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
import javax.servlet.http.*;

/**
 * Reads everything from an input stream and puts it into a page.
 *
 * @author  AO Industries, Inc.
 */
abstract public class InputStreamPage extends WebPage {

    public InputStreamPage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, InputStreamPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public InputStreamPage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, InputStreamPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public InputStreamPage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, InputStreamPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public void doGet(
	ChainWriter out,
	WebSiteRequest req,
	HttpServletResponse resp
    ) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, InputStreamPage.class, "doGet(ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            WebPageLayout layout=getWebPageLayout(req);
            layout.startContent(out, req, 1, getPreferredContentWidth(req));
            try {
                layout.printContentTitle(out, req, this, 1);
                layout.printContentHorizontalDivider(out, req, 1, false);
                layout.startContentLine(out, req, 1, null);
                try {
                    InputStream in=getInputStream();
                    try {
                        printStream(out, req, in);
                    } finally {
                        in.close();
                    }
                } finally {
                    layout.endContentLine(out, req, 1, false);
                }
            } finally {
                layout.endContent(this, out, req, 1);
            }
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the stream that the text should be read from.
     */
    public abstract InputStream getInputStream() throws IOException;

    public void printStream(ChainWriter out, WebSiteRequest req, InputStream in) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, InputStreamPage.class, "printStream(ChainWriter,WebSiteRequest,InputStream)", null);
        try {
            printStreamStatic(out, in);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    public static void printStreamStatic(ChainWriter out, InputStream in) throws IOException {
        Profiler.startProfile(Profiler.IO, InputStreamPage.class, "printStreamStatic(ChainWriter,InputStream)", null);
        try {
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
            Profiler.endProfile(Profiler.IO);
        }
    }
}
