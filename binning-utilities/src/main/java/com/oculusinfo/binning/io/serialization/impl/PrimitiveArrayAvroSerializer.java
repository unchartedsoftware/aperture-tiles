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

/**
 * A serializer to serialize tiles whose bin values are lists of some primitive
 * type, using Avro.
 *
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
public class PrimitiveArrayAvroSerializer<T> extends GenericAvroArraySerializer<T> {
	private static final long serialVersionUID = 5994875196491382037L;

	private static PatternedSchemaStore __schemaStore = new PatternedSchemaStore(
		       "{\n" +
		       "  \"name\":\"entryType\",\n" +
		       "  \"namespace\":\"ar.avro\",\n" +
		       "  \"type\":\"record\",\n" +
		       "  \"fields\":[\n" +
		       "    {\"name\":\"value\", \"type\":\"%s\"}\n" +
		       "  ]\n" +
		       "}");



	private Class<? extends T>          _type;
	private transient Schema            _schema          = null;
	// A bit of a hack to handle string tiles as strings rather than Utf8s
	private boolean                     _toString;

	public PrimitiveArrayAvroSerializer (Class<? extends T> type, CodecFactory compressionCodec) {
		super(compressionCodec, PrimitiveAvroSerializer.getPrimitiveTypeDescriptor(type));

		_type = type;
		_toString = (String.class.equals(type));
	}

	@Override
	protected String getEntrySchemaFile() {
		throw new UnsupportedOperationException("Primitive types have standard schema; schema files should not be required.");
	}

	@Override
	protected Schema createEntrySchema () {
		if (null == _schema) {
			String typeName = PrimitiveAvroSerializer.getAvroType(_type);
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
	protected T getEntryValue(GenericRecord entry) {
		if (_toString) {
			// A bit of a hack to handle string tiles as strings rather than Utf8s
			return (T) entry.get(0).toString();
		} else {
			return (T) entry.get(0);
		}
	}

	@Override
	protected void setEntryValue(GenericRecord avroEntry, T rawEntry) {
		avroEntry.put("value", rawEntry);
	}
}
