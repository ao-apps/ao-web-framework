/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2013, 2015, 2016, 2019, 2020, 2021, 2022, 2023  AO Industries, Inc.
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

import static com.aoapps.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;

import com.aoapps.html.servlet.ContentEE;
import com.aoapps.html.servlet.FlowContent;
import com.aoapps.lang.io.IoUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Pulls the content from a file with the same name and location as the <code>.class</code>
 * and <code>.java</code> but with a <code>.html</code> extension.  As the file is being
 * sent to the client, any <code>href="@<i>classname</i>"</code> URL is rewritten and
 * maintains the current <code>WebSiteRequest</code> parameters.
 *
 * @author  AO Industries, Inc.
 */
public abstract class HtmlInputStreamPage extends InputStreamPage {

  private static final long serialVersionUID = 1L;

  @Override
  public <__ extends FlowContent<__>> __ printStream(
      WebSiteRequest req,
      HttpServletResponse resp,
      WebPageLayout layout,
      ContentEE<?> content,
      __ contentLine,
      InputStream in
  ) throws ServletException, IOException {
    return printHtmlStream(req, resp, layout, content, contentLine, in, "aoLightLink", new AtomicReference<>());
  }

  /**
   * Gets the file that the text should be read from.
   */
  @Override
  public InputStream getInputStream() throws IOException {
    return getHtmlInputStream(getClass());
  }

  /**
   * Gets the HTML file with the same name as the provided Class or {@code null} when not found.
   */
  public static InputStream getHtmlInputStream(Class<?> clazz) {
    String resource = clazz.getName().replace('.', '/') + ".html";
    InputStream in = HtmlInputStreamPage.class.getResourceAsStream("/" + resource);
    if (in == null) {
      // Try ClassLoader for when modules enabled
      ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      in = (classloader != null)
          ? classloader.getResourceAsStream(resource)
          : ClassLoader.getSystemResourceAsStream(resource);
    }
    return in;
  }

