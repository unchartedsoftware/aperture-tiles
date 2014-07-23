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
package com.oculusinfo.annotation.init.providers;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.annotation.filter.AnnotationFilterFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.tile.init.DelegateFactoryProviderTarget;
import com.oculusinfo.tile.init.FactoryProvider;

import java.util.List;
import java.util.Set;


@Singleton
public class StandardAnnotationFilterFactoryProvider implements FactoryProvider<AnnotationFilter> {
	
	protected static class ChildProvider {

		final FactoryProvider<?> provider;
		final List<String> path;
		final String factoryName;
		
		public ChildProvider(FactoryProvider<?> provider, List<String> path, String factoryName) {
			this.provider = provider;
			this.path = path;
			this.factoryName = factoryName;
		}
	}

	/**
	 * Concatenates two string lists together.
	 * @return If either of the input lists are null, then the other is returned, otherwise
	 * a new list is created that contains both lists.
	 */
	private static List<String> getMergedPath(List<String> path1, List<String> path2) {
		if (path1 == null)
			return path2;
		if (path2 == null)
			return path1;
		return Lists.newArrayList(Iterables.concat(path1, path2));
	}

	//-------------------------------------------------------------------
	
	List<ChildProvider> childFactories = Lists.newArrayList();

	
	//-------------------------------------------------------------------
	
	
	@Inject
	public StandardAnnotationFilterFactoryProvider(Set<DelegateFactoryProviderTarget<AnnotationFilter>> providers) {
		for (DelegateFactoryProviderTarget<AnnotationFilter> provider : providers) {
			childFactories.add(new ChildProvider(provider, provider.getPath(), provider.getFactoryName()));
		}
	}
	
	/**
	 * Creates a List of factories for all the 'child' factory providers.<br>
	 * Each factory is passed a relative base path where its configuration can be found, and then it
	 * will add its own path, if needed. 
	 * @param path
	 * @return
	 */
	protected List<ConfigurableFactory<?>> createChildren(List<String> path) {
		List<ConfigurableFactory<?>> children = Lists.newArrayList();
		for (ChildProvider childProvider : childFactories) {
			ConfigurableFactory<?> factory = childProvider.provider.createFactory(childProvider.factoryName, null, getMergedPath(path, childProvider.path));
			children.add(factory);
		}
		
		return children;
	}
	
	@Override
	public ConfigurableFactory<AnnotationFilter> createFactory (List<String> path) {
		return new AnnotationFilterFactory(null, path, createChildren(path));
	}

	@Override
	public ConfigurableFactory<AnnotationFilter> createFactory (ConfigurableFactory<?> parent,
	                                                     List<String> path) {
		return new AnnotationFilterFactory(parent, path, createChildren(getMergedPath(parent.getRootPath(), path)));
	}

	@Override
	public ConfigurableFactory<AnnotationFilter> createFactory (String factoryName,
	                                                     ConfigurableFactory<?> parent,
	                                                     List<String> path) {
		return new AnnotationFilterFactory(factoryName, parent, path, createChildren(getMergedPath(parent.getRootPath(), path)));
	}
	
}
