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
package com.oculusinfo.factory.providers;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.oculusinfo.factory.ConfigurableFactory;



/**
 * Base class to simplify creation of UberFactories
 * 
 * @author nkronenfeld, pulled out from code by cregnier
 */
abstract public class StandardUberFactoryProvider<T> extends AbstractFactoryProvider<T> {
	private List<FactoryProvider<T>> _childFactories;



	protected StandardUberFactoryProvider (Set<FactoryProvider<T>> providers) {
		_childFactories = new ArrayList<>();
		for (FactoryProvider<T> provider: providers) {
			_childFactories.add(provider);
		}
	}



	/**
	 * Creates a List of factories for all the 'child' factory providers.
	 *
	 * Each factory is passed a relative base path where its configuration can be found, and then it will add its own
	 * path, if needed.
	 * 
	 * @param path The absolute base path where the configuration can be found
	 * @return The created children.
	 */
	protected List<ConfigurableFactory<? extends T>> createChildren (List<String> path) {
		List<ConfigurableFactory<? extends T>> children = new ArrayList<>();
		for (FactoryProvider<T> childProvider: _childFactories) {
			ConfigurableFactory<? extends T> factory = childProvider.createFactory(path);
			children.add(factory);
		}

		return children;
	}

	/**
	 * Creates a List of factories for all the 'child' factory providers.
	 *
	 * Each factory is passed a relative base path where its configuration can be found, and then it will add its own
	 * path, if needed.
	 *
	 * @param parent The parent factory under which to create sub-factories.  Sub-factories will have the same parent
	 *               as the UberFactory
	 * @param path The base path where the configuration can be found, relative to the parent's path.
	 * @return The created children.
	 */
	protected List<ConfigurableFactory<? extends T>> createChildren (ConfigurableFactory<?> parent, List<String> path)  {
		List<ConfigurableFactory<? extends T>> children = new ArrayList<>();
		for (FactoryProvider<T> childProvider: _childFactories) {
			ConfigurableFactory<? extends T> factory = childProvider.createFactory(parent, path);
			children.add(factory);
		}

		return children;
	}
}
