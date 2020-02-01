/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
import com.aoindustries.encoding.MediaWriter;
import com.aoindustries.encoding.TextInJavaScriptEncoder;
import com.aoindustries.html.Doctype;
import com.aoindustries.html.Html;
import com.aoindustries.html.Serialization;
import com.aoindustries.html.servlet.DoctypeEE;
import com.aoindustries.html.servlet.SerializationEE;
import com.aoindustries.net.URIEncoder;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
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
	 */
	final public String[] getLayoutChoices() {
		return layoutChoices;
	}

	/**
	 * Writes all of the HTML preceding the content of the page,
	 * whether the page is in a frameset or not.
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @see SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	abstract public void startHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out,
		String onload
	) throws IOException, SQLException;

	/**
	 * Writes all of the HTML following the content of the page,
	 * whether the page is in a frameset or not.
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @see SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 */
	abstract public void endHTML(
		WebPage page,
		WebSiteRequest req,
		HttpServletResponse resp,
		ChainWriter out
	) throws IOException, SQLException;

	/**
	 * Prints the content HTML that shows the output of a search.  This output must include an
	 * additional search form named <code>"search_two"</code>, with two fields named
	 * <code>"search_query"</code> and <code>"search_target"</code>.
	 *
	 * <p>
	 * Both the {@link Serialization} and {@link Doctype} may have been set
	 * on the request, and these must be considered in the HTML generation.
	 * </p>
	 *
	 * @see SerializationEE#get(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest)
	 * @see DoctypeEE#get(javax.servlet.ServletContext, javax.servlet.ServletRequest)
	 *
	 * @see WebPage#doPostWithSearch(com.aoindustries.website.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void printSearchOutput(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String query, boolean isEntireSite, List<SearchResult> results, String[] words) throws IOException, SQLException {
		Html html = page.getHtml(req, out);
		startContent(out, req, resp, 1, 600);
		printContentTitle(out, req, resp, "Search Results", 1);
		printContentHorizontalDivider(out, req, resp, 1, false);
		startContentLine(out, req, resp, 1, "center", null);
		beginLightArea(req, resp, out, null, "300", true);
		out.print("      <form action=\"\" id=\"search_two\" method=\"post\">\n");
		req.printFormFields(html);
		out.print("        <table cellspacing=\"0\" cellpadding=\"0\"><tr><td style=\"white-space:nowrap\">\n"
				+ "          Word(s) to search for: ");
		html.input.text().size(24).name("search_query").value(query).__().br__().nl();
		out.print("          Search Location: ");
		html.input.radio().name("search_target").value("entire_site").checked(isEntireSite).__();
		out.print(" Entire Site&#160;&#160;&#160;");
		html.input.radio().name("search_target").value("this_area").checked(!isEntireSite).__();
		// TODO: html.text methods (or innerText / innerHTML?)
		out.print(" This Area");
		html.br__().nl();
			out.print("          ");
			html.br__().nl();
			out.print("          <div style=\"text-align:center\">");
			html.input.submit().clazz("ao_button").value(" Search ").__();
			out.print("</div>\n"
				+ "        </td></tr></table>\n"
				+ "      </form>\n"
		);
		endLightArea(req, resp, out);
		endContentLine(out, req, resp, 1, false);
		printContentHorizontalDivider(out, req, resp, 1, false);
		startContentLine(out, req, resp, 1, "center", null);
		if (results.isEmpty()) {
			if (words.length > 0) {
				out.print(
					  "      <b>No matches found</b>\n"
				);
			}
		} else {
			beginLightArea(req, resp, out);
			out.print("  <table cellspacing=\"0\" cellpadding=\"0\" class=\"aoLightRow\">\n"
					+ "    <tr>\n"
					+ "      <th style=\"white-space:nowrap\">% Match</th>\n"
					+ "      <th style=\"white-space:nowrap\">Title</th>\n"
					+ "      <th style=\"white-space:nowrap\">&#160;</th>\n"
					+ "      <th style=\"white-space:nowrap\">Description</th>\n"
					+ "    </tr>\n"
			);

			// Find the highest probability
			float highest = results.get(0).getProbability();

			// Display the results
			int size = results.size();
			for (int c = 0; c < size; c++) {
				String rowClass= (c & 1) == 0 ? "aoLightRow":"aoDarkRow";
				String linkClass = (c & 1) == 0 ? "aoDarkLink":"aoLightLink";
				SearchResult result = results.get(c);
				String url=result.getUrl();
				String title=result.getTitle();
				String description=result.getDescription();
				out.print("    <tr class=\"").print(rowClass).print("\">\n"
						+ "      <td style=\"white-space:nowrap; text-align:center\">").print(Math.round(99 * result.getProbability() / highest)).print("%</td>\n"
						+ "      <td style=\"white-space:nowrap; text-align:left\"><a class=\""+linkClass+"\" href=\"")
					.encodeXmlAttribute(
						resp.encodeURL(
							URIEncoder.encodeURI(
								req.getContextPath() + url
							)
						)
					).print("\">").print(title.length()==0?"&#160;":title).print("</a></td>\n"
						+ "      <td style=\"white-space:nowrap\">&#160;&#160;&#160;</td>\n"
						+ "      <td style=\"white-space:nowrap; text-align:left\">").print(description.length()==0?"&#160;":description).print("</td>\n"
						+ "    </tr>\n");
			}
			out.print(
				  "  </table>\n"
			);
			endLightArea(req, resp, out);
		}
		endContentLine(out, req, resp, 1, false);
		endContent(page, out, req, resp, 1);
	}

	/**
	 * Starts the content area of a page.
	 */
	final public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int contentColumns, int preferredWidth) throws IOException, SQLException {
		startContent(out, req, resp, new int[] {contentColumns}, preferredWidth);
	}

	/**
	 * Starts the content area of a page.
	 */
	abstract public void startContent(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans, int preferredWidth) throws IOException, SQLException;

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	final public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, boolean endsInternal) throws IOException, SQLException {
		printContentHorizontalDivider(out, req, resp, new int[] {colspan}, endsInternal);
	}

	/**
	 * Prints a horizontal divider of the provided colspan.
	 */
	abstract public void printContentHorizontalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] colspansAndDirections, boolean endsInternal) throws IOException, SQLException;

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	final public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, WebPage page, int contentColumns) throws IOException, SQLException {
		printContentTitle(out, req, resp, page.getTitle(), contentColumns);
	}

	/**
	 * Prints the title of the page in one row in the content area.
	 */
	abstract public void printContentTitle(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, String title, int contentColumns) throws IOException, SQLException;

	/**
	 * Starts one line of content with the initial colspan set to the provided colspan.
	 */
	abstract public void startContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int colspan, String align, String width) throws IOException, SQLException;

	/**
	 * Ends one part of a line and starts the next.
	 */
	abstract public void printContentVerticalDivider(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int direction, int colspan, int rowspan, String align, String width) throws IOException, SQLException;

	/**
	 * Ends one line of content.
	 */
	abstract public void endContentLine(ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int rowspan, boolean endsInternal) throws IOException, SQLException;

	/**
	 * Ends the content area of a page.
	 */
	final public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int contentColumns) throws IOException, SQLException {
		endContent(page, out, req, resp, new int[] {contentColumns});
	}

	/**
	 * Ends the content area of a page.
	 */
	abstract public void endContent(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp, int[] contentColumnSpans) throws IOException, SQLException;

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
	final public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException {
		beginLightArea(req, resp, out, null, null, false);
	}

	/**
	 * Begins a lighter colored area of the site.
	 */
	abstract public void beginLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, String align, String width, boolean nowrap) throws IOException;

	/**
	 * Ends a lighter area of the site.
	 */
	abstract public void endLightArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException;

	/**
	 * Begins an area with a white background.
	 */
	final public void beginWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException {
		beginWhiteArea(req, resp, out, null, null, false);
	}

	/**
	 * Begins a lighter colored area of the site.
	 */
	abstract public void beginWhiteArea(WebSiteRequest req, HttpServletResponse response, ChainWriter out, String align, String width, boolean nowrap) throws IOException;

	/**
	 * Ends a lighter area of the site.
	 */
	abstract public void endWhiteArea(WebSiteRequest req, HttpServletResponse resp, ChainWriter out) throws IOException;

	/**
	 * Each layout has a name.
	 */
	abstract public String getName();

	public boolean printWebPageLayoutSelector(WebPage page, ChainWriter out, WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException {
		if(layoutChoices.length>=2) {
			Html html = page.getHtml(req, out);
			try (MediaWriter script = html.script().out__()) {
				script.write("  function selectLayout(layout) {\n");
				for(String choice : layoutChoices) {
					script.write("    if(layout==\"");
					TextInJavaScriptEncoder.encodeTextInJavaScript(choice, script);
					script.write("\") window.top.location.href=\"");
					TextInJavaScriptEncoder.encodeTextInJavaScript(
						req.getEncodedURL(page, "layout="+choice, resp),
						script
					);
					script.write("\";\n");
				}
				script.write("  }\n");
			}
			html.nl();
			out.print("<form action=\"#\" style=\"display:inline\"><div style=\"display:inline\">\n"
				+ "  <select name=\"layout_selector\" onchange=\"selectLayout(this.form.layout_selector.options[this.form.layout_selector.selectedIndex].value);\">\n");
			for(String choice : layoutChoices) {
				out.print("    ");
				html.option().value(choice).selected(choice.equalsIgnoreCase(getName())).text__(choice).nl();
			}
			out.print("  </select>\n"
				+ "</div></form>\n");
			return true;
		} else return false;
	}

	protected void printJavaScriptIncludes(WebSiteRequest req, HttpServletResponse resp, ChainWriter out, WebPage page) throws IOException, SQLException {
		Object O = page.getJavaScriptSrc(req);
		if (O != null) {
			Html html = page.getHtml(req, out);
			if (O instanceof String[]) {
				String[] SA = (String[]) O;
				int len = SA.length;
				for (int c = 0; c < len; c++) {
					out.write("    ");
					html.script().src(req.getEncodedURLForPath('/'+SA[c], null, false, resp)).__().nl();
				}
			} else if(O instanceof Class) {
				out.write("    ");
				html.script().src(req.getEncodedURL(((Class<?>)O).asSubclass(WebPage.class), null, resp)).__().nl();
			} else if(O instanceof WebPage) {
				out.write("    ");
				html.script().src(req.getEncodedURL((WebPage)O, resp)).__().nl();
			} else {
				out.write("    ");
				html.script().src(req.getEncodedURLForPath('/'+O.toString(), null, false, resp)).__().nl();
			}
		}
	}
}
