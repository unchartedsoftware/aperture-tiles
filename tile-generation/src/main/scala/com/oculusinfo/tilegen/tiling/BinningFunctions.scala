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
package com.oculusinfo.tilegen.tiling

import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex




object StandardBinningFunctions
		extends StandardPointBinningFunctions
		with StandardLinearBinningFunctions
		with StandardArcBinningFunctions
{
}



/**
 * A repository of standard index location and tile population functions for point inputs,
 * for use with the UniversalBinner
 */
trait StandardPointBinningFunctions {
	/**
	 * Simple function to spread an input point over several levels of tile pyramid.
	 */
	def locateIndexOverLevels[T](indexScheme: IndexScheme[T], pyramid: TilePyramid,
	                             levels: Traversable[Int], xBins: Int = 256, yBins: Int = 256)
			: T => Traversable[(TileIndex, Array[BinIndex])] =
		index => {
			val (x, y) = indexScheme.toCartesian(index)
			levels.map{level =>
				val tile = pyramid.rootToTile(x, y, level, xBins, yBins)
				val bin = pyramid.rootToBin(x, y, tile)
				(tile, Array(bin))
			}
		}



	/**
	 * Simple function to spread an input point over several levels of tile pyramid, ignoring
	 * points that are out of bounds
	 */
	def locateBoundedIndexOverLevels[T](indexScheme: IndexScheme[T], pyramid: TilePyramid,
	                                    levels: Traversable[Int], xBins: Int = 256, yBins: Int = 256)
			: T => Traversable[(TileIndex, Array[BinIndex])] = {
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		val (minX, minY, maxX, maxY) = (bounds.getMinX, bounds.getMinY,
		                                bounds.getMaxX, bounds.getMaxY)

		index => {
			val (x, y) = indexScheme.toCartesian(index)
			if (minX <= x && x < maxX && minY <= y && y < maxY) {
				levels.map{level =>
					val tile = pyramid.rootToTile(x, y, level, xBins, yBins)
					val bin = pyramid.rootToBin(x, y, tile)
					(tile, Array(bin))
				}
			} else {
				Traversable()
			}
		}
	}



	/**
	 * Simple population function that just takes input points and outputs them, as is, in the
	 * correct coordinate system.
	 */
	def populateTileIdentity[T]: (TileIndex, Array[BinIndex], T) => Map[BinIndex, T] =
		(tile, bins, value) => bins.map(bin => (TileIndex.universalBinIndexToTileBinIndex(tile, bin).getBin, value)).toMap

}



/**
 * A repository of standard index location and tile population functions for line inputs,
 * generating linear segment output, for use with the UniversalBinner
 */
