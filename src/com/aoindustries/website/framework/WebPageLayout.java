package com.aoindustries.website.framework;

/*
 * Copyright 2000-2007 by AO Industries, Inc.,
 * 816 Azalea Rd, Mobile, Alabama, 36693, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import com.aoindustries.profiler.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;

/**
 * A <code>WebPageLayout</code> controls how a <code>WebPage</code> looks while providing a little
 * isolation from the code that provides the functionality.
 *
 * @author  AO Industries, Inc.
 */
abstract public class WebPageLayout {

    /**
     * Directional references.
     */
    public static final int
        NONE=0,
        UP=1,
        DOWN=2,
        UP_AND_DOWN=3
    ;

    private final String[] layoutChoices;
    
    public WebPageLayout(String[] layoutChoices) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "<init>(String[])", null);
        try {
            this.layoutChoices=layoutChoices;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Gets the names of every supported layout.
     */
    final public String[] getLayoutChoices() {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "getLayoutChoices()", null);
        try {
            return layoutChoices;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Prints the frameset used.
     */
    abstract public void printFrameSet(
	WebPage page,
	WebSiteRequest req,
        HttpServletResponse resp,
	ChainWriter out
    ) throws IOException, SQLException;

    /**
     * Prints the frame named <code>frame</code>.
     *
     * @return  <code>true</code> if the frame was printed, or <code>false</code> if the frame does not exist
     */
    abstract public boolean printFrame(
	WebPage page,
	WebSiteRequest req,
	HttpServletResponse resp,
	ChainWriter out,
	String onLoad,
	String frame
    ) throws IOException, SQLException;

    /**
     * Writes all of the HTML preceeding the content of the page,
     * whether the page is in a frameset or not.
     */
    abstract public void startHTML(
	WebPage page,
	WebSiteRequest req,
        HttpServletResponse resp,
	ChainWriter out,
	String onLoad
    ) throws IOException, SQLException;

    /**
     * Writes all of the HTML following the content of the page,
     * whether the page is in a frameset or not.
     */
    abstract public void endHTML(
	WebPage page,
	WebSiteRequest req,
	ChainWriter out
    ) throws IOException, SQLException;

    /**
     * Determines if frames should be used.  By default, frames are used if the page requests it,
     * the request is not from a Lynx browser, and the request is not from a search engine.
     *
     * @see WebPage#doGet(WebSiteRequest,HttpServletResponse)
     */
    public boolean useFrames(WebPage page, WebSiteRequest req) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "useFrames(WebPage,WebSiteRequest)", null);
        try {
            return
                page.useFrames(req)
                && !req.isLynx()
                && !req.isSearchEngine()
            ;
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Prints the content HTML that shows the output of a search.  This output must include an
     * additional search form named <code>"search_two"</code>, with two fields named
     * <code>"search_query"</code> and <code>"search_target"</code>.
     *
     * @see  WebPage#doPostWithSearch(WebSiteRequest,HttpServletResponse)
     */
    public void printSearchOutput(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String query, boolean isEntireSite, List<SearchResult> results, String[] words) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, WebPageLayout.class, "printSearchOutput(WebPage page, ChainWriter,WebSiteRequest,HttpServletResponse,String,boolean,List<SearchResult>,String[])", null);
        try {
            startContent(out, req, resp, 1, 600);
            printContentTitle(out, req, resp, "Search Results", 1);
            printContentHorizontalDivider(out, req, resp, 1, false);
            startContentLine(out, req, resp, 1, "center");
            beginLightArea(req, resp, out, "300", true);
            boolean isSecure = req.isSecure();
            out.print("      <FORM name='search_two' method='POST'>\n");
            req.printFormFields(out, 4);
            out.print("        <TABLE border=0 cellspacing=0 cellpadding=0><TR><TD nowrap>\n"
                    + "          Word(s) to search for: <input type='text' size=24 name='search_query' value='").writeHtmlAttribute(query).print("'><BR>\n"
                    + "          Search Location: <input type='radio' name='search_target' value='entire_site'");
            if(isEntireSite) out.print(" checked");
            out.print("> Entire Site&nbsp;&nbsp;&nbsp;<input type='radio' name='search_target' value='this_area'");
            if(!isEntireSite) out.print(" checked");
            out.print("> This Area<BR>\n"
                    + "          <BR>\n"
                    + "          <CENTER><INPUT type='submit' class='ao_button' value=' Search '></CENTER>\n"
                    + "        </TD></TR></TABLE>\n"
                    + "      </FORM>\n"
            );
            endLightArea(req, resp, out);
            endContentLine(out, req, resp, 1, false);
            printContentHorizontalDivider(out, req, resp, 1, false);
            startContentLine(out, req, resp, 1, "center");
            if (results.isEmpty()) {
                if (words.length > 0) {
                    out.print(
                          "      <B>No matches found</B>\n"
                    );
                }
            } else {
                beginLightArea(req, resp, out);
                out.print("  <table cellspacing=0 border=0 cellpadding=0 class='ao_light_row'>\n"
                        + "    <TR>\n"
                        + "      <TH nowrap>% Match</TH>\n"
                        + "      <TH nowrap>Title</TH>\n"
                        + "      <TH nowrap>&nbsp;</TH>\n"
                        + "      <TH nowrap>Description</TH>\n"
                        + "    </TR>\n"
                );

                // Find the highest probability
                float highest = results.get(0).getProbability();

                // Display the results
                int size = results.size();
                for (int c = 0; c < size; c++) {
                    String rowClass= (c & 1) == 0 ? "ao_light_row":"ao_dark_row";
                    String linkClass = (c & 1) == 0 ? "ao_dark_link":"ao_light_link";
                    SearchResult result = results.get(c);
                    String url=result.getUrl();
                    String title=result.getTitle();
                    String description=result.getDescription();
                    out.print("    <TR class='").print(rowClass).print("'>\n"
                            + "      <TD nowrap align=center>").print(Math.round(99 * result.getProbability() / highest)).print("%</TD>\n"
                            + "      <TD nowrap><A class='"+linkClass+"' href='").print(resp.encodeURL(url)).print("'>").print(title.length()==0?"&nbsp;":title).print("</A></TD>\n"
                            + "      <TD nowrap>&nbsp;&nbsp;&nbsp;</TD>\n"
                            + "      <TD nowrap>").print(description.length()==0?"&nbsp;":description).print("</TD>\n"
                            + "    </TR>\n");
                }
                out.print(
                      "  </TABLE>\n"
                );
                endLightArea(req, resp, out);
            }
            endContentLine(out, req, resp, 1, false);
            endContent(page, out, req, resp, 1);
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }
    
    /**
     * Starts the content area of a page.
     */
    final public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int contentColumns, int preferredWidth) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "startContent(ChainWriter,WebSiteRequest,HttpServletResponse,int,int)", null);
        try {
            startContent(out, req, resp, new int[] {contentColumns}, preferredWidth);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Starts the content area of a page.
     */
    abstract public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException, SQLException;

    /**
     * Prints a horizontal divider of the provided colspan.
     */
    final public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, boolean endsInternal) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "printContentHorizontalDivider(ChainWriter,WebSiteRequest,HttpServletResponse,int,boolean)", null);
        try {
            printContentHorizontalDivider(out, req, resp, new int[] {colspan}, endsInternal);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Prints a horizontal divider of the provided colspan.
     */
    abstract public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException, SQLException;

    /**
     * Prints the title of the page in one row in the content area.
     */
    final public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage page, int contentColumns) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "printContentTitle(ChainWriter,WebSiteRequest,HttpServletResponse,WebPage,int)", null);
        try {
            printContentTitle(out, req, resp, page.getTitle(), contentColumns);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Prints the title of the page in one row in the content area.
     */
    abstract public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException, SQLException;

    /**
     * Starts one line of content with the initial colspan set to the provided colspan.
     */
    abstract public void startContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, String align) throws IOException, SQLException;

    /**
     * Ends one part of a line and starts the next.
     */
    abstract public void printContentVerticalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align) throws IOException, SQLException;

    /**
     * Ends one line of content.
     */
    abstract public void endContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws IOException, SQLException;

    /**
     * Ends the content area of a page.
     */
    final public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int contentColumns) throws IOException, SQLException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "endContent(WebPage,ChainWriter,WebSiteRequest,HttpServletResponse,int)", null);
        try {
            endContent(page, out, req, resp, new int[] {contentColumns});
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Ends the content area of a page.
     */
    abstract public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws IOException, SQLException;

    /**
     * The background color for the page or <code>-1</code> for browser default.
     */
    public int getBackgroundColor(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "getBackgroundColor(WebSiteRequest)", null);
        try {
            return -1;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * The text color for the page or <code>-1</code> for browser default.
     */
    public int getTextColor(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "getTextColor(WebSiteRequest)", null);
        try {
            return -1;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * The link color for the page or <code>-1</code> for browser default.
     */
    public int getLinkColor(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "getLinkColor(WebSiteRequest)", null);
        try {
            return -1;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * The visited link color for the page or <code>-1</code> for browser default.
     */
    public int getVisitedLinkColor(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "getVisitedLinkColor(WebSiteRequest)", null);
        try {
            return -1;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * The active link color for the page or <code>-1</code> for browser default.
     */
    public int getActiveLinkColor(WebSiteRequest req) {
        Profiler.startProfile(Profiler.INSTANTANEOUS, WebPageLayout.class, "getActiveLinkColor(WebSiteRequest)", null);
        try {
            return -1;
        } finally {
            Profiler.endProfile(Profiler.INSTANTANEOUS);
        }
    }

    /**
     * Begins a lighter colored area of the site.
     */
    final public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "beginLightArea(WebSiteRequest,HttpServletResponse,ChainWriter)", null);
        try {
            beginLightArea(req, resp, out, null, false);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Begins a lighter colored area of the site.
     */
    abstract public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String width, boolean nowrap) throws IOException;

    /**
     * Ends a lighter area of the site.
     */
    abstract public void endLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException;
    
    /**
     * Begins an area with a white background.
     */
    final public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException {
        Profiler.startProfile(Profiler.FAST, WebPageLayout.class, "beginWhiteArea(WebSiteRequest,HttpServletResponse,ChainWriter)", null);
        try {
            beginWhiteArea(req, resp, out, null, false);
        } finally {
            Profiler.endProfile(Profiler.FAST);
        }
    }

    /**
     * Begins a lighter colored area of the site.
     */
    abstract public void beginWhiteArea(WebSiteRequest req, HttpServletResponse response, ChainWriter out, String width, boolean nowrap) throws IOException;

    /**
     * Ends a lighter area of the site.
     */
    abstract public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException;

    /**
     * Each layout has a name.
     */
    abstract public String getName();
    
    public boolean printWebPageLayoutSelector(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
        Profiler.startProfile(Profiler.IO, WebPageLayout.class, "printWebPageLayoutSelector(WebPage,ChainWriter,WebSiteRequest,HttpServletResponse)", null);
        try {
            if(layoutChoices.length>=2) {
                out.print("<SCRIPT language='JavaScript1.2'><!--\n"
                        + "  function selectLayout(layout) {\n");
                for(int c=0;c<layoutChoices.length;c++) {
                    String choice=layoutChoices[c];
                    out.print("    if(layout=='").print(choice).print("') window.top.location.href='").print(resp.encodeURL(req.getURL(page, "layout="+choice))).print("';\n");
                }
                out.print("  }\n"
                        + "// --></SCRIPT>\n"
                        + "<FORM style='display:inline;'>\n"
                        + "  <SELECT name='layout_selector' onChange='selectLayout(this.form.layout_selector.options[this.form.layout_selector.selectedIndex].value);'>\n");
                for(int c=0; c<layoutChoices.length; c++) {
                    String choice=layoutChoices[c];
                    out.print("    <OPTION value='").writeHtmlAttribute(choice).print('\'');
                    if(choice.equalsIgnoreCase(getName())) out.print(" selected");
                    out.print('>').writeHtml(choice).print("</OPTION>\n");
                }
                out.print("  </SELECT>\n"
                        + "</FORM>\n");
                return true;
            } else return false;
        } finally {
            Profiler.endProfile(Profiler.IO);
        }
    }
    
    protected void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, WebPage page) throws IOException, SQLException {
	Object O = page.getJavaScriptSrc(req);
	if (O != null) {
            if (O instanceof String[]) {
                String[] SA = (String[]) O;
                int len = SA.length;
                for (int c = 0; c < len; c++) {
                    out.print("    <SCRIPT language='JavaScript1.2' src='").writeHtmlAttribute(resp.encodeURL(req.getURL(SA[c], req.isSecure(), null, false))).print("'></SCRIPT>\n");
                }
            } else if(O instanceof Class) {
                out.print("    <SCRIPT language='JavaScript1.2' src='").writeHtmlAttribute(resp.encodeURL(req.getURL(((Class<?>)O).asSubclass(WebPage.class), null))).print("'></SCRIPT>\n");
            } else if(O instanceof WebPage) {
                out.print("    <SCRIPT language='JavaScript1.2' src='").writeHtmlAttribute(resp.encodeURL(req.getURL((WebPage)O, req.isSecure(), null))).print("'></SCRIPT>\n");
            } else {
                out.print("    <SCRIPT language='JavaScript1.2' src='").writeHtmlAttribute(resp.encodeURL(req.getURL(O.toString(), req.isSecure(), null, false))).print("'></SCRIPT>\n");
            }
	}
    }
}
