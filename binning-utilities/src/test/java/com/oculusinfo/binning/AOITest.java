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

import com.oculusinfo.binning.impl.AOITilePyramid;
import org.junit.Assert;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class AOITest {
	private static final double EPSILON = 1E-12;

	private AOITilePyramid _aoi = new AOITilePyramid(0, 0, 16, 16);
	private Point2D _point = new Point2D.Double(3.5, 5.5);
	private TileIndex _tile = new TileIndex(3, 1, 2);
	private BinIndex _bin = new BinIndex(192, 63);

	@Test
	public void rooToBinTest() {
		BinIndex bin = _aoi.rootToBin(_point, _tile);
		Assert.assertEquals(_bin, bin);
	}

	@Test
	public void rooToTileTest() {
		TileIndex tile = _aoi.rootToTile(_point, 3);
		Assert.assertEquals(_tile, tile);
	}

	@Test
	public void getTileBoundsTest() {
		Rectangle2D tileBounds = _aoi.getTileBounds(_tile);

		Assert.assertEquals(2.0, tileBounds.getMinX(), EPSILON);
		Assert.assertEquals(4.0, tileBounds.getMaxX(), EPSILON);

		Assert.assertEquals(4.0, tileBounds.getMinY(), EPSILON);
		Assert.assertEquals(6.0, tileBounds.getMaxY(), EPSILON);
	}

	@Test
	public void getBinBoundsTest() {
		Rectangle2D binBounds = _aoi.getBinBounds(_tile, _bin);

		Assert.assertEquals(3.5000000, binBounds.getMinX(), EPSILON);
		Assert.assertEquals(3.5078125, binBounds.getMaxX(), EPSILON);

		Assert.assertEquals(5.5000000, binBounds.getMinY(), EPSILON);
		Assert.assertEquals(5.5078125, binBounds.getMaxY(), EPSILON);
	}
}
