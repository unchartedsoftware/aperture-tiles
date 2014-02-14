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




import java.lang.{Iterable => JavaIterable}
import java.lang.UnsupportedOperationException
import java.io.InputStream
import java.util.{List => JavaList}
import java.util.Properties

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer



/**
 * A simple PyramidIO implementation for in-memory tests
 *
 * This class is a complete fake for testing purposes only - it relies on the fact
 * that tests take place on a local spark, and has only one worker, so that the
 * singleton object is common across all (1) instances, and won't have any
 * synchronization issues
 */
object TestPyramidIO {
  private val metaDatas = MutableMap[String, String]()
  private val datas = MutableMap[String, MutableMap[TileIndex, TileData[_]]]()
}
class TestPyramidIO extends PyramidIO with Serializable {
  import TestPyramidIO._

  def initializeForWrite (pyramidId: String): Unit = {
  }

  def writeTiles[T] (pyramidId: String, 
		     tilePyramid: TilePyramid,
		     serializer: TileSerializer[T],
		     data: JavaIterable[TileData[T]]): Unit = {
    if (!datas.contains(pyramidId)) {
      datas(pyramidId) = MutableMap[TileIndex, TileData[_]]()
    }

    data.asScala.foreach(datum => {
      datas(pyramidId)(datum.getDefinition) = datum
    })
  }

  def writeMetaData (pyramidId: String,
		     metaData: String): Unit = {
    metaDatas(pyramidId) = metaData
  }

  def initializeForRead (pyramidId: String,
			 tileSize: Int,
			 dataDescription: Properties): Unit = {}

  def readTiles[T] (pyramidId: String,
		    serializer: TileSerializer[T], 
		    tiles: JavaIterable[TileIndex]): JavaList[TileData[T]] = {
    tiles.asScala.map(index => {
      datas(pyramidId).get(index).getOrElse(null)
    }).toList.asInstanceOf[List[TileData[T]]].asJava
  }

  def getTileStream (pyramidId: String, tile: TileIndex): InputStream = {
    throw new UnsupportedOperationException("Can't get a stream from a TestPyramidIO")
  }

  def readMetaData (pyramidId: String): String = {
    metaDatas.get(pyramidId).getOrElse(null)
  }
}


/**
 * A simple TileIO implementation for in-memory tests
 */
class TestTileIO extends TileIO {
  val pyramidIO = new TestPyramidIO
  def getPyramidIO: PyramidIO =
    pyramidIO

  def getTile (pyramidId: String, tile: TileIndex): Option[TileData[_]] = {
    val tiles = pyramidIO.readTiles(pyramidId, null, List(tile).asJava)
    if (null != tiles && !tiles.isEmpty() && null != tiles.get(0)) {
      Some(tiles.get(0))
    } else {
      None
    }
  }
}
