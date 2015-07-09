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



import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Collection;



/**
 * This interface represents a method of determining and using pyramided tiles
 * on a 2-D coordinate system.
 *
 * There are four coordinate systems involved:
 *
 * <ul>
 * <li>Root coordinates - the coordinate system of the geometry really being
 * described - typically lon/lat, for world coordinates</li>
 * <li>Tile coordinates - the coordinates of the tiles - x, y, z, z being tile
 * level. The lower left corner of the world at each level should be tile (0, 0)
 * </li>
 * <li>Bin coordinates - the coordinates of the bins within a particular tile
 * The upper left corner of each tile should be bin (0, 0)</li>
 * <li>Universal bin coordinates - the coordinate of the bin within the entirety
 * of the data. The upper left corner of this should be (0, 0), and the lower
 * right, (2^level*xBinCount, 2^level*yBinCount). Universal bin coordinates are
 * only used by a few static tile methods -
 * {@link TileIndex#tileBinIndexToUniversalBinIndex(TileIndex, BinIndex)} and
 * {@link TileIndex#universalBinIndexToTileBinIndex(TileIndex, BinIndex)}.</li>
 * </ul>
 *
 * @author nkronenfeld
 */
public interface TilePyramid extends Serializable {
	/**
	 * Convert from root to tile coordinates
	 *
	 * @param point
	 *            The coordinate of the point to be transformed, in the root
	 *            coordinate system
	 * @param level
	 *            The tile level required
	 * @return The tile into which (x, y) falls at the given level
	 */
	public TileIndex rootToTile (Point2D point, int level);

	/**
	 * Convert from root to tile coordinates
	 *
	 * @param point
	 *            The coordinate of the point to be transformed, in the root
	 *            coordinate system
	 * @param level
	 *            The tile level required
	 * @param xBins
	 *            The number of bins along the horizontal dimension of the
	 *            requested tile. This doesn't make any difference in choice of
	 *            tile, but is included in the return value, so we allow passing
	 *            it in so as to avoid the need to change the return value.
	 * @param yBins
	 *            The number of bins along the vertical dimension of the
	 *            requested tile. This doesn't make any difference in choice of
	 *            tile, but is included in the return value, so we allow passing
	 *            it in so as to avoid the need to change the return value.
	 * @return The tile into which (x, y) falls at the given level
	 */
	public TileIndex rootToTile (Point2D point, int level, int xBins, int yBins);

	/**
	 * Convert from root to tile coordinates
	 *
	 * @param x
	 *            The x coordinate of the point to be transformed, in the
	 *            root coordinate system
	 * @param y
	 *            The y coordinate of the point to be transformed, in the
	 *            root coordinate system
	 * @param level
	 *            The tile level required
	 * @return The tile into which (x, y) falls at the given level
	 */
	public TileIndex rootToTile (double x, double y, int level);

	/**
	 * Convert from root to tile coordinates
	 *
	 * @param x
	 *            The x coordinate of the point to be transformed, in the root
	 *            coordinate system
	 * @param y
	 *            The y coordinate of the point to be transformed, in the root
	 *            coordinate system
	 * @param level
	 *            The tile level required
	 * @param xBins
	 *            The number of bins along the horizontal dimension of the
	 *            requested tile. This doesn't make any difference in choice of
	 *            tile, but is included in the return value, so we allow passing
	 *            it in so as to avoid the need to change the return value.
	 * @param yBins
	 *            The number of bins along the vertical dimension of the
	 *            requested tile. This doesn't make any difference in choice of
	 *            tile, but is included in the return value, so we allow passing
	 *            it in so as to avoid the need to change the return value.
	 * @return The tile into which (x, y) falls at the given level
	 */
	public TileIndex rootToTile (double x, double y, int level, int xBins, int yBins);

	/**
	 * Convert from root to a particular bin coordinate
	 *
	 * @param point
	 *            the coordinates of the point to be transformed, in the root
	 *            coordinate system
	 * @param tile
	 *            The tile used to define which bin coordinate system is to be
	 *            used.
	 * @return The bin into which the given point falls
	 */
	public BinIndex rootToBin (Point2D point, TileIndex tile);

	/**
	 * Convert from root to a particular bin coordinate
	 *
	 * @param x
	 *            The x coordinate of the point to be transformed, in the
	 *            root coordinate system
	 * @param y
	 *            The y coordinate of the point to be transformed, in the
	 *            root coordinate system
	 * @param tile
	 *            The tile used to define which bin coordinate system is to be
	 *            used.
	 * @return The bin into which the given point falls
	 */
	public BinIndex rootToBin (double x, double y, TileIndex tile);

	/**
	 * Get the root coordinates of the bounds of a tile
	 *
	 * @param tile
	 *            The tile in question
	 * @return The root coordinates of the tile's bounds
	 */
	public Rectangle2D getTileBounds (TileIndex tile);

	/**
	 * Get the root coordinates of the bounds of a bin
	 *
	 * @param tile
	 *            The tile in which the bin lies
	 * @param bin
	 *            the bin in question
	 * @return The root coordinates of the bin's bounds
	 */
	public Rectangle2D getBinBounds (TileIndex tile, BinIndex bin);

	/**
	 * Get the percent of the area of the bin that is overlapped by the given
	 * root-coordinate area.
	 *
	 * @param tile
	 *            The tile of interest
	 * @param bin
	 *            The bin of interest in the tile
	 * @param area
	 *            The area, in root coordinates, whose overlap with this bin is
	 *            required
	 * @return A proportion (0-1) indicating how much of the bin of interest is
	 *         overlapped by the root-coordinate area of interest. This overlap
	 *         should be calculated in bin space, not root space.
	 */
	public double getBinOverlap (TileIndex tile, BinIndex bin, Rectangle2D area);

	/**
	 * Given a set of root bounds, gets the tile or tiles that should be taken
	 * as the top of the pyramid one would use to investigate data within those
	 * bounds
	 *
	 * @param bounds
	 *            The bounds of the area of interest, in root coordinates.
	 * @return A tile or set of tiles best used to look at the area of interest.
	 */
	public Collection<TileIndex> getBestTiles (Rectangle2D bounds);

	/**
	 * Returns a human-readable name for the projection used by this pyramid
	 * @return Human-readable name for the projection.
	 */
	public String getProjection ();

	/**
	 * Returns a human-readable name for the way tiles are laid out in this pyramid
	 * @return Human-readable name for the tile scheme.
	 */
	public String getTileScheme ();


	public Rectangle2D getBounds();
}
