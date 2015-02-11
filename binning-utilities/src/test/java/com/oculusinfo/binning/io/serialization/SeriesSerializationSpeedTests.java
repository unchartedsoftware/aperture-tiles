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
package com.oculusinfo.binning.io.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.file.CodecFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.impl.HBasePyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;

public class SeriesSerializationSpeedTests {
    static enum TYPE {kryo, avro};
    private static final int ITERATIONS = 1;
    private static final int TILES_PER_REQUEST = 1;

    private HBasePyramidIO                          _io;
    private Map<TYPE, TileSerializer<List<Double>>> _serializers;

    @Before
    public void setup () throws Exception {
        _io = new HBasePyramidIO("hadoop-s1.oculus.local", "2181", "hadoop-s1.oculus.local:60000");
        _serializers = new HashMap<SeriesSerializationSpeedTests.TYPE, TileSerializer<List<Double>>>();
        _serializers.put(TYPE.avro, new PrimitiveArrayAvroSerializer<>(Double.class,  CodecFactory.bzip2Codec()));
        _serializers.put(TYPE.kryo, new KryoSerializer<List<Double>>(new TypeDescriptor(List.class, new TypeDescriptor(Double.class))));
    }

    @After
    public void teardown () throws Exception {
        _io.close();
        _io = null;
    }
    private void testTableSpeed (TYPE type, String size) throws IOException {
        String tableName = type.toString()+"-"+size+".julia.x.y.series";
        TileSerializer<List<Double>> serializer = _serializers.get(type);
        int sqs = (int) Math.ceil(Math.sqrt(TILES_PER_REQUEST));
        int expectedSize = Integer.parseInt(size);

        // Formulate requests
        List<List<TileIndex>> requests = new ArrayList<>();
        for (int i=0; i<ITERATIONS; ++i) {
            List<TileIndex> request = new ArrayList<>();
            for (int n=0; n<TILES_PER_REQUEST; ++n) {
                int y = (int) Math.floor(n/sqs);
                int x = n - (y * sqs);
                // request.add(new TileIndex(3, 4+sqs/2-x, 4+sqs/2-y));
                request.add(new TileIndex(0, 0, 0));
            }
            requests.add(request);
        }

        // Make and time requests
        long checkTime = 0L;
        long checkBins = 0L;
        long allBins = 0L;

        long startTime = System.currentTimeMillis();
        for (int i=0; i<ITERATIONS; ++i) {
            List<TileData<List<Double>>> tiles = _io.readTiles(tableName, serializer, requests.get(i));
            long startCheckTime = System.currentTimeMillis();
            for (TileData<List<Double>> tile: tiles) {
                for (int x=0; x<256; ++x) {
                    for (int y=0; y < 256; ++y) {
                        allBins++;
                        if (tile.getBin(x, y).size() > 0) {
                            Assert.assertEquals(expectedSize, tile.getBin(x, y).size());
                            checkBins++;
                        }
                    }
                }
            }
            long endCheckTime = System.currentTimeMillis();
            checkTime += (endCheckTime - startCheckTime);
        }
        long endTime = System.currentTimeMillis();

        long time = endTime - startTime;
        double avgTime = (time-checkTime) / ((double) (ITERATIONS * TILES_PER_REQUEST));
        System.out.println("Done checking data set "+type+"-"+size+" with "+ITERATIONS+" requests of "+TILES_PER_REQUEST+" tiles");
        System.out.println("Total elapsed read time:"+formatTime(time));
        System.out.println("Total time to check results: "+formatTime(checkTime));
        System.out.println(String.format("\tchecked bins: %d of %d (%.2f%%)", checkBins, allBins, ((double) checkBins)/allBins));
        System.out.println("Average time per request: "+formatTime(avgTime));
    }
    private String formatTime (double elapsedTime) {
        if (elapsedTime > 60000) {
            int min = (int) Math.floor(elapsedTime/60000);
            double sec = (elapsedTime - 60000*min) / 1000.0;
            return String.format("%dm%.3fs", min, sec);
        } else {
            return String.format("%.3fs", elapsedTime/1000.0);
        }
    }

    @Test
    public void testKryo () throws IOException {
        testTableSpeed(TYPE.kryo, "002");
    }

    @Test
    public void testAvro () throws IOException {
        testTableSpeed(TYPE.avro, "002");
    }
}
