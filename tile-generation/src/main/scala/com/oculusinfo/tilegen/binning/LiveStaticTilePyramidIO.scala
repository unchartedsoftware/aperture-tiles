/**
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



package com.oculusinfo.tilegen.binning



import java.io.InputStream
import java.io.IOException
import java.lang.{Iterable => JavaIterable}
import java.util.{List => JavaList}
import java.util.Properties

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer

import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor
import com.oculusinfo.tilegen.tiling.TileMetaData
import com.oculusinfo.tilegen.util.Rectangle




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class LiveStaticTilePyramidIO (sc: SparkContext) extends PyramidIO {
  private val datasets = MutableMap[String, Dataset[_, _]]()


  def initializeForWrite (pyramidId: String): Unit = {
  }

  def writeTiles[T] (pyramidId: String,
                     tilePyramid: TilePyramid,
                     serializer: TileSerializer[T],
                     data: JavaIterable[TileData[T]]): Unit =
    throw new IOException("Can't write raw data")

  def writeMetaData (pyramidId: String,
                     metaData: String): Unit =
    throw new IOException("Can't write raw data")

  def initializeForRead (pyramidId: String,
			 tileSize: Int,
			 dataDescription: Properties): Unit = {
    if (!datasets.contains(pyramidId)) {
      datasets(pyramidId) = DatasetFactory.createDataset(sc, dataDescription, true, tileSize)
    }
  }

  /*
   * Convert a set of tiles to testable bounds.
   *
   * The returned bounds are in two potential forms, paired into a 2-tuble.
   *
   * The first element is the bounds, in tile indices.
   */
  private def tilesToBounds (pyramid: TilePyramid,
                             tiles: Iterable[TileIndex]): Bounds = {
    var mutableRows = MutableList[Bounds]()
    val bounds = tiles.map(tile => 
      ((tile.getX, tile.getY()),
       new Bounds(tile.getLevel(),
                  new Rectangle[Int](tile.getX(), tile.getX(), tile.getY(), tile.getY()),
                  None))
    ).toSeq.sortBy(_._1).map(_._2).foreach(bounds => {
      if (mutableRows.isEmpty) {
        mutableRows += bounds
      } else {
        val last = mutableRows.last
        val combination = last union bounds
        if (combination.isEmpty) {
          mutableRows += bounds
        } else {
          mutableRows(mutableRows.size-1) = combination.get
        }
      }
    })

    val rows = mutableRows.foldRight(None: Option[Bounds])((bounds, rest) =>
      Some(new Bounds(bounds.level, bounds.indexBounds, rest))
    ).getOrElse(null)

    if (null == rows) {
      null
    } else {
      // reduce returns None if no reduction is required
      rows.reduce.getOrElse(rows)
    }
  }

  def readTiles[PT] (pyramidId: String,
                     serializer: TileSerializer[PT],
                     javaTiles: JavaIterable[TileIndex]): JavaList[TileData[PT]] = {
    def inner[BT: ClassManifest]: JavaList[TileData[PT]] = {
      val tiles: Iterable[TileIndex] = javaTiles.asScala

      if (!datasets.contains(pyramidId) || 
          tiles.isEmpty) {
	null
      } else {
	val dataset = datasets(pyramidId).asInstanceOf[Dataset[BT, PT]]

	val pyramid = dataset.getTilePyramid
	val bins = tiles.head.getXBins()
	val bounds = tilesToBounds(pyramid, tiles)

	val boundsTest = bounds.getSerializableContainmentTest(pyramid, bins)
	val spreaderFcn = bounds.getSpreaderFunction[BT](pyramid, bins);

	val binner = new RDDBinner
	binner.debug = true

	dataset.transformRDD[TileData[PT]](
	  rdd => {
	    binner.processData(rdd, dataset.getBinDescriptor, spreaderFcn, bins)
	  }
	).collect.toList.asJava
      }
    }

    inner
  }

  def getTileStream (pyramidId: String, tile: TileIndex): InputStream = {
    null
  }

  def readMetaData (pyramidId: String): String =
    datasets.get(pyramidId).map(_.createMetaData(pyramidId).toString).getOrElse(null)
}
