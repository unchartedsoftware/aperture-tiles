/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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
import com.oculusinfo.factory.SharedInstanceFactory;
import com.oculusinfo.factory.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class HBaseSlicedPyramidIOFactory extends SharedInstanceFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HBasePyramidIOFactory.class);


	public HBaseSlicedPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("hbase-sliced", PyramidIO.class, parent, path);

		addProperty(HBasePyramidIOFactory.HBASE_ZOOKEEPER_QUORUM);
		addProperty(HBasePyramidIOFactory.HBASE_ZOKEEPER_PORT);
		addProperty(HBasePyramidIOFactory.HBASE_MASTER);
	}

	@Override
	protected PyramidIO createInstance () throws ConfigurationException {
		try {
			String quorum = getPropertyValue(HBasePyramidIOFactory.HBASE_ZOOKEEPER_QUORUM);
			String port = getPropertyValue(HBasePyramidIOFactory.HBASE_ZOKEEPER_PORT);
			String master = getPropertyValue(HBasePyramidIOFactory.HBASE_MASTER);
			return new HBaseSlicedPyramidIO(quorum, port, master);
		} catch (IOException e) {
			throw new ConfigurationException("Error creating HBase sliced pyramid IO", e);
		}
	}
}

