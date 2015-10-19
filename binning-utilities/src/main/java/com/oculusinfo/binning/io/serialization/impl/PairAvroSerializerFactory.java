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
package com.oculusinfo.binning.io.serialization.impl;

import java.util.List;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.TileSerializerFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.util.Pair;

/**
 * This serializer factory constructs a
 * {@link com.oculusinfo.binning.io.serialization.impl.PairAvroSerializer},
 * for use with tiles whose values are pairs of primitive Avro types.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
public class PairAvroSerializerFactory<S, T> extends ConfigurableFactory<TileSerializer<Pair<S, T>>>
{
	private static <S, T> String getName (Class<? extends S> keyType, Class<? extends T> valueType) {
		if (!PrimitiveAvroSerializer.isValidPrimitive(keyType) ||
		    !PrimitiveAvroSerializer.isValidPrimitive(valueType))
			throw new IllegalArgumentException("Attempt to create pair serializer factory with non-primitive class(es) "+keyType+" and/or "+valueType);
		return "("+keyType.getSimpleName().toLowerCase()+", "+valueType.getSimpleName().toLowerCase()+")-a";
	}

	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <S, T> Class<TileSerializer<Pair<S, T>>>
		getGenericSerializerClass (Class<? extends S> keyType, Class<? extends T> valueType) {
		if (!PrimitiveAvroSerializer.isValidPrimitive(keyType) ||
		    !PrimitiveAvroSerializer.isValidPrimitive(valueType))
			throw new IllegalArgumentException("Attempt to create pair array serializer factory with non-primitive class(es) "+keyType+" and/or "+valueType);
		return (Class) TileSerializer.class;
	} 

	private Class<? extends S> _keyType;
	private Class<? extends T> _valueType;
	public PairAvroSerializerFactory (ConfigurableFactory<?> parent, List<String> path,
	                                  Class<? extends S> keyType,
	                                  Class<? extends T> valueType) {
		super(getName(keyType, valueType), getGenericSerializerClass(keyType, valueType), parent, path);
		_keyType = keyType;
		_valueType = valueType;
	}

	@Override
	protected TileSerializer<Pair<S, T>> create () throws ConfigurationException {
		return new PairAvroSerializer<S, T>(_keyType, _valueType, TileSerializerFactory.getCodecFactory(this));
	}
}
