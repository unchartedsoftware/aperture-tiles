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
import java.util.{List => JavaList}
import java.awt.geom.Point2D


import scala.collection.JavaConversions._
import scala.util.{Try, Success, Failure}


import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD


import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex

import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.MissingArgumentException



class GenericSeriesBinner[T: ClassManifest] (source: DataSource,
                                             parser: RecordParser[T],
                                             extractor: FieldExtractor[T]) extends Serializable {
	// Extract just the coordinates
	private def extractCoordinates (data: RDD[T],
	                                coordFcns: Seq[T => Try[Double]]):
			RDD[Seq[Double]] = {
		data.map(record =>
			// Extract our variables
			coordFcns.map(f => f(record))
		).filter(coords =>
			// Filter out missing values
			coords.map(_.isSuccess).fold(true)(_ && _)
		).map(coords =>
			// Get the actual values, removing the exception-passing structure
			coords.map(_.get)
		)
	}

	// Figure out coordinate bounds.
	// This presumes extractCoordinates has already been run and  cached;
	// coordFcns is passed in solely so that we will know how many coordinates
	// there are per point.  Therefore, this method relies on the caller having
	// created the data rdd by calling extractCoordinates using the same
	// coordFcns passed into this method
	private def getBounds[T <: Seq[Double]] (data: RDD[T],
	                                         numCoords: Int):
			Seq[(Double, Double)] = {
		// Create min and max accumulators for each coordinate
		val accums = Range(0, numCoords).map(fcn =>
			// Note: fcn is not used, the mapping over coordFcns is just to get the
			// same number of accumulators
			(data.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam),
			 data.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam))
		)

		// Figure out the bounds of our values, coordinate by coordinate
		data.foreach(point =>
			{
				point.zip(accums).map(coordAccumsPair =>
					{
						val coord = coordAccumsPair._1

						val minAccum = coordAccumsPair._2._1
						minAccum += coord

						val maxAccum = coordAccumsPair._2._2
						maxAccum += coord
					}
				)
			}
		)

		accums.map(minmax => (minmax._1.value, minmax._2.value))
	}

	def doBinning (sc: SparkContext,
	               tileIO: TileIO,
	               pyramidName: String,
	               xVar: String, yVar: String, zVar: String,
	               resultVar: String,
	               resultBins: Int,
	               levelSets: List[List[Int]],
	               consolidationPartitions: Option[Int],
	               name: String = "unknown",
	               description: String = "unknown"): Unit = {
		val startTime = System.currentTimeMillis()
		val localParser = parser
		val localExtractor = extractor
		val maxLevel = levelSets.map(_.reduce(_ max _)).reduce(_ max _)
		val variables = List(xVar, yVar, zVar, resultVar)


		println("Getting raw data")
		val rawData = source.getData(sc)
		println("Data retrieved")

		val data = rawData.mapPartitions(iter =>
			// Parse the records from the raw data
			localParser.parseRecords(iter, xVar, yVar, zVar, resultVar)
		).filter(_.isSuccess).map(_.get).map(record =>
			// Pull out the relevant variables
			variables.map(variable => localExtractor.getFieldValue(variable)(record))
		).filter(_.map(_.isSuccess).fold(true)(_ && _)).map(_.map(_.get))
		data.cache
		println("Data parsed and cached")

		// Figure out the bounds of our data
		val bounds = getBounds(data, 3)
		println("Bounds retrieved")
		// include a fraction of a bin extra in the bounds, so the max goes on the
		// right side of the last tile, rather than forming an extra tile.
		val epsilon = (1.0/(1 << maxLevel)) / (256.0*256.0)
		val minX = bounds(0)._1
		val maxX = bounds(0)._2 + (bounds(0)._2 - bounds(0)._1) * epsilon
		val minY = bounds(1)._1
		val maxY = bounds(1)._2 + (bounds(1)._2 - bounds(1)._1) * epsilon
		val minZ = bounds(2)._1
		val maxZ = bounds(2)._2 + (bounds(2)._2 - bounds(2)._1) * epsilon

		val xbins = 256
		val ybins = 256

		val pyramider = localExtractor.getTilePyramid(xVar, minX, maxX,
		                                              yVar, minY, maxY)

		// Start binning up data
		levelSets.map(levelSet =>
			{
				// Get data for this level
				val bins: RDD[((TileIndex, BinIndex), Seq[Double])] = data.flatMap(record =>
					{
						val x = record(0)
						val y = record(1)
						val z = record(2)
						val value = record(3)
						val pt = new Point2D.Double(x, y)

						levelSet.map(level =>
							{
								val rawTile = pyramider.rootToTile(pt, level)
								val tile = new TileIndex(rawTile.getLevel(), rawTile.getX(), rawTile.getY(), xbins, ybins)
								val bin = pyramider.rootToBin(pt, tile)

								// Figure out in which time bin this point lies
								val timeBin = math.floor((z - minZ) / (maxZ - minZ) * resultBins).toInt
								val valueList = ((Range(0, timeBin).map(n => 0.0)
									                  :+ value)
									                 ++ Range(0, resultBins - timeBin - 1).map(n => 0.0))

								((tile, bin), valueList.toSeq)
							}
						)
					}
				)

				// Consolidate bins
				// This is all copied from GeneralBinner, and should be consolidated into that class
				val binDesc = new StandardDoubleArrayBinDescriptor
				val reduced = bins.reduceByKey(binDesc.aggregateBins(_, _),
				                               getNumSplits(consolidationPartitions, bins)).map(record =>
					(record._1._1, (record._1._2, record._2))
				)
				reduced.cache
				reduced.count
				println("\n\n\nReduction complete\n\n\n")

				// Produce tiles
				val tiles = reduced.groupByKey(getNumSplits(consolidationPartitions, reduced)).map(t =>
					{
						val index = t._1
						val bins = t._2
						val xLimit = index.getXBins()
						val yLimit = index.getYBins()
						val tile = new TileData[JavaList[JavaDouble]](index)
						val defaultBinValue = binDesc.convert(binDesc.defaultProcessedBinValue)

						for (x <- 0 until xLimit) {
							for (y <- 0 until yLimit) {
								tile.setBin(x, y, defaultBinValue)
							}
						}

						bins.foreach(p =>
							{
								val bin = p._1
								val value = p._2
								tile.setBin(bin.getX(), bin.getY(), binDesc.convert(value))
							}
						)

						tile
					}
				)

				// write tiles
				tileIO.writeTileSet(pyramider, pyramidName, tiles, binDesc, name, description)
			}
		)
	}

	def getNumSplits[T: ClassManifest] (requestedPartitions: Option[Int], dataSet: RDD[T]): Int = {
		val curSize = dataSet.partitions.size
		val result = curSize max requestedPartitions.getOrElse(0)
		result
	}
}
