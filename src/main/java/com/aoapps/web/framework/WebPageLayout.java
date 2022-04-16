/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.encoding.Doctype;
import com.aoapps.encoding.Serialization;
import com.aoapps.encoding.servlet.DoctypeEE;
import com.aoapps.encoding.servlet.SerializationEE;
import com.aoapps.html.any.attributes.Enum.Method;
import com.aoapps.html.servlet.ContentEE;
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.ScriptSupportingContent;
import com.aoapps.net.URIEncoder;
import com.aoapps.net.URIParametersMap;
import com.aoapps.servlet.function.ServletConsumerE;
import com.aoapps.servlet.function.ServletRunnableE;
import com.aoapps.web.resources.registry.Registry;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * A <code>WebPageLayout</code> controls how a <code>WebPage</code> looks while providing a little
 * isolation from the code that provides the functionality.
 *
 * @author  AO Industries, Inc.
 */
public abstract class WebPageLayout {

	/**
	 * Directional references.
	 */
	public static final int
		NONE = 0,
		UP = 1,
		DOWN = 2,
		UP_AND_DOWN = 3
	;

	private final String[] layoutChoices;

	protected WebPageLayout(String[] layoutChoices) {
		this.layoutChoices=layoutChoices;
	}

	/**
	 * Gets the names of every supported layout.  The layout at index 0 is the default.
	 * Is an empty array during a search.
	 *
	 * @return  No defensive copy is made
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField")
	public final String[] getLayoutChoices() {
		return layoutChoices;
	}

	/**
	 * Configures the {@linkplain com.aoapps.web.resources.servlet.RegistryEE.Request request-scope web resources} that this layout uses.
	 * <p>
	 * Implementers should call <code>super.configureResources(…)</code> as a matter of convention, despite this default implementation doing nothing.
	 * </p>
	 */
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void configureResources(
		ServletContext servletContext,
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		Registry requestRegistry
	) {
		// Do nothing
	}

