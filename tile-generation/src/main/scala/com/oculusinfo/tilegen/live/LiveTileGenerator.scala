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

package com.oculusinfo.tilegen.live


import scala.reflect.ClassTag

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD


import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.DenseTileData

import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic



class LiveTileGenerator[PT: ClassTag,
                        BT: ClassTag] (data: RDD[(Double, Double, PT)],
                                       pyramidScheme: TilePyramid,
                                       binAnalytic: BinningAnalytic[PT, BT],
                                       numXBins: Int = 256,
                                       numYBins: Int = 256) {
	def getTile (tileLevel: Int, tileX: Int, tileY: Int): TileData[BT] = {
		// Localize some of our fields to avoid the need for serialization
		val localPyramidScheme = pyramidScheme
		val localBinAnalytic = binAnalytic
		val targetTile = new TileIndex(tileLevel, tileX, tileY, numXBins, numYBins)

		val bins = data.filter(record =>
			{
				val tile = localPyramidScheme.rootToTile(record._1, record._2, tileLevel)
				tileX == tile.getX() && tileY == tile.getY()
			}
		).map(record =>
			{
				val bin = localPyramidScheme.rootToBin(record._1, record._2, targetTile)
				(bin, record._3)
			}
		).reduceByKey(localBinAnalytic.aggregate(_, _)).collect()

		val tile = new DenseTileData[BT](targetTile)
		val defaultBinValue = localBinAnalytic.finish(localBinAnalytic.defaultProcessedValue)
		for (x <- 0 until numXBins) {
			for (y <- 0 until numYBins) {
				tile.setBin(x, y, defaultBinValue)
			}
		}
		bins.foreach(p =>
			{
				val bin = p._1
				val value = p._2
				tile.setBin(bin.getX(), bin.getY(), localBinAnalytic.finish(value))
			}
		)
		tile
	}
}
