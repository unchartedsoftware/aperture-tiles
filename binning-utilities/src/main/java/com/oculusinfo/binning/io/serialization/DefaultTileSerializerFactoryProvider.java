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
package com.oculusinfo.binning.io.serialization;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oculusinfo.binning.io.serialization.impl.*;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.providers.AbstractFactoryProvider;
import com.oculusinfo.factory.util.Pair;


/**
 * Basic enum of all the default {@link FactoryProvider} types
 * availables in the system.<br>
 * <br>
 * To create one use the create method for the desired type. Example:<br>
 *
 * This isn't really an enum, but acts like one in all ways except
 * initialization; it is not one to allow us to use loops and generification
 * during initialization.
 *
 * <pre>
 * <code>
 * DefaultPyramidIOFactoryProvider.FILE.create();
 * </code>
 * </pre>
 */
public final class DefaultTileSerializerFactoryProvider
	extends AbstractFactoryProvider<TileSerializer<?>>
	implements Comparable<DefaultTileSerializerFactoryProvider>
{
	private static int __currentOrdinal                                        = 0;
	private static List<DefaultTileSerializerFactoryProvider> __values         = new ArrayList<>();
	private static Map<String, DefaultTileSerializerFactoryProvider> __reverse = new HashMap<>();


	// Specific, un-generified serializer types

	// Our old pre-avro serializer
	@Deprecated
	public static final DefaultTileSerializerFactoryProvider LEGACY =
		new DefaultTileSerializerFactoryProvider("legacy", new Constructor() {
				@Override
				public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
				                                                                List<String> path) {
					return new com.oculusinfo.binning.io.serialization.impl.BackwardsCompatibilitySerializerFactory(parent, path);
				}
			});

	// JSON serializers
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
	// Single-value serialziers
	public static final List<DefaultTileSerializerFactoryProvider> PRIMITIVES =
		Collections.unmodifiableList(new ArrayList<DefaultTileSerializerFactoryProvider>() {
				private static final long serialVersionUID = 1L;
				{
					for (final Class<?> type: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String name = PrimitiveAvroSerializer.getAvroType(type)+"_avro";
						// Note that the double-valued primitive serializer should be the default.
						add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
								@Override
								public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
								                                                                List<String> path) {
									return new PrimitiveAvroSerializerFactory<>(parent, path, type);
								}
							}, Double.class.equals(type)));
					}
				}
			});

	// Array serializers
	public static final List<DefaultTileSerializerFactoryProvider> PRIMITIVE_ARRAYS =
		Collections.unmodifiableList(new ArrayList<DefaultTileSerializerFactoryProvider>() {
				private static final long serialVersionUID = 1L;
				{
					for (final Class<?> type: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String name = PrimitiveAvroSerializer.getAvroType(type)+"_array_avro";
						add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
								@Override
								public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
								                                                                List<String> path) {
									return new PrimitiveArrayAvroSerializerFactory<>(parent, path, type);
								}
							}));
					}
				}
			});

	// Simple Pair serializers
	public static final List<DefaultTileSerializerFactoryProvider> PAIRS =
		Collections.unmodifiableList(new ArrayList<DefaultTileSerializerFactoryProvider>() {
				private static final long serialVersionUID = 1L;

				{
					for (final Class<?> keyType: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String keyName = PrimitiveAvroSerializer.getAvroType(keyType);
						for (final Class<?> valueType: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
							String valueName = PrimitiveAvroSerializer.getAvroType(valueType);
							String name = keyName + "_" + valueName + "_pair_avro";
							add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
									@Override
									public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
									                                                                List<String> path) {
										return new PairAvroSerializerFactory<>(parent, path, keyType, valueType);
									}
								}));
						}
					}
				}
			});

	// Array of Pair (can be used for maps) serializers
	public static final List<DefaultTileSerializerFactoryProvider> PAIR_ARRAYS =
		Collections.unmodifiableList(new ArrayList<DefaultTileSerializerFactoryProvider>() {
				private static final long serialVersionUID = 1L;

				{
					for (final Class<?> keyType: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
						String keyName = PrimitiveAvroSerializer.getAvroType(keyType);
						for (final Class<?> valueType: PrimitiveAvroSerializer.PRIMITIVE_TYPES) {
							String name = keyName+"_"+PrimitiveAvroSerializer.getAvroType(valueType)+"_pair_array_avro";
							add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
									@Override
									public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
									                                                                List<String> path) {
										return new PairArrayAvroSerializerFactory<>(parent, path, keyType, valueType);
									}
								}));
						}
					}
				}
			});

	// Kryo serializers
	public static final List<DefaultTileSerializerFactoryProvider> KRYO =
		Collections.unmodifiableList(new ArrayList<DefaultTileSerializerFactoryProvider>() {
				private static final long serialVersionUID = 1L;

				{
					List<Pair<String, TypeDescriptor>> primitives = new ArrayList<>();
					for (Class<?> primitive: KryoSerializer.PRIMITIVE_TYPES) {
						primitives.add(new Pair<String, TypeDescriptor>(primitive.getSimpleName().toLowerCase(), new TypeDescriptor(primitive)));
					}

					// Simple types
					for (final Pair<String, TypeDescriptor> primitive: primitives) {
						String name = primitive.getFirst()+"_kryo";
						add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
								@Override
								public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
								                                                                List<String> path) {
									return new KryoSerializerFactory<>(parent, path, primitive.getSecond());
								}
							}));
					}

					// Array types
					for (final Pair<String, TypeDescriptor> primitive: primitives) {
						String name = primitive.getFirst()+"_array_kryo";
						add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
								@Override
								public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
								                                                                List<String> path) {
									return new KryoSerializerFactory<>(parent, path, new TypeDescriptor(List.class, primitive.getSecond()));
								}
							}));
					}

					// Map types
					for (final Pair<String, TypeDescriptor> kPrimitive: primitives) {
						for (final Pair<String, TypeDescriptor> vPrimitive: primitives) {
							String name = kPrimitive.getFirst()+"_"+vPrimitive.getFirst()+"_pair_array_kryo";
							add(new DefaultTileSerializerFactoryProvider(name, new Constructor() {
									@Override
									public ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
									                                                                List<String> path) {
										return new KryoSerializerFactory<>(parent, path, new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, kPrimitive.getSecond(), vPrimitive.getSecond())));
									}
								}));
						}
					}
				}
			});


	// -------------------------------------

	private final String      _name;
	private final Constructor _constructor;
	private final int         _ordinal;



	private DefaultTileSerializerFactoryProvider (String name, Constructor constructor) {
		this(name, constructor, false);
	}
	private DefaultTileSerializerFactoryProvider (String name, Constructor constructor, boolean isDefault) {
		_name = name;
		_constructor = constructor;
		_ordinal = __currentOrdinal;
		__currentOrdinal = __currentOrdinal+1;
		if (isDefault) {
			__values.add(0, this);
		} else {
			__values.add(this);
		}
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
	public ConfigurableFactory<? extends TileSerializer<?>> createFactory (String name,
	                                                                       ConfigurableFactory<?> parent,
	                                                                       List<String> path) {
		return _constructor.create(parent, path);
	}

	// Enum mimics
	@Override
	public String toString () {
		return _name;
	}

	@Override
	protected Object clone () throws CloneNotSupportedException {
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



	// Enum static mimics
	public static DefaultTileSerializerFactoryProvider valueOf (String name) {
		return __reverse.get(name.toLowerCase());
	}

	public static DefaultTileSerializerFactoryProvider[] values () {
		return __values.toArray(new DefaultTileSerializerFactoryProvider[__values.size()]);
	}



	private static interface Constructor {
		ConfigurableFactory<? extends TileSerializer<?>> create (ConfigurableFactory<?> parent,
		                                                         List<String> path);
	}
}
