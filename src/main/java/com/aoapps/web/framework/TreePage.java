/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2014, 2015, 2016, 2019, 2020, 2021, 2022 AO Industries, Inc.
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

import com.aoapps.encoding.JavaScriptWriter;
import com.aoapps.html.any.attributes.Enum.Method;
import com.aoapps.html.servlet.CODE_c;
import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.html.servlet.PhrasingContent;
import com.aoapps.html.servlet.Union_Interactive_Phrasing;
import com.aoapps.lang.EmptyArrays;
import com.aoapps.lang.io.ContentType;
import com.aoapps.lang.io.IoUtils;
import com.aoapps.net.URIEncoder;
import com.aoapps.net.URIParametersMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * @author  AO Industries, Inc.
 */
public abstract class TreePage extends WebPage {

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
	public void doGet(WebSiteRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String s = req.getParameter("image_num");
		if(s == null) {
			super.doGet(req, resp);
		} else {
			try {
				int imageNum = Integer.parseInt(s);
				if(imageNum < 0 || imageNum > 9) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find image number "+imageNum);
				} else {
					boolean useSmooth = useSmoothOutline(req);
					resp.setContentType(imageNum == 0 ? ContentType.GIF : useSmooth ? ContentType.JPEG : ContentType.GIF);
					byte[] bytes = getImageBytes(imageNum, useSmooth);
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
	public long getSearchLastModified() throws ServletException {
		return getClassLastModified();
	}

	@Override
	public long getLastModified(WebSiteRequest req, HttpServletResponse resp) {
		if(req == null) return -1;
		String s = req.getParameter("image_num");
		return (s == null) ? -1 : getUptime();
	}

	@Override
	public <__ extends FlowContent<__>> void doGet(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		__ flow
	) throws ServletException, IOException {
		List<? extends TreePageData> tree = getTree(req, resp);
		if(displayText(req)) {
			int treeLen = tree.size();

			// Break apart each line
			String[][] paths = getLines(tree);

			// Get the widest of the lines
			int longest=0;
			for(String[] path : paths) {
				int width=0;
				for (int d=0; d<path.length; d++) {
					if(d>0) width+=3;
					width+=path[d].length();
				}
				width+=3;
				if(width>longest) longest=width;
			}
			int longest_ = longest;
			flow.pre__(pre -> {
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
								pre.text(hasMore ? "|  " : "   ");
								width += 3;
							}
							int len2 = last[pos].length();
							pre.sp(len2);
							width += len2;
						} else break;
					}
					for (; pos < pathLen; pos++) {
						String p = path[pos];
						if (pos > 0 && p.length() > 0) {
							pre.text("+--");
							width += 3;
						}
						String href;
						if (pos == (path[pathLen-1].length()==0?(pathLen-2):(pathLen-1)) && (href = tree.get(c).getUrl()) != null) {
							pre.a(
								resp.encodeURL(
									URIEncoder.encodeURI(
										req.getContextPath() + href
									)
								)
							).__(p);
						} else {
							pre.text(p);
						}
						if (
							p.length() > 0
							&& (
								pos < (pathLen - 1)
								|| tree.get(c).hasChildren()
							)
						) {
							pre.text('/');
						} else {
							pre.sp();
						}
						width += p.length() + 1;
					}
					pre.sp(longest_ - width).text(tree.get(c).getDescription()).nl();

					last = path;
				}
			});
		} else {
			handleRequest(req, resp, layout, flow, tree, -1, -1, null);
		}
	}

	// TODO: 3.0.0: Override different method that already does everything before the layout
	@Override
	public void doPost(
		WebSiteRequest req,
		HttpServletResponse resp
	) throws ServletException, IOException {
		List<? extends TreePageData> tree = getTree(req, resp);

		// Get the scroll to position
		int scrollToX = Integer.parseInt(req.getParameter("scroll_to_x"));
		int scrollToY = Integer.parseInt(req.getParameter("scroll_to_y"));

		DocumentEE document = getDocument(req, resp);
		WebPageLayout layout = getWebPageLayout(req);
		layout.doPage(
			req,
			resp,
			this,
			document,
			scrollToX >= 0 ? ("window.scrollTo(" + scrollToX + ", " + scrollToY + ");") : null,
			flow -> {
				int treeLen = tree.size();
				boolean[] opened = new boolean[treeLen];
				for (int c = 0; c < treeLen; c++) {
					opened[c] = Boolean.parseBoolean(req.getParameter("opened_" + c));
				}

				// Print the new table
				handleRequest(req, resp, layout, document, tree, scrollToX, scrollToY, opened);
			}
		);
	}

	/**
	 * Gets the tree to be displayed.  Each row consists of three elements: path, href, description
	 */
	protected abstract List<? extends TreePageData> getTree(WebSiteRequest req, HttpServletResponse resp) throws ServletException;

	/**
	 * Handles the interactive form of this page.
	 */
	private <__ extends FlowContent<__>> void handleRequest(
		WebSiteRequest req,
		HttpServletResponse resp,
		WebPageLayout layout,
		__ flow,
		List<? extends TreePageData> tree,
		int scrollToX,
		int scrollToY,
		boolean[] opened
	) throws ServletException, IOException {
		// Get the tree data
		int treeLen = tree.size();

		// Break apart each line
		String[][] paths = getLines(tree);

		if (opened == null) {
			// Default to opened for first item
			opened = new boolean[treeLen];
			if (treeLen > 0) opened[0] = true;
		}
		boolean[] opened_ = opened;

		layout.content(req, resp, this, flow, content -> {
			layout.contentTitle(req, resp, this, content);
			layout.contentHorizontalDivider(req, resp, content);
			layout.contentLine(req, resp, content, contentLine -> {
				// Write the javascript that controls the form
				try (JavaScriptWriter script = contentLine.script()._c()) {
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
						+ "  }");
				}

				// Write the form containing the current settings
				contentLine.form("").id("tree_form").method(Method.Value.POST).__(form -> form
					.div__(div -> {
						req.printFormFields(div);
						div.input().hidden().name("scroll_to_x").value(scrollToX).__().autoNl()
						.input().hidden().name("scroll_to_y").value(scrollToY).__().autoNl();
						for(int c=0; c<treeLen; c++) {
							div.input().hidden().name("opened_" + c).value(opened_[c]).__().autoNl();
						}

						// Display the tree in a table with links for opening/closing the different parts
						div.table().clazz("ao-packed").__(table -> {
							String[] last = EmptyArrays.EMPTY_STRING_ARRAY;
							for (int c = 0; c < treeLen; c++) {
								int c_ = c;
								String[] path = paths[c];
								final int pathLen = path.length;

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
												if (!opened_[e]) {
													visible = false;
													break Loop2;
												}
												break;
											}
										}
									}
								}
								if (visible) {
									String[] last_ = last;
									table.tbody__(tbody -> tbody
										.tr__(tr -> tr
											.td().style("white-space:nowrap", "border:0px").__(td -> td
												.table().clazz("ao-packed").__(table2 -> table2
													.tbody__(tbody2 -> tbody2
														.tr__(tr2 -> {
															var td2 = tr2.td().style("white-space:nowrap", "border:0px")._c();
															int max = Math.min(pathLen - 1, last_.length);
															int pos = 0;

															// Skip the part of the path that is already displayed by the parent
															for (; pos < max; pos++) {
																if (last_[pos].equals(path[pos])) {
																	boolean hasMore = false;
																	for (int d = c_ + 1; d < treeLen; d++) {
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
																	td2.img()
																		.src(req.getEncodedURL(this, URIParametersMap.of("image_num", (hasMore ? 1 : 0)), resp))
																		.style("border:0px", "display:inline", "vertical-align:bottom")
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
																	if (c_ < (treeLen - 1)) {
																		String[] next_path = paths[c_ + 1];
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
																	final boolean hasMore;
																	{
																		boolean found = false;
																		for (int d = c_; d < treeLen; d++) {
																			int end = pos;
																			for (int e = 0; e < end; e++) {
																				if (paths[d][e].equals(path[e])) {
																					if (e == (end - 1) && !paths[d][end].equals(path[end])) {
																						found = true;
																						break;
																					}
																				} else break;
																			}
																		}
																		hasMore = found;
																	}

																	if(hasSub) {
																		td2.a().href(
																			"javascript:"
																			+ (opened_[c_] ? "closeNode(" : "openNode(")
																			+ c_
																			+ ");"
																		).__((Union_Interactive_Phrasing<?> a) -> a
																			.img()
																				.alt(opened_[c_] ? "Close" : "Open")
																				.src(
																					req.getEncodedURL(
																						this,
																						URIParametersMap.of(
																							"image_num",
																							opened_[c_]
																								? (hasMore ? 4 : (c_ > 0 ? 5 : 9))
																								: (hasMore ? 6 : (c_ > 0 ? 7 : 8))
																						),
																						resp
																					)
																				).style("vertical-align:bottom", "border:0px", "display:inline")
																				.width(IMAGE_WIDTH)
																				.height(IMAGE_HEIGHT)
																			.__()
																		);
																	} else {
																		td2.img()
																			.src(
																				req.getEncodedURL(
																					this,
																					URIParametersMap.of("image_num", (hasMore ? 2 : 3)),
																					resp
																				)
																			).style("vertical-align", "bottom; border:0px", "display:inline")
																			.width(IMAGE_WIDTH)
																			.height(IMAGE_HEIGHT)
																			.alt("")
																		.__();
																	}
																	td2.img()
																		.src(req.getEncodedURL(this, URIParametersMap.of("image_num", 0), resp))
																		.style("vertical-align:bottom", "border:0px", "display:inline")
																		.width(4)
																		.height(IMAGE_HEIGHT)
																		.alt("")
																	.__();
																	// Close and open new TD
																	td2 = td2.__().td().style("white-space:nowrap")._c();
																}

																boolean useCodeFont=useCodeFont(req);
																CODE_c<?> code;
																PhrasingContent<?> phrasing;
																if (useCodeFont) {
																	code = td2.code_c();
																	phrasing = code;
																} else {
																	code = null;
																	phrasing = td2;
																}
																String href;
																if(
																	(
																		(pathLen>=2 && pos==(pathLen-2) && path[pathLen-1].length()==0)
																		|| (pos==(pathLen-1) && path[pathLen-1].length()>0)
																	) && (href = tree.get(c_).getUrl()) != null
																) {
																	phrasing.a().clazz("aoLightLink").href(
																		resp.encodeURL(
																			URIEncoder.encodeURI(
																				req.getContextPath() + href
																			)
																		)
																	).__(path[pos]);
																} else if(!path[pos].isEmpty()) {
																	phrasing.text(path[pos]);
																}
																if(code != null) code.__();
															}
															td2.__();
														})
													)
												)
											)
											.td().style("white-space:nowrap", "width:20px").__(td -> td
												.img()
													.src(req.getEncodedURL(this, URIParametersMap.of("image_num", 0), resp))
													.style("vertical-align:bottom", "border:0px", "display:inline")
													.width(20)
													.height(1)
													.alt("")
													.__()
											)
											.td().style("white-space:nowrap").__(tree.get(c_).getDescription())
										)
									);
									last = path;
								}
							}
						});
					})
				);
			});
		});
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

	private boolean displayText(WebSiteRequest req) throws ServletException {
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

	public abstract boolean useSmoothOutline(WebSiteRequest req);
}
