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
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;



public class MercatorBinningTests {
	private static final double EPSILON = 1E-12;
	// When dealing with numbers on the scale of the circumference of the globe
	// in meters, 1E-12 implies too many digits of precision. We really want
	// this relative, but JUnit doesn't give us that, so we satisfy ourselves by
	// just bumping it up the (approximately 6) significant digits of the
	// circumference.
	private static final double WORLD_SCALE_EPSILON = 1E-6;

	private WebMercatorTilePyramid _mercator;

	@Before
	public void setup () {
		_mercator = new WebMercatorTilePyramid();
	}

	@After
	public void teardown () {
		_mercator = null;
	}

	@Test
	public void testLevel0 () {
		TileIndex tile = new TileIndex(0, 0, 0);
		Rectangle2D tileBounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(-180.0, tileBounds.getMinX(), EPSILON);
		Assert.assertEquals(180.0, tileBounds.getMaxX(), EPSILON);
		Assert.assertEquals(-85.05112877980659, tileBounds.getMinY(), EPSILON);
		Assert.assertEquals(85.05112877980659, tileBounds.getMaxY(), EPSILON);
	}

	@Test
	public void testLevel1 () {
		TileIndex tile;
		Rectangle2D bounds;

		tile = new TileIndex(1, 0, 0);
		bounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(-180.0, bounds.getMinX(), EPSILON);
		Assert.assertEquals(-85.05112877980659, bounds.getMinY(), EPSILON);
		Assert.assertEquals(0.0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(0.0, bounds.getMaxY(), EPSILON);

		tile = new TileIndex(1, 0, 1);
		bounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(-180.0, bounds.getMinX(), EPSILON);
		Assert.assertEquals(0.0, bounds.getMinY(), EPSILON);
		Assert.assertEquals(0.0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(85.05112877980659, bounds.getMaxY(), EPSILON);

		tile = new TileIndex(1, 1, 0);
		bounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(0.0, bounds.getMinX(), EPSILON);
		Assert.assertEquals(-85.05112877980659, bounds.getMinY(), EPSILON);
		Assert.assertEquals(180.0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(0.0, bounds.getMaxY(), EPSILON);

		tile = new TileIndex(1, 1, 1);
		bounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(0.0, bounds.getMinX(), EPSILON);
		Assert.assertEquals(0.0, bounds.getMinY(), EPSILON);
		Assert.assertEquals(180.0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(85.05112877980659, bounds.getMaxY(), EPSILON);
	}

	@Test
	public void testLevel2 () {
		TileIndex tile;
		Rectangle2D bounds;

		tile = new TileIndex(2, 0, 0);
		bounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(-180.0, bounds.getMinX(), EPSILON);
		Assert.assertEquals(-85.05112877980659, bounds.getMinY(), EPSILON);
		Assert.assertEquals(-90.0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(-66.51326044311186, bounds.getMaxY(), EPSILON);

		tile = new TileIndex(2, 3, 3);
		bounds = _mercator.getTileBounds(tile);
		Assert.assertEquals(180.0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(85.05112877980659, bounds.getMaxY(), EPSILON);
		Assert.assertEquals(90.0, bounds.getMinX(), EPSILON);
		Assert.assertEquals(66.51326044311186, bounds.getMinY(), EPSILON);
	}

	@Test
	public void testBoundingBoxes () {
		TileIndex tile = new TileIndex(10, 14, 6, 256, 256);
		Rectangle2D tileBounds = _mercator.getTileBounds(tile);

		BinIndex bll = new BinIndex(0, 255);
		BinIndex bur = new BinIndex(255, 0);
		Rectangle2D llBounds = _mercator.getBinBounds(tile, bll);
		Rectangle2D urBounds = _mercator.getBinBounds(tile, bur);

		Assert.assertEquals(llBounds.getMinX(), tileBounds.getMinX(), EPSILON);
		Assert.assertEquals(urBounds.getMaxX(), tileBounds.getMaxX(), EPSILON);
		Assert.assertEquals(llBounds.getMinY(), tileBounds.getMinY(), EPSILON);
		Assert.assertEquals(urBounds.getMaxY(), tileBounds.getMaxY(), EPSILON);
	}

	@Test
	public void testBounds () {
		int minBinX = 1000;
		int maxBinX = -1000;
		int minBinY = 1000;
		int maxBinY = -1000;
		for (int i=0; i<10000000; ++i) {
			double longitude = Math.random()*180-180;
			double latitude = Math.random()*90-90;
			Point2D pt = new Point2D.Double(longitude, latitude);
			TileIndex tile = _mercator.rootToTile(pt, 1);
			if (0 != tile.getX() || 0 != tile.getY()) continue;

			BinIndex bin = _mercator.rootToBin(pt, tile);
			if (bin.getX() < minBinX) minBinX = bin.getX();
			if (bin.getX() > maxBinX) maxBinX = bin.getX();
			if (bin.getY() < minBinY) minBinY = bin.getY();
			if (bin.getY() > maxBinY) maxBinY = bin.getY();
		}
		Assert.assertEquals(0, minBinX);
		Assert.assertEquals(0, minBinY);
		Assert.assertEquals(255, maxBinX);
		Assert.assertEquals(255, maxBinY);
	}


	private Point2D getCenter (Rectangle2D rect) {
		return new Point2D.Double(rect.getCenterX(), rect.getCenterY());
	}

	@Test
	public void testRoundTripTransformation () {
		for (int level=0; level<4; ++level) {
			int pow2 = 1 << level;
			for (int x = 0; x<pow2; ++x) {
				for (int y=0; y<pow2; ++y) {
					TileIndex t0 = new TileIndex(level, x, y);
					Rectangle2D tileBounds = _mercator.getTileBounds(t0);
					TileIndex t1 = _mercator.rootToTile(getCenter(tileBounds), level);
					Assert.assertEquals(t0,  t1);

					for (int dx=0; dx<t0.getXBins(); ++dx) {
						for (int dy=0; dy<t0.getYBins(); ++dy) {
							BinIndex b0 = new BinIndex(dx, dy);
							Rectangle2D binBounds = _mercator.getBinBounds(t0, b0);
							BinIndex b1 = _mercator.rootToBin(getCenter(binBounds), t0);
							Assert.assertEquals(b0, b1);
						}
					}
				}
			}
		}
	}

	@Test
	public void testBoundingBoxDirection () {
		TileIndex tile = new TileIndex(5, 23, 17);
		BinIndex bin = new BinIndex(34, 78);
		Rectangle2D bounds = _mercator.getBinBounds(tile, bin);
		Assert.assertTrue(bounds.getMaxX() > bounds.getMinX());
		Assert.assertTrue(bounds.getMaxY() > bounds.getMinY());
	}


	@Test
	public void testEPSG900913Projection () {
		TileIndex tile = new TileIndex(1, 0, 0);
		Rectangle2D bounds = _mercator.getEPSG_900913Bounds(tile, null);
		Assert.assertEquals(-20037508.342789244, bounds.getMinX(), EPSILON);
		Assert.assertEquals(-20037508.342789244, bounds.getMinY(), EPSILON);
		Assert.assertEquals(0, bounds.getMaxX(), EPSILON);
		Assert.assertEquals(0, bounds.getMaxY(), EPSILON);

		tile = new TileIndex(3, 1, 2);
		bounds = _mercator.getEPSG_900913Bounds(tile, null);

		Assert.assertEquals(-15028131.257091932, bounds.getMinX(), WORLD_SCALE_EPSILON);
		Assert.assertEquals(-10018754.171394622, bounds.getMaxX(), WORLD_SCALE_EPSILON);
		Assert.assertEquals(-10018754.171394622 , bounds.getMinY(), WORLD_SCALE_EPSILON);
		Assert.assertEquals(-5009377.085697312, bounds.getMaxY(), WORLD_SCALE_EPSILON);
	}

	@Test
	public void testBinOverlap () {
		// Southern hemisphere; upper half should be smaller
		TileIndex tile = new TileIndex(5, 12, 14);
		BinIndex bin = new BinIndex(124, 154);
		Rectangle2D realBounds = _mercator.getBinBounds(tile, bin);
		Assert.assertEquals(1.0,
		                    _mercator.getBinOverlap(tile, bin, realBounds),
		                    EPSILON);

		Rectangle2D leftHalf = new Rectangle2D.Double(realBounds.getMinX(),
		                                              realBounds.getMinY(),
		                                              realBounds.getWidth() / 2.0,
		                                              realBounds.getHeight());
		Assert.assertEquals(0.5, _mercator.getBinOverlap(tile, bin, leftHalf),
		                    EPSILON);

		Rectangle2D rightHalf = new Rectangle2D.Double(realBounds.getMinX() + realBounds.getWidth()/2.0,
		                                               realBounds.getMinY(),
		                                               realBounds.getWidth() / 2.0,
		                                               realBounds.getHeight());
		Assert.assertEquals(0.5, _mercator.getBinOverlap(tile, bin, rightHalf),
		                    EPSILON);

		Rectangle2D topHalf = new Rectangle2D.Double(realBounds.getMinX(),
		                                             realBounds.getMinY() + realBounds.getHeight()/2.0,
		                                             realBounds.getWidth(),
		                                             realBounds.getHeight() / 2.0);
		double topOverlap = _mercator.getBinOverlap(tile, bin, topHalf);
		Assert.assertTrue(0.5 > topOverlap);
		Assert.assertTrue(0.0 < topOverlap);

		Rectangle2D bottomHalf = new Rectangle2D.Double(realBounds.getMinX(),
		                                                realBounds.getMinY(),
		                                                realBounds.getWidth(),
		                                                realBounds.getHeight() / 2.0);
		double bottomOverlap = _mercator.getBinOverlap(tile, bin, bottomHalf);
		Assert.assertTrue(0.5 < bottomOverlap);
		Assert.assertTrue(1.0 > bottomOverlap);
		Assert.assertEquals(1.0, topOverlap + bottomOverlap, EPSILON);

		Rectangle2D bottomLeftCorner = new Rectangle2D.Double(realBounds.getMinX(),
		                                                      realBounds.getMinY(),
		                                                      realBounds.getWidth() / 2.0,
		                                                      realBounds.getHeight() / 2.0);
		Assert.assertEquals(bottomOverlap / 2.0,
		                    _mercator.getBinOverlap(tile, bin, bottomLeftCorner),
		                    EPSILON);

		Rectangle2D bottomLeftCornerPlus = new Rectangle2D.Double(realBounds.getMinX() - 1.0,
		                                                          realBounds.getMinY() - 1.0,
		                                                          realBounds.getWidth() / 2.0 + 1.0,
		                                                          realBounds.getHeight() / 2.0 + 1.0);
		Assert.assertEquals(bottomOverlap / 2.0,
		                    _mercator.getBinOverlap(tile, bin, bottomLeftCornerPlus),
		                    EPSILON);
	}


	// Write out test data for testing actual binning in situ
	// Writes a dot at the center of tile(5, 23, 14), with lines going north and east.
	@Ignore
	@Test
	public void isolateTestCases () {
		WebMercatorTilePyramid mercator = new WebMercatorTilePyramid();

		TileIndex tile = new TileIndex(5, 23, 17);

		for (int x=-3; x<3; ++x) {
			for (int y=-3; y<4; ++y) {
				BinIndex bin = new BinIndex(127+x, 127+y);
				Rectangle2D bounds = mercator.getBinBounds(tile, bin);
				int reps = Math.max(0, 16-((x*x+y*y)));
				for (int i=0; i<reps; ++i) {
					System.out.println(String.format("ais(3.0), 0, 'test ship', '', %.6f, %.6f, 0.0, 0.0, 1207040300, 'TEST', 0203011000", bounds.getCenterY(), bounds.getCenterX()));
				}
			}
		}
		for (int n=0; n<10; ++n) {
			BinIndex xbin = new BinIndex(131+n, 127);
			BinIndex ybin = new BinIndex(127, 131+n);
			Rectangle2D xbounds = mercator.getBinBounds(tile, xbin);
			Rectangle2D ybounds = mercator.getBinBounds(tile, ybin);
			for (int i=0; i<16; ++i) {
				System.out.println(String.format("ais(3.0), 0, 'test ship', '', %.6f, %.6f, 0.0, 0.0, 1207040300, 'TEST', 0203011000",
				                                 xbounds.getCenterY(), xbounds.getCenterX()));
				System.out.println(String.format("ais(3.0), 0, 'test ship', '', %.6f, %.6f, 0.0, 0.0, 1207040300, 'TEST', 0203011000",
				                                 ybounds.getCenterY(), ybounds.getCenterX()));
			}
		}
	}
}
