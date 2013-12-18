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



package com.oculusinfo.tilegen.binning



import java.io.InputStream
import java.io.IOException
import java.lang.{Iterable => JavaIterable}
import java.util.{List => JavaList}

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}

import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.TileSerializer

import com.oculusinfo.tilegen.util.Rectangle
import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.tiling.TileMetaData




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class LiveStaticTilePyramidIO (master: String,
                               sparkHome: String,
                               user: Option[String]) extends PyramidIO {
  // We need to generate metadata for each 
  private val sc = new GeneralSparkConnector(master, sparkHome, user).getSparkContext("Live Static Tile Generation")
  private val metaDatas = MutableMap[String, TileMetaData]()
  private val pyramids = MutableMap[String, TilePyramid]()
  private val datas = MutableMap[String, RDD[(Double, Double, Double)]]()


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

  /**
   * Generate metadata for the given levels of the given dataset.
   * 
   * If we already have a metadata structure, just add those levels to it.
   */
  def initializeForRead (pyramidId: String,
                         tilePyramid: TilePyramid,
                         tileSize: Int,
                         minLevel: Int,
                         maxLevel: Int): Unit = {
    if (!datas.contains(pyramidId)) {
      datas(pyramidId) = sc.textFile(pyramidId).map(line => {
        val fields = line.split(',')

        (fields(0).toDouble, fields(1).toDouble, fields(2).toDouble)
      })
      datas(pyramidId).cache()
    }
    if (!metaDatas.contains(pyramidId)) {
      val fullBounds = tilePyramid.getTileBounds(new TileIndex(0, 0, 0, tileSize, tileSize))
      metaDatas(pyramidId) = new TileMetaData(pyramidId,
                                             "Live static tile level",
                                             tileSize,
                                             tilePyramid.getTileScheme(),
                                             tilePyramid.getProjection(),
                                             0,
                                             scala.Int.MaxValue,
                                             fullBounds,
                                             MutableList[(Int, String)](),
                                             MutableList[(Int, String)]())
    }
    pyramids(pyramidId) = tilePyramid
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

    mutableRows.foldRight(None: Option[Bounds])((bounds, rest) =>
      Some(new Bounds(bounds.level, bounds.indexBounds, rest))
    ).map(_.reduce).getOrElse(None).get
  }

  def readTiles[T] (pyramidId: String,
                    serializer: TileSerializer[T],
                    javaTiles: JavaIterable[TileIndex]): JavaList[TileData[T]] = {
    val tiles = javaTiles.asScala
    if (!datas.contains(pyramidId) ||
        !metaDatas.contains(pyramidId) ||
        !pyramids.contains(pyramidId) ||
        !tiles.isEmpty) {
      null
    } else {
      val metaData = metaDatas(pyramidId)
      val pyramid = pyramids(pyramidId)
      val data = datas(pyramidId)

      val bounds = tilesToBounds(pyramid, tiles)

      val boundsTest = bounds.getSerializableContainmentTest(pyramid)

      // TODO:
      //   * Modify RDDBinner to allow direct calling with already-set-up RDD
      //     DONE (actually, already existed, but was private)
      //   * Add a method in RDDBinner that allows the caller to specify individual 
      //     tiles, rather than just levels
      //     DONE
      //     * Keep the overall bounds test as a quick first pass
      //   * Add to Bounds a way of communicating to RDDBinner the tiles and bounds needed
      //   * Call RDDBinner with the data set, filtered by overall bounds, and with the 
      //     info needed to transform points to specific tiles

      null
    }
  }

  def getTileStream (pyramidId: String, tile: TileIndex): InputStream = {
    null
  }

  def readMetaData (pyramidId: String): String =
    metaDatas.get(pyramidId).map(_.toString).getOrElse(null)

}
