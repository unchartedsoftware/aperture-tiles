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
import com.oculusinfo.binning.TileIterator;
import com.oculusinfo.binning.TilePyramid;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



/**
 * Mercator tile projections using simple mercator (uncorrected for ellipsoidal
 * distortion, which really shouldn't matter anyhow, except very near the
 * equator)
 *
 * Sources:
 *
 * https://en.wikipedia.org/wiki/Mercator_projection
 *
 * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
 *
 *
 * Root coordinate system is longitude/latitude, both in degrees.
 *
 * @author nkronenfeld
 */
public class WebMercatorTilePyramid implements TilePyramid, Serializable {
	// We have no intrinsic data yet, so a default serial version should be fine.
	private static final long serialVersionUID = 1L;

	// From http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
	// What is the coordinate extent of Earth in EPSG:900913?
	//
	// [-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244]
	// Constant 20037508.342789244 comes from the circumference of the Earth in meters,
	// which is 40 thousand kilometers, the coordinate origin is in the middle of extent.
	// In fact you can calculate the constant as: 2 * math.pi * 6378137 / 2.0
	//
	// Polar areas with abs(latitude) bigger then 85.05112878 are clipped off.
	private static final double EPSG_900913_SCALE_FACTOR = 20037508.342789244;
	private static final double EPSG_900913_LATITUDE	 = 85.05112878;

	private static final double PI                      = Math.PI;
	// ellipsoid equatorial getRadius, in meters
	public static final double  WGS84_EQUATORIAL_RADIUS = 6378137.0;
	// ellipsoid polar getRadius, in meters
	public static final double  WGS84_POLAR_RADIUS      = 6356752.3;
	// eccentricity squared, semi-major axis
	public static final double  WGS84_ES                = 0.00669437999013;

	private static double _minX = 180.0;
	private static double _maxX = -180.0;
	private static double _minY = -EPSG_900913_LATITUDE;
	private static double _maxY = EPSG_900913_LATITUDE;

	protected double gudermannian(double y) {
		// converts a y value from -PI(bottom) to PI(top) into the
		// mercator projection latitude
		return Math.toDegrees(Math.atan( Math.sinh(y) ));
	}


	protected double gudermannianInv (double latitude ) {
		// converts a latitude value from -EPSG_900913_LATITUDE to EPSG_900913_LATITUDE into
		// a y value from -PI(bottom) to PI(top)
		double sign = ( latitude != 0 ) ? latitude / Math.abs(latitude) : 0,
			sin = Math.sin(Math.toRadians(latitude) * sign);
		return sign * (Math.log((1.0 + sin) / (1.0 - sin)) / 2.0);
	}


	protected double linearToGudermannian (double value) {
		// convert linear coordinates into their equivalent gudermannian counterparts
		return gudermannian( (value / EPSG_900913_LATITUDE) * Math.PI );
	}


	protected double gudermannianToLinear (double value) {
		// convert gudermannian coordinates into their equivalent linear counterparts
		return (gudermannianInv( value ) / Math.PI) * EPSG_900913_LATITUDE;
	}


	private Point2D rootToTileMercator (Point2D point, int level) {
		return rootToTileMercator(point.getX(), point.getY(), level);
	}

	private Point2D rootToTileMercator (double lon, double lat, int level) {
		double latR = Math.toRadians(lat);
		int pow2 = 1 << level;
		double x = (lon+180.0)/360.0 * pow2;
		double y = (1 - Math.log(Math.tan(latR) + 1 / Math.cos(latR)) / Math.PI) / 2 * pow2;
		return new Point2D.Double(x, pow2-y);
	}

	@Override
	public Rectangle2D getBounds()  {
		return new Rectangle2D.Double( _minX, _minY, _maxX - _minX, _maxY - _minY );
	}

	@Override
	public String getProjection () {
		return "EPSG:900913";
	}

	@Override
	public String getTileScheme () {
		return "TMS";
	}


	public Rectangle2D getEPSG_900913Bounds (TileIndex tile, BinIndex bin) {
		int pow2 = 1 << tile.getLevel();
		double tileIncrement = 1.0/pow2;
		double minX = tile.getX() * tileIncrement - 0.5;
		double minY = tile.getY() * tileIncrement - 0.5;
		double maxX, maxY;
		if (null != bin) {
			double binXInc = tileIncrement/tile.getXBins();
			double binYInc = tileIncrement/tile.getYBins();
			minX = minX + bin.getX()*binXInc;
			minY = minY + (tile.getYBins() - bin.getY()-1)*binYInc;
			maxX = minX + binXInc;
			maxY = minY + binYInc;
		} else {
			maxX = minX + tileIncrement;
			maxY = minY + tileIncrement;
		}

		// Our range is actually (-0.5, 0.5), not (-1.0, 1.0), so we need 2 x ScaleFactor
		return new Rectangle2D.Double (minX*2.0*EPSG_900913_SCALE_FACTOR,
		                               minY*2.0*EPSG_900913_SCALE_FACTOR,
		                               (maxX-minX)*2.0*EPSG_900913_SCALE_FACTOR,
		                               (maxY-minY)*2.0*EPSG_900913_SCALE_FACTOR);
	}



