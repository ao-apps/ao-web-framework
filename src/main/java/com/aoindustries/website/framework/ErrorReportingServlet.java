/*
 * aoweb-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019  AO Industries, Inc.
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

import com.aoindustries.io.IoUtils;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Any error occuring during servlet execution is reported to <code>System.err</code>.
 * Also keeps track of hit stastistics for doGet, doPost, and getLastModified methods.
 *
 * @author  AO Industries, Inc.
 */
public abstract class ErrorReportingServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * The response buffer is set to this size.
	 */
	public static final int BUFFER_SIZE = 256 * 1024;

	/**
	 * The time that the servlet environment started.
	 */
	private static final long uptime = System.currentTimeMillis();

	/**
	 * Gets the time the servlet environment was loaded.
	 */
	public static long getUptime() {
		return uptime;
	}

	/**
	 * The GET requests are counted in a thread safe way.
	 */
	private static final AtomicLong getCount = new AtomicLong();

	/**
	 * The POST requests are counted in a thread safe way.
	 */
	private static final AtomicLong postCount = new AtomicLong();

	/**
	 * The getLastModified calls are counted in a thread safe way.
	 */
	private static final AtomicLong lastModifiedCount = new AtomicLong();

	private final LoggerAccessor loggerAccessor;

	protected ErrorReportingServlet(LoggerAccessor loggerAccessor) {
		this.loggerAccessor = loggerAccessor;
	}

	/**
	 * Gets the loggerAccess for this page.
	 */
	protected LoggerAccessor getLoggerAccessor() {
		return loggerAccessor;
	}

	/**
	 * Gets the logger for this servlet.
	 */
	public Logger getLogger() {
		return getLogger(getClass());
	}

	/**
	 * Gets the logger for the provided class.
	 */
	protected Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	/**
	 * Gets the provided named logger.
	 */
	protected Logger getLogger(String name) {
		return loggerAccessor.getLogger(getServletContext(), name);
	}

	/**
	 * Any error that occurs during a <code>doGet</code> is caught and reported here.
	 */
	@Override
	final protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setBufferSize(BUFFER_SIZE);
		getCount.incrementAndGet();
		try {
			reportingDoGet(req, resp);
		} catch (ThreadDeath t) {
			throw t;
		} catch (RuntimeException | ServletException | IOException e) {
			getLogger().log(Level.SEVERE, null, e);
			throw e;
		} catch (SQLException t) {
			getLogger().log(Level.SEVERE, null, t);
			throw new ServletException(t);
		}
	}

	/**
	 * Any error that occurs during a <code>doPost</code> is caught and reported here.
	 */
	@Override
	final protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setBufferSize(BUFFER_SIZE);
		postCount.incrementAndGet();
		try {
			reportingDoPost(req, resp);
		} catch (ThreadDeath t) {
			throw t;
		} catch (RuntimeException | ServletException | IOException e) {
			getLogger().log(Level.SEVERE, null, e);
			throw e;
		} catch (SQLException t) {
			getLogger().log(Level.SEVERE, null, t);
			throw new ServletException(t);
		}
	}

	/**
	 * Gets the number of GET requests that have been placed.
	 */
	public static long getGetCount() {
		return getCount.get();
	}

	/**
	 * Any error that occurs during a <code>getLastModified</code> call is caught here.
	 */
	@Override
	final protected long getLastModified(HttpServletRequest req) {
		lastModifiedCount.incrementAndGet();
		try {
			return reportingGetLastModified(req);
		} catch (ThreadDeath err) {
			throw err;
		} catch (RuntimeException err) {
			getLogger().log(Level.SEVERE, null, err);
			throw err;
		} catch (IOException | SQLException err) {
			getLogger().log(Level.SEVERE, null, err);
			throw new RuntimeException(err);
		}
	}

	/**
	 * Gets the number of calls to <code>getLastModified</code>.
	 */
	public static long getLastModifiedCount() {
		return lastModifiedCount.get();
	}

	/**
	 * Gets the number of POST requests that have been made.
	 */
	public static long getPostCount() {
		return postCount.get();
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest,HttpServletResponse)
	 */
	protected void reportingDoGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
		super.doGet(req, resp);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest,HttpServletResponse)
	 */
	protected void reportingDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, SQLException, IOException {
		super.doPost(req, resp);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)
	 */
	protected long reportingGetLastModified(HttpServletRequest req) throws IOException, SQLException {
		return super.getLastModified(req);
	}

	/**
	 * @deprecated  Please call logger directly for accurate class and method
	 */
	@Override
	@Deprecated
	final public void log(String message) {
		getLogger().log(Level.SEVERE, message);
	}

	/**
	 * @deprecated  Please call logger directly for accurate class and method
	 */
	@Override
	@Deprecated
	final public void log(String message, Throwable err) {
		getLogger().log(Level.SEVERE, message, err);
	}

	private static final SecureRandom secureRandom = new SecureRandom();

	/**
	 * @see  WebSiteRequest#getRandom()
	 */
	static SecureRandom getSecureRandom() {
		return secureRandom;
	}

	/**
	 * A fast pseudo-random number generated seeded by secure random.
	 */
	private static final Random fastRandom = new Random(IoUtils.bufferToLong(secureRandom.generateSeed(8)));
	static Random getFastRandom() {
		return fastRandom;
	}
}
