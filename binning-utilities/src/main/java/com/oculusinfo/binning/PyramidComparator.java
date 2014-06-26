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


import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Comparator;



/**
 * This class uses a Z-order curve (Morton curve) to sort points so that at
 * every level, points in the same tile will be grouped together.
 * 
 * Direct comparison code is taken from
 * http://en.wikipedia.org/wiki/Z-order_curve
 * 
 * Key-generation code is taken from one of Chris Bethune's projects.
 * 
 * This class acts as a comparator, but does not implement the comparator
 * interface, because it can handle both raw points and indices (and we can't
 * simultaneously implement Comparator<Point> and Comparator<TileIndex>); if
 * comparator implementation is needed, though - say, for instance, for sorting
 * - it can provide one.
 * 
 * @author nkronenfeld
 */
public class PyramidComparator implements Serializable {
	private static final long     serialVersionUID         = 1L;

	// Constants for bit interleaving.
	public static final long      BITS[]                   = {
		0x5555555555555555L, 0x3333333333333333L, 0x0F0F0F0F0F0F0F0FL,
		0x00FF00FF00FF00FFL, 0x0000FFFF0000FFFFL, 0x00000000FFFFFFFFL};

	public static final long      SHIFTS[]                 = {1, 2, 4, 8, 16};

	public static final byte      SW                       = 0;
	public static final byte      SE                       = 1;
	public static final byte      NW                       = 2;
	public static final byte      NE                       = 3;



	/**
	 * Comparing points at level 20 has a granularity of 1M in each dimension in
	 * tiles alone
	 * 
	 * This means, with 1E12 partitions - way more than hadoop or spark can
	 * handle - we would have roughly 1 tile/partition. So this should be
	 * fine-grained enough for anything we might care about.
	 */
	private static final int      DEFAULT_COMPARISON_LEVEL = 20;



	private int                   _comparisonLevel;
	private TilePyramid           _pyramid;
	private Comparator<Point>     _rawComparator;
	private Comparator<TileIndex> _indexComparator;



	/**
	 * Create a comparator that compares raw values at the default level (
	 * {@link #DEFAULT_COMPARISON_LEVEL} (or indices at any level)
	 * 
	 * @param pyramid They pyramid scheme with which raw values are converted to
	 *            tile values
	 */
	public PyramidComparator (TilePyramid pyramid) {
		this(pyramid, DEFAULT_COMPARISON_LEVEL);
	}

	/**
	 * Creates a comparator that compares raw values at the given level (or
	 * indices at any level)
	 * 
	 * @param pyramid They pyramid scheme with which raw values are converted to
	 *            tile values
	 * @param comparisonLevel The level at which raw values are converted to
	 *            tile indices for comparison. Comparing at level n gives 2^n
	 *            bins in each dimension, or 4^n different discernable locations
	 *            for comparison
	 */
	public PyramidComparator (TilePyramid pyramid, int comparisonLevel) {
		_pyramid = pyramid;
		_comparisonLevel = comparisonLevel;
		_rawComparator = new RawCoordinateComparator();
		_indexComparator = new TileIndexComparator();
	}

	/**
	 * Compare two raw input values according to our pyramid scheme
	 * 
	 * @return Standard comparison values, as per {@link Comparator}
	 */
	public int compareRaw (Point2D pt1, Point2D pt2) {
		// Mostly taken from http://en.wikipedia.org/wiki/Z-order_curve, but
		// this is a very simple 2-dimensional case, so the results are rather 
		// simplified.
		TileIndex tile1 = _pyramid.rootToTile(pt1, _comparisonLevel);
		TileIndex tile2 = _pyramid.rootToTile(pt2, _comparisonLevel);

		return compareIndexAtLevel(tile1, tile2);
	}

	/**
	 * Compare two raw input values according to our pyramid scheme
	 * 
	 * @return Standard comparison values, as per {@link Comparator}
	 */
	public int compareRaw (double x1, double y1, double x2, double y2) {
		TileIndex tile1 = _pyramid.rootToTile(x1, y1, _comparisonLevel);
		TileIndex tile2 = _pyramid.rootToTile(x2, y2, _comparisonLevel);

		return compareIndexAtLevel(tile1, tile2);
	}



