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
 * {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer},
 * for use with tiles whose values are lists of some primitive Avro type.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
public class PrimitiveArrayAvroSerializerFactory<T>  extends ConfigurableFactory<TileSerializer<List<T>>> {
	private static <T> String getName (Class<? extends T> entryType) {
		if (!PrimitiveAvroSerializer.isValidPrimitive(entryType))
			throw new IllegalArgumentException("Attempt to create primitive array serializer factory with non-primitive class "+entryType);
		return "["+entryType.getSimpleName().toLowerCase()+"]-a";
	}
	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <T> Class<TileSerializer<List<T>>> getGenericSerializerClass (Class<? extends T> entryType) {
		if (!PrimitiveAvroSerializer.isValidPrimitive(entryType))
			throw new IllegalArgumentException("Attempt to create primitive array serializer factory with non-primitive class "+entryType);
		return (Class) TileSerializer.class;
	}

	private Class<? extends T> _entryType;
	public PrimitiveArrayAvroSerializerFactory (ConfigurableFactory<?> parent, List<String> path,
	                                            Class<? extends T> entryType) {
		super(getName(entryType), getGenericSerializerClass(entryType), parent, path);
		_entryType = entryType;
	}

	@Override
	protected TileSerializer<List<T>> create () throws ConfigurationException {
		return new PrimitiveArrayAvroSerializer<>(_entryType, TileSerializerFactory.getCodecFactory(this));
	}
}
