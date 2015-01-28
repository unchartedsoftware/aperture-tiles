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
package com.oculusinfo.tilegen.datasets



import java.io.{FileWriter, File}
import java.util.Properties

import scala.collection.JavaConverters._

import org.scalatest.{BeforeAndAfterAll, FunSuite}

import org.apache.spark.{SparkContext, SharedSparkContext}

import com.oculusinfo.binning.{TileData, TileIndex}
import com.oculusinfo.tilegen.binning.OnDemandAccumulatorPyramidIO



/**
 * Created by nkronenfeld on 11/24/2014.
 */
class TilingTaskBinningTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId = "live-tile test"
	var dataFile: File = null
	var pyramidIo: OnDemandAccumulatorPyramidIO = null

	override def beforeAll = {
		super.beforeAll
		createDataset(sc)
	}

	override def afterAll = {
		cleanupDataset
		super.afterAll
	}

	private def createDataset(sc: SparkContext): Unit = {
		// Create our data
		// We create a simple data set with pairs (n, 7-n) as n goes from 0 to 6
		dataFile = File.createTempFile("simple-live-tile-test", ".csv")
		println("Creating temporary data file " + dataFile.getAbsolutePath())
		val writer = new FileWriter(dataFile)
		Range(0, 7).foreach(n =>
			writer.write("%f,%f\n".format(n.toDouble, (7 - n).toDouble))
		)
		writer.flush()
		writer.close()

		// Create our pyramid IO
		pyramidIo = new OnDemandAccumulatorPyramidIO(sqlc)

		// Read the one into the other
		val props = new Properties()
		props.setProperty("oculus.binning.source.location.0", dataFile.getAbsolutePath())
		props.setProperty("oculus.binning.projection.autobounds", "false")
		props.setProperty("oculus.binning.projection.type", "areaofinterest")
		props.setProperty("oculus.binning.projection.minX", "0.0")
		props.setProperty("oculus.binning.projection.maxX", "7.9999")
		props.setProperty("oculus.binning.projection.minY", "0.0")
		props.setProperty("oculus.binning.projection.maxY", "7.9999")
		props.setProperty("oculus.binning.parsing.separator", ",")
		props.setProperty("oculus.binning.parsing.x.index", "0")
		props.setProperty("oculus.binning.parsing.y.index", "1")
		props.setProperty("oculus.binning.index.type", "cartesian")
		props.setProperty("oculus.binning.index.field.0", "x")
		props.setProperty("oculus.binning.levels.0", "1")

		pyramidIo.initializeForRead(pyramidId, 4, 4, props)
	}

	private def cleanupDataset: Unit = {
		if (dataFile.exists) {
			println("Deleting temporary data file " + dataFile)
			dataFile.delete
		}
		dataFile = null
		pyramidIo = null
	}

	test("Simple one-dimensional binning using TilingTask") {
		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile000: TileData[_] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(0, 0, 0, 4, 1)).asJava).get(0)
		assert(tile000.getDefinition.getXBins() === 4)
		assert(tile000.getDefinition.getYBins() === 1)
		assertTileContents(List[Double](2.0, 2.0, 2.0, 1.0), tile000)
		val tile100: TileData[_] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(1, 0, 0, 4, 1)).asJava).get(0)
		assert(tile100.getDefinition.getXBins() === 4)
		assert(tile100.getDefinition.getYBins() === 1)
		assertTileContents(List[Double](1.0, 1.0, 1.0, 1.0), tile100)
		val tile110: TileData[_] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(1, 1, 0, 4, 1)).asJava).get(0)
		assert(tile110.getDefinition.getXBins() === 4)
		assert(tile110.getDefinition.getYBins() === 1)
		assertTileContents(List[Double](1.0, 1.0, 1.0, 0.0), tile110)
	}
}
