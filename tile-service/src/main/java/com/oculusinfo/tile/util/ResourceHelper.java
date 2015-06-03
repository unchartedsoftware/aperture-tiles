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
package com.oculusinfo.tile.util;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for operating on resources (files)
 *
 * @author rharper
 *
 */
public class ResourceHelper {

	static final Logger logger = LoggerFactory.getLogger(ResourceHelper.class);

	/**
	 * Load a resource give a path and a default (if the path is empty).  The path should
	 * be in the form res:// if it's a classpath resource, otherwise should be a filename.
	 * @param path
	 * @param defaultPath
	 * @return an inputstream or null if failure
	 * @throws IOException
	 */
	static public InputStream getStreamForPath( String path, String defaultPath ) throws IOException {
		if( path == null || path.isEmpty() ) {
			// If not specified use a JAR-local resource
			path = defaultPath;
		}

		// Load properties
		InputStream inp = null;

		if (path.startsWith("res://")) {
			// A resource on the classpath
			path = path.substring(6);
			logger.info("Loading resource from: " + path);
			inp = ResourceHelper.class.getResourceAsStream(path);
		} else {
			// A file path
			try {
				File file = new File(path);
				logger.info("Loading file from: " + file.getAbsolutePath());
				inp = Files.asByteSource(file).openStream();
			}
			catch (IOException e){
				logger.info("Error loading file from: " + path);
				logger.info("Using default resource from: " + defaultPath);
				path = defaultPath.substring(6);
				inp = ResourceHelper.class.getResourceAsStream(path);
			}
		}
		return inp;
	}
}