	/**
	 * Returns a lookup key given a set of index values uniquely identifying a
	 * tile in the tree. The bits of the X and Y are interleaved to generate a
	 * Morton code, and then combined with an offset for level to get a final
	 * location code.
	 * 
	 * @param rawX The x coordinate of the raw data
	 * 
	 * @param rawY The y coordinate of the raw data
	 * 
	 * @return A location key that should be in order according to the Morton
	 *         curve ordering.
	 */
	public long getComparisonKey (double rawX, double rawY) {
		TileIndex index = _pyramid.rootToTile(rawX, rawY, _comparisonLevel);
		long x = index.getX();
		long y = index.getY();

		x = (x | (x << SHIFTS[4])) & BITS[4];
		x = (x | (x << SHIFTS[3])) & BITS[3];
		x = (x | (x << SHIFTS[2])) & BITS[2];
		x = (x | (x << SHIFTS[1])) & BITS[1];
		x = (x | (x << SHIFTS[0])) & BITS[0];

		y = (y | (y << SHIFTS[4])) & BITS[4];
		y = (y | (y << SHIFTS[3])) & BITS[3];
		y = (y | (y << SHIFTS[2])) & BITS[2];
		y = (y | (y << SHIFTS[1])) & BITS[1];
		y = (y | (y << SHIFTS[0])) & BITS[0];

		long z = x | (y << 1);

		// Apply fence bit
		return (0x01L << (2 * (_comparisonLevel + 1))) | z;
	}


	/**
	 * Compare two tile indices. The indices are compared at the level of the
	 * least zoomed-in of the two tiles.
	 * 
	 * @return Standard comparison values, as per {@link Comparator}
	 */
	public int compareIndex (TileIndex t1, TileIndex t2) {
		int l1 = t1.getLevel();
		int l2 = t2.getLevel();

		if (l1 < l2) {
			// Bring t2 up to the level of t1
			int p2 = 1 << (l2 - l1);
			t2 = new TileIndex(l1, t2.getX() / p2, t2.getY() / p2,
			                   t2.getXBins(), t2.getYBins());
		} else if (l1 > l2) {
			// Bring t1 up to the level of t2
			int p2 = 1 << (l1 - l2);
			t1 = new TileIndex(l2, t1.getX() / p2, t1.getY() / p2,
			                   t1.getXBins(), t1.getYBins());
		}

		return compareIndexAtLevel(t1, t2);
	}

	/*
	 * Compare two tile indices, making the assumption that they are at the same
	 * level. This method is private so that we may guarantee that all calls to
	 * it take that assumption into account.
	 */
	private int compareIndexAtLevel (TileIndex t1, TileIndex t2) {
		int x1 = t1.getX();
		int y1 = t1.getY();
		int x2 = t2.getX();
		int y2 = t2.getY();

		int x = x1 ^ x2;
		int y = y1 ^ y2;

		// if =, default to x
		if (y < x && y < (x ^ y)) {
			return x1 - x2;
		} else {
			return y1 - y2;
		}
	}

	/**
	 * Retrieve a implementation of Comparator that can be used to compare raw
	 * points generically.
	 */
	public Comparator<Point> getRawCoordinateComparator () {
		return _rawComparator;
	}

	/**
	 * Retreive an implementation of Comparator that can be used to compare tile
	 * indices generically.
	 */
	public Comparator<TileIndex> getTileIndexComparator () {
		return _indexComparator;
	}



	private class RawCoordinateComparator implements Comparator<Point>,
	                                                 Serializable {
		private static final long     serialVersionUID         = 1L;

		@Override
		public int compare (Point p1, Point p2) {
			return compareRaw(p1, p2);
		}
	}



	private class TileIndexComparator implements Comparator<TileIndex>,
	                                             Serializable {
		private static final long     serialVersionUID         = 1L;

		@Override
		public int compare (TileIndex t1, TileIndex t2) {
			return compareIndex(t1, t2);
		}
	}
}