trait StandardLinearBinningFunctions {
	/**
	 * Simple function to spread an input lines over several levels of tile pyramid.
	 *
	 * @param indexScheme The scheme for interpretting input indices
	 * @param pyramid The tile pyramid for projecting interpretted indices into tile space.
	 * @param levels The levels at which to tile
	 * @param minBins The minimum length of a segment, in bins, below which it is not drawn, or None
	 *                to have no minimum segment length
	 * @param maxBins The maximum length of a segment, in bins, above which it is not drawn, or None
	 *                to have no minimum segment length
	 * @param xBins The number of bins into which each tile is broken in the horizontal direction
	 * @param yBins the number of bins into which each tile is broken in the vertical direction
	 * @return a traversable over the tiles this line crosses, each associated with the overall
	 *         endpoints of this line, in universal bin coordinates.
	 */
	def locateLine[T](indexScheme: IndexScheme[T], pyramid: TilePyramid, levels: Traversable[Int],
	                  minBins: Option[Int], maxBins: Option[Int],
	                  xBins: Int = 256, yBins: Int = 256)
			: T => Traversable[(TileIndex, Array[BinIndex])] = {
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		val (minX, minY, maxX, maxY) = (bounds.getMinX, bounds.getMinY,
		                                bounds.getMaxX, bounds.getMaxY)

		index => {
			val (x1, y1, x2, y2) = indexScheme.toCartesianEndpoints(index)
			if (minX <= x1 && x1 <= maxX &&
				    minY <= y1 && y1 <= maxY &&
				    minX <= x2 && x2 <= maxX &&
				    minY < y2 && y2 <= maxY) {
				levels.flatMap{level =>
					val tile1 = pyramid.rootToTile(x1, y1, level, xBins, yBins)
					val tileBin1 = pyramid.rootToBin(x1, y1, tile1)
					val uniBin1 = TileIndex.tileBinIndexToUniversalBinIndex(tile1, tileBin1)

					val tile2 = pyramid.rootToTile(x2, y2, level, xBins, yBins)
					val tileBin2 = pyramid.rootToBin(x2, y2, tile2)
					val uniBin2 = TileIndex.tileBinIndexToUniversalBinIndex(tile2, tileBin2)

					val length = (math.abs(uniBin1.getX - uniBin2.getX) max
						              math.abs(uniBin1.getY - uniBin2.getY))

					if (minBins.map(_ <= length).getOrElse(true) &&
						    maxBins.map(_ > length).getOrElse(true)) {
						// Fill in somewhere around here.
						linearTiles(uniBin1, uniBin2, tile1).map(tile => (tile, Array(uniBin1, uniBin2)))
					} else {
						Traversable()
					}
				}
			} else {
				Traversable()
			}
		}
	}



	/**
	 * Line segment population function
	 *
	 * Takes endpoints of line segments, and populates the tiles with the points appropriate to that tile
	 */
	def populateTileWithLineSegments[T]: (TileIndex, Array[BinIndex], T) => Map[BinIndex, T] =
		(tile, bins, value) => {
			linearBinsForTile(bins(0), bins(1), tile).map(bin => (bin, value)).toMap
		}



	/*
	 * Re-order coords of two endpoints for efficient implementation of Bresenham's line algorithm
	 */
	private def  initializeBresenham (start: BinIndex, end: BinIndex)
			: (Boolean, Int, Int, Int, Int) = {
		val xs = start.getX()
		val xe = end.getX()
		val ys = start.getY()
		val ye = end.getY()
		val steep = (math.abs(ye - ys) > math.abs(xe - xs))

		if (steep) {
			if (ys > ye) {
				(steep, ye, xe, ys, xs)
			} else {
				(steep, ys, xs, ye, xe)
			}
		} else {
			if (xs > xe) {
				(steep, xe, ye, xs, ys)
			} else {
				(steep, xs, ys, xe, ye)
			}
		}
	}



	/**
	 * Compute the intermediate points between two endpoints using Bresneham's algorithm
	 *
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @return Each bin in the segment, in universal bin coordinates
	 */
	def linearUniversalBins (start: BinIndex, end: BinIndex): Traversable[BinIndex] = {
		val (steep, x0, y0, x1, y1) = initializeBresenham(start, end)

		val deltax = x1-x0
		val deltay = math.abs(y1-y0)
		var error = deltax>>1
		var y = y0
		val ystep = if (y0 < y1) 1 else -1

		// x1+1 needed here so that "end" bin is included in Sequence
		Iterable.range(x0, x1+1).map{x =>
			val ourY = y
			error = error - deltay
			if (error < 0) {
				y = y + ystep
				error = error + deltax
			}

			if (steep) new BinIndex(ourY, x)
			else new BinIndex(x, ourY)
		}
	}



	/**
	 * Compute the tiles between two endpoints, using a modified version of Bresneham's
	 * algorithm, in a way that should be completely self-consistent with a Bresneham-based bin
	 * extraction function.
	 *
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @param sample A sample tile, indicating the level and tile size of the desired output tiles
	 * @return Each tile in the segment, in universal bin coordinates
	 */
	def linearTiles (start: BinIndex, end: BinIndex, sample: TileIndex)
			: Traversable[TileIndex] = {
		closeLinearTiles(start, end, sample, Int.MaxValue)
	}



