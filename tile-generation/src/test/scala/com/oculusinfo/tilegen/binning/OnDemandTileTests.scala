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

import com.oculusinfo.binning.{DenseTileData, SparseTileData, TileData, TileIndex}


class LiveTileTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId1 = "live-tile test 1"
	val pyramidId2 = "live-tile test 2"
	var dataFile1: File = null
	var dataFile2: File = null
	var pyramidIoA: OnDemandAccumulatorPyramidIO = null
	var pyramidIoB: OnDemandBinningPyramidIO = null
	var properties1: Properties = null
	var properties2: Properties = null

	override def beforeAll = {
		super.beforeAll
		createDataset1(sc)
		createDataset2(sc)
	}

	override def afterAll = {
		cleanupDataset1
		cleanupDataset2
		super.afterAll
	}

	private def createDataset1 (sc: SparkContext): Unit = {
		// Create our data
		dataFile1 = File.createTempFile("simple-live-tile-test", ".csv")
		println("Creating temporary data file "+dataFile1.getAbsolutePath())
		val writer = new FileWriter(dataFile1)
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
		properties1 = new Properties()
		properties1.setProperty("oculus.binning.source.location.0", dataFile1.getAbsolutePath())
		properties1.setProperty("oculus.binning.projection.autobounds", "false")
		properties1.setProperty("oculus.binning.projection.type", "areaofinterest")
		properties1.setProperty("oculus.binning.projection.minX", "0.0")
		properties1.setProperty("oculus.binning.projection.maxX", "7.9999")
		properties1.setProperty("oculus.binning.projection.minY", "0.0")
		properties1.setProperty("oculus.binning.projection.maxY", "7.9999")
		properties1.setProperty("oculus.binning.parsing.separator", ",")
		properties1.setProperty("oculus.binning.parsing.x.index", "0")
		properties1.setProperty("oculus.binning.parsing.y.index", "1")
		properties1.setProperty("oculus.binning.index.type", "cartesian")
		properties1.setProperty("oculus.binning.index.field.0", "x")
		properties1.setProperty("oculus.binning.index.field.1", "y")
		properties1.setProperty("oculus.binning.levels.0", "1")
	}

	private def createDataset2 (sc: SparkContext): Unit = {
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
		dataFile2 = File.createTempFile("sparse-dense-live-tile-test", "csv")
		println("Creating temporary data file "+dataFile2.getAbsolutePath)
		val writer = new FileWriter(dataFile2)
		data.map{case (x, y) => writer.write("%f,%f\n".format(x.toDouble, 7.0 - y.toDouble))}
		writer.flush()
		writer.close()

		// Property set for reading our second data file
		properties2 = new Properties()
		properties2.setProperty("oculus.binning.source.location.0", dataFile2.getAbsolutePath())
		properties2.setProperty("oculus.binning.projection.autobounds", "false")
		properties2.setProperty("oculus.binning.projection.type", "areaofinterest")
		properties2.setProperty("oculus.binning.projection.minX", "0.0")
		properties2.setProperty("oculus.binning.projection.maxX", "7.9999")
		properties2.setProperty("oculus.binning.projection.minY", "0.0")
		properties2.setProperty("oculus.binning.projection.maxY", "7.9999")
		properties2.setProperty("oculus.binning.parsing.separator", ",")
		properties2.setProperty("oculus.binning.parsing.x.index", "0")
		properties2.setProperty("oculus.binning.parsing.y.index", "1")
		properties2.setProperty("oculus.binning.index.type", "cartesian")
		properties2.setProperty("oculus.binning.index.field.0", "x")
		properties2.setProperty("oculus.binning.index.field.1", "y")
		properties2.setProperty("oculus.binning.levels.0", "1")
	}
	
	private def cleanupDataset1: Unit = {
		if (null != dataFile1 && dataFile1.exists) {
			println("Deleting temporary data file "+dataFile1)
			dataFile1.delete
		}
		dataFile1 = null
		properties1 = null
	}

	private def cleanupDataset2: Unit = {
		if (null != dataFile2 && dataFile2.exists) {
			println("Deleting temporary data file " + dataFile2)
			dataFile2.delete
		}
		dataFile2 = null
		properties2 = null
	}

	test("Simple binning - accumulator") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId1, 4, 4, properties1)

		val tile100 = pyramidIoA.readTiles(pyramidId1, null,
		                                   List(new TileIndex(1, 0, 0, 4, 4)).asJava)
		assert(tile100.isEmpty)
		val tile111 = pyramidIoA.readTiles(pyramidId1, null,
		                                   List(new TileIndex(1, 1, 1, 4, 4)).asJava)
		assert(tile111.isEmpty)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile000: TileData[_] =
			pyramidIoA.readTiles(pyramidId1, null,
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
			pyramidIoA.readTiles(pyramidId1, null,
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
			pyramidIoA.readTiles(pyramidId1, null,
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
		pyramidIoB.initializeForRead(pyramidId1, 4, 4, properties1)

		val tile100 = pyramidIoB.readTiles(pyramidId1, null,
		                                   List(new TileIndex(1, 0, 0, 4, 4)).asJava)
		assert(tile100.isEmpty)
		val tile111 = pyramidIoB.readTiles(pyramidId1, null,
		                                   List(new TileIndex(1, 1, 1, 4, 4)).asJava)
		assert(tile111.isEmpty)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile000: TileData[_] =
			pyramidIoB.readTiles(pyramidId1, null,
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
			pyramidIoB.readTiles(pyramidId1, null,
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
			pyramidIoB.readTiles(pyramidId1, null,
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

	test("choice of sparse or dense tiles - accumulator, heuristic") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId2, 4, 4, properties2)

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
		val denseProps = new Properties(properties2)
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
		val sparseProps = new Properties(properties2)
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
		pyramidIoB.initializeForRead(pyramidId2, 4, 4, properties2)

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
		val denseProps = new Properties(properties2)
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
		val sparseProps = new Properties(properties2)
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


	test("Accumulator cleanup") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId1, 4, 4, properties1)

		val store = pyramidIoA.debugAccumulatorStore

		pyramidIoA.readTiles(pyramidId1, null,
		                     List(new TileIndex(1, 0, 0, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 1)

		pyramidIoA.readTiles(pyramidId1, null,
		                     List(new TileIndex(1, 1, 0, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 1)


		pyramidIoA.readTiles(pyramidId1, null,
		                     List(new TileIndex(1, 0, 0, 4, 4),
		                          new TileIndex(1, 0, 1, 4, 4),
		                          new TileIndex(1, 1, 0, 4, 4),
		                          new TileIndex(1, 1, 1, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 4)

		pyramidIoA.readTiles(pyramidId1, null,
		                     List(new TileIndex(2, 1, 1, 4, 4)).asJava)
		assert(store.inUseCount === 0)
		assert(store.availableCount === 4)
	}
}
