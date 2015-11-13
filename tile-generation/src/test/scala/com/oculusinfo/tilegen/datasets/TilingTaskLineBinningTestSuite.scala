/*
 * Copyright (c) 2015 Uncharted Software http://www.uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.datasets



import java.lang.{Integer => JavaInt, Double => JavaDouble}
import java.io.{FileWriter, File}
import java.util.Properties

import com.oculusinfo.tilegen.pipeline.PipelineData

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.{BinIndex, TileData, TileIndex}
import com.oculusinfo.tilegen.binning.{OnDemandBinningPyramidIO, OnDemandAccumulatorPyramidIO}
import com.oculusinfo.tilegen.tiling.{StandardBinningFunctions, TestTileIO}
import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.spark.sql.DataFrame
import org.apache.spark.{SparkContext, SharedSparkContext}
import org.scalatest.{BeforeAndAfterAll, FunSuite}



case class LineBinningTestRecord (x1: Double, y1: Double, x2: Double, y2: Double)
case class LineBinningValuedTestRecord (x1: Double, y1: Double, x2: Double, y2: Double, value: Double)

class TilingTaskLineBinningTestSuite extends FunSuite with SharedSparkContext with TileAssertions {
	def testAoILine (pt1: (Double, Double), pt2: (Double, Double), level: Int, expected: Map[TileIndex, Option[TileData[JavaDouble]]]): Unit = {
		testAoILine(List(LineBinningTestRecord(pt1._1, pt1._2, pt2._1, pt2._2)), level, expected)
	}

	def testAoILine[A <: Product : TypeTag](rawData: Seq[A], level: Int,
	                                        expected: Map[TileIndex, Option[TileData[JavaDouble]]],
	                                        propertyOverrides: Map[String, String] = Map[String, String](),
                                          minimumSegmentLength:Some[Int] = Some(2), maximumSegmentLength:Some[Int] = Some(8)): Unit = {
		val pyramidId = "segmentTest"
		val tileIO = new TestTileIO
		try {
			val data: DataFrame = sqlc.createDataFrame(rawData)

			val props = new Properties()
			props.setProperty("oculus.binning.projection.autobounds", "false")
			props.setProperty("oculus.binning.projection.type", "areaofinterest")
			props.setProperty("oculus.binning.projection.minX", "0.0")
			props.setProperty("oculus.binning.projection.maxX", "15.9999")
			props.setProperty("oculus.binning.projection.minY", "0.0")
			props.setProperty("oculus.binning.projection.maxY", "15.9999")
			props.setProperty("oculus.binning.index.type", "cartesian")
			props.setProperty("oculus.binning.index.field.0", "x1")
			props.setProperty("oculus.binning.index.field.1", "y1")
			props.setProperty("oculus.binning.index.field.2", "x2")
			props.setProperty("oculus.binning.index.field.3", "y2")
			props.setProperty("oculus.binning.levels.0", level.toString)
			props.setProperty("oculus.binning.name", pyramidId)
			props.setProperty("oculus.binning.lineType", "Lines")
			props.setProperty("oculus.binning.tileWidth", "4")
			props.setProperty("oculus.binning.tileHeight", "4")
			propertyOverrides.foreach{case (property, value) => props.setProperty(property, value)}

			data.registerTempTable(pyramidId)

			val task = TilingTask(sqlc, pyramidId, props)

      def withValueType[PT: ClassTag] (task: TilingTask[PT, _, _, _]) = {
        val locateFcn = StandardBinningFunctions.locateLine(task.getIndexScheme, task.getTilePyramid,
          minimumSegmentLength, maximumSegmentLength, task.getNumXBins, task.getNumYBins)
        val populateFcn: (TileIndex, Array[BinIndex], PT) => MutableMap[BinIndex, PT] = StandardBinningFunctions.populateTileWithLineSegments(StandardScalingFunctions.identityScale)

        task.doParameterizedTiling(tileIO, locateFcn, populateFcn)
      }
      withValueType(task)

			// Make sure only the expected tiles were created
			val allGenerated = tileIO.getPyramid(pyramidId)
			if (0 == expected.size)
				assert(0 === allGenerated.size || 0 === allGenerated.get.size)
			else
				assert(expected.size === allGenerated.get.size)

			// Make sure all the expected tiles were created
			expected.map { case (index, data) =>
				val actual = tileIO.getTile(pyramidId, index)
				if (data.isEmpty) assert(actual.isEmpty)
				else assertTileContents(data.get, actual.get)
			}
		} finally {
			tileIO.clearPyramid(pyramidId)
		}
	}


	def d(x: Double): JavaDouble = new JavaDouble(x)

	def createTile(index: TileIndex, defaultValue: Double, values: List[Double]): (TileIndex, Option[TileData[JavaDouble]]) = {
		val tile = new DenseTileData[JavaDouble](index, new JavaDouble(defaultValue), values.map(x => new JavaDouble(x)).asJava)
		tile.setMetaData("minimum", values.reduce(_ min _).toString)
		tile.setMetaData("maximum", values.reduce(_ max _).toString)
		(index -> Some(tile))
	}

	test("test horizontal lines") {
		// Test minimum segment length
		testAoILine((1.0, 1.0), (1.9999, 1.0), 0, Map[TileIndex, Option[TileData[JavaDouble]]]())

		// Test level 0
		testAoILine(
			(1.0, 7.9999), (14.9999, 7.9999), 0,
			Map(createTile(
				    new TileIndex(0, 0, 0, 4, 4), 0.0,
				    List(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0)
			    ))
		)

		// Test level 1
		testAoILine(
			(1.0, 7.9999), (14.9999, 7.9999), 1,
			Map(
				createTile(
					new TileIndex(1, 0, 0, 4, 4), 0.0,
					List(1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
				),
				createTile(
					new TileIndex(1, 1, 0, 4, 4), 0.0,
					List(1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
				)
			)
		)

		// Test level 2
		testAoILine((1.0, 7.9999), (14.9999, 7.9999), 2, Map[TileIndex, Option[TileData[JavaDouble]]]())
	}

	test("Test vertical lines") {
		// Test minimum segment length
		testAoILine((1.0, 1.0), (1.0, 1.9999), 0, Map[TileIndex, Option[TileData[JavaDouble]]]())

		// Test level 0
		testAoILine(
			(7.9999, 1.0), (7.9999, 14.9999), 0,
			Map(createTile(
				    new TileIndex(0, 0, 0, 4, 4), 0.0,
				    List(0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0)
			    ))
		)

		// Test level 1
		testAoILine(
			(7.9999, 1.0), (7.9999, 14.9999), 1,
			Map(
				createTile(
					new TileIndex(1, 0, 0, 4, 4), 0.0,
					List(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
				),
				createTile(
					new TileIndex(1, 0, 1, 4, 4), 0.0,
					List(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
				)
			)
		)

		// Test level 2
		testAoILine((7.9999, 1.0), (7.9999, 14.9999), 2, Map[TileIndex, Option[TileData[JavaDouble]]]())
	}

	test("Test diagonal lines") {
		// Test minimum segment length
		testAoILine((1.0, 1.0), (1.9999, 1.9999), 0, Map[TileIndex, Option[TileData[JavaDouble]]]())

		// Level 0
		testAoILine(
			(1.0, 1.0), (14.9999, 14.9999), 0,
			Map(
				createTile(new TileIndex(0, 0, 0, 4, 4), 0.0,
				           List(0.0, 0.0, 0.0, 1.0,  0.0, 0.0, 1.0, 0.0,  0.0, 1.0, 0.0, 0.0,  1.0, 0.0, 0.0, 0.0)
				)
			)
		)

		// Level 1
		testAoILine(
			(1.0, 1.0), (14.9999, 14.9999), 1,
			Map(
				createTile(
					new TileIndex(1, 0, 0, 4, 4), 0.0,
					List(0.0, 0.0, 0.0, 1.0,  0.0, 0.0, 1.0, 0.0,  0.0, 1.0, 0.0, 0.0,  1.0, 0.0, 0.0, 0.0)
				),
				createTile(
					new TileIndex(1, 1, 1, 4, 4), 0.0,
					List(0.0, 0.0, 0.0, 1.0,  0.0, 0.0, 1.0, 0.0,  0.0, 1.0, 0.0, 0.0,  1.0, 0.0, 0.0, 0.0)
				)
			)
		)

		// Level 2
		testAoILine((1.0, 1.0), (14.9999, 14.9999), 2, Map[TileIndex, Option[TileData[JavaDouble]]]())
	}

	// Test a more complex case, with multiple lines and a different minimum length
	test("test multi-line") {
		val rawData = (0 to 15).map(n => LineBinningTestRecord(0.5, n+0.5, n+0.5, n+0.6))
		testAoILine(
			rawData, 2,
			Map(
				createTile(
					new TileIndex(2, 0, 1, 4, 4), 0.0,
					List(1.0, 1.0, 1.0, 1.0,  1.0, 1.0, 1.0, 1.0,  1.0, 1.0, 1.0, 1.0,  1.0, 1.0, 1.0, 1.0)
				),
				createTile(
					new TileIndex(2, 1, 1, 4, 4), 0.0,
					List(1.0, 1.0, 1.0, 1.0,  1.0, 1.0, 1.0, 0.0,  1.0, 1.0, 0.0, 0.0,  1.0, 0.0, 0.0, 0.0)
				)
			),
			Map(),
      Some(4),
      Some(8)
		)
	}


	// Test line binning with non-sum values
	test("Line binning with non-sum valuation") {
		// +----+----+
		// |0000|0000|
		// |0003|0500|
		// |0223|2520|
		// |0003|0500|
		// +----+----+
		// |0444|4540|
		// |0003|0500|
		// |0003|0500|
		// |0000|0000|
		// +----+----+
		val rawData = Seq(
			LineBinningValuedTestRecord(1.5, 3.5, 6.5, 3.5, 4.0),
			LineBinningValuedTestRecord(1.5, 5.5, 6.5, 5.5, 2.0),
			LineBinningValuedTestRecord(3.5, 1.5, 3.5, 6.5, 3.0),
			LineBinningValuedTestRecord(5.5, 1.5, 5.5, 6.5, 5.0)
		)
		testAoILine(
			rawData, 2,
			Map(
				createTile(new TileIndex(2, 0, 0, 4, 4), 0.0,
				           List(0.0, 4.0, 4.0, 4.0,  0.0, 0.0, 0.0, 3.0,  0.0, 0.0, 0.0, 3.0,  0.0, 0.0, 0.0, 0.0)
				),
				createTile(new TileIndex(2, 1, 0, 4, 4), 0.0,
				           List(4.0, 5.0, 4.0, 0.0,  0.0, 5.0, 0.0, 0.0,  0.0, 5.0, 0.0, 0.0,  0.0, 0.0, 0.0, 0.0)
				),
				createTile(new TileIndex(2, 0, 1, 4, 4), 0.0,
				           List(0.0, 0.0, 0.0, 0.0,  0.0, 0.0, 0.0, 3.0,  0.0, 2.0, 2.0, 3.0,  0.0, 0.0, 0.0, 3.0)
				),
				createTile(new TileIndex(2, 1, 1, 4, 4), 0.0,
				           List(0.0, 0.0, 0.0, 0.0,  0.0, 5.0, 0.0, 0.0,  2.0, 5.0, 2.0, 0.0,  0.0, 5.0, 0.0, 0.0)
				)
			),
			Map[String, String](
				"oculus.binning.value.type" -> "field",
				"oculus.binning.value.field" -> "value",
				"oculus.binning.value.aggregation" -> "max"
			)
		)
	}
}
