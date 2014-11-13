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

import com.oculusinfo.binning.io.serialization.AvroSchemaComposer;
import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;

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



    private Schema _schema;

    public PrimitiveArrayAvroSerializer (Class<? extends T> type, CodecFactory compressionCodec) {
        super(compressionCodec, PrimitiveAvroSerializer.getPrimitiveTypeDescriptor(type));

        String typeName = PrimitiveAvroSerializer.getAvroType(type);
        _schema = __schemaStore.getSchema(type, typeName);
    }

    @Override
    protected String getEntrySchemaFile() {
        throw new UnsupportedOperationException("Primitive types have standard schema; schema files should not be required.");
    }

    @Override
    protected Schema createEntrySchema () {
        return _schema;
    }

    @Override
    protected T getEntryValue(GenericRecord entry) {
        return (T) entry.get(0);
    }

    @Override
    protected void setEntryValue(GenericRecord avroEntry, T rawEntry) {
        avroEntry.put("value", rawEntry);
    }
}
