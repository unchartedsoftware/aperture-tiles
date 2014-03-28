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
package com.oculusinfo.twitter.binning;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import org.junit.Assert;
import org.junit.Test;
import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.util.Pair;



public class TwitterDemoSerializationTests {
    @Test
    public void roundTripTest () throws IOException {
        TwitterDemoAvroSerializer serializer = new TwitterDemoAvroSerializer(CodecFactory.nullCodec());
        TwitterDemoRecord recordIn = new TwitterDemoRecord("tag",
                                                           12, Arrays.asList(1, 12, 2, 6, 3, 4),
                                                           15, Arrays.asList(1, 15, 3, 5),
                                                           16, Arrays.asList(1, 16, 2, 8, 4),
                                                           5, Arrays.asList(5, 1),
                                                           Arrays.asList(new Pair<String, Long>("this is a #tag", 1L),
                                                                         new Pair<String, Long>("This is another #tag", 2L),
                                                                         new Pair<String, Long>("more #tags and more", -4L)));

        TilePyramid pyramid = new AOITilePyramid(0, 0, 1, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TileIndex index = new TileIndex(0, 0, 0, 1, 1);
        TileData<List<TwitterDemoRecord>> tileIn = new TileData<>(index, Arrays.asList(Arrays.asList(recordIn)));
        serializer.serialize(tileIn, pyramid, baos);
        baos.flush();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TileData<List<TwitterDemoRecord>> tileOut = serializer.deserialize(index, bais);
        Assert.assertNotNull(tileOut);
        Assert.assertEquals(index, tileOut.getDefinition());
        Assert.assertNotNull(tileOut.getBin(0, 0));
        Assert.assertEquals(1, tileOut.getBin(0, 0).size());
        TwitterDemoRecord recordOut = tileOut.getBin(0, 0).get(0);
        Assert.assertEquals(recordIn, recordOut);
    }
    
    @Test
    public void unicodeTest () throws IOException {
        TwitterDemoAvroSerializer serializer = new TwitterDemoAvroSerializer(CodecFactory.nullCodec());
        TwitterDemoRecord recordIn = new TwitterDemoRecord("tag",
                                                           12, Arrays.asList(1, 12, 2, 6, 3, 4),
                                                           15, Arrays.asList(1, 15, 3, 5),
                                                           16, Arrays.asList(1, 16, 2, 8, 4),
                                                           5, Arrays.asList(5, 1),
                                                           Arrays.asList(new Pair<String, Long>("\u00C0", 1L),
                                                                         new Pair<String, Long>("\u2728", 2L),
                                                                         new Pair<String, Long>("\u1F302", -4L)));

        TilePyramid pyramid = new AOITilePyramid(0, 0, 1, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TileIndex index = new TileIndex(0, 0, 0, 1, 1);
        TileData<List<TwitterDemoRecord>> tileIn = new TileData<>(index, Arrays.asList(Arrays.asList(recordIn)));
        serializer.serialize(tileIn, pyramid, baos);
        baos.flush();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TileData<List<TwitterDemoRecord>> tileOut = serializer.deserialize(index, bais);
        Assert.assertNotNull(tileOut);
        Assert.assertEquals(index, tileOut.getDefinition());
        Assert.assertNotNull(tileOut.getBin(0, 0));
        Assert.assertEquals(1, tileOut.getBin(0, 0).size());
        TwitterDemoRecord recordOut = tileOut.getBin(0, 0).get(0);
        Assert.assertEquals(recordIn, recordOut);
    }
}
