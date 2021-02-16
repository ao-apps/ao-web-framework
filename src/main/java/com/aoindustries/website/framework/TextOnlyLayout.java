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
import com.aoindustries.html.Html;
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
	public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, Html html, String align, String width, boolean nowrap) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		html.out.write("<table style=\"border:5px outset #a0a0a0");
		if(align != null) {
			html.out.write(";text-align:");
			encodeTextInXhtmlAttribute(align, html.out);
		}
		if(width != null) {
			html.out.write(';');
			appendWidthStyle(width, html.out);
		}
		html.out.write("\" cellpadding=\"0\" cellspacing=\"0\">\n"
		+ "  <tr>\n"
		+ "    <td class=\"aoLightRow\" style=\"padding:4px");
		if(nowrap) html.out.write(";white-space:nowrap");
		html.out.write("\">");
	}

	@Override
	public void endLightArea(WebSiteRequest req, HttpServletResponse resp, Html html) throws IOException {
		html.out.write("</td>\n"
		+ "  </tr>\n"
		+ "</table>\n");
	}

	@Override
	public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, Html html, String align, String width, boolean nowrap) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		html.out.write("<table style=\"border:5px outset #a0a0a0");
		if(align != null) {
			html.out.write(";text-align:");
			encodeTextInXhtmlAttribute(align, html.out);
		}
		if(width != null) {
			html.out.write(';');
			appendWidthStyle(width, html.out);
		}
		html.out.write("\" cellpadding=\"0\" cellspacing=\"0\">\n"
		+ "  <tr>\n"
		+ "    <td class=\"aoWhiteRow\" style=\"background-color:white;padding:4px");
		if(nowrap) html.out.write(";white-space:nowrap");
		html.out.write("\">");
	}

	@Override
	public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, Html html) throws IOException {
		html.out.write("</td>\n"
		+ "  </tr>\n"
		+ "</table>\n");
	}

	@Override
	@SuppressWarnings("deprecation")
	public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		Html html,
		String onload
	) throws ServletException, IOException, SQLException {
		boolean isOkResponseStatus = (resp.getStatus() == HttpServletResponse.SC_OK);
		ServletContext servletContext = req.getServletContext();
		String trackingId = getGoogleAnalyticsNewTrackingCode(servletContext);
		// Write doctype
		html.xmlDeclaration(resp.getCharacterEncoding());
		html.doctype();
		// Write <html>
		HtmlTag.beginHtmlTag(resp, html.out, html.serialization, (GlobalAttributes)null); html.out.write("\n"
		+ "  <head>\n");
		// If this is not the default layout, then robots noindex
		if(!isOkResponseStatus || !getName().equals(getLayoutChoices()[0])) {
			html.out.write("    "); html.meta(Meta.Name.ROBOTS).content("noindex, nofollow").__().nl();
		}
		if(html.doctype == Doctype.HTML5) {
			html.out.write("    "); html.meta().charset(resp.getCharacterEncoding()).__().nl();
		} else {
			html.out.write("    "); html.meta(Meta.HttpEquiv.CONTENT_TYPE).content(resp.getContentType()).__().out.write("\n"
			// Default style language
			+ "    "); html.meta(Meta.HttpEquiv.CONTENT_STYLE_TYPE).content(com.aoindustries.html.Style.Type.TEXT_CSS).__().out.write("\n"
			+ "    "); html.meta(Meta.HttpEquiv.CONTENT_SCRIPT_TYPE).content(Script.Type.TEXT_JAVASCRIPT).__().nl();
		}
		if(html.doctype == Doctype.HTML5) {
			GoogleAnalytics.writeGlobalSiteTag(html, trackingId);
		} else {
			GoogleAnalytics.writeAnalyticsJs(html, trackingId);
		}
		// Mobile support
		html.out.write("    "); html.meta(Meta.Name.VIEWPORT).content("width=device-width, initial-scale=1.0").__().out.write("\n"
		// TODO: This is probably only appropriate for single-page applications!
		//       See https://medium.com/@firt/dont-use-ios-web-app-meta-tag-irresponsibly-in-your-progressive-web-apps-85d70f4438cb
		+ "    "); html.meta(Meta.Name.APPLE_MOBILE_WEB_APP_CAPABLE).content("yes").__().out.write("\n"
		+ "    "); html.meta(Meta.Name.APPLE_MOBILE_WEB_APP_STATUS_BAR_STYLE).content("black").__().nl();
		// Authors
		// TODO: dcterms copyright
		String author = page.getAuthor();
		if(author != null && !(author = author.trim()).isEmpty()) {
			html.out.write("    "); html.meta(Meta.Name.AUTHOR).content(author).__().nl();
		}
		String authorHref = page.getAuthorHref(req, resp);
		if(authorHref != null && !(authorHref = authorHref.trim()).isEmpty()) {
			html.out.write("    "); html.link(Link.Rel.AUTHOR).href(authorHref).__().nl();
		}
		html.out.write("    <title>");
		// No more page stack, just show current page only
		html.text(page.getTitle());
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
		html.out.write("</title>\n");
		String description = page.getDescription();
		if(description != null && !(description = description.trim()).isEmpty()) {
			html.out.write("    "); html.meta(Meta.Name.DESCRIPTION).content(description).__().nl();
		}
		String keywords = page.getKeywords();
		if(keywords != null && !(keywords = keywords.trim()).isEmpty()) {
			html.out.write("    "); html.meta(Meta.Name.KEYWORDS).content(keywords).__().nl();
		}
		// TODO: Review HTML 4/HTML 5 differences from here
		String copyright = page.getCopyright(req, resp, page);
		if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
			// TODO: Dublin Core: https://stackoverflow.com/questions/6665312/is-the-copyright-meta-tag-valid-in-html5
			html.out.write("    "); html.meta().name("copyright").content(copyright).__().nl();
		}

		// Configure layout resources
		Registry requestRegistry = RegistryEE.Request.get(servletContext, req);
		configureResources(servletContext, req, resp, page, requestRegistry);
		// Configure page resources
		Registry pageRegistry = RegistryEE.Page.get(req);
		if(pageRegistry == null) throw new ServletException("page-scope registry not found.  WebPage.service(ServletRequest,ServletResponse) invoked?");
		page.configureResources(servletContext, req, resp, this, pageRegistry);
		// Render links
		html.out.write("    "); Renderer.get(servletContext).renderStyles(
			req,
			resp,
			html,
			"    ",
			true, // registeredActivations
			null, // No additional activations
			requestRegistry, // request-scope
			RegistryEE.Session.get(req.getSession(false)), // session-scope
			pageRegistry
		); html.nl()
		.script().src(req.getEncodedURLForPath("/global.js", null, false, resp)).__().nl();
		printJavaScriptIncludes(req, resp, html, page);
		// TODO: Canonical?
		html.out.write("  </head>\n"
		+ "  <body\n");
		int color = getBackgroundColor(req);
		if(color != -1) {
			html.out.write("    bgcolor=\"");
			ChainWriter.writeHtmlColor(color, html.out);
			html.out.write("\"\n");
		}
		color = getTextColor(req);
		if(color != -1) {
			html.out.write("    text=\"");
			ChainWriter.writeHtmlColor(color, html.out);
			html.out.write("\"\n");
		}
		color = getLinkColor(req);
		if(color != -1) {
			html.out.write("    link=\"");
			ChainWriter.writeHtmlColor(color, html.out);
			html.out.write("\"\n");
		}
		color = getVisitedLinkColor(req);
		if(color != -1) {
			html.out.write("    vlink=\"");
			ChainWriter.writeHtmlColor(color, html.out);
			html.out.write("\"\n");
		}
		color = getActiveLinkColor(req);
		if(color != -1) {
			html.out.write("    alink=\"");
			ChainWriter.writeHtmlColor(color, html.out);
			html.out.write("\"\n");
		}
		// TODO: These onloads should be merged?
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null) {
			html.out.write("    onload=\""); encodeJavaScriptInXhtmlAttribute(onload, html.out); html.out.write("\"\n");
		}
		html.out.write("  >\n"
		+ "    <table cellspacing=\"10\" cellpadding=\"0\">\n"
		+ "      <tr>\n"
		+ "        <td valign=\"top\">\n");
		printLogo(page, html, req, resp);
		boolean isLoggedIn=req.isLoggedIn();
		if(isLoggedIn) {
			html.out.write("          "); html.hr__().out.write("\n"
			+ "          Logout: <form style=\"display:inline\" id=\"logout_form\" method=\"post\" action=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), html.out);
			html.out.write("\"><div style=\"display:inline\">");
			req.printFormFields(html);
			html.input.hidden().name(WebSiteRequest.LOGOUT_REQUESTED).value(true).__()
			.input.submit__("Logout").out.write("</div></form>\n");
		} else {
			html.out.write("          "); html.hr__().out.write("\n"
			+ "          Login: <form style=\"display:inline\" id=\"login_form\" method=\"post\" action=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), html.out);
			html.out.write("\"><div style=\"display:inline\">");
			req.printFormFields(html);
			html.input.hidden().name(WebSiteRequest.LOGIN_REQUESTED).value(true).__()
			.input.submit__("Login").out.write("</div></form>\n");
		}
		html.out.write("          "); html.hr__().out.write("\n"
		+ "          <div style=\"white-space:nowrap\">\n");
		if(getLayoutChoices().length >= 2) html.text("Layout:").out.write(' ');
		if(printWebPageLayoutSelector(page, html, req, resp)) {
			html.br__().out.write("\n"
			+ "            "); html.text("Search:"); html.out.write(" <form id=\"search_site\" style=\"display:inline\" method=\"post\" action=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(page, resp), html.out);
			html.out.write("\"><div style=\"display:inline\">\n"
			+ "              "); html.input.hidden().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_ENTIRE_SITE).__().nl();
		}
		req.printFormFields(html);
		html.out.write("              "); html.input.text().name(WebSiteRequest.SEARCH_QUERY).size(12).maxlength(255).__().out.write("\n"
		+ "            </div></form>"); html.br__().out.write("\n"
		+ "          </div>\n"
		+ "          "); html.hr__().out.write("\n"
		+ "          <b>"); html.text("Current Location"); html.out.write("</b>"); html.br__().out.write("\n"
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
			html.out.write("            <a href=\"");
			encodeTextInXhtmlAttribute(req.getEncodedURL(parent, resp), html.out);
			html.out.write("\">"); html.text(navAlt);
			if(navSuffix != null) {
				html.out.write(" ("); html.text(navSuffix).out.write(')');
			}
			html.out.write("</a>"); html.br__().nl();
		}
		html.out.write("          </div>\n"
		+ "          "); html.hr__().out.write("\n"
		+ "          <b>"); html.text("Related Pages"); html.out.write("</b>"); html.br__().out.write("\n"
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
				html.out.write("          <a href=\"");
				encodeTextInXhtmlAttribute(tpage.getNavImageURL(req, resp, null), html.out);
				html.out.write("\">"); html.text(navAlt);
				if(navSuffix != null) {
					html.out.write(" ("); html.text(navSuffix).out.write(')');
				}
				html.out.write("</a>"); html.br__().nl();
			}
		}
		html.out.write("          </div>\n"
		+ "          "); html.hr__().nl();
		printBelowRelatedPages(html, req);
		html.out.write("        </td>\n"
		+ "        <td valign=\"top\">");
		WebPage[] commonPages = getCommonPages(page, req);
		if(commonPages != null && commonPages.length > 0) {
			html.out.write("        <table cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%\"><tr>\n");
			for(int c = 0; c < commonPages.length; c++) {
				if(c > 0) html.out.write("          <td style=\"text-align:center;width:1%\">|</td>\n");
				WebPage tpage = commonPages[c];
				html.out.append("          <td style=\"white-space:nowrap; text-align:center; width:").append(((101 - commonPages.length) / commonPages.length) + "%").append("\"><a href=\"");
				encodeTextInXhtmlAttribute(tpage.getNavImageURL(req, resp, null), html.out);
				html.out.write("\">"); html.text(tpage.getNavImageAlt(req)); html.out.write("</a></td>\n");
			}
			html.out.write("        </tr></table>\n");
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
		Html html
	) throws ServletException, IOException, SQLException {
		html.out.write("        </td>\n"
		+ "      </tr>\n"
		+ "    </table>\n"
		+ "  </body>\n");
		HtmlTag.endHtmlTag(html.out); html.nl();
	}

	/**
	 * Starts the content area of a page.
	 */
	@Override
	public void startContent(Html html, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException {
		html.out.write("<table cellpadding=\"0\" cellspacing=\"0\"");
		if(preferredWidth != -1) {
			html.out.append(" style=\"width:").append(Integer.toString(preferredWidth)).append("px\"");
		}
		html.out.write(">\n"
		+ "  <tr>\n");
		int totalColumns = 0;
		for(int c = 0; c < contentColumnSpans.length; c++) {
			if(c > 0) totalColumns++;
			totalColumns += contentColumnSpans[c];
		}
		html.out.write("    <td");
		if(totalColumns != 1) {
			html.out.append(" colspan=\"").append(Integer.toString(totalColumns)).append('"');
		}
		html.out.write('>'); html.hr__().out.write("</td>\n"
		+ "  </tr>\n");
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	@Override
	public void printContentHorizontalDivider(Html html, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException {
		html.out.write("  <tr>\n");
		for(int c = 0; c < colspansAndDirections.length; c += 2) {
			int direction = (c == 0) ? -1 : colspansAndDirections[c - 1];
			if(direction != -1) {
				switch(direction) {
					case UP:
						html.out.write("    <td>&#160;</td>\n");
						break;
					case DOWN:
						html.out.write("    <td>&#160;</td>\n");
						break;
					case UP_AND_DOWN:
						html.out.write("    <td>&#160;</td>\n");
						break;
					default: throw new IllegalArgumentException("Unknown direction: " + direction);
				}
			}

			int colspan=colspansAndDirections[c];
			html.out.write("    <td");
			if(colspan != 1) html.out.append(" colspan=\"").append(Integer.toString(colspan)).append('"');
			html.out.write('>'); html.hr__().out.write("</td>\n");
		}
		html.out.write("  </tr>\n");
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	@Override
	public void printContentTitle(Html html, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException {
		startContentLine(html, req, resp, contentColumns, "center", null);
		html.out.write("<h1>"); html.text(title).out.write("</h1>\n");
		endContentLine(html, req, resp, 1, false);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void startContentLine(Html html, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		html.out.write("  <tr>\n"
		+ "    <td");
		if(align != null || width != null) {
			html.out.write(" style=\"");
			if(align != null) {
				html.out.write("text-align:");
				encodeTextInXhtmlAttribute(align, html.out);
			}
			if(width != null) {
				if(align != null) html.out.write(';');
				appendWidthStyle(width, html.out);
			}
			html.out.write('"');
		}
		html.out.write(" valign=\"top\"");
		if(colspan != 1) {
			html.out.append(" colspan=\"").append(Integer.toString(colspan)).append('"');
		}
		html.out.write('>');
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	public void printContentVerticalDivider(Html html, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws IOException {
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		html.out.write("    </td>\n");
		switch(direction) {
			case UP_AND_DOWN:
				html.out.write("    <td>&#160;</td>\n");
				break;
			case NONE:
				break;
			default: throw new IllegalArgumentException("Unknown direction: " + direction);
		}
		html.out.write("    <td");
		if(align != null || width != null) {
			html.out.write(" style=\"");
			if(align != null) {
				html.out.write("text-align:");
				encodeTextInXhtmlAttribute(align, html.out);
			}
			if(width != null) {
				if(align != null) html.out.write(';');
				appendWidthStyle(width, html.out);
			}
			html.out.write('"');
		}
		html.out.write(" valign=\"top\"");
		if(colspan != 1) html.out.append(" colspan=\"").append(Integer.toString(colspan)).append('"');
		if(rowspan != 1) html.out.append(" rowspan=\"").append(Integer.toString(rowspan)).append('"');
		html.out.write('>');
	}

	/**
	 * Ends one line of content.
	 */
	@Override
	public void endContentLine(Html html, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws IOException {
		html.out.write("    </td>\n"
		+ "  </tr>\n");
	}

	/**
	 * Ends the content area of a page.
	 */
	@Override
	public void endContent(WebPage page, Html html, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws IOException, SQLException {
		int totalColumns=0;
		for(int c = 0; c < contentColumnSpans.length; c++) {
			if(c > 0) totalColumns += 1;
			totalColumns += contentColumnSpans[c];
		}
		html.out.write("  <tr><td");
		if(totalColumns != 1) html.out.append(" colspan=\"").append(Integer.toString(totalColumns)).append('"');
		html.out.write('>'); html.hr__().out.write("</td></tr>\n");
		String copyright = page.getCopyright(req, resp, page);
		if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
			html.out.write("  <tr><td");
			if(totalColumns != 1) html.out.append(" colspan=\"").append(Integer.toString(totalColumns)).append('"');
			html.out.write(" style=\"text-align:center\"><span style=\"font-size:x-small\">"); html.text(copyright); html.out.write("</span></td></tr>\n");
		}
		html.out.write("</table>\n");
	}

	@Override
	public String getName() {
		return NAME;
	}

	public WebPage[] getCommonPages(WebPage page, WebSiteRequest req) throws IOException, SQLException {
		return null;
	}

	public void printLogo(WebPage page, Html html, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
	}

	/**
	 * Prints content below the related pages area on the left.
	 */
	public void printBelowRelatedPages(Html html, WebSiteRequest req) throws IOException, SQLException {
	}
}