	/**
	 * Compute all the bins on a single tile that are on the line between two given endpoints,
	 * using a modified version of Bresneham's algorithm, and in a way that guarantees
	 * consistency between this and a total-line Bresneham-based line-drawing function.
	 *
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @param tile The tile whose bins are desired
	 * @return Each bin in the given tile on this line, in tile coordinates.
	 */
	def linearBinsForTile (start: BinIndex, end: BinIndex, tile: TileIndex): Traversable[BinIndex] = {
		closeLinearBinsForTile(start, end, tile, Int.MaxValue)
	}



	private def axialDistance (a: BinIndex, b: BinIndex): Int =
		math.max(math.abs(a.getX-b.getX), math.abs(a.getY-b.getY))

	/**
	 * Compute the tiles between two endpoints, using a modified version of Bresneham's
	 * algorithm, in a way that should be completely self-consistent with a Bresneham-based bin
	 * extraction function, and only returns tiles within D bins of an endpoint.
	 *
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @param sample A sample tile, indicating the level and tile size of the desired output tiles
	 * @param maxBinDistance The maximum distance D allowed between either endpoint and a returned
	 *                       tile; any tiles farther than this from an endpoint will not be
	 *                       returned, even if they are on the line.  Distance is calculated as
	 *                       minimal axial distance, not euclidean distance.
	 * @return Each tile in the segment, in universal bin coordinates
	 */
	def closeLinearTiles (start: BinIndex, end: BinIndex, sample: TileIndex, maxBinDistance: Int)
	: Traversable[TileIndex] = {
		val (steep, x0, y0, x1, y1) = initializeBresenham(start, end)

		val singleTileGap = if (steep) sample.getYBins else sample.getXBins
		val isTileGap = maxBinDistance != Int.MaxValue && axialDistance(start, end) > 2L*maxBinDistance + singleTileGap

		val (xSize, ySize) =
			if (steep) (sample.getYBins, sample.getXBins)
			else (sample.getXBins, sample.getYBins)
		val level = sample.getLevel

		val deltax: Long = x1 - x0
		val deltay: Long = math.abs(y1 - y0)
		val baseError: Long = deltax >> 1
		val ystep = if (y0 < y1) 1 else -1

		// Function to convert from universal bin to tile quickly and easily
		def binToTile(x: Int, y: Int) =
			if (steep) TileIndex.universalBinIndexToTileBinIndex(sample, new BinIndex(y, x)).getTile
			else TileIndex.universalBinIndexToTileBinIndex(sample, new BinIndex(x, y)).getTile

		// Find nth bin from scratch
		def tileX(x: Int) = {
			val dx = x - x0
			val e = baseError - deltay * dx
			val y = if (e < 0) {
				val factor = math.ceil(-e.toDouble / deltax).toInt
				y0 + factor * ystep
			} else {
				y0
			}
			binToTile(x, y)
		}

		// Determine the start of the range of internal tiles
		val t0 = (x0 + xSize - (x0 % xSize)) / xSize
		// Determine the end of the range of internal tiles
		val tn = (x1 - (x1 % xSize)) / xSize

		// Determine the end of the range of internal tiles
		val x11 = x1 - (x1 % xSize)
		val t1 = x11 / xSize

		// Determine first and last tiles
		val tile0 = binToTile(x0, y0)
		val tile0a = tileX(t0 * xSize - 1)
		val tile1a = tileX(t1 * xSize)
		val tile1 = binToTile(x1, y1)
		val initialTiles = if (tile0 == tile0a || t0 > t1) Traversable(tile0) else Traversable(tile0, tile0a)
		val finalTiles = if (tile1 == tile1a || t0 > t1) Traversable(tile1) else Traversable(tile1a, tile1)

		val intermediateTiles =
		if (isTileGap) {
			// Determine the end of the lead tiles
			val x0f = x0 + maxBinDistance
			val t0f = (x0f + xSize - (x0f % xSize)) / xSize
			// Determine the start of the trailing tiles
			val x1s = x1 - maxBinDistance
			val tns = (x1s - (x1s % xSize)) / xSize

			Iterable.range(t0, t0f).flatMap { t =>
				val startTile = tileX(t * xSize)
				val endTile = tileX((t + 1) * xSize - 1)

				if (startTile == endTile) Traversable(startTile) else Traversable(startTile, endTile)
			} ++ Iterable.range(tns, tn).flatMap{t =>
				val startTile = tileX(t * xSize)
				val endTile = tileX((t + 1) * xSize - 1)

				if (startTile == endTile) Traversable(startTile) else Traversable(startTile, endTile)
			}
		} else {
			Iterable.range(t0, tn).flatMap{t =>
				val startTile = tileX(t * xSize)
				val endTile = tileX((t + 1) * xSize - 1)

				if (startTile == endTile) Traversable(startTile) else Traversable(startTile, endTile)
			}
		}

		initialTiles ++ intermediateTiles ++ finalTiles
	}

