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
 * An iterator over all tiles on a single level that overlap a given area
 * 
 * @author Jesse McGeachie
 */
public class TileIterator implements Iterator<TileIndex> {
	private TilePyramid _pyramid;

	// Desired tile level
	private int        _level;


	private int        _minTileX;
	private int        _minTileY;
	private int        _maxTileX;
	private int        _maxTileY;

	private int        _curTileX;
	private int        _curTileY;

	/**
	 * Create an iterator over a particular area for a particular level, given a
	 * projection.
	 * 
	 * @param pyramid The bin pyramid (projection) describing how to translate
	 *            raw coordinates to bin indices
	 * @param level
	 *            The tile level to check
	 * @param area
	 *            The area covered by this iterator
	 */
	public TileIterator (TilePyramid pyramid, int level, Rectangle2D area) {
		_pyramid = pyramid;
		_level = level;

		Point llCoords = getTileCoordinates(area.getMinX(), area.getMinY());
		_minTileX = llCoords.x;
		_minTileY = llCoords.y;

		Point urCoords = getTileCoordinates(area.getMaxX(), area.getMaxY());
		_maxTileX = urCoords.x;
		_maxTileY = urCoords.y;

		_curTileX = _minTileX;
		_curTileY = _minTileY;
	}


	private Point getTileCoordinates (double x, double y) {
		Point2D point = new Point2D.Double(x, y);
		TileIndex tile = _pyramid.rootToTile(point, _level);
		return new Point(tile.getX(), tile.getY());
	}

	@Override
	public boolean hasNext () {
		return _curTileX <= _maxTileX && _curTileY <= _maxTileY;
	}

	@Override
	public TileIndex next () {
		TileIndex tile = new TileIndex(_level, _curTileX, _curTileY);

		++_curTileX;
		if (_curTileX > _maxTileX) {
			_curTileX = _minTileX;
			++_curTileY;
		}

		return tile;
	}

	@Override
	public void remove () {
		throw new UnsupportedOperationException("Can't remove from a bin iterator");
	}
}
