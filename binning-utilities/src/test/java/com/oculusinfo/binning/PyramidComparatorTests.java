/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class PyramidComparatorTests {
	private TilePyramid         _pyramid;
	private PyramidComparator   _comparator;
	private List<List<Point2D>> _points;
	private List<List<Integer>> _order;



	@Before
	public void setup () {
		_pyramid = new AOITilePyramid(0.0, 0.0, 16.0, 16.0);

		_comparator = new PyramidComparator(_pyramid, 4);

		_points = new ArrayList<>(8);
		for (int y = 0; y < 8; ++y) {
			_points.add(new ArrayList<Point2D>());
			for (int x = 0; x < 8; ++x) {
				_points.get(y).add(new Point2D.Double((double) x, (double) y));
			}
		}

		_order = Arrays.asList(Arrays.asList(1, 2, 5, 6, 17, 18, 21, 22),
		                       Arrays.asList(3, 4, 7, 8, 19, 20, 23, 24),
		                       Arrays.asList(9, 10, 13, 14, 25, 26, 29, 30),
		                       Arrays.asList(11, 12, 15, 16, 27, 28, 31, 32),
		                       Arrays.asList(33, 34, 37, 38, 49, 50, 53, 54),
		                       Arrays.asList(35, 36, 39, 40, 51, 52, 55, 56),
		                       Arrays.asList(41, 42, 45, 46, 57, 58, 61, 62),
		                       Arrays.asList(43, 44, 47, 48, 59, 60, 63, 64));
	}

	@After
	public void teardown () {
		_pyramid = null;
		_comparator = null;
		_points = null;
		_order = null;
	}

	private int signum (int n) {
		return (int) Math.signum(n);
	}

	@Test
	public void testRawComparisons () {
		for (int x1 = 0; x1 < 8; ++x1) {
			for (int y1 = 0; y1 < 8; ++y1) {
				int order1 = _order.get(y1).get(x1);
				Point2D point1 = _points.get(y1).get(x1);

				for (int x2 = 0; x2 < 8; ++x2) {
					for (int y2 = 0; y2 < 8; ++y2) {
						int order2 = _order.get(y2).get(x2);
						Point2D point2 = _points.get(y2).get(x2);

						String message = String.format("Point [%d, %d] (expected order %d) didn't compare properly to point [%d, %d] (expected order %d)",
						                               x1, y1, order1, x2, y2,
						                               order2);

						Assert.assertEquals(message,
						                    signum(Integer.compare(order1, order2)),
						                    signum(_comparator.compareRaw(point1, point2)));
					}
				}
			}
		}
	}

	@Test
	public void testKeyGeneration () {
		for (int x1 = 0; x1 < 8; ++x1) {
			for (int y1 = 0; y1 < 8; ++y1) {
				int order1 = _order.get(y1).get(x1);
				Point2D point1 = _points.get(y1).get(x1);
				long key1 = _comparator.getComparisonKey(point1.getX(), point1.getY());

				for (int x2 = 0; x2 < 8; ++x2) {
					for (int y2 = 0; y2 < 8; ++y2) {
						int order2 = _order.get(y2).get(x2);
						Point2D point2 = _points.get(y2).get(x2);
						long key2 = _comparator.getComparisonKey(point2.getX(), point2.getY());

						String message = String.format("Point [%d, %d] (expected order %d) didn't compare properly to point [%d, %d] (expected order %d)",
						                               x1, y1, order1, x2, y2,
						                               order2);

						Assert.assertEquals(message,
						                    signum(Integer.compare(order1, order2)),
						                    signum(Long.compare(key1, key2)));
					}
				}
			}
		}
	}
	@Test
	public void testTileComparisons () {
		for (int L1 = 0; L1 < 5; ++L1) {
			for (int x1 = 0; x1 < 8; ++x1) {
				for (int y1 = 0; y1 < 8; ++y1) {
					int order1 = _order.get(y1).get(x1);
					Point2D point1 = _points.get(y1).get(x1);
					TileIndex index1 = _pyramid.rootToTile(point1, L1);

					for (int L2 = 0; L2 < 5; ++L2) {
						for (int x2 = 0; x2 < 8; ++x2) {
							for (int y2 = 0; y2 < 8; ++y2) {
								int order2 = _order.get(y2).get(x2);
								Point2D point2 = _points.get(y2).get(x2);
								TileIndex index2 = _pyramid.rootToTile(point2, L2);

								TileIndex index1AtLevel2 = _pyramid.rootToTile(point1, L2);
								TileIndex index2AtLevel1 = _pyramid.rootToTile(point2, L1);

								if (index1.equals(index2AtLevel1) || index2.equals(index1AtLevel2)) {
									String message = String.format("Points %s and %s didn't identify",
									                               index1.toString(), index2.toString());
									Assert.assertEquals(message,
									                    0,
									                    signum(_comparator.compareIndex(index1, index2)));
								} else {
									String message = String.format("Point %s (expected order %d) didn't compare properly to point %s (expected order %d)",
									                               index1.toString(), order1,
									                               index2.toString(), order2);

									Assert.assertEquals(message,
									                    signum(Integer.compare(order1, order2)),
									                    signum(_comparator.compareIndex(index1, index2)));
								}
							}
						}
					}
				}
			}
		}
	}
}
