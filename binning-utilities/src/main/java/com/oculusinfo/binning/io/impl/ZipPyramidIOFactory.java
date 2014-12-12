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
package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ZipPyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZipPyramidIOFactory.class);



	public static StringProperty ROOT_PATH              = new StringProperty("root.path",
		   "Indicates the root path of the tile pyramid - the full path to a .zip file.  There is no default for this property.",
		   null);
	public static StringProperty EXTENSION              = new StringProperty("extension",
		   "The file extension which the serializer should expect to find on individual tiles.",
		   "avro");
	
	public ZipPyramidIOFactory (ConfigurableFactory<?> parent, List<String> path) {
		super("zip", PyramidIO.class, parent, path);
		
		addProperty(ROOT_PATH);
		addProperty(EXTENSION);
	}

	// We meed a global cache of zip stream sources - zip files are very slow to
	// read, so the source is slow to initialize, so creating a new one each
	// time we run isn't feasible.
	private static Map<Pair<String, String>, ZipResourcePyramidStreamSource> _zipfileCache = new HashMap<>();
	private static ZipResourcePyramidStreamSource getZipSource (String rootpath, String extension) {
		Pair<String, String> key = new Pair<>(rootpath, extension);
		if (!_zipfileCache.containsKey(key)) {
			synchronized (_zipfileCache) {
				if (!_zipfileCache.containsKey(key)) {
					URL zipFile = PyramidIOFactory.class.getResource(rootpath);
					ZipResourcePyramidStreamSource source = new ZipResourcePyramidStreamSource(zipFile.getFile(), extension);
					_zipfileCache.put(key, source);
				}
			}
		}
		return _zipfileCache.get(key);
	}

	@Override
	protected PyramidIO create() {
		try {
			// We need a cache of zip sources - they are slow to read.
			PyramidStreamSource source = getZipSource(getPropertyValue(ROOT_PATH), getPropertyValue(EXTENSION));
			return new ResourceStreamReadOnlyPyramidIO(source);
		}
		catch (Exception e) {
			LOGGER.error("Error trying to create ZipPyramidIO", e);
		}
		return null;
	}
}