	public <__ extends FlowContent<__>> boolean printWebPageLayoutSelector(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		__ flow
	) throws ServletException, IOException {
		if(layoutChoices.length >= 2) {
			flow.script().out(script -> {
				script.indent().append("function selectLayout(layout) {").incDepth().nl();
				for(String choice : layoutChoices) {
					script
						.indent()
						.append("if(layout==")
						.text(choice)
						.append(") window.top.location.href=")
						.text(req.getEncodedURL(page, URIParametersMap.of(WebSiteRequest.LAYOUT.getName(), choice), resp))
						.append(';')
						.nl();
				}
				script.decDepth().indent().append('}');
			}).__()
			.form().action("").style("display:inline").__(form -> form
				.div().style("display:inline").__(div -> div
					// TODO: Constant for "layout_selector"?
					.select().name("layout_selector").onchange("selectLayout(this.form.layout_selector.options[this.form.layout_selector.selectedIndex].value);").__(select -> {
						for(String choice : layoutChoices) {
							select.option().value(choice).selected(choice.equalsIgnoreCase(getName())).__(choice); // TODO: .equals() like aoindustries.com:DefaultSkin.java?
						}
					})
				)
			);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Writes all of the HTML preceding the content of the page.
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @return  The {@link FlowContent} that should be used to write the page contents.
	 *          This is also given to {@link #endPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *          to finish the template.
	 *
	 * @see  SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see  DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	public abstract <__ extends FlowContent<__>> __ startPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		DocumentEE document,
		String onload
	) throws ServletException, IOException;

	/**
	 * Writes all of the HTML following the content of the page.
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @param  flow  The {@link FlowContent} that was returned by
	 *               {@link #startPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.DocumentEE, java.lang.String)}.
	 *
	 * @see  SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see  DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	public abstract void endPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		FlowContent<?> flow
	) throws ServletException, IOException;

	/**
	 * {@linkplain #startPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.DocumentEE, java.lang.String) Starts the page},
	 * invokes the given page body, then
	 * {@linkplain #endPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent) ends the page}.
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.DocumentEE, java.lang.String)
	 * @see  #endPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)
	 * @see  SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see  DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	public final <__ extends FlowContent<__>, Ex extends Throwable> void doPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		DocumentEE document,
		String onload,
		ServletConsumerE<? super __, Ex> body
	) throws ServletException, IOException, Ex {
		__ flow = startPage(req, resp, page, document, onload);
		if(body != null) body.accept(flow);
		endPage(req, resp, page, flow);
	}

	/**
	 * {@linkplain #startPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.DocumentEE, java.lang.String) Starts the page},
	 * invokes the given page body, then
	 * {@linkplain #endPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent) ends the page}.
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.DocumentEE, java.lang.String)
	 * @see  #endPage(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)
	 * @see  SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see  DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	public final <Ex extends Throwable> void doPage(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		DocumentEE document,
		String onload,
		ServletRunnableE<Ex> body
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = startPage(req, resp, page, document, onload);
		if(body != null) body.run();
		endPage(req, resp, page, flow);
	}

	/**
	 * Prints the content HTML that shows the output of a search.  This output must include an
	 * additional search form named {@link WebPage#SEARCH_TWO}, with two fields named
	 * {@link WebSiteRequest#SEARCH_QUERY} and {@link WebSiteRequest#SEARCH_TARGET}.
	 *
	 * @see WebPage#doPostWithSearch(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 */
	public <__ extends FlowContent<__>> void printSearchOutput(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		__ flow,
		String query,
		boolean isEntireSite,
		List<SearchResult> results,
		String[] words
	) throws ServletException, IOException {
		content(req, resp, page, flow, 1, "600px", 1, content -> {
			contentTitle(req, resp, content, "Search Results");
			contentHorizontalDivider(req, resp, content);
			contentLine(req, resp, content, 1, "center", null, 1, false, contentLine ->
				lightArea(req, resp, contentLine, null, "300", true, lightArea -> lightArea
					.form("").id(WebPage.SEARCH_TWO).method(Method.Value.POST).__(form -> {
						req.printFormFields(form);
						form.table().clazz("ao-packed").__(table -> table
							.tbody__(tbody -> tbody
								.tr__(tr -> tr
									.td().style("white-space:nowrap").__(td -> td
										.text("Word(s) to search for:").sp()
										.input().text().size(24).name(WebSiteRequest.SEARCH_QUERY).value(query).__().br__()
										.text("Search Location:").sp().input().radio().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_ENTIRE_SITE).checked(isEntireSite).__()
										.sp().text("Entire Site").nbsp(3).input().radio().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_THIS_AREA).checked(!isEntireSite).__()
										.sp().text("This Area").br__()
										.br__()
										.div().style("text-align:center").__(div -> div
											.input().submit().clazz("ao_button").value(" Search ").__()
										)
									)
								)
							)
						);
					})
				)
			);
			contentHorizontalDivider(req, resp, content);
			contentLine(req, resp, content, 1, "center", null, 1, false, contentLine -> {
				if (results.isEmpty()) {
					if (words.length > 0) {
						contentLine.b__("No matches found");
					}
				} else {
					lightArea(req, resp, contentLine, lightArea -> lightArea
						.table().clazz("ao-packed", "aoLightRow").__(table -> {
							table.thead__(thead -> thead
								.tr__(tr -> tr
									.th().style("white-space:nowrap").__("% Match")
									.th().style("white-space:nowrap").__("Title")
									.th().style("white-space:nowrap").__("\u00A0")
									.th().style("white-space:nowrap").__("Description")
								)
							);

							// Find the highest probability
							float highest = results.get(0).getProbability();

							// Display the results
							int size = results.size();
							if(size > 0) {
								table.tbody__(tbody -> {
									for (int c = 0; c < size; c++) {
										String rowClass= (c & 1) == 0 ? "aoLightRow":"aoDarkRow";
										String linkClass = (c & 1) == 0 ? "aoDarkLink":"aoLightLink";
										SearchResult result = results.get(c);
										String url = result.getUrl();
										String title = result.getTitle();
										String description = result.getDescription();
										tbody.tr().clazz(rowClass).__(tr -> tr
											.td().style("white-space:nowrap", "text-align:center").__(Math.round(99 * result.getProbability() / highest) + "%")
											.td().style("white-space:nowrap", "text-align:left").__(td -> td
												.a().clazz(linkClass).href(
													resp.encodeURL(
														URIEncoder.encodeURI(
															req.getContextPath() + url
														)
													)
												).__(title)
											)
											.td().style("white-space:nowrap").__("\u00A0\u00A0\u00A0")
											.td().style("white-space:nowrap", "text-align:left").__(description)
										);
									}
								});
							}
						})
					);
				}
			});
		});
	}

