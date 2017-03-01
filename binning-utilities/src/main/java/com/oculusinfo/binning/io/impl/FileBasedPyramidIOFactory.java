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
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Extends a ConfigurableFactory for file system based (zip, resource, or directory) PyramidIO types.
 * 	This class will be injected with a PyramidSource object that will provide the necesarry tile access depending
 *  on the particular type of file system tile used.
 *
 */
public class FileBasedPyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedPyramidIOFactory.class);

	public static final String NAME = "file";

	public static StringProperty ROOT_PATH              = new StringProperty("root.path",
		   "Indicates the root path of the tile pyramid - the full path to an archive file(zip), directory, or resource location."
		   + "  There is no default for this property.",
		   null);
	public static StringProperty EXTENSION              = new StringProperty("extension",
		   "The file extension which the serializer should expect to find on individual tiles.",
		   "avro");

	public FileBasedPyramidIOFactory (ConfigurableFactory<?> parent, List<String> path) {
		super(NAME, PyramidIO.class, parent, path);

		addProperty(ROOT_PATH);
		addProperty(EXTENSION);
	}


	@Override
	protected PyramidIO create() throws ConfigurationException {

		String prop = getPropertyValue(ROOT_PATH);
		if (prop == null) {
			LOGGER.error(this.getExplicitConfiguration().toString());
		}

		String rootpath = getPropertyValue(ROOT_PATH).trim();
		String extension = getPropertyValue(EXTENSION);
		PyramidSource source = null;

		if (rootpath.endsWith(".zip")) {
			// currently only handle zip, can expand to others (tar, rar, etc...)
			// We need a cache of zip sources - they are slow to read.
			source = ZipResourcePyramidSource.getZipSource(rootpath, extension);
		} else if (rootpath.startsWith("file://")) {
			// a file/directory on the file system
			rootpath = rootpath.substring(7);
			source = new FileSystemPyramidSource(rootpath, extension);
		} else if (rootpath.startsWith("res://")) {
			// a file/directory within the webapp resources
			rootpath = rootpath.substring(6);
			source = new ResourcePyramidSource(rootpath, extension);
		} else if (rootpath.startsWith("http://")) {
			// a file/directory within the webapp resources
			rootpath = rootpath.substring(7);
			source = new UrlPyramidSource(rootpath, extension);
		} else {
			// no prefix / postfix supplied, default to file for legacy support
			source = new FileSystemPyramidSource(rootpath, extension);
		}
		return new FileBasedPyramidIO(source);
	}
}
