/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2003-2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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
import com.aoindustries.html.Document;
import com.aoindustries.html.Link;
import com.aoindustries.html.Meta;
import com.aoindustries.html.Script;
import com.aoindustries.html.util.GoogleAnalytics;
import static com.aoindustries.lang.Strings.trimNullIfEmpty;
import static com.aoindustries.taglib.AttributeUtils.appendWidthStyle;
import com.aoindustries.taglib.GlobalAttributes;
import com.aoindustries.taglib.HtmlTag;
import com.aoindustries.web.resources.registry.Group;
import com.aoindustries.web.resources.registry.Registry;
import com.aoindustries.web.resources.registry.Style;
import com.aoindustries.web.resources.renderer.Renderer;
import com.aoindustries.web.resources.servlet.RegistryEE;
import java.io.IOException;
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
//       This would start to decompose this in a way like we're doing in SemanticCMS 2.
//       Probably not worth it for this legacy system?  Could they converge?
public class TextOnlyLayout extends WebPageLayout {

	// Matches TextSkin.NAME
	public static final String NAME = "Text";

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
	public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, Document document, String align, String width, boolean nowrap) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		document.out.write("<table style=\"border:5px outset #a0a0a0");
		if(align != null) {
			document.out.write(";text-align:");
			encodeTextInXhtmlAttribute(align, document.out);
		}
		if(width != null) {
			document.out.write(';');
			appendWidthStyle(width, document.out);
		}
		document.out.write("\" cellpadding=\"0\" cellspacing=\"0\">\n"
		+ "  <tr>\n"
		+ "    <td class=\"aoLightRow\" style=\"padding:4px");
		if(nowrap) document.out.write(";white-space:nowrap");
		document.out.write("\">");
	}

	@Override
	public void endLightArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException {
		document.out.write("</td>\n"
		+ "  </tr>\n"
		+ "</table>\n");
	}

	@Override
	public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, Document document, String align, String width, boolean nowrap) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		document.out.write("<table style=\"border:5px outset #a0a0a0");
		if(align != null) {
			document.out.write(";text-align:");
			encodeTextInXhtmlAttribute(align, document.out);
		}
		if(width != null) {
			document.out.write(';');
			appendWidthStyle(width, document.out);
		}
		document.out.write("\" cellpadding=\"0\" cellspacing=\"0\">\n"
		+ "  <tr>\n"
		+ "    <td class=\"aoWhiteRow\" style=\"background-color:white;padding:4px");
		if(nowrap) document.out.write(";white-space:nowrap");
		document.out.write("\">");
	}

	@Override
	public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException {
		document.out.write("</td>\n"
		+ "  </tr>\n"
		+ "</table>\n");
	}

	@Override
	@SuppressWarnings("deprecation")
	public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		Document document,
		String onload
	) throws ServletException, IOException {
		boolean isOkResponseStatus = (resp.getStatus() == HttpServletResponse.SC_OK);
		ServletContext servletContext = req.getServletContext();
		String trackingId = getGoogleAnalyticsNewTrackingCode(servletContext);
		// Write doctype
		document.xmlDeclaration(resp.getCharacterEncoding());
		document.doctype();
		// Write <html>
		HtmlTag.beginHtmlTag(resp, document.out, document.serialization, (GlobalAttributes)null); document.out.write("\n"
		+ "  <head>\n");
		// If this is not the default layout, then robots noindex
		if(!isOkResponseStatus || !getName().equals(getLayoutChoices()[0])) {
			document.out.write("    "); document.meta(Meta.Name.ROBOTS).content("noindex, nofollow").__().nl();
		}
		if(document.doctype == Doctype.HTML5) {
			document.out.write("    "); document.meta().charset(resp.getCharacterEncoding()).__().nl();
		} else {
			document.out.write("    "); document.meta(Meta.HttpEquiv.CONTENT_TYPE).content(resp.getContentType()).__().out.write("\n"
			// Default style language
			+ "    "); document.meta(Meta.HttpEquiv.CONTENT_STYLE_TYPE).content(com.aoindustries.html.Style.Type.TEXT_CSS).__().out.write("\n"
			+ "    "); document.meta(Meta.HttpEquiv.CONTENT_SCRIPT_TYPE).content(Script.Type.TEXT_JAVASCRIPT).__().nl();
		}
		if(document.doctype == Doctype.HTML5) {
			GoogleAnalytics.writeGlobalSiteTag(document, trackingId);
		} else {
			GoogleAnalytics.writeAnalyticsJs(document, trackingId);
		}
		// Mobile support
		document.out.write("    "); document.meta(Meta.Name.VIEWPORT).content("width=device-width, initial-scale=1.0").__().out.write("\n"
		// TODO: This is probably only appropriate for single-page applications!
		//       See https://medium.com/@firt/dont-use-ios-web-app-meta-tag-irresponsibly-in-your-progressive-web-apps-85d70f4438cb
		+ "    "); document.meta(Meta.Name.APPLE_MOBILE_WEB_APP_CAPABLE).content("yes").__().out.write("\n"
		+ "    "); document.meta(Meta.Name.APPLE_MOBILE_WEB_APP_STATUS_BAR_STYLE).content("black").__().nl();
		// Authors
		// TODO: dcterms copyright
		String author = page.getAuthor();
		if(author != null && !(author = author.trim()).isEmpty()) {
			document.out.write("    "); document.meta(Meta.Name.AUTHOR).content(author).__().nl();
		}
		String authorHref = page.getAuthorHref(req, resp);
		if(authorHref != null && !(authorHref = authorHref.trim()).isEmpty()) {
			document.out.write("    "); document.link(Link.Rel.AUTHOR).href(authorHref).__().nl();
		}
		document.out.write("    <title>");
		// No more page stack, just show current page only
		document.text(page.getTitle());
		/*
		List<WebPage> parents=new ArrayList<>();
		WebPage parent=page;
		while(parent!=null) {
			if(parent.showInLocationPath(req)) parents.add(parent);
			parent=parent.getParent();
		}
		for(int c=(parents.size()-1);c>=0;c--) {
			if(c<(parents.size()-1)) html.out.write(" - ");
			parent=parents.get(c);
			encodeTextInXhtml(parent.getTitle(), html.out); // Encode directly, since doesn't support Markup
		}
		*/
		document.out.write("</title>\n");
		String description = page.getDescription();
		if(description != null && !(description = description.trim()).isEmpty()) {
			document.out.write("    "); document.meta(Meta.Name.DESCRIPTION).content(description).__().nl();
		}
		String keywords = page.getKeywords();
		if(keywords != null && !(keywords = keywords.trim()).isEmpty()) {
			document.out.write("    "); document.meta(Meta.Name.KEYWORDS).content(keywords).__().nl();
		}
		// TODO: Review HTML 4/HTML 5 differences from here
		String copyright = page.getCopyright(req, resp, page);
		if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
			// TODO: Dublin Core: https://stackoverflow.com/questions/6665312/is-the-copyright-meta-tag-valid-in-html5
			document.out.write("    "); document.meta().name("copyright").content(copyright).__().nl();
		}

		// Configure layout resources
		Registry requestRegistry = RegistryEE.Request.get(servletContext, req);
		configureResources(servletContext, req, resp, page, requestRegistry);
		// Configure page resources
		Registry pageRegistry = RegistryEE.Page.get(req);
		if(pageRegistry == null) throw new ServletException("page-scope registry not found.  WebPage.service(ServletRequest,ServletResponse) invoked?");
		page.configureResources(servletContext, req, resp, this, pageRegistry);
		// Render links
		document.out.write("    "); Renderer.get(servletContext).renderStyles(
			req,
			resp,
			document,
			"    ",
			true, // registeredActivations
			null, // No additional activations
			requestRegistry, // request-scope
			RegistryEE.Session.get(req.getSession(false)), // session-scope
			pageRegistry
		); document.nl()
		.script().src(req.getEncodedURLForPath("/global.js", null, false, resp)).__().nl();
		printJavaScriptIncludes(req, resp, document, page);
		// TODO: Canonical?
		document.out.write("  </head>\n"
		+ "  <body\n");
		int color = getBackgroundColor(req);
		if(color != -1) {
			document.out.write("    bgcolor=\"");
			ChainWriter.writeHtmlColor(color, document.out);
			document.out.write("\"\n");
		}
		color = getTextColor(req);
		if(color != -1) {
			document.out.write("    text=\"");
			ChainWriter.writeHtmlColor(color, document.out);
			document.out.write("\"\n");
		}
		color = getLinkColor(req);
		if(color != -1) {
			document.out.write("    link=\"");
			ChainWriter.writeHtmlColor(color, document.out);
			document.out.write("\"\n");
		}
		color = getVisitedLinkColor(req);
		if(color != -1) {
			document.out.write("    vlink=\"");
			ChainWriter.writeHtmlColor(color, document.out);
			document.out.write("\"\n");
		}
		color = getActiveLinkColor(req);
		if(color != -1) {
			document.out.write("    alink=\"");
			ChainWriter.writeHtmlColor(color, document.out);
			document.out.write("\"\n");
		}
		// TODO: These onloads should be merged?
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null) {
			document.out.write("    onload=\""); encodeJavaScriptInXhtmlAttribute(onload, document.out); document.out.write("\"\n");
		}
		document.out.write("  >\n"
		+ "    <table cellspacing=\"10\" cellpadding=\"0\">\n"
		+ "      <tr>\n"
		+ "        <td valign=\"top\">\n");
		printLogo(page, document, req, resp);
		boolean isLoggedIn=req.isLoggedIn();
		if(isLoggedIn) {
			document.out.write("          "); document.hr__().out.write("\n"
			+ "          Logout: <form style=\"display:inline\" id=\"logout_form\" method=\"post\" action=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), document.out);
			document.out.write("\"><div style=\"display:inline\">");
			req.printFormFields(document);
			document.input().hidden().name(WebSiteRequest.LOGOUT_REQUESTED).value(true).__()
			.input().submit__("Logout").out.write("</div></form>\n");
		} else {
			document.out.write("          "); document.hr__().out.write("\n"
			+ "          Login: <form style=\"display:inline\" id=\"login_form\" method=\"post\" action=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), document.out);
			document.out.write("\"><div style=\"display:inline\">");
			req.printFormFields(document);
			document.input().hidden().name(WebSiteRequest.LOGIN_REQUESTED).value(true).__()
			.input().submit__("Login").out.write("</div></form>\n");
		}
		document.out.write("          "); document.hr__().out.write("\n"
		+ "          <div style=\"white-space:nowrap\">\n");
		if(getLayoutChoices().length >= 2) document.text("Layout:").out.write(' ');
		if(printWebPageLayoutSelector(page, document, req, resp)) {
			document.br__().out.write("\n"
			+ "            "); document.text("Search:"); document.out.write(" <form id=\"search_site\" style=\"display:inline\" method=\"post\" action=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), document.out);
			document.out.write("\"><div style=\"display:inline\">\n"
			+ "              "); document.input().hidden().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_ENTIRE_SITE).__().nl();
		}
		req.printFormFields(document);
		document.out.write("              "); document.input().text().name(WebSiteRequest.SEARCH_QUERY).size(12).maxlength(255).__().out.write("\n"
		+ "            </div></form>"); document.br__().out.write("\n"
		+ "          </div>\n"
		+ "          "); document.hr__().out.write("\n"
		+ "          <b>"); document.text("Current Location"); document.out.write("</b>"); document.br__().out.write("\n"
		+ "          <div style=\"white-space:nowrap\">\n");
		List<WebPage> parents=new ArrayList<>();
		//parents.clear();
		WebPage parent=page;
		while(parent!=null) {
			if(parent.showInLocationPath(req)) parents.add(parent);
			parent=parent.getParent();
		}
		for(int c=(parents.size()-1);c>=0;c--) {
			parent=parents.get(c);
			String navAlt = parent.getNavImageAlt(req);
			String navSuffix=parent.getNavImageSuffix(req);
			document.out.write("            <a href=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(parent, resp), document.out);
			document.out.write("\">"); document.text(navAlt);
			if(navSuffix != null) {
				document.out.write(" ("); document.text(navSuffix).out.write(')');
			}
			document.out.write("</a>"); document.br__().nl();
		}
		document.out.write("          </div>\n"
		+ "          "); document.hr__().out.write("\n"
		+ "          <b>"); document.text("Related Pages"); document.out.write("</b>"); document.br__().out.write("\n"
		+ "          <div style=\"white-space:nowrap\">\n");
		WebPage[] children = page.getCachedChildren(req, resp);
		parent = page;
		if(children.length == 0) {
			parent = page.getParent();
			if(parent != null) children = parent.getCachedChildren(req, resp);
		}

		for(int c = -1; c < children.length; c++) {
			WebPage tpage;
			if (c == -1) {
				if (parent!=null && parent.includeNavImageAsParent()) tpage = parent;
				else tpage = null;
			} else {
				tpage = children[c];
			}
			if(
				tpage != null
				&& (
					tpage.useNavImage()
					|| tpage.equals(page)
					|| (
						tpage.includeNavImageAsParent()
						&& tpage.equals(parent)
					)
				)
			) {
				String navAlt = tpage.getNavImageAlt(req);
				String navSuffix=tpage.getNavImageSuffix(req);
				//boolean isSelected=tpage.equals(page);
				document.out.write("          <a href=\"");
				encodeTextInXhtmlAttribute(tpage.getNavImageURL(req, resp, null), document.out);
				document.out.write("\">"); document.text(navAlt);
				if(navSuffix != null) {
					document.out.write(" ("); document.text(navSuffix).out.write(')');
				}
				document.out.write("</a>"); document.br__().nl();
			}
		}
		document.out.write("          </div>\n"
		+ "          "); document.hr__().nl();
		printBelowRelatedPages(document, req);
		document.out.write("        </td>\n"
		+ "        <td valign=\"top\">");
		WebPage[] commonPages = getCommonPages(page, req);
		if(commonPages != null && commonPages.length > 0) {
			document.out.write("        <table cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%\"><tr>\n");
			for(int c = 0; c < commonPages.length; c++) {
				if(c > 0) document.out.write("          <td style=\"text-align:center;width:1%\">|</td>\n");
				WebPage tpage = commonPages[c];
				document.out.append("          <td style=\"white-space:nowrap; text-align:center; width:").append(((101 - commonPages.length) / commonPages.length) + "%").append("\"><a href=\"");
				encodeTextInXhtmlAttribute(tpage.getNavImageURL(req, resp, null), document.out);
				document.out.write("\">"); document.text(tpage.getNavImageAlt(req)); document.out.write("</a></td>\n");
			}
			document.out.write("        </tr></table>\n");
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
		Document document
	) throws ServletException, IOException {
		document.out.write("        </td>\n"
		+ "      </tr>\n"
		+ "    </table>\n"
		+ "  </body>\n");
		HtmlTag.endHtmlTag(document.out); document.nl();
	}

	/**
	 * Starts the content area of a page.
	 */
	@Override
	public void startContent(Document document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException {
		document.out.write("<table cellpadding=\"0\" cellspacing=\"0\"");
		if(preferredWidth != -1) {
			document.out.append(" style=\"width:").append(Integer.toString(preferredWidth)).append("px\"");
		}
		document.out.write(">\n"
		+ "  <tr>\n");
		int totalColumns = 0;
		for(int c = 0; c < contentColumnSpans.length; c++) {
			if(c > 0) totalColumns++;
			totalColumns += contentColumnSpans[c];
		}
		document.out.write("    <td");
		if(totalColumns != 1) {
			document.out.append(" colspan=\"").append(Integer.toString(totalColumns)).append('"');
		}
		document.out.write('>'); document.hr__().out.write("</td>\n"
		+ "  </tr>\n");
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	@Override
	public void printContentHorizontalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException {
		document.out.write("  <tr>\n");
		for(int c = 0; c < colspansAndDirections.length; c += 2) {
			int direction = (c == 0) ? -1 : colspansAndDirections[c - 1];
			if(direction != -1) {
				switch(direction) {
					case UP:
						document.out.write("    <td>&#160;</td>\n");
						break;
					case DOWN:
						document.out.write("    <td>&#160;</td>\n");
						break;
					case UP_AND_DOWN:
						document.out.write("    <td>&#160;</td>\n");
						break;
					default: throw new IllegalArgumentException("Unknown direction: " + direction);
				}
			}

			int colspan=colspansAndDirections[c];
			document.out.write("    <td");
			if(colspan != 1) document.out.append(" colspan=\"").append(Integer.toString(colspan)).append('"');
			document.out.write('>'); document.hr__().out.write("</td>\n");
		}
		document.out.write("  </tr>\n");
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	@Override
	public void printContentTitle(Document document, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException {
		startContentLine(document, req, resp, contentColumns, "center", null);
		document.h1__(title).nl();
		endContentLine(document, req, resp, 1, false);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void startContentLine(Document document, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		document.out.write("  <tr>\n"
		+ "    <td");
		if(align != null || width != null) {
			document.out.write(" style=\"");
			if(align != null) {
				document.out.write("text-align:");
				encodeTextInXhtmlAttribute(align, document.out);
			}
			if(width != null) {
				if(align != null) document.out.write(';');
				appendWidthStyle(width, document.out);
			}
			document.out.write('"');
		}
		document.out.write(" valign=\"top\"");
		if(colspan != 1) {
			document.out.append(" colspan=\"").append(Integer.toString(colspan)).append('"');
		}
		document.out.write('>');
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void printContentVerticalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		document.out.write("    </td>\n");
		switch(direction) {
			case UP_AND_DOWN:
				document.out.write("    <td>&#160;</td>\n");
				break;
			case NONE:
				break;
			default: throw new IllegalArgumentException("Unknown direction: " + direction);
		}
		document.out.write("    <td");
		if(align != null || width != null) {
			document.out.write(" style=\"");
			if(align != null) {
				document.out.write("text-align:");
				encodeTextInXhtmlAttribute(align, document.out);
			}
			if(width != null) {
				if(align != null) document.out.write(';');
				appendWidthStyle(width, document.out);
			}
			document.out.write('"');
		}
		document.out.write(" valign=\"top\"");
		if(colspan != 1) document.out.append(" colspan=\"").append(Integer.toString(colspan)).append('"');
		if(rowspan != 1) document.out.append(" rowspan=\"").append(Integer.toString(rowspan)).append('"');
		document.out.write('>');
	}

	/**
	 * Ends one line of content.
	 */
	@Override
	public void endContentLine(Document document, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws IOException {
		document.out.write("    </td>\n"
		+ "  </tr>\n");
	}

	/**
	 * Ends the content area of a page.
	 */
	@Override
	public void endContent(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws ServletException, IOException {
		int totalColumns=0;
		for(int c = 0; c < contentColumnSpans.length; c++) {
			if(c > 0) totalColumns += 1;
			totalColumns += contentColumnSpans[c];
		}
		document.out.write("  <tr><td");
		if(totalColumns != 1) document.out.append(" colspan=\"").append(Integer.toString(totalColumns)).append('"');
		document.out.write('>'); document.hr__().out.write("</td></tr>\n");
		String copyright = page.getCopyright(req, resp, page);
		if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
			document.out.write("  <tr><td");
			if(totalColumns != 1) document.out.append(" colspan=\"").append(Integer.toString(totalColumns)).append('"');
			document.out.write(" style=\"text-align:center\"><span style=\"font-size:x-small\">"); document.text(copyright); document.out.write("</span></td></tr>\n");
		}
		document.out.write("</table>\n");
	}

	@Override
	public String getName() {
		return NAME;
	}

	public WebPage[] getCommonPages(WebPage page, WebSiteRequest req) throws ServletException {
		return null;
	}

	public void printLogo(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	/**
	 * Prints content below the related pages area on the left.
	 */
	public void printBelowRelatedPages(Document document, WebSiteRequest req) throws ServletException, IOException {
	}
}
