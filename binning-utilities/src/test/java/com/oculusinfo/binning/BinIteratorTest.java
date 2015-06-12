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

import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import org.junit.Assert;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BinIteratorTest {
	private Point2D getBinCenter (TilePyramid binner,
	                              int level, int tileX, int tileY,
	                              int binX, int binY) {
		TileIndex tile = new TileIndex(level, tileX, tileY);
		BinIndex bin = new BinIndex(binX, binY);
		Rectangle2D area = binner.getBinBounds(tile, bin);
		return new Point2D.Double(area.getCenterX(), area.getCenterY());
	}



	@Test
	public void smallIteratorTest () {
		WebMercatorTilePyramid mercator = new WebMercatorTilePyramid();



		// Expected results
		Set<TileAndBinIndices> expectedResults = new HashSet<TileAndBinIndices>();
		// Bottom row of tiles
		TileIndex tile = new TileIndex(6, 7, 8);
		for (int x=250; x<256; ++x) {
			for (int y=5; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(6, 8, 8);
		for (int x=0; x<=5; ++x) {
			for (int y=5; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}

		// Top row of tiles
		tile = new TileIndex(6, 7, 9);
		for (int x=250; x<256; ++x) {
			for (int y=255; y>=250; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(6, 8, 9);
		for (int x=0; x<=5; ++x) {
			for (int y=255; y>=250; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}



		// Actual results
		Point2D ll = getBinCenter(mercator, 6, 7, 8, 250, 5);
		Point2D ur = getBinCenter(mercator, 6, 8, 9, 5, 250);

		List<TileAndBinIndices> results = new ArrayList<TileAndBinIndices>();
		BinIterator i = new BinIterator(mercator, 6,
		                                new Rectangle2D.Double(ll.getX(), ll.getY(),
		                                                       ur.getX()-ll.getX(),
		                                                       ur.getY()-ll.getY()));
		while (i.hasNext()) {
			results.add(i.next());
		}



		// Figure out differences
		Set<TileAndBinIndices> missing = new HashSet<TileAndBinIndices>(expectedResults);
		missing.removeAll(results);
		Set<TileAndBinIndices> extra = new HashSet<TileAndBinIndices>(results);
		extra.removeAll(expectedResults);

		Assert.assertEquals(expectedResults.size(), results.size());
		Assert.assertTrue(missing.isEmpty());
		Assert.assertTrue(extra.isEmpty());
	}



	@Test
	public void largeIteratorTest () {
		WebMercatorTilePyramid mercator = new WebMercatorTilePyramid();



		// Expected results
		Set<TileAndBinIndices> expectedResults = new HashSet<TileAndBinIndices>();
		// Bottom row of tiles
		TileIndex tile = new TileIndex(5, 4, 6);
		for (int x=250; x<256; ++x) {
			for (int y=5; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(5, 5, 6);
		for (int x=0; x<256; ++x) {
			for (int y=5; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(5, 6, 6);
		for (int x=0; x<=5; ++x) {
			for (int y=5; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}

		// Middle row of tiles
		tile = new TileIndex(5, 4, 7);
		for (int x=250; x<256; ++x) {
			for (int y=255; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(5, 5, 7);
		for (int x=0; x<256; ++x) {
			for (int y=255; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(5, 6, 7);
		for (int x=0; x<=5; ++x) {
			for (int y=255; y>=0; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}

		// Top row of tiles
		tile = new TileIndex(5, 4, 8);
		for (int x=250; x<256; ++x) {
			for (int y=255; y>=250; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(5, 5, 8);
		for (int x=0; x<256; ++x) {
			for (int y=255; y>=250; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}
		tile = new TileIndex(5, 6, 8);
		for (int x=0; x<=5; ++x) {
			for (int y=255; y>=250; --y) {
				expectedResults.add(new TileAndBinIndices(tile, new BinIndex(x, y)));
			}
		}



		// Actual results
		Point2D ll = getBinCenter(mercator, 5, 4, 6, 250, 5);
		Point2D ur = getBinCenter(mercator, 5, 6, 8, 5, 250);

		List<TileAndBinIndices> results = new ArrayList<TileAndBinIndices>();
		BinIterator i = new BinIterator(mercator, 5,
		                                new Rectangle2D.Double(ll.getX(), ll.getY(),
		                                                       ur.getX()-ll.getX(),
		                                                       ur.getY()-ll.getY()));
		while (i.hasNext()) {
			results.add(i.next());
		}



		// Figure out differences
		Set<TileAndBinIndices> missing = new HashSet<TileAndBinIndices>(expectedResults);
		missing.removeAll(results);
		Set<TileAndBinIndices> extra = new HashSet<TileAndBinIndices>(results);
		extra.removeAll(expectedResults);

		Assert.assertEquals(expectedResults.size(), results.size());
		Assert.assertTrue(missing.isEmpty());
		Assert.assertTrue(extra.isEmpty());
	}
}
