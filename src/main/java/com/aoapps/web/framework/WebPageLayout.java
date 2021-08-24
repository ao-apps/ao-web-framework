/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.html.any.attributes.Enum.Method;
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.ScriptSupportingContent;
import com.aoapps.lang.io.function.IOConsumerE;
import com.aoapps.lang.io.function.IORunnableE;
import com.aoapps.net.URIEncoder;
import com.aoapps.net.URIParametersMap;
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
abstract public class WebPageLayout {

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

	public WebPageLayout(String[] layoutChoices) {
		this.layoutChoices=layoutChoices;
	}

	/**
	 * Gets the names of every supported layout.  The layout at index 0 is the default.
	 * Is an empty array during a search.
	 *
	 * @return  No defensive copy is made
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField")
	final public String[] getLayoutChoices() {
		return layoutChoices;
	}

	/**
	 * Configures the {@linkplain com.aoapps.web.resources.servlet.RegistryEE.Request request-scope web resources} that this layout uses.
	 * <p>
	 * Implementers should call <code>super.configureResources(…)</code> as a matter of convention, despite this default implementation doing nothing.
	 * </p>
	 */
	@SuppressWarnings("NoopMethodInAbstractClass")
	public void configureResources(ServletContext servletContext, WebSiteRequest req, HttpServletResponse resp, WebPage page, Registry requestRegistry) {
		// Do nothing
	}

