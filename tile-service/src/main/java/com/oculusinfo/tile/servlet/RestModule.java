/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 * <p/>
 * Released under the MIT License.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oculusinfo.tile.util.ResourceHelper;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.web.AlreadyCommittedException;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.ShutdownListener;
import net.sf.ehcache.constructs.web.filter.SimplePageCachingFilter;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;

/**
 * This module sets up all REST related infrastructure, including:
 * <ul>
 * <li>Binding the /rest/* endpoint to the RestServlet</li>
 * <li>Binding a Restlet Application object that is configured with all ServerResources
 * mapped to paths in other modules</li>
 * </ul>
 *
 * @author rharper
 */
public class RestModule extends ServletModule implements ServletContextListener {

	private static final String REST_BASE = "/rest";

	/**
	 * This is an unfortunate business but seems to the only way to prevent ehcache from
	 * caching pages that have already explicitly been marked with a no cache header. If
	 * it shouldn't be cached on the client it shouldn't be cached here.
	 *
	 * @author djonker
	 */
	private static class CustomPageInfo extends PageInfo {

		private static final long serialVersionUID = 1L;

		private boolean cacheable;

		/**
		 * @param statusCode
		 * @param contentType
		 * @param cookies
		 * @param body
		 * @param storeGzipped
		 * @param timeToLiveSeconds
		 * @param headers
		 * @param cacheable
		 * @throws AlreadyGzippedException
		 */
		public CustomPageInfo( int statusCode, String contentType,
							   Collection cookies, byte[] body, boolean storeGzipped,
							   long timeToLiveSeconds, Collection<Header<? extends Serializable>> headers, boolean cacheable )
			throws AlreadyGzippedException {
			super( statusCode, contentType, cookies, body, storeGzipped, timeToLiveSeconds, headers );

			this.cacheable = cacheable;
		}

		/* (non-Javadoc)
		 * ehCache source code tells us that this is the only post-request criteria ehCache considers when
		 * deciding whether to cache a response, and that it's not used to indicate anything else.
		 *
		 * @see net.sf.ehcache.constructs.web.PageInfo#isOk()
		 */
		@Override
		public boolean isOk() {
			return cacheable && super.isOk();
		}

	}

	/**
	 * A trivial extension to the ehcache-web that ensures only GET method
	 * requests are cached, all others are passed through.
	 */
	private static class CustomPageCachingFilter extends SimplePageCachingFilter {

		@Override
		protected void doFilter( HttpServletRequest request,
								 HttpServletResponse response,
								 FilterChain chain ) throws Exception {

			if ( "GET".equals( request.getMethod() ) ) {

				// Go through cache
				if ( response.isCommitted() ) {
					throw new AlreadyCommittedException(
						"Response already committed before doing buildPage." );
				}

				logRequestHeaders( request );
				PageInfo pageInfo = buildPageInfo( request, response, chain );

				// Always write the response. This is necessary if
				// we want the ajax error handler to be able to
				// display exception messages from the server.
				writeResponse( request, response, pageInfo );
			} else {
				// Don't go through cache
				chain.doFilter( request, response );
			}
		}

		/* (non-Javadoc)
		 * @see net.sf.ehcache.constructs.web.filter.CachingFilter#buildPage(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
		 */
		@Override
		protected PageInfo buildPage( HttpServletRequest request,
									  HttpServletResponse response,
									  FilterChain chain ) throws Exception {

			// Invoke the next entity in the chain
			final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
			final GenericResponseWrapper wrapper = new GenericResponseWrapper( response, outstr );
			chain.doFilter( request, wrapper );
			wrapper.flush();

			final long timeToLiveSeconds = blockingCache.getCacheConfiguration()
				.getTimeToLiveSeconds();

			boolean cacheable = true;

			for ( Header<? extends Serializable> header : wrapper.getAllHeaders() ) {
				if ( HeaderConstants.HEADER_CACHE_CONTROL.equals( header.getName() ) &&
					HeaderConstants.CACHE_NO_CACHE.equals( header.getValue() ) ) {
					cacheable = false;
				}
			}

			// Return the page info
			return new CustomPageInfo( wrapper.getStatus(), wrapper.getContentType(),
				wrapper.getCookies(), outstr.toByteArray(), true,
				timeToLiveSeconds, wrapper.getAllHeaders(),
				cacheable );
		}
	}

	final Logger logger = LoggerFactory.getLogger( getClass() );

	private final ServletContext context;

	private CustomPageCachingFilter filter;

	public RestModule( ServletContext context ) {
		this.context = context;
	}

	@Override
	protected void configureServlets() {

		/*
		 * Servlets
		 */

		// Handle all RPC requests with a servlet bound to the RPCServlet Name
		logger.debug( "Setting REST base path to '" + REST_BASE + "/*'" );
		serve( REST_BASE + "/*" ).with( RestletServlet.class );


		// Read the ehCache property from web.xml or override-web.xml
		String filename = context.getInitParameter( "ehcacheConfig" );

		try {
			logger.info( "Loading ehcache configuration..." );

			if ( System.getProperty( "ehcache.disk.store.dir" ) == null ) {
				System.setProperty( "ehcache.disk.store.dir", Files.createTempDir().getPath() );
			}

			// Load properties
			InputStream inp = ResourceHelper.getStreamForPath(filename, "res:///ehcache.xml");

			// Create cache manager with provided configuration
			CacheManager.create( inp );

			try {
				inp.close();
			} catch ( IOException ioe ) {
				logger.warn( "Failed to close ehcache configuration input stream.", ioe );
			}

		} catch ( IOException e ) {
			// Failed to load properties, error
			addError( e );
		}

		filter = new CustomPageCachingFilter();

		// Filter all REST calls
		filter( REST_BASE + "/*" ).through( filter );
	}

	/**
	 * Creates the restlet application that will be used to handle all rest calls
	 * Takes a map of paths to resource classes
	 * <p/>
	 * Use the following three lines to access the routing multibinder:
	 * TypeLiteral<String> pathType = new TypeLiteral<String>() {};
	 * TypeLiteral<Class<? extends ServerResource>> clazzType = new TypeLiteral<Class<? extends ServerResource>>() {};
	 * MapBinder<String, Class<? extends ServerResource>> resourceBinder = MapBinder.newMapBinder(binder(), pathType, clazzType);
	 * resourceBinder.bind("/my/path").toInstance(MyResource.class);
	 */
	@Provides
	Application createApplication( FinderFactory factory, Map<String, Class<? extends ServerResource>> routes ) {
		Context context = new Context();
		Application application = new Application();
		application.setContext( context );
		Router router = new Router( context );
		// Set binding rules here
		for ( Entry<String, Class<? extends ServerResource>> entry : routes.entrySet() ) {
			final Class<? extends ServerResource> resource = entry.getValue();
			logger.info( "Binding '" + entry.getKey() + "' to " + resource );
			router.attach( entry.getKey(), factory.finder( resource ) );
		}
		application.setInboundRoot( router );
		return application;
	}

	@Override
	public void contextInitialized( ServletContextEvent sce ) {
	}

	@Override
	public void contextDestroyed( ServletContextEvent sce ) {
		final ShutdownListener listener = new ShutdownListener();
		listener.contextDestroyed( sce );
	}

}
