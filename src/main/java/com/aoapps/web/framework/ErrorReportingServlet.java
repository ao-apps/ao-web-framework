/*
 * ao-web-framework - Legacy servlet-based web framework, superfast and capable but tedious to use.
 * Copyright (C) 2000-2009, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.lang.Throwables;
import com.aoapps.lang.exception.WrappedException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Any error occurring during servlet execution is reported to <code>System.err</code>.
 * Also keeps track of hit statistics for doGet, doPost, and getLastModified methods.
 *
 * @author  AO Industries, Inc.
 */
// TODO: Get rid of this.  Handle statistics in a request listener
//       And handle errors the propery way
public abstract class ErrorReportingServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(ErrorReportingServlet.class.getName());

	private static final long serialVersionUID = 1L;

	protected ErrorReportingServlet() {
	}

	/**
	 * Any error that occurs during a <code>doGet</code> is caught and reported here.
	 */
	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	final protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			reportingDoGet(req, resp);
		} catch (ThreadDeath td) {
			throw td;
		} catch (Throwable t) {
			logger.log(Level.SEVERE, null, t);
			if(t instanceof IOException) throw (IOException)t;
			throw Throwables.wrap(t, ServletException.class, ServletException::new);
		}
	}

	/**
	 * Any error that occurs during a <code>doPost</code> is caught and reported here.
	 */
	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	final protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			reportingDoPost(req, resp);
		} catch (ThreadDeath td) {
			throw td;
		} catch (Throwable t) {
			logger.log(Level.SEVERE, null, t);
			if(t instanceof IOException) throw (IOException)t;
			throw Throwables.wrap(t, ServletException.class, ServletException::new);
		}
	}

	private static final String RESPONSE_REQUEST_ATTRIBUTE = ErrorReportingServlet.class.getName() + ".resp";

	/**
	 * Stores the current response in a request attribute named {@link #RESPONSE_REQUEST_ATTRIBUTE}.
	 * This is used by {@link #reportingGetLastModified(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Store the current response in the request, so can be used for getLastModified
		Object oldResp = req.getAttribute(RESPONSE_REQUEST_ATTRIBUTE);
		try {
			req.setAttribute(RESPONSE_REQUEST_ATTRIBUTE, resp);
			super.service(req, resp);
		} finally {
			req.setAttribute(RESPONSE_REQUEST_ATTRIBUTE, oldResp);
		}
	}

	/**
	 * Any error that occurs during a <code>getLastModified</code> call is caught here.
	 */
	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	final protected long getLastModified(HttpServletRequest req) {
		try {
			HttpServletResponse resp = (HttpServletResponse)req.getAttribute(RESPONSE_REQUEST_ATTRIBUTE);
			if(resp == null) throw new IllegalStateException("HttpServletResponse not found on the request: " + RESPONSE_REQUEST_ATTRIBUTE);
			return reportingGetLastModified(req, resp);
		} catch (ThreadDeath td) {
			throw td;
		} catch (Throwable t) {
			logger.log(Level.SEVERE, null, t);
			throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
		}
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest,HttpServletResponse)
	 */
	protected void reportingDoGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest,HttpServletResponse)
	 */
	protected void reportingDoPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doPost(req, resp);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#getLastModified(HttpServletRequest)
	 */
	protected long reportingGetLastModified(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		return super.getLastModified(req);
	}
}