	/**
	 * Writes all of the HTML preceding the content of the page,
	 * whether the page is in a frameset or not.
	 *
	 * @return  The {@link FlowContent} that should be used to write the page contents.
	 *          This is also given to {@link #endPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *          to finish the template.
	 */
	abstract public <__ extends FlowContent<__>> __ startPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		DocumentEE document,
		String onload
	) throws ServletException, IOException;

	/**
	 * Writes all of the HTML following the content of the page,
	 * whether the page is in a frameset or not.
	 *
	 * @param  flow  The {@link FlowContent} that was returned by
	 *               {@link #startPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.DocumentEE, java.lang.String)}.
	 */
	abstract public void endPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> flow
	) throws ServletException, IOException;

	/**
	 * {@linkplain #startPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.DocumentEE, java.lang.String) Starts the page},
	 * invokes the given page body, then
	 * {@linkplain #endPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the page}.
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.DocumentEE, java.lang.String)
	 * @see  #endPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <__ extends FlowContent<__>, Ex extends Throwable> void doPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		DocumentEE document,
		String onload,
		IOConsumerE<? super __, Ex> body
	) throws ServletException, IOException, Ex {
		__ flow = startPage(page, req, resp, document, onload);
		if(body != null) body.accept(flow);
		endPage(page, req, resp, flow);
	}

	/**
	 * {@linkplain #startPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.DocumentEE, java.lang.String) Starts the page},
	 * invokes the given page body, then
	 * {@linkplain #endPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the page}.
	 *
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #startPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.DocumentEE, java.lang.String)
	 * @see  #endPage(com.aoapps.web.framework.WebPage, com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <Ex extends Throwable> void doPage(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		DocumentEE document,
		String onload,
		IORunnableE<Ex> body
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = startPage(page, req, resp, document, onload);
		if(body != null) body.run();
		endPage(page, req, resp, flow);
	}

	/**
	 * Prints the content HTML that shows the output of a search.  This output must include an
	 * additional search form named {@link WebPage#SEARCH_TWO}, with two fields named
	 * {@link WebSiteRequest#SEARCH_QUERY} and {@link WebSiteRequest#SEARCH_TARGET}.
	 *
	 * @see WebPage#doPostWithSearch(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 */
	public <__ extends FlowContent<__>> void printSearchOutput(WebPage page, __ flow, WebSiteRequest req, HttpServletResponse resp, String query, boolean isEntireSite, List<SearchResult> results, String[] words) throws ServletException, IOException {
		DocumentEE document = flow.getDocument();
		startContent(document, req, resp, 1, 600);
		printContentTitle(document, req, resp, "Search Results", 1);
		printContentHorizontalDivider(document, req, resp, 1, false);
		startContentLine(document, req, resp, 1, "center", null);
		lightArea(req, resp, flow, null, "300", true, lightArea -> lightArea
			.form("").id(WebPage.SEARCH_TWO).method(Method.Value.POST).__(form -> {
				req.printFormFields(form);
				form.table().cellspacing(0).cellpadding(0).__(table -> table
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
				);
			})
		);
		endContentLine(document, req, resp, 1, false);
		printContentHorizontalDivider(document, req, resp, 1, false);
		startContentLine(document, req, resp, 1, "center", null);
		if (results.isEmpty()) {
			if (words.length > 0) {
				flow.b__("No matches found");
			}
		} else {
			lightArea(req, resp, flow, lightArea -> lightArea
				.table().cellspacing(0).cellpadding(0).clazz("aoLightRow").__(table -> {
					table.tr__(tr -> tr
						.th().style("white-space:nowrap").__("% Match")
						.th().style("white-space:nowrap").__("Title")
						.th().style("white-space:nowrap").__("\u00A0")
						.th().style("white-space:nowrap").__("Description")
					);

					// Find the highest probability
					float highest = results.get(0).getProbability();

					// Display the results
					int size = results.size();
					for (int c = 0; c < size; c++) {
						String rowClass= (c & 1) == 0 ? "aoLightRow":"aoDarkRow";
						String linkClass = (c & 1) == 0 ? "aoDarkLink":"aoLightLink";
						SearchResult result = results.get(c);
						String url = result.getUrl();
						String title = result.getTitle();
						String description = result.getDescription();
						table.tr().clazz(rowClass).__(tr -> tr
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
				})
			);
		}
		endContentLine(document, req, resp, 1, false);
		endContent(page, document, req, resp, 1);
	}

	/**
	 * Starts the content area of a page.
	 */
	// TODO: 3.0.0: Lambda-friend variants of this and similar methods, that would call start, lambda, end
	final public void startContent(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int contentColumns, int preferredWidth) throws ServletException, IOException {
		startContent(document, req, resp, new int[] {contentColumns}, preferredWidth);
	}

	/**
	 * Starts the content area of a page.
	 */
	// TODO: 3.0.0: Return TBody<TODO> that would be used by lambda consumer
	abstract public void startContent(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws ServletException, IOException;

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	final public void printContentHorizontalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int colspan, boolean endsInternal) throws ServletException, IOException {
		printContentHorizontalDivider(document, req, resp, new int[] {colspan}, endsInternal);
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	abstract public void printContentHorizontalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws ServletException, IOException;

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	final public void printContentTitle(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, WebPage page, int contentColumns) throws ServletException, IOException {
		printContentTitle(document, req, resp, page.getTitle(), contentColumns);
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	abstract public void printContentTitle(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws ServletException, IOException;

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	abstract public void startContentLine(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws ServletException, IOException;

	/**
	 * Ends one part of a line and starts the next.
	 */
	abstract public void printContentVerticalDivider(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws ServletException, IOException;

	/**
	 * Ends one line of content.
	 */
	abstract public void endContentLine(DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws ServletException, IOException;

	/**
	 * Ends the content area of a page.
	 */
	final public void endContent(WebPage page, DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int contentColumns) throws ServletException, IOException {
		endContent(page, document, req, resp, new int[] {contentColumns});
	}

	/**
	 * Ends the content area of a page.
	 */
	abstract public void endContent(WebPage page, DocumentEE document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws ServletException, IOException;

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
	final public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc
	) throws ServletException, IOException {
		return beginLightArea(req, resp, pc, null, null, false);
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
	abstract public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginLightArea(
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
	 *               {@link #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *               or {@link #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, java.lang.String, java.lang.String, boolean)}.
	 */
	abstract public void endLightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> lightArea
	) throws ServletException, IOException;

	/**
	 * {@linkplain #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		IOConsumerE<? super __, Ex> lightArea
	) throws ServletException, IOException, Ex {
		__ flow = beginLightArea(req, resp, pc);
		if(lightArea != null) lightArea.accept(flow);
		endLightArea(req, resp, flow);
	}

	/**
	 * {@linkplain #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		IORunnableE<Ex> lightArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = beginLightArea(req, resp, pc);
		if(lightArea != null) lightArea.run();
		endLightArea(req, resp, flow);
	}

	/**
	 * {@linkplain #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
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
		IOConsumerE<? super __, Ex> lightArea
	) throws ServletException, IOException, Ex {
		__ flow = beginLightArea(req, resp, pc, align, width, nowrap);
		if(lightArea != null) lightArea.accept(flow);
		endLightArea(req, resp, flow);
	}

	/**
	 * {@linkplain #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a light area},
	 * invokes the given area body, then
	 * {@linkplain #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the light area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void lightArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap,
		IORunnableE<Ex> lightArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = beginLightArea(req, resp, pc, align, width, nowrap);
		if(lightArea != null) lightArea.run();
		endLightArea(req, resp, flow);
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
	final public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc
	) throws ServletException, IOException {
		return beginWhiteArea(req, resp, pc, null, null, false);
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
	abstract public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>
	> __ beginWhiteArea(
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
	 *               {@link #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
	 *               or {@link #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, java.lang.String, java.lang.String, boolean)}.
	 */
	abstract public void endWhiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		FlowContent<?> whiteArea
	) throws ServletException, IOException;

	/**
	 * {@linkplain #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
		PC extends FlowContent<PC>,
		__ extends FlowContent<__>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		IOConsumerE<? super __, Ex> whiteArea
	) throws ServletException, IOException, Ex {
		__ flow = beginWhiteArea(req, resp, pc);
		if(whiteArea != null) whiteArea.accept(flow);
		endWhiteArea(req, resp, flow);
	}

	/**
	 * {@linkplain #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		IORunnableE<Ex> whiteArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = beginWhiteArea(req, resp, pc);
		if(whiteArea != null) whiteArea.run();
		endWhiteArea(req, resp, flow);
	}

	/**
	 * {@linkplain #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <__>  This content model, which will be the parent content model of child elements
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
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
		IOConsumerE<? super __, Ex> whiteArea
	) throws ServletException, IOException, Ex {
		__ flow = beginWhiteArea(req, resp, pc, align, width, nowrap);
		if(whiteArea != null) whiteArea.accept(flow);
		endWhiteArea(req, resp, flow);
	}

	/**
	 * {@linkplain #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) Begins a white area},
	 * invokes the given area body, then
	 * {@linkplain #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent) ends the white area}.
	 *
	 * @param  <PC>  The parent content model this area is within
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 *
	 * @see  #beginWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 * @see  #endWhiteArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)
	 */
	public <
		PC extends FlowContent<PC>,
		Ex extends Throwable
	> void whiteArea(
		WebSiteRequest req,
		HttpServletResponse resp,
		PC pc,
		String align,
		String width,
		boolean nowrap,
		IORunnableE<Ex> whiteArea
	) throws ServletException, IOException, Ex {
		FlowContent<?> flow = beginWhiteArea(req, resp, pc, align, width, nowrap);
		if(whiteArea != null) whiteArea.run();
		endWhiteArea(req, resp, flow);
	}

	/**
	 * Each layout has a name.
	 */
	abstract public String getName();

	public <__ extends FlowContent<__>> boolean printWebPageLayoutSelector(WebPage page, __ flow, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(layoutChoices.length >= 2) {
			flow.script().out(script -> {
				script.indent().append("function selectLayout(layout) {").incDepth().nl();
				for(String choice : layoutChoices) {
					script.indent().append("if(layout==").text(choice).append(") window.top.location.href=").text(
						req.getEncodedURL(page, URIParametersMap.of(WebSiteRequest.LAYOUT, choice), resp)
					).append(';').nl();
				}
				script.decDepth().indent().append('}');
			}).__()
			.form("#").style("display:inline").__(form -> form
				.div().style("display:inline").__(div -> div
					// TODO: Constant for "layout_selector"?
					// TODO: onchange event
					.select().name("layout_selector").attribute("onchange", "selectLayout(this.form.layout_selector.options[this.form.layout_selector.selectedIndex].value);").__(select -> {
						for(String choice : layoutChoices) {
							select.option().value(choice).selected(choice.equalsIgnoreCase(getName())).__(choice);
						}
					})
				)
			);
			return true;
		} else return false;
	}

	protected <__ extends ScriptSupportingContent<__>> void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, __ content, WebPage page) throws ServletException, IOException {
		Object O = page.getJavaScriptSrc(req);
		if (O != null) {
			if (O instanceof String[]) {
				String[] SA = (String[]) O;
				int len = SA.length;
				for (int c = 0; c < len; c++) {
					content.script().src(req.getEncodedURLForPath('/'+SA[c], null, false, resp)).__();
				}
			} else if(O instanceof Class) {
				content.script().src(req.getEncodedURL(((Class<?>)O).asSubclass(WebPage.class), null, resp)).__();
			} else if(O instanceof WebPage) {
				content.script().src(req.getEncodedURL((WebPage)O, resp)).__();
			} else {
				content.script().src(req.getEncodedURLForPath('/'+O.toString(), null, false, resp)).__();
			}
		}
	}
}
