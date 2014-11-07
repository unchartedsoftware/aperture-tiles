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

import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;

public class StringFloatPairArrayAvroSerializer
    extends GenericAvroArraySerializer<Pair<String, Float>>
{
    private static final long serialVersionUID = 4642227108626700911L;
    private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(Pair.class,
           new TypeDescriptor(String.class),
           new TypeDescriptor(Float.class));



    public StringFloatPairArrayAvroSerializer(CodecFactory compressionCodec) {
        super(compressionCodec, TYPE_DESCRIPTOR);
    }

    @Override
    protected String getEntrySchemaFile () {
        return "stringFloatPairEntry.avsc";
    }

    @Override
    protected Pair<String, Float> getEntryValue (GenericRecord entry) {
        return new Pair<String, Float>(entry.get("key").toString(), (Float) entry.get("value"));
    }

    @Override
    protected void setEntryValue (GenericRecord avroEntry,
                                  Pair<String, Float> rawEntry) throws IOException {
        avroEntry.put("key", rawEntry.getFirst());
        avroEntry.put("value", rawEntry.getSecond());
    }
}
