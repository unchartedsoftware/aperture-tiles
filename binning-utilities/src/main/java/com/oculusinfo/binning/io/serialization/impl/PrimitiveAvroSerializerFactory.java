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

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.TileSerializerFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;

import java.util.List;

/**
 * This serializer factory constructs a
 * {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer},
 * for use with tiles whose bin values are the basic avro primitive value types.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
public class PrimitiveAvroSerializerFactory<T> extends ConfigurableFactory<TileSerializer<T>> {

    public static String DEFAULT = getName( Double.class );

	private static <T> String getName (Class<? extends T> type) {
		if (!PrimitiveAvroSerializer.isValidPrimitive(type))
			throw new IllegalArgumentException("Attempt to create primitive serializer factory with non-primitive class "+type);
		return type.getSimpleName().toLowerCase()+"-a";
	}
	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <ST> Class<TileSerializer<ST>> getGenericSerializerClass (Class<? extends ST> type) {
		if (!PrimitiveAvroSerializer.isValidPrimitive(type))
			throw new IllegalArgumentException("Attempt to create primitive serializer factory with non-primitive class "+type);
		return (Class) TileSerializer.class;
	}



	private Class<? extends T> _type;

	public PrimitiveAvroSerializerFactory (ConfigurableFactory<?> parent, List<String> path, Class<? extends T> type) {
		super(getName(type), getGenericSerializerClass(type), parent, path);
		_type = type;
	}

	@Override
	protected TileSerializer<T> create () throws ConfigurationException {
		return new PrimitiveAvroSerializer<>(_type, TileSerializerFactory.getCodecFactory(this));
	}
}
