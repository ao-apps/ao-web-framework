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
import com.aoindustries.html.Document;
import com.aoindustries.html.FlowContent;
import com.aoindustries.html.LINK;
import com.aoindustries.html.META;
import com.aoindustries.html.SCRIPT;
import com.aoindustries.html.STYLE;
import com.aoindustries.html.TABLE;
import com.aoindustries.html.TABLE_c;
import com.aoindustries.html.TD;
import com.aoindustries.html.TD_c;
import com.aoindustries.html.TR_c;
import com.aoindustries.html.attributes.Enum.Method;
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
// TODO: Move into own microproject (also split into ao-web-framework-html)?
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

	// TODO: Return Content<?> and pass to endLightArea
	@Override
	public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, Document document, String align, String width, boolean nowrap) throws IOException {
		String align_ = trimNullIfEmpty(align);
		String width_ = trimNullIfEmpty(width);
		document.table().style(style -> {
			style.append("border:5px outset #a0a0a0");
			if(align_ != null) {
				style.append(";text-align:").append(align_);
			}
			if(width_ != null) {
				style.append(';');
				appendWidthStyle(width_, document.out);
			}
		}).cellpadding(0).cellspacing(0)._c()
			.tr_c()
				.td().clazz("aoLightRow").style("padding:4px", nowrap ? "white-space:nowrap" : null)._c();
	}

	@Override
	public void endLightArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException {
		document.out.write("</td>\n"
		+ "  </tr>\n"
		+ "</table>\n");
	}

	// TODO: Return Content<?> and pass to endWhiteArea
	@Override
	public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, Document document, String align, String width, boolean nowrap) throws IOException {
		String align_ = trimNullIfEmpty(align);
		String width_ = trimNullIfEmpty(width);
		document.table().style(style -> {
			style.append("border:5px outset #a0a0a0");
			if(align_ != null) {
				style.append(";text-align:").append(align_);
			}
			if(width_ != null) {
				style.append(';');
				appendWidthStyle(width_, document.out);
			}
		}).cellpadding(0).cellspacing(0)._c()
			.tr_c()
				.td().clazz("aoWhiteRow").style("background-color:white", "padding:4px", nowrap ? "white-space:nowrap" : null)._c();
	}

	@Override
	public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException {
		document.out.write("</td>\n"
		+ "  </tr>\n"
		+ "</table>\n");
	}

	@Override
	@SuppressWarnings("deprecation")
	public FlowContent<?> startPage(
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
			document.out.write("    "); document.meta(META.Name.ROBOTS).content("noindex, nofollow").__().nl();
		}
		if(document.doctype == Doctype.HTML5) {
			document.out.write("    "); document.meta().charset(resp.getCharacterEncoding()).__().nl();
		} else {
			document.out.write("    "); document.meta(META.HttpEquiv.CONTENT_TYPE).content(resp.getContentType()).__().out.write("\n"
			// Default style language
			+ "    "); document.meta(META.HttpEquiv.CONTENT_STYLE_TYPE).content(STYLE.Type.TEXT_CSS).__().out.write("\n"
			+ "    "); document.meta(META.HttpEquiv.CONTENT_SCRIPT_TYPE).content(SCRIPT.Type.TEXT_JAVASCRIPT).__().nl();
		}
		if(document.doctype == Doctype.HTML5) {
			GoogleAnalytics.writeGlobalSiteTag(document, trackingId);
		} else {
			GoogleAnalytics.writeAnalyticsJs(document, trackingId);
		}
		// Mobile support
		document.out.write("    "); document.meta(META.Name.VIEWPORT).content("width=device-width, initial-scale=1.0").__().out.write("\n"
		// TODO: This is probably only appropriate for single-page applications!
		//       See https://medium.com/@firt/dont-use-ios-web-app-meta-tag-irresponsibly-in-your-progressive-web-apps-85d70f4438cb
		+ "    "); document.meta(META.Name.APPLE_MOBILE_WEB_APP_CAPABLE).content("yes").__().out.write("\n"
		+ "    "); document.meta(META.Name.APPLE_MOBILE_WEB_APP_STATUS_BAR_STYLE).content("black").__().nl();
		// Authors
		// TODO: dcterms copyright
		String author = page.getAuthor();
		if(author != null && !(author = author.trim()).isEmpty()) {
			document.out.write("    "); document.meta(META.Name.AUTHOR).content(author).__().nl();
		}
		String authorHref = page.getAuthorHref(req, resp);
		if(authorHref != null && !(authorHref = authorHref.trim()).isEmpty()) {
			document.out.write("    "); document.link(LINK.Rel.AUTHOR).href(authorHref).__().nl();
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
			document.out.write("    "); document.meta(META.Name.DESCRIPTION).content(description).__().nl();
		}
		String keywords = page.getKeywords();
		if(keywords != null && !(keywords = keywords.trim()).isEmpty()) {
			document.out.write("    "); document.meta(META.Name.KEYWORDS).content(keywords).__().nl();
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
		document.out.write("  >\n");
		TD_c<TR_c<TABLE_c<Document>>> tdc = document.table().cellspacing(10).cellpadding(0)._c()
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
								div2.input().hidden().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_ENTIRE_SITE).__().nl();
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
						tdc.table().cellspacing(0).cellpadding(0).style("width:100%").__(table -> table
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
		return tdc;
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
		TD_c<TR_c<TABLE_c<Document>>> tdc = (TD_c<TR_c<TABLE_c<Document>>>)flow;
		Document document =
				tdc.__()
			.__()
		.__();
		document.out.write("  </body>\n");
		HtmlTag.endHtmlTag(document.out); document.nl();
	}

	/**
	 * Starts the content area of a page.
	 */
	@Override
	// TODO: Return value to be passed on to other methods
	public void startContent(Document document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException {
		TABLE<Document> table = document.table().cellpadding(0).cellspacing(0);
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
	public void printContentHorizontalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException {
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
	public void printContentTitle(Document document, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException {
		startContentLine(document, req, resp, contentColumns, "center", null);
		document.h1__(title).nl();
		endContentLine(document, req, resp, 1, false);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	@Override
	// TODO: Return value to be passed on to other methods
	public void startContentLine(Document document, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws IOException {
		String align_ = trimNullIfEmpty(align);
		String width_ = trimNullIfEmpty(width);
		TD<TR_c<Document>> td = document.tr_c()
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
	public void printContentVerticalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws IOException {
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
		TD<Document> td = document.td();
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
	public void endContentLine(Document document, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws IOException {
		document.out.write("    </td>\n"
		+ "  </tr>\n");
	}

	/**
	 * Ends the content area of a page.
	 */
	// TODO: Accept value from other methods
	@Override
	public void endContent(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws ServletException, IOException {
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

	public void printLogo(WebPage page, FlowContent<?> td, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	/**
	 * Prints content below the related pages area on the left.
	 */
	public void printBelowRelatedPages(FlowContent<?> td, WebSiteRequest req) throws ServletException, IOException {
	}
}
