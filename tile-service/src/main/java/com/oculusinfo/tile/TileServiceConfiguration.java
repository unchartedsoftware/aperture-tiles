/**
 * Copyright (c) 2013 Oculus Info Inc. http://www.oculusinfo.com/
 * <p/>
 * Released under the MIT License.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile;

import com.google.inject.Singleton;
import com.oculusinfo.tile.servlet.DefaultServerConfigModule;
import com.oculusinfo.tile.servlet.RestModule;
import com.oculusinfo.tile.servlet.ServletLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.Constructor;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;

@Singleton
public class TileServiceConfiguration extends GuiceServletContextListener {

	static private final Logger LOGGER = LoggerFactory.getLogger( TileServiceConfiguration.class );

	static private final String MODULES_ATTRIBUTE = "guice-modules";

	static private List<ServletLifecycleListener> _lifecycleListeners = new ArrayList<>();

	private List<Module> _modules;

	public void addLifecycleListener( ServletLifecycleListener listener ) {
		_lifecycleListeners.add( listener );
	}

	public void removeLifecycleListener( ServletLifecycleListener listener ) {
		_lifecycleListeners.remove( listener );
	}

	@Override
	public void contextInitialized( ServletContextEvent servletContextEvent ) {

		// Notify any listeners
		if ( null != _lifecycleListeners ) {
			for ( ServletLifecycleListener listener : _lifecycleListeners ) {
				listener.onServletInitialized( servletContextEvent );
			}
		}

		ServletContext context = servletContextEvent.getServletContext();

		/*
		 * Extract modules class names to add from web.xml
		 */
		String moduleNames = context.getInitParameter( MODULES_ATTRIBUTE );
		_modules = Lists.newLinkedList();

		// these are standard and in our core.
		LOGGER.info( "Adding Core Aperture Guice Modules: "
				+ DefaultServerConfigModule.class.getName() + ", "
				+ RestModule.class.getName()
		);

		_modules.add( new DefaultServerConfigModule( context ) );
		_modules.add( new RestModule( context ) );

		if ( moduleNames != null ) {
			for ( String moduleName : Splitter.on( ':' ).split( moduleNames ) ) {
				try {
					moduleName = moduleName.trim();
					if ( moduleName.length() > 0 ) {
						// Create instance of module, add to list
						Class<?> moduleClass = Class.forName( moduleName );

						Module module = null;

						// First try constructor that takes a ServletContext
						try {
							Constructor<?> cons = moduleClass.getConstructor( ServletContext.class );
							// Have a specialized constructor, invoke
							module = ( Module ) cons.newInstance( context );
						} catch ( Exception e ) {
							// Noop - silent fail on this constructor
						}

						// Second, try no-arg constructor
						if ( module == null ) {
							// No specialized constructor, use no-arg
							module = ( Module ) moduleClass.newInstance();
						}

						if ( module != null ) {
							LOGGER.info( "Adding Guice Module: " + moduleClass.getName() );
							_modules.add( module );

							if ( module instanceof ServletContextListener ) {
								try {
									( ( ServletContextListener ) module ).contextInitialized( servletContextEvent );
								} catch ( Exception e ) {
									LOGGER.error( "Exception caught while notifying module of servlet context destruction", e );
								}
							}

						} else {
							// Cannot load specified module,
							throw new RuntimeException( "No valid constructor found for module class: " + moduleClass.getName() );
						}
					}
				} catch ( Exception e ) {
					throw new RuntimeException( e );
				}
			}
		}

		super.contextInitialized( servletContextEvent );
	}


	/* (non-Javadoc)
	 * @see com.google.inject.servlet.GuiceServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed( ServletContextEvent servletContextEvent ) {

		// Notify any listeners
		if ( null != _lifecycleListeners ) {
			for ( ServletLifecycleListener listener : _lifecycleListeners ) {
				listener.onServletDestroyed( servletContextEvent );
			}
		}

		for ( Module module : _modules ) {
			if ( module instanceof ServletContextListener ) {
				try {
					( ( ServletContextListener ) module ).contextDestroyed( servletContextEvent );
				} catch ( Exception e ) {
					LOGGER.error( "Exception caught while notifying module of servlet context destruction", e );
				}
			}
		}

		super.contextDestroyed( servletContextEvent );
	}

	@Override
	protected Injector getInjector() {
		// Create injector
		return Guice.createInjector( _modules );
	}

}
