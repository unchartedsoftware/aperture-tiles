/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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

import com.oculusinfo.binning.io.impl.*;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.providers.FactoryProvider;



/**
 * Basic enum of all the default {@link FactoryProvider} types
 * availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 *
 * <pre>
 * <code>
 * DefaultPyramidIOFactoryProvider.FILE.create();
 * </code>
 * </pre>
 *
 * @author cregnier
 */
public enum DefaultPyramidIOFactoryProvider implements FactoryProvider<PyramidIO> {
	HBASE(new Constructor() {
			@Override
			public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
				return new HBasePyramidIOFactory(parent, path);
			}
		}),
	HBASE_SLICED(new Constructor() {
			@Override
			public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
				return new HBaseSlicedPyramidIOFactory(parent, path);
			}
		}),
    FILE(new Constructor() {
            @Override
            public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
                return new FileBasedPyramidIOFactory(parent, path);
            }
        }),
    JDBC(new Constructor() {
            @Override
            public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
                return new JDBCPyramidIOFactory(parent, path);
            }
        }),
    SQLITE(new Constructor() {
            @Override
            public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
                return new SQLitePyramidIOFactory(parent, path);
            }
        }),
	ELASTICSEARCH(new Constructor() {
		@Override
		public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
			return new ElasticsearchPyramidIOFactory(parent, path);
		}
	}),
    DUMMY(new Constructor() {
            @Override
            public ConfigurableFactory<PyramidIO> create(ConfigurableFactory<?> parent, java.util.List<String> path) {
                return new DummyPyramidIOFactory(parent, path);
            }
        });


	// -------------------------------------

	private final Constructor _constructor;



	private DefaultPyramidIOFactoryProvider (Constructor constructor) {
		this._constructor = constructor;
	}

	@Override
	public ConfigurableFactory<PyramidIO> createFactory (List<String> path) {
		return createFactory(null, null, path);
	}

    @Override
    public ConfigurableFactory<PyramidIO> createFactory (ConfigurableFactory<?> parent,
                                                         List<String> path) {
        return createFactory(null, parent, path);
    }

    @Override
	public ConfigurableFactory<PyramidIO> createFactory (String name,
	                                                     ConfigurableFactory<?> parent,
	                                                     List<String> path) {
		return _constructor.create(parent, path);
	}

	private static interface Constructor {
		ConfigurableFactory<PyramidIO> create (ConfigurableFactory<?> parent,
		                                       List<String> path);
	}
}
