/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2014, 2015, 2016, 2019, 2020, 2021 AO Industries, Inc.
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

import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.html.Html;
import com.aoindustries.io.ContentType;
import com.aoindustries.io.IoUtils;
import com.aoindustries.lang.EmptyArrays;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.net.URIParametersMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * @author  AO Industries, Inc.
 */
abstract public class TreePage extends WebPage {

	private static final long serialVersionUID = 1L;

	/**
	 * The color of the lines.
	 */
	public static final int TREE_LINE_COLOR=0xa8a8a8;

	/**
	 * The parameter name to control the TreePage display mode.
	 */
	public static final String TREEPAGE_MODE="treepage_mode";

	/**
	 * Indicates the TreePage should automatically select the correct mode,
	 * this is the default if the parameter is not provided.
	 */
	public static final String MODE_AUTO="auto";

	/**
	 * Indicates the TreePage should display its contents in a text-only format.
	 */
	public static final String MODE_TEXT="text";

	/**
	 * Indicates the TreePage should display its contents in an interactive format.
	 */
	public static final String MODE_GUI="gui";

	/**
	 * The width of the images.
	 */
	private static final int IMAGE_WIDTH=24;

	/**
	 * The height of the images.
	 */
	private static final int IMAGE_HEIGHT=32;

	private static byte[] blank;
	private static final byte[][] jpgCache=new byte[9][];
	private static final byte[][] gifCache=new byte[9][];

	private static byte[] getImageBytes(int imageNum, boolean isSmooth) throws IOException {
		synchronized(jpgCache) {
			byte[] bytes;
			if(imageNum==0) {
				// Load the blank image
				bytes = blank;
				if(bytes==null) {
					try (InputStream in = TreePage.class.getResourceAsStream("images/blank.gif")) {
						blank = bytes = IoUtils.readFully(in);
					}
				}
			} else {
				byte[][] cache=isSmooth?jpgCache:gifCache;
				bytes=cache[imageNum-1];
				if(bytes==null) {
					try (InputStream in = TreePage.class.getResourceAsStream("images/tree_"+imageNum+"."+(isSmooth?"jpg":"gif"))) {
						cache[imageNum-1] = bytes = IoUtils.readFully(in);
					}
				}
			}
			return bytes;
		}
	}

