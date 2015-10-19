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


public class HBasePyramidIOFactory extends SharedInstanceFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HBasePyramidIOFactory.class);

	
	public static StringProperty HBASE_ZOOKEEPER_QUORUM = new StringProperty("hbase.zookeeper.quorum",
		   "Only used if type=\"hbase\".  An HBase configuration parameter, this should match the similar value in hbase-site.xml.  There is no default for this property.",
		   null);
	public static StringProperty HBASE_ZOKEEPER_PORT = new StringProperty("hbase.zookeeper.port",
		   "Only used if type=\"hbase\".  An HBase configuration parameter, this should match the similar value in hbase-site.xml.",
		   "2181");
	public static StringProperty HBASE_MASTER = new StringProperty("hbase.master",
		   "Only used if type=\"hbase\".  An HBase configuration parameter, this should match the similar value in hbase-site.xml.  There is no default for this property.",
		   null);
	
	public HBasePyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("hbase", PyramidIO.class, parent, path);
		
		addProperty(HBASE_ZOOKEEPER_QUORUM);
		addProperty(HBASE_ZOKEEPER_PORT);
		addProperty(HBASE_MASTER);
	}

	@Override
	protected PyramidIO createInstance () throws ConfigurationException {
		try {
			String quorum = getPropertyValue(HBASE_ZOOKEEPER_QUORUM);
			String port = getPropertyValue(HBASE_ZOKEEPER_PORT);
			String master = getPropertyValue(HBASE_MASTER);
			return new HBasePyramidIO(quorum, port, master);
		} catch (IOException e) {
			throw new ConfigurationException("Exception creating HBase pyramid IO", e);
		}
	}
}
