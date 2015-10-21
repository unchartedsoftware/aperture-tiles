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


import java.util.List;

import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializerFactory;
import com.oculusinfo.factory.ConfigurationException;
import org.apache.avro.file.CodecFactory;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.UberFactory;
import com.oculusinfo.factory.properties.EnumProperty;
import com.oculusinfo.factory.properties.IntegerProperty;




/**
 * The TileSerializerFactory can create a serializer from a property node.
 * 
 * @author nkronenfeld
 */
public class TileSerializerFactory
	extends UberFactory<TileSerializer<?>> {
	public static EnumProperty<CodecType> CODEC_TYPE      = new EnumProperty<CodecType>("codec",
		              "The codec to use when compressing results if the serializer is an Avro serializer.",
		              CodecType.class,
		              CodecType.BZip2);
	public static IntegerProperty         DEFLATE_LEVEL   = new IntegerProperty("deflation",
		      "The deflation setting used if the serializer is an Avro serializer, and the deflate codec is chosen",
		      4);



	public static enum CodecType {
		BZip2, Deflate, Snappy, None
	}

	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Class<TileSerializer<?>> getGenericSerializerClass () {
		return (Class) TileSerializer.class;
	}

	public TileSerializerFactory (ConfigurableFactory<?> parent,
	                              List<String> path,
	                              List<ConfigurableFactory<? extends TileSerializer<?>>> children) {
		this(null, parent, path, children);
	}

	public TileSerializerFactory (String name,
	                              ConfigurableFactory<?> parent,
	                              List<String> path,
	                              List<ConfigurableFactory<? extends TileSerializer<?>>> children) {
		super(name, getGenericSerializerClass(), parent, path, children, PrimitiveAvroSerializerFactory.DEFAULT );

		addProperty(CODEC_TYPE);
		addProperty(DEFLATE_LEVEL);
	}

	public static <T> CodecFactory getCodecFactory (ConfigurableFactory<TileSerializer<T>> subFactory) throws ConfigurationException {
		CodecType codecType = subFactory.getPropertyValue(CODEC_TYPE);
		switch (codecType) {
		case Snappy:
			return  CodecFactory.snappyCodec();
		case Deflate:
			int deflateLevel = subFactory.getPropertyValue(DEFLATE_LEVEL);
			return  CodecFactory.deflateCodec(deflateLevel);
		case None:
			return CodecFactory.nullCodec();
		case BZip2:
		default:
			return CodecFactory.bzip2Codec();
		}
	}
}
