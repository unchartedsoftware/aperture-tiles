/**
 * Copyright (c) 2013 Oculus Info Inc.
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



import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD



import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D



import scala.collection.mutable.{Map => MutableMap}



import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.BinIterator
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.DensityStripData
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.TileSerializer



/**
 * This class is the bassis of all (or, at least, nearly all) of the
 * other binning classes.  This takes an RDD of data and transforms it
 * into a pyramid of tiles.
 *
 * @param tileScheme the type of tile pyramid this binner can bin.
 */
class RDDBinner {
  var debug: Boolean = false
  var densityStrip: Boolean = false


  /**
   * Fully process a dataset of input records into output tiles
   * 
   * @param IT The input record type
   * @param OT The output bin type
   */
  def binAndWriteData[IT: ClassManifest, OT: ClassManifest, BT] (
    data: RDD[IT],
    ptFcn: Iterator[IT] => Iterator[(ValueOrException[Double],
                                     ValueOrException[Double],
                                     ValueOrException[OT])],
    binDesc: BinDescriptor[OT, BT],
    tileScheme: TilePyramid,
    consolidationPartitions: Option[Int],
    writeLocation: String,
    tileIO: TileIO,
    levelSets: List[List[Int]],
    bins: Int = 256,
    name: String = "unknown",
    description: String = "unknown") =
  {
    if (debug) {
      println("Binning data")
      println("\tConsolidation partitions: "+consolidationPartitions)
      println("\tWrite location: "+writeLocation)
      println("\tTile io type: "+tileIO.getClass.getName)
      println("\tlevel sets: "+levelSets.map(_.mkString("[", ", ", "]")).mkString("[", ", ", "]"))
      println("\tBins: "+bins)
      println("\tName: "+name)
      println("\tDescription: "+description)
    }

    val startTime = System.currentTimeMillis()

    // Process the data to remove all but the minimal portion we need for 
    // tiling - x coordinate, y coordinate, and bin value
    val bareData = data.mapPartitions(iter => {
      println("Initial partition processing")
      ptFcn(iter)
    }).filter(record => {
      record._1.hasValue && record._2.hasValue && record._3.hasValue
    }).map(record => {
      (record._1.get, record._2.get, record._3.get)
    })
    // Cache this, we'll use it at least once for each level set
    bareData.cache()

    levelSets.foreach(levels => {
      val levelStartTime = System.currentTimeMillis()
      // For each level set, process the bare data into tiles...
      var tiles = processData(bareData, binDesc, tileScheme,
                              consolidationPartitions, levels, bins)
      // ... and write them out.
      tileIO.writeTileSet(tileScheme, writeLocation, tiles, 
                          binDesc, name, description)
      if (debug) {
	val levelEndTime = System.currentTimeMillis()
	println("Finished binning levels ["+levels.mkString(", ")+"] of data set " 
		+ name + " in " + ((levelEndTime-levelStartTime)/60000.0) + " minutes")
      }
    })

    if (debug) {
      val endTime = System.currentTimeMillis()
      println("Finished binning data set " + name + " into " + levelSets.map(_.size).reduce(_+_)
              + " levels (" + levelSets.map(_.mkString(",")).mkString(";") + ") in "
              + ((endTime-startTime)/60000.0) + " minutes")
    }
  }



  private def processData[PT: ClassManifest, BT] (data: RDD[(Double, Double, PT)],
                                                  binDesc: BinDescriptor[PT, BT],
                                                  tileScheme: TilePyramid,
                                                  consolidationPartitions: Option[Int],
                                                  levels: List[Int],
                                                  bins: Int): RDD[TileData[BT]] = {
    // We first bin data in each partition into its associated bins
    val partitionBins = data.mapPartitions(iter => {
      val partitionResults: MutableMap[(TileIndex, BinIndex), PT] =
        MutableMap[(TileIndex, BinIndex), PT]()

      iter
      // Map each data point in this partition into its bin
        .flatMap(record =>
        levels.map(level => {
          val tile = tileScheme.rootToTile(record._1, record._2, level, bins)
          val bin = tileScheme.rootToBin(record._1, record._2, tile)
                                        ((tile, bin), record._3)
        })
      )
      // And combine bins within this partition
      .foreach(tbr => {
        val key = tbr._1
        val value = tbr._2
        if (partitionResults.contains(key)) {
          partitionResults(key) = binDesc.aggregateBins(partitionResults(key), value)
        } else {
          partitionResults(key) = value
        }
      })

      partitionResults.iterator
    })

    // Now, combine by-partition bins into global bins, and turn them into tiles.
    consolidate(partitionBins, binDesc, consolidationPartitions)
  }



  private def consolidate[PT: ClassManifest, BT] (data: RDD[((TileIndex, BinIndex), PT)],
                                                  binDesc: BinDescriptor[PT, BT],
                                                  consolidationPartitions: Option[Int]):
  RDD[TileData[BT]] = {
    val densityStripLocal = densityStrip
    val reduced = data.reduceByKey(binDesc.aggregateBins(_, _),
                                   getNumSplits(consolidationPartitions, data)).map(p =>
      (p._1._1, (p._1._2, p._2))
    )
    val result = reduced
      .groupByKey(getNumSplits(consolidationPartitions, reduced))
      .map(t => {
        val index = t._1
        val bins = t._2
        val xLimit = index.getXBins()
        val yLimit = index.getYBins()
        val tile = if (densityStripLocal) new DensityStripData[BT](index)
                   else new TileData[BT](index)

        for (x <- 0 until xLimit) {
          for (y <- 0 until yLimit) {
            tile.setBin(x, y, binDesc.defaultBinValue)
          }
        }

        bins.foreach(p => {
          val bin = p._1
          val value = p._2
          tile.setBin(bin.getX(), bin.getY(), binDesc.convert(value))
        })

        tile
      })

    result
  }


  def getNumSplits[T: ClassManifest] (requestedPartitions: Option[Int], dataSet: RDD[T]): Int = {
    val curSize = dataSet.partitions.size
    val result = curSize max requestedPartitions.getOrElse(0)
    result
  }
}
