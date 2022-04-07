/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2003-2013, 2015, 2016, 2019, 2020, 2021, 2022  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-framework.
 *
 * ao-web-framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-framework.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.aoapps.web.framework;

import com.aoapps.encoding.ChainWriter;
import com.aoapps.encoding.Doctype;
import com.aoapps.html.any.AnyLINK;
import com.aoapps.html.any.AnyMETA;
import com.aoapps.html.any.AnySCRIPT;
import com.aoapps.html.any.AnySTYLE;
import com.aoapps.html.any.attributes.Enum.Method;
import com.aoapps.html.servlet.BODY;
import com.aoapps.html.servlet.BODY_c;
import com.aoapps.html.servlet.ContentEE;
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.HEAD__;
import com.aoapps.html.servlet.HTML_c;
import com.aoapps.html.servlet.TABLE_c;
import com.aoapps.html.servlet.TBODY_c;
import com.aoapps.html.servlet.TD_c;
import com.aoapps.html.servlet.TR_c;
import com.aoapps.html.util.GoogleAnalytics;
import static com.aoapps.lang.Strings.trimNullIfEmpty;
import com.aoapps.net.URIEncoder;
import static com.aoapps.taglib.AttributeUtils.getWidthStyle;
import com.aoapps.web.resources.registry.Group;
import com.aoapps.web.resources.registry.Registry;
import com.aoapps.web.resources.registry.Style;
import com.aoapps.web.resources.renderer.Renderer;
import com.aoapps.web.resources.servlet.RegistryEE;
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
// TODO: Move into own microproject (also split into ao-web-framework-html)?
//       This would start to decompose this in a way like we're doing in SemanticCMS 2.
//       Probably not worth it for this legacy system?  Could they converge?
public class TextOnlyLayout extends WebPageLayout {

	// Matches TextSkin.NAME
	public static final String NAME = "Text";

	/**
	 * The name of the {@linkplain com.aoapps.web.resources.servlet.RegistryEE.Application application-scope}
	 * group that will be used for text layout web resources.
	 */
	public static final Group.Name RESOURCE_GROUP = new Group.Name(TextOnlyLayout.class.getName());

	public static final Style LAYOUT_TEXT_CSS = new Style("/layout/text/layout-text.css");

