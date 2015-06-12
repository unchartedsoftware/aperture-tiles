/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.servlet;

import com.google.common.io.Closeables;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.oculusinfo.tile.util.ResourceHelper;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.web.AlreadyCommittedException;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.filter.FilterNonReentrantException;
import net.sf.ehcache.constructs.web.filter.SimplePageCachingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * Set up caching
 *
 * @author rharper
 *
 */
public class CacheConfigModule extends ServletModule {

	/**
	 * A trivial extension to the ehcache-web that ensures only GET method
	 * requests are cached, all others are passed through.
	 */
	@Singleton
	private static class CustomPageCachingFilter extends SimplePageCachingFilter {
		@Override
		protected void doFilter(HttpServletRequest request,
		                        HttpServletResponse response, FilterChain chain)
			throws AlreadyGzippedException, AlreadyCommittedException,
			       FilterNonReentrantException, LockTimeoutException, Exception {

			if( "GET".equals(request.getMethod()) ) {
				// Go through cache
				if (response.isCommitted()) {
					throw new AlreadyCommittedException(
					                                    "Response already committed before doing buildPage.");
				}
				logRequestHeaders(request);
				PageInfo pageInfo = buildPageInfo(request, response, chain);

				// Always write the response. This is necessary if
				// we want the ajax error handler to be able to
				// display exception messages from the server.
				writeResponse(request, response, pageInfo);
			} else {
				// Don't go through cache
				chain.doFilter(request, response);
			}
		}
	};




	final Logger logger = LoggerFactory.getLogger(getClass());

	private final ServletContext context;


	public CacheConfigModule(ServletContext context) {
		this.context = context;
	}


	@Override
	protected void configureServlets() {

		// Read the ehCache property from web.xml or override-web.xml
		String filename = context.getInitParameter("ehcacheConfig");

		try {
			logger.info("Loading ehcache configuration:");

			// Load properties
			InputStream inp = ResourceHelper.getStreamForPath(filename, "res:///ehcache.xml");

			// Create cache manager with provided configuration
			CacheManager.create(inp);

			Closeables.close(inp, false);
		} catch (IOException e) {
			// Failed to load properties, error
			addError(e);
		}


		// Filter all REST calls
		filter("/rest/*").through(CustomPageCachingFilter.class);

	}

}
