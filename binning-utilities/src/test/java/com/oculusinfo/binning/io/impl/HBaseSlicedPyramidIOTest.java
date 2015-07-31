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
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;
import org.apache.avro.file.CodecFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
		String table = "nycTaxiHeatmap_sw2015_weekly";
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> serializer = new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)));

		TileSerializer<List<Integer>> ks = new KryoSerializer<List<Integer>>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)));
		TileIndex index = new TileIndex(9, 151, 319);
		List<TileIndex> indices = Arrays.asList(index);

		System.out.println("Reading full tile");
		long fullStart = System.nanoTime();
		TileData<List<Integer>> full = io.readTiles(table, ks, indices).get(0);
		long fullEnd = System.nanoTime();

		System.out.println("Reading 7-27, pyramided");
		long pyramidedStart = System.nanoTime();
		TileData<List<Integer>> pyramided = io.readTiles(table+"[7-27]", ks, indices).get(0);
		long pyramidedEnd = System.nanoTime();

		System.out.println("Reading 7-27, individual");
		io.setPyramidding(false);
		long individualStart = System.nanoTime();
		TileData<List<Integer>> individual = io.readTiles(table+"[7-27]", ks, indices).get(0);
		long individualEnd = System.nanoTime();

		double fullTime = (fullEnd - fullStart) / 1000000.0;
		double pTime = (pyramidedEnd - pyramidedStart) / 1000000.0;
		double iTime = (individualEnd - individualStart) / 1000000.0;
		System.out.println("Full time: " + fullTime + "ms");
		System.out.println("Pyramided time: "+pTime+"ms");
		System.out.println("One-by-one time: "+iTime+"ms");
		// Check contents
		String pDiffs = "";
		String iDiffs = "";
		for (int x=0; x<index.getXBins(); ++x) {
			for (int y=0; y<index.getYBins(); ++y) {
				List<Integer> fullBin = full.getBin(x, y);
				List<Integer> pBin = pyramided.getBin(x, y);
				List<Integer> iBin = individual.getBin(x, y);

				if (null == fullBin || 28 > fullBin.size()) {
					if (null != pBin && pBin.size() > 0)  pDiffs += "["+x+", "+y+"]: Not null\n";
					if (null != iBin && iBin.size() > 0)  iDiffs += "["+x+", "+y+"]: Not null\n";
				} else {
					if (null == pBin || pBin.size() != 21) pDiffs += "["+x+", "+y+"]: null\n";
					if (null == iBin || iBin.size() != 21) iDiffs += "["+x+", "+y+"]: null\n";
					for (int z=7; z<=27; ++z) {
						int iVal = iBin.get(z-7);
						int pVal = pBin.get(z-7);
						int fVal = fullBin.get(z);
						if (pVal != fVal)
							pDiffs += "["+x+", "+y+", "+z+"]\n";
						if (iVal != fVal)
							iDiffs += "["+x+", "+y+", "+z+"]\n";
					}
				}
			}
		}
		Assert.assertTrue(pDiffs, 0 == pDiffs.length());
		Assert.assertTrue(iDiffs, 0 == iDiffs.length());
	}


	@Test
	public void testDenseSlicedTiles () throws Exception {
		TileIndex index = new TileIndex(0, 0, 0, 4, 4);
		TileData<List<Integer>> tile = new DenseTileData<>(index);
		for (int x=0; x<4; ++x) {
			for (int y=0; y<4; ++y) {
				List<Integer> bin = new ArrayList<>();
				for (int z=0; z<4; ++z) {
					bin.add(x+y+z);
				}
				tile.setBin(x, y, bin);
			}
		}

		String table = "dense-bucket-test";
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> serializer = new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)));

		io.initializeForWrite(table);
		io.writeTiles(table, serializer, Arrays.asList(tile));

		List<TileIndex> indices = Arrays.asList(index);
		TileData<List<Integer>> full = io.readTiles(table, serializer, indices).get(0);
		TileData<List<Integer>> s0 = io.readTiles(table+"[0]", serializer, indices).get(0);
		TileData<List<Integer>> s1 = io.readTiles(table+"[1]", serializer, indices).get(0);
		TileData<List<Integer>> s2 = io.readTiles(table+"[2]", serializer, indices).get(0);
		TileData<List<Integer>> s3 = io.readTiles(table+"[3]", serializer, indices).get(0);
		List<TileData<List<Integer>>> slices = Arrays.asList(s0, s1, s2, s3);
		TileData<List<Integer>> s01 = io.readTiles(table+"[0-1]", serializer, indices).get(0);
		TileData<List<Integer>> s23 = io.readTiles(table+"[2-3]", serializer, indices).get(0);
		List<TileData<List<Integer>>> slicePairs = Arrays.asList(s01, s23);

		for (int x=0; x<4; ++x) {
			for (int y=0; y<4; ++y) {
				for (int z=0; z<4; ++z) {
					Assert.assertEquals(tile.getBin(x, y).get(z), full.getBin(x, y).get(z));
					Assert.assertEquals(tile.getBin(x, y).get(z), slices.get(z).getBin(x, y).get(0));
					Assert.assertEquals(tile.getBin(x, y).get(z), slicePairs.get(z/2).getBin(x, y).get(z%2));
				}
			}
		}
	}

	// List out all indices in the given weird tile set, for use in the next test
	@Ignore
	public void listIndices () throws Exception {
		String tableName = "nycHeatmap_sw2015_sliced_DELETEME";
		String zookeeperQuorum = "hadoop-s1";
		String zookeeperPort = "2181";
		String hbaseMaster = "hadoop-s1:60000";

		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", zookeeperQuorum);
		config.set("hbase.zookeeper.property.clientPort", zookeeperPort);
		config.set("hbase.master", hbaseMaster);
		config.set("hbase.client.keyvalue.maxsize", "0");
		Connection connection = ConnectionFactory.createConnection(config);
		Admin admin = connection.getAdmin();
		Table table = connection.getTable(TableName.valueOf(tableName));

		System.out.println("scanning full table:");
		Scan scan = new Scan();
		scan.setFilter(new FirstKeyOnlyFilter());
		ResultScanner scanner = table.getScanner(scan);
		for (Result rr : scanner) {
			byte[] key = rr.getRow();
			String keyName = new String(key);
			if (keyName.equals("metadata")) continue;
			String[] parts = keyName.split(",");
			int level = Integer.parseInt(parts[0]);
			while (parts[1].startsWith("0")) parts[1] = parts[1].substring(1);
			int x = Integer.parseInt(parts[1]);
			while (parts[2].startsWith("0")) parts[2] = parts[2].substring(1);
			int y = Integer.parseInt(parts[2]);
//			if (level > 12) {
//				int tx = x;
//				int ty = y;
//				int tl = level;
//				while (tl > 12) {
//					tl--;
//					tx = (int) Math.floor(tx/2.0);
//					ty = (int) Math.floor(ty/2.0);
//				}
//				if (tx == 1206 && ty == 2556) {
//					System.out.println(keyName);
//				}
//			}
			if (level > 14) System.out.println(keyName);
		}
	}

	private <T> Map<TileIndex, TileData<T>> getTileMap (PyramidIO io, TileSerializer<T> serializer,
														String table, Integer slice, List<TileIndex> indices) throws Exception {
		if (null != slice) {
			table = table + "[" + slice + "]";
		}
		List<TileData<T>> tiles = io.readTiles(table, serializer, indices);
		Map<TileIndex, TileData<T>> result = new HashMap<>();
		for (TileData<T> tile: tiles) {
			result.put(tile.getDefinition(), tile);
		}
		return result;
	}
	List<Integer> addLists (List<Integer> a, List<Integer> b) {
		if (null == a || 0 == a.size()) return b;
		if (null == b || 0 == b.size()) return a;
		List<Integer> c = new ArrayList<>();
		int N = Math.max(a.size(), b.size());
		for (int n=0; n<N; ++n) {
			int v = 0;
			if (n < a.size()) v += a.get(n);
			if (n < b.size()) v += b.get(n);
			c.add(v);
		}
		return c;
	}

	private Map<Integer, Map<TileIndex, TileData<List<Integer>>>> data12_1206_2556 (PyramidIO io, TileSerializer<List<Integer>> serializer, String table, int slice) throws Exception {
		Map<Integer, Map<TileIndex, TileData<List<Integer>>>> result = new HashMap<>();
		result.put(12, getTileMap(io, serializer, table, slice,
			                      Arrays.asList(new TileIndex(12, 1206, 2556))));
		result.put(13, getTileMap(io, serializer, table, slice,
			                      Arrays.asList(new TileIndex(13,2412,5112),
									            new TileIndex(13,2412,5113),
									            new TileIndex(13,2413,5112),
									            new TileIndex(13,2413,5113))));
		result.put(14, getTileMap(io, serializer, table, slice,
			                      Arrays.asList(new TileIndex(14,4824,10224),
									            new TileIndex(14,4824,10225),
									            new TileIndex(14,4824,10226),
									            new TileIndex(14,4824,10227),
									            new TileIndex(14,4825,10225),
									            new TileIndex(14,4825,10226),
									            new TileIndex(14,4825,10227),
									            new TileIndex(14,4827,10224),
									            new TileIndex(14,4827,10225),
									            new TileIndex(14,4827,10226),
									            new TileIndex(14,4827,10227))));

		return result;
	}

	private Map<Integer, Map<TileIndex, TileData<List<Integer>>>> dataFull (PyramidIO io, TileSerializer<List<Integer>> serializer, String table, int slice) throws Exception {
		Map<TileIndex, TileData<List<Integer>>> level8 =
			getTileMap(io, serializer, table, slice,
				Arrays.asList(new TileIndex(8, 75, 159)));
		Map<TileIndex, TileData<List<Integer>>> level9 =
			getTileMap(io, serializer, table, slice,
				Arrays.asList(new TileIndex(9, 150, 319), new TileIndex(9, 151, 319)));
		Map<TileIndex, TileData<List<Integer>>> level10 =
			getTileMap(io, serializer, table, slice,
				Arrays.asList(new TileIndex(10, 300, 638), new TileIndex(10, 301, 638),
					new TileIndex(10, 301, 639), new TileIndex(10, 302, 638),
					new TileIndex(10, 302, 639)));
		Map<TileIndex, TileData<List<Integer>>> level11 =
			getTileMap(io, serializer, table, slice,
				Arrays.asList(new TileIndex(11, 601, 1276), new TileIndex(11, 601, 1277),
					new TileIndex(11, 602, 1276), new TileIndex(11, 602, 1277),
					new TileIndex(11, 602, 1278), new TileIndex(11, 603, 1276),
					new TileIndex(11, 603, 1277), new TileIndex(11, 603, 1278),
					new TileIndex(11, 603, 1279), new TileIndex(11, 604, 1276),
					new TileIndex(11, 604, 1277), new TileIndex(11, 604, 1278),
					new TileIndex(11, 604, 1279)));
		Map<TileIndex, TileData<List<Integer>>> level12 =
			getTileMap(io, serializer, table, slice,
				Arrays.asList(new TileIndex(12, 1202, 2552), new TileIndex(12, 1202, 2553),
					new TileIndex(12, 1203, 2552), new TileIndex(12, 1203, 2553),
					new TileIndex(12, 1203, 2554), new TileIndex(12, 1203, 2555),
					new TileIndex(12, 1204, 2552), new TileIndex(12, 1204, 2553),
					new TileIndex(12, 1204, 2554), new TileIndex(12, 1204, 2555),
					new TileIndex(12, 1205, 2554), new TileIndex(12, 1205, 2555),
					new TileIndex(12, 1205, 2556), new TileIndex(12, 1206, 2553),
					new TileIndex(12, 1206, 2554), new TileIndex(12, 1206, 2555),
					new TileIndex(12, 1206, 2556), new TileIndex(12, 1206, 2557),
					new TileIndex(12, 1206, 2558), new TileIndex(12, 1207, 2553),
					new TileIndex(12, 1207, 2554), new TileIndex(12, 1207, 2555),
					new TileIndex(12, 1207, 2556), new TileIndex(12, 1207, 2557),
					new TileIndex(12, 1207, 2558), new TileIndex(12, 1208, 2553),
					new TileIndex(12, 1208, 2554), new TileIndex(12, 1208, 2555),
					new TileIndex(12, 1208, 2556), new TileIndex(12, 1208, 2557),
					new TileIndex(12, 1208, 2558), new TileIndex(12, 1209, 2554),
					new TileIndex(12, 1209, 2555), new TileIndex(12, 1209, 2556)));
		Map<TileIndex, TileData<List<Integer>>> level13 =
			getTileMap(io, serializer, table, slice,
				Arrays.asList(
					new TileIndex(13, 2405, 5104), new TileIndex(13, 2405, 5105),
					new TileIndex(13, 2405, 5106), new TileIndex(13, 2406, 5104),
					new TileIndex(13, 2406, 5105), new TileIndex(13, 2406, 5106),
					new TileIndex(13, 2406, 5107), new TileIndex(13, 2406, 5108),
					new TileIndex(13, 2406, 5109), new TileIndex(13, 2407, 5105),
					new TileIndex(13, 2407, 5106), new TileIndex(13, 2407, 5107),
					new TileIndex(13, 2407, 5108), new TileIndex(13, 2407, 5109),
					new TileIndex(13, 2407, 5110), new TileIndex(13, 2408, 5105),
					new TileIndex(13, 2408, 5106), new TileIndex(13, 2408, 5107),
					new TileIndex(13, 2408, 5108), new TileIndex(13, 2408, 5109),
					new TileIndex(13, 2408, 5110), new TileIndex(13, 2409, 5107),
					new TileIndex(13, 2409, 5108), new TileIndex(13, 2409, 5109),
					new TileIndex(13, 2409, 5110), new TileIndex(13, 2410, 5108),
					new TileIndex(13, 2410, 5109), new TileIndex(13, 2410, 5110),
					new TileIndex(13, 2411, 5108), new TileIndex(13, 2411, 5109),
					new TileIndex(13, 2411, 5110), new TileIndex(13, 2411, 5111),
					new TileIndex(13, 2411, 5112), new TileIndex(13, 2411, 5113),
					new TileIndex(13, 2412, 5107), new TileIndex(13, 2412, 5108),
					new TileIndex(13, 2412, 5109), new TileIndex(13, 2412, 5110),
					new TileIndex(13, 2412, 5111), new TileIndex(13, 2412, 5112),
					new TileIndex(13, 2412, 5113), new TileIndex(13, 2412, 5114),
					new TileIndex(13, 2412, 5115), new TileIndex(13, 2413, 5107),
					new TileIndex(13, 2413, 5108), new TileIndex(13, 2413, 5109),
					new TileIndex(13, 2413, 5110), new TileIndex(13, 2413, 5111),
					new TileIndex(13, 2413, 5112), new TileIndex(13, 2413, 5113),
					new TileIndex(13, 2413, 5114), new TileIndex(13, 2413, 5115),
					new TileIndex(13, 2413, 5116), new TileIndex(13, 2413, 5117),
					new TileIndex(13, 2414, 5107), new TileIndex(13, 2414, 5109),
					new TileIndex(13, 2414, 5110), new TileIndex(13, 2414, 5111),
					new TileIndex(13, 2414, 5112), new TileIndex(13, 2414, 5113),
					new TileIndex(13, 2414, 5114), new TileIndex(13, 2414, 5115),
					new TileIndex(13, 2414, 5116), new TileIndex(13, 2414, 5117),
					new TileIndex(13, 2415, 5107), new TileIndex(13, 2415, 5109),
					new TileIndex(13, 2415, 5110), new TileIndex(13, 2415, 5111),
					new TileIndex(13, 2415, 5112), new TileIndex(13, 2415, 5113),
					new TileIndex(13, 2415, 5114), new TileIndex(13, 2415, 5115),
					new TileIndex(13, 2415, 5116), new TileIndex(13, 2415, 5117),
					new TileIndex(13, 2416, 5107), new TileIndex(13, 2416, 5108),
					new TileIndex(13, 2416, 5109), new TileIndex(13, 2416, 5110),
					new TileIndex(13, 2416, 5111), new TileIndex(13, 2416, 5112),
					new TileIndex(13, 2416, 5113), new TileIndex(13, 2416, 5114),
					new TileIndex(13, 2416, 5115), new TileIndex(13, 2416, 5116),
					new TileIndex(13, 2416, 5117), new TileIndex(13, 2417, 5107),
					new TileIndex(13, 2417, 5108), new TileIndex(13, 2417, 5109),
					new TileIndex(13, 2417, 5110), new TileIndex(13, 2417, 5111),
					new TileIndex(13, 2417, 5112), new TileIndex(13, 2417, 5113),
					new TileIndex(13, 2417, 5114), new TileIndex(13, 2418, 5108),
					new TileIndex(13, 2418, 5109), new TileIndex(13, 2418, 5110),
					new TileIndex(13, 2418, 5111), new TileIndex(13, 2418, 5112),
					new TileIndex(13, 2418, 5113)
				));

		Map<Integer, Map<TileIndex, TileData<List<Integer>>>> allTiles = new HashMap<>();
		allTiles.put(8, level8);
		allTiles.put(9, level9);
		allTiles.put(10, level10);
		allTiles.put(11, level11);
		allTiles.put(12, level12);
		allTiles.put(13, level13);

		return allTiles;
	}

	// Make sure the various levels match in a weird tile set - i.e., make sure there are no lower-level holes that
	// don't also exist at upper levels.
	@Test
	public void testWeirdTable () throws Exception {
		String table = "nycHeatmap_sw2015_sliced_DELETEME";
		HBaseSlicedPyramidIO io = new HBaseSlicedPyramidIO("hadoop-s1", "2181", "hadoop-s1:60000");
		TileSerializer<List<Integer>> serializer = new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)));

		for (int slice = 0; slice < 53; ++slice) {

			for (int t = 8; t < 13; ++t) {
				Map<Integer, Map<TileIndex, TileData<List<Integer>>>> allTiles = dataFull(io, serializer, table, slice);

				for (TileData<List<Integer>> topTile : allTiles.get(t).values()) {
					TileIndex topIdx = topTile.getDefinition();
					for (int b = t + 1; b < 14; ++b) {
						int lf = 1 << (b - t);
						for (int x = 0; x < 256; ++x) {
							for (int y = 0; y < 256; ++y) {
								List<Integer> expected = topTile.getBin(x, y);
								List<Integer> actual = null;
								BinIndex baseTopUBin = TileIndex.tileBinIndexToUniversalBinIndex(topIdx, new BinIndex(x, y));
								BinIndex baseUBin = new BinIndex(baseTopUBin.getX() * lf, baseTopUBin.getY() * lf);
								TileIndex baseTile = new TileIndex(b, 0, 0);
								for (int rx = 0; rx < lf; ++rx) {
									for (int ry = 0; ry < lf; ++ry) {
										BinIndex bin = new BinIndex(baseUBin.getX() + rx, baseUBin.getY() + ry);
										TileAndBinIndices tbi = TileIndex.universalBinIndexToTileBinIndex(baseTile, bin);
										TileData<List<Integer>> subTile = allTiles.get(b).get(tbi.getTile());
										if (null != subTile) {
											List<Integer> subBin = subTile.getBin(tbi.getBin().getX(), tbi.getBin().getY());
											actual = addLists(actual, subBin);
										}
									}
								}
								if (null == expected || 0 == expected.size()) {
									Assert.assertTrue(null == actual || 0 == actual.size());
								} else {
									Assert.assertEquals(expected.size(), actual.size());
									for (int n = 0; n < expected.size(); ++n)
										Assert.assertEquals(expected.get(n), actual.get(n));
								}
							}
						}
					}
				}
			}
		}
	}
}
