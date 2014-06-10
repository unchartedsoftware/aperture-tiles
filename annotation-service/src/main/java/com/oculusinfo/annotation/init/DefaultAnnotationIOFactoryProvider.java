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

import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.FileSystemAnnotationIOFactory;
import com.oculusinfo.annotation.io.HBaseAnnotationIOFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.tile.init.DelegateFactoryProviderTarget;

import java.util.List;

/**
 * Basic enum of all the default {@link DelegateFactoryProviderTarget} types availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 * <pre><code>
 * DefaultAnnotationIOFactoryProvider.FILE_SYSTEM.create();
 * </code></pre>
 * 
 * @author cregnier
 *
 */
public enum DefaultAnnotationIOFactoryProvider {
	HBASE ("hbase"),
	FILE_SYSTEM ("file-system");

	//-------------------------------------

	private final String factoryTypeName;

	private DefaultAnnotationIOFactoryProvider(String factoryTypeName) {
		this.factoryTypeName = factoryTypeName;
	}
	
	/**
	 * Creates the {@link DelegateFactoryProviderTarget} for the desired type.
	 */
	public DelegateFactoryProviderTarget<AnnotationIO> create() {
		return new DefaultAnnotationIOFactoryProviderImpl(factoryTypeName);
	}

	//-------------------------------------

	protected static class DefaultAnnotationIOFactoryProviderImpl implements DelegateFactoryProviderTarget<AnnotationIO> {
	
		private final String factoryType;
		
		protected DefaultAnnotationIOFactoryProviderImpl(String factoryType) {
			this.factoryType = factoryType;
		}
		
		@Override
		public ConfigurableFactory<AnnotationIO> createFactory(List<String> path) {
			return createFactory(null, null, path);
		}
	
		@Override
		public ConfigurableFactory<AnnotationIO> createFactory(ConfigurableFactory<?> parent, List<String> path) {
			return createFactory(null, parent, path);
		}
	
		@Override
		public ConfigurableFactory<AnnotationIO> createFactory(String factoryName, ConfigurableFactory<?> parent, List<String> path) {
			ConfigurableFactory<AnnotationIO> factory = null;
			if (factoryType.equals("hbase")) {
				factory = new HBaseAnnotationIOFactory(factoryName, parent, path);
			}
			else if (factoryType.equals("file-system") || factoryName.equals("file")) {
				factory = new FileSystemAnnotationIOFactory(factoryName, parent, path);
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
