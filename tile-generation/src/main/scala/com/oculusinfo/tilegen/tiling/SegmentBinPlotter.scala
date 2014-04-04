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



import java.lang.{Double => JavaDouble}
import java.awt.geom.Point2D

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TileAndBinIndices
import com.oculusinfo.binning.impl.AOITilePyramid

import com.oculusinfo.tilegen.util.ArgumentParser



/**
 * Base class for both line segment binners, in which is collected common
 * elements of both
 *
 * @param tileScheme The pyramidder used to transform raw coordinates into
 *                   tiles and bins
 * @param minBins The minimum number of bins that a segment must cross at a
 *                given zoom level for it to be used in tiles at that zoom
 *                level.  Essentially, if a segment doesn't cross this many
 *                bins, it is too short to be drawn at the given zoom level.
 * @param maxBins The maximum number of bins that a segment may cross at a
 *                given zoom level for it to be used in tiles at that zoom
 *                level.  Essentially, if a segment crosses too many bins,
 *                it is too long to be drawn at a given zoom level - it
 *                represents data at a higher level.
 */
abstract protected class LineSegmentBinnerBase (tileScheme: TilePyramid,
                                                minBins: Int,
                                                maxBins: Int) {
	protected def getMinBins = minBins
	protected def getMaxBins = maxBins
	protected def getTileScheme = tileScheme
	var debug: Boolean = false



	/**
	 * Transforms a set of pairs of points to a level or set of levels in a tile
	 * set, indicating how many lines intersected each bin.  Write out the
	 * resultant tile level or levels, as well as returning them.
	 */
	def binAndWriteData (data: RDD[((Double, Double), (Double, Double))],
	                     writeLocation: String,
	                     tileIO: TileIO,
	                     levels: List[Int],
	                     name: String = "unknown",
	                     description: String = "unknown") = {
		val startTime = System.currentTimeMillis()
		val tiles = processData(data, levels);
		val binDesc = new StandardDoubleBinDescriptor
		tileIO.writeTileSet(getTileScheme, writeLocation, tiles, binDesc, name, description)
		val endTime = System.currentTimeMillis()

		if (debug) {
			println("Finished binning line segment data set " + name + " into " + levels.size
				        + " levels (" + levels.mkString(",") + ") in "
				        + ((endTime-startTime)/60000.0) + " minutes")
		}

		tiles
	}

	def processData(data: RDD[((Double, Double), (Double, Double))],
	                levels: List[Int]): RDD[TileData[JavaDouble]];



	/**
	 * Determine all bins that are required to draw a line two endpoint bins.
	 *
	 * Bresenham's algorithm for filling in the intermediate pixels in a line.
	 *
	 * From wikipedia
	 * 
	 * @param start
	 *        The start bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param end
	 *        The end bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @return All bins, in universal bin coordinates, falling on the direct
	 *         line between the two endoint bins.
	 */
	protected val endpointsToUniversalBins: (BinIndex, BinIndex) => IndexedSeq[BinIndex] =
		(start, end) => {
			// Bresenham's algorithm, as per wikipedia
			var steep = math.abs(end.getY() - start.getY()) > math.abs(end.getX() - start.getX())

			var (x0, y0, x1, y1) =
				if (steep) {
					if (start.getY() > end.getY()) {
						(end.getY(), end.getX(), start.getY(), start.getX())
					} else {
						(start.getY(), start.getX(), end.getY(), end.getX())
					}
				} else {
					if (start.getX() > end.getX()) {
						(end.getX(), end.getY(), start.getX(), start.getY())
					} else {
						(start.getX(), start.getY(), end.getX(), end.getY())
					}
				}

			val deltax = x1-x0
			val deltay = math.abs(y1-y0)
			var error = deltax/2
			var y = y0
			val ystep = if (y0 < y1) 1 else -1

			Range(x0, x1).map(x =>
				{
					val ourY = y
					error = error - deltay
					if (error < 0) {
						y = y + ystep
						error = error + deltax
					}

					if (steep) new BinIndex(ourY, x)
					else new BinIndex(x, ourY)
				}
			)
		}




	/**
	 * Determine all tiles required to draw a line between two endpoint bins.
	 *
	 * @param baseTile
	 *        A sample tile specifying level and number of bins of all required
	 *        results.
	 * @param start
	 *        The start bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param end
	 *        The end bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @return All tiles falling on the direct line (in universal bin
	 *         coordinates) between the two endpoint bins
	 */
	protected val universalBinsToTiles:
			(TileIndex, IndexedSeq[BinIndex]) => Traversable[TileIndex] =
		(baseTile, bins) => {
			bins.map(ubin =>
				{
					val tb = TileIndex.universalBinIndexToTileBinIndex(baseTile, ubin)
					tb.getTile()
				}
			).toSet
			// transform to set to remove duplicates
		}

	/**
	 * Determine all bins within a given tile that are required to draw a line
	 * between two endpoint bins.
	 *
	 * @param tile
	 *        The tile of interest
	 * @param start
	 *        The start bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param end
	 *        The end bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @return All bins, in tile bin coordinates, in the given tile, falling on
	 *         the direct line (in universal bin coordinates) between the two
	 *         endoint bins.
	 */
	protected val universalBinsToBins:
			(TileIndex, IndexedSeq[BinIndex]) => IndexedSeq[BinIndex] =
		(tile, bins) => {
			bins.map(ubin =>
				TileIndex.universalBinIndexToTileBinIndex(tile, ubin)
			).filter(_.getTile().equals(tile)).map(_.getBin())
		}
}