	/**
	 * Compute all the bins on a single tile that are on the line between two given endpoints,
	 * using a modified version of Bresneham's algorithm, and in a way that guarantees
	 * consistency between this and a total-line Bresneham-based line-drawing function, and
	 * only returns bins within D bins of an endpoint
	 *
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @param tile The tile whose bins are desired
	 * @param maxBinDistance The maximum distance D allowed between either endpoint and a returned
	 *                       bin.  Distance is calculated as minimum axial distance, not euclidean
	 *                       distance.
	 * @return Each bin in the given tile on this line, in tile coordinates.
	 */
	def closeLinearBinsForTile (start: BinIndex, end: BinIndex, tile: TileIndex, maxBinDistance: Int): Traversable[BinIndex] = {
		val (steep, x0, y0, x1, y1) = initializeBresenham(start, end)

		val deltax: Long = x1 - x0
		val deltay: Long = math.abs(y1 - y0)
		var error: Long = deltax >> 1
		var y = y0
		val ystep = if (y0 < y1) 1 else -1

		// Figure out the bounds of this tile in our x direction
		val tileMin = TileIndex.tileBinIndexToUniversalBinIndex(tile, new BinIndex(0, 0))
		val tileMax = TileIndex.tileBinIndexToUniversalBinIndex(tile, new BinIndex(tile.getXBins-1, tile.getYBins-1))
		val (minX, maxX, minY, maxY) =
			if (steep) (tileMin.getY, tileMax.getY, tileMin.getX, tileMax.getX)
			else (tileMin.getX, tileMax.getX, tileMin.getY, tileMax.getY)

		val xx0 = x0 max minX
		val xx1 = (x1 min maxX) + 1

		// Offset to our start location
		val startOffset = xx0 - x0
		error = error - startOffset * deltay
		if (error < 0) {
			val factor = math.ceil(-error.toDouble / deltax).toInt
			error = error + factor * deltax
			y = y + factor*ystep
		}

		// Get the universal bin of our lower left corner (for offsets)
		var baseX = if (steep) tileMax.getY else tileMin.getX
		var baseY = if (steep) tileMin.getX else tileMax.getY

		// And iterate over our range
		Iterable.range(xx0, xx1).flatMap{x =>
			val curY = y
			error = error - deltay
			if (error < 0) {
				y = y + ystep
				error = error + deltax
			}

			if (minY <= curY && curY <= maxY) {
				val uBin = if (steep) new BinIndex(curY, x) else new BinIndex(x, curY)

				if (maxBinDistance == Int.MaxValue || axialDistance(uBin, start) <= maxBinDistance || axialDistance(uBin, end) <= maxBinDistance) {
					Some(TileIndex.universalBinIndexToTileBinIndex(tile, uBin).getBin)
				} else None
			}
			else None
		}
	}
}



