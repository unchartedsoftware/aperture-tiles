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



import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import scala.collection.TraversableOnce
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.BinIterator
import com.oculusinfo.binning.DensityStripData
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.TileAndBinIndices

import com.oculusinfo.tilegen.datasets.ValueDescription


class LineSegmentIndexScheme extends IndexScheme[(Double, Double, Double, Double)] with Serializable { 
	def toCartesianEndpoints (coords: (Double, Double, Double, Double)): (Double, Double, Double, Double) = coords
	def toCartesian (coords: (Double, Double, Double, Double)): (Double, Double) = (coords._1, coords._2)
}


/**
 * This takes an RDD of line segment data (ie pairs of endpoints) and transforms it
 * into a pyramid of tiles.  minBins and maxBins define the min and max valid rane for
 * line segments that are included in the binning process
 * 
 *
 * @param tileScheme the type of tile pyramid this binner can bin.
 */
object RDDLineBinner {
	
	def getNumSplits[T: ClassTag] (requestedPartitions: Option[Int], dataSet: RDD[T]): Int = {
		val curSize = dataSet.partitions.size
		val result = curSize max requestedPartitions.getOrElse(0)
		result
	}

	def getPoints (start: BinIndex, end: BinIndex): (Boolean, Int, Int, Int, Int) = {
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
	protected val endpointsToLineBins: (BinIndex, BinIndex) => IndexedSeq[BinIndex] =
		(start, end) => {

			// Bresenham's algorithm
			val (steep, x0, y0, x1, y1) = getPoints(start, end)
			val deltax = x1-x0
			val deltay = math.abs(y1-y0)
			var error = deltax>>1
			var y = y0
			val ystep = if (y0 < y1) 1 else -1

			// x1+1 needed here so that "end" bin is included in Sequence
			Range(x0, x1+1).map(x =>
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
	 * Determine all bins that are required to draw an arc between two endpoint bins.
	 *
	 * Bresenham's Midpoint circle algorithm is used for filling in the intermediate pixels in an arc.
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
	protected val endpointsToArcBins: (BinIndex, BinIndex) => IndexedSeq[BinIndex] =
		(start, end) => {
			var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())

			//---- Find centre of circle to be used to draw the arc
			val dx = x1-x0
			val dy = y1-y0
			val len = Math.sqrt(dx*dx + dy*dy)	//length between endpoints
			val r = len.toInt	// set radius of circle = len for now  (TODO -- could make this a tunable parameter?)

			val halfPI = 0.5*Math.PI
			val theta1 = halfPI - Math.asin(len/(2.0*r)); // angle from each endpoint to circle's centre (centre and endpoints form an isosceles triangle)
			val angleTemp = Math.atan2(dy, dx)-theta1;
			val xC = (x0 + r*Math.cos(angleTemp)).toInt		   // co-ords for circle's centre
			val yC = (y0 + r*Math.sin(angleTemp)).toInt
			//val xC_2 = x0 + (r*Math.cos(Math.atan2(dy, dx)+theta1)).toInt	//Note: 2nd possiblility for circle's centre
			//val yC_2 = y0 + (r*Math.cos(Math.atan2(dy, dx)+theta1)).toInt	//(corresponds to CCW arc, so not needed in this case)

			//---- Use Midpoint Circle algorithm to draw the arc
			var f = 1-r
			var ddF_x = 1
			var ddF_y = -2*r
			var x = 0
			var y = r

			// angles for start and end points of arc
			var startRad = Math.atan2(y0-yC, x0-xC)
			var stopRad = Math.atan2(y1-yC, x1-xC)

			val bWrapNeg = (stopRad-startRad > Math.PI)
			if (bWrapNeg) startRad += 2.0*Math.PI	//note: this assumes using CW arcs only!
													//(otherwise would need to check if stopRad is negative here as well)

			val arcBins = scala.collection.mutable.ArrayBuffer[BinIndex]()

			// calc points for the four vertices of circle
			saveArcPoint((xC, yC+r), halfPI)
			saveArcPoint((xC, yC-r), -halfPI)
			saveArcPoint((xC+r, yC), 0)
			saveArcPoint((xC-r, yC), Math.PI)

			while (x < y-1) {
				if (f >= 0) {
					y = y - 1
					ddF_y = ddF_y + 2
					f = f + ddF_y
				}
				x = x + 1
				ddF_x = ddF_x + 2
				f = f + ddF_x

				val newAngle = Math.atan2(y, x)
				saveArcPoint((xC+x, yC+y), newAngle)
				saveArcPoint((xC-x, yC+y), Math.PI - newAngle)
				saveArcPoint((xC+x, yC-y), -newAngle)
				saveArcPoint((xC-x, yC-y), -Math.PI + newAngle)

				if (x!=y) {
					saveArcPoint((xC+y, yC+x), halfPI - newAngle)
					saveArcPoint((xC-y, yC+x), halfPI + newAngle)
						saveArcPoint((xC+y, yC-x), -halfPI + newAngle)
					saveArcPoint((xC-y, yC-x), -halfPI - newAngle)
				}
			}

			//------
			def saveArcPoint(point: (Int, Int), angle: Double) = {
				var newAngle = angle
				if (bWrapNeg && newAngle < 0.0)
					newAngle += 2.0*Math.PI

				if (newAngle <= startRad && newAngle >= stopRad)
					arcBins += new BinIndex(point._1, point._2)
			}

			arcBins.toIndexedSeq	// seq of arc bins
		}

	/**
	 * Determine all tiles required to draw a line between two endpoint bins.
	 *
	 * @param baseTile
	 *        A sample tile specifying level and number of bins of all required
	 *        results.
	 * @return All tiles falling on the direct line (in universal bin
	 *         coordinates) between the two endpoint bins
	 */
	protected def universalBinsToTiles(baseTile: TileIndex, bins: IndexedSeq[BinIndex],
	                                   uniBinToTB: (TileIndex, BinIndex) => TileAndBinIndices):
			Traversable[TileIndex] =
	{
		bins.map(ubin =>
			{
				val tb = uniBinToTB(baseTile, ubin)
				tb.getTile()
			}
		).toSet	// transform to set to remove duplicates			
	}	
	/**
	 * Determine all bins within a given tile that are required to draw a line
	 * between two endpoint bins.
	 *
	 * @param tile
	 *        The tile of interest
	 * @return All bins, in tile bin coordinates, in the given tile, falling on
	 *         the direct line (in universal bin coordinates) between the two
	 *         endoint bins.
	 */
	protected def universalBinsToBins(tile: TileIndex, bins: IndexedSeq[BinIndex],
	                                  uniBinToTB: (TileIndex, BinIndex) => TileAndBinIndices):
			IndexedSeq[BinIndex] =
	{
		bins.map(ubin =>
			uniBinToTB(tile, ubin)
		).filter(_.getTile().equals(tile)).map(_.getBin())
	}			
}



class RDDLineBinner(minBins: Int = 2,
                    maxBins: Int = 1024) {	// 1<<10 = 1024  256*4 = 4 tile widths
	var debug: Boolean = true


	def transformData[RT: ClassTag, IT: ClassTag, PT: ClassTag, DT: ClassTag]
	(data: RDD[RT],
	 indexFcn: RT => Try[IT],
	 valueFcn: RT => Try[PT],
	 dataAnalytics: Option[AnalysisDescription[RT, DT]] = None):
			RDD[(IT, PT, Option[DT])] =
	{
		// Process the data to remove all but the minimal portion we need for
		// tiling - x coordinate, y coordinate, and bin value
		data.mapPartitions(iter =>
			iter.map(i => (indexFcn(i), valueFcn(i), dataAnalytics.map(_.convert(i))))
		).filter(record => record._1.isSuccess && record._2.isSuccess)
			.map(record =>(record._1.get, record._2.get, record._3))
	}

	/**
	 * Fully process a dataset of input records into output tiles written out
	 * somewhere
	 * 
	 * @param RT The raw input record type
	 * @param IT The coordinate type
	 * @param PT The processing bin type
	 * @param BT The output bin type
	 */
	def binAndWriteData[RT: ClassTag, IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT] (
		data: RDD[RT],
		indexFcn: RT => Try[IT],
		valueFcn: RT => Try[PT],
		indexScheme: IndexScheme[IT],
		binAnalytic: BinningAnalytic[PT, BT],
		tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		dataAnalytics: Option[AnalysisDescription[RT, DT]],
		valueScheme: ValueDescription[BT],
		tileScheme: TilePyramid,
		consolidationPartitions: Option[Int],
		writeLocation: String,
		tileIO: TileIO,
		levelSets: Seq[Seq[Int]],
		bins: Int = 256,
		name: String = "unknown",
		description: String = "unknown") =
	{
		if (debug) {
			println("Binning data")
			println("\tConsolidation partitions: "+consolidationPartitions)
			println("\tWrite location: "+writeLocation)
			println("\tTile io type: "+tileIO.getClass.getName)
			println("\tlevel sets: "+levelSets.map(_.mkString("[", ", ", "]"))
				        .mkString("[", ", ", "]"))
			println("\tBins: "+bins)
			println("\tName: "+name)
			println("\tDescription: "+description)
		}

		val startTime = System.currentTimeMillis()

		val bareData = transformData(data, indexFcn, valueFcn, dataAnalytics)

		// Cache this, we'll use it at least once for each level set
		bareData.persist(StorageLevel.MEMORY_AND_DISK)

		levelSets.foreach(levels =>
			{
				val levelStartTime = System.currentTimeMillis()
				// For each level set, process the bare data into tiles...
				var tiles = processDataByLevel(bareData,
				                               indexScheme,
				                               binAnalytic,
				                               tileAnalytics,
				                               dataAnalytics,
				                               tileScheme,
				                               levels,
				                               bins,
				                               consolidationPartitions)
				// ... and write them out.
				tileIO.writeTileSet(tileScheme, writeLocation, tiles,
				                    valueScheme, tileAnalytics, dataAnalytics,
				                    name, description)
				if (debug) {
					val levelEndTime = System.currentTimeMillis()
					println("Finished binning levels ["+levels.mkString(", ")+"] of data set "
						        + name + " in " + ((levelEndTime-levelStartTime)/60000.0) + " minutes")
				}
			}
		)

		bareData.unpersist(false)

		if (debug) {
			val endTime = System.currentTimeMillis()
			println("Finished binning data set " + name + " into "
				        + levelSets.map(_.size).reduce(_+_)
				        + " levels (" + levelSets.map(_.mkString(",")).mkString(";") + ") in "
				        + ((endTime-startTime)/60000.0) + " minutes")
		}
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles on the given levels.
	 *
	 * @param data The data to be processed
	 * @param indexScheme A conversion scheme for converting from the index type 
	 *        to one we can use.
	 * @param binAnalytic A description of how raw values are aggregated into 
     *                    bin values
     * @param tileAnalytics A description of analytics that can be run on each 
     *                      tile, and how to aggregate them
     * @param dataAnalytics A description of analytics that can be run on the
     *                      raw data, and recorded (in the aggregate) on each
     *                      tile
	 * @param tileScheme A description of how raw values are transformed to bin
	 *                   coordinates
	 * @param levels A list of levels on which to create tiles
	 * @param bins The number of bins per coordinate on each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * 
	 * @param IT the index type, convertable to a cartesian pair with the 
     *           coordinateFromIndex function
	 * @param PT The bin type, when processing and aggregating
     * @param AT The type of tile-level analytic to calculate for each tile.
     * @param DT The type of raw data-level analytic that already has been 
     *           calculated for each tile.
     * @param BT The final bin type, ready for writing to tiles
	 */
	def processDataByLevel[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 indexScheme: IndexScheme[IT],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 tileScheme: TilePyramid,
		 levels: Seq[Int],
		 bins: Int = 256,
		 consolidationPartitions: Option[Int] = None,
		 isDensityStrip: Boolean = false,
	                                          usePointBinner: Boolean = true,
	                                          linesAsArcs: Boolean = false):
			RDD[TileData[BT]] =
	{
		val tileBinToUniBin = {
			if (bins == 256)
				(TileIndex.tileBinIndexToUniversalBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.tileBinIndexToUniversalBinIndex)_
		}

		val localMinBins = minBins
		val localMaxBins = maxBins

		val mapOverLevels: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)] =
			//BinIndex == universal bin indices in this case
			index => {
				val (x1, y1, x2, y2) = indexScheme.toCartesianEndpoints(index)
				levels.map(level =>
					{
						// find 'universal' bins for both endpoints
						val tile1 = tileScheme.rootToTile(x1, y1, level, bins)
						val tileBin1 = tileScheme.rootToBin(x1, y1, tile1)
						val unvBin1 = tileBinToUniBin(tile1, tileBin1)

						val tile2 = tileScheme.rootToTile(x2, y2, level, bins)
						val tileBin2 = tileScheme.rootToBin(x2, y2, tile2)
						val unvBin2 = tileBinToUniBin(tile2, tileBin2)

						// check if line length is within valid range 
						val points =
							(math.abs(unvBin1.getX()-unvBin2.getX()) max
								 math.abs(unvBin1.getY()-unvBin2.getY()))
						if (points < localMinBins || points > localMaxBins) {
							// line segment either too short or too long (so 
							// disregard)
							(null, null, null)
						} else {
							// return endpoints as pair of universal bins per 
							// level
							if (unvBin1.getX() < unvBin2.getX())
								// use convention of endpoint with min X value 
								// is listed first
								(unvBin1, unvBin2, tile1)
							else
								// note, we need one of the tile indices here 
								// to keep level info for this line segment
								(unvBin2, unvBin1, tile2)
						}
					}
				)
			}

		processData(data, binAnalytic, tileAnalytics, dataAnalytics,
		            mapOverLevels, bins, consolidationPartitions, isDensityStrip, usePointBinner, linesAsArcs)
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles.
	 * 
	 * @param data The data to be processed
	 * @param binAnalytic A description of how raw values are aggregated into 
     *                    bin values
     * @param tileAnalytics A description of analytics that can be run on each 
     *                      tile, and how to aggregate them
     * @param dataAnalytics A description of analytics that can be run on the
     *                      raw data, and recorded (in the aggregate) on each
     *                      tile
	 * @param datumToTiles A function that spreads a data point out over the
	 *                     tiles and bins of interest
	 * @param levels A list of levels on which to create tiles
	 * @param bins The number of bins per coordinate on each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * 
	 * @param IT The index type, convertable to tile and bin
	 * @param PT The bin type, when processing and aggregating
     * @param AT The type of tile-level analytic to calculate for each tile.
     * @param DT The type of raw data-level analytic that already has been 
     *           calculated for each tile.
     * @param BT The final bin type, ready for writing to tiles
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 indexToUniversalBins: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)],
		 bins: Int = 256,
		 consolidationPartitions: Option[Int] = None,
		 isDensityStrip: Boolean = false,
	                                   usePointBinner: Boolean = true,
	                                   linesAsArcs: Boolean = false): RDD[TileData[BT]] =
	{
		val metaData = processMetaData(data, indexToUniversalBins, dataAnalytics)

		// We first bin data in each partition into its associated bins
		val partitionBins = data.mapPartitions(iter =>
			{
				val partitionResults: MutableMap[(BinIndex, BinIndex, TileIndex), PT] =
					MutableMap[(BinIndex, BinIndex, TileIndex), PT]()

				// Map each data point in this partition into its bins
				iter.flatMap(record =>
					indexToUniversalBins(record._1)
						.filter(_._1 != null)
						.map(tbi => (tbi, record._2))
				).foreach(tbv =>
					// And combine bins within this partition
					{
						val key = tbv._1
						val value = tbv._2
						if (partitionResults.contains(key)) {
							partitionResults(key) = binAnalytic.aggregate(partitionResults(key), value)
						} else {
							partitionResults(key) = value
						}
					}
				)

				partitionResults.iterator
			}
		)

		// Now, combine by-partition bins into global bins, and turn them into tiles.
		if (usePointBinner) {
			consolidateByPoints(partitionBins, binAnalytic, tileAnalytics,
			                    metaData, consolidationPartitions, isDensityStrip, bins, linesAsArcs)
		} else {
			consolidateByTiles(partitionBins, binAnalytic, tileAnalytics,
			                    metaData, consolidationPartitions, isDensityStrip, bins, linesAsArcs)
		}
	}



	/**
     * Process a simplified input dataset to run any raw data-based analysis
     * 
     * @param IT The index type of the data set
     * @param DT The type of data analytic used
	 * @param data The data to process
     * @param indexToUniversalBins A function that spreads a data point out over
	 *                             area of interest
	 * @param dataAnalytic An optional transformation from a raw data record
	 *                     into an aggregable analytic value.  If None, this
	 *                     should cause no extra processing. If multiple
	 *                     analytics are desired, use ComposedTileAnalytic
     */
	def processMetaData[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[(IT, PT, Option[DT])],
		 indexToUniversalBins: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)],
		 dataAnalytics: Option[AnalysisDescription[_, DT]]):
			Option[RDD[(TileIndex, Map[String, String])]] =
	{
		dataAnalytics.map(da =>
			data.mapPartitions(iter =>
				{
					val partitionResults = MutableMap[TileIndex, DT]()
					iter.foreach(record =>
						{
							indexToUniversalBins(record._1).foreach(indices =>
								{
									val tile = indices._3
									val value = record._3
									value.foreach(v =>
										{
											partitionResults(tile) =
												if (partitionResults.contains(tile)) {
													da.analytic.aggregate(partitionResults(tile), v)
												} else {
													v
												}
											da.accumulate(tile, v)
										}
									)
								}
							)
						}
					)

					partitionResults.map{case (tile, value) =>
						(tile, da.analytic.toMap(value))
					}.iterator
				}
			)
		)
	}



	private def consolidateByPoints[PT: ClassTag, AT: ClassTag, BT]
		(data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 tileMetaData: Option[RDD[(TileIndex, Map[String, String])]],
		 consolidationPartitions: Option[Int],
		 isDensityStrip: Boolean,
	                                           bins: Int = 256,
	                                           linesAsArcs: Boolean = false): RDD[TileData[BT]] =
	{
		
	    val uniBinToTileBin = {
			if (bins == 256) 
				(TileIndex.universalBinIndexToTileBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.universalBinIndexToTileBinIndex)_
		}
	    val calcLinePixels = {
	    	if (linesAsArcs)
	    		RDDLineBinner.endpointsToArcBins
	    	else
	    		RDDLineBinner.endpointsToLineBins
	    }	    
		
		val densityStripLocal = isDensityStrip
		
		// Do reduceByKey to account for duplicate lines at a given level 
		// (not really necessary, but might speed things up?)
		// val reduced1 = data.reduceByKey(binAnalytic.aggregate(_, _),
		//                                 getNumSplits(consolidationPartitions, data))

		// We need to consolidate both metadata and binning data, so our result
		// has two slots, and each half populates one of them.
		//
		// We need to do this right away because the tiles should be immutable
		// once created
		//
		// For the same reason, we'll have to run the tile analytic when we 
		// create the tile, too.
		//
		// First, the binning data half.

		//     Draw lines (based on endpoint bins), and convert all results from 
		//     universal bins to tile,bin coords
		val expanded = data.flatMap(p =>
			{
				calcLinePixels(p._1._1, p._1._2).map(bin =>
					{
						val tb = uniBinToTileBin(p._1._3, bin)
						((tb.getTile(), tb.getBin()), p._2)
					}
				)
			}
		)

		//     Rest of process is same as regular RDDBinner (reduceByKey, convert 
		//     to (tile,(bin,value)), groupByKey, and create tiled results)
		val reduced: RDD[(TileIndex, (Option[(BinIndex, PT)],
		                              Option[Map[String, String]]))] =
			expanded.reduceByKey(binAnalytic.aggregate(_, _),
			                    RDDLineBinner.getNumSplits(consolidationPartitions, expanded)
			).map(p => (p._1._1, (Some((p._1._2, p._2)), None)))

		// Now the metadata half (in a way that should take no work if there is no metadata)
		val metaData: Option[RDD[(TileIndex, (Option[(BinIndex, PT)],
		                                      Option[Map[String, String]]))]] =
			tileMetaData.map(
				_.map{case (index, metaData) => (index, (None, Some(metaData))) }
			)

		// Get the combination of the two sets, again in a way that does
		// no extra work if there is no metadata
		val toTile = 
			if (metaData.isDefined) reduced union metaData.get
		    else reduced



		toTile
			.groupByKey(RDDLineBinner.getNumSplits(consolidationPartitions, toTile))
			.map(t =>
			{
				val index = t._1
				val tileData = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()

				// Create our tile
				val tile = if (densityStripLocal) new DensityStripData[BT](index)
				else new TileData[BT](index)

				// Put the proper default in all bins
				val defaultBinValue =
					binAnalytic.finish(binAnalytic.defaultProcessedValue)
				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						tile.setBin(x, y, defaultBinValue)
					}
				}

				// Put the proper value in each bin
				tileData.filter(_._1.isDefined).foreach(p =>
					{
						val bin = p._1.get._1
						val value = p._1.get._2
						tile.setBin(bin.getX(), bin.getY(), binAnalytic.finish(value))
					}
				)

				// Add in any pre-calculated metadata
				tileData.filter(_._2.isDefined).foreach(p =>
					{
						val metaData = p._2.get
						metaData.map{
						  case (key, value) => tile.setMetaData(key, value)
						}
					}
				)

				// Calculate and add in any tile-level metadata we've been told to calcualte
				tileAnalytics.map(ta =>
					{
						// Figure out the value for this tile
						val analyticValue = ta.convert(tile)
						// Add it into any appropriate accumulators
						ta.accumulate(index, analyticValue)
						// And store it in the tile's metadata
						ta.analytic.toMap(analyticValue).map{case (key, value) =>
							tile.setMetaData(key, value)
						}
					}
				)

				tile
			}
		)
	}

	

	private def consolidateByTiles[PT: ClassTag, AT: ClassTag, BT]
		(data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 tileMetaData: Option[RDD[(TileIndex, Map[String, String])]],
		 consolidationPartitions: Option[Int],
		 isDensityStrip: Boolean,
	                                           bins: Int = 256,
	                                           linesAsArcs: Boolean = false):
			RDD[TileData[BT]] = {
		
	    val uniBinToTileBin = {
			if (bins == 256) 
				(TileIndex.universalBinIndexToTileBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.universalBinIndexToTileBinIndex)_
		}	    
	    val calcLinePixels = {
	    	if (linesAsArcs)
	    		RDDLineBinner.endpointsToArcBins
	    	else
	    		RDDLineBinner.endpointsToLineBins
	    }
		
		val densityStripLocal = isDensityStrip
		
		// Do reduceByKey to account for duplicate lines at a given level (not 
		// really necessary, but might speed things up?)
		// val reduced1 = data.reduceByKey(binDesc.aggregateBins(_, _),
		//                               getNumSplits(consolidationPartitions, data))

		// We need to consolidate (join) both metadata and segment data, so our 
		// result has two slots, and each half populates one of them.
		//
		// We need to do this before we actually create tiles, because the 
		// tiles should be immutable, once created.
		//
		// For the same reason, we'll have to run the tile analytic when we 
		// create the tile, too.
		//
		// First, the segment data half

		// Draw lines (based on endpoint bins), and convert all results from 
		// universal bins to tile,bin coords
		// Need flatMap here, else result is an RDD IndexedSeq
		// val reduced2 = reduced1.flatMap(p => {
		
		val segmentsByTile: RDD[(TileIndex, (Option[(BinIndex, BinIndex, PT)],
		                                     Option[Map[String, String]]))] =
			data.flatMap(p =>
				{
					RDDLineBinner.universalBinsToTiles(p._1._3,
					                                   calcLinePixels(p._1._1, p._1._2),
					                                   uniBinToTileBin).map(tile =>
						(tile, (Some((p._1._1, p._1._2, p._2)), None))
					)
				}
			)


		// Now, the metadata half (in a way that should take no work if there 
		// is no metadata)
		val metaData: Option[RDD[(TileIndex, (Option[(BinIndex, BinIndex, PT)],
		                                      Option[Map[String, String]]))]] =
			tileMetaData.map(_.map{case (index, metaData) => (index, (None, Some(metaData)))})

		// Get the combination of the two sets, again in a way that does no 
		// extra work if there is no metadata
		//
		// Note that we don't do a simple join because we want all entries from 
		// the segmentsByTile dataset, whether or not they have a corresponding 
		// entry in the metadata dataset
		val toTile =
			if (metaData.isDefined) segmentsByTile union metaData.get
			else segmentsByTile

		// Consolidate segments for each tile index, and any associated metadata,
		// and draw a tile data based on the consolidated results
		val partitions = RDDLineBinner.getNumSplits(consolidationPartitions, segmentsByTile)
		toTile
			.groupByKey(partitions)
			.map(t =>
			{
				val index = t._1
				val tileData = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()

				// Create and default our tile as an array, first, for efficiency.
				//init 2D array of of type PT
				val binValues = Array.ofDim[PT](xLimit, yLimit)
				val defaultRawValue = binAnalytic.defaultUnprocessedValue
				val defaultCookedValue = binAnalytic.defaultProcessedValue
				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						binValues(x)(y) = defaultRawValue
					}
				}

				// Determine proper bin values
				tileData.filter(_._1.isDefined).foreach(p =>
					{
						val segment = p._1.get
						// get all universal bins in line, discard ones not in current tile, 
						// and convert bins to 'regular' tile/bin units
						RDDLineBinner.universalBinsToBins(index,
						                                  calcLinePixels(segment._1,
						                                                 segment._2),
						                                  uniBinToTileBin).foreach(bin =>
							{
								val x = bin.getX()
								val y = bin.getY()
								binValues(x)(y) =  binAnalytic.aggregate(binValues(x)(y), segment._3)
							}
						)
					}
				)
				
				// convert aggregated bin values from type PT to BT, and save tile results
				// Create our tile
				val tile = if (densityStripLocal) new DensityStripData[BT](index)
				else new TileData[BT](index)

				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						val value =
							if (binValues(x)(y) == defaultRawValue) defaultCookedValue
							else binValues(x)(y)
						tile.setBin(x, y, binAnalytic.finish(value))
					}
				}

				// Add in any pre-calculated metadata
				tileData.filter(_._2.isDefined).foreach(p =>
					{
						val metaData = p._2.get
						metaData.map{
							case (key, value) => tile.setMetaData(key, value)
						}
					}
				)
				// Calculate and add in any tile-level metadata we've been told
				// to calculate
				tileAnalytics.map(ta =>
					{
						// Figure out the value for this tile
						val analyticValue = ta.convert(tile)
						// Add it into any appropriate accumulators
						ta.accumulate(index, analyticValue)
						// And store it in the tile's metadata
						ta.analytic.toMap(analyticValue).map{case (key, value) =>
							tile.setMetaData(key, value)
						}
					}
				)
			tile
			}
		)
	}
}