	@Override
	public void doGet(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException, SQLException {
		String S=req.getParameter("image_num");
		if(S == null) {
			super.doGet(req, resp);
		} else {
			try {
				int imageNum=Integer.parseInt(S);
				if(imageNum<0 || imageNum>9) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find image number "+imageNum);
				} else {
					boolean useSmooth=useSmoothOutline(req);
					resp.setContentType(imageNum==0?ContentType.GIF:useSmooth?ContentType.JPEG:ContentType.GIF);
					byte[] bytes=getImageBytes(imageNum, useSmooth);
					try (OutputStream out = resp.getOutputStream()) {
						out.write(bytes);
					}
				}
			} catch(NumberFormatException err) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to parse image_num");
			}
		}
	}

	@Override
	public long getSearchLastModified() throws IOException, SQLException {
		return getClassLastModified();
	}

	@Override
	public long getLastModified(WebSiteRequest req, HttpServletResponse resp) {
		if(req==null) return -1;
		String S=req.getParameter("image_num");
		return S==null?-1:getUptime();
	}

	@Override
	public void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		Html html,
		WebPageLayout layout
	) throws IOException, SQLException {
		List<? extends TreePageData> tree = getTree(req, resp);
		String mode;
		if(displayText(req)) {
			int treeLen = tree.size();

			// Break apart each line
			String[][] paths = getLines(tree);

			// Get the widest of the lines
			int longest=0;
			for(String path[] : paths) {
				int width=0;
				for (int d=0; d<path.length; d++) {
					if(d>0) width+=3;
					width+=path[d].length();
				}
				width+=3;
				if(width>longest) longest=width;
			}

			html.out.write("<pre>\n");

			String[] last = EmptyArrays.EMPTY_STRING_ARRAY;
			for (int c = 0; c < treeLen; c++) {
				int width = 0;
				String[] path = paths[c];
				int pathLen = path.length;
				int max = Math.min(pathLen - 1, last.length);
				int pos = 0;
				for (; pos < max; pos++) {
					if (last[pos].equals(path[pos])) {
						if (pos > 0) {
							boolean hasMore = false;
							for (int d = c + 1; d < treeLen; d++) {
								int end = pos;
								for (int e = 0; e < end; e++) {
									if (paths[d][e].equals(path[e])) {
										if (e == (end - 1) && !paths[d][end].equals(path[end])) {
											hasMore = true;
											break;
										}
									} else break;
								}
							}
							html.out.write(hasMore ? "|  " : "   ");
							width += 3;
						}
						int len2 = last[pos].length();
						for (int d = 0; d < len2; d++) {
							html.out.write(' ');
						}
						width += len2;
					} else break;
				}
				for (; pos < pathLen; pos++) {
					String p = path[pos];
					if (pos > 0 && p.length() > 0) {
						html.out.write("+--");
						width += 3;
					}
					String href;
					if (pos == (path[pathLen-1].length()==0?(pathLen-2):(pathLen-1)) && (href = tree.get(c).getUrl()) != null) {
						html.out.write("<a href='");
						encodeTextInXhtmlAttribute(
							resp.encodeURL(
								URIEncoder.encodeURI(
									req.getContextPath() + href
								)
							),
							html.out
						);
						html.out.write("'>"); html.text(p); html.out.write("</a>");
					} else {
						html.text(p);
					}
					if (
						p.length() > 0
						&& (
							pos < (pathLen - 1)
							|| tree.get(c).hasChildren()
						)
					) {
						html.out.write('/');
					} else {
						html.out.write(' ');
					}
					width += p.length() + 1;
				}
				for (; width < longest; width++) {
					html.out.write(' ');
				}
				String description=tree.get(c).getDescription();
				if(description != null) html.text(description).nl();

				last = path;
			}
			html.out.write("</pre>\n");
		} else {
			handleRequest(html, req, resp, tree, -1, -1, null);
		}
	}

	// TODO: Override different method that already does everything before the layout
	@Override
	public void doPost(
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException, SQLException {
		List<? extends TreePageData> tree = getTree(req, resp);

		// Get the scroll to position
		int scrollToX = Integer.parseInt(req.getParameter("scroll_to_x"));
		int scrollToY = Integer.parseInt(req.getParameter("scroll_to_y"));

		Html html = getHTML(req, resp);
		WebPageLayout layout = getWebPageLayout(req);
		layout.startHTML(
			this,
			req,
			resp,
			html,
			scrollToX >= 0 ? ("window.scrollTo(" + scrollToX + ", " + scrollToY + ");") : null
		);

		int treeLen = tree.size();
		boolean[] opened = new boolean[treeLen];
		for (int c = 0; c < treeLen; c++) {
			opened[c] = Boolean.parseBoolean(req.getParameter("opened_" + c));
		}

		// Print the new table
		handleRequest(html, req, resp, tree, scrollToX, scrollToY, opened);

		layout.endHTML(this, req, resp, html);
	}

	/**
	 * Gets the tree to be displayed.  Each row consists of three elements: path, href, description
	 */
	abstract protected List<? extends TreePageData> getTree(WebSiteRequest req, HttpServletResponse resp) throws IOException, SQLException;

	/**
	 * Handles the interactive form of this page.
	 */
	private void handleRequest(
		Html html,
		WebSiteRequest req,
		HttpServletResponse resp,
		List<? extends TreePageData> tree,
		int scrollToX,
		int scrollToY,
		boolean[] opened
	) throws IOException, SQLException {
		WebPageLayout layout=getWebPageLayout(req);
		layout.startContent(html, req, resp, 1, getPreferredContentWidth(req));
		layout.printContentTitle(html, req, resp, this, 1);
		layout.printContentHorizontalDivider(html, req, resp, 1, false);
		layout.startContentLine(html, req, resp, 1, null, null);
		// Get the tree data
		int treeLen = tree.size();

		// Break apart each line
		String[][] paths = getLines(tree);

		if (opened == null) {
			// Default to opened for first item
			opened = new boolean[treeLen];
			if (treeLen > 0) opened[0] = true;
		}

		// Write the javascript that controls the form
		try (MediaWriter script = html.script().out__()) {
			script.write("  function openNode(index) {\n"
				+ "    eval('document.forms[\"tree_form\"].opened_'+index+'.value=\"true\";');\n"
				+ "    document.forms[\"tree_form\"].scroll_to_x.value=getPageXOffset(window);\n"
				+ "    document.forms[\"tree_form\"].scroll_to_y.value=getPageYOffset(window);\n"
				+ "    document.forms[\"tree_form\"].submit();\n"
				+ "  }\n"
				+ "\n"
				+ "  function closeNode(index) {\n"
				+ "    eval('document.forms[\"tree_form\"].opened_'+index+'.value=\"false\";');\n"
				+ "    document.forms[\"tree_form\"].scroll_to_x.value=getPageXOffset(window);\n"
				+ "    document.forms[\"tree_form\"].scroll_to_y.value=getPageYOffset(window);\n"
				+ "    document.forms[\"tree_form\"].submit();\n"
				+ "  }\n");
		}
		html.nl();

		// Write the form containing the current settings
		html.out.write("<form action='' id='tree_form' method='post'><div>\n");
		req.printFormFields(html);
		html.out.write("  ");
		html.input.hidden().name("scroll_to_x").value(scrollToX).__().nl();
		html.out.write("  ");
		html.input.hidden().name("scroll_to_y").value(scrollToY).__().nl();
		for(int c=0; c<treeLen; c++) {
			html.out.write("  ");
			html.input.hidden().name("opened_" + c).value(opened[c]).__().nl();
		}

		// Display the tree in a table with links for opening/closing the different parts
		html.out.write("  <table cellspacing='0' cellpadding='0'>\n");

		String[] last = EmptyArrays.EMPTY_STRING_ARRAY;
		for (int c = 0; c < treeLen; c++) {
			String[] path = paths[c];
			int pathLen = path.length;

			// Every parent must be open for this to be visible
			boolean visible = true;
			Loop2 :
				for (int d = 0; d < (pathLen - 1); d++) {
					// Find the first row that has all the path up to current step
					for (int e = 0; e < c; e++) {
						String[] parentpath = paths[e];
						if (parentpath.length > d) {
							boolean isParent = true;
							for (int f = 0; f <= d; f++) {
								if (!parentpath[f].equals(path[f])) {
									isParent = false;
									break;
								}
							}
							if (isParent) {
								if (!opened[e]) {
									visible = false;
									break Loop2;
								}
								break;
							}
						}
					}
				}
				if (visible) {
					html.out.write("    <tr>\n"
							+ "      <td style='white-space:nowrap; border:0px;'><table cellspacing='0' cellpadding='0'><tr><td style='white-space:nowrap; border:0px;'>");

					int max = Math.min(pathLen - 1, last.length);
					int pos = 0;

					// Skip the part of the path that is already displayed by the parent
					for (; pos < max; pos++) {
						if (last[pos].equals(path[pos])) {
							boolean hasMore = false;
							for (int d = c + 1; d < treeLen; d++) {
								int end = pos;
								for (int e = 0; e < end; e++) {
									if (paths[d][e].equals(path[e])) {
										if (e == (end - 1) && !paths[d][end].equals(path[end])) {
											hasMore = true;
											break;
										}
									} else break;
								}
							}
							html.img()
								.src(req.getEncodedURL(this, URIParametersMap.of("image_num", (hasMore ? 1 : 0)), resp))
								.style("border:0px; display:inline; vertical-align:bottom")
								.width(IMAGE_WIDTH)
								.height(IMAGE_HEIGHT)
								.alt("")
								.__();
						} else break;
					}

					// Display the remaining part of the path
					for(; pos < pathLen; pos++) {
						if(path[pos].length()>0) {
							// Determine has sub items
							boolean hasSub = false;
							if (c < (treeLen - 1)) {
								String[] next_path = paths[c + 1];
								if (next_path.length >= pathLen) {
									hasSub = true;
									for (int e = 0; e < pathLen; e++) {
										String tempPath=path[e];
										if(tempPath.length()>0 && !tempPath.equals(next_path[e])) {
											hasSub = false;
											break;
										}
									}
								}
							}

							// Determine if the line continues farther down
							boolean hasMore = false;
							for (int d = c; d < treeLen; d++) {
								int end = pos;
								for (int e = 0; e < end; e++) {
									if (paths[d][e].equals(path[e])) {
										if (e == (end - 1) && !paths[d][end].equals(path[end])) {
											hasMore = true;
											break;
										}
									} else break;
								}
							}

							if(hasSub) {
								html.out
									.append("<a href='javascript:")
									.append(opened[c] ? "closeNode(" : "openNode(")
									.append(Integer.toString(c))
									.append(");'>");
								html.img()
									.alt(opened[c] ? "Close" : "Open")
									.src(
										req.getEncodedURL(
											this,
											URIParametersMap.of(
												"image_num",
												opened[c]
													? (hasMore ? 4 : (c > 0 ? 5 : 9))
													: (hasMore ? 6 : (c > 0 ? 7 : 8))
											),
											resp
										)
									).style("vertical-align:bottom; border:0px; display:inline")
									.width(IMAGE_WIDTH)
									.height(IMAGE_HEIGHT)
									.__();
								html.out.write("</a>");
							} else {
								html.img()
									.src(
										req.getEncodedURL(
											this,
											URIParametersMap.of("image_num", (hasMore ? 2 : 3)),
											resp
										)
									).style("vertical-align:bottom; border:0px; display:inline")
									.width(IMAGE_WIDTH)
									.height(IMAGE_HEIGHT)
									.alt("")
									.__();
							}
							html.img()
								.src(req.getEncodedURL(this, URIParametersMap.of("image_num", 0), resp))
								.style("vertical-align:bottom; border:0px; display:inline")
								.width(4)
								.height(IMAGE_HEIGHT)
								.alt("")
								.__();
							html.out.write("</td><td style='white-space:nowrap'>");
						}

						boolean useCodeFont=useCodeFont(req);
						if (useCodeFont) html.out.write("<code>");
						String href;
						if(
							(
								(pathLen>=2 && pos==(pathLen-2) && path[pathLen-1].length()==0)
								|| (pos==(pathLen-1) && path[pathLen-1].length()>0)
							) && (href = tree.get(c).getUrl()) != null
						) {
							html.out.write("<a class='aoLightLink' href='");
							encodeTextInXhtmlAttribute(
								resp.encodeURL(
									URIEncoder.encodeURI(
										req.getContextPath() + href
									)
								),
								html.out
							);
							html.out.write("'>"); html.text(path[pos]); html.out.write("</a>");
						} else if(!path[pos].isEmpty()) {
							html.text(path[pos]);
						}
						if(useCodeFont) html.out.write("</code>");
					}

					html.out.write("</td></tr></table></td>\n"
							+ "      <td style='white-space:nowrap; width:20px'>");
					html.img()
						.src(req.getEncodedURL(this, URIParametersMap.of("image_num", 0), resp))
						.style("vertical-align:bottom; border:0px; display:inline")
						.width(20)
						.height(1)
						.alt("")
						.__();
					html.out.write("</td>\n"
							+ "      <td style='white-space:nowrap'>");
					html.text(tree.get(c).getDescription()).out.write("</td>\n"
							+ "    </tr>\n");
					last = path;
				}
		}

		html.out.write("  </table>\n"
				+ "</div></form>\n");
		layout.endContentLine(html, req, resp, 1, false);
		layout.endContent(this, html, req, resp, 1);
	}

	protected boolean useCodeFont(WebSiteRequest req) {
		return false;
	}

	private static String[][] getLines(List<? extends TreePageData> tree) {
		int treeLen = tree.size();
		String[][] paths = new String[treeLen][];
		for (int c = 0; c < treeLen; c++) {
			TreePageData data = tree.get(c);
			String[] path = data.getPath();
			if(data.hasChildren()) {
				path = Arrays.copyOf(path, path.length + 1);
				path[path.length - 1] = "";
			}
			paths[c] = path;
		}
		return paths;
	}

	private boolean displayText(WebSiteRequest req) throws IOException, SQLException {
		// A search being performed
		if(req==null) return true;

		WebPageLayout layout=getWebPageLayout(req);
		if(layout instanceof TextOnlyLayout) return true;

		String mode=req.getParameter(TREEPAGE_MODE);

		// Auto mode
		if(mode==null || MODE_AUTO.equals(mode)) return req.isLynx();

		// Text mode or default to gui mode
		return MODE_TEXT.equals(mode);
	}

	abstract public boolean useSmoothOutline(WebSiteRequest req);
}
