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



import java.io.File
import java.io.FileWriter
import java.util.Properties

import scala.collection.JavaConverters._

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

import org.apache.spark.SparkContext
import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.{TileData, TileIndex}
import com.oculusinfo.tilegen.binning.OnDemandBinningPyramidIO
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescriptionTileWrapper
import com.oculusinfo.tilegen.tiling.analytics.MonolithicAnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.NumericMaxTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMinTileAnalytic



class DatasetAnalyticTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId: String = "test"
	var dataFile: File = null
	var pyramidIo: OnDemandBinningPyramidIO = null

	override def beforeAll = {
		super.beforeAll
		createDataset(sc)
	}

	override def afterAll  = {
		cleanupDataset
		super.afterAll
	}

	def createDataset (sc: SparkContext): Unit = {
		// Create some data
		dataFile = File.createTempFile("analytic-test", ".csv")
		println("Creating temporary data file "+dataFile.getAbsolutePath())
		val writer = new FileWriter(dataFile)
		for (x <- 0 until 256) {
			for (y <- 0 until 256) {
				writer.write("%d,%d,%d\n".format(x, y, x+y))
			}
		}
		writer.flush()
		writer.close()

		val props = new Properties()
		props.setProperty("oculus.binning.source.location.0", dataFile.getAbsolutePath())
		props.setProperty("oculus.binning.projection.type", "areaofinterest")
		props.setProperty("oculus.binning.projection.autobounds", "false")
		props.setProperty("oculus.binning.projection.minX", "0.0")
		props.setProperty("oculus.binning.projection.maxX", "256.0")
		props.setProperty("oculus.binning.projection.minY", "0.0")
		props.setProperty("oculus.binning.projection.maxY", "256.0")
		props.setProperty("oculus.binning.parsing.separator", ",")
		props.setProperty("oculus.binning.parsing.x.index", "0")
		props.setProperty("oculus.binning.parsing.y.index", "1")
		props.setProperty("oculus.binning.parsing.v.index", "2")
		props.setProperty("oculus.binning.parsing.v.fieldType", "long")
		props.setProperty("oculus.binning.index.type", "cartesian")
		props.setProperty("oculus.binning.index.field.0", "x")
		props.setProperty("oculus.binning.index.field.1", "y")
		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.valueType", "long")
		props.setProperty("oculus.binning.value.field", "v")
		props.setProperty("oculus.binning.value.aggregation", "mean")
		props.setProperty("oculus.binning.levels.0", "0,1,2,3,4,5,6")
		props.setProperty("oculus.binning.analytics.tile.0.analytic",
		                  "com.oculusinfo.tilegen.datasets.TestTileAnalytic")
		props.setProperty("oculus.binning.analytics.data.0.analytic",
		                  "com.oculusinfo.tilegen.datasets.TestDataAnalytic")
		props.setProperty("oculus.binning.analytics.data.0.fields.0", "v")

		pyramidIo = new OnDemandBinningPyramidIO(sqlc)
		pyramidIo.initializeForRead(pyramidId, 4, 4, props)
	}

	def cleanupDataset (): Unit = {
		if (dataFile.exists) {
			println("Deleting temporary data file "+dataFile)
			dataFile.delete
		}
	}



	test("Test averaging") {
		// Note that visually, the tiles should look exactly as we enter them here
		
		// First test a simple low-level tile to make sure the values come accross correctly
		val i600=new TileIndex(6, 0, 0, 4, 4)
		val tile600: TileData[_] = pyramidIo.readTiles(pyramidId, null, List(i600).asJava).get(0)
		assert(tile600.getDefinition === i600)
		assertTileContents(List[Double](3.0, 4.0, 5.0, 6.0,
		                                2.0, 3.0, 4.0, 5.0,
		                                1.0, 2.0, 3.0, 4.0,
		                                0.0, 1.0, 2.0, 3.0),
		                   tile600)

		// Now test one and two levels up to make sure that averaging works properly
		val i500 = new TileIndex(5, 0, 0, 4, 4)
		val tile500: TileData[_] = pyramidIo.readTiles(pyramidId, null, List(i500).asJava).get(0)
		assert(tile500.getDefinition === i500)
		assertTileContents(List[Double]( 7.0,  9.0, 11.0, 13.0,
		                                 5.0,  7.0,  9.0, 11.0,
		                                 3.0,  5.0,  7.0,  9.0,
		                                 1.0,  3.0,  5.0,  7.0),
		                   tile500)

		val i400 = new TileIndex(4, 0, 0, 4, 4)
		val tile400: TileData[_] = pyramidIo.readTiles(pyramidId, null, List(i400).asJava).get(0)
		assert(tile400.getDefinition === i400)
		assertTileContents(List[Double](15.0, 19.0, 23.0, 27.0,
		                                11.0, 15.0, 19.0, 23.0,
		                                7.0, 11.0, 15.0, 19.0,
		                                3.0,  7.0, 11.0, 15.0),
		                   tile400)

	}

	test("Test min/max values") {
		// Note that visually, the tiles should look exactly as we enter them here

		val analytics = pyramidIo.getTask(pyramidId).getTileAnalytics
		assert(analytics.isDefined)

		val i400 = new TileIndex(4, 0, 0, 4, 4)
		val tile400: TileData[_] = pyramidIo.readTiles(pyramidId, null, List(i400).asJava).get(0)
		assert(3.0 === tile400.getMetaData("minimum").toDouble)
		assert(27.0 === tile400.getMetaData("maximum").toDouble)
	}

	test("Custom analytics") {
		// Note that visually, the tiles should look exactly as we enter them here

		val analytics = pyramidIo.getTask(pyramidId).getDataAnalytics
		assert(analytics.isDefined)

		val i400 = new TileIndex(4, 0, 0, 4, 4)
		val tile400: TileData[_] = pyramidIo.readTiles(pyramidId, null, List(i400).asJava).get(0)
		assert(729.0 === tile400.getMetaData("tile test").toDouble)
		assert(1.0 == tile400.getMetaData("data test").toDouble)
	}
}

class TestTileAnalytic
		extends AnalysisDescriptionTileWrapper[Double, Double] (
	v => v*v,
	new NumericMaxTileAnalytic[Double](Some("tile test")))
{}

class TestDataAnalytic
		extends MonolithicAnalysisDescription[Seq[Any], Double] (
	v => {
		val num = v(0).asInstanceOf[Long]+1.0
		val result = num*num
		result
	},
	new NumericMinTileAnalytic[Double](Some("data test")))
{}
