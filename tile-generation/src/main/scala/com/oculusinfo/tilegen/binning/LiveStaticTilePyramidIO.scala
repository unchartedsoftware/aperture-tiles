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



import java.lang.Iterable
import java.util.{List => JavaList}
import java.io.InputStream
import java.io.IOException

import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}

import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.TileSerializer

import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.tiling.TileMetaData



class LiveStaticTilePyramidIO (master: String,
                               sparkHome: String,
                               user: Option[String]) extends PyramidIO {
  // We need to generate metadata for each 
  private val sc = new GeneralSparkConnector(master, sparkHome, user).getSparkContext("Live Static Tile Generation")
  private val metaData = MutableMap[String, TileMetaData]()
  private val data = MutableMap[String, RDD[(Double, Double, Double)]]()


  def initializeForWrite (pyramidId: String): Unit = {
  }

  def writeTiles[T] (pyramidId: String,
                     tilePyramid: TilePyramid,
                     serializer: TileSerializer[T],
                     data: Iterable[TileData[T]]): Unit =
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
    if (!data.contains(pyramidId)) {
      data(pyramidId) = sc.textFile(pyramidId).map(line => {
        val fields = line.split(',')

        (fields(0).toDouble, fields(1).toDouble, fields(2).toDouble)
      })
      data(pyramidId).cache()
    }
    if (!metaData.contains(pyramidId)) {
      metaData(pyramidId) = new TileMetaData(pyramidId,
                                             "Live static tile level",
                                             tileSize,
                                             tilePyramid.getTileScheme(),
                                             tilePyramid.getProjection(),
                                             0,
                                             Int.MaxValue,
                                             tilePyramid.getTileBounds(new TileIndex(0, 0, 0, tileSize, tileSize)),
                                             MutableList[(Int, String)](),
                                             MutableList[(Int, String)]())
    }
  }

  def readTiles[T] (pyramidId: String,
                    serializer: TileSerializer[T],
                    tiles: Iterable[TileIndex]): JavaList[TileData[T]] = {
    data.get(pyramidId).map(datum => {
      // Do processing here
      null
    }).getOrElse(null)
  }

  def getTileStream (pyramidId: String, tile: TileIndex): InputStream = {
    null
  }

  def readMetaData (pyramidId: String): String =
    metaData.get(pyramidId).map(_.toString).getOrElse(null)

}