/**
 * A class to take pairs of points, put them in bin space, and fill in the
 * intervening bins in a straight line from one to the other.
 *
 * Lines are only filled in if they intersect between a number of bins (not
 * tiles) in a fixed range set by the caller.
 *
 * This version converts data to bin, and then consolidates bins.
 *
 * This naive approach is generally much slower than the more efficient
 * approach below - but not universally.  At high zoom levels, or any
 * circumstance where segments are sparse, this may be more efficient.
 */
class LineSegmentPointBinner (tileScheme: TilePyramid,
                              minBins: Int = 1 << 1,
                              maxBins: Int = 1 << 10)
		extends LineSegmentBinnerBase(tileScheme, minBins, maxBins) {
	def processData(data: RDD[((Double, Double), (Double, Double))],
	                levels: List[Int]): RDD[TileData[JavaDouble]] =
		consolidate(binData(data, levels))



	/*
	 * Figure out in which bins all the data lies on the given zoom levels
	 */
	def binData (data: RDD[((Double, Double), (Double, Double))],
	             levels: List[Int]): RDD[((TileIndex, BinIndex), Int)] = {
		val minPts = getMinBins
		val maxPts = getMaxBins
		val pixelator = endpointsToUniversalBins
		val ts = getTileScheme

		data.flatMap(p =>
			{
				val pt1 = new Point2D.Double(p._1._1, p._1._2)
				val pt2 = new Point2D.Double(p._2._1, p._2._2)

				levels.flatMap(level =>
					{
						val tile1 = ts.rootToTile(pt1, level)
						val tileBin1 = ts.rootToBin(pt1, tile1)
						val bin1 = TileIndex.tileBinIndexToUniversalBinIndex(tile1, tileBin1)


						val tile2 = ts.rootToTile(pt2, level)
						val tileBin2 = ts.rootToBin(pt2, tile2)
						val bin2 = TileIndex.tileBinIndexToUniversalBinIndex(tile2, tileBin2)

						val points = math.abs(bin1.getX()-bin2.getX()) max math.abs(bin1.getY()-bin2.getY())
						if (points < minPts || maxPts < points) {
							List[((TileIndex, BinIndex), Int)]()
						} else {
							pixelator(bin1, bin2).map(bin =>
								{
									val tb = TileIndex.universalBinIndexToTileBinIndex(tile1, bin);
									((tb.getTile(), tb.getBin()), 1)
								}
							)
						}
					}
				)
			}
		)
	}

	/*
	 * Consolidate individual bins into tiles
	 */
	private def consolidate (data: RDD[((TileIndex, BinIndex), Int)]):
			RDD[TileData[JavaDouble]] = {
		data.reduceByKey(_ + _).map(p =>
			// Remap to key by tile, and have the value be (bin, count) pairs
			(p._1._1, (p._1._2, p._2))
		).groupByKey().map(t => {
			                   val tile = t._1
			                   val bins:Map[BinIndex, Int] = t._2.toMap
			                   val xLimit = tile.getXBins()
			                   val yLimit = tile.getYBins()
			                   val counter = new TileData[JavaDouble](tile)

			                   for (x <- 0 until xLimit) {
				                   for (y <- 0 until yLimit) {
					                   val bin = new BinIndex(x, y)
					                   val value: Int = bins.get(bin).getOrElse(0)
					                   counter.setBin(x, y, value)
				                   }
			                   }
			                   counter
		                   })
	}
}






