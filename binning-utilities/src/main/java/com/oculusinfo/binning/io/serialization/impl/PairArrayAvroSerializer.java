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



import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;

import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;



/**
 * A serializer to serialize tiles whose bin values are lists of pairs of some
 * primitive types, using Avro.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
public class PairArrayAvroSerializer<S, T> extends GenericAvroArraySerializer<Pair<S, T>> {
	private static final long serialVersionUID = 1777339648086558933L;
	private static PatternedSchemaStore __schemaStore = new PatternedSchemaStore(
		       "{\n" +
		       "  \"name\":\"entryType\",\n" +
		       "  \"namespace\":\"ar.avro\",\n" +
		       "  \"type\":\"record\",\n" +
		       "  \"fields\":[\n" +
		       "    {\"name\":\"key\", \"type\":\"%s\"},\n" +
		       "    {\"name\":\"value\", \"type\":\"%s\"}\n" +
		       "  ]\n" +
		       "}");



	private Class<? extends S>          _keyType;
	private Class<? extends T>          _valueType;
	private transient Schema            _schema          = null;
	// A bit of a hack to handle string tiles as strings rather than Utf8s
	private boolean                     _keyToString;
	private boolean                     _valueToString;

	public PairArrayAvroSerializer (Class<? extends S> keyType, Class<? extends T> valueType,
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
	protected String getEntrySchemaFile() {
		throw new UnsupportedOperationException("Primitive types have standard schema; schema files should not be required.");
	}

	@Override
	protected Schema createEntrySchema () {
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
	protected Pair<S, T> getEntryValue(GenericRecord entry) {
		S key;
		if (_keyToString) key = (S) entry.get("key").toString();
		else key = (S) entry.get("key");

		T value;
		if (_valueToString) value = (T) entry.get("value").toString();
		else value = (T) entry.get("value");

		return new Pair<S, T>(key, value);
	}

	@Override
	protected void setEntryValue(GenericRecord avroEntry, Pair<S, T> rawEntry) {
		avroEntry.put("key", rawEntry.getFirst());
		avroEntry.put("value", rawEntry.getSecond());
	}
}