	@Override
	public TileIndex rootToTile (Point2D point, int level) {
		return rootToTile(point, level, 256, 256);
	}

	@Override
	public TileIndex rootToTile (Point2D point, int level, int xBins, int yBins) {
		Point2D tileMercator = rootToTileMercator(point, level);
		return new TileIndex(level,
		                     (int) Math.floor(tileMercator.getX()),
		                     (int) Math.floor(tileMercator.getY()),
		                     xBins, yBins);
	}

	@Override
	public TileIndex rootToTile (double x, double y, int level) {
		return rootToTile(x, y, level, 256, 256);
	}

	@Override
	public TileIndex rootToTile (double x, double y, int level, int xBins, int yBins) {
		Point2D tileMercator = rootToTileMercator(x, y, level);
		return new TileIndex(level,
		                     (int) Math.floor(tileMercator.getX()),
		                     (int) Math.floor(tileMercator.getY()),
		                     yBins, xBins);
	}

	@Override
	public BinIndex rootToBin (Point2D point, TileIndex tile) {
		Point2D tileMercator = rootToTileMercator(point, tile.getLevel());
		return new BinIndex((int) Math.floor((tileMercator.getX()-tile.getX())*tile.getXBins()),
		                    tile.getYBins()-1-(int) Math.floor((tileMercator.getY()-tile.getY())*tile.getYBins()));
	}

	@Override
	public BinIndex rootToBin (double x, double y, TileIndex tile) {
		Point2D tileMercator = rootToTileMercator(x, y, tile.getLevel());
		return new BinIndex((int) Math.floor((tileMercator.getX()-tile.getX())*tile.getXBins()),
		                    tile.getYBins()-1-(int) Math.floor((tileMercator.getY()-tile.getY())*tile.getYBins()));
	}


	private double tileToLon (double x, int level) {
		int pow2 = 1 << level;
		return x/pow2 * 360.0 - 180.0;
	}

	private double tileToLat (double y, int level) {
		int pow2 = 1 << level;
		double n = -PI + (2.0*PI*y)/pow2;
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}


	@Override
	public Rectangle2D getTileBounds (TileIndex tile) {
		int level = tile.getLevel();
		double north = tileToLat(tile.getY()+1, level);
		double south = tileToLat(tile.getY(), level);
		double east = tileToLon(tile.getX()+1, level);
		double west = tileToLon(tile.getX(), level);
		return new Rectangle2D.Double(west, south, east-west, north-south);
	}

	@Override
	public Rectangle2D getBinBounds (TileIndex tile, BinIndex bin) {
		int level = tile.getLevel();

		double binXInc = 1.0/tile.getXBins();
		double baseX = tile.getX()+bin.getX()*binXInc;

		double binYInc = 1.0/tile.getYBins();
		double baseY = tile.getY()+(tile.getYBins()-1-bin.getY())*binYInc;

		double north = tileToLat(baseY + binYInc, level);
		double south = tileToLat(baseY, level);
		double east = tileToLon(baseX + binXInc, level);
		double west = tileToLon(baseX, level);

		return new Rectangle2D.Double(west, south, east-west, north-south);
	}

	@Override
	public double getBinOverlap (TileIndex tile, BinIndex bin, Rectangle2D area) {
		Point2D lowerLeftRoot = new Point2D.Double(area.getMinX(), area.getMinY());
		Point2D lowerLeftMercator = rootToTileMercator(lowerLeftRoot, tile.getLevel());
		double left = (lowerLeftMercator.getX()-tile.getX())*tile.getXBins()-bin.getX();
		double bottom = (tile.getYBins()-1) - (lowerLeftMercator.getY()-tile.getY())*tile.getYBins() - bin.getY();

		Point2D upperRightRoot = new Point2D.Double(area.getMaxX(), area.getMaxY());
		Point2D upperRightMercator = rootToTileMercator(upperRightRoot, tile.getLevel());
		double right = (upperRightMercator.getX()-tile.getX())*tile.getXBins()-bin.getX();
		double top = (tile.getYBins()-1) - (upperRightMercator.getY() - tile.getY())*tile.getYBins() - bin.getY();

		// Top and bottom actually reversed, but since we take absolute values, it doesn't really matter.
		left = Math.min(Math.max(left, 0.0), 1.0);
		right = Math.min(Math.max(right, 0.0), 1.0);
		top = Math.min(Math.max(top, -1.0), 0.0);
		bottom = Math.min(Math.max(bottom, -1.0), 0.0);

		return Math.abs((right-left) * (top-bottom));
	}

	@Override
	public Collection<TileIndex> getBestTiles (Rectangle2D bounds) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<TileIndex> getTiles (Rectangle2D bounds, int level){
		TileIterator tileIt = new TileIterator(this, level, bounds);
		List<TileIndex> results = new ArrayList<TileIndex>();
		while (tileIt.hasNext()) {
			results.add(tileIt.next());
		}

		return results;
	}

	// All web mercator tile pyramids are equal
	@Override
	public int hashCode () {
		return 1500450271;
	}

	@Override
	public boolean equals (Object that) {
		if (null == that) return false;
		return (that instanceof WebMercatorTilePyramid);
	}
}