/**
 * A class to take pairs of points, put them in bin space, and fill in the
 * intervening bins in a straight line from one to the other.
 *
 * Lines are only filled in if they intersect between a number of bins (not
 * tiles) in a fixed range set by the caller.
 *
 * This version converts data to tiles, then on each tile, runs over all
 * segments that intersect that tile to populate the tile.
 *
 * This is more efficient in most circumstances than the naive approach
 * above - notably at low zoom levels, where there are a lot of tiles (and
 * a lot of bins).
 */
class LineSegmentTileBinner (tileScheme: TilePyramid,
                             minBins: Int = 1 << 1,
                             maxBins: Int = 1 << 10)
		extends LineSegmentBinnerBase(tileScheme, minBins, maxBins)  {
	def processData(data: RDD[((Double, Double), (Double, Double))],
	                levels: List[Int]): RDD[TileData[JavaDouble]] = {
		val minPts = getMinBins
		val maxPts = getMaxBins
		val ts = getTileScheme
		val pixelator = endpointsToUniversalBins
		val tiler = universalBinsToTiles
		val binner = universalBinsToBins

		// Figure out which segments intersect each tile
		val tiledData = data.flatMap(p => {
			                             val pt1 = new Point2D.Double(p._1._1, p._1._2)
			                             val pt2 = new Point2D.Double(p._2._1, p._2._2)

			                             levels.flatMap(level => {
				                                            val tile1 = ts.rootToTile(pt1, level)
				                                            val tileBin1 = ts.rootToBin(pt1, tile1)
				                                            val bin1 = TileIndex.tileBinIndexToUniversalBinIndex(tile1, tileBin1)

				                                            val tile2 = ts.rootToTile(pt2, level)
				                                            val tileBin2 = ts.rootToBin(pt2, tile2)
				                                            val bin2 = TileIndex.tileBinIndexToUniversalBinIndex(tile2, tileBin2)

				                                            val points = math.abs(bin1.getX()-bin2.getX()) max math.abs(bin1.getY()-bin2.getY())
				                                            if (points < minPts || maxPts < points) {
					                                            List[(TileIndex, (BinIndex, BinIndex))]()
				                                            } else {
					                                            tiler(tile1, pixelator(bin1, bin2)).map(tile =>
						                                            (tile, (bin1, bin2))
					                                            )
				                                            }
			                                            })
		                             })

		// Consolidate segments for each tile index, and draw a tile data based on
		// the consolidated results
		tiledData.groupByKey().map(t =>
			{
				val tile: TileIndex = t._1
				val segments: Seq[(BinIndex, BinIndex)] = t._2

				val xLimit = tile.getXBins()
				val yLimit = tile.getYBins()
				val counter = new TileData[JavaDouble](tile)

				segments.foreach(segment =>
					{
						binner(tile, pixelator(segment._1, segment._2)).foreach(bin =>
							{
								val x = bin.getX()
								val y = bin.getY()
								counter.setBin(x, y, counter.getBin(x, y)+1)
							}
						)
					}
				)

				counter
			}
		)
	}
}
