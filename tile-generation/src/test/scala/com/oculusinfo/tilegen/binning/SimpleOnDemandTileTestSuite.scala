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



import java.util.Properties
import java.io.File
import java.io.FileWriter

import scala.collection.JavaConverters._

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

import org.apache.spark.SparkContext
import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.tilegen.datasets.TileAssertions



/**
 * Basic, simple on-demand tiling tests
 */
class SimpleOnDemandTileTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId = "simple on-demand tiling tests"
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
		// Create our data
		dataFile = File.createTempFile("simple-live-tile-test", ".csv")
		println("Creating temporary data file "+dataFile.getAbsolutePath())
		val writer = new FileWriter(dataFile)
		Range(0, 12).foreach(n =>
			if (n < 8) {
				// Good data
				writer.write("%f,%f\n".format(n.toDouble, (7-n).toDouble))
			} else {
				// Bad data
				writer.write("a,b)\n")
			}
		)
		writer.flush()
		writer.close()

		// Read the one into the other
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
			println("Deleting temporary data file "+dataFile)
			dataFile.delete
		}
		dataFile = null
		properties = null
	}



	test("Simple binning - accumulator") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId, 4, 4, properties)

		val tile100 = pyramidIoA.readTiles(pyramidId, null,
		                                   List(new TileIndex(1, 0, 0, 4, 4)).asJava)
		assert(tile100.isEmpty)
		val tile111 = pyramidIoA.readTiles(pyramidId, null,
		                                   List(new TileIndex(1, 1, 1, 4, 4)).asJava)
		assert(tile111.isEmpty)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile000: TileData[_] =
			pyramidIoA.readTiles(pyramidId, null,
			                     List(new TileIndex(0, 0, 0, 4, 4)).asJava).get(0)
		assert(tile000.getDefinition.getXBins() === 4)
		assert(tile000.getDefinition.getYBins() === 4)
		assertTileContents(List[Double](2.0, 0.0, 0.0, 0.0,
		                                0.0, 2.0, 0.0, 0.0,
		                                0.0, 0.0, 2.0, 0.0,
		                                0.0, 0.0, 0.0, 2.0),  tile000)
		// Tile should definitely be sparse - it's only 1/4 full
		assert(tile000.isInstanceOf[SparseTileData[_]])

		val tile101: TileData[_] =
			pyramidIoA.readTiles(pyramidId, null,
			                     List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		assert(tile101.getDefinition.getXBins() === 4)
		assert(tile101.getDefinition.getYBins() === 4)
		assertTileContents(List[Double](1.0, 0.0, 0.0, 0.0,
		                                0.0, 1.0, 0.0, 0.0,
		                                0.0, 0.0, 1.0, 0.0,
		                                0.0, 0.0, 0.0, 1.0), tile101)
		// Tile should definitely be sparse - it's only 1/4 full
		assert(tile101.isInstanceOf[SparseTileData[_]])

		val tile110: TileData[_] =
			pyramidIoA.readTiles(pyramidId, null,
			                     List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		assert(tile110.getDefinition.getXBins() === 4)
		assert(tile110.getDefinition.getYBins() === 4)
		assertTileContents(List[Double](1.0, 0.0, 0.0, 0.0,
		                                0.0, 1.0, 0.0, 0.0,
		                                0.0, 0.0, 1.0, 0.0,
		                                0.0, 0.0, 0.0, 1.0), tile110)
		// Tile should definitely be sparse - it's only 1/4 full
		assert(tile110.isInstanceOf[SparseTileData[_]])
	}



	test("Simple binning - traditional binning") {
		// Create our pyramid IO
		val pyramidIoB = new OnDemandBinningPyramidIO(sqlc)
		pyramidIoB.initializeForRead(pyramidId, 4, 4, properties)

		val tile100 = pyramidIoB.readTiles(pyramidId, null,
		                                   List(new TileIndex(1, 0, 0, 4, 4)).asJava)
		assert(tile100.isEmpty)
		val tile111 = pyramidIoB.readTiles(pyramidId, null,
		                                   List(new TileIndex(1, 1, 1, 4, 4)).asJava)
		assert(tile111.isEmpty)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile000: TileData[_] =
			pyramidIoB.readTiles(pyramidId, null,
			                     List(new TileIndex(0, 0, 0, 4, 4)).asJava).get(0)
		assert(tile000.getDefinition.getXBins() === 4)
		assert(tile000.getDefinition.getYBins() === 4)
		assertTileContents(List[Double](2.0, 0.0, 0.0, 0.0,
		                                0.0, 2.0, 0.0, 0.0,
		                                0.0, 0.0, 2.0, 0.0,
		                                0.0, 0.0, 0.0, 2.0), tile000)
		// Tile should definitely be sparse - it's only 1/4 full
		assert(tile000.isInstanceOf[SparseTileData[_]])

		val tile101: TileData[_] =
			pyramidIoB.readTiles(pyramidId, null,
			                     List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
		assert(tile101.getDefinition.getXBins() === 4)
		assert(tile101.getDefinition.getYBins() === 4)
		assertTileContents(List[Double](1.0, 0.0, 0.0, 0.0,
		                                0.0, 1.0, 0.0, 0.0,
		                                0.0, 0.0, 1.0, 0.0,
		                                0.0, 0.0, 0.0, 1.0), tile101)
		// Tile should definitely be sparse - it's only 1/4 full
		assert(tile101.isInstanceOf[SparseTileData[_]])

		val tile110: TileData[_] =
			pyramidIoB.readTiles(pyramidId, null,
			                     List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
		assert(tile110.getDefinition.getXBins() === 4)
		assert(tile110.getDefinition.getYBins() === 4)
		assertTileContents(List[Double](1.0, 0.0, 0.0, 0.0,
		                                0.0, 1.0, 0.0, 0.0,
		                                0.0, 0.0, 1.0, 0.0,
		                                0.0, 0.0, 0.0, 1.0), tile110)
		// Tile should definitely be sparse - it's only 1/4 full
		assert(tile110.isInstanceOf[SparseTileData[_]])
	}



	test("Accumulator cleanup") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId, 4, 4, properties)

		val store = pyramidIoA.debugAccumulatorStore

		pyramidIoA.readTiles(pyramidId, null,
		                     List(new TileIndex(1, 0, 0, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 1)

		pyramidIoA.readTiles(pyramidId, null,
		                     List(new TileIndex(1, 1, 0, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 1)


		pyramidIoA.readTiles(pyramidId, null,
		                     List(new TileIndex(1, 0, 0, 4, 4),
		                          new TileIndex(1, 0, 1, 4, 4),
		                          new TileIndex(1, 1, 0, 4, 4),
		                          new TileIndex(1, 1, 1, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 4)

		pyramidIoA.readTiles(pyramidId, null,
		                     List(new TileIndex(2, 1, 1, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 4)
	}
}