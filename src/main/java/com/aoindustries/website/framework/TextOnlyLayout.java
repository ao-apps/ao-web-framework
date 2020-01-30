/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2003-2013, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Doctype;
import com.aoindustries.html.Html;
import com.aoindustries.html.Link;
import com.aoindustries.html.servlet.HtmlEE;
import com.aoindustries.html.util.GoogleAnalytics;
import static com.aoindustries.taglib.AttributeUtils.appendWidthStyle;
import com.aoindustries.taglib.HtmlTag;
import static com.aoindustries.util.StringUtility.trimNullIfEmpty;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
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
	public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String align, String width, boolean nowrap) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		out.print("<table style=\"border:5px outset #a0a0a0");
		if(align != null) {
			out.append(";text-align:");
			encodeTextInXhtmlAttribute(align, out);
		}
		if(width != null) {
			out.append(';');
			appendWidthStyle(width, out);
		}
		out.print("\" cellpadding=\"0\" cellspacing=\"0\">\n"
				+ "  <tr>\n"
				+ "    <td class=\"aoLightRow\" style=\"padding:4px");
		if(nowrap) out.append(";white-space:nowrap");
		out.append("\">");
	}

	@Override
	public void endLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) {
	out.print("</td>\n"
				+ "  </tr>\n"
				+ "</table>\n");
	}

	@Override
	public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String align, String width, boolean nowrap) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		out.print("<table style=\"border:5px outset #a0a0a0");
		if(align != null) {
			out.append(";text-align:");
			encodeTextInXhtmlAttribute(align, out);
		}
		if(width != null) {
			out.append(';');
			appendWidthStyle(width, out);
		}
		out.print("\" cellpadding=\"0\" cellspacing=\"0\">\n"
				+ "  <tr>\n"
				+ "    <td class=\"aoWhiteRow\" style=\"background-color:white;padding:4px");
		if(nowrap) out.append(";white-space:nowrap");
		out.append("\">");
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
		ServletContext servletContext = req.getServletContext();
		String trackingId = getGoogleAnalyticsNewTrackingCode(servletContext);
		// Write doctype
		Html html = page.getHtml(req, out);
		html.xmlDeclaration(resp.getCharacterEncoding());
		html.doctype();
		// Write <html>
		HtmlTag.beginHtmlTag(resp, out, html.serialization, null); // TODO: Move to Html class
		out.write("\n"
				+ "  <head>\n");
		// If this is not the default layout, then robots noindex
		if(!isOkResponseStatus || !getName().equals(getLayoutChoices()[0])) {
			out.print("    <meta name=\"ROBOTS\" content=\"NOINDEX, NOFOLLOW\"");
			html.selfClose().nl();
		}
		if(html.doctype == Doctype.HTML5) {
			out.print("    <meta charset=\"");
			out.encodeXmlAttribute(resp.getCharacterEncoding());
			out.print('"');
			html.selfClose().nl();
		} else {
			out.print("    <meta http-equiv=\"Content-Type\" content=\"").encodeXmlAttribute(resp.getContentType()).print('"');
			html.selfClose().nl();
			// Default style language
			out.print("    <meta http-equiv=\"Content-Style-Type\" content=\"text/css\"");
			html.selfClose().nl();
			out.print("    <meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\"");
			html.selfClose().nl();
		}
		if(html.doctype == Doctype.HTML5) {
			GoogleAnalytics.writeGlobalSiteTag(html, trackingId);
		} else {
			GoogleAnalytics.writeAnalyticsJs(html, trackingId);
		}
		// Mobile support
		out.print("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"");
		html.selfClose().nl();
		out.print("    <meta name=\"apple-mobile-web-app-capable\" content=\"yes\"");
		html.selfClose().nl();
		out.print("    <meta name=\"apple-mobile-web-app-status-bar-style\" content=\"black\"");
		html.selfClose().nl();
		// Authors
		// TODO: dcterms copyright
		String author = page.getAuthor();
		if(author != null && (author = author.trim()).length() > 0) {
			out.print("    <meta name=\"author\" content=\"").encodeXmlAttribute(author).print('"');
			html.selfClose().nl();
		}
		String authorHref = page.getAuthorHref(req, resp);
		if(authorHref != null && (authorHref = authorHref.trim()).length() > 0) {
			out.print("    ");
			html.link().rel(Link.Rel.AUTHOR).href(authorHref).__().nl();
		}
		out.print("    <title>");
		// No more page stack, just show current page only
		out.encodeXhtml(page.getTitle());
		/*
		List<WebPage> parents=new ArrayList<>();
		WebPage parent=page;
		while(parent!=null) {
			if(parent.showInLocationPath(req)) parents.add(parent);
			parent=parent.getParent();
		}
		for(int c=(parents.size()-1);c>=0;c--) {
			if(c<(parents.size()-1)) out.print(" - ");
			parent=parents.get(c);
			out.encodeXhtml(parent.getShortTitle());
		}
		*/
		out.print("</title>\n");
		String description = page.getDescription();
		if(description != null && (description = description.trim()).length() > 0) {
			out.print("    <meta name=\"description\" content=\"").encodeXmlAttribute(description).print('"');
			html.selfClose().nl();
		}
		String keywords = page.getKeywords();
		if(keywords != null && (keywords = keywords.trim()).length() > 0) {
			out.print("    <meta name=\"keywords\" content=\"").encodeXmlAttribute(keywords).print('"');
			html.selfClose().nl();
		}
		// TODO: Review HTML 4/HTML 5 differences from here
		String copyright = page.getCopyright(req, resp, page);
		if(copyright!=null && copyright.length()>0) {
			out.print("    <meta name=\"copyright\" content=\"").encodeXmlAttribute(copyright).print('"');
			html.selfClose();
			out.print('\n');
		}
		out.print("    <link rel=\"stylesheet\" href=\"").encodeXmlAttribute(req.getEncodedURLForPath("/layout/text/global.css", null, false, resp)).print("\" type=\"text/css\"");// TODO: Include type in HTML5?
		html.selfClose().nl();
		html.script().src(req.getEncodedURLForPath("/global.js", null, false, resp)).__().nl();
		printJavaScriptIncludes(req, resp, out, page);
		// TODO: Canonical?
		out.print("  </head>\n"
				+ "  <body\n");
		int color=getBackgroundColor(req);
		if(color!=-1) out.print("    bgcolor=\"").writeHtmlColor(color).print("\"\n");
		color=getTextColor(req);
		if(color!=-1) out.print("    text=\"").writeHtmlColor(color).print("\"\n");
		color=getLinkColor(req);
		if(color!=-1) out.print("    link=\"").writeHtmlColor(color).print("\"\n");
		color=getVisitedLinkColor(req);
		if(color!=-1) out.print("    vlink=\"").writeHtmlColor(color).print("\"\n");
		color=getActiveLinkColor(req);
		if(color!=-1) out.print("    alink=\"").writeHtmlColor(color).print("\"\n");
		out.print("    onload=\"");
		// TODO: These onloads should be merged?
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null) {
			// TODO: out.print(' ');
			encodeTextInXhtmlAttribute(onload, out);
		}
		out.print("\"\n"
				+ "  >\n"
				+ "    <table cellspacing=\"10\" cellpadding=\"0\">\n");
		out.print("      <tr>\n"
				+ "        <td valign=\"top\">\n");
		printLogo(page, out, req, resp);
		boolean isLoggedIn=req.isLoggedIn();
		if(isLoggedIn) {
			out.print("          ");
			html.hr__().nl();
			// TODO: Is it POST or post?
			out.print("          Logout: <form style=\"display:inline\" id=\"logout_form\" method=\"post\" action=\"").encodeXmlAttribute(req.getEncodedURL(page, resp)).print("\"><div style=\"display:inline\">");
			req.printFormFields(out, 2);
			out.print("<input type=\"hidden\" name=\"logout_requested\" value=\"true\"");
			html.selfClose();
			out.print("<input type=\"submit\" value=\"Logout\"");
			html.selfClose();
			out.print("</div></form>\n");
		} else {
			out.print("          ");
			html.hr__().nl();
			out.print("          Login: <form style=\"display:inline\" id=\"login_form\" method=\"post\" action=\"").encodeXmlAttribute(req.getEncodedURL(page, resp)).print("\"><div style=\"display:inline\">");
			req.printFormFields(out, 2);
			out.print("<input type=\"hidden\" name=\"login_requested\" value=\"true\"");
			html.selfClose();
			out.print("<input type=\"submit\" value=\"Login\"");
			html.selfClose();
			out.print("</div></form>\n");
		}
		out.print("          ");
		html.hr__().nl();
		out.print("          <div style=\"white-space:nowrap\">\n");
		if(getLayoutChoices().length>=2) out.print("Layout: ");
		if(printWebPageLayoutSelector(page, out, req, resp)) {
			html.br__().nl();
			out.print("            Search: <form id=\"search_site\" style=\"display:inline\" method=\"post\" action=\"").encodeXmlAttribute(req.getEncodedURL(page, resp)).print("\"><div style=\"display:inline\">\n"
				+ "              <input type=\"hidden\" name=\"search_target\" value=\"entire_site\"");
			html.selfClose();
			out.print("\n");
		}
		req.printFormFields(out, 3);
		out.print("              <input type=\"text\" name=\"search_query\" size=\"12\" maxlength=\"255\"");
		html.selfClose();
		out.print("\n"
				+ "            </div></form>");
		html.br__().nl();
		out.print("          </div>\n"
				+ "          ");
		html.hr__().nl();
		out.print("          <b>Current Location</b>");
		html.br__().nl();
		out.print("          <div style=\"white-space:nowrap\">\n");
		List<WebPage> parents=new ArrayList<>();
		//parents.clear();
		WebPage parent=page;
		while(parent!=null) {
			if(parent.showInLocationPath(req)) parents.add(parent);
			parent=parent.getParent();
		}
		for(int c=(parents.size()-1);c>=0;c--) {
			parent=parents.get(c);
			String navAlt=parent.getNavImageAlt(req);
			String navSuffix=parent.getNavImageSuffix(req);
			out.print("            <a href=\"").encodeXmlAttribute(req.getEncodedURL(parent, resp)).print("\">").encodeXhtml(TreePage.replaceHTML(navAlt));
			if(navSuffix!=null) out.print(" (").encodeXhtml(navSuffix).print(')');
			out.print("</a>");
			html.br__().nl();
		}
		out.print("          </div>\n"
				+ "          ");
		html.hr__().nl();
		out.print("          <b>Related Pages</b>");
		html.br__().nl();
		out.print("          <div style=\"white-space:nowrap\">\n");
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
				out.print("          <a href=\"").encodeXmlAttribute(tpage.getNavImageURL(req, resp, null)).print("\">").encodeXhtml(TreePage.replaceHTML(navAlt));
				if(navSuffix!=null) out.print(" (").encodeXhtml(navSuffix).print(')');
				out.print("</a>");
				html.br__().nl();
			}
		}
		out.print("          </div>\n"
				+ "          ");
		html.hr__().nl();
		printBelowRelatedPages(out, req);
		out.print("        </td>\n"
				+ "        <td valign=\"top\">");
		WebPage[] commonPages=getCommonPages(page, req);
		if(commonPages!=null && commonPages.length>0) {
			out.print("        <table cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%\"><tr>\n");
			for(int c=0;c<commonPages.length;c++) {
				if(c>0) out.print("          <td style=\"text-align:center;width:1%\">|</td>\n");
				WebPage tpage=commonPages[c];
				out.print("          <td style=\"white-space:nowrap; text-align:center; width:").print((101-commonPages.length)/commonPages.length).print("%\"><a href=\"").encodeXmlAttribute(tpage.getNavImageURL(req, resp, null)).print("\">").print(tpage.getNavImageAlt(req)).print("</a></td>\n");
			}
			out.print("        </tr></table>\n");
		}
	}

	/**
	 * Gets the Google Analytics New Tracking Code (ga.js) or <code>null</code>
	 * if unavailable.
	 */
	public String getGoogleAnalyticsNewTrackingCode(ServletContext servletContext) {
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
				+ "    </table>\n"
				+ "  </body>\n");
		HtmlTag.endHtmlTag(out);
		out.write('\n');
	}

	/**
	 * Starts the content area of a page.
	 */
	@Override
	public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException {
		Html html = HtmlEE.get(req, out);
		out.print("<table cellpadding=\"0\" cellspacing=\"0\"");
		if(preferredWidth != -1) {
			out.print(" style=\"width:").print(preferredWidth).print("px\"");
		}
		out.print(">\n"
				+ "  <tr>\n");
		int totalColumns=0;
		for(int c=0;c<contentColumnSpans.length;c++) {
			if(c>0) totalColumns++;
			totalColumns+=contentColumnSpans[c];
		}
		out.print("    <td");
		if(totalColumns!=1) out.print(" colspan=\"").print(totalColumns).print('"');
		out.print('>');
		html.hr__();
		out.print("</td>\n"
				+ "  </tr>\n");
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	@Override
	public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException {
		Html html = HtmlEE.get(req, out);
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
			if(colspan!=1) out.print(" colspan=\"").print(colspan).print('"');
			out.print('>');
			html.hr__();
			out.print("</td>\n");
		}
		out.print("  </tr>\n");
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	@Override
	public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException {
		startContentLine(out, req, resp, contentColumns, "center", null);
		out.print("<h1>").print(title).print("</h1>\n");
		endContentLine(out, req, resp, 1, false);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void startContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		out.print("  <tr>\n"
				+ "    <td");
		if(align != null || width != null) {
			out.append(" style=\"");
			if(align != null) {
				out.append("text-align:");
				encodeTextInXhtmlAttribute(align, out);
			}
			if(width != null) {
				if(align != null) out.append(';');
				appendWidthStyle(width, out);
			}
			out.append('"');
		}
		out.print(" valign=\"top\"");
		if(colspan!=1) out.print(" colspan=\"").print(colspan).print('"');
		out.print('>');
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void printContentVerticalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
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
		if(align != null || width != null) {
			out.append(" style=\"");
			if(align != null) {
				out.append("text-align:");
				encodeTextInXhtmlAttribute(align, out);
			}
			if(width != null) {
				if(align != null) out.append(';');
				appendWidthStyle(width, out);
			}
			out.append('"');
		}
		out.print(" valign=\"top\"");
		if(colspan!=1) out.print(" colspan=\"").print(colspan).print('"');
		if(rowspan!=1) out.print(" rowspan=\"").print(rowspan).print('"');
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
		Html html = page.getHtml(req, out);
		int totalColumns=0;
		for(int c=0;c<contentColumnSpans.length;c++) {
			if(c>0) totalColumns+=1;
			totalColumns+=contentColumnSpans[c];
		}
		out.print("  <tr><td");
		if(totalColumns!=1) out.print(" colspan=\"").print(totalColumns).print('"');
		out.print('>');
		html.hr__();
		out.print("</td></tr>\n");
		String copyright=page.getCopyright(req, resp, page);
		if(copyright!=null && copyright.length()>0) {
			out.print("  <tr><td");
			if(totalColumns!=1) out.print(" colspan=\"").print(totalColumns).print('"');
			out.print(" style=\"text-align:center\"><span style=\"font-size:x-small\">").print(copyright).print("</span></td></tr>\n");
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
