/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.JSONProperty;
import com.oculusinfo.factory.properties.StringProperty;



/**
 * Factory class to create the standard types of PyramidIOs
 * 
 * @author nkronenfeld
 */
public class PyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PyramidIOFactory.class);



	public static StringProperty PYRAMID_IO_TYPE        = new StringProperty("type",
		   "The location to and from which to read tile pyramids",
		   null,
		   null);
	public static JSONProperty   INITIALIZATION_DATA    = new JSONProperty("data",
		 "Data to be passed to the PyramidIO for read initialization",
		 null);

	public PyramidIOFactory (ConfigurableFactory<?> parent, List<String> path, List<ConfigurableFactory<?>> children) {
		this(null, parent, path, children);
	}

	public PyramidIOFactory (String name, ConfigurableFactory<?> parent, List<String> path, List<ConfigurableFactory<?>> children) {
		super(name, PyramidIO.class, parent, path);

		List<String> pyramidTypes = getPyramidTypes(children);
		
		//use the first factory name for the first child as the default type
		String defaultType = null;
		if (pyramidTypes.size() > 0) {
			defaultType = pyramidTypes.get(0);
		}
		
		//set up the PYRAMID_IO_TYPE property to use all the associated children factory names.
		PYRAMID_IO_TYPE = new StringProperty("type",
				   "The location to and from which to read tile pyramids",
				   defaultType,
				   pyramidTypes.toArray(new String[0]));
		
		addProperty(PYRAMID_IO_TYPE);
		addProperty(INITIALIZATION_DATA);
		
		//add any child factories
		if (children != null) {
			for (ConfigurableFactory<?> factory : children) {
				addChildFactory(factory);
			}
		}
		
	}

	private static List<String> getPyramidTypes(List<ConfigurableFactory<?>> childFactories) {
		List<String> pyramidTypes = Lists.newArrayListWithCapacity(childFactories.size());
		
		//add any child factories
		if (childFactories != null) {
			for (ConfigurableFactory<?> factory : childFactories) {
				String factoryName = factory.getName();
				if (factoryName != null) {
					pyramidTypes.add(factoryName);
				}
			}
		}
		
		return pyramidTypes;
	}
	
	@Override
	protected PyramidIO create () {
		String pyramidIOType = getPropertyValue(PYRAMID_IO_TYPE);

		try {
			return produce(pyramidIOType, PyramidIO.class);
		} catch (Exception e) {
			LOGGER.error("Error trying to create PyramidIO", e);
			return null;
		}
	}
	
}
