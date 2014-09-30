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
package com.oculusinfo.binning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of the Tile and Bin classes
 * 
 * @author nkronenfeld
 */
public class IndexTests {
	@Test
	public void testTileStringification () {
		TileIndex t = new TileIndex(0, 1, 2, 3, 4);
		String ts = t.toString();
		Assert.assertEquals("[1 / 3, 2 / 4, lvl 0]", ts);
		TileIndex t2 = TileIndex.fromString(ts);
		Assert.assertEquals(t, t2);

		t = new TileIndex(9, 45, 71, 512, 1024);
		ts = t.toString();
		Assert.assertEquals("[45 / 512, 71 / 1024, lvl 9]", ts);
		t2 = TileIndex.fromString(ts);
		Assert.assertEquals(t,  t2);
	}

	@Test
	public void testBinStringification () {
		BinIndex b = new BinIndex(23, 41);
		String bs = b.toString();
		Assert.assertEquals("[23, 41]", bs);
		BinIndex b2 = BinIndex.fromString(bs);
		Assert.assertEquals(b, b2);
	}


	@Test
	public void testTileComparison () {
		TileIndex t0 = new TileIndex(5, 12, 34);
		TileIndex t1 = new TileIndex(6, 12, 34);
		Assert.assertTrue(-1 == t0.compareTo(t1));
		Assert.assertTrue(1 == t1.compareTo(t0));

		t0 = new TileIndex(5, 12, 34);
		t1 = new TileIndex(5, 13, 34);
		Assert.assertTrue(-1 == t0.compareTo(t1));
		Assert.assertTrue(1 == t1.compareTo(t0));

		t0 = new TileIndex(5, 12, 34);
		t1 = new TileIndex(5, 12, 35);
		Assert.assertTrue(-1 == t0.compareTo(t1));
		Assert.assertTrue(1 == t1.compareTo(t0));

		t0 = new TileIndex(5, 12, 34);
		t1 = new TileIndex(5, 12, 34);
		Assert.assertTrue(0 == t0.compareTo(t1));
		Assert.assertTrue(0 == t1.compareTo(t0));

		t0 = new TileIndex(5, 12, 34);
		t1 = new TileIndex(5, 11, 35);
		Assert.assertTrue(-1 == t0.compareTo(t1));
		Assert.assertTrue(1 == t1.compareTo(t0));

		List<TileIndex> tiles = new ArrayList<TileIndex>();
		tiles.add(new TileIndex(4, 0, 0));
		tiles.add(new TileIndex(4, 0, 1));
		tiles.add(new TileIndex(3, 0, 1));
		tiles.add(new TileIndex(3, 1, 1));
		tiles.add(new TileIndex(4, 1, 1));
		tiles.add(new TileIndex(3, 0, 0));
		tiles.add(new TileIndex(3, 1, 0));
		tiles.add(new TileIndex(4, 1, 0));

		Collections.sort(tiles);
		Assert.assertEquals(new TileIndex(3, 0, 0), tiles.get(0));
		Assert.assertEquals(new TileIndex(3, 1, 0), tiles.get(1));
		Assert.assertEquals(new TileIndex(3, 0, 1), tiles.get(2));
		Assert.assertEquals(new TileIndex(3, 1, 1), tiles.get(3));
		Assert.assertEquals(new TileIndex(4, 0, 0), tiles.get(4));
		Assert.assertEquals(new TileIndex(4, 1, 0), tiles.get(5));
		Assert.assertEquals(new TileIndex(4, 0, 1), tiles.get(6));
		Assert.assertEquals(new TileIndex(4, 1, 1), tiles.get(7));
	}

	@Test
	public void testBinComparison () {
		BinIndex b0 = new BinIndex(0, 0);
		BinIndex b1 = new BinIndex(1, 0);
		Assert.assertTrue(-1 == b0.compareTo(b1));
		Assert.assertTrue(1 == b1.compareTo(b0));

		b0 = new BinIndex(0, 0);
		b1 = new BinIndex(0, 1);
		Assert.assertTrue(-1 == b0.compareTo(b1));
		Assert.assertTrue(1 == b1.compareTo(b0));

		b0 = new BinIndex(0, 0);
		b1 = new BinIndex(0, 0);
		Assert.assertTrue(0 == b0.compareTo(b1));
		Assert.assertTrue(0 == b1.compareTo(b0));

		List<BinIndex> bins = new ArrayList<BinIndex>();
		bins.add(new BinIndex(2, 1));
		bins.add(new BinIndex(2, 0));
		bins.add(new BinIndex(0, 1));
		bins.add(new BinIndex(1, 1));
		bins.add(new BinIndex(1, 2));
		bins.add(new BinIndex(1, 0));
		bins.add(new BinIndex(0, 2));
		bins.add(new BinIndex(0, 0));
		bins.add(new BinIndex(2, 2));

		Collections.sort(bins);
		Assert.assertEquals(new BinIndex(0, 0), bins.get(0));
		Assert.assertEquals(new BinIndex(1, 0), bins.get(1));
		Assert.assertEquals(new BinIndex(2, 0), bins.get(2));
		Assert.assertEquals(new BinIndex(0, 1), bins.get(3));
		Assert.assertEquals(new BinIndex(1, 1), bins.get(4));
		Assert.assertEquals(new BinIndex(2, 1), bins.get(5));
		Assert.assertEquals(new BinIndex(0, 2), bins.get(6));
		Assert.assertEquals(new BinIndex(1, 2), bins.get(7));
		Assert.assertEquals(new BinIndex(2, 2), bins.get(8));
	}

	private void testTile (TileIndex tile, int binX, int binY, int univX, int univY) {
		BinIndex tileBin = new BinIndex(binX, binY);
		BinIndex universalBin = new BinIndex(univX, univY);

		Assert.assertEquals(universalBin,
		                    TileIndex.tileBinIndexToUniversalBinIndex(tile, tileBin));
		Assert.assertEquals(new TileAndBinIndices(tile, tileBin),
		                    TileIndex.universalBinIndexToTileBinIndex(tile, universalBin));
	}
	@Test
	public void testTileUniversalConversion () {
		TileIndex tile = new TileIndex(0, 0, 0, 256, 256);
		testTile(tile, 0, 0, 0, 0);
		testTile(tile, 0, 255, 0, 255);
		testTile(tile, 255, 0, 255, 0);
		testTile(tile, 255, 255, 255, 255);


		tile = new TileIndex(1, 0, 0, 256, 256);
		testTile(tile, 0, 0, 0, 256);
		testTile(tile, 0, 255, 0, 511);
		testTile(tile, 255, 0, 255, 256);
		testTile(tile, 255, 255, 255, 511);

		tile = new TileIndex(1, 0, 1, 256, 256);
		testTile(tile, 0, 0, 0, 0);
		testTile(tile, 0, 255, 0, 255);
		testTile(tile, 255, 0, 255, 0);
		testTile(tile, 255, 255, 255, 255);

		tile = new TileIndex(1, 1, 0, 256, 256);
		testTile(tile, 0, 0, 256, 256);
		testTile(tile, 0, 255, 256, 511);
		testTile(tile, 255, 0, 511, 256);
		testTile(tile, 255, 255, 511, 511);

		tile = new TileIndex(1, 1, 1, 256, 256);
		testTile(tile, 0, 0, 256, 0);
		testTile(tile, 0, 255, 256, 255);
		testTile(tile, 255, 0, 511, 0);
		testTile(tile, 255, 255, 511, 255);
	}
}
