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
package com.oculusinfo.annotation.init;

import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.annotation.filter.EmptyFilterFactory;
import com.oculusinfo.annotation.filter.NMostRecentByGroupFactory;
import com.oculusinfo.annotation.filter.ScriptableFilterFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.tile.init.DelegateFactoryProviderTarget;

import java.util.List;

/**
 * Basic enum of all the default {@link DelegateFactoryProviderTarget} types availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 * <pre><code>
 * DefaultAnnotationFilterFactoryProvider.N_MOST_RECENT_BY_GROUP.create();
 * </code></pre>
 * 
 * @author cregnier
 *
 */
public enum DefaultAnnotationFilterFactoryProvider {
	EMPTY ("empty"),
	N_MOST_RECENT_BY_GROUP ("n-most-recent-by-group"),
    SCRIPTABLE ("scriptable");

	//-------------------------------------

	private final String factoryTypeName;

	private DefaultAnnotationFilterFactoryProvider(String factoryTypeName) {
		this.factoryTypeName = factoryTypeName;
	}
	
	/**
	 * Creates the {@link DelegateFactoryProviderTarget} for the desired type.
	 */
	public DelegateFactoryProviderTarget<AnnotationFilter> create() {
		return new DefaultAnnotationFilterFactoryProviderImpl(factoryTypeName);
	}

	//-------------------------------------

	protected static class DefaultAnnotationFilterFactoryProviderImpl implements DelegateFactoryProviderTarget<AnnotationFilter> {
	
		private final String factoryType;
		
		protected DefaultAnnotationFilterFactoryProviderImpl(String factoryType) {
			this.factoryType = factoryType;
		}
		
		@Override
		public ConfigurableFactory<AnnotationFilter> createFactory(List<String> path) {
			return createFactory(null, null, path);
		}
	
		@Override
		public ConfigurableFactory<AnnotationFilter> createFactory(ConfigurableFactory<?> parent, List<String> path) {
			return createFactory(null, parent, path);
		}
	
		@Override
		public ConfigurableFactory<AnnotationFilter> createFactory(String factoryName, ConfigurableFactory<?> parent, List<String> path) {
			ConfigurableFactory<AnnotationFilter> factory = null;
			if (factoryType.equals("empty")) {
				factory = new EmptyFilterFactory(factoryName, parent, path);
			} else if (factoryType.equals("n-most-recent-by-group")) {
				factory = new NMostRecentByGroupFactory(factoryName, parent, path);
			} else if (factoryType.equals("scriptable")) {
                factory = new ScriptableFilterFactory(factoryName, parent, path);
            }
			return factory;
		}
	
		@Override
		public String getFactoryName() {
			return factoryType;
		}
	
		@Override
		public List<String> getPath() {
			return null;
		}
	
	}
}
