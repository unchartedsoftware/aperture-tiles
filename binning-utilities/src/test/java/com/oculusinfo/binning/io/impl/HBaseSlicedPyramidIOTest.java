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

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;
import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Ignore
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
		int iterations = 1;
		int slices = 53;
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> ks = new KryoSerializer<List<Integer>>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)));
		String table = "nycTaxiHeatmap_sw2015_weekly";
		List<TileIndex> indices = Arrays.asList(new TileIndex(9,151,319));

		System.out.println("Reading full tile");
		TileData<List<Integer>> full = null;
		long startFull = System.currentTimeMillis();
		for (int i=0; i<iterations; ++i) {
			for (int s=0; s<slices; ++s) {
				full = io.readTiles(table, ks, indices).get(0);
			}
		}
		long endFull = System.currentTimeMillis();

		System.out.println("Checking slice contents");
		for (int s=0; s<slices; ++s) {
			TileData<List<Integer>> slice = io.readTiles(table + "["+s+"]", ks, indices).get(0);
			for (int x=0; x<full.getDefinition().getXBins(); ++x) {
				for (int y=0; y<full.getDefinition().getYBins(); ++y) {
					List<Integer> fullBin = full.getBin(x, y);
					List<Integer> sliceBin = slice.getBin(x, y);
					if (null == fullBin || 0 == fullBin.size()) {
						Assert.assertTrue(null == sliceBin || 0 == sliceBin.size());
					} else {
						Assert.assertEquals(slices, fullBin.size());
						Assert.assertEquals(1, sliceBin.size());
						Assert.assertEquals(fullBin.get(s), sliceBin.get(0));
					}
				}
			}
		}

		System.out.println("Reading sliced tiles");
		long startSlice = System.currentTimeMillis();
		for (int i=0; i<iterations; ++i) {
			for (int s=0; s<slices; ++s) {
				io.readTiles(table + "["+s+"]", ks, indices);
			}
		}
		long endSlice = System.currentTimeMillis();

		System.out.println("Reading unsliced tiles");
		indices = Arrays.asList(new TileIndex(0, 0, 0));
		TileSerializer<Double> dSerializer = new PrimitiveAvroSerializer<>(Double.class, CodecFactory.bzip2Codec());
		String singleSliceTable = "twitter-ebola-p1-heatmap";
		long startSingle = System.currentTimeMillis();
		for (int i=0; i<iterations; ++i) {
			for (int s=0; s<slices; ++s) {
				io.readTiles(singleSliceTable, dSerializer, indices);
			}
		}
		long endSingle = System.currentTimeMillis();

		System.out.println("Time for full tile: " + ((endFull - startFull) / 1000.0));
		System.out.println("Time for slices: "+((endSlice-startSlice)/1000.0));
		System.out.println("Time for unsliced table: "+((endSingle-startSingle)/1000.0));
	}

	private <S, T> Pair<S, T> p(S s, T t) {
		return new Pair<S, T>(s, t);
	}

	private void testRange (int start, int end, Pair<Integer, Integer>... expectedRangeElements) {
		List<Pair<Integer, Integer>> actualRangeElements = HBaseSlicedPyramidIO.decomposeRange(start, end);
		Assert.assertEquals(expectedRangeElements.length, actualRangeElements.size());
		for (int n=0; n<expectedRangeElements.length; ++n)
			Assert.assertEquals(expectedRangeElements[n], actualRangeElements.get(n));
	}

	@Test
	public void testRangeDecomposition () {
		testRange(0, 15, p(0, 15));
		testRange(1, 15, p(1, 1), p(2, 3), p(4, 7), p(8, 15));
		testRange(16, 16, p(16, 16));
		testRange(16, 17, p(16, 17));
		testRange(16, 18, p(16, 17), p(18, 18));
		testRange(16, 30, p(16, 23), p(24, 27), p(28, 29), p(30, 30));
		testRange(22, 84, p(22, 23), p(24, 31), p(32, 63), p(64, 79), p(80, 83), p(84, 84));
		testRange(5, 5, p(5, 5));

		for (int start=0; start<128; ++start) {
			for (int end=start; end<128; ++end) {
				// We make sure of five things for each range set:
				//   (1) the endpoints from one sub-range to the next match
				//   (2) the total range of the sub-ranges is the intended total range
				//   (3) No more than two sub-ranges in a row are the same size (if they are, they should be
				//       on either side of our algorithm's 'mid-point', but we can't check that)
				//   (4) All ranges start on the proper boundary modulus.
				//   (5) There are no empty ranges.
				List<Pair<Integer, Integer>> rangeElts = HBaseSlicedPyramidIO.decomposeRange(start, end);
				Assert.assertTrue(rangeElts.size() > 0);  // Check 5, part of check 2
				int duplicateSizes = 0;
				int lastSize = 0;
				int lastEnd = start-1;                                // Part of check 2
				for (Pair<Integer, Integer> elt: rangeElts) {
					int size = elt.getSecond() - elt.getFirst() + 1;
					Assert.assertTrue(size > 0);                      // Check 5
					Assert.assertTrue(0 == (elt.getFirst() % size));  // Check 4
					if (lastSize == size) ++ duplicateSizes;
					lastSize = size;
					Assert.assertEquals(lastEnd+1, elt.getFirst()+0); // Check 1, part of check 2
					lastEnd = elt.getSecond();
				}
				Assert.assertEquals(end, lastEnd);                    // Part of check 2
				Assert.assertTrue(duplicateSizes <= 1);               // Check 3
			}
		}
	}

	@Test
	public void testSliceDecompositionVsSingleSlices () throws Exception {
		String table = "hbsioTest";
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> serializer = new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)));
	}
}
