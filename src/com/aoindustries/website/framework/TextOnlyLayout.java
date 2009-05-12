package com.aoindustries.website.framework;

/*
 * Copyright 2003-2009 by AO Industries, Inc.,
 * 7262 Bull Pen Cir, Mobile, Alabama, 36695, U.S.A.
 * All rights reserved.
 */
import com.aoindustries.io.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * The default text-only layout.
 *
 * @author  AO Industries, Inc.
 */
public class TextOnlyLayout extends WebPageLayout {

    public TextOnlyLayout(String[] layoutChoices) {
        super(layoutChoices);
    }

    public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String width, boolean nowrap) {
        out.print("<TABLE border=5 cellpadding=0 cellspacing=0>\n"
                + "  <TR>\n"
                + "    <TD class='ao_light_row'");
        if(width!=null) out.print(" width='").print(width).print('\'');
        if(nowrap) out.print(" nowrap");
        out.print(">");
    }

    public void endLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
	out.print("</TD>\n"
                + "  </TR>\n"
                + "</TABLE>\n");
    }

    public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String width, boolean nowrap) {
        out.print("<TABLE border=5 cellpadding=0 cellspacing=0>\n"
                + "  <TR>\n"
                + "    <TD class='ao_light_row'");
        if(width!=null) out.print(" width='").print(width).print('\'');
        if(nowrap) out.print(" nowrap");
        out.print(" bgcolor='white'>");
    }

    public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
	out.print("</TD>\n"
                + "  </TR>\n"
                + "</TABLE>\n");
    }

    public void printFrameSet(
	WebPage page,
	WebSiteRequest req,
        HttpServletResponse resp,
	ChainWriter out
    ) {
        throw new RuntimeException("Frames should never be used for text only layout.");
    }

    public boolean printFrame(
	WebPage page,
	WebSiteRequest req,
	HttpServletResponse resp,
	ChainWriter out,
	String onLoad,
	String frame
    ) {
        // Frames are not supported by text only layout
	return false;
    }

    public void startHTML(
	WebPage page,
	WebSiteRequest req,
        HttpServletResponse resp,
	ChainWriter out,
	String onLoad
    ) throws IOException, SQLException {
        out.print("<HTML>\n"
            + "  <HEAD>\n");
        // If this is not the default layout, then robots noindex
        if(!getName().equals(getLayoutChoices()[0])) {
            out.print("    <META NAME=\"ROBOTS\" CONTENT=\"NOINDEX, NOFOLLOW\" />\n");
        }
        out.print("    <TITLE>");
        List<WebPage> parents=new ArrayList<WebPage>();
        WebPage parent=page;
        while(parent!=null) {
            if(parent.showInLocationPath(req)) parents.add(parent);
            parent=parent.getParent();
        }
        for(int c=(parents.size()-1);c>=0;c--) {
            if(c<(parents.size()-1)) out.print(" - ");
            parent=parents.get(c);
            out.print(parent.getTitle());
        }
        out.print("</TITLE>\n"
                + "    <META http-equiv='Content-Type' content='text/html; charset=iso-8859-1'>\n"
		+ "    <META name='keywords' content='").writeHtmlAttribute(page.getKeywords()).print("'>\n"
		+ "    <META name='description' content='").writeHtmlAttribute(page.getDescription()).print("'>\n"
		+ "    <META name='author' content='").writeHtmlAttribute(page.getAuthor()).print("'>\n"
                + "    <LINK rel='stylesheet' href='").writeHtmlAttribute(resp.encodeURL(req.getURL("layout/text/global.css", req.isSecure(), null, false))).print("' type='text/css'>\n"
                + "    <SCRIPT language='JavaScript1.2' src='").writeHtmlAttribute(resp.encodeURL(req.getURL("global.js", req.isSecure(), null, false))).print("'></SCRIPT>\n");
        printJavaScriptIncludes(req, resp, out, page);
        out.print("  </HEAD>\n"
                + "  <BODY\n");
        int color=getBackgroundColor(req);
        if(color!=-1) out.print("    bgcolor='").writeHtmlColor(color).print("'\n");
        color=getTextColor(req);
        if(color!=-1) out.print("    text='").writeHtmlColor(color).print("'\n");
        color=getLinkColor(req);
        if(color!=-1) out.print("    link='").writeHtmlColor(color).print("'\n");
        color=getVisitedLinkColor(req);
        if(color!=-1) out.print("    vlink='").writeHtmlColor(color).print("'\n");
        color=getActiveLinkColor(req);
        if(color!=-1) out.print("    alink='").writeHtmlColor(color).print("'\n");
        out.print("    onLoad=\"");
	if (onLoad == null) onLoad = page.getOnLoadScript(req);
	if (onLoad != null) {
            out.print(' ');
            out.print(onLoad);
	}
	out.print("\"\n"
                + "  >\n"
                + "    <TABLE border=0 cellspacing=10 cellpadding=0>\n");
        out.print("      <TR>\n"
                + "        <TD valign='top'>\n");
        printLogo(page, out, req, resp);
        boolean isLoggedIn=req.isLoggedIn();
        if(isLoggedIn) {
            out.print("          <HR>\n"
                    + "          Logout: <FORM target='_top' style='display:inline;' name='logout_form' method='post' action='").writeHtmlAttribute(resp.encodeURL(req.getURL(page, req.isSecure(), null))).print("'>");
            req.printFormFields(out, 2);
            out.print("<INPUT type='hidden' name='logout_requested' value='true'><INPUT type='submit' value='Logout'></FORM>\n");
        } else {
            out.print("          <HR>\n"
                    + "          Login: <FORM target='_top' style='display:inline;' name='login_form' method='post' action='").writeHtmlAttribute(resp.encodeURL(req.getURL(page, true, null))).print("'>");
            req.printFormFields(out, 2);
            out.print("<INPUT type='hidden' name='login_requested' value='true'><INPUT type='submit' value='Login'></FORM>\n");
        }
        out.print("          <HR>\n"
                + "          <SPAN style='white-space: nowrap'>\n");
        if(getLayoutChoices().length>=2) out.print("Layout: ");
        if(printWebPageLayoutSelector(page, out, req, resp)) out.print("<BR>\n"
                + "            Search:  <FORM name='search_site' style='display:inline;' method='post' action='").writeHtmlAttribute(resp.encodeURL(req.getURL(page, req.isSecure(), null))).print("'>\n"
                + "              <INPUT type='hidden' name='search_target' value='entire_site'>\n");
	req.printFormFields(out, 3);
        out.print("              <INPUT type='text' name='search_query' size=12 maxlength=255>\n"
                + "            </FORM><BR>\n"
                + "          </SPAN>\n"
                + "          <HR>\n"
                + "          <B>Current Location</B><BR>\n"
                + "          <SPAN style='white-space: nowrap'>\n");
        parents.clear();
        parent=page;
        while(parent!=null) {
            if(parent.showInLocationPath(req)) parents.add(parent);
            parent=parent.getParent();
        }
        for(int c=(parents.size()-1);c>=0;c--) {
            parent=parents.get(c);
            String navAlt=parent.getNavImageAlt(req);
            String navSuffix=parent.getNavImageSuffix(req);
            out.print("            <A href='").writeHtmlAttribute(resp.encodeURL(req.getURL(parent))).print("'>").print(TreePage.replaceHTML(navAlt));
            if(navSuffix!=null) out.print(" (").writeHtml(navSuffix).print(')');
            out.print("</A><BR>\n");
        }
        out.print("          </SPAN>\n"
                + "          <HR>\n"
                + "          <B>Related Pages</B><BR>\n"
                + "          <SPAN style='white-space: nowrap'>\n");
        WebPage[] pages=page.getCachedPages(req);
        parent=page;
        if(pages.length==0) {
            parent=page.getParent();
            if(parent!=null) pages=parent.getCachedPages(req);
        }

        for(int c=-1;c<pages.length;c++) {
            WebPage tpage;
            if (c==-1) {
                if (parent!=null && parent.includeNavImageAsParent()) tpage=parent;
                else tpage=null;
            } else {
                tpage=pages[c];
            }
            if(tpage!=null && (tpage.useNavImage() || tpage.equals(page) || (tpage.includeNavImageAsParent() && tpage.equals(parent)))) {
                String navAlt=tpage.getNavImageAlt(req);
                String navSuffix=tpage.getNavImageSuffix(req);
                boolean isSelected=tpage.equals(page);
                out.print("          <A href='").writeHtmlAttribute(resp.encodeURL(tpage.getNavImageURL(req, null))).print("'>").writeHtml(TreePage.replaceHTML(navAlt));
                if(navSuffix!=null) out.print(" (").writeHtml(navSuffix).print(')');
                out.print("</A><BR>\n");
            }
        }
        out.print("          </SPAN>\n"
                + "          <HR>\n");
        printBelowRelatedPages(out, req);
        out.print("        </TD>\n"
                + "        <TD valign='top'>");
        WebPage[] commonPages=getCommonPages(page, req);
        if(commonPages!=null && commonPages.length>0) {
            out.print("        <TABLE border=0 cellspacing=0 cellpadding=0 width=100%><TR>\n");
            for(int c=0;c<commonPages.length;c++) {
                if(c>0) out.print("          <TD align='center' width='1%'>|</TD>\n");
                WebPage tpage=commonPages[c];
                out.print("          <TD nowrap align='center' width='").print((101-commonPages.length)/commonPages.length).print("%'><A href='").writeHtmlAttribute(resp.encodeURL(tpage.getNavImageURL(req, null))).print("'>").print(tpage.getNavImageAlt(req)).print("</A></TD>\n");
            }
            out.print("        </TR></TABLE>\n");
        }
    }

    public void endHTML(
	WebPage page,
	WebSiteRequest req,
	ChainWriter out
    ) throws IOException, SQLException {
        out.print("        </TD>\n"
                + "      </TR>\n"
                + "    </TABLE>\n"
                + "  </BODY>\n"
                + "</HTML>\n");
    }

    /**
     * Starts the content area of a page.
     */
    public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) {
        out.print("<TABLE cellpadding=0 cellspacing=0 border=0");
        if(preferredWidth!=-1) out.print(" width='").print(preferredWidth).print('\'');
        out.print(" align='left' valign='top'>\n"
                + "  <TR>\n");
        int totalColumns=0;
        for(int c=0;c<contentColumnSpans.length;c++) {
            if(c>0) totalColumns++;
            totalColumns+=contentColumnSpans[c];
        }
        out.print("    <TD");
        if(totalColumns!=1) out.print(" colspan=").print(totalColumns);
        out.print("><HR></TD>\n"
                + "  </TR>\n");
    }

    /**
     * Prints a horizontal divider of the provided colspan.
     */
    public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) {
        out.print("  <TR>\n");
        for(int c=0;c<colspansAndDirections.length;c+=2) {
            int direction=c==0?-1:colspansAndDirections[c-1];
            if(direction!=-1) {
                switch(direction) {
                    case UP:
                        out.print("    <TD>&nbsp;</TD>\n");
                        break;
                    case DOWN:
                        out.print("    <TD>&nbsp;</TD>\n");
                        break;
                    case UP_AND_DOWN:
                        out.print("    <TD>&nbsp;</TD>\n");
                        break;
                    default: throw new IllegalArgumentException("Unknown direction: "+direction);
                }
            }

            int colspan=colspansAndDirections[c];
            out.print("    <TD");
            if(colspan!=1) out.print(" colspan=").print(colspan);
            out.print("><HR></TD>\n");
        }
        out.print("  </TR>\n");
    }

    /**
     * Prints the title of the page in one row in the content area.
     */
    public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) {
        startContentLine(out, req, resp, contentColumns, "center");
        out.print("<H1>").print(title).print("</H1>\n");
        endContentLine(out, req, resp, 1, false);
    }

    /**
     * Starts one line of content with the initial colspan set to the provided colspan.
     */
    public void startContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, String align) {
        out.print("  <TR>\n"
                + "    <TD valign='top'");
        if(colspan!=1) out.print(" colspan=").print(colspan);
        if(align!=null && !align.equalsIgnoreCase("left")) out.print(" align='").print(align).print('\'');
        out.print('>');
    }

    /**
     * Starts one line of content with the initial colspan set to the provided colspan.
     */
    public void printContentVerticalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align) {
        out.print("    </TD>\n");
        switch(direction) {
            case UP_AND_DOWN:
                out.print("    <TD>&nbsp;</TD>\n");
                break;
            case NONE:
                break;
            default: throw new IllegalArgumentException("Unknown direction: "+direction);
        }
        out.print("    <TD valign='top'");
        if(colspan!=1) out.print(" colspan=").print(colspan);
        if(rowspan!=1) out.print(" rowspan=").print(rowspan);
        if(align!=null && !align.equals("left")) out.print(" align='").print(align).print('\'');
        out.print('>');
    }

    /**
     * Ends one line of content.
     */
    public void endContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) {
        out.print("    </TD>\n"
                + "  </TR>\n");
    }

    /**
     * Ends the content area of a page.
     */
    public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws IOException, SQLException {
        int totalColumns=0;
        for(int c=0;c<contentColumnSpans.length;c++) {
            if(c>0) totalColumns+=1;
            totalColumns+=contentColumnSpans[c];
        }
        out.print("  <TR><TD");
        if(totalColumns!=1) out.print(" colspan=").print(totalColumns);
        out.print("><HR></TD></TR>\n");
        String copyright=page.getCopyright(req, page);
        if(copyright!=null && copyright.length()>0) {
            out.print("  <TR><TD");
            if(totalColumns!=1) out.print(" colspan=").print(totalColumns);
            out.print(" align='center'><FONT size=-2>").print(copyright).print("</FONT></TD></TR>\n");
        }
        out.print("</TABLE>\n");
    }
    
    public String getName() {
        return "Text";
    }

    @Override
    public boolean useFrames(WebPage page, WebSiteRequest req) {
        return false;
    }

    public WebPage[] getCommonPages(WebPage page, WebSiteRequest req) throws IOException, SQLException {
        return null;
    }

    public void printLogo(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
    }

    /**
     * Prints content below the related pages area on the left.
     */
    public void printBelowRelatedPages(ChainWriter out, WebSiteRequest req) throws IOException, SQLException {
    }
}
