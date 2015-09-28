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

import java.sql.SQLException;
import java.util.List;


public class JDBCPyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JDBCPyramidIOFactory.class);



	public static StringProperty ROOT_PATH              = new StringProperty("root.path",
		   "Indicates the root path of the tile pyramid - the URL of the database.  There is no default for this property.",
		   null);
	public static StringProperty JDBC_DRIVER            = new StringProperty("jdbc.driver",
		   "The full class name of the JDBC driver to use.  There is no default for this property.",
		   null);
	
	public JDBCPyramidIOFactory (ConfigurableFactory<?> parent, List<String> path) {
		super("jdbc", PyramidIO.class, parent, path);
		
		addProperty(ROOT_PATH);
		addProperty(JDBC_DRIVER);
	}

	@Override
	protected PyramidIO create() throws ConfigurationException {
		try {
			String driver = getPropertyValue(JDBC_DRIVER);
			String rootPath = getPropertyValue(ROOT_PATH);
			return new JDBCPyramidIO(driver, rootPath);
		} catch (ClassNotFoundException | SQLException e) {
			throw new ConfigurationException("Error creating JDBCPyramidIO", e);
		}
	}
}
