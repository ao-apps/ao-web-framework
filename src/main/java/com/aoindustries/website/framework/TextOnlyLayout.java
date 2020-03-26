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
import com.aoindustries.encoding.Doctype;
import static com.aoindustries.encoding.JavaScriptInXhtmlAttributeEncoder.encodeJavaScriptInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Html;
import com.aoindustries.html.Link;
import com.aoindustries.html.Meta;
import com.aoindustries.html.Script;
import com.aoindustries.html.util.GoogleAnalytics;
import static com.aoindustries.lang.Strings.trimNullIfEmpty;
import static com.aoindustries.taglib.AttributeUtils.appendWidthStyle;
import com.aoindustries.taglib.HtmlTag;
import com.aoindustries.web.resources.registry.Group;
import com.aoindustries.web.resources.registry.Registry;
import com.aoindustries.web.resources.registry.Style;
import com.aoindustries.web.resources.renderer.Renderer;
import com.aoindustries.web.resources.servlet.RegistryEE;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletResponse;

/**
 * The default text-only layout.
 *
 * @author  AO Industries, Inc.
 */
// TODO: Move into own mircoproject (also split into ao-web-framework-html)?
// TODO: This would start to decompose this in a way like we're doing in SemanticCMS 2.
// TODO: Probably not worth it for this legacy system?  Could they converge?
public class TextOnlyLayout extends WebPageLayout {

	/**
	 * The name of the {@linkplain com.aoindustries.web.resources.servlet.RegistryEE.Application application-scope}
	 * group that will be used for text layout web resources.
	 */
	public static final Group.Name RESOURCE_GROUP = new Group.Name(TextOnlyLayout.class.getName());

	public static final Style GLOBAL_CSS = new Style("/layout/text/global.css");

	@WebListener
	public static class Initializer implements ServletContextListener {
		@Override
		public void contextInitialized(ServletContextEvent event) {
			RegistryEE.Application.get(event.getServletContext())
				.getGroup(RESOURCE_GROUP)
				.styles
				.add(GLOBAL_CSS);
		}
		@Override
		public void contextDestroyed(ServletContextEvent event) {
			// Do nothing
		}
	}

	public TextOnlyLayout(String[] layoutChoices) {
		super(layoutChoices);
	}

	@Override
	public void configureResources(ServletContext servletContext, WebSiteRequest req, HttpServletResponse resp, WebPage page, Registry requestRegistry) {
		super.configureResources(servletContext, req, resp, page, requestRegistry);
		requestRegistry.activate(RESOURCE_GROUP);
	}

