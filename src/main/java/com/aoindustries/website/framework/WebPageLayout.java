/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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

import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Document;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.net.URIParametersMap;
import com.aoindustries.web.resources.registry.Registry;
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
	 * Configures the {@linkplain com.aoindustries.web.resources.servlet.RegistryEE.Request request-scope web resources} that this layout uses.
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
	 */
	abstract public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		Document document,
		String onload
	) throws ServletException, IOException;

	/**
	 * Writes all of the HTML following the content of the page,
	 * whether the page is in a frameset or not.
	 */
	abstract public void endHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		Document document
	) throws ServletException, IOException;

	/**
	 * Prints the content HTML that shows the output of a search.  This output must include an
	 * additional search form named {@link WebPage#SEARCH_TWO}, with two fields named
	 * {@link WebSiteRequest#SEARCH_QUERY} and {@link WebSiteRequest#SEARCH_TARGET}.
	 *
	 * @see WebPage#doPostWithSearch(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void printSearchOutput(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp, String query, boolean isEntireSite, List<SearchResult> results, String[] words) throws ServletException, IOException {
		startContent(document, req, resp, 1, 600);
		printContentTitle(document, req, resp, "Search Results", 1);
		printContentHorizontalDivider(document, req, resp, 1, false);
		startContentLine(document, req, resp, 1, "center", null);
		beginLightArea(req, resp, document, null, "300", true);
		document.out.write("      <form action=\"\" id=\"" + WebPage.SEARCH_TWO + "\" method=\"post\">\n");
		req.printFormFields(document);
		document.out.write("        <table cellspacing=\"0\" cellpadding=\"0\"><tr><td style=\"white-space:nowrap\">\n"
		+ "          "); document.text("Word(s) to search for:"); document.out.write(' ');
		document.input().text().size(24).name(WebSiteRequest.SEARCH_QUERY).value(query).__().br__().out.write("\n"
		+ "          "); document.text("Search Location:"); document.out.write(' ');
		document.input().radio().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_ENTIRE_SITE).checked(isEntireSite).__()
		.out.write(' '); document.text("Entire Site"); document.out.write("&#160;&#160;&#160;");
		document.input().radio().name(WebSiteRequest.SEARCH_TARGET).value(WebSiteRequest.SEARCH_THIS_AREA).checked(!isEntireSite).__()
		.out.write(' '); document.text("This Area").br__().out.write("\n"
		+ "          "); document.br__().out.write("\n"
		+ "          <div style=\"text-align:center\">"); document.input().submit().clazz("ao_button").value(" Search ").__().out.write("</div>\n"
		+ "        </td></tr></table>\n"
		+ "      </form>\n");
		endLightArea(req, resp, document);
		endContentLine(document, req, resp, 1, false);
		printContentHorizontalDivider(document, req, resp, 1, false);
		startContentLine(document, req, resp, 1, "center", null);
		if (results.isEmpty()) {
			if (words.length > 0) {
				document.out.write("      <b>"); document.text("No matches found"); document.out.write("</b>\n");
			}
		} else {
			beginLightArea(req, resp, document);
			document.out.write("  <table cellspacing=\"0\" cellpadding=\"0\" class=\"aoLightRow\">\n"
			+ "    <tr>\n"
			+ "      <th style=\"white-space:nowrap\">"); document.text("% Match"); document.out.write("</th>\n"
			+ "      <th style=\"white-space:nowrap\">"); document.text("Title"); document.out.write("</th>\n"
			+ "      <th style=\"white-space:nowrap\">&#160;</th>\n"
			+ "      <th style=\"white-space:nowrap\">"); document.text("Description"); document.out.write("</th>\n"
			+ "    </tr>\n");

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
				document.out.write("    <tr class=\""); document.out.write(rowClass); document.out.write("\">\n"
				+ "      <td style=\"white-space:nowrap; text-align:center\">"); document.text(Math.round(99 * result.getProbability() / highest) + "%"); document.out.write("</td>\n"
				+ "      <td style=\"white-space:nowrap; text-align:left\"><a class=\""); document.out.write(linkClass); document.out.write("\" href=\"");
				encodeTextInXhtmlAttribute(
					resp.encodeURL(
						URIEncoder.encodeURI(
							req.getContextPath() + url
						)
					),
					document.out
				);
				document.out.write("\">"); document.text(title); document.out.write("</a></td>\n"
				+ "      <td style=\"white-space:nowrap\">&#160;&#160;&#160;</td>\n"
				+ "      <td style=\"white-space:nowrap; text-align:left\">"); document.text(description); document.out.write("</td>\n"
				+ "    </tr>\n");
			}
			document.out.write("  </table>\n");
			endLightArea(req, resp, document);
		}
		endContentLine(document, req, resp, 1, false);
		endContent(page, document, req, resp, 1);
	}

	/**
	 * Starts the content area of a page.
	 */
	final public void startContent(Document document, WebSiteRequest req, HttpServletResponse resp, int contentColumns, int preferredWidth) throws ServletException, IOException {
		startContent(document, req, resp, new int[] {contentColumns}, preferredWidth);
	}

	/**
	 * Starts the content area of a page.
	 */
	abstract public void startContent(Document document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws ServletException, IOException;

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	final public void printContentHorizontalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int colspan, boolean endsInternal) throws ServletException, IOException {
		printContentHorizontalDivider(document, req, resp, new int[] {colspan}, endsInternal);
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	abstract public void printContentHorizontalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws ServletException, IOException;

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	final public void printContentTitle(Document document, WebSiteRequest req, HttpServletResponse resp, WebPage page, int contentColumns) throws ServletException, IOException {
		printContentTitle(document, req, resp, page.getTitle(), contentColumns);
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	abstract public void printContentTitle(Document document, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws ServletException, IOException;

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	abstract public void startContentLine(Document document, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws ServletException, IOException;

	/**
	 * Ends one part of a line and starts the next.
	 */
	abstract public void printContentVerticalDivider(Document document, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws ServletException, IOException;

	/**
	 * Ends one line of content.
	 */
	abstract public void endContentLine(Document document, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws ServletException, IOException;

	/**
	 * Ends the content area of a page.
	 */
	final public void endContent(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp, int contentColumns) throws ServletException, IOException {
		endContent(page, document, req, resp, new int[] {contentColumns});
	}

	/**
	 * Ends the content area of a page.
	 */
	abstract public void endContent(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws ServletException, IOException;

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
	 */
	final public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException {
		beginLightArea(req, resp, document, null, null, false);
	}

	/**
	 * Begins a lighter colored area of the site.
	 */
	abstract public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, Document document, String align, String width, boolean nowrap) throws IOException;

	/**
	 * Ends a lighter area of the site.
	 */
	abstract public void endLightArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException;

	/**
	 * Begins an area with a white background.
	 */
	final public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException {
		beginWhiteArea(req, resp, document, null, null, false);
	}

	/**
	 * Begins a lighter colored area of the site.
	 */
	abstract public void beginWhiteArea(WebSiteRequest req, HttpServletResponse response, Document document, String align, String width, boolean nowrap) throws IOException;

	/**
	 * Ends a lighter area of the site.
	 */
	abstract public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, Document document) throws IOException;

	/**
	 * Each layout has a name.
	 */
	abstract public String getName();

	public boolean printWebPageLayoutSelector(WebPage page, Document document, WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(layoutChoices.length >= 2) {
			document.script().out(script -> {
				script.append("function selectLayout(layout) {\n");
				for(String choice : layoutChoices) {
					script.append("  if(layout==").text(choice).append(") window.top.location.href=").text(
						req.getEncodedURL(page, URIParametersMap.of(WebSiteRequest.LAYOUT, choice), resp)
					).append(";\n");
				}
				script.append('}');
			}).__().out.write("\n"
			+ "<form action=\"#\" style=\"display:inline\"><div style=\"display:inline\">\n"
			+ "  <select name=\"layout_selector\" onchange=\"selectLayout(this.form.layout_selector.options[this.form.layout_selector.selectedIndex].value);\">\n");
			for(String choice : layoutChoices) {
				document.out.write("    "); document.option().value(choice).selected(choice.equalsIgnoreCase(getName())).text__(choice).nl();
			}
			document.out.write("  </select>\n"
			+ "</div></form>\n");
			return true;
		} else return false;
	}

	protected void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, Document document, WebPage page) throws ServletException, IOException {
		Object O = page.getJavaScriptSrc(req);
		if (O != null) {
			if (O instanceof String[]) {
				String[] SA = (String[]) O;
				int len = SA.length;
				for (int c = 0; c < len; c++) {
					document.out.write("    "); document.script().src(req.getEncodedURLForPath('/'+SA[c], null, false, resp)).__().nl();
				}
			} else if(O instanceof Class) {
				document.out.write("    "); document.script().src(req.getEncodedURL(((Class<?>)O).asSubclass(WebPage.class), null, resp)).__().nl();
			} else if(O instanceof WebPage) {
				document.out.write("    "); document.script().src(req.getEncodedURL((WebPage)O, resp)).__().nl();
			} else {
				document.out.write("    "); document.script().src(req.getEncodedURLForPath('/'+O.toString(), null, false, resp)).__().nl();
			}
		}
	}
}
