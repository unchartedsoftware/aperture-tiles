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



import scala.reflect.ClassTag
import org.apache.spark.rdd.RDD
import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileData.StorageType
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic


object UniversalBinner {
	/**
	 * Get the number of partitions to use when operating on a data set.
	 *
	 * @param dataSet the dataSet for which to determine the number of
	 *                partitions.
	 * @param minPartitions The minimum number of partitions into which to split the data.  If none, 
	 *                      the number of partitions will not be increased.
	 * @param maxPartitions The maximum number of partitions into which to split the data.  If none, 
	 *                      the number of partitions will not be decreased.
	 * @return The number of partitions that should be used for this dataset.
	 */
	def getNumSplits[T: ClassTag] (dataSet: RDD[T],
	                               minPartitions: Option[Int],
	                               maxPartitions: Option[Int]): Int =
		dataSet.partitions.size
			.max(minPartitions.getOrElse(0))
			.min(maxPartitions.getOrElse(Int.MaxValue))

	/**
     * Optionally aggregate two optional values
     * 
     * @param aggFcn An aggregation function for combining two Ts
     * @param value1 The first value to aggregate
     * @param value2 The second value to aggregate
     * @tparam T The type of value to aggregate
     */
	def optAggregate[T] (aggFcn: Option[(T, T) => T],
	                     value1: Option[T], value2: Option[T]): Option[T] =
		aggFcn.map(fcn => (value1 ++ value2).reduceLeftOption(fcn)).getOrElse(None)
}



class UniversalBinner {
	import UniveralBinner._



	/**
	 * @param data The data to tile
	 * @param binAnalytic The aggregation function to use to tile the data
	 * @param tileAnalytics Optional analytics to apply to each produced tile
	 * @param dataAnalytics Optional analytics to apply to each raw data point, and bin along with 
	 *                      the tiles.
	 * @param locateIndexFcn: A function that takes in input index, and indicates which tile(s) it 
	 *                        is on.  The array of bin indices indicates precisely where on the 
	 *                        tiles is indicated, in universal bin coordinates.
	 * @param populateTileFcn A function that takes the precise specification of where an input 
	 *                        record specified a tile, and indicates which bins on that tile are 
	 *                        needed.  The input bins are in universal bin coordinates, as in 
	 *                        locateIndexFcn, while the output bins are bins for the specific tile
	 *                        in question
	 * @param parameters General binning parameters affecting how this tiling will be done.
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 locateIndexFcn: IT => Traversable[(TileIndex, Array[BinIndex])],
		 populateTileFcn: (TileIndex, Array[BinIndex]) => Array[BinIndex],
		 parameters: BinningParameters = new BinningParameters()): RDD[TileData[BT]] =
	{
		// If asked to run in debug mode, keep some stats on how much aggregation is going on in
		// this stage.
		val aggregationTracker = if (parameters.debug) Some(data.context.accumulator(0)) else None

		// First, within each partition, group data by tile
		val consolidatedByPartition = data.mapPartition{iter =>
			val partitionResults = MutableMap[(TileIndex, Array[BinIndex]), (PT, Option[DT])]()

			// Map each input record in this partition into tile coordinates, ...
			iter.flatMap(record => locateIndexFcn(record._1).map(index => (index, (record._2, record._3))))
			// ... and consolidate identical input records in this partition.
				.foreach{case (key, newValue) =>
					if (partitionResults.contains(key)) {
						val oldValue = partitionResults(key)
						partitionResults(key) = (binAnalytic.aggregate(newValue._1, oldValue._1),
						                         optAggregate(dataAnalytics.map(_.analytic.aggregate), newValue._2, oldValue._2))
					} else {
						partitionResults(key) = (vb, vd)
					}

		}
	}
}

case class BinningParameters (debug: Boolean = true,
                              xBins: Int = 256, yBins: Int = 256,
                              minPartitions: Option[Int] = None,
                              maxPartitions: Option[Int] = None,
                              tileType: Option[StorageType] = None)
