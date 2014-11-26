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



import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.providers.DelegateFactoryProviderTarget;
import com.oculusinfo.factory.providers.FactoryProvider;



/**
 * Base class to simplify creation of UberFactories
 * 
 * @author nkronenfeld, pulled out from code by cregnier
 */
abstract public class StandardUberFactoryProvider<T> implements FactoryProvider<T> {
	/**
	 * Concatenates two string lists together.
	 * 
	 * @return If either of the input lists are null, then the other is
	 *         returned, othwerise a new list is created that contains both
	 *         lists.
	 */
	protected static List<String> getMergedPath (List<String> path1,
	                                             List<String> path2) {
		if (path1 == null)
			return path2;
		if (path2 == null)
			return path1;
		return Lists.newArrayList(Iterables.concat(path1, path2));
	}



	private List<DelegateFactoryProviderTarget<T>> _childFactories;



	protected StandardUberFactoryProvider (Set<DelegateFactoryProviderTarget<T>> providers) {
		_childFactories = Lists.newArrayList();
		for (DelegateFactoryProviderTarget<T> provider: providers) {
			_childFactories.add(provider);
		}
	}



	/**
	 * Creates a List of factories for all the 'child' factory providers.<br>
	 * Each factory is passed a relative base path where its configuration can
	 * be found, and then it will add its own path, if needed.
	 * 
	 * @param path
	 * @return
	 */
	protected List<ConfigurableFactory<? extends T>> createChildren (List<String> path) {
		List<ConfigurableFactory<? extends T>> children = Lists.newArrayList();
		for (DelegateFactoryProviderTarget<T> childProvider: _childFactories) {
			ConfigurableFactory<? extends T> factory = childProvider.createFactory(path);
			children.add(factory);
		}

		return children;
	}
}