	@WebListener
	public static class Initializer implements ServletContextListener {
		@Override
		public void contextInitialized(ServletContextEvent event) {
			RegistryEE.Application.get(event.getServletContext())
				.getGroup(RESOURCE_GROUP)
				.styles
				.add(LAYOUT_TEXT_CSS);
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
	public void configureResources(
		ServletContext servletContext,
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		Registry requestRegistry
	) {
		super.configureResources(servletContext, req, resp, page, requestRegistry);
		requestRegistry.activate(RESOURCE_GROUP);
	}

	public static void writeBodyColorStyle(WebPageLayout layout, WebSiteRequest req, HEAD__<?> head) throws IOException {
		int backgroundColor = layout.getBackgroundColor(req);
		int textColor = layout.getTextColor(req);
		int linkColor = layout.getLinkColor(req);
		int visitedLinkColor = layout.getVisitedLinkColor(req);
		int activeLinkColor = layout.getActiveLinkColor(req);
		if(
			backgroundColor != -1
			|| textColor != -1
			|| linkColor != -1
			|| visitedLinkColor != -1
			|| activeLinkColor != -1
		) {
			try (var style = head.style()._c()) {
				if(backgroundColor != -1 || textColor != -1) {
					style.append("body {").nl();
					if(backgroundColor != -1) {
						style.indent().append("background-color:");
						ChainWriter.writeHtmlColor(backgroundColor, style);
						style.append(';').nl();
					}
					if(textColor != -1) {
						style.indent().append("color:");
						ChainWriter.writeHtmlColor(textColor, style);
						style.append(';').nl();
					}
					style.append('}').nl();
				}
				if(linkColor != -1) {
					style.append("a {").nl();
					style.indent().append("color:");
					ChainWriter.writeHtmlColor(linkColor, style);
					style.append(';').nl();
					style.append('}').nl();
				}
				if(visitedLinkColor != -1) {
					style.append("a:visited {").nl();
					style.indent().append("color:");
					ChainWriter.writeHtmlColor(visitedLinkColor, style);
					style.append(';').nl();
					style.append('}').nl();
				}
				if(activeLinkColor != -1) {
					style.append("a:active {").nl();
					style.indent().append("color:");
					ChainWriter.writeHtmlColor(activeLinkColor, style);
					style.append(';').nl();
					style.append('}').nl();
				}
			}
		}
	}

	@Override
	// TODO: uncomment once only expected deprecated remains: @SuppressWarnings("deprecation")
	public <__ extends FlowContent<__>> __ startPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		DocumentEE document,
		String onload
	) throws ServletException, IOException {
		boolean isOkResponseStatus = (resp.getStatus() == HttpServletResponse.SC_OK);
		ServletContext servletContext = req.getServletContext();
		String trackingId = getGoogleAnalyticsNewTrackingCode(servletContext);
		// Write doctype
		document.xmlDeclaration();
		document.doctype();
		// Write <html>
		HTML_c<DocumentEE> html_c = document.html().lang()._c();
		html_c.head__(head -> {
			// If this is not the default layout, then robots noindex
			if(!isOkResponseStatus || !getName().equals(getLayoutChoices()[0])) {
				head.meta().name(AnyMETA.Name.ROBOTS).content("noindex, nofollow").__();
			}
			Doctype doctype = document.encodingContext.getDoctype();
			if(doctype == Doctype.HTML5) {
				head.meta().charset().__();
			} else {
				head
					.meta().httpEquiv(AnyMETA.HttpEquiv.CONTENT_TYPE).content(resp.getContentType()).__()
					// Default style language
					.meta().httpEquiv(AnyMETA.HttpEquiv.CONTENT_STYLE_TYPE).content(AnySTYLE.Type.TEXT_CSS).__()
					.meta().httpEquiv(AnyMETA.HttpEquiv.CONTENT_SCRIPT_TYPE).content(AnySCRIPT.Type.TEXT_JAVASCRIPT).__();
			}
			if(doctype == Doctype.HTML5) {
				GoogleAnalytics.writeGlobalSiteTag(head, trackingId);
			} else {
				GoogleAnalytics.writeAnalyticsJs(head, trackingId);
			}
			// Mobile support
			head
				.meta().name(AnyMETA.Name.VIEWPORT).content("width=device-width, initial-scale=1.0").__()
				// TODO: This is probably only appropriate for single-page applications!
				//       See https://medium.com/@firt/dont-use-ios-web-app-meta-tag-irresponsibly-in-your-progressive-web-apps-85d70f4438cb
				.meta().name(AnyMETA.Name.APPLE_MOBILE_WEB_APP_CAPABLE).content("yes").__()
				.meta().name(AnyMETA.Name.APPLE_MOBILE_WEB_APP_STATUS_BAR_STYLE).content("black").__();
			// Authors
			// TODO: 3.0.0: dcterms copyright
			String author = page.getAuthor(req);
			if(author != null && !(author = author.trim()).isEmpty()) {
				head.meta().name(AnyMETA.Name.AUTHOR).content(author).__();
			}
			String authorHref = page.getAuthorHref(req, resp);
			if(authorHref != null && !(authorHref = authorHref.trim()).isEmpty()) {
				head.link(AnyLINK.Rel.AUTHOR).href(
					// TODO: RFC 3986-only always?
					resp.encodeURL(
						URIEncoder.encodeURI(authorHref) // TODO: Conditionally convert from context-relative paths
					)
				).__();
			}
			head.title__(page.getTitle(req));
			String description = page.getDescription(req);
			if(description != null && !(description = description.trim()).isEmpty()) {
				head.meta().name(AnyMETA.Name.DESCRIPTION).content(description).__();
			}
			String keywords = page.getKeywords(req);
			if(keywords != null && !(keywords = keywords.trim()).isEmpty()) {
				head.meta().name(AnyMETA.Name.KEYWORDS).content(keywords).__();
			}
			// TODO: 3.0.0: Review HTML 4/HTML 5 differences from here
			String copyright = page.getCopyright(req, resp, page);
			if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
				// TODO: 3.0.0: Dublin Core: https://stackoverflow.com/questions/6665312/is-the-copyright-meta-tag-valid-in-html5
				head.meta().name("copyright").content(copyright).__();
			}

			// Configure layout resources
			Registry requestRegistry = RegistryEE.Request.get(servletContext, req);
			configureResources(servletContext, req, resp, page, requestRegistry);
			// Configure page resources
			Registry pageRegistry = RegistryEE.Page.get(req);
			if(pageRegistry == null) {
				throw new ServletException("page-scope registry not found.  WebPage.service(ServletRequest,ServletResponse) invoked?");
			}
			page.configureResources(servletContext, req, resp, this, pageRegistry);
			// Render links
			Renderer.get(servletContext).renderStyles(
				req,
				resp,
				head,
				true, // registeredActivations
				null, // No additional activations
				requestRegistry, // request-scope
				RegistryEE.Session.get(req.getSession(false)), // session-scope
				pageRegistry
			);
			head.script().src(req.getEncodedURLForPath("/global.js", null, false, resp)).__();
			printJavascriptIncludes(req, resp, page, head);
			writeBodyColorStyle(this, req, head);
			// TODO: Canonical?
		});
		BODY<HTML_c<DocumentEE>> body = html_c.body();
		// TODO: These onloads should be merged?
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null && !(onload = onload.trim()).isEmpty()) {
			body.onload(onload);
		}
		BODY_c<HTML_c<DocumentEE>> body_c = body._c();
		TD_c<TR_c<TBODY_c<TABLE_c<BODY_c<HTML_c<DocumentEE>>>>>> td_c = body_c.table().cellspacing(10).cellpadding(0)._c()
			.tbody_c()
				.tr_c()
					.td().style("vertical-align:top").__(td -> {
						printLogo(req, resp, page, td);
						boolean isLoggedIn = req.isLoggedIn();
						if(isLoggedIn) {
							td.hr__()
							.text("Logout: ")
							.form()
								.style("display:inline")
								.id("logout_form")
								.method(Method.Value.POST)
								.action(req.getEncodedURL(page, resp))
							.__(form -> form
								.div().style("display:inline").__(div -> {
									req.printFormFields(div);
									div.input().hidden().name(WebSiteRequest.LOGOUT_REQUESTED).value(true).__()
									.input().submit__("Logout");
								})
							);
						} else {
							td.hr__()
							.text("Login: ")
							.form()
								.style("display:inline")
								.id("login_form")
								.method(Method.Value.POST)
								.action(req.getEncodedURL(page, resp))
							.__(form -> form
								.div().style("display:inline").__(div -> {
									req.printFormFields(div);
									div.input().hidden().name(WebSiteRequest.LOGIN_REQUESTED).value(true).__()
									.input().submit__("Login");
								})
							);
						}
						td.hr__()
						.div().style("white-space:nowrap").__(div -> {
							if(getLayoutChoices().length >= 2) div.text("Layout: ");
							if(printWebPageLayoutSelector(req, resp, page, div)) div.br__();
							div.text("Search: ").form().id("search_site").style("display:inline").method(Method.Value.POST).action(req.getEncodedURL(page, resp)).__(form -> form
								.div().style("display:inline").__(div2 -> {
									div2.input().hidden().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_ENTIRE_SITE).__().autoNl();
									req.printFormFields(div2);
									div2.input().text().name(WebSiteRequest.SEARCH_QUERY).size(12).maxlength(255).__();
								})
								.br__()
							);
						})
						.hr__()
						// Display the parents
						.b__("Current Location").br__()
						.div().style("white-space:nowrap").__(div -> {
							List<WebPage> parents = new ArrayList<>();
							WebPage parent = page;
							while(parent != null) {
								if(parent.showInLocationPath(req)) parents.add(parent);
								parent = parent.getParent();
							}
							for(int c = (parents.size() - 1); c >= 0; c--) {
								parent = parents.get(c);
								String navAlt = parent.getNavImageAlt(req);
								String navSuffix = parent.getNavImageSuffix(req);
								div.a(req.getEncodedURL(parent, resp)).__(a -> {
									a.text(navAlt);
									if(navSuffix != null) {
										a.text(" (").text(navSuffix).text(')');
									}
								}).br__();
							}
						})
						.hr__()
						// Related Pages
						.b__("Related Pages").br__()
						.div().style("white-space:nowrap").__(div -> {
							WebPage[] related = page.getCachedChildren(req, resp);
							WebPage parent = page;
							if(related.length == 0) {
								parent = page.getParent();
								if(parent != null) related = parent.getCachedChildren(req, resp);
							}
							for(int c = -1; c < related.length; c++) {
								WebPage tpage;
								if (c == -1) {
									if (parent!=null && parent.includeNavImageAsParent()) tpage = parent;
									else tpage = null;
								} else {
									tpage = related[c];
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
									div.a(tpage.getNavImageURL(req, resp, null)).__(a -> {
										a.text(navAlt);
										if(navSuffix != null) {
											a.text(" (").text(navSuffix).text(')');
										}
									}).br__();
								}
							}
						})
						.hr__();
						printBelowRelatedPages(td, req);
					})
					.td().style("vertical-align:top")._c();
						WebPage[] commonPages = getCommonPages(page, req);
						if(commonPages != null && commonPages.length > 0) {
							td_c.table().clazz("ao-packed").style("width:100%").__(table -> table
								.tbody__(tbody -> tbody
									.tr__(tr -> {
										for(int c = 0; c < commonPages.length; c++) {
											if(c > 0) tr.td().style("text-align:center", "width:1%").__('|');
											WebPage tpage = commonPages[c];
											tr.td().style("white-space:nowrap", "text-align:center", "width:" + ((101 - commonPages.length) / commonPages.length) + "%").__(td -> td
												.a(tpage.getNavImageURL(req, resp, null)).__(tpage.getNavImageAlt(req))
											);
										}
									})
								)
							);
						}
		@SuppressWarnings("unchecked") __ flow = (__)td_c;
		return flow;
	}

	/**
	 * Gets the Google Analytics New Tracking Code (ga.js) or <code>null</code>
	 * if unavailable.
	 */
	public String getGoogleAnalyticsNewTrackingCode(ServletContext servletContext) {
		return null;
	}

	@Override
	public void endPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		FlowContent<?> flow
	) throws ServletException, IOException {
		@SuppressWarnings("unchecked")
		TD_c<TR_c<TBODY_c<TABLE_c<BODY_c<HTML_c<DocumentEE>>>>>> td_c = (TD_c)flow;
		DocumentEE document = td_c
							.__()
						.__()
					.__()
				.__()
			.__()
		.__();
		assert document != null : "Is fully closed back to DocumentEE";
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>
	> __ startContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int[] contentColumnSpans,
		String width
	) throws ServletException, IOException {
		if(width == null) width = page.getPreferredContentWidth(req);
		width = trimNullIfEmpty(width);
		final int totalColumns;
		{
			int totalColumns_ = 0;
			for(int c = 0; c < contentColumnSpans.length; c++) {
				if(c > 0) totalColumns_++;
				totalColumns_ += contentColumnSpans[c];
			}
			totalColumns = totalColumns_;
		}
		TBODY_c<TABLE_c<PC>> tbody = pc.table()
			.clazz("ao-packed")
			.style(getWidthStyle(width))
		._c()
			.thead__(thead -> thead
				.tr__(tr -> tr
					.td().colspan(totalColumns).__(td -> td
						.hr__()
					)
				)
			)
			.tbody_c();
		@SuppressWarnings("unchecked")
		__ content = (__)tbody;
		return content;
	}

	@Override
	public void contentTitle(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		String title,
		int contentColumns
	) throws ServletException, IOException {
		FlowContent<?> contentLine = startContentLine(req, resp, content, contentColumns, "center", null); {
			contentLine.h1__(title);
		} endContentLine(req, resp, contentLine);
	}

	@Override
	public <__ extends FlowContent<__>> __ startContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int colspan,
		String align,
		String width
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TBODY_c<TABLE_c<DocumentEE>> tbody = (TBODY_c)content;
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		TD_c<TR_c<TBODY_c<TABLE_c<DocumentEE>>>> td = tbody
			.tr_c()
				.td()
					.style(
						align == null ? null : "text-align:" + align,
						getWidthStyle(width),
						"vertical-align:top"
					)
					.colspan(colspan)
				._c();
		@SuppressWarnings("unchecked")
		__ contentLine = (__)td;
		return contentLine;
	}

	@Override
	public <__ extends FlowContent<__>> __ contentVerticalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine,
		int direction,
		int colspan,
		int rowspan,
		String align,
		String width
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TD_c<TR_c<TBODY_c<TABLE_c<DocumentEE>>>> td = (TD_c)contentLine;
		align = trimNullIfEmpty(align);
		width = trimNullIfEmpty(width);
		TR_c<TBODY_c<TABLE_c<DocumentEE>>> tr = td.__();
		switch(direction) {
			case UP_AND_DOWN:
				tr.td__('\u00A0');
				break;
			case NONE:
				break;
			default: throw new IllegalArgumentException("Unknown direction: " + direction);
		}
		TD_c<TR_c<TBODY_c<TABLE_c<DocumentEE>>>> newTd = tr.td()
			.style(
				align == null ? null : "text-align:" + align,
				getWidthStyle(width),
				"vertical-align:top"
			)
			.colspan(colspan)
			.rowspan(rowspan)
		._c();
		@SuppressWarnings("unchecked")
		__ newContentLine = (__)newTd;
		return newContentLine;
	}

	@Override
	public void endContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine,
		int rowspan,
		boolean endsInternal
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TD_c<TR_c<TBODY_c<TABLE_c<DocumentEE>>>> td = (TD_c)contentLine;
		TABLE_c<DocumentEE> table = td
				.__()
			.__()
		.__();
		assert table != null : "Is fully closed back to TABLE_c";
	}

	@Override
	public void contentHorizontalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int[] colspansAndDirections,
		boolean endsInternal
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TBODY_c<TABLE_c<DocumentEE>> tbody = (TBODY_c)content;
		tbody.tr__(tr -> {
			for(int c = 0; c < colspansAndDirections.length; c += 2) {
				if(c > 0) {
					int direction = colspansAndDirections[c - 1];
					switch(direction) {
						case UP:
							tr.td__('\u00A0');
							break;
						case DOWN:
							tr.td__('\u00A0');
							break;
						case UP_AND_DOWN:
							tr.td__('\u00A0');
							break;
						default: throw new IllegalArgumentException("Unknown direction: " + direction);
					}
				}
				int colspan = colspansAndDirections[c];
				tr.td()
					.colspan(colspan)
				.__(td -> td
					.hr__()
				);
			}
		});
	}

	@Override
	public void endContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content,
		int[] contentColumnSpans
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TBODY_c<TABLE_c<DocumentEE>> tbody = (TBODY_c)content;
		final int totalColumns;
		{
			int totalColumns_ = 0;
			for(int c = 0; c < contentColumnSpans.length; c++) {
				if(c > 0) totalColumns_ += 1;
				totalColumns_ += contentColumnSpans[c];
			}
			totalColumns = totalColumns_;
		}
		tbody.tr__(tr -> tr
			.td()
				.colspan(totalColumns)
			.__(td -> td
				.hr__()
			)
		);
		TABLE_c<DocumentEE> table = tbody.__();
		String copyright = page.getCopyright(req, resp, page);
		if(copyright != null) copyright = copyright.trim();
		if(copyright != null && !copyright.isEmpty()) {
			String copyright_ = copyright;
			table.tfoot__(tfoot -> tfoot
				.tr__(tr -> tr
					.td()
						.colspan(totalColumns)
						.style("text-align:center", "font-size:x-small")
					.__(copyright_)
				)
			);
		}
		table.__();
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ startLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException {
		align = trimNullIfEmpty(align);
		TD_c<TR_c<TBODY_c<TABLE_c<PC>>>> td = pc.table()
			.clazz("ao-packed")
			.style("border:5px outset #a0a0a0", getWidthStyle(width))
		._c()
			.tbody_c()
				.tr_c()
					.td()
						.clazz("aoLightRow")
						.style(
							"padding:4px",
							(align != null) ? ("text-align:" + align) : null,
							nowrap ? "white-space:nowrap" : null
						)
					._c();
		@SuppressWarnings("unchecked")
		__ lightArea = (__)td;
		return lightArea;
	}

	@Override
	public void endLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> lightArea
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TD_c<TR_c<TBODY_c<TABLE_c<DocumentEE>>>> td = (TD_c)lightArea;
		TABLE_c<DocumentEE> table = td
				.__()
			.__()
		.__();
		assert table != null : "Is fully closed back to TABLE_c";
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ startWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException {
		align = trimNullIfEmpty(align);
		TD_c<TR_c<TBODY_c<TABLE_c<PC>>>> td = pc.table()
			.clazz("ao-packed")
			.style("border:5px outset #a0a0a0", getWidthStyle(width))
		._c()
			.tbody_c()
				.tr_c()
					.td()
						.clazz("aoWhiteRow")
						.style(
							"padding:4px",
							(align != null) ? ("text-align:" + align) : null,
							nowrap ? "white-space:nowrap" : null
						)
					._c();
		@SuppressWarnings("unchecked")
		__ whiteArea = (__)td;
		return whiteArea;
	}

	@Override
	public void endWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> whiteArea
	) throws ServletException, IOException {
		// Lying about "DocumentEE" here, but it makes the compiler happy and is otherwise irrelevant
		@SuppressWarnings("unchecked")
		TD_c<TR_c<TBODY_c<TABLE_c<DocumentEE>>>> td = (TD_c)whiteArea;
		TABLE_c<DocumentEE> table = td
				.__()
			.__()
		.__();
		assert table != null : "Is fully closed back to TABLE_c";
	}

	@Override
	public String getName() {
		return NAME;
	}

	public WebPage[] getCommonPages(WebPage page, WebSiteRequest req) throws ServletException {
		return null;
	}

	public <__ extends FlowContent<__>> void printLogo(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		__ td
	) throws ServletException, IOException {
		// Do nothing
	}

	/**
	 * Prints content below the related pages area on the left.
	 */
	public <__ extends FlowContent<__>> void printBelowRelatedPages(__ td, WebSiteRequest req) throws ServletException, IOException {
		// Do nothing
	}
}