  /**
   * Prints HTML content, parsing for special <code>@</code> tags.  Types of tags include:
   * <ul>
   *   <li>@URL(classname)    Loads a WebPage of the given class and builds a URL to it</li>
   *   <li>@BEGIN_LIGHT_AREA  Calls {@link WebPageLayout#startLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}</li>
   *   <li>@END_LIGHT_AREA    Calls {@link WebPageLayout#endLightArea(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}</li>
   *   <li>@END_CONTENT_LINE  Calls {@link WebPageLayout#endContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}</li>
   *   <li>@PRINT_CONTENT_HORIZONTAL_DIVIDER  Calls {@link WebPageLayout#contentHorizontalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)}</li>
   *   <li>@START_CONTENT_LINE  Calls {@link WebPageLayout#startContentLine(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.ContentEE)}</li>
   *   <li>@LINK_CLASS        The preferred link class for this element</li>
   * </ul>
   *
   * @return  The current {@code contentLine}, which may have been replaced by a call to
   *          {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
   *          or {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
   *
   * @see  #printHtmlStream(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPageLayout, com.aoapps.html.servlet.ContentEE, com.aoapps.html.servlet.FlowContent, java.io.InputStream, java.lang.String, java.util.concurrent.atomic.AtomicReference)
   */
  @SuppressWarnings("deprecation")
  public static <__ extends FlowContent<__>> __ printHtml(
      WebSiteRequest req,
      HttpServletResponse resp,
      WebPageLayout layout,
      ContentEE<?> content,
      __ contentLine,
      String htmlContent,
      String linkClass,
      AtomicReference<FlowContent<?>> lightAreaRef
  ) throws ServletException, IOException {
    if (req == null) {
      contentLine.unsafe(htmlContent);
    } else {
      Writer unsafe = contentLine.getRawUnsafe();
      int len = htmlContent.length();
      int pos = 0;
      while (pos < len) {
        char ch = htmlContent.charAt(pos++);
        if (ch == '@') {
          // TODO: regionsMatches would be faster than repeated substring
          if ((pos + 4) < len && "URL(".equalsIgnoreCase(htmlContent.substring(pos, pos + 4))) {
            int endPos = htmlContent.indexOf(')', pos + 4);
            if (endPos == -1) {
              throw new IllegalArgumentException("Unable to find closing parenthesis for @URL( substitution, pos=" + pos);
            }
            String className = htmlContent.substring(pos + 4, endPos);
            encodeTextInXhtmlAttribute(req.getEncodedUrlForClass(className, resp), unsafe);
            pos = endPos + 1;
          } else if ((pos + 16) < len && "BEGIN_LIGHT_AREA".equalsIgnoreCase(htmlContent.substring(pos, pos + 16))) {
            if (lightAreaRef.get() != null) {
              throw new IllegalStateException("@BEGIN_LIGHT_AREA may not be nested");
            }
            FlowContent<?> lightArea = layout.startLightArea(req, resp, contentLine);
            if (lightArea == null) {
              throw new AssertionError("lightArea == null");
            }
            lightAreaRef.set(lightArea);
            pos += 16;
          } else if ((pos + 14) < len && "END_LIGHT_AREA".equalsIgnoreCase(htmlContent.substring(pos, pos + 14))) {
            FlowContent<?> lightArea = lightAreaRef.get();
            if (lightArea == null) {
              throw new IllegalStateException("@END_LIGHT_AREA does not have matching @BEGIN_LIGHT_AREA");
            }
            layout.endLightArea(req, resp, lightArea);
            lightAreaRef.set(null);
            pos += 14;
          } else if ((pos + 16) < len && "END_CONTENT_LINE".equalsIgnoreCase(htmlContent.substring(pos, pos + 16))) {
            layout.endContentLine(req, resp, contentLine);
            pos += 16;
          } else if ((pos + 32) < len && "PRINT_CONTENT_HORIZONTAL_DIVIDER".equalsIgnoreCase(htmlContent.substring(pos, pos + 32))) {
            layout.contentHorizontalDivider(req, resp, content);
            pos += 32;
          } else if ((pos + 18) < len && "START_CONTENT_LINE".equalsIgnoreCase(htmlContent.substring(pos, pos + 18))) {
            contentLine = layout.startContentLine(req, resp, content);
            unsafe = contentLine.getRawUnsafe();
            pos += 18;
          } else if ((pos + 10) < len && "LINK_CLASS".equalsIgnoreCase(htmlContent.substring(pos, pos + 10))) {
            unsafe.write(linkClass == null ? "aoLightLink" : linkClass);
            pos += 10;
          } else {
            unsafe.write('@');
          }
        } else {
          unsafe.write(ch);
        }
      }
    }
    return contentLine;
  }

  private static final String[] tags = {
      "@PRINT_CONTENT_HORIZONTAL_DIVIDER",
      "@START_CONTENT_LINE",
      "@BEGIN_LIGHT_AREA",
      "@END_CONTENT_LINE",
      "@END_LIGHT_AREA",
      "@LINK_CLASS",
      "@URL"
  };

