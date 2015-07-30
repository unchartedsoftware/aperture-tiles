/*
 * Copyright (c) 2015 Uncharted Software Inc.
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
package com.uncharted.tile.source.server.io

import java.awt.geom.Rectangle2D
import java.io.File
import java.lang.{Integer => JavaInt}
import java.util.{Arrays => JavaArrays}

import org.apache.avro.file.CodecFactory
import org.json.JSONObject

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

import org.scalatest.FunSuite

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.io.impl.{FileBasedPyramidIO, FileSystemPyramidSource}
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.io.serialization.{DefaultTileSerializerFactoryProvider, TileSerializer}
import com.oculusinfo.binning.io.{DefaultPyramidIOFactoryProvider, PyramidIO}
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.factory.providers.FactoryProvider
import com.oculusinfo.factory.util.Pair
import com.uncharted.tile.source.client.io.TileServerPyramidIO
import com.uncharted.tile.source.server.TileServer



/**
 * Test the TileServerPyramidIO to make sure it works.
 *
 * Because the tile creation uses Spark, this has to exist in the server project.
 */
class TileServerPyramidIOTestSuite extends FunSuite {
  import ExecutionContext.Implicits.global
  def getRootPath: String =
    new File(".").getCanonicalFile.getName match {
      case "tile-server" => "./src/test/resources/tilesets"
      case "aperture-tiles" => "./tile-server/src/test/resources/tilesets"
      case _ => throw new Exception("Create data run in invalid directory")
    }

  // Create a sample dataset with which to test the server.  This only ever needs to be run once, but is kept here
  // for reference
  ignore("Create data") {
    // figure out our base directory. Depending on how we are build, the current working directory should be either
    // aperture-tiles, or aperture-tiles/tile-server
    val root = getRootPath
    val writeIO = new FileBasedPyramidIO(new FileSystemPyramidSource(root, "avro"))

    val table = "read-test"
    writeIO.initializeForWrite(table)

    val metadata = new PyramidMetaData("test data", "test description", 4, 4, "TMS", "EPSG:4326",
      JavaArrays.asList(0, 1), new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0),
      JavaArrays.asList(new Pair(0, "0"), new Pair(1, "0")),
      JavaArrays.asList(new Pair(0, "40"), new Pair(1, "10"))
    )
    writeIO.writeMetaData(table, metadata.toString)

    val serializer = new PrimitiveAvroSerializer[JavaInt](classOf[JavaInt], CodecFactory.bzip2Codec())
    val tile000 = new DenseTileData[JavaInt](new TileIndex(0, 0, 0, 4, 4), 0,
      JavaArrays.asList(16, 23, 18, 21, 20, 16, 11, 27, 16, 23, 20, 14, 23, 30, 17, 20))
    val tile100 = new DenseTileData[JavaInt](new TileIndex(1, 0, 0, 4, 4), 0,
      JavaArrays.asList(5, 2, 6, 10, 0, 9, 6, 1, 3, 0, 8, 10, 10, 10, 10, 2))
    val tile110 = new DenseTileData[JavaInt](new TileIndex(1, 1, 0, 4, 4), 0,
      JavaArrays.asList(2, 6, 4, 9, 4, 8, 1, 0, 3, 6, 6, 6, 7, 1, 1, 7))
    val tile101 = new DenseTileData[JavaInt](new TileIndex(1, 0, 1, 4, 4), 0,
      JavaArrays.asList(2, 4, 8, 9, 5, 5, 2, 4, 8, 0, 8, 6, 4, 8, 0, 2))
    val tile111 = new DenseTileData[JavaInt](new TileIndex(1, 1, 1, 4, 4), 0,
      JavaArrays.asList(10, 3, 2, 10, 1, 4, 9, 0, 1, 1, 6, 8, 9, 0, 7, 6))

    writeIO.writeTiles[JavaInt](table, serializer, JavaArrays.asList(tile000, tile100, tile110, tile101, tile111))
  }

  // This require RabbitMQ on the building machine, which is not guaranteed to be installed,
  // so we set it to ignore by default.
  ignore("Test tile-client-based PyramidIO") {
    val pyramidIOProviders: Set[FactoryProvider[PyramidIO]] = DefaultPyramidIOFactoryProvider.values.toSet
    val pioFactoryProvider = new StandardPyramidIOFactoryProvider(pyramidIOProviders.asJava)
    val server = new TileServer("localhost", pioFactoryProvider)
    val tileSerializerProviders: Set[FactoryProvider[TileSerializer[_]]] = DefaultTileSerializerFactoryProvider.values.toSet
    val io = new TileServerPyramidIO("localhost", 1000*60*60)
    try {
      concurrent.future(server.listenForRequests)


      val rootPath = getRootPath
      val configuration = new JSONObject(
        s"""{
           |  "type": "file",
           |  "rootpath": "$rootPath",
           |  "extension": "avro"
           |}""".stripMargin)
      val tableName = "read-test"
      io.initializeForRead(tableName, 4, 4, JsonUtilities.jsonObjToProperties(configuration))
      val serializer = new PrimitiveAvroSerializer[JavaInt](classOf[JavaInt], CodecFactory.bzip2Codec())

      val metaData = new PyramidMetaData(io.readMetaData(tableName))
      val tiles = io.readTiles[JavaInt](tableName, serializer,
        JavaArrays.asList(new TileIndex(0, 0, 0, 4, 4), new TileIndex(1, 0, 0, 4, 4), new TileIndex(1, 1, 0, 4, 4),
          new TileIndex(1, 0, 1, 4, 4), new TileIndex(1, 1, 1, 4, 4)))

      assert(metaData.getName === "test data")
      assert(metaData.getDescription === "test description")
      assert(metaData.getMinZoom === 0)
      assert(metaData.getMaxZoom === 1)
      assert(metaData.getScheme === "TMS")
      assert(metaData.getProjection === "EPSG:4326")
      assert(metaData.getBounds.getMinX === 0.0)
      assert(metaData.getBounds.getMinY === 0.0)
      assert(metaData.getBounds.getMaxX === 1.0)
      assert(metaData.getBounds.getMaxY === 1.0)

      assert(tiles.get(0).getDefinition === new TileIndex(0, 0, 0, 4, 4))
      assert(tiles.get(1).getDefinition === new TileIndex(1, 0, 0, 4, 4))
      assert(tiles.get(2).getDefinition === new TileIndex(1, 1, 0, 4, 4))
      assert(tiles.get(3).getDefinition === new TileIndex(1, 0, 1, 4, 4))
      assert(tiles.get(4).getDefinition === new TileIndex(1, 1, 1, 4, 4))
    } finally {
      server.shutdown
    }
  }
}
