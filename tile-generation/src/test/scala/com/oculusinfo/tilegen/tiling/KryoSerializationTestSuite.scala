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
import java.io.File
import java.io.FileWriter
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.Properties
import java.util.{List => JavaList}

import scala.collection.JavaConverters._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

import org.apache.spark.SparkContext
import org.apache.spark.SharedSparkContext

import com.oculusinfo.tilegen.binning.OnDemandAccumulatorPyramidIO
import com.oculusinfo.tilegen.binning.OnDemandBinningPyramidIO
import com.oculusinfo.tilegen.datasets.TileAssertions
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer
import com.oculusinfo.binning.util.TypeDescriptor



class KryoSerializationTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId = "kryo tile test"
	var dataFile: File = null
	var properties: Properties = null

	override def beforeAll = {
		super.beforeAll
		createDataset(sc)
	}

	override def afterAll = {
		cleanupDataset
		super.afterAll
	}
	private def createDataset (sc: SparkContext): Unit = {
		val (t, f) = (true, false)
		val rawData = List(List(t, t, t, t, f, f, f, f),
		                   List(t, t, t, t, f, f, f, f),
		                   List(t, t, t, t, f, f, f, f),
		                   List(t, t, t, t, t, f, f, f),
		                   List(t, f, t, f, t, f, t, f),
		                   List(f, t, f, t, f, t, f, t),
		                   List(t, f, t, f, t, f, t, f),
		                   List(f, t, f, t, t, t, f, t))
		val data = rawData.map(_.zipWithIndex).zipWithIndex.flatMap { case (xRow, y) =>
			xRow.filter(_._1).map(_._2).map(x => (x, y))
		}
		dataFile = File.createTempFile("sparse-dense-live-tile-test", "csv")
		println("Creating temporary data file "+dataFile.getAbsolutePath)
		val writer = new FileWriter(dataFile)
		data.map{case (x, y) => {
			         val xx = x.toDouble
			         val yy = 7.0 - y.toDouble
			         writer.write("%f,%f,%f,%f\n".format(xx, yy, xx+yy, xx*yy))}
		}
		writer.flush()
		writer.close()

		// Property set for reading our second data file
		properties = new Properties()
		properties.setProperty("oculus.binning.source.location.0", dataFile.getAbsolutePath())
		properties.setProperty("oculus.binning.projection.autobounds", "false")
		properties.setProperty("oculus.binning.projection.type", "areaofinterest")
		properties.setProperty("oculus.binning.projection.minX", "0.0")
		properties.setProperty("oculus.binning.projection.maxX", "7.9999")
		properties.setProperty("oculus.binning.projection.minY", "0.0")
		properties.setProperty("oculus.binning.projection.maxY", "7.9999")
		properties.setProperty("oculus.binning.parsing.separator", ",")
		properties.setProperty("oculus.binning.parsing.x.index", "0")
		properties.setProperty("oculus.binning.parsing.y.index", "1")
		properties.setProperty("oculus.binning.parsing.plus.index", "2")
		properties.setProperty("oculus.binning.parsing.times.index", "3")
		properties.setProperty("oculus.binning.index.type", "cartesian")
		properties.setProperty("oculus.binning.index.field.0", "x")
		properties.setProperty("oculus.binning.index.field.1", "y")
		properties.setProperty("oculus.binning.levels.0", "1")
	}

	private def cleanupDataset: Unit = {
		if (null != dataFile && dataFile.exists) {
			println("Deleting temporary data file "+dataFile)
			dataFile.delete
		}
		dataFile = null
		properties = null
	}

	def throughKryo (tile: TileData[JavaDouble]): TileData[JavaDouble] = {
		val kryoIO = new KryoSerializer[JavaDouble](new TypeDescriptor(classOf[JavaDouble]))
		val baos = new ByteArrayOutputStream
		kryoIO.serialize(tile, baos)
		baos.flush()
		baos.close()

		val bais = new ByteArrayInputStream(baos.toByteArray())
		kryoIO.deserialize(tile.getDefinition, bais)
	}

	test("Test kryo double serialization/deserialization") {
		// Create our pyramid IO
		val pyramidIo = new OnDemandBinningPyramidIO(sqlc)
		pyramidIo.initializeForRead(pyramidId, 4, 4, properties)

		val tile100 = throughKryo(pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0))
		val tile101 = throughKryo(pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0))
		val tile110 = throughKryo(pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0))
		val tile111 = throughKryo(pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0))

		// Exactly half-full tile - should be sparse
		assert(tile100.isInstanceOf[SparseTileData[_]])
		assertTileContents(List(1.0, 0.0, 1.0, 0.0,
		                        0.0, 1.0, 0.0, 1.0,
		                        1.0, 0.0, 1.0, 0.0,
		                        0.0, 1.0, 0.0, 1.0), tile100)
		// One more than half-full tile - should be dense
		assert(tile110.isInstanceOf[DenseTileData[_]])
		assertTileContents(List(1.0, 0.0, 1.0, 0.0,
		                        0.0, 1.0, 0.0, 1.0,
		                        1.0, 0.0, 1.0, 0.0,
		                        1.0, 1.0, 0.0, 1.0), tile110)
		// Full tile - should be dense
		assert(tile101.isInstanceOf[DenseTileData[_]])
		assertTileContents(List(1.0, 1.0, 1.0, 1.0,
		                        1.0, 1.0, 1.0, 1.0,
		                        1.0, 1.0, 1.0, 1.0,
		                        1.0, 1.0, 1.0, 1.0), tile101)
		// Nearly empty tile - should be sparse
		assert(tile111.isInstanceOf[SparseTileData[_]])
		assertTileContents(List(0.0, 0.0, 0.0, 0.0,
		                        0.0, 0.0, 0.0, 0.0,
		                        0.0, 0.0, 0.0, 0.0,
		                        1.0, 0.0, 0.0, 0.0), tile111)
	}

	def throughKryoSeries (tile: TileData[JavaList[JavaDouble]]): TileData[JavaList[JavaDouble]] = {
		val kryoIO = new KryoSerializer[JavaList[JavaDouble]](new TypeDescriptor(classOf[JavaList[JavaDouble]], new TypeDescriptor(classOf[JavaDouble])))
		val baos = new ByteArrayOutputStream
		kryoIO.serialize(tile, baos)
		baos.flush()
		baos.close()

		val bais = new ByteArrayInputStream(baos.toByteArray())
		kryoIO.deserialize(tile.getDefinition, bais)
	}

	test("test kryo double list serialization/deserialization") {
		// Create our pyramid IO
		val pyramidIo = new OnDemandBinningPyramidIO(sqlc)
		val seriesProps = new Properties(properties)
		val id = pyramidId+" series"
		seriesProps.setProperty("oculus.binning.value.type", "series")
		seriesProps.setProperty("oculus.binning.value.fields.0", "plus")
		seriesProps.setProperty("oculus.binning.value.fields.1", "times")
		pyramidIo.initializeForRead(id, 4, 4, seriesProps)

		val tile100 = throughKryoSeries(pyramidIo.readTiles(id, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0))
		val tile101 = throughKryoSeries(pyramidIo.readTiles(id, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0))
		val tile110 = throughKryoSeries(pyramidIo.readTiles(id, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0))
		val tile111 = throughKryoSeries(pyramidIo.readTiles(id, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0))

		// Exactly half-full tile - should be sparse
		assert(tile100.isInstanceOf[SparseTileData[_]])
		val empty = List[Double]()
		assertListTileContents(List[List[Double]](List(3.0, 0.0), empty, List(5.0, 6.0), empty,
		                                          empty, List(3.0, 2.0), empty, List(5.0, 6.0),
		                                          List(1.0, 0.0), empty, List(3.0, 2.0), empty,
		                                          empty, List(1.0, 0.0), empty, List(3.0, 0.0)), tile100)
		// One more than half-full tile - should be dense
		assert(tile110.isInstanceOf[DenseTileData[_]])
		assertListTileContents(List(List(7.0, 12.0), empty, List(9.0, 18.0), empty,
		                            empty, List(7.0, 10.0), empty, List(9.0, 14.0),
		                            List(5.0, 4.0), empty, List(7.0, 6.0), empty,
		                            List(4.0, 0.0), List(5.0, 0.0), empty, List(7.0, 0.0)), tile110)
		// Full tile - should be dense
		assert(tile101.isInstanceOf[DenseTileData[_]])
		assertListTileContents(List(List(7.0, 0.0), List(8.0, 7.0), List(9.0, 14.0), List(10.0, 21.0),
		                            List(6.0, 0.0), List(7.0, 6.0), List(8.0, 12.0), List(9.0, 18.0),
		                            List(5.0, 0.0), List(6.0, 5.0), List(7.0, 10.0), List(8.0, 15.0),
		                            List(4.0, 0.0), List(5.0, 4.0), List(6.0, 8.0), List(7.0, 12.0)), tile101)
		// Nearly empty tile - should be sparse
		assert(tile111.isInstanceOf[SparseTileData[_]])
		assertListTileContents(List(empty, empty, empty, empty,
		                            empty, empty, empty, empty,
		                            empty, empty, empty, empty,
		                            List(8.0, 16.0), empty, empty, empty), tile111)
	}
}
