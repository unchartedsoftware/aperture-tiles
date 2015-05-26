
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



import java.util.Properties

import org.apache.spark.sql.DataFrame

import scala.collection.JavaConverters._

import org.scalatest.{BeforeAndAfterAll, FunSuite}

import org.apache.spark.{SparkContext, SharedSparkContext}

import com.oculusinfo.binning.{TileData, TileIndex}
import com.oculusinfo.tilegen.binning.OnDemandAccumulatorPyramidIO


/**
 * Basic tests for the TilingTask object
 */
class TilingTaskTestSuite extends FunSuite with SharedSparkContext with BeforeAndAfterAll with TileAssertions {
	val pyramidId = "unknown.x.y.count"
	var data: DataFrame = null
	var task: TilingTask[_, _, _, _] = null
	var pyramidIo: OnDemandAccumulatorPyramidIO = null

	override def beforeAll = {
		super.beforeAll
		createTask(sc)
	}

	override def afterAll = {
		cleanupTask
		super.afterAll
	}

	private def createTask(sc: SparkContext): Unit = {
		// Create our data
		// We create a simple data set with pairs (n, 7-n) as n goes from 0 to 6
		data = sqlc.jsonRDD(sc.parallelize(Range(0, 7)).map(n =>
			                    "{\"x\": %f, \"y\": %f}".format(n.toDouble, (7-n).toDouble)
		                    ))
		data.registerTempTable("test")

		// Create our pyramid IO
		pyramidIo = new OnDemandAccumulatorPyramidIO(sqlc)

		// Read the one into the other
		val props = new Properties()
		props.setProperty("oculus.binning.source.type", "schema")
		props.setProperty("oculus.binning.table", "test")
		props.setProperty("oculus.binning.projection.autobounds", "false")
		props.setProperty("oculus.binning.projection.type", "EPSG:4326")
		props.setProperty("oculus.binning.projection.minX", "0.0")
		props.setProperty("oculus.binning.projection.maxX", "7.9999")
		props.setProperty("oculus.binning.projection.minY", "0.0")
		props.setProperty("oculus.binning.projection.maxY", "7.9999")
		props.setProperty("oculus.binning.index.type", "cartesian")
		props.setProperty("oculus.binning.index.field.0", "x")
		props.setProperty("oculus.binning.index.field.1", "y")
		props.setProperty("oculus.binning.levels.0", "1")

		val task = TilingTask(sqlc, "test", props)
		pyramidIo.initializeDirectly(pyramidId, task)
	}

	private def cleanupTask: Unit = {
		data = null
		pyramidIo = null
	}

	test("Simple tiling using TilingTask") {
		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile000: TileData[_] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(0, 0, 0, 2, 2)).asJava).get(0)
		assert(tile000.getDefinition.getXBins() === 2)
		assert(tile000.getDefinition.getYBins() === 2)
		assertTileContents(List[Double](4.0, 0.0,
		                                0.0, 3.0),
		                   tile000)

		val tile100: Seq[TileData[_]] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(1, 0, 0, 2, 2)).asJava).asScala
		assert(0 === tile100.size)

		val tile110: TileData[_] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(1, 1, 0, 2, 2)).asJava).get(0)
		assert(tile110.getDefinition.getXBins() === 2)
		assert(tile110.getDefinition.getYBins() === 2)
		assertTileContents(List[Double](2.0, 0.0,
		                                0.0, 1.0),
		                   tile110)

		val tile101: TileData[_] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(1, 0, 1, 2, 2)).asJava).get(0)
		assert(tile101.getDefinition.getXBins() === 2)
		assert(tile101.getDefinition.getYBins() === 2)
		assertTileContents(List[Double](2.0, 0.0,
		                                0.0, 2.0),
		                   tile101)

		val tile111: Seq[TileData[_]] =
			pyramidIo.readTiles(pyramidId, null,
			                    List(new TileIndex(1, 1, 1, 2, 2)).asJava).asScala
		assert(0 === tile111.size)
	}
}