	@Override
	// TODO: Use Html instead of ChainWriter for layouts
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
	@SuppressWarnings("deprecation")
	public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		String onload
	) throws ServletException, IOException, SQLException {
		boolean isOkResponseStatus;
		{
			Integer responseStatus = (Integer)req.getAttribute(HTTP_SERVLET_RESPONSE_STATUS);
			isOkResponseStatus = responseStatus==null || responseStatus==HttpServletResponse.SC_OK;
		}
		ServletContext servletContext = req.getServletContext();
		String trackingId = getGoogleAnalyticsNewTrackingCode(servletContext);
		// Write doctype
		Html html = page.getHtml(req, resp, out);
		html.xmlDeclaration(resp.getCharacterEncoding());
		html.doctype();
		// Write <html>
		HtmlTag.beginHtmlTag(resp, out, html.serialization, null); // TODO: Move to Html class
		out.write("\n"
				+ "  <head>\n");
		// If this is not the default layout, then robots noindex
		if(!isOkResponseStatus || !getName().equals(getLayoutChoices()[0])) {
			out.print("    ");
			html.meta(Meta.Name.ROBOTS).content("noindex, nofollow").__().nl();
		}
		if(html.doctype == Doctype.HTML5) {
			out.print("    ");
			html.meta().charset(resp.getCharacterEncoding()).__().nl();
		} else {
			out.print("    ");
			html.meta(Meta.HttpEquiv.CONTENT_TYPE).content(resp.getContentType()).__().nl();
			// Default style language
			out.print("    ");
			html.meta(Meta.HttpEquiv.CONTENT_STYLE_TYPE).content(com.aoindustries.html.Style.Type.TEXT_CSS).__().nl();
			out.print("    ");
			html.meta(Meta.HttpEquiv.CONTENT_SCRIPT_TYPE).content(Script.Type.TEXT_JAVASCRIPT).__().nl();
		}
		if(html.doctype == Doctype.HTML5) {
			GoogleAnalytics.writeGlobalSiteTag(html, trackingId);
		} else {
			GoogleAnalytics.writeAnalyticsJs(html, trackingId);
		}
		// Mobile support
		out.print("    ");
		html.meta(Meta.Name.VIEWPORT).content("width=device-width, initial-scale=1.0").__().nl();
		out.print("    ");
		// TODO: This is probably only appropriate for single-page applications!
		// TODO: See https://medium.com/@firt/dont-use-ios-web-app-meta-tag-irresponsibly-in-your-progressive-web-apps-85d70f4438cb
		html.meta(Meta.Name.APPLE_MOBILE_WEB_APP_CAPABLE).content("yes").__().nl();
		out.print("    ");
		html.meta(Meta.Name.APPLE_MOBILE_WEB_APP_STATUS_BAR_STYLE).content("black").__().nl();
		// Authors
		// TODO: dcterms copyright
		String author = page.getAuthor();
		if(author != null && (author = author.trim()).length() > 0) {
			out.print("    ");
			html.meta(Meta.Name.AUTHOR).content(author).__().nl();
		}
		String authorHref = page.getAuthorHref(req, resp);
		if(authorHref != null && (authorHref = authorHref.trim()).length() > 0) {
			out.print("    ");
			html.link(Link.Rel.AUTHOR).href(authorHref).__().nl();
		}
		out.print("    <title>");
		// No more page stack, just show current page only
		out.textInXhtml(page.getTitle());
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
			out.encodeXhtml(parent.getTitle());
		}
		*/
		out.print("</title>\n");
		String description = page.getDescription();
		if(description != null && (description = description.trim()).length() > 0) {
			out.print("    ");
			html.meta(Meta.Name.DESCRIPTION).content(description).__().nl();
		}
		String keywords = page.getKeywords();
		if(keywords != null && (keywords = keywords.trim()).length() > 0) {
			out.print("    ");
			html.meta(Meta.Name.KEYWORDS).content(keywords).__().nl();
		}
		// TODO: Review HTML 4/HTML 5 differences from here
		String copyright = page.getCopyright(req, resp, page);
		if(copyright!=null && copyright.length()>0) {
			out.print("    ");
			// TODO: Dublin Core: https://stackoverflow.com/questions/6665312/is-the-copyright-meta-tag-valid-in-html5
			html.meta().name("copyright").content(copyright).__().nl();
		}

		// Configure layout resources
		Registry requestRegistry = RegistryEE.Request.get(servletContext, req);
		configureResources(servletContext, req, resp, page, requestRegistry);
		// Configure page resources
		Registry pageRegistry = RegistryEE.Page.get(req);
		if(pageRegistry == null) throw new ServletException("page-scope registry not found.  WebPage.service(ServletRequest,ServletResponse) invoked?");
		page.configureResources(servletContext, req, resp, this, pageRegistry);
		// Render links
		out.print("    ");
		Renderer.get(servletContext).renderStyles(
			req,
			resp,
			html,
			"    ",
			true, // registeredActivations
			null, // No additional activations
			requestRegistry, // request-scope
			RegistryEE.Session.get(req.getSession(false)), // session-scope
			pageRegistry
		);
		html.nl();

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
		// TODO: These onloads should be merged?
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null) {
			out.print("    onload=\"");
			encodeJavaScriptInXhtmlAttribute(onload, out);
			out.print("\"\n");
		}
		out.print("  >\n"
				+ "    <table cellspacing=\"10\" cellpadding=\"0\">\n");
		out.print("      <tr>\n"
				+ "        <td valign=\"top\">\n");
		printLogo(page, out, req, resp);
		boolean isLoggedIn=req.isLoggedIn();
		if(isLoggedIn) {
			out.print("          ");
			html.hr__().nl();
			out.print("          Logout: <form style=\"display:inline\" id=\"logout_form\" method=\"post\" action=\"").textInXmlAttribute(req.getEncodedURL(page, resp)).print("\"><div style=\"display:inline\">");
			req.printFormFields(html);
			html.input.hidden().name("logout_requested").value(true).__();
			html.input.submit__("Logout");
			out.print("</div></form>\n");
		} else {
			out.print("          ");
			html.hr__().nl();
			out.print("          Login: <form style=\"display:inline\" id=\"login_form\" method=\"post\" action=\"").textInXmlAttribute(req.getEncodedURL(page, resp)).print("\"><div style=\"display:inline\">");
			req.printFormFields(html);
			html.input.hidden().name("login_requested").value(true).__();
			html.input.submit__("Login");
			out.print("</div></form>\n");
		}
		out.print("          ");
		html.hr__().nl();
		out.print("          <div style=\"white-space:nowrap\">\n");
		if(getLayoutChoices().length>=2) out.print("Layout: ");
		if(printWebPageLayoutSelector(page, out, req, resp)) {
			html.br__().nl();
			out.print("            Search: <form id=\"search_site\" style=\"display:inline\" method=\"post\" action=\"").textInXmlAttribute(req.getEncodedURL(page, resp)).print("\"><div style=\"display:inline\">\n"
				+ "              ");
			html.input.hidden().name("search_target").value("entire_site").__().nl();
		}
		req.printFormFields(html);
		out.print("              ");
		html.input.text().name("search_query").size(12).maxlength(255).__().nl();
		out.print("            </div></form>");
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
			out.print("            <a href=\"").textInXmlAttribute(req.getEncodedURL(parent, resp)).print("\">").textInXhtml(TreePage.replaceHTML(navAlt));
			if(navSuffix!=null) out.print(" (").textInXhtml(navSuffix).print(')');
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
				out.print("          <a href=\"").textInXmlAttribute(tpage.getNavImageURL(req, resp, null)).print("\">").textInXhtml(TreePage.replaceHTML(navAlt));
				if(navSuffix!=null) out.print(" (").textInXhtml(navSuffix).print(')');
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
				out.print("          <td style=\"white-space:nowrap; text-align:center; width:").print((101-commonPages.length)/commonPages.length).print("%\"><a href=\"").textInXmlAttribute(tpage.getNavImageURL(req, resp, null)).print("\">").print(tpage.getNavImageAlt(req)).print("</a></td>\n");
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
	) throws ServletException, IOException, SQLException {
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
		Html html = new Html(out);
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
		Html html = new Html(out);
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
		Html html = page.getHtml(req, resp, out);
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
