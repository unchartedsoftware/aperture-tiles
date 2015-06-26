/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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
package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class HBaseSlicedPyramidIOTest {
	@Test
	public void testRoundRoundTripWhole () throws Exception {
		String table = "hbsioTest";
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> serializer = new PrimitiveArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());
		try {
			TileIndex index = new TileIndex(0, 0, 0, 1, 1);

			TileData<List<Integer>> data = new DenseTileData<>(index);
			data.setBin(0, 0, Arrays.asList(-0, -1, -2, -3, -4, -5, -6, -7));

			io.initializeForWrite(table);
			io.writeTiles(table, serializer, Arrays.asList(data));

			// Check each slice
			for (int i = 0; i < 8; ++i) {
				List<TileData<List<Integer>>> slice = io.readTiles(table + "[" + i + "]", serializer, Arrays.asList(index));
				Assert.assertEquals(1, slice.size());
				TileData<List<Integer>> tile = slice.get(0);
				List<Integer> bin = tile.getBin(0, 0);
				Assert.assertEquals(1, bin.size());
				Assert.assertEquals(-i, bin.get(0).intValue());
			}

			// Check the whole tile
			List<TileData<List<Integer>>> slice = io.readTiles(table, serializer, Arrays.asList(index));
			Assert.assertEquals(1, slice.size());
			TileData<List<Integer>> tile = slice.get(0);
			List<Integer> bin = tile.getBin(0, 0);
			Assert.assertEquals(8, bin.size());
			for (int i = 0; i < 8; ++i) {
				Assert.assertEquals(-i, bin.get(i).intValue());
			}
		} finally {
			io.dropTable(table);
		}
	}

	@Test
	public void testRelativeReadSpeed () throws Exception {
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> serializer = new PrimitiveArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());
		String table = "heatmapTimeDebug-sliced";
		TileIndex index = new TileIndex(0, 0, 0);
		List<TileIndex> indices = Arrays.asList(index);

		long startFull = System.currentTimeMillis();
		for (int i=0; i<256; ++i) {
			io.readTiles(table, serializer, indices);
		}
		long endFull = System.currentTimeMillis();

		long startSlice = System.currentTimeMillis();
		for (int i=0; i<256; ++i) {
			List<TileData<List<Integer>>> slices = io.readTiles(table + "["+i+"]", serializer, indices);
			TileData<List<Integer>> slice = slices.get(0);
			int bins = slice.getDefinition().getXBins();
		}
		long endSlice = System.currentTimeMillis();

		System.out.println("Time for full tile: " + ((endFull - startFull) / 1000.0));
		System.out.println("Time for slices: "+((endSlice-startSlice)/1000.0));
	}
}
