package com.aoindustries.website.framework;

/*
 * Copyright 2000-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import com.aoindustries.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;

/**
 * Pulls the content from a file with the same name and location as the <code>.class</code>
 * and <code>.java</code> but with a <code>.html</code> extension.  As the file is being
 * sent to the client, any <code>href='@<i>classname</i>'</code> URL is rewritten and
 * maintains the current <code>WebSiteRequest</code> parameters.
 *
 * @author  AO Industries, Inc.
 */
public abstract class HTMLInputStreamPage extends InputStreamPage {

    public HTMLInputStreamPage() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, HTMLInputStreamPage.class, "<init>()", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public HTMLInputStreamPage(WebSiteRequest req) {
	super(req);
        Profiler.startProfile(Profiler.INSTANTANEOUS, HTMLInputStreamPage.class, "<init>(WebSiteRequest)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public HTMLInputStreamPage(Object param) {
	super(param);
        Profiler.startProfile(Profiler.INSTANTANEOUS, HTMLInputStreamPage.class, "<init>(Object)", null);
        Profiler.endProfile(Profiler.INSTANTANEOUS);
    }

    public void printStream(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, InputStream in) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, HTMLInputStreamPage.class, "printStream(ChainWriter,WebSiteRequest,HttpServletResponse,InputStream)", null);
        try {
            printHTMLStream(out, req, resp, getWebPageLayout(req), in, "ao_light_link");
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the file that the text should be read from.
     */
    public InputStream getInputStream() throws IOException {
        Profiler.startProfile(Profiler.FAST, HTMLInputStreamPage.class, "getInputStream()", null);
        try {
            return getHTMLInputStream(getClass());
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Gets the HTML file with the same name as the provided Class.
     */
    public static InputStream getHTMLInputStream(Class clazz) throws IOException {
        Profiler.startProfile(Profiler.IO, HTMLInputStreamPage.class, "getHTMLInputStream(Class)", null);
        try {
            return HTMLInputStreamPage.class.getResourceAsStream('/'+clazz.getName().replace('.', '/')+".html");
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    /**
     * Prints HTML content, parsing for special <code>@</code> tags.  Types of tags include:
     * <UL>
     *   <LI>@URL(classname)    Loads a WebPage of the given class and builds a URL to it</LI>
     *   <LI>@BEGIN_LIGHT_AREA  Calls <code>layout.beginLightArea(ChainWriter)</code></LI>
     *   <LI>@END_LIGHT_AREA    Calls <code>layout.endLightArea(ChainWriter)</code></LI>
     *   <LI>@END_CONTENT_LINE  Calls <code>layout.endContentLine</code></LI>
     *   <LI>@PRINT_CONTENT_HORIZONTAL_DIVIDER  Calls <code>layout.printContentHorizontalDivider</code></LI>
     *   <LI>@START_CONTENT_LINE  Calls <code>layout.startContentLine</code></LI>
     *   <LI>@LINK_CLASS        The preferred link class for this element</LI>
     * </UL>
     */
    public static void printHTML(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPageLayout layout, String html, String linkClass) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, HTMLInputStreamPage.class, "printHTML(ChainWriter,WebSiteRequest,HttpServletResponse,WebPageLayout,String,String)", null);
        try {
            if(req==null) out.print(html);
            else {
                int len=html.length();
                int pos=0;
                while(pos<len) {
                    char ch=html.charAt(pos++);
                    if(ch=='@') {
                        if((pos+4)<len && html.substring(pos, pos+4).equalsIgnoreCase("URL(")) {
                            int endPos=html.indexOf(')', pos+4);
                            if(endPos==-1) throw new IllegalArgumentException("Unable to find closing parenthesis for @URL( substitution, pos="+pos);
                            String className=html.substring(pos+4, endPos);
                            out.writeHtmlAttribute(resp.encodeURL(req.getURL(className)));
                            pos=endPos+1;
                        } else if((pos+16)<len && html.substring(pos, pos+16).equalsIgnoreCase("BEGIN_LIGHT_AREA")) {
                            layout.beginLightArea(req, resp, out);
                            pos+=16;
                        } else if((pos+14)<len && html.substring(pos, pos+14).equalsIgnoreCase("END_LIGHT_AREA")) {
                            layout.endLightArea(req, resp, out);
                            pos+=14;
                        } else if((pos+16)<len && html.substring(pos, pos+16).equalsIgnoreCase("END_CONTENT_LINE")) {
                            layout.endContentLine(out, req, resp, 1, false);
                            pos+=16;
                        } else if((pos+32)<len && html.substring(pos, pos+32).equalsIgnoreCase("PRINT_CONTENT_HORIZONTAL_DIVIDER")) {
                            layout.printContentHorizontalDivider(out, req, resp, 1, false);
                            pos+=32;
                        } else if((pos+18)<len && html.substring(pos, pos+18).equalsIgnoreCase("START_CONTENT_LINE")) {
                            layout.startContentLine(out, req, resp, 1, null);
                            pos+=18;
                        } else if((pos+10)<len && html.substring(pos, pos+10).equalsIgnoreCase("LINK_CLASS")) {
                            out.print(linkClass==null?"ao_light_link":linkClass);
                            pos+=10;
                        } else out.print('@');
                    } else {
                        out.print(ch);
                    }
                }
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    private static final String[] tags={
        "@PRINT_CONTENT_HORIZONTAL_DIVIDER",
        "@START_CONTENT_LINE",
        "@BEGIN_LIGHT_AREA",
        "@END_CONTENT_LINE",
        "@END_LIGHT_AREA",
        "@LINK_CLASS",
        "@URL"
    };

    /**
     * @see  #printHTML
     */
    public static void printHTMLStream(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPageLayout layout, InputStream in, String linkClass) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, HTMLInputStreamPage.class, "printHTMLStream(ChainWriter,WebSiteRequest,HttpServletResponse,WebPageLayout,InputStream,String)", null);
        try {
            if(in==null) throw new NullPointerException("in is null");
            if(req==null) {
                byte[] bytes=BufferManager.getBytes();
                try {
                    char[] chars=BufferManager.getChars();
                    try {
                        int ret;
                        while((ret=in.read(bytes, 0, BufferManager.BUFFER_SIZE))!=-1) {
                            for(int c=0;c<ret;c++) chars[c]=(char)bytes[c];
                            out.write(chars, 0, ret);
                        }
                    } finally {
                        BufferManager.release(chars);
                    }
                } finally {
                    BufferManager.release(bytes);
                }
            } else {
                StringBuilder buffer=null;
                int ch;
                while((ch=in.read())!=-1) {
                    if(ch=='@') {
                        if(buffer==null) buffer=new StringBuilder();
                        // Read until a tag is matched, or until a tag cannot be matched
                        buffer.append('@');
                    Loop:
                        while((ch=in.read())!=-1) {
                            // If @ found, print buffer and reset for next tag
                            if(ch=='@') {
                                out.print(buffer.toString());
                                buffer.setLength(0);
                                buffer.append('@');
                            } else {
                                buffer.append((char)ch);
                                String tagPart=buffer.toString();
                                // Does one of the tags begin with or match this tag
                                boolean found=false;
                                for(int c=0;c<tags.length;c++) {
                                    String tag=tags[c];
                                    if(tag.length()>=tagPart.length()) {
                                        if(tags[c].equalsIgnoreCase(tagPart)) {
                                            if(c==0) layout.printContentHorizontalDivider(out, req, resp, 1, false);
                                            else if(c==1) layout.startContentLine(out, req, resp, 1, null);
                                            else if(c==2) layout.beginLightArea(req, resp, out);
                                            else if(c==3) layout.endContentLine(out, req, resp, 1, false);
                                            else if(c==4) layout.endLightArea(req, resp, out);
                                            else if(c==5) out.print(linkClass==null?"ao_light_link":linkClass);
                                            else if(c==6) {
                                                // Read up to a ')'
                                                while((ch=in.read())!=-1) {
                                                    if(ch==')') {
                                                        String className=buffer.toString().substring(5, buffer.length());
                                                        out.writeHtmlAttribute(resp.encodeURL(req.getURL(className)));
                                                        buffer.setLength(0);
                                                        break;
                                                    } else buffer.append((char)ch);
                                                }
                                                if(buffer.length()>0) throw new IllegalArgumentException("Unable to find closing parenthesis for @URL( substitution, buffer="+buffer.toString());
                                            } else throw new RuntimeException("This index should not be used because it is biffer than tags.length");
                                            buffer.setLength(0);
                                            break Loop;
                                        } else if(tags[c].toUpperCase().startsWith(tagPart.toUpperCase())) {
                                            found=true;
                                            break;
                                        }
                                    } else {
                                        // Sorted with longest first, can break here
                                        break;
                                    }
                                }
                                if(!found) {
                                    out.print(tagPart);
                                    buffer.setLength(0);
                                    break Loop;
                                }
                            }
                        }
                    } else out.print((char)ch);
                }
            }
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }

    public Object getOutputCacheKey(WebSiteRequest req) {
        Profiler.startProfile(Profiler.FAST, HTMLInputStreamPage.class, "getOutputCacheKey(WebSiteRequest)", null);
        try {
            return req.getOutputCacheKey();
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }
}