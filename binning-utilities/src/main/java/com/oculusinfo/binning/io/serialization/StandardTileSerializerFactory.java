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

import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.IntegerAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringDoublePairArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayJSONSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.EnumProperty;
import com.oculusinfo.factory.IntegerProperty;
import com.oculusinfo.factory.StringProperty;




/**
 * The TileSerializerFactory can create a serializer from a property node.
 * 
 * @author nkronenfeld
 */
public class StandardTileSerializerFactory
       extends ConfigurableFactory<TileSerializer<?>> {
    public static StringProperty          SERIALIZER_TYPE = new StringProperty("type",
                                                                               ("The type of serializer desired.  Except for the legacy serializer, this will "
                                                                                + "just be a short-hand describing the class type of the individual bins of the tile "
                                                                                + "type that the serializer can handle.  In this short-hand, square brackets surround "
                                                                                + "an array, parentheses, an n-tuple.  Finally, after the type is a suffix indicating "
                                                                                + "the format in which the data will be written - \"-a\" for Apache Avro, \"-j\" for "
                                                                                + "JSON.  So, \"double-a\" indicates a serializer for tiles whose bins are just doubles, "
                                                                                + "written using Avro."),
                                                                                "double-a",
                                                                               new String[] {
                                                                                             "legacy",
                                                                                             "[(string, integer)]-j",
                                                                                             "integer-a", "double-a", "[double]-a", "[string]-a",
                                                                                             "[(string, integer)]-a", "[(string, double)]-a"
                                                                               });
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

    public StandardTileSerializerFactory (ConfigurableFactory<?> parent,
                                          List<String> path) {
        super(getGenericSerializerClass(), parent, path);

        initializeProperties();
    }

    public StandardTileSerializerFactory (String name,
                                          ConfigurableFactory<?> parent,
                                          List<String> path) {
        super(name, getGenericSerializerClass(), parent, path);

        initializeProperties();
    }

    protected void initializeProperties () {
        addProperty(SERIALIZER_TYPE);
        addProperty(CODEC_TYPE);
        addProperty(DEFLATE_LEVEL);
    }

    @Override
    protected TileSerializer<?> create () {
        String serializerType = getPropertyValue(SERIALIZER_TYPE);

        if ("legacy".equals(serializerType)) {
            return new BackwardCompatibilitySerializer();
        }

        if ("[(string, integer)]-j".equals(serializerType)) {
            return new StringIntPairArrayJSONSerializer();
        }

        // The rest are all avro
        CodecType codecType = getPropertyValue(CODEC_TYPE);
        CodecFactory codec = null;
        switch (codecType) {
            case Snappy:
                codec = CodecFactory.snappyCodec();
                break;
            case Deflate:
                int deflateLevel = getPropertyValue(DEFLATE_LEVEL);
                codec = CodecFactory.deflateCodec(deflateLevel);
                break;
            case None:
                codec = CodecFactory.nullCodec();
                break;
            case BZip2:
            default:
                codec = CodecFactory.bzip2Codec();
                break;
        }
        if ("integer-a".equals(serializerType)) {
            return new IntegerAvroSerializer(codec);
        }
        if ("double-a".equals(serializerType)) {
            return new DoubleAvroSerializer(codec);
        }
        if ("[double]-a".equals(serializerType)) {
            return new DoubleArrayAvroSerializer(codec);
        }
        if ("[string]-a".equals(serializerType)) {
            return new StringArrayAvroSerializer(codec);
        }
        if ("[(string, integer)]-a".equals(serializerType)) {
            return new StringIntPairArrayAvroSerializer(codec);
        }
        if ("[(string, double)]-a".equals(serializerType)) {
            return new StringDoublePairArrayAvroSerializer(codec);
        }

        return null;
    }
}
