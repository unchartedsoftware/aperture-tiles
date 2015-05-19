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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;

import com.oculusinfo.tile.util.ResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class DefaultServerConfigModule extends AbstractModule {

	static private final Logger LOGGER = LoggerFactory.getLogger( DefaultServerConfigModule.class );

	private final ServletContext context;

	public DefaultServerConfigModule( ServletContext context ) {
		this.context = context;
	}

	@Override
	protected void configure() {

		InputStream inp = null;

		try {

			/*
			 * Load property values
			 */
			inp = ResourceHelper.getStreamForPath( "res:///tile.properties", null );

			Properties properties = new Properties();
			properties.load( inp );

			try {
				if ( inp != null ) {
					inp.close();
				}
			} catch ( IOException ioe ) {
				LOGGER.warn( "Failed to close properties input stream.", ioe );
			}

			// bind all properties to named annotations
			Names.bindProperties( this.binder(), properties );

			// Output all properties values to log
			if ( LOGGER.isDebugEnabled() ) {
				for ( Object key : properties.keySet() ) {
					LOGGER.debug( "System Setting - " + key + ": " + properties.get( key ) );
				}
			}

		} catch ( IOException e ) {
			// Failed to load properties, error
			if ( inp != null ) {
				try {
					inp.close();
				} catch ( IOException ioe ) {
					LOGGER.warn( "Failed to close properties input stream.", ioe );
				}
			}
		}

	}

}
