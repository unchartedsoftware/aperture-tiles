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

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

import org.apache.spark.SharedSparkContext
import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.tilegen.datasets.TileAssertions



/**
 * Tests assuring that tiles work with types other than the default
 */
class OnDemandTypeTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId = "on-demand type system tests"
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
		dataFile = File.createTempFile("non-double-live-tile-test", ".csv")
		println("Creating temporary data file "+dataFile.getAbsolutePath)
		val writer = new FileWriter(dataFile)
		(0 to 7).foreach(n => writer.write("%d\t%d\t%d\n".format(n, n, n)))
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
		properties.setProperty("oculus.binning.parsing.separator", "\t")
		properties.setProperty("oculus.binning.parsing.x.index", "0")
		properties.setProperty("oculus.binning.parsing.x.fieldType", "long")
		properties.setProperty("oculus.binning.parsing.y.index", "1")
		properties.setProperty("oculus.binning.parsing.y.fieldType", "long")
		properties.setProperty("oculus.binning.parsing.v.index", "2")
		properties.setProperty("oculus.binning.parsing.v.fieldType", "long")
		properties.setProperty("oculus.binning.index.type", "cartesian")
		properties.setProperty("oculus.binning.index.field.0", "x")
		properties.setProperty("oculus.binning.index.field.1", "y")
		properties.setProperty("oculus.binning.value.type", "field")
		properties.setProperty("oculus.binning.value.field", "v")
		properties.setProperty("oculus.binning.value.valueType", "long")
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



	test("non-double cartesian inputs") {
		val pyramidIoA = new OnDemandAccumulatorPyramidIO(sqlc)
		pyramidIoA.initializeForRead(pyramidId, 4, 4, properties)
		val tile000 = pyramidIoA.readTiles(pyramidId, null, List(new TileIndex(0, 0, 0, 4, 4)).asJava).get(0)
		assertTileContents(List(0.0, 0.0, 0.0, 13.0,
		                        0.0, 0.0, 9.0, 0.0,
		                        0.0, 5.0, 0.0, 0.0,
		                        1.0, 0.0, 0.0, 0.0), tile000)
	}
}
