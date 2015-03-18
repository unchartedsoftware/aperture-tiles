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

package com.oculusinfo.tilegen.binning



import java.io.File
import java.io.FileWriter
import java.util.Properties
import com.oculusinfo.tilegen.datasets.TileAssertions
import scala.collection.JavaConverters._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import org.apache.spark.SparkContext
import org.apache.spark.SharedSparkContext
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.tilegen.datasets.CSVReader
import com.oculusinfo.tilegen.util.PropertiesWrapper


/*
 * Tests assuring that tiles are made with the right density
 */
class OnDemandTileDensityTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId2 = "on-demand tile density tests"
	var dataFile: File = null
	var pyramidIoA: OnDemandAccumulatorPyramidIO = null
	var pyramidIoB: OnDemandBinningPyramidIO = null
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
		data.map{case (x, y) => writer.write("%f,%f\n".format(x.toDouble, 7.0 - y.toDouble))}
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
		properties.setProperty("oculus.binning.index.type", "cartesian")
		properties.setProperty("oculus.binning.index.field.0", "x")
		properties.setProperty("oculus.binning.index.field.1", "y")
		properties.setProperty("oculus.binning.levels.0", "1")
	}

	private def cleanupDataset: Unit = {
		if (null != dataFile && dataFile.exists) {
			println("Deleting temporary data file " + dataFile)
			dataFile.delete
		}
		dataFile = null
		properties = null
	}


	test("choice of sparse or dense tiles - accumulator, heuristic") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId2, 4, 4, properties)

		val tile100 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0)
		val tile101 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		val tile110 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		val tile111 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0)

		// Exactly half-full tile - should be sparse
		assert(tile100.isInstanceOf[SparseTileData[_]])
		// One more than half-full tile - should be dense
		assert(tile110.isInstanceOf[DenseTileData[_]])
		// Full tile - should be dense
		assert(tile101.isInstanceOf[DenseTileData[_]])
		// Nearly empty tile - should be sparse
		assert(tile111.isInstanceOf[SparseTileData[_]])
	}

	test("choice of sparse or dense tiles - accumulator, dense") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		val denseProps = new Properties(properties)
		val keys = denseProps.stringPropertyNames()
		val tt = denseProps.getProperty("oculus.binning.index.type")
		denseProps.setProperty("oculus.binning.tileType", "dense")
		pyramidIoA.initializeForRead(pyramidId2, 4, 4, denseProps)

		val tile100 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0)
		val tile101 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		val tile110 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		val tile111 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0)

		// Exactly half-full tile
		assert(tile100.isInstanceOf[DenseTileData[_]])
		// One more than half-full tile
		assert(tile110.isInstanceOf[DenseTileData[_]])
		// Full tile - should be dense
		assert(tile101.isInstanceOf[DenseTileData[_]])
		// Nearly empty tile
		assert(tile111.isInstanceOf[DenseTileData[_]])
	}

	test("choice of sparse or dense tiles - accumulator, sparse") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		val sparseProps = new Properties(properties)
		sparseProps.setProperty("oculus.binning.tileType", "sparse")
		pyramidIoA.initializeForRead(pyramidId2, 4, 4, sparseProps)

		val tile100 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0)
		val tile101 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		val tile110 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		val tile111 = pyramidIoA.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0)

		// Exactly half-full tile
		assert(tile100.isInstanceOf[SparseTileData[_]])
		// One more than half-full tile
		assert(tile110.isInstanceOf[SparseTileData[_]])
		// Full tile - should be dense
		assert(tile101.isInstanceOf[SparseTileData[_]])
		// Nearly empty tile
		assert(tile111.isInstanceOf[SparseTileData[_]])
	}

	test("choice of sparse or dense tiles - traditional binning, heuristic") {
		// Create our pyramid IO
		val pyramidIoB = new OnDemandBinningPyramidIO(sqlc)
		pyramidIoB.initializeForRead(pyramidId2, 4, 4, properties)

		val tile100 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0)
		val tile101 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		val tile110 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		val tile111 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0)

		// Exactly half-full tile - should be sparse
		assert(tile100.isInstanceOf[SparseTileData[_]])
		// One more than half-full tile - should be dense
		assert(tile110.isInstanceOf[DenseTileData[_]])
		// Full tile - should be dense
		assert(tile101.isInstanceOf[DenseTileData[_]])
		// Nearly empty tile - should be sparse
		assert(tile111.isInstanceOf[SparseTileData[_]])
	}

	test("choice of sparse or dense tiles - traditional binning, dense") {
		// Create our pyramid IO
		val pyramidIoB = new OnDemandBinningPyramidIO(sqlc)
		val denseProps = new Properties(properties)
		val keys = denseProps.stringPropertyNames()
		val tt = denseProps.getProperty("oculus.binning.index.type")
		denseProps.setProperty("oculus.binning.tileType", "dense")
		pyramidIoB.initializeForRead(pyramidId2, 4, 4, denseProps)

		val tile100 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0)
		val tile101 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		val tile110 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		val tile111 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0)

		// Exactly half-full tile
		assert(tile100.isInstanceOf[DenseTileData[_]])
		// One more than half-full tile
		assert(tile110.isInstanceOf[DenseTileData[_]])
		// Full tile - should be dense
		assert(tile101.isInstanceOf[DenseTileData[_]])
		// Nearly empty tile
		assert(tile111.isInstanceOf[DenseTileData[_]])
	}

	test("choice of sparse or dense tiles - traditional binning, sparse") {
		// Create our pyramid IO
		val pyramidIoB = new OnDemandBinningPyramidIO(sqlc)
		val sparseProps = new Properties(properties)
		sparseProps.setProperty("oculus.binning.tileType", "sparse")
		pyramidIoB.initializeForRead(pyramidId2, 4, 4, sparseProps)

		val tile100 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava).get(0)
		val tile101 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		val tile110 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		val tile111 = pyramidIoB.readTiles(pyramidId2, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava).get(0)

		// Exactly half-full tile
		assert(tile100.isInstanceOf[SparseTileData[_]])
		// One more than half-full tile
		assert(tile110.isInstanceOf[SparseTileData[_]])
		// Full tile - should be dense
		assert(tile101.isInstanceOf[SparseTileData[_]])
		// Nearly empty tile
		assert(tile111.isInstanceOf[SparseTileData[_]])
	}
}