  /**
   * Prints HTML with template substitutions.
   *
   * @return  The current {@code contentLine}, which may have been replaced by a call to
   *          {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent)}
   *          or {@link WebPageLayout#contentVerticalDivider(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.html.servlet.FlowContent, int, int, int, java.lang.String, java.lang.String)}.
   *
   * @see  #printHtml(com.aoapps.web.framework.WebSiteRequest, javax.servlet.http.HttpServletResponse, com.aoapps.web.framework.WebPageLayout, com.aoapps.html.servlet.ContentEE, com.aoapps.html.servlet.FlowContent, java.lang.String, java.lang.String, java.util.concurrent.atomic.AtomicReference)
   */
  @SuppressWarnings("deprecation")
  public static <__ extends FlowContent<__>> __ printHtmlStream(
      WebSiteRequest req,
      HttpServletResponse resp,
      WebPageLayout layout,
      ContentEE<?> content,
      __ contentLine,
      InputStream in,
      String linkClass,
      AtomicReference<FlowContent<?>> lightAreaRef
  ) throws ServletException, IOException {
    if (in == null) {
      throw new NullPointerException("in is null");
    }
    Reader reader = new InputStreamReader(in);
    if (req == null) {
      IoUtils.copy(reader, contentLine.unsafe());
    } else {
      Writer unsafe = contentLine.getRawUnsafe();
      StringBuilder buffer = null;
      int ch;
      while ((ch = reader.read()) != -1) {
        if (ch == '@') {
          if (buffer == null) {
            buffer = new StringBuilder();
          }
          // Read until a tag is matched, or until a tag cannot be matched
          buffer.append('@');
          Loop:
          while ((ch = reader.read()) != -1) {
            // If @ found, print buffer and reset for next tag
            if (ch == '@') {
              unsafe.write(buffer.toString());
              buffer.setLength(0);
              buffer.append('@');
            } else {
              buffer.append((char) ch);
              String tagPart = buffer.toString();
              // Does one of the tags begin with or match this tag
              boolean found = false;
              for (int c = 0; c < tags.length; c++) {
                String tag = tags[c];
                if (tag.length() >= tagPart.length()) {
                  if (tags[c].equalsIgnoreCase(tagPart)) {
                    if (c == 0) {
                      layout.contentHorizontalDivider(req, resp, content);
                    } else if (c == 1) {
                      contentLine = layout.startContentLine(req, resp, content);
                      unsafe = contentLine.getRawUnsafe();
                    } else if (c == 2) {
                      if (lightAreaRef.get() != null) {
                        throw new IllegalStateException("@BEGIN_LIGHT_AREA may not be nested");
                      }
                      FlowContent<?> lightArea = layout.startLightArea(req, resp, contentLine);
                      if (lightArea == null) {
                        throw new AssertionError("lightArea == null");
                      }
                      lightAreaRef.set(lightArea);
                    } else if (c == 3) {
                      layout.endContentLine(req, resp, contentLine);
                    } else if (c == 4) {
                      FlowContent<?> lightArea = lightAreaRef.get();
                      if (lightArea == null) {
                        throw new IllegalStateException("@END_LIGHT_AREA does not have matching @BEGIN_LIGHT_AREA");
                      }
                      layout.endLightArea(req, resp, lightArea);
                      lightAreaRef.set(null);
                    } else if (c == 5) {
                      unsafe.write(linkClass == null ? "aoLightLink" : linkClass);
                    } else if (c == 6) {
                      // Read up to a ')'
                      while ((ch = reader.read()) != -1) {
                        if (ch == ')') {
                          String className = buffer.toString().substring(5, buffer.length());
                          encodeTextInXhtmlAttribute(req.getEncodedUrlForClass(className, resp), unsafe);
                          buffer.setLength(0);
                          break;
                        } else {
                          buffer.append((char) ch);
                        }
                      }
                      if (buffer.length() > 0) {
                        throw new IllegalArgumentException("Unable to find closing parenthesis for @URL( substitution, buffer=" + buffer.toString());
                      }
                    } else {
                      throw new RuntimeException("This index should not be used because it is biffer than tags.length");
                    }
                    buffer.setLength(0);
                    break Loop;
                  } else if (tags[c].toUpperCase().startsWith(tagPart.toUpperCase())) {
                    found = true;
                    break;
                  }
                } else {
                  // Sorted with longest first, can break here
                  break;
                }
              }
              if (!found) {
                unsafe.write(tagPart);
                buffer.setLength(0);
                break;
              }
            }
          }
        } else {
          unsafe.write((char) ch);
        }
      }
    }
    return contentLine;
  }
}
