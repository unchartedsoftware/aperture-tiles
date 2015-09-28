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
package com.oculusinfo.factory;



import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.factory.properties.StringProperty;



/**
 * An UberFactory encapsulates a standard pattern whereby each created object
 * has its own individual factory, and those individual factories are added to
 * one central UberFactory which is passed around as needed; the UberFactory's
 * create call calls the specific sub-factory as needed to create the actual
 * object.
 * 
 * @author nkronenfeld, pulled out from code by cregnier
 */
public class UberFactory<T> extends ConfigurableFactory<T> {
	private static final Logger  LOGGER       = LoggerFactory.getLogger(UberFactory.class);



	// A public-facing version of the factory type property.
	public static StringProperty FACTORY_TYPE = new StringProperty("type",
	                                                               "The sub-type of object to create.  The value of this parameter is used to determine which contained factory to use.",
	                                                               null);



	private String                                 _defaultType;
	private List<ConfigurableFactory<? extends T>> _children;



	public UberFactory (Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path, List<ConfigurableFactory<? extends T>> children, String defaultType) {
		this(null, factoryType, parent, path, false, children, defaultType);
	}

	public UberFactory (Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path, boolean isSingleton, List<ConfigurableFactory<? extends T>> children, String defaultType) {
		this(null, factoryType, parent, path, isSingleton, children, defaultType);
	}

	public UberFactory (String name, Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path, List<ConfigurableFactory<? extends T>> children, String defaultType) {
		this(name, factoryType, parent, path, false, children, defaultType);
	}

	public UberFactory (String name, Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path, boolean isSingleton, List<ConfigurableFactory<? extends T>> children, String defaultType) {
		super(name, factoryType, parent, path, isSingleton);

		_children = children;
		for (ConfigurableFactory<? extends T> child: children) {
			addChildFactory(child);
		}
		addProperty(FACTORY_TYPE);
		setDefaultValue(FACTORY_TYPE, defaultType);

		_defaultType = defaultType;
		if (null == _defaultType)
			_defaultType = children.get(0).getName();
	}

	@Override
	protected T create () throws ConfigurationException {
		String subType = getPropertyValue(FACTORY_TYPE);
		try {
			return super.produce(subType, getFactoryType());
		} catch (ConfigurationException e) {
			LOGGER.warn("Error creating product {}[{}] for {}", new Object[] {getFactoryType(), getName(), subType});
			return null;
		}
	}

	@Override
	public <GT> GT produce (String name, Class<GT> goodsType) throws ConfigurationException {
	    // Uber-factories only produce for exact name matches.
	    if (null == getName() && null != name) return null;
	    if (null != getName() && !getName().equals(name)) return null;

	    return super.produce(name, goodsType);
	}
}