trait StandardArcBinningFunctions {
	/**
	 * Takes the two endpoints of the desired arc, and returns the center, radius, start slope, end
	 * slope, and a list of the needed octants.
	 *
	 * We assume a 60 degree arc. with the center on the RHS of the line, when travelling from the
	 * first to the second point (which means the arc goes counter-clockwise).
	 *
	 * With 0deg being due east, octant 0 is from 0-45 degrees, octant 1 from 45-90 degrees, etc.
	 *
	 * Each octant is annotated with whether it is the initial and whether it is the final octant.
	 */
	private[tiling] def initializeArc (start: BinIndex, end: BinIndex)
			: (Double, Double, Double, Double, Double, Seq[(Int, Boolean, Boolean)]) = {
		val x1 = start.getX
		val y1 = start.getY
		val x2 = end.getX
		val y2 = end.getY
		val dx = x2-x1
		val dy = y2-y1

		// Assuming a 60 degree arc for now
		val arcLength = math.Pi/3

		// Since it's a 60 degree arc, the two endpoints and the center form an equilateral
		// triangle, so the distance between the endpoints is the radius.
		val radius = math.sqrt(dx*dx+dy*dy)
		// If we weren't using a guaranteed equilateral triangle, we would instead use
		// val radius = math.sqrt(dx*dx+dy*dy)/(2*math.sin(arcLength/2))

		// Go from the midpoint of our chord to the midpoint of the circle
		// The amount by which to scale the radius to get to the center
		val chordRadiusScale = math.sqrt(3)/2.0
		// If not equilatieral, this should be
		// val chordRadiusScale = math.cos(arcLength/2)
		val xc = (x1+x2)/2.0 + dy * chordRadiusScale
		val yc = (y1+y2)/2.0 - dx * chordRadiusScale

		// Find the relevant octants
		def findOctant (x: Double, y: Double, isStart: Boolean): Int = {
			if (x == 0.0)      if ((isStart && y >= 0.0) || (!isStart && y <= 0.0)) 0 else 4
			else if (y == 0.0) if ((isStart && x > 0.0) || (!isStart && x < 0.0)) 2 else 6
			else if (x > 0.0 && y > 0.0) if (x > y) 0 else 1
			else if (x < 0.0 && y > 0.0) if (y > -x) 2 else 3
			else if (x < 0.0 && y < 0.0) if (-x > -y) 4 else 5
			else if (-y > x) 6 else 7
		}
		val startOctant = findOctant(x1-xc, y1-yc, true)
		val endOctant = findOctant(x2-xc, y2-yc, false)
		val octants =
			(if (endOctant < startOctant) (endOctant to startOctant)
			 else (endOctant to (startOctant+8)).map(_ % 8))
				.map(octant =>
				(octant, octant == startOctant, octant == endOctant))

		(xc,
		 yc,
		 radius,
		 (y1-yc)/(x1-xc),
		 (y2-yc)/(x2-xc),
		 octants)
	}



	def arcUniversalBins (start: BinIndex, end: BinIndex): Traversable[BinIndex] = {
		def octantTransform (x: Int, y: Int, octant: Int): (Int, Int) =
			octant match {
				case 0 => (x, y)
				case 1 => (y, x)
				case 2 => (-y, x)
				case 3 => (-x, y)
				case 4 => (-x, -y)
				case 5 => (-y, -x)
				case 6 => (y, -x)
				case 7 => (x, -y)
			}

		val x0 = start.getX
		val y0 = start.getY
		val x1 = end.getX
		val y1 = end.getY
		val (xc, yc, radius, startSlope, endSlope, octants) = initializeArc(start, end)

		// Offset from y from 0 so the y coordinate is the center of its column.
		var yOffset = math.round(yc) - yc
		var y = yOffset
		// x1^2 = x0^2 - 2 y0 dy - dy^2, and y0 = 0
		var x2 = radius*radius - yOffset*yOffset
		var x = math.sqrt(x2)

		new WhileIterator(
			() => x >= y,
			() => {
				val curX = x
				val curY = y

				x2 = x2 - 2 * y - 1
				y = y + 1
				var nextX = math.round(x)-0.5
				if (x2 <= nextX*nextX) x = x - 1

				(math.round(curX).toInt, math.round(curY).toInt)
			}
		).flatMap{case (x, y) =>
				octants.flatMap{octant =>
					val (xr, yr) = octantTransform(x, y, octant._1)
					val slope = yr.toDouble/xr
					if ((octant._2 && slope <= startSlope) ||
						    (octant._3 && slope >= endSlope) ||
						    (!(octant._2 || octant._3))) {
						Some(new BinIndex(math.round(xc+xr).toInt, math.round(yc+yr).toInt))
					} else {
						None
					}
				}
		}.toTraversable
	}




