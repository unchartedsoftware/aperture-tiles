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
package com.oculusinfo.binning.io;

import com.oculusinfo.binning.io.impl.SQLitePyramidIO;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class SQLitePyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLitePyramidIOFactory.class);

	
	public static StringProperty ROOT_PATH              = new StringProperty("root.path",
			"Unused with type=\"hbase\".  Indicates the root path of the tile pyramid - either a directory (if \"file-system\"), a package name (if \"resource\"), the full path to a .zip file (if \"zip\"), the database path (if \"sqlite\"), or the URL of the database (if \"jdbc\").  There is no default for this property.",
			null);
	
	public SQLitePyramidIOFactory(String factoryName, ConfigurableFactory<?> parent, List<String> path) {
		super(factoryName, PyramidIO.class, parent, path);
		
		addProperty(ROOT_PATH);
	}

	@Override
	protected PyramidIO create() {
		try {
			String rootPath = getPropertyValue(ROOT_PATH);
			return new SQLitePyramidIO(rootPath);
		}
		catch (Exception e) {
			LOGGER.error("Error trying to create SQLitePyramidIO", e);
		}
		return null;
	}
	

}
