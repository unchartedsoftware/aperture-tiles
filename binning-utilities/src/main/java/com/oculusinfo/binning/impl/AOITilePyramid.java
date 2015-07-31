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
package com.oculusinfo.binning.impl;

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Collection;

/**
 * Area-of-Interest tile pyramid
 *
 * A tile pyramid that represents a predefined area, breaking it up linearly and
 * evenly.
 *
 * @author Jesse McGeachie
 */
public class AOITilePyramid implements TilePyramid, Serializable {
	private static final long serialVersionUID = 1L;



	double _minX;
	double _maxX;
	double _minY;
	double _maxY;
	double _recipDiffX;
	double _recipDiffY;

	public AOITilePyramid(double minX, double minY, double maxX, double maxY){
		_minX = minX;
		_maxX = maxX;
		_minY = minY;
		_maxY = maxY;
		_recipDiffX = 1.0/(_maxX-_minX);
		_recipDiffY = 1.0/(_maxY-_minY);
	}

	@Override
	public Rectangle2D getBounds()  {
		return new Rectangle2D.Double( _minX, _minY ,_maxX - _minX, _maxY - _minY);
	}

	@Override
	public String getProjection () {
		return "EPSG:4326";
	}

	@Override
	public String getTileScheme () {
		return "TMS";
	}

	@Override
	public TileIndex rootToTile (Point2D point, int level) {
		return rootToTile(point.getX(), point.getY(), level, 256, 256);
	}

	@Override
	public TileIndex rootToTile (Point2D point, int level, int xBins, int yBins) {
		return rootToTile(point.getX(), point.getY(), level, xBins, yBins);
	}

	@Override
	public TileIndex rootToTile (double x, double y, int level) {
		return rootToTile(x, y, level, 256, 256);
	}

	@Override
	public TileIndex rootToTile (double x, double y, int level, int xBins, int yBins) {
		long numDivs = 1L << level;

		int tileX = (int) Math.floor(numDivs*(x-_minX)*_recipDiffX);
		int tileY = (int) Math.floor(numDivs*(y-_minY)*_recipDiffY);
		//		int tileX = (int) Math.floor(numDivs*(x-_minX)/(_maxX - _minX));
		//		int tileY = (int) Math.floor(numDivs*(y-_minY)/(_maxY - _minY));

		return new TileIndex(level, tileX, tileY, xBins, yBins);
	}

	@Override
	public BinIndex rootToBin (Point2D point, TileIndex tile) {
		return rootToBin(point.getX(), point.getY(), tile);
	}

	@Override
	public BinIndex rootToBin (double x, double y, TileIndex tile) {

		long numDivs = 1L << tile.getLevel();

		int binX = (int) Math.floor((numDivs*(x-_minX)*_recipDiffX - tile.getX())*tile.getXBins());
		int binY = (int) Math.floor((numDivs*(y-_minY)*_recipDiffY - tile.getY())*tile.getYBins());

		//		int pow2 = 1 << tile.getLevel();
		//		double tileXSize = (_maxX-_minX)/pow2;
		//		double tileYSize = (_maxY-_minY)/pow2;
		//
		//		double xInTile = x-_minX - tile.getX()*tileXSize;
		//		double yInTile = y-_minY - tile.getY()*tileYSize;
		//
		//		int binX = (int) Math.floor(xInTile*tile.getXBins()/tileXSize);
		//		int binY = (int) Math.floor(yInTile*tile.getYBins()/tileYSize);

		return new BinIndex(binX, tile.getYBins()-1-binY);
	}

	@Override
	public Rectangle2D getTileBounds (TileIndex tile) {
		long pow2 = 1L << tile.getLevel();
		double tileXSize = (_maxX-_minX)/pow2;
		double tileYSize = (_maxY-_minY)/pow2;

		return new Rectangle2D.Double(_minX+tileXSize*tile.getX(),
		                              _minY+tileYSize*tile.getY(),
		                              tileXSize, tileYSize);
	}

	@Override
	public Rectangle2D getBinBounds(TileIndex tile, BinIndex bin) {
		long pow2 = 1L << tile.getLevel();
		double tileXSize = (_maxX-_minX)/pow2;
		double tileYSize = (_maxY-_minY)/pow2;
		double binXSize = tileXSize/tile.getXBins();
		double binYSize = tileYSize/tile.getYBins();

		int adjustedBinY = tile.getYBins()-1-bin.getY();
		return new Rectangle2D.Double(_minX+tileXSize*tile.getX()+binXSize*bin.getX(),
		                              _minY+tileYSize*tile.getY()+binYSize*adjustedBinY,
		                              binXSize, binYSize);
	}

	@Override
	public double getBinOverlap (TileIndex tile, BinIndex bin, Rectangle2D area) {
		Rectangle2D binBounds = getBinBounds(tile, bin);

		// We actually work in lat/lon, so this is what we want.
		double minx = (area.getMinX()-binBounds.getMinX())/binBounds.getWidth();
		double maxx = (area.getMaxX()-binBounds.getMinX())/binBounds.getWidth();
		double miny = (area.getMinY()-binBounds.getMinY())/binBounds.getHeight();
		double maxy = (area.getMaxY()-binBounds.getMinY())/binBounds.getHeight();

		minx = Math.min(Math.max(minx, 0.0), 1.0);
		maxx = Math.min(Math.max(maxx, 0.0), 1.0);
		miny = Math.min(Math.max(miny, 0.0), 1.0);
		maxy = Math.min(Math.max(maxy, 0.0), 1.0);

		return Math.abs((maxx-minx) * (maxy-miny));
	}

	@Override
	public Collection<TileIndex> getBestTiles(Rectangle2D bounds) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public int hashCode () {
		return (int) Math.round(_minX * 49980419 + _maxX * 54907427 +
		                        _minY * 62059399 + _maxY * 67432721);
	}

	@Override
	public boolean equals (Object other) {
		if (this == other) return true;
		if (null == other) return false;
		if (!(other instanceof AOITilePyramid)) return false;

		AOITilePyramid that = (AOITilePyramid) other;
		double epsilon = 1E-12;
		if (Math.abs(this._minX - that._minX) > epsilon) return false;
		if (Math.abs(this._maxX - that._maxX) > epsilon) return false;
		if (Math.abs(this._minY - that._minY) > epsilon) return false;
		if (Math.abs(this._maxY - that._maxY) > epsilon) return false;

		return true;
	}
}
