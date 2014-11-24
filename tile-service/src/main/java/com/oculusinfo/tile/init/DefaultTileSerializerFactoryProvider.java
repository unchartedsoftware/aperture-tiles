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



import java.util.Collections;
import java.util.List;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.*;
import com.oculusinfo.binning.io.serialization.impl.PairArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.PairArrayAvroSerializerFactory;
import com.oculusinfo.binning.io.serialization.impl.PairArrayAvroSerializerFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import org.apache.avro.util.Utf8;


/**
 * Basic enum of all the default {@link DelegateFactoryProviderTarget} types
 * availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 *
 * This isn't really an enum, but acts like one in all ways; it is not one to allow us to 
 * use loops and generification during initialization.
 * 
 * <pre>
 * <code>
 * DefaultPyramidIOFactoryProvider.FILE.create();
 * </code>
 * </pre>
 */
public final class DefaultTileSerializerFactoryProvider
	implements DelegateFactoryProviderTarget<TileSerializer<?>>,
	           Comparable<DefaultTileSerializerFactoryProvider>
{
	// Specific, un-generified serializer types
	@Deprecated
	public static final DefaultTileSerializerFactoryProvider LEGACY =
		new DefaultTileSerializerFactoryProvider("legacy", new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new com.oculusinfo.binning.io.serialization.impl.BackwardsCompatibilitySerializerFactory(parent, path);
				}
			});

	public static final DefaultTileSerializerFactoryProvider DOUBLE_JSON =
		new DefaultTileSerializerFactoryProvider("double_json", new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new DoubleJsonSerializerFactory(parent, path);
				}
			});

	public static final DefaultTileSerializerFactoryProvider STRING_INT_PAIR_ARRAY_JSON =
		new DefaultTileSerializerFactoryProvider("string_int_pair_array_json", new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringIntPairArrayJsonSerializerFactory(parent, path);
				}
			});

	public static final DefaultTileSerializerFactoryProvider STRING_LONG_PAIR_ARRAY_MAP_JSON =
		new DefaultTileSerializerFactoryProvider("string_long_pair_array_map_json", new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new StringLongPairArrayMapJsonSerializerFactory(parent, path);
				}
			});



	// Generified serializer types
	public static final DefaultTileSerializationFactoryProvider PRIMITIVES =
		Collections.unmodifiableList(new ArrayList<>() {
				{
					for (Class<?> type: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String name = PrimitiveArraySerializer.getAvroType(type)+"_avro";
						add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
								@Override
								public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
								                                                                List<String> path) {
									return new PrimitiveAvroSerializerFactory<>(parent, path, type);
								}
							}))
					}
				}
			});

	public static final DefaultTileSerializationFactoryProvider PRIMITIVE_ARRAYS =
		Collections.unmodifiableList(new ArrayList<>() {
				{
					for (Class<?> type: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String name = PrimitiveArraySerializer.getAvroType(type)+"_array_avro";
						add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
								@Override
								public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
								                                                                List<String> path) {
									return new PrimitiveArrayAvroSerializerFactory<>(parent, path, type);
								}
							}))
					}
				}
			});

	public static final DefaultTileSerializationFactoryProvider PAIRS =
		Collections.unmodifiableList(new ArrayList<>() {
				{
					for (Class<?> keyType: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String keyName = PrimitiveArraySerializer.getAvroType(keyType);
						for (Class<?> valueType: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
							String name = keyName+"_"+PrimitiveArraySerializer.getAvroType(valueType)+"_pair_array_avro";
							add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
									@Override
									public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
									                                                                List<String> path) {
										return new PairArrayAvroSerializerFactory<>(parent, path, keyType, valueType);
									}
								}))
						}
					}
				}
			});


	
	// -------------------------------------

	private final String      _name;
	private final Constructor _constructor;
	private final int         _ordinal;



	private DefaultTileSerializerFactoryProvider (String name, Constructor constructor) {
		_constructor = constructor;
		_ordinal = __currentOrdinal;
		__currentOrdinal = __currentOrdinal+1;
		__values.add(this);
		__reverse.put(name, this);
	}

	public int oridinal () {
		return _ordinal;
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

	@Override
	public String toString () {
		return _name;
	}

	// Enum mimics
	@Override
	protected Object clone () {
		throw new CloneNotSupportedException("Default Tile Serializer Factory Providers should be treated like enums.");
	}

	@Override
	public int compareTo (DefaultTileSerializerFactoryProvider that) {
		return this._ordinal - that._ordinal;
	}

	public final Class<DefaultTileSerializerFactoryProvider> getDeclaringClass () {
		return DefaultTileSerializerFactoryProvider.class;
	}

	@Override
	public final boolean equals (Object that) {
		return this == that;
	}

	@Override
	public final int hashCode () {
		return super.hashCode();
	}





	private static interface Constructor {
		ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
		                                                         List<String> path);
	}



	public static DefaultTileSerializerFactoryProvider valueOf (String name) {
		return __reverse.get(name.toLowerCase());
	}

	public static List<DefaultTileSerializerFactoryProvider> values () {
		return __values;
	}

	private static __currentOrdinal                                            = 0;
	private static List<DefaultTileSerializerFactoryProvider> __values         = new ArrayList<>();
	private static Map<String, DefaultTileSerializerFactoryProvider> __reverse = new HashMap<>();
}
