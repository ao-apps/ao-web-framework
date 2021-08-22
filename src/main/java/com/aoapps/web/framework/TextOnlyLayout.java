/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2003-2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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
 * along with ao-web-framework.  If not, see <http://www.gnu.org/licenses/>.
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
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.HTML_c;
import com.aoapps.html.servlet.TABLE;
import com.aoapps.html.servlet.TABLE_c;
import com.aoapps.html.servlet.TD;
import com.aoapps.html.servlet.TD_c;
import com.aoapps.html.servlet.TR_c;
import com.aoapps.html.util.GoogleAnalytics;
import static com.aoapps.lang.Strings.trimNullIfEmpty;
import static com.aoapps.taglib.AttributeUtils.appendWidthStyle;
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
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException {
		String align_ = trimNullIfEmpty(align);
		TD_c<TR_c<TABLE_c<PC>>> td = pc.table()
			.style(
				"border:5px outset #a0a0a0",
				(align_ != null) ? ("text-align:" + align_) : null,
				getWidthStyle(width)
			)
			.cellpadding(0)
			.cellspacing(0)
		._c()
			.tr_c()
				.td().clazz("aoLightRow").style("padding:4px", nowrap ? "white-space:nowrap" : null)._c();
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
		@SuppressWarnings("unchecked")
		TD_c<? extends TR_c<? extends TABLE_c<?>>> td = (TD_c)lightArea;
					td
				.__()
			.__()
		.__();
	}

	@Override
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException {
		String align_ = trimNullIfEmpty(align);
		TD_c<TR_c<TABLE_c<PC>>> td = pc.table()
			.style(
				"border:5px outset #a0a0a0",
				(align_ != null) ? ("text-align:" + align_) : null,
				getWidthStyle(width)
			)
			.cellpadding(0)
			.cellspacing(0)
		._c()
			.tr_c()
				.td().clazz("aoWhiteRow").style("background-color:white", "padding:4px", nowrap ? "white-space:nowrap" : null)._c();
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
		@SuppressWarnings("unchecked")
		TD_c<? extends TR_c<? extends TABLE_c<?>>> td = (TD_c)whiteArea;
					td
				.__()
			.__()
		.__();
	}

	@Override
	// TODO: uncomment once only expected deprecated remains: @SuppressWarnings("deprecation")
	public <__ extends FlowContent<__>> __ startPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
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
				head.meta(AnyMETA.Name.ROBOTS).content("noindex, nofollow").__();
			}
			if(document.doctype == Doctype.HTML5) {
				head.meta().charset(resp.getCharacterEncoding()).__();
			} else {
				head
					.meta(AnyMETA.HttpEquiv.CONTENT_TYPE).content(resp.getContentType()).__()
					// Default style language
					.meta(AnyMETA.HttpEquiv.CONTENT_STYLE_TYPE).content(AnySTYLE.Type.TEXT_CSS).__()
					.meta(AnyMETA.HttpEquiv.CONTENT_SCRIPT_TYPE).content(AnySCRIPT.Type.TEXT_JAVASCRIPT).__();
			}
			if(document.doctype == Doctype.HTML5) {
				GoogleAnalytics.writeGlobalSiteTag(head, trackingId);
			} else {
				GoogleAnalytics.writeAnalyticsJs(head, trackingId);
			}
			// Mobile support
			head
				.meta(AnyMETA.Name.VIEWPORT).content("width=device-width, initial-scale=1.0").__()
				// TODO: This is probably only appropriate for single-page applications!
				//       See https://medium.com/@firt/dont-use-ios-web-app-meta-tag-irresponsibly-in-your-progressive-web-apps-85d70f4438cb
				.meta(AnyMETA.Name.APPLE_MOBILE_WEB_APP_CAPABLE).content("yes").__()
				.meta(AnyMETA.Name.APPLE_MOBILE_WEB_APP_STATUS_BAR_STYLE).content("black").__();
			// Authors
			// TODO: dcterms copyright
			String author = page.getAuthor();
			if(author != null && !(author = author.trim()).isEmpty()) {
				head.meta(AnyMETA.Name.AUTHOR).content(author).__();
			}
			String authorHref = page.getAuthorHref(req, resp);
			if(authorHref != null && !(authorHref = authorHref.trim()).isEmpty()) {
				head.link(AnyLINK.Rel.AUTHOR).href(authorHref).__();
			}
			head.title__(
				// No more page stack, just show current page only
				page.getTitle()
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
			);
			String description = page.getDescription();
			if(description != null && !(description = description.trim()).isEmpty()) {
				head.meta(AnyMETA.Name.DESCRIPTION).content(description).__();
			}
			String keywords = page.getKeywords();
			if(keywords != null && !(keywords = keywords.trim()).isEmpty()) {
				head.meta(AnyMETA.Name.KEYWORDS).content(keywords).__();
			}
			// TODO: Review HTML 4/HTML 5 differences from here
			String copyright = page.getCopyright(req, resp, page);
			if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
				// TODO: Dublin Core: https://stackoverflow.com/questions/6665312/is-the-copyright-meta-tag-valid-in-html5
				head.meta().name("copyright").content(copyright).__();
			}

			// Configure layout resources
			Registry requestRegistry = RegistryEE.Request.get(servletContext, req);
			configureResources(servletContext, req, resp, page, requestRegistry);
			// Configure page resources
			Registry pageRegistry = RegistryEE.Page.get(req);
			if(pageRegistry == null) throw new ServletException("page-scope registry not found.  WebPage.service(ServletRequest,ServletResponse) invoked?");
			page.configureResources(servletContext, req, resp, this, pageRegistry);
			// Render links
			Renderer.get(servletContext).renderStyles(
				req,
				resp,
				head, // TODO: MetadataContent
				true, // registeredActivations
				null, // No additional activations
				requestRegistry, // request-scope
				RegistryEE.Session.get(req.getSession(false)), // session-scope
				pageRegistry
			);
			head.script().src(req.getEncodedURLForPath("/global.js", null, false, resp)).__();
			printJavaScriptIncludes(req, resp, head, page);
			// TODO: Canonical?
		});
		BODY<HTML_c<DocumentEE>> body = html_c.body().nl();
		// TODO: Write a <style>, like done for dans-home.com
		int bgcolor = getBackgroundColor(req);
		if(bgcolor != -1) {
			body.attribute("bgcolor", attr -> ChainWriter.writeHtmlColor(bgcolor, attr)).nl();
		}
		int text = getTextColor(req);
		if(text != -1) {
			body.attribute("text", attr -> ChainWriter.writeHtmlColor(text, attr)).nl();
		}
		int link = getLinkColor(req);
		if(link != -1) {
			body.attribute("link", attr -> ChainWriter.writeHtmlColor(link, attr)).nl();
		}
		int vlink = getVisitedLinkColor(req);
		if(vlink != -1) {
			body.attribute("vlink", attr -> ChainWriter.writeHtmlColor(vlink, attr)).nl();
		}
		int alink = getActiveLinkColor(req);
		if(alink != -1) {
			body.attribute("alink", attr -> ChainWriter.writeHtmlColor(alink, attr)).nl();
		}
		// TODO: These onloads should be merged?
		if (onload == null) onload = page.getOnloadScript(req);
		if (onload != null && !onload.isEmpty()) {
			body.onload(onload).nl();
		}
		BODY_c<HTML_c<DocumentEE>> body_c = body._c();
		TD_c<TR_c<TABLE_c<BODY_c<HTML_c<DocumentEE>>>>> td_c = body_c.table().cellspacing(10).cellpadding(0)._c()
			.tr_c()
				.td().attribute("valign", "top").__(td -> {
					printLogo(page, td, req, resp);
					boolean isLoggedIn = req.isLoggedIn();
					if(isLoggedIn) {
						td.hr__()
						.text("Logout: ").form().style("display:inline").id("logout_form").method(Method.Value.POST).action(req.getEncodedURL(page, resp)).__(form -> form
							.div().style("display:inline").__(div -> {
								req.printFormFields(div);
								div.input().hidden().name(WebSiteRequest.LOGOUT_REQUESTED).value(true).__()
								.input().submit__("Logout");
							})
						);
					} else {
						td.hr__()
						.text("Login: ").form().style("display:inline").id("login_form").method(Method.Value.POST).action(req.getEncodedURL(page, resp)).__(form -> form
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
						if(printWebPageLayoutSelector(page, div, req, resp)) div.br__();
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
					.b__("Current Location").br__()
					.div().style("white-space:nowrap").__(div -> {
						List<WebPage> parents=new ArrayList<>();
						//parents.clear();
						WebPage parent = page;
						while(parent != null) {
							if(parent.showInLocationPath(req)) parents.add(parent);
							parent = parent.getParent();
						}
						for(int c=(parents.size()-1);c>=0;c--) {
							parent=parents.get(c);
							String navAlt = parent.getNavImageAlt(req);
							String navSuffix=parent.getNavImageSuffix(req);
							div.a(req.getEncodedURL(parent, resp)).__(a -> {
								a.text(navAlt);
								if(navSuffix != null) {
									a.text(" (").text(navSuffix).text(')');
								}
							}).br__();
						}
					})
					.hr__()
					.b__("Related Pages").br__()
					.div().style("white-space:nowrap").__(div -> {
						WebPage[] children = page.getCachedChildren(req, resp);
						WebPage parent = page;
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
				.td().attribute("valign", "top")._c();
					WebPage[] commonPages = getCommonPages(page, req);
					if(commonPages != null && commonPages.length > 0) {
						td_c.table().cellspacing(0).cellpadding(0).style("width:100%").__(table -> table
							.tr__(tr -> {
								for(int c = 0; c < commonPages.length; c++) {
									if(c > 0) tr.td().style("text-align:center", "width:1%").__("|");
									WebPage tpage = commonPages[c];
									tr.td().style("white-space:nowrap", "text-align:center", "width:" + ((101 - commonPages.length) / commonPages.length) + "%").__(td -> td
										.a(tpage.getNavImageURL(req, resp, null)).__(tpage.getNavImageAlt(req))
									);
								}
							})
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
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> flow
	) throws ServletException, IOException {
		@SuppressWarnings("unchecked")
		TD_c<TR_c<TABLE_c<BODY_c<HTML_c<DocumentEE>>>>> td_c = (TD_c<TR_c<TABLE_c<BODY_c<HTML_c<DocumentEE>>>>>)flow;
		DocumentEE document = td_c
						.__()
					.__()
				.__()
			.__()
		.__();
		assert document != null : "Is fully closed back to DocumentEE";
	}

	/**
	 * Starts the content area of a page.
	 */
	@Override
	// TODO: Return value to be passed on to other methods
	public void startContent(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException {
		TABLE<DocumentEE> table = document.table().cellpadding(0).cellspacing(0);
		if(preferredWidth != -1) {
			table.style(style -> style.append("width:").append(Integer.toString(preferredWidth)).append("px"));
		}
		/* TODO: return */ table._c()
			// TODO: tbody
			.tr__(tr -> {
				int totalColumns = 0;
				for(int c = 0; c < contentColumnSpans.length; c++) {
					if(c > 0) totalColumns++;
					totalColumns += contentColumnSpans[c];
				}
				tr.td().colspan(totalColumns).__(td -> td.hr__());
			});
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	@Override
	public void printContentHorizontalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException {
		document.tr__(tr -> {
			for(int c = 0; c < colspansAndDirections.length; c += 2) {
				int direction = (c == 0) ? -1 : colspansAndDirections[c - 1];
				if(direction != -1) {
					switch(direction) {
						case UP:
							tr.td__("\u00A0");
							break;
						case DOWN:
							tr.td__("\u00A0");
							break;
						case UP_AND_DOWN:
							tr.td__("\u00A0");
							break;
						default: throw new IllegalArgumentException("Unknown direction: " + direction);
					}
				}

				int colspan = colspansAndDirections[c];
				tr.td().colspan(colspan).__(td -> td.hr__());
			}
		});
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	@Override
	public void printContentTitle(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException {
		startContentLine(document, req, resp, contentColumns, "center", null);
		document.h1__(title);
		endContentLine(document, req, resp, 1, false);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	// TODO: Return value to be passed on to other methods
	public void startContentLine(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws IOException {
		String align_ = trimNullIfEmpty(align);
		String width_ = trimNullIfEmpty(width);
		TD<TR_c<DocumentEE>> td = document.tr_c()
			.td();
			if(align_ != null || width_ != null) {
				td.style(style -> {
					if(align_ != null) {
						style.append("text-align:").append(align_);
					}
					if(width_ != null) {
						if(align_ != null) style.append(';');
						appendWidthStyle(width_, document.out);
					}
				});
			}
			td.attribute("valign", "top");
			/* TODO: return */ td.colspan(colspan)._c();
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	// TODO: Accept and return value to be passed on to other methods
	@Override
	public void printContentVerticalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws IOException {
		String align_ = trimNullIfEmpty(align);
		String width_ = trimNullIfEmpty(width);
		document.out.write("    </td>\n");
		switch(direction) {
			case UP_AND_DOWN:
				document.td__("\u00A0");
				break;
			case NONE:
				break;
			default: throw new IllegalArgumentException("Unknown direction: " + direction);
		}
		TD<DocumentEE> td = document.td();
		if(align_ != null || width_ != null) {
			td.style(style -> {
				if(align_ != null) {
					style.append("text-align:").append(align_);
				}
				if(width_ != null) {
					if(align_ != null) style.append(';');
					appendWidthStyle(width_, document.out);
				}
			});
		}
		/* TODO: return */ td
			.attribute("valign", "top")
			.colspan(colspan)
			.rowspan(rowspan)
			._c();
	}

	/**
	 * Ends one line of content.
	 */
	// TODO: Accept value from other methods
	@Override
	public void endContentLine(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws IOException {
		document.out.write("    </td>\n"
		+ "  </tr>\n");
	}

	/**
	 * Ends the content area of a page.
	 */
	// TODO: Accept value from other methods
	@Override
	public void endContent(WebPage page, DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws ServletException, IOException {
		int totalColumns=0;
		for(int c = 0; c < contentColumnSpans.length; c++) {
			if(c > 0) totalColumns += 1;
			totalColumns += contentColumnSpans[c];
		}
		int totalColumns_ = totalColumns;
		document.tr__(tr -> tr
			.td().colspan(totalColumns_).__(td -> td.hr__())
		);
		String copyright = page.getCopyright(req, resp, page);
		if(copyright != null && !(copyright = copyright.trim()).isEmpty()) {
			String copyright_ = copyright;
			document.tr__(tr -> tr
				.td().colspan(totalColumns_).style("text-align:center").__(td -> td
					// TODO: span unneeded, combine style into td?
					.span().style("font-size:x-small").__(copyright_)
				)
			);
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

	public <__ extends FlowContent<__>> void printLogo(WebPage page, __ td, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	/**
	 * Prints content below the related pages area on the left.
	 */
	public <__ extends FlowContent<__>> void printBelowRelatedPages(__ td, WebSiteRequest req) throws ServletException, IOException {
	}
}
