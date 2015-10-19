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



import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * This is like a normal ConfigurableFactory, except that, given the same
 * configuration, any factory of the same type will always produce the same
 * product. Note this takes into account the entire configuration, not just the
 * part related to the factory's properties, or those properties used to create
 * the product, so extraneous properties or configuration details will cause
 * false cache misses.
 *
 * @author nkronenfeld
 */
public abstract class SharedInstanceFactory<T> extends ConfigurableFactory<T> {
	/*
	 * The static mapping containing all instances ever made. This maps from
	 * factory type to configuration to object. Configurations are kepts as
	 * strings, because equivalent JSON objects are not equal.
	 * 
	 * We need concurrency across several calls, so just using a
	 * ConcurrentHashMap is insufficient here - we instead have to synchronize
	 * across the appropriate calls.
	 */
	private static final Map<Class<?>, Map<String, Object>> _instances = new HashMap<>();




	protected SharedInstanceFactory (Class<T> factoryType,
	                                 ConfigurableFactory<?> parent,
	                                 List<String> path) {
		super(factoryType, parent, path);
	}

	protected SharedInstanceFactory (String name, Class<T> factoryType,
	                                 ConfigurableFactory<?> parent,
	                                 List<String> path) {
		super(name, factoryType, parent, path);
	}

	@Override
	final protected T create () throws ConfigurationException {
		synchronized (_instances) {
			if (!_instances.containsKey(this.getClass())) {
				_instances.put(this.getClass(), new HashMap<String, Object>());
			}
			Map<String, Object> factoryClassProducts = _instances.get(this.getClass());
			String configuration = getConfigurationNode().toString();
			if (!factoryClassProducts.containsKey(configuration)) {
				factoryClassProducts.put(configuration, createInstance());
			}
			return getFactoryType().cast(factoryClassProducts.get(configuration));
		}
	}

	abstract protected T createInstance () throws ConfigurationException;
}
