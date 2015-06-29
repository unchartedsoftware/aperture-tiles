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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;

import com.oculusinfo.binning.io.serialization.GenericAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;

/**
 * A serializer to serialize tiles whose bin values are any Avro primitive type.
 *
 * The Avro primitives are: boolean, int, long, float, double, bytes, and string,
 * which correspond to Java Boolean, Integer, Long, Float, Double,
 * java.nio.ByteBuffer, and org.apache.avro.util.Utf8.  We do a bit of a hack to
 * allow use of the much simpler String instead of Utf8, but the others stand as
 * is.  Attempting to create a version with any other class will result in a
 * run-time error.
 */
public class PrimitiveAvroSerializer<T> extends GenericAvroSerializer<T> {
	private static final long serialVersionUID = 4949141562108321166L;

	private static final Map<Class<?>, String> VALID_PRIMITIVE_TYPES =
		Collections.unmodifiableMap(new HashMap<Class<?>, String>() {
				private static final long serialVersionUID = 1L;
				{
					put(Boolean.class, "boolean");
					put(Integer.class, "int");
					put(Long.class, "long");
					put(Float.class, "float");
					put(Double.class, "double");
					put(ByteBuffer.class, "bytes");
					put(String.class, "string");
				}
			});
	public static final Set<Class<?>> PRIMITIVE_TYPES = VALID_PRIMITIVE_TYPES.keySet();


	/**
	 * Determines if a class represents a valid Avro primitive type
	 */
	static <T> boolean isValidPrimitive (Class<? extends T> type) {
		return VALID_PRIMITIVE_TYPES.containsKey(type);
	}

	/**
	 * Get the avro string type of a valid primitive type
	 */
	public static <T> String getAvroType (Class<? extends T> type) {
		return VALID_PRIMITIVE_TYPES.get(type);
	}

	/**
	 * Wrap a primitive type in a type descriptor, making sure while doing so that it
	 * actually is a primitive type.
	 */
	static <T> TypeDescriptor getPrimitiveTypeDescriptor (Class<? extends T> type) {
		if (!isValidPrimitive(type))
			throw new IllegalArgumentException("Invalid type "+type+
			                                   ": Not one of the prescribed primitives allowed.");
		return new TypeDescriptor(type);
	}

	private static PatternedSchemaStore __schemaStore = new PatternedSchemaStore(
		       "{\n" +
		       "  \"name\":\"recordType\",\n" +
		       "  \"namespace\":\"ar.avro\",\n" +
		       "  \"type\":\"record\",\n" +
		       "  \"fields\":[\n" +
		       "    {\"name\":\"value\", \"type\":\"%s\"}\n" +
		       "  ]\n" +
		       "}");



	private Class<? extends T>          _type;
	private transient Schema            _schema = null;
	// A bit of a hack to handle string tiles as strings rather than Utf8s
	private boolean                     _toString;

	public PrimitiveAvroSerializer (Class<? extends T> type, CodecFactory compressionCodec) {
		super(compressionCodec, getPrimitiveTypeDescriptor(type));

		_type = type;
		_toString = (String.class.equals(type));
	}

	@Override
	protected String getRecordSchemaFile () {
		throw new UnsupportedOperationException("Primitive types have standard schema; schema files should not be required.");
	}

	@Override
	protected Schema createRecordSchema () throws IOException {
		if (null == _schema) {
			String typeName = getAvroType(_type);
			_schema = __schemaStore.getSchema(_type, typeName);
		}
		return _schema;
	}

	// This doesn't need to be checked because
	//  (a) One can't create a serializer for which it theoreticallly won't work.
	//  (b) It is possible to use the wrong serializer for a given tile, in which
	//      case it will fail - but it should fail in that case.
	@SuppressWarnings("unchecked")
	@Override
	protected T getValue (GenericRecord bin) {
		if (_toString) {
			// A bit of a hack to handle string tiles as strings rather than Utf8s
			return (T) bin.get("value").toString();
		} else {
			if (null == bin || null == bin.get("value")) {
				return null;
			}
			return (T) bin.get("value");
		}
	}

	@Override
	protected void setValue (GenericRecord bin, T value) throws IOException {
		if (null == value) throw new IOException("Null value for bin");
		bin.put("value", value);
	}
}