	def arcUniversalBins2 (start: BinIndex, end: BinIndex): TraversableOnce[BinIndex] = {
		def octantTransform (x: Int, y: Int, octant: Int): (Int, Int) =
			octant match {
				case 0 => (x, y)
				case 1 => (y, x)
				case 2 => (-y, x)
				case 3 => (-x, y)
				case 4 => (-x, -y)
				case 5 => (-y, -x)
				case 6 => (y, -x)
				case 7 => (x, -y)
			}

		val x0 = start.getX
		val y0 = start.getY
		val x1 = end.getX
		val y1 = end.getY
		val (xc, yc, radius, startSlope, endSlope, octants) = initializeArc(start, end)

		// Rotate so everything is E of the Y axis
		//
		// 60 degree arcs should never inhabit more than three quadrants, so we can rotate them
		// so they are on the right side.
		val rotation: Int = {
			def isGoodRotation (r: Int): Boolean = {
				val min = octants.map(oct => (oct._1 + r) % 8).reduce(_ min _)
				val max = octants.map(oct => (oct._1 + r) % 8).reduce(_ max _)
				((6 == min && 7 == max) || (0 == min && 1 == max) || (0 == min && 7 == max))
			}
			var tmpRot = 0
			while (!isGoodRotation(tmpRot)) tmpRot = tmpRot + 2
			tmpRot
		}

		def rotate [@specialized(Double, Int) N: Numeric] (x: N, y: N, rotation: Int): (N, N) = {
			val num: Numeric[N] = implicitly[Numeric[N]]
			import num.mkNumericOps

			rotation match {
				case -6 => (-y, x)
				case -4 => (-x, -y)
				case -2 => (y, -x)
				case 0 => (x, y)
				case 2 => (-y, x)
				case 4 => (-x, -y)
				case 6 => (y, -x)
				case _ => throw new IllegalArgumentException("Bad rotation "+rotation)
			}
		}

		// Get the endpoint coordinates relative to the center, rotated into the correct position
		val (x0r, y0r) = rotate(x0 - xc, y0 - yc, rotation)
		val (x1r, y1r) = rotate(x1 - xc, y1 - yc, rotation)
		val (xcr, ycr) = rotate(xc, yc, rotation)

		var y = math.round(y1r+ycr)-0.5-ycr
		val yEnd = math.round(y0r+ycr)-0.5-ycr
		val r2 = radius*radius
		new WhileIterator(
			() => y <= yEnd,
			() => {
				val yCur = y
				y = y + 1
				yCur
			}
		).flatMap{y =>
			val ypr = math.round(ycr+y).toInt
			// x range from the start of the bin to the end of the bin
			val ya = y max y1r
			val x2ad = math.sqrt(r2 - (ya * ya)) + xcr
			val yb = (y+1) min y0r
			val x2bd = math.sqrt(r2 - (yb * yb)) + xcr
			val x2a = math.round(math.sqrt(r2 - (ya * ya)) + xcr).toInt
			val x2b = math.round(math.sqrt(r2 - (yb * yb)) + xcr).toInt

			((x2a min x2b) to (x2a max x2b)).map{xpr =>
				val (xp, yp) = rotate(xpr, ypr, -rotation)
				new BinIndex(xp, yp)
			}
		}
	}
}

class WhileIterator[T] (more: () => Boolean, fcn: () => T) extends Iterator[T] {
	def hasNext: Boolean = {
		val res = more()
		res
	}
	def next(): T = {
		val res = fcn()
		res
	}
}
