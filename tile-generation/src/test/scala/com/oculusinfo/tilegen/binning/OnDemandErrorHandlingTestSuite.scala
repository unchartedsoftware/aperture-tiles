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

import scala.collection.JavaConverters._

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

import org.apache.spark.SharedSparkContext
import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.tilegen.datasets.TileAssertions



class OnDemandErrorHandlingTestSuite  extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId = "on-demand tiling error-handling tests"
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
		// Write a header column, to make sure it gets weeded out
		writer.write("X,Y,V\n")         // Header line, should be ignored
		writer.write("\n")              // Blank line, should be ignored
		writer.write("1.0\n")           // Too few fields
		writer.write("1.0,2.0\n")
		writer.write("1.0,2.0,a\n")     // wrong type fields
		writer.write("1.0,a,3.0\n")
		writer.write("a,2.0,3.0\n")
		writer.write("abc, def\n")      // Too few fields, and the wrong type
		writer.write("1.0, 2.0, 3.0\n") // extra spaces - should be good
		writer.write("2.0,3.0,4.0\n")   // perfectly good
		writer.flush()
		writer.close()

		// Read the one into the other
		properties = new Properties()
		properties.setProperty("oculus.binning.source.location.0", dataFile.getAbsolutePath())
		properties.setProperty("oculus.binning.projection.autobounds", "true")
		properties.setProperty("oculus.binning.projection.type", "areaofinterest")
		properties.setProperty("oculus.binning.parsing.separator", ",")
		properties.setProperty("oculus.binning.parsing.x.index", "0")
		properties.setProperty("oculus.binning.parsing.y.index", "1")
		properties.setProperty("oculus.binning.parsing.v.index", "2")
		properties.setProperty("oculus.binning.index.type", "cartesian")
		properties.setProperty("oculus.binning.index.field.0", "x")
		properties.setProperty("oculus.binning.index.field.1", "y")
		properties.setProperty("oculus.binning.value.type", "field")
		properties.setProperty("oculus.binning.value.field", "v")
		properties.setProperty("oculus.binning.value.valuetype", "int")
		properties.setProperty("oculus.binning.levels.0", "0,1")
	}

	private def cleanupDataset: Unit = {
		if (null != dataFile && dataFile.exists) {
			println("Deleting temporary data file "+dataFile)
			dataFile.delete
		}
		dataFile = null
		properties = null
	}



	test("Ignore lines with errors - accumulator") {
		// Create our pyramid IO
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId, 1, 1, properties)

		val tile000List = pyramidIoA.readTiles(pyramidId, null, List(new TileIndex(0, 0, 0, 1, 1)).asJava)
		val tile000 = tile000List.get(0)
		assert(tile000.getDefinition.getXBins() === 1)
		assert(tile000.getDefinition.getYBins() === 1)
		assertTileContents(List[Double](7.0),  tile000)

		val tile100List = pyramidIoA.readTiles(pyramidId, null, List(new TileIndex(1, 0, 0, 1, 1)).asJava)
		val tile100 = tile100List.get(0)
		assertTileContents(List[Double](3.0),  tile100)
		val tile111List = pyramidIoA.readTiles(pyramidId, null, List(new TileIndex(1, 1, 1, 1, 1)).asJava)
		val tile111 = tile111List.get(0)
		assertTileContents(List[Double](4.0),  tile111)

		assert(pyramidIoA.readTiles(pyramidId, null, List(new TileIndex(1, 0, 1, 1, 1)).asJava).isEmpty)
		assert(pyramidIoA.readTiles(pyramidId, null, List(new TileIndex(1, 1, 0, 1, 1)).asJava).isEmpty)
	}
}
