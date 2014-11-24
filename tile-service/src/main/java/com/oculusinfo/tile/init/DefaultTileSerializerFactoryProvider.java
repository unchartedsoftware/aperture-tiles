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
package com.oculusinfo.tile.init;



import java.util.List;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleJsonSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.StringDoublePairArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.StringFloatPairArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayJsonSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayMapJsonSerializerFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import org.apache.avro.util.Utf8;


/**
 * Basic enum of all the default {@link DelegateFactoryProviderTarget} types
 * availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 * 
 * <pre>
 * <code>
 * DefaultPyramidIOFactoryProvider.FILE.create();
 * </code>
 * </pre>
 */
public enum DefaultTileSerializerFactoryProvider
	implements DelegateFactoryProviderTarget<TileSerializer<?>>
{
	LEGACY(new Constructor() {
			@SuppressWarnings("deprecation")
			@Override
			public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
			                                                                List<String> path) {
				return new com.oculusinfo.binning.io.serialization.impl.BackwardsCompatibilitySerializerFactory(parent, path);
			}
		}),
		DOUBLE_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveAvroSerializerFactory<>(parent, path, Double.class);
				}
        
			}),
		FLOAT_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveAvroSerializerFactory<>(parent, path, Float.class);
				}

			}),
		INTEGER_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveAvroSerializerFactory<>(parent, path, Integer.class);
				}
        
			}),
		LONG_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveAvroSerializerFactory<>(parent, path, Long.class);
				}
        
			}),
		DOUBLE_JSON(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new DoubleJsonSerializerFactory(parent, path);
				}
        
			}),
		DOUBLE_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveArrayAvroSerializerFactory<>(parent, path, Double.class);
				}
        
			}),
		FLOAT_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveArrayAvroSerializerFactory<>(parent, path, Float.class);
				}
        
			}),
		INTEGER_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveArrayAvroSerializerFactory<>(parent, path, Integer.class);
				}
        
			}),
		LONG_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveArrayAvroSerializerFactory<>(parent, path, Long.class);
				}
        
			}),
		STRING_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new PrimitiveArrayAvroSerializerFactory<>(parent, path, Utf8.class);
				}
        
			}),
		STRING_DOUBLE_PAIR_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringDoublePairArrayAvroSerializerFactory(parent, path);
				}
        
			}),
		STRING_FLOAT_PAIR_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringFloatPairArrayAvroSerializerFactory(parent, path);
				}
        
			}),
		STRING_INT_PAIR_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringIntPairArrayAvroSerializerFactory(parent, path);
				}
        
			}),
		STRING_LONG_PAIR_ARRAY_AVRO(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringLongPairArrayAvroSerializerFactory(parent, path);
				}
        
			}),
		STRING_INT_PAIR_ARRAY_JSON(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringIntPairArrayJsonSerializerFactory(parent, path);
				}
        
			}),
		STRING_LONG_PAIR_ARRAY_MAP_JSON(new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringLongPairArrayMapJsonSerializerFactory(parent, path);
				}
			});

	// -------------------------------------

	private final Constructor _constructor;



	private DefaultTileSerializerFactoryProvider (Constructor constructor) {
		_constructor = constructor;
	}


	@Override
	public ConfigurableFactory<? extends TileSerializer<?>> createFactory (List<String> path) {
		return createFactory(null, path);
	}

	@Override
	public ConfigurableFactory<? extends TileSerializer<?>> createFactory (ConfigurableFactory<?> parent,
		 List<String> path) {
		return _constructor.create(parent, path);
	}



	private static interface Constructor {
		ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
		                                                         List<String> path);
	}
}
