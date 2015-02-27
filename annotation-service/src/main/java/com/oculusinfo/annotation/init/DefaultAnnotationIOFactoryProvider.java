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
import com.oculusinfo.annotation.io.impl.FileSystemAnnotationIOFactory;
import com.oculusinfo.annotation.io.impl.HBaseAnnotationIOFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.providers.FactoryProvider;

import java.util.List;

/**
 * Basic enum of all the default {@link FactoryProvider} types availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 * <pre><code>
 * DefaultAnnotationIOFactoryProvider.FILE_SYSTEM.create();
 * </code></pre>
 * 
 * @author cregnier
 *
 */
public enum DefaultAnnotationIOFactoryProvider implements FactoryProvider<AnnotationIO> {
	HBASE(new Constructor() {
			@Override
			public ConfigurableFactory<AnnotationIO> create(ConfigurableFactory<?> parent, List<String> path) {
				return new HBaseAnnotationIOFactory(parent, path);
			}
		}),
    FILE(new Constructor() {
            @Override
            public ConfigurableFactory<AnnotationIO> create(ConfigurableFactory<?> parent, List<String> path) {
                return new FileSystemAnnotationIOFactory(parent, path);
            }
    });


	// -------------------------------------

	private final Constructor _constructor;



	private DefaultAnnotationIOFactoryProvider (Constructor constructor) {
		this._constructor = constructor;
	}

	@Override
	public ConfigurableFactory<AnnotationIO> createFactory (List<String> path) {
		return createFactory(null, null, path);
	}

    @Override
    public ConfigurableFactory<AnnotationIO> createFactory (ConfigurableFactory<?> parent,
                                                            List<String> path) {
        return createFactory(null, parent, path);
    }

    @Override
	public ConfigurableFactory<AnnotationIO> createFactory (String name,
	                                                        ConfigurableFactory<?> parent,
	                                                        List<String> path) {
		return _constructor.create(parent, path);
	}

	private static interface Constructor {
		ConfigurableFactory<AnnotationIO> create (ConfigurableFactory<?> parent,
		                                          List<String> path);
	}
}
