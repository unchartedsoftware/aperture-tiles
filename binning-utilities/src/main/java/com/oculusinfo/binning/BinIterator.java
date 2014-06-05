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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;



/**
 * An iterator over all (tile, bin) pairs on a single level that overlap a given
 * area
 * 
 * @author Nathan Kronenfeld
 */
public class BinIterator implements Iterator<TileAndBinIndices> {
	private TilePyramid _pyramid;
	// Desired tile level
	private int        _level;
	// Desired number of horizontal bins per tile 
	private int        _numXBins;
	// Desired number of vertical bins per tile
	private int        _numYBins;

	// We just store stuff internally as bins - but not maxed out at number of
	// bins per tile, but rather infinite.
	private int        _minBinX;
	private int        _minBinY;
	private int        _maxBinX;
	private int        _maxBinY;

	private int        _curBinX;
	private int        _curBinY;

    

	/**
	 * Create an iterator over a particular area for a particular level, given a
	 * projection.
	 * 
	 * @param pyramid The bin pyramid (projection) describing how to translate
	 *            raw coordinates to bin indices
	 * @param level The tile level to check
	 * @param area The area covered by this iterator
	 */
	public BinIterator (TilePyramid pyramid, int level, Rectangle2D area) {
		_pyramid = pyramid;
		_level = level;
		_numXBins = 256;
		_numYBins = 256;

		Point llCoords = getBinCoordinates(area.getMinX(), area.getMinY());
		_minBinX = llCoords.x;
		_minBinY = llCoords.y;

		Point urCoords = getBinCoordinates(area.getMaxX(), area.getMaxY());
		_maxBinX = urCoords.x;
		_maxBinY = urCoords.y;

		_curBinX = _minBinX;
		_curBinY = _minBinY;
	}


	private Point getBinCoordinates (double x, double y) {
		Point2D point = new Point2D.Double(x, y);
		TileIndex tile = _pyramid.rootToTile(point, _level);
		if (tile.getXBins() != _numXBins && tile.getYBins() != _numYBins) {
			tile = new TileIndex(tile.getLevel(), tile.getX(), tile.getY(), _numXBins, _numYBins);
		}
		BinIndex bin = _pyramid.rootToBin(point, tile);
		return new Point(tile.getX()*tile.getXBins()+bin.getX(),
		                 tile.getY()*tile.getYBins()+(tile.getYBins()-1-bin.getY()));
	}

	@Override
	public boolean hasNext () {
		return _curBinX <= _maxBinX && _curBinY <= _maxBinY;
	}

	@Override
	public TileAndBinIndices next () {
		int tileX = (int) Math.floor(_curBinX/_numXBins);
		int tileY = (int) Math.floor(_curBinY/_numYBins);
		TileIndex tile = new TileIndex(_level, tileX, tileY, _numXBins, _numYBins);
		int binX = _curBinX-_numXBins*tileX;
		int binY = _curBinY-_numYBins*tileY;
		BinIndex bin = new BinIndex(binX, _numYBins-1-binY);

		++_curBinX;
		if (_curBinX > _maxBinX) {
			_curBinX = _minBinX;
			++_curBinY;
		}

		return new TileAndBinIndices(tile, bin);
	}

	@Override
	public void remove () {
		throw new UnsupportedOperationException("Can't remove from a bin iterator");
	}
}
