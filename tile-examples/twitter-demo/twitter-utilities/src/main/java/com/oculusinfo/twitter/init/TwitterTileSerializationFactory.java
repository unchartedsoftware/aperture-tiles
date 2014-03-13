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
package com.oculusinfo.twitter.init;

import java.util.List;

import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.io.serialization.StandardTileSerializerFactory;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.StringProperty;
import com.oculusinfo.twitter.binning.TwitterDemoAvroSerializer;

public class TwitterTileSerializationFactory extends StandardTileSerializerFactory {
    public static StringProperty TWITTER_SERIALIZER_TYPE = new StringProperty("type",
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
                                                                                            "[(string, integer)]-a", "[(string, double)]-a",
                                                                                            "[twitterdemorecord]-a"
    });
    public TwitterTileSerializationFactory (ConfigurableFactory<?> parent,
                                            List<String> path) {
        super(parent, path);
    }

    public TwitterTileSerializationFactory (String name,
                                            ConfigurableFactory<?> parent,
                                            List<String> path) {
        super(name, parent, path);
    }

    @Override
    protected void initializeProperties () {
        addProperty(TWITTER_SERIALIZER_TYPE);
        addProperty(CODEC_TYPE);
        addProperty(DEFLATE_LEVEL);
    }

    @Override
    protected TileSerializer<?> create () {
        String serializerType = getPropertyValue(SERIALIZER_TYPE);

        if ("[twitterdemorecord]-a".equals(serializerType)) {
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
            return new TwitterDemoAvroSerializer(codec);
        } else {
            return super.create();
        }
    }
}
