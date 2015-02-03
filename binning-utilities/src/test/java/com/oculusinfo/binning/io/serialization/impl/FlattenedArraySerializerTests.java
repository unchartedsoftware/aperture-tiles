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


import com.oculusinfo.binning.*;
import com.oculusinfo.binning.io.serialization.ConstantMap;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.Pair;
import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

// Test that flattened array serialization through avro works with dense and sparse tiles.
public class FlattenedArraySerializerTests {
    @Test
    public void testRoundTripConstantSizeDenseArray () throws Exception {
        TileIndex index = new TileIndex(3, 4, 5, 4, 4);
        TileData<List<Integer>> input = new DenseTileData<>(index);
        for (int x = 0; x < index.getXBins(); ++x) {
            for (int y = 0; y < index.getYBins(); ++y) {
                List<Integer> value = new ArrayList<>();
                for (int z = 0; z < 4; ++ z) {
                    value.add(x+4*y+z);
                }
                input.setBin(x, y, value);
            }
        }

        TileSerializer<List<Integer>> serializer = new PrimitiveDenseFlatArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(input, baos);
        baos.flush();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TileData<List<Integer>> output = serializer.deserialize(index, bais);

        Assert.assertEquals(input.getDefinition(), output.getDefinition());
        for (int x = 0; x < index.getXBins(); ++x) {
            for (int y = 0; y < index.getYBins(); ++y) {
                List<Integer> inVal = input.getBin(x, y);
                List<Integer> outVal = output.getBin(x, y);
                Assert.assertEquals(inVal.size(), outVal.size());
                for (int z = 0; z < inVal.size(); ++z) {
                    Assert.assertEquals(inVal.get(z), outVal.get(z));
                }
            }
        }
    }

    @Test
    public void testRoundTripVariableSizeDenseArray () throws Exception {
        TileIndex index = new TileIndex(3, 4, 5, 4, 4);
        TileData<List<Integer>> input = new DenseTileData<>(index);
        for (int x = 0; x < index.getXBins(); ++x) {
            for (int y = 0; y < index.getYBins(); ++y) {
                List<Integer> value = new ArrayList<>();
                for (int z = 0; z < x+y+1; ++ z) {
                    value.add(x+4*y+z);
                }
                input.setBin(x, y, value);
            }
        }

        TileSerializer<List<Integer>> serializer = new PrimitiveDenseFlatArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(input, baos);
        baos.flush();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TileData<List<Integer>> output = serializer.deserialize(index, bais);

        Assert.assertEquals(input.getDefinition(), output.getDefinition());
        for (int x = 0; x < index.getXBins(); ++x) {
            for (int y = 0; y < index.getYBins(); ++y) {
                List<Integer> inVal = input.getBin(x, y);
                List<Integer> outVal = output.getBin(x, y);
                Assert.assertEquals(inVal.size(), outVal.size());
                for (int z = 0; z < inVal.size(); ++z) {
                    Assert.assertEquals(inVal.get(z), outVal.get(z));
                }
            }
        }
    }

    private <T> Map<BinIndex, Map<Integer, T>> sparseToMap (TileData<Map<Integer, T>> tile) {
        SparseTileData<Map<Integer, T>> sparseTile = (SparseTileData<Map<Integer, T>>) tile;

        Map<BinIndex, Map<Integer, T>> result = new HashMap<>();
        Iterator<Pair<BinIndex, Map<Integer, T>>> i = sparseTile.getData();
        while (i.hasNext()) {
            Pair<BinIndex, Map<Integer, T>> p = i.next();
            result.put(p.getFirst(), p.getSecond());
        }

        return result;
    }

    @Test
    public void testRoundTripSparseArray () throws Exception {
        TileIndex index = new TileIndex(3, 4, 5, 4, 4);
        TileData<Map<Integer, Integer>> input = new SparseTileData<Map<Integer, Integer>>(index, new ConstantMap<Integer>(-3));
        for (int x = 0; x < index.getXBins(); ++x) {
            for (int y = 0; y < index.getYBins(); ++y) {
                if (0 == ((x + y) % 2)) {
                    Map<Integer, Integer> value = new HashMap<>();
                    for (int z = 0; z < x + y; z += 2) {
                        value.put(z, x + 4 * y + z);
                    }
                    input.setBin(x, y, value);
                }
            }
        }

        TileSerializer<Map<Integer, Integer>> serializer = new PrimitiveSparseFlatArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(input, baos);
        baos.flush();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TileData<Map<Integer, Integer>> output = serializer.deserialize(index, bais);

        Assert.assertEquals(input.getDefinition(), output.getDefinition());
        Map<BinIndex, Map<Integer, Integer>> sparseIn = sparseToMap(input);
        Map<BinIndex, Map<Integer, Integer>> sparseOut = sparseToMap(output);

        // Key sets won't be identical - there's one key in our input tile that has no contents, and it doesn't
        // (and shouldn't) show up in our output set.
        //
        // So, instead, we just test for equality of contents for each key present in either set.
        Set<BinIndex> keySet = new HashSet<>(sparseIn.keySet());
        keySet.addAll(sparseOut.keySet());
        for (BinIndex bin : sparseIn.keySet()) {
            Map<Integer, Integer> valIn = sparseIn.get(bin);
            Map<Integer, Integer> valOut = sparseOut.get(bin);

            Set<Integer> exValIn;
            if (null == valIn) exValIn = new HashSet<>();
            else exValIn = new HashSet<>(valIn.keySet());

            Set<Integer> exValOut;
            if (null == valOut) exValOut = new HashSet<>();
            else exValOut = new HashSet<>(valOut.keySet());

            Assert.assertEquals(exValIn.size(), exValOut.size());

            Set<Integer> valKeys = new HashSet<>(exValIn);
            valKeys.addAll(exValOut);
            for (Integer key : valKeys) {
                Assert.assertEquals(valIn.get(key), valOut.get(key));
            }
        }
    }
}
