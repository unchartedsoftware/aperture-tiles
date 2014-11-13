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
import com.oculusinfo.binning.io.serialization.GenericAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A serializer to serialize tiles whose bin values are any Avro primitive type.
 *
 * The Avro primitives are: boolean, int, long, float, double, bytes, and string,
 * which correspond to Java Boolean, Integer, Long, Float, Double,
 * java.nio.ByteBuffer, and org.apache.avro.util.Utf8.  Attempting to create a
 * version with any other class will result in a run-time error.
 */
public class PrimitiveAvroSerializer<T> extends GenericAvroSerializer<T> {
    private static final long serialVersionUID = 4949141562108321166L;

    private static Map<Class<?>, String> VALID_PRIMITIVE_TYPES = null;

    static {
        Map<Class<?>, String> validTypes = new HashMap<>();
        validTypes.put(Boolean.class, "boolean");
        validTypes.put(Integer.class, "int");
        validTypes.put(Long.class, "long");
        validTypes.put(Float.class, "float");
        validTypes.put(Double.class, "double");
        validTypes.put(ByteBuffer.class, "bytes");
        validTypes.put(Utf8.class, "string");
        VALID_PRIMITIVE_TYPES = Collections.unmodifiableMap(validTypes);
    }

    /**
     * Determines if a class represents a valid Avro primitive type
      */
    static <T> boolean isValidPrimitive (Class<? extends T> type) {
        return VALID_PRIMITIVE_TYPES.containsKey(type);
    }

    /**
     * Get the avro string type of a valid primitive type
     */
    static <T> String getAvroType (Class<? extends T> type) {
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



    private Schema _schema;

    public PrimitiveAvroSerializer (Class<? extends T> type, CodecFactory compressionCodec) {
        super(compressionCodec, getPrimitiveTypeDescriptor(type));

        String typeName = getAvroType(type);
        _schema = __schemaStore.getSchema(type, typeName);
    }


    @Override
    protected String getRecordSchemaFile () {
        throw new UnsupportedOperationException("Primitive types have standard schema; schema files should not be required.");
    }

    @Override
    protected Schema createRecordSchema () throws IOException {
        return _schema;
    }

    @Override
    protected T getValue (GenericRecord bin) {
        return (T) bin.get("value");
    }

    @Override
    protected void setValue (GenericRecord bin, T value) throws IOException {
        if (null == value) throw new IOException("Null value for bin");
        bin.put("value", value);
    }
}
