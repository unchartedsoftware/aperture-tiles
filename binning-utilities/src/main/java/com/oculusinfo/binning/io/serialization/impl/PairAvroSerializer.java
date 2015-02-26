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

import com.oculusinfo.binning.io.serialization.GenericAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;

/**
 * A serializer to serialize tiles whose bin values are a pair of Avro primitive type.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
public class PairAvroSerializer<S, T> extends GenericAvroSerializer<Pair<S, T>> {
	private static final long serialVersionUID = 1777339648086558933L;
	private static PatternedSchemaStore __schemaStore = new PatternedSchemaStore(
		       "{\n" +
		       "  \"name\":\"recordType\",\n" +
		       "  \"namespace\":\"ar.avro\",\n" +
		       "  \"type\":\"record\",\n" +
		       "  \"fields\":[\n" +
		       "    {\"name\":\"key\", \"type\":\"%s\"},\n" +
		       "    {\"name\":\"value\", \"type\":\"%s\"}\n" +
		       "  ]\n" +
		       "}");



	private Class<? extends S>          _keyType;
	private Class<? extends T>          _valueType;
	private transient Schema _schema    = null;
	// A bit of a hack to handle string tiles as strings rather than Utf8s
	private boolean                     _keyToString;
	private boolean                     _valueToString;

	public PairAvroSerializer (Class<? extends S> keyType, Class<? extends T> valueType,
	                           CodecFactory compressionCodec) {
		super(compressionCodec,
		      new TypeDescriptor(Pair.class,
		                         PrimitiveAvroSerializer.getPrimitiveTypeDescriptor(keyType),
		                         PrimitiveAvroSerializer.getPrimitiveTypeDescriptor(valueType)));

		_keyType = keyType;
		_valueType = valueType;
		_keyToString = (String.class.equals(keyType));
		_valueToString = (String.class.equals(valueType));
	}


	@Override
	protected String getRecordSchemaFile () {
		throw new UnsupportedOperationException("Primitive types have standard schema; schema files should not be required.");
	}

	@Override
	protected Schema createRecordSchema () throws IOException {
		if (null == _schema) {
			String keyTypeName = PrimitiveAvroSerializer.getAvroType(_keyType);
			String valueTypeName = PrimitiveAvroSerializer.getAvroType(_valueType);
			_schema = __schemaStore.getSchema(new Pair<>(_keyType, _valueType), keyTypeName, valueTypeName);
		}
		return _schema;
	}

	// This doesn't need to be checked because
	//  (a) One can't create a serializer for which it theoreticallly won't work.
	//  (b) It is possible to use the wrong serializer for a given tile, in which
	//      case it will fail - but it should fail in that case.
	@SuppressWarnings("unchecked")
	@Override
	protected Pair<S, T> getValue (GenericRecord bin) {
		S key;
		if (_keyToString) key = (S) bin.get("key").toString();
		else key = (S) bin.get("key");

		T value;
		if (_valueToString) value = (T) bin.get("value").toString();
		else value = (T) bin.get("value");

		return new Pair<S, T>(key, value);
	}

	@Override
	protected void setValue (GenericRecord bin, Pair<S, T> value) throws IOException {
		if (null == value) throw new IOException("Null value for bin");
		bin.put("key", value.getFirst());
		bin.put("value", value.getSecond());
	}
}
