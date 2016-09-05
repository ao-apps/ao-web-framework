/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2003-2013, 2015, 2016  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aoweb-framework.
 *
 * aoweb-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aoweb-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aoweb-framework.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.website.framework;

import com.aoindustries.encoding.ChainWriter;
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

	@Override
	public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String width, boolean nowrap) {
		out.print("<table style='border:5px outset #a0a0a0;");
		if(width!=null && (width=width.trim()).length()>0) {
			out.append(" width:");
			try {
				int widthInt = Integer.parseInt(width);
				out.append(Integer.toString(widthInt));
				out.append("px");
			} catch(NumberFormatException err) {
				out.append(width);
			}
			out.append(';');
		}
		out.print("' cellpadding='0' cellspacing='0'>\n"
				+ "  <tr>\n"
				+ "    <td class='aoLightRow' style='padding:4px;");
		if(nowrap) out.append(" white-space:nowrap;");
		out.append("'>");
	}

	@Override
	public void endLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
	out.print("</td>\n"
				+ "  </tr>\n"
				+ "</table>\n");
	}

	@Override
	public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String width, boolean nowrap) {
		out.print("<table style='border:5px outset #a0a0a0;");
		if(width!=null && (width=width.trim()).length()>0) {
			out.append(" width:");
			try {
				int widthInt = Integer.parseInt(width);
				out.append(Integer.toString(widthInt));
				out.append("px");
			} catch(NumberFormatException err) {
				out.append(width);
			}
			out.append(';');
		}
		out.print("' cellpadding='0' cellspacing='0'>\n"
				+ "  <tr>\n"
				+ "    <td class='aoWhiteRow' style='background-color:white; padding:4px;");
		if(nowrap) out.append(" white-space:nowrap;");
		out.append("'>");
	}

	@Override
	public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
		out.print("</td>\n"
				+ "  </tr>\n"
				+ "</table>\n");
	}

	/**
	 * Until version 3.0 there will not be a getStatus method on the HttpServletResponse class.
	 * To allow code to detect the current status, anytime the status is set one should
	 * also set the request attribute of this name to a java.lang.Integer of the status.
	 *
	 * Matches value in com.aoindustries.website.Constants.HTTP_SERVLET_RESPONSE_STATUS for
	 * interoperability between the frameworks.
	 */
	public static final String HTTP_SERVLET_RESPONSE_STATUS = "httpServletResponseStatus";

	@Override
	public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		String onload
	) throws IOException, SQLException {
		boolean isOkResponseStatus;
		{
			Integer responseStatus = (Integer)req.getAttribute(HTTP_SERVLET_RESPONSE_STATUS);
			isOkResponseStatus = responseStatus==null || responseStatus==HttpServletResponse.SC_OK;
		}

		out.print("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">\n"
				+ "  <head>\n");
		// If this is not the default layout, then robots noindex
		if(!isOkResponseStatus || !getName().equals(getLayoutChoices()[0])) {
			out.print("    <meta name=\"ROBOTS\" content=\"NOINDEX, NOFOLLOW\" />\n");
		}
		// Default style language
		out.print("<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />\n"
				+ "    <title>");
		List<WebPage> parents=new ArrayList<>();
		WebPage parent=page;
		while(parent!=null) {
			if(parent.showInLocationPath(req)) parents.add(parent);
			parent=parent.getParent();
		}
		for(int c=(parents.size()-1);c>=0;c--) {
			if(c<(parents.size()-1)) out.print(" - ");
			parent=parents.get(c);
			out.encodeXhtml(parent.getTitle());
		}
		out.print("</title>\n"
				+ "    <meta http-equiv='Content-Type' content='text/html; charset=").encodeXmlAttribute(resp.getCharacterEncoding()).print("' />\n"
				+ "    <meta name='keywords' content='").encodeXmlAttribute(page.getKeywords()).print("' />\n"
				+ "    <meta name='description' content='").encodeXmlAttribute(page.getDescription()).print("' />\n"
				+ "    <meta name='abstract' content='").encodeXmlAttribute(page.getDescription()).print("' />\n");
		String copyright = page.getCopyright(req, resp, page);
		if(copyright!=null && copyright.length()>0) {
			out.print("    <meta name='copyright' content='").encodeXmlAttribute(copyright).print("' />\n");
		}
		String author = page.getAuthor();
		if(author!=null && author.length()>0) {
			out.print("    <meta name='author' content='").encodeXmlAttribute(author).print("' />\n");
		}
		out.print("    <link rel='stylesheet' href='").encodeXmlAttribute(resp.encodeURL(req.getContextPath()+req.getURL("/layout/text/global.css", null, false))).print("' type='text/css' />\n"
				+ "    <script type='text/javascript' src='").encodeXmlAttribute(resp.encodeURL(req.getContextPath()+req.getURL("/global.js", null, false))).print("'></script>\n");
		String googleAnalyticsNewTrackingCode = getGoogleAnalyticsNewTrackingCode();
		if(googleAnalyticsNewTrackingCode!=null) {
			out.print("    <script type='text/javascript' src='").print(req.isSecure() ? "https://ssl.google-analytics.com/ga.js" : "http://www.google-analytics.com/ga.js").print("'></script>\n");
		}

		printJavaScriptIncludes(req, resp, out, page);
		out.print("  </head>\n"
				+ "  <body\n");
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
		out.print("    onload=\"");
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null) {
			out.print(' ');
			out.print(onload);
		}
		out.print("\"\n"
				+ "  >\n"
				+ "    <table cellspacing='10' cellpadding='0'>\n");
		out.print("      <tr>\n"
				+ "        <td valign='top'>\n");
		printLogo(page, out, req, resp);
		boolean isLoggedIn=req.isLoggedIn();
		if(isLoggedIn) {
			out.print("          <hr />\n"
					+ "          Logout: <form style='display:inline;' id='logout_form' method='post' action='").encodeXmlAttribute(resp.encodeURL(req.getContextPath()+req.getURL(page))).print("'><div style='display:inline;'>");
			req.printFormFields(out, 2);
			out.print("<input type='hidden' name='logout_requested' value='true' /><input type='submit' value='Logout' /></div></form>\n");
		} else {
			out.print("          <hr />\n"
					+ "          Login: <form style='display:inline;' id='login_form' method='post' action='").encodeXmlAttribute(resp.encodeURL(req.getContextPath()+req.getURL(page))).print("'><div style='display:inline;'>");
			req.printFormFields(out, 2);
			out.print("<input type='hidden' name='login_requested' value='true' /><input type='submit' value='Login' /></div></form>\n");
		}
		out.print("          <hr />\n"
				+ "          <div style='white-space:nowrap'>\n");
		if(getLayoutChoices().length>=2) out.print("Layout: ");
		if(printWebPageLayoutSelector(page, out, req, resp)) out.print("<br />\n"
				+ "            Search: <form id='search_site' style='display:inline;' method='post' action='").encodeXmlAttribute(resp.encodeURL(req.getContextPath()+req.getURL(page))).print("'><div style='display:inline;'>\n"
				+ "              <input type='hidden' name='search_target' value='entire_site' />\n");
		req.printFormFields(out, 3);
		out.print("              <input type='text' name='search_query' size='12' maxlength='255' />\n"
				+ "            </div></form><br />\n"
				+ "          </div>\n"
				+ "          <hr />\n"
				+ "          <b>Current Location</b><br />\n"
				+ "          <div style='white-space:nowrap'>\n");
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
			out.print("            <a href='").encodeXmlAttribute(resp.encodeURL(req.getContextPath()+req.getURL(parent))).print("'>").print(TreePage.replaceHTML(navAlt));
			if(navSuffix!=null) out.print(" (").encodeXhtml(navSuffix).print(')');
			out.print("</a><br />\n");
		}
		out.print("          </div>\n"
				+ "          <hr />\n"
				+ "          <b>Related Pages</b><br />\n"
				+ "          <div style='white-space:nowrap'>\n");
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
				//boolean isSelected=tpage.equals(page);
				out.print("          <a href='").encodeXmlAttribute(tpage.getNavImageURL(req, resp, null)).print("'>").encodeXhtml(TreePage.replaceHTML(navAlt));
				if(navSuffix!=null) out.print(" (").encodeXhtml(navSuffix).print(')');
				out.print("</a><br />\n");
			}
		}
		out.print("          </div>\n"
				+ "          <hr />\n");
		printBelowRelatedPages(out, req);
		out.print("        </td>\n"
				+ "        <td valign='top'>");
		WebPage[] commonPages=getCommonPages(page, req);
		if(commonPages!=null && commonPages.length>0) {
			out.print("        <table cellspacing='0' cellpadding='0' style='width:100%;'><tr>\n");
			for(int c=0;c<commonPages.length;c++) {
				if(c>0) out.print("          <td align='center' style='width:1%'>|</td>\n");
				WebPage tpage=commonPages[c];
				out.print("          <td style='white-space:nowrap; text-align:center; width:").print((101-commonPages.length)/commonPages.length).print("%'><a href='").encodeXmlAttribute(tpage.getNavImageURL(req, resp, null)).print("'>").print(tpage.getNavImageAlt(req)).print("</a></td>\n");
			}
			out.print("        </tr></table>\n");
		}
	}

	/**
	 * Gets the Google Analytics New Tracking Code (ga.js) or <code>null</code>
	 * if unavailable.
	 */
	public String getGoogleAnalyticsNewTrackingCode() {
		return null;
	}

	@Override
	public void endHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out
	) throws IOException, SQLException {
		out.print("        </td>\n"
				+ "      </tr>\n"
				+ "    </table>\n");
		String googleAnalyticsNewTrackingCode = getGoogleAnalyticsNewTrackingCode();
		if(googleAnalyticsNewTrackingCode!=null) {
			out.print("    <script type=\"text/javascript\">\n"
					+ "      // <![CDATA[\n"
					+ "      try {\n"
					+ "        var pageTracker = _gat._getTracker(\""); out.print(googleAnalyticsNewTrackingCode); out.print("\");\n");
			Integer responseStatus = (Integer)req.getAttribute(HTTP_SERVLET_RESPONSE_STATUS);
			if(responseStatus==null || responseStatus==HttpServletResponse.SC_OK) {
				out.print("        pageTracker._trackPageview();\n");
			} else {
				out.print("        pageTracker._trackPageview(\"/");
				out.print(responseStatus.toString());
				out.print(".html?page=\"+document.location.pathname+document.location.search+\"&from=\"+document.referrer);\n");
			}
			out.print("      } catch(err) {\n"
					+ "      }\n"
					+ "      // ]]>\n"
					+ "    </script>\n");
		}
		out.print("  </body>\n"
				+ "</html>\n");
	}

	/**
	 * Starts the content area of a page.
	 */
	@Override
	public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) {
		out.print("<table cellpadding='0' cellspacing='0'");
		if(preferredWidth!=-1) out.print(" width='").print(preferredWidth).print('\'');
		out.print(">\n"
				+ "  <tr>\n");
		int totalColumns=0;
		for(int c=0;c<contentColumnSpans.length;c++) {
			if(c>0) totalColumns++;
			totalColumns+=contentColumnSpans[c];
		}
		out.print("    <td");
		if(totalColumns!=1) out.print(" colspan='").print(totalColumns).print('\'');
		out.print("><hr /></td>\n"
				+ "  </tr>\n");
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	@Override
	public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) {
		out.print("  <tr>\n");
		for(int c=0;c<colspansAndDirections.length;c+=2) {
			int direction=c==0?-1:colspansAndDirections[c-1];
			if(direction!=-1) {
				switch(direction) {
					case UP:
						out.print("    <td>&#160;</td>\n");
						break;
					case DOWN:
						out.print("    <td>&#160;</td>\n");
						break;
					case UP_AND_DOWN:
						out.print("    <td>&#160;</td>\n");
						break;
					default: throw new IllegalArgumentException("Unknown direction: "+direction);
				}
			}

			int colspan=colspansAndDirections[c];
			out.print("    <td");
			if(colspan!=1) out.print(" colspan='").print(colspan).print('\'');
			out.print("><hr /></td>\n");
		}
		out.print("  </tr>\n");
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	@Override
	public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) {
		startContentLine(out, req, resp, contentColumns, "center", null);
		out.print("<h1>").print(title).print("</h1>\n");
		endContentLine(out, req, resp, 1, false);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void startContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) {
		out.print("  <tr>\n"
				+ "    <td");
		if(width!=null && width.length()>0) {
			out.append(" style='width:");
			out.append(width);
			out.append('\'');
		}
		out.print(" valign='top'");
		if(colspan!=1) out.print(" colspan='").print(colspan).print('\'');
		if(align!=null && !align.equalsIgnoreCase("left")) out.print(" align='").print(align).print('\'');
		out.print('>');
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void printContentVerticalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) {
		out.print("    </td>\n");
		switch(direction) {
			case UP_AND_DOWN:
				out.print("    <td>&#160;</td>\n");
				break;
			case NONE:
				break;
			default: throw new IllegalArgumentException("Unknown direction: "+direction);
		}
		out.print("    <td");
		if(width!=null && width.length()>0) {
			out.append(" style='width:");
			out.append(width);
			out.append('\'');
		}
		out.print(" valign='top'");
		if(colspan!=1) out.print(" colspan='").print(colspan).print('\'');
		if(rowspan!=1) out.print(" rowspan='").print(rowspan).print('\'');
		if(align!=null && !align.equals("left")) out.print(" align='").print(align).print('\'');
		out.print('>');
	}

	/**
	 * Ends one line of content.
	 */
	@Override
	public void endContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) {
		out.print("    </td>\n"
				+ "  </tr>\n");
	}

	/**
	 * Ends the content area of a page.
	 */
	@Override
	public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws IOException, SQLException {
		int totalColumns=0;
		for(int c=0;c<contentColumnSpans.length;c++) {
			if(c>0) totalColumns+=1;
			totalColumns+=contentColumnSpans[c];
		}
		out.print("  <tr><td");
		if(totalColumns!=1) out.print(" colspan='").print(totalColumns).print('\'');
		out.print("><hr /></td></tr>\n");
		String copyright=page.getCopyright(req, resp, page);
		if(copyright!=null && copyright.length()>0) {
			out.print("  <tr><td");
			if(totalColumns!=1) out.print(" colspan='").print(totalColumns).print('\'');
			out.print(" align='center'><span style='font-size:x-small;'>").print(copyright).print("</span></td></tr>\n");
		}
		out.print("</table>\n");
	}

	@Override
	public String getName() {
		return "Text";
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