	/**
	 * Starts the content area of a page.  The content area provides additional features such as a nice border, and vertical and horizontal dividers.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 *
	 * @return  The {@link ContentEE} that should be used to write the area contents.
	 *          This is also given to {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, java.lang.String)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, java.lang.String, int)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)},
	 *          {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, boolean)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int[], boolean)},
	 *          {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)},
	 *          and {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[])}.
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>
	> __ startContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc
	) throws ServletException, IOException {
		return startContent(req, resp, page, pc, new int[] {1}, null);
	}

	/**
	 * Starts the content area of a page.  The content area provides additional features such as a nice border, and vertical and horizontal dividers.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  width  When {@code null}, will use {@linkplain WebPage#getPreferredContentWidth(com.aoapps.web.framework.WebSiteRequest) page's preferred width}, if any.
	 *                When {@code ""}, will force no width specified.
	 *
	 * @return  The {@link ContentEE} that should be used to write the area contents.
	 *          This is also given to {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, java.lang.String)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, java.lang.String, int)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)},
	 *          {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, boolean)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int[], boolean)},
	 *          {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)},
	 *          and {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[])}.
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>
	> __ startContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int contentColumns,
		String width
	) throws ServletException, IOException {
		return startContent(req, resp, page, pc, new int[] {contentColumns}, width);
	}

	/**
	 * Starts the content area of a page.  The content area provides additional features such as a nice border, and vertical and horizontal dividers.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  width  When {@code null}, will use {@linkplain WebPage#getPreferredContentWidth(com.aoapps.web.framework.WebSiteRequest) page's preferred width}, if any.
	 *                When {@code ""}, will force no width specified.
	 *
	 * @return  The {@link ContentEE} that should be used to write the area contents.
	 *          This is also given to {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, java.lang.String)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, java.lang.String, int)},
	 *          {@link #contentTitle(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)},
	 *          {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, boolean)},
	 *          {@link #contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int[], boolean)},
	 *          {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)},
	 *          {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)},
	 *          and {@link #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[])}.
	 */
	public abstract <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>
	> __ startContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int[] contentColumnSpans,
		String width
	) throws ServletException, IOException;

	/**
	 * Prints an entire content line including the provided title.  The colspan should match the total colspan in startContent for proper appearance
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void contentTitle(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content
	) throws ServletException, IOException {
		contentTitle(req, resp, content, page.getTitle(req), 1);
	}

	/**
	 * Prints an entire content line including the provided title.  The colspan should match the total colspan in startContent for proper appearance
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void contentTitle(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content,
		int contentColumns
	) throws ServletException, IOException {
		contentTitle(req, resp, content, page.getTitle(req), contentColumns);
	}

	/**
	 * Prints an entire content line including the provided title.  The colspan should match the total colspan in startContent for proper appearance
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void contentTitle(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		String title
	) throws ServletException, IOException {
		contentTitle(req, resp, content, title, 1);
	}

	/**
	 * Prints an entire content line including the provided title.  The colspan should match the total colspan in startContent for proper appearance
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public abstract void contentTitle(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		String title,
		int contentColumns
	) throws ServletException, IOException;

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 *
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 *
	 * @return  The {@link FlowContent} that should be used to write the line contents.
	 *          This is also given to {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)},
	 *          {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          and {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean)}.
	 */
	public final <__ extends FlowContent<__>> __ startContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content
	) throws ServletException, IOException {
		return startContentLine(req, resp, content, 1, null, null);
	}

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 *
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 *
	 * @return  The {@link FlowContent} that should be used to write the line contents.
	 *          This is also given to {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)},
	 *          {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          and {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean)}.
	 */
	public abstract <__ extends FlowContent<__>> __ startContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int colspan,
		String align,
		String width
	) throws ServletException, IOException;

	/**
	 * Ends one part of a line and starts the next.
	 *
	 * @param  contentLine  The {@link FlowContent} that was returned by
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *                      {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *                      or {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
	 *
	 * @return  The {@link FlowContent} that should be used to write the line contents.
	 *          This is also given to {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)},
	 *          {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          and {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean)}.
	 */
	public final <__ extends FlowContent<__>> __ contentVerticalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine
	) throws ServletException, IOException {
		return contentVerticalDivider(req, resp, contentLine, UP_AND_DOWN, 1, 1, null, null);
	}

	/**
	 * Ends one part of a line and starts the next.
	 *
	 * @param  contentLine  The {@link FlowContent} that was returned by
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *                      {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *                      or {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
	 *
	 * @return  The {@link FlowContent} that should be used to write the line contents.
	 *          This is also given to {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)},
	 *          {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *          and {@link #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean)}.
	 */
	public abstract <__ extends FlowContent<__>> __ contentVerticalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine,
		int direction, // TODO: This should be an enum, or maybe "boolean visible" like aoweb-struts:Skin.java
		int colspan,
		int rowspan,
		String align,
		String width
	) throws ServletException, IOException;

	/**
	 * Ends one line of content.
	 *
	 * @param  contentLine  The {@link FlowContent} that was returned by
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *                      {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *                      or {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
	 */
	public final void endContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine
	) throws ServletException, IOException {
		endContentLine(req, resp, contentLine, 1, false);
	}

	/**
	 * Ends one line of content.
	 *
	 * @param  contentLine  The {@link FlowContent} that was returned by
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)},
	 *                      {@link #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)},
	 *                      {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)},
	 *                      or {@link #contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
	 */
	public abstract void endContentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> contentLine,
		int rowspan,
		boolean endsInternal
	) throws ServletException, IOException;

	/**
	 * {@linkplain #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE) Starts a content line},
	 * invokes the given line body, then
	 * {@linkplain #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the content line}.
	 *
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)
	 * @see  #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void contentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		ServletConsumerE<? super __, Ex> contentLine
	) throws ServletException, IOException, Ex {
		this.<__, Ex>contentLine(req, resp, content, 1, null, null, 1, false, contentLine);
	}

	/**
	 * {@linkplain #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE) Starts a content line},
	 * invokes the given line body, then
	 * {@linkplain #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the content line}.
	 *
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)
	 * @see  #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void contentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		ServletRunnableE<Ex> contentLine
	) throws ServletException, IOException, Ex {
		contentLine(req, resp, content, 1, null, null, 1, false, contentLine);
	}

	/**
	 * {@linkplain #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String) Starts a content line},
	 * invokes the given line body, then
	 * {@linkplain #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean) ends the content line}.
	 *
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)
	 * @see  #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean)
	 */
	public final <
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void contentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int colspan,
		String align,
		String width,
		int endRowspan,
		boolean endsInternal,
		ServletConsumerE<? super __, Ex> contentLine
	) throws ServletException, IOException, Ex {
		__ flow = startContentLine(req, resp, content, colspan, align, width); {
			if(contentLine != null) contentLine.accept(flow);
		} endContentLine(req, resp, flow, endRowspan, endsInternal);
	}

	/**
	 * {@linkplain #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String) Starts a content line},
	 * invokes the given line body, then
	 * {@linkplain #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean) ends the content line}.
	 *
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE, int, java.lang.String, java.lang.String)
	 * @see  #endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, boolean)
	 */
	public final <
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void contentLine(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int colspan,
		String align,
		String width,
		int endRowspan,
		boolean endsInternal,
		ServletRunnableE<Ex> contentLine
	) throws ServletException, IOException, Ex {
		__ flow = startContentLine(req, resp, content, colspan, align, width); {
			if(contentLine != null) contentLine.run();
		} endContentLine(req, resp, flow, endRowspan, endsInternal);
	}

	/**
	 * Prints a horizontal divider.
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void contentHorizontalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content
	) throws ServletException, IOException {
		contentHorizontalDivider(req, resp, content, new int[] {1}, false);
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void contentHorizontalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int colspan,
		boolean endsInternal
	) throws ServletException, IOException {
		contentHorizontalDivider(req, resp, content, new int[] {colspan}, endsInternal);
	}

	/**
	 * Prints a horizontal divider of the provided colspans.
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public abstract void contentHorizontalDivider(
		WebSiteRequest req,
		HttpServletResponse resp,
		ContentEE<?> content,
		int[] colspansAndDirections,
		boolean endsInternal
	) throws ServletException, IOException;

	/**
	 * Ends the content area of a page.
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void endContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content
	) throws ServletException, IOException {
		endContent(req, resp, page, content, new int[] {1});
	}

	/**
	 * Ends the content area of a page.
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public final void endContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content,
		int contentColumns
	) throws ServletException, IOException {
		endContent(req, resp, page, content, new int[] {contentColumns});
	}

	/**
	 * Ends the content area of a page.
	 *
	 * @param  content  The {@link ContentEE} that was returned by
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)}
	 *                  {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)}
	 *                  or {@link #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)}.
	 */
	public abstract void endContent(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		ContentEE<?> content,
		int[] contentColumnSpans
	) throws ServletException, IOException;

	/**
	 * {@linkplain #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent) Starts a content area},
	 * invokes the given area body, then
	 * {@linkplain #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE) ends the content area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)
	 * @see  #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>,
		Ex extends Throwable
	> void content(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		ServletConsumerE<? super __, Ex> content
	) throws ServletException, IOException, Ex {
		this.<PC, __, Ex>content(req, resp, page, pc, new int[] {1}, null, new int[] {1}, content);
	}

	/**
	 * {@linkplain #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent) Starts a content area},
	 * invokes the given area body, then
	 * {@linkplain #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE) ends the content area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent)
	 * @see  #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>,
		Ex extends Throwable
	> void content(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		ServletRunnableE<Ex> content
	) throws ServletException, IOException, Ex {
		content(req, resp, page, pc, new int[] {1}, null, new int[] {1}, content);
	}

	/**
	 * {@linkplain #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String) Starts a content area},
	 * invokes the given area body, then
	 * {@linkplain #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int) ends the content area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)
	 * @see  #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>,
		Ex extends Throwable
	> void content(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int startContentColumns,
		String width,
		int endContentColumns,
		ServletConsumerE<? super __, Ex> content
	) throws ServletException, IOException, Ex {
		this.<PC, __, Ex>content(req, resp, page, pc, new int[] {startContentColumns}, width, new int[] {endContentColumns}, content);
	}

	/**
	 * {@linkplain #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String) Starts a content area},
	 * invokes the given area body, then
	 * {@linkplain #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int) ends the content area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int, java.lang.String)
	 * @see  #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>,
		Ex extends Throwable
	> void content(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int startContentColumns,
		String width,
		int endContentColumns,
		ServletRunnableE<Ex> content
	) throws ServletException, IOException, Ex {
		content(req, resp, page, pc, new int[] {startContentColumns}, width, new int[] {endContentColumns}, content);
	}

	/**
	 * {@linkplain #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String) Starts a content area},
	 * invokes the given area body, then
	 * {@linkplain #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[]) ends the content area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)
	 * @see  #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[])
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>,
		Ex extends Throwable
	> void content(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int[] startContentColumnSpans,
		String width,
		int[] endContentColumnSpans,
		ServletConsumerE<? super __, Ex> content
	) throws ServletException, IOException, Ex {
		__ contentEE = startContent(req, resp, page, pc, startContentColumnSpans, width); {
			if(content != null) content.accept(contentEE);
		} endContent(req, resp, page, contentEE, endContentColumnSpans);
	}

	/**
	 * {@linkplain #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String) Starts a content area},
	 * invokes the given area body, then
	 * {@linkplain #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[]) ends the content area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.FlowContent, int[], java.lang.String)
	 * @see  #endContent(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPage, com.aoapps.html.servlet.ContentEE, int[])
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends ContentEE<__>,
		Ex extends Throwable
	> void content(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		PC pc,
		int[] startContentColumnSpans,
		String width,
		int[] endContentColumnSpans,
		ServletRunnableE<Ex> content
	) throws ServletException, IOException, Ex {
		__ contentEE = startContent(req, resp, page, pc, startContentColumnSpans, width); {
			if(content != null) content.run();
		} endContent(req, resp, page, contentEE, endContentColumnSpans);
	}

	/**
	 * The background color for the page or <code>-1</code> for browser default.
	 */
	public int getBackgroundColor(WebSiteRequest req) {
		return -1;
	}

	/**
	 * The text color for the page or <code>-1</code> for browser default.
	 */
	public int getTextColor(WebSiteRequest req) {
		return -1;
	}

	/**
	 * The link color for the page or <code>-1</code> for browser default.
	 */
	public int getLinkColor(WebSiteRequest req) {
		return -1;
	}

	/**
	 * The visited link color for the page or <code>-1</code> for browser default.
	 */
	public int getVisitedLinkColor(WebSiteRequest req) {
		return -1;
	}

	/**
	 * The active link color for the page or <code>-1</code> for browser default.
	 */
	public int getActiveLinkColor(WebSiteRequest req) {
		return -1;
	}

	/**
	 * Begins a lighter colored area of the site.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 *
	 * @return  The {@link FlowContent} that should be used to write the area contents.
	 *          This is also given to {@link #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *          to finish the area.
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ startLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc
	) throws ServletException, IOException {
		return startLightArea(req, resp, pc, null, null, false);
	}

	/**
	 * Begins a lighter colored area of the site.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 *
	 * @return  The {@link FlowContent} that should be used to write the area contents.
	 *          This is also given to {@link #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *          to finish the area.
	 */
	public abstract <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ startLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException;

	/**
	 * Ends a lighter area of the site.
	 *
	 * @param  lightArea  The {@link FlowContent} that was returned by
	 *                    {@link #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *                    or {@link #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, java.lang.String, java.lang.String, boolean)}.
	 */
	public abstract void endLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> lightArea
	) throws ServletException, IOException;

	/**
	 * {@linkplain #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		ServletConsumerE<? super __, Ex> lightArea
	) throws ServletException, IOException, Ex {
		__ flow = startLightArea(req, resp, pc); {
			if(lightArea != null) lightArea.accept(flow);
		} endLightArea(req, resp, flow);
	}

	/**
	 * {@linkplain #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		ServletRunnableE<Ex> lightArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = startLightArea(req, resp, pc); {
			if(lightArea != null) lightArea.run();
		} endLightArea(req, resp, flow);
	}

	/**
	 * {@linkplain #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap,
		ServletConsumerE<? super __, Ex> lightArea
	) throws ServletException, IOException, Ex {
		__ flow = startLightArea(req, resp, pc, align, width, nowrap); {
			if(lightArea != null) lightArea.accept(flow);
		} endLightArea(req, resp, flow);
	}

	/**
	 * {@linkplain #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap,
		ServletRunnableE<Ex> lightArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = startLightArea(req, resp, pc, align, width, nowrap); {
			if(lightArea != null) lightArea.run();
		} endLightArea(req, resp, flow);
	}

	/**
	 * Begins a white area of the site.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 *
	 * @return  The {@link FlowContent} that should be used to write the area contents.
	 *          This is also given to {@link #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *          to finish the area.
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ startWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc
	) throws ServletException, IOException {
		return startWhiteArea(req, resp, pc, null, null, false);
	}

	/**
	 * Begins a white area of the site.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 *
	 * @return  The {@link FlowContent} that should be used to write the area contents.
	 *          This is also given to {@link #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *          to finish the area.
	 */
	public abstract <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ startWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap
	) throws ServletException, IOException;

	/**
	 * Ends a white area of the site.
	 *
	 * @param  whiteArea  The {@link FlowContent} that was returned by
	 *                    {@link #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *                    or {@link #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, java.lang.String, java.lang.String, boolean)}.
	 */
	public abstract void endWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> whiteArea
	) throws ServletException, IOException;

	/**
	 * {@linkplain #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		ServletConsumerE<? super __, Ex> whiteArea
	) throws ServletException, IOException, Ex {
		__ flow = startWhiteArea(req, resp, pc); {
			if(whiteArea != null) whiteArea.accept(flow);
		} endWhiteArea(req, resp, flow);
	}

	/**
	 * {@linkplain #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		ServletRunnableE<Ex> whiteArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = startWhiteArea(req, resp, pc); {
			if(whiteArea != null) whiteArea.run();
		} endWhiteArea(req, resp, flow);
	}

	/**
	 * {@linkplain #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap,
		ServletConsumerE<? super __, Ex> whiteArea
	) throws ServletException, IOException, Ex {
		__ flow = startWhiteArea(req, resp, pc, align, width, nowrap); {
			if(whiteArea != null) whiteArea.accept(flow);
		} endWhiteArea(req, resp, flow);
	}

	/**
	 * {@linkplain #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public final <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap,
		ServletRunnableE<Ex> whiteArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = startWhiteArea(req, resp, pc, align, width, nowrap); {
			if(whiteArea != null) whiteArea.run();
		} endWhiteArea(req, resp, flow);
	}

	/**
	 * Each layout has a name.
	 */
	public abstract String getName();

	protected <__ extends ScriptSupportingContent<__>> void printJavascriptIncludes(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPage page,
		__ content
	) throws ServletException, IOException {
		Object src = page.getJavascriptSrc(req);
		if (src != null) {
			if (src instanceof String[]) {
				String[] sa = (String[]) src;
				int len = sa.length;
				for (int c = 0; c < len; c++) {
					content.script().src(req.getEncodedURLForPath('/' + sa[c], null, false, resp)).__();
				}
			} else if(src instanceof Class) {
				content.script().src(req.getEncodedURL(((Class<?>)src).asSubclass(WebPage.class), null, resp)).__();
			} else if(src instanceof WebPage) {
				content.script().src(req.getEncodedURL((WebPage)src, resp)).__();
			} else {
				content.script().src(req.getEncodedURLForPath('/' + src.toString(), null, false, resp)).__();
			}
		}
	}
}
