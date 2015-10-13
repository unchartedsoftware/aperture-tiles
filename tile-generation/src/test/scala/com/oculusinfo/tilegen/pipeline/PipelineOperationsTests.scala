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
package com.oculusinfo.tilegen.pipeline



import java.io.File
import java.lang.{Double => JavaDouble}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.{Calendar, GregorianCalendar, Date}

import org.apache.hadoop.util.Shell

import scala.collection.JavaConverters._

import com.oculusinfo.binning.{BinIndex, TileIndex}
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.impl.{FileSystemPyramidSource, FileBasedPyramidIO}
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.JSONUtilitiesTests
import com.oculusinfo.tilegen.datasets.{TileAssertions, SchemaTypeUtilities}
import com.oculusinfo.tilegen.tiling.LocalTileIO
import org.apache.avro.file.CodecFactory
import org.apache.spark.SharedSparkContext
import org.apache.spark.sql.Column
import org.apache.spark.sql.types._
import org.json.JSONObject
import org.scalatest.FunSuite

import scala.collection.mutable.ListBuffer



class PipelineOperationsTests extends FunSuite with SharedSparkContext with TileAssertions {
	import PipelineOperations._
	import PipelineOperationsParsing._

	def outputOps(colSpecs: List[String], output: ListBuffer[Any])(input: PipelineData) = {
		val extractors = colSpecs.map(SchemaTypeUtilities.calculateExtractor(_, input.srdd.schema))
		val results = input.srdd.collect().map(row => extractors.map(_(row)))
		output ++= results.toList.flatten
		input
	}

	def outputOp(colSpec: String, output: ListBuffer[Any])(input: PipelineData) = {
		outputOps(List(colSpec), output)(input)
	}

	def convertColumnOp (colSpec: String, targetType: DataType)(input: PipelineData) = {
		val withConvert = input.srdd.withColumnRenamed(colSpec, colSpec + "Old").withColumn(colSpec, new Column(colSpec + "Old").cast(targetType))
		val lessOld = withConvert.select(withConvert.columns.filter(_ != (colSpec+"Old")).map(new Column(_)):_*)

		PipelineData(input.sqlContext, lessOld)
	}

	test("Test load JSON data parse and operation") {
		val resultList = ListBuffer[Any]()

		val resPath = getClass.getResource("/json_test.data").toURI.getPath
		val argsMap = Map("ops.path" -> resPath, "ops.partitions" -> "1")

		val loadStage = new PipelineStage("load", parseLoadJsonDataOp(argsMap))
		loadStage.addChild(new PipelineStage("output", outputOps(List("val", "time"), resultList)(_)))
		PipelineTree.execute(loadStage, sqlc)

		assertResult(List(
			             "one", "2015-01-01 10:15:30",
			             "two", "2015-01-02 8:15:30",
			             "three", "2015-01-03 10:15:30",
			             "four", "2015-01-04 8:15:30"))(resultList.toList)
	}

	test("Test load CSV data parse and operation") {
		val resultList = ListBuffer[Any]()

		val resPath = getClass.getResource("/csv_test.data").toURI.getPath
		val argsMap = Map(
			"ops.path" -> resPath,
			"ops.partitions" -> "1",
      "ops.errorLog" -> "stdout",
			"oculus.binning.parsing.separator" -> " *, *",
			"oculus.binning.parsing.vAl.index" -> "0",
			"oculus.binning.parsing.vAl.fieldType" -> "string", // use mixed case fieldname to test case sensitivity
			"oculus.binning.parsing.num.index" -> "1",
			"oculus.binning.parsing.num.fieldType" -> "long",
			"oculus.binning.parsing.num_1.index" -> "2",
			"oculus.binning.parsing.num_1.fieldType" -> "double",
			"oculus.binning.parsing.time.index" -> "3",
			"oculus.binning.parsing.time.fieldType" -> "string",
			"oculus.binning.parsing.desc.index" -> "4",
			"oculus.binning.parsing.desc.fieldType" -> "string"
		)

		val loadStage = PipelineStage("load", parseLoadCsvDataOp(argsMap))
		loadStage.addChild(PipelineStage("output", outputOps(List("vAl", "time"), resultList)(_)))
		PipelineTree.execute(loadStage, sqlc)

		assertResult(List(
			             "one", "2015-01-01 10:15:30",
			             "two", "2015-01-02 8:15:30",
			             "three", "2015-01-03 10:15:30",
			             "four", "2015-01-04 8:15:30"))(resultList.toList)
	}

	test("Test cache operation") {
		def checkTableName(count: Int, clearTableName: Boolean)(input: PipelineData) = {
			assertResult(Some(s"cached_table_$count"))(input.tableName)
			if (clearTableName) PipelineData(input.sqlContext, input.srdd) else input
		}

		val argMap = Map("ops.path" -> getClass.getResource("/json_test.data").toURI.getPath)
		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("cache_op_1", parseCacheDataOp(Map.empty)(_)))
			.addChild(PipelineStage("check_cache_op_1", checkTableName(0, true)(_)))
			.addChild(PipelineStage("cache_op_2", parseCacheDataOp(Map.empty)(_)))
			.addChild(PipelineStage("check_cache_op_2", checkTableName(1, false)(_)))
			.addChild(PipelineStage("cache_op_3", parseCacheDataOp(Map.empty)(_)))
			.addChild(PipelineStage("check_cache_op_3", checkTableName(1, false)(_)))
		PipelineTree.execute(rootStage, sqlc)
	}

	test("Test date filter parse and operation") {
		val resultList = ListBuffer[Any]()
		val argMap = Map(
			"ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
			"ops.column" -> "time",
			"ops.start" -> "2015-01-01 15:15:30",
			"ops.end" -> "2015-01-02 10:15:30",
			"ops.format" -> "yyyy-MM-DD HH:mm:ss")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("date_filter", parseDateFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("time", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List("2015-01-02 8:15:30"))(resultList.toList)
	}

	test("Test date filter operation") {
		val resultList = ListBuffer[Any]()
		val resPath = getClass.getResource("/csv_test.data").toURI.getPath
		val argsMap = Map(
			"ops.path" -> resPath,
			"ops.partitions" -> "1",
			"oculus.binning.parsing.separator" -> " *, *",
			"oculus.binning.parsing.time.index" -> "3",
			"oculus.binning.parsing.time.fieldType" -> "date",
			"oculus.binning.parsing.time.dateFormat" -> "yyyy-MM-DD HH:mm:ss"
		)

		val d = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss")
		val min = d.parse("2015-01-01 15:15:30")
		val max = d.parse("2015-01-02 10:15:30")

		val rootStage = PipelineStage("load", parseLoadCsvDataOp(argsMap))
		rootStage.addChild(PipelineStage("date_filter", dateFilterOp(min, max, "time")(_)))
			.addChild(PipelineStage("output", outputOp("time", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List(d.parse("2015-01-02 3:15:30").getTime)) (resultList.map(t => t.asInstanceOf[Timestamp].getTime).toList)
	}

	test("Test mercator filter parse and operation") {
		val resultList = ListBuffer[Any]()
		val pyramid = new WebMercatorTilePyramid
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		// Mercator projection is ok for all X; just bad for Y out of range.
		val rawData = List(SchemaTypeUtilities.row("a", 0.0, 0.0),
		                   SchemaTypeUtilities.row("b", 0.0, bounds.getMinY),
		                   SchemaTypeUtilities.row("c", 0.0, bounds.getMinY-1E-12),
		                   SchemaTypeUtilities.row("d", 0.0, bounds.getMaxY),
		                   SchemaTypeUtilities.row("e", 0.0, bounds.getMaxY-1E-12),
		                   SchemaTypeUtilities.row("f", -181.0, 0.0),
		                   SchemaTypeUtilities.row("g", 181.0, 0.0))
		val data = sqlc.createDataFrame(
			sc.parallelize(rawData),
			SchemaTypeUtilities.structSchema(SchemaTypeUtilities.schemaField("id", classOf[String]),
			                                 SchemaTypeUtilities.schemaField("lon", classOf[Double]),
			                                 SchemaTypeUtilities.schemaField("lat", classOf[Double])))

		val rootStage = PipelineStage("load", loadRDDOp(data))
		rootStage.addChild(PipelineStage("mercator filter", parseMercatorFilterOp(Map("ops.latitude" -> "lat"))))
			.addChild(PipelineStage("output", outputOp("id", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List("a", "b", "e", "f", "g"))(resultList.toList)
	}

	test("Test integral range filter parse and operation") {
		val resultList = ListBuffer[Any]()
		val argMap = Map(
			"ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
			"ops.columns" -> "num,num_1",
			"ops.min" -> "2,2",
			"ops.max" -> "3,3")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("range_filter", parseIntegralRangeFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List(2, 3))(resultList.toList)
	}


	test("Test integral range filter parse and operation with multiple types") {
		val resultList = ListBuffer[Any]()
		val argMap = Map(
			"ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
			"ops.columns" -> "num,num_1",
			"ops.min" -> "2,2",
			"ops.max" -> "3,3")

		{
			resultList.clear()
			val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
			val stage1 = rootStage.addChild(PipelineStage("convert", convertColumnOp("num", IntegerType)(_)))
			val stage2 = stage1.addChild(PipelineStage("range_filter", parseIntegralRangeFilterOp(argMap)))
			val stage3 = stage2.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

			PipelineTree.execute(rootStage, sqlc)

			assertResult(List(2, 3))(resultList.toList)
		}

		{
			resultList.clear()
			val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
			val stage1 = rootStage.addChild(PipelineStage("convert", convertColumnOp("num", LongType)(_)))
			val stage2 = stage1.addChild(PipelineStage("range_filter", parseIntegralRangeFilterOp(argMap)))
			val stage3 = stage2.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

			PipelineTree.execute(rootStage, sqlc)

			assertResult(List(2, 3))(resultList.toList)
		}
	}

	test("Test integral range filter parse and operation with exclusions") {
		val resultList = ListBuffer[Any]()
		val argMap = Map(
			"ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
			"ops.columns" -> "num,num_1",
			"ops.min" -> "2,2",
			"ops.max" -> "3,3",
			"ops.exclude" -> "true")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("range_filter", parseIntegralRangeFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List(1, 4))(resultList.toList)
	}

	test("Test fractional range filter parse and operation") {
		val resultList = ListBuffer[Any]()

		val argMap = Map(
			"ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
			"ops.columns" -> "num,num_1",
			"ops.min" -> "2.0,2.0",
			"ops.max" -> "3.0,3.0")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("range_filter", parseFractionalRangeFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List(2, 3))(resultList.toList)
	}

	test("Test fractional range filter parse and operation with multiple types") {
		val resultList = ListBuffer[Any]()
		val argMap = Map(
			"ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
			"ops.columns" -> "num,num_1",
			"ops.min" -> "2.0,2.0",
			"ops.max" -> "3.0,3.0")

		{
			resultList.clear()
			val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
			val stage1 = rootStage.addChild(PipelineStage("convert", convertColumnOp("num", FloatType)(_)))
			val stage2 = stage1.addChild(PipelineStage("range_filter", parseFractionalRangeFilterOp(argMap)))
			val stage3 = stage2.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

			PipelineTree.execute(rootStage, sqlc)

			assertResult(List(2, 3))(resultList.toList)
		}

		{
			resultList.clear()
			val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
			val stage1 = rootStage.addChild(PipelineStage("convert", convertColumnOp("num", DoubleType)(_)))
			val stage2 = stage1.addChild(PipelineStage("range_filter", parseFractionalRangeFilterOp(argMap)))
			val stage3 = stage2.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

			PipelineTree.execute(rootStage, sqlc)

			assertResult(List(2, 3))(resultList.toList)
		}
	}

	test("Test fractional range filter parse and operation with exclusions") {
		val resultList = ListBuffer[Any]()

		val resPath = getClass.getResource("/json_test.data").toURI.getPath
		val argMap = Map(
			"ops.path" -> resPath,
			"ops.columns" -> "num,num_1",
			"ops.min" -> "2.0,2.0",
			"ops.max" -> "3.0,3.0",
			"ops.exclude" -> "true")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("range_filter", parseFractionalRangeFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("num", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List(1, 4))(resultList.toList)
	}

	test("Test regex filter parse and operation") {
		val resultList = ListBuffer[Any]()

		val resPath = getClass.getResource("/json_test.data").toURI.getPath
		val argMap = Map(
			"ops.path" -> resPath,
			"ops.column" -> "desc",
			"ops.exclude" -> "false",
			"ops.regex" -> """a+b+\d*\..*\w\w""")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("regex_filter", parseRegexFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("desc", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List("aabb.?cc", "ab99.?xx", "ab66.?xx"))(resultList.toList)
	}

	test("Test regex filter parse and operation with exclude") {
		val resultList = ListBuffer[Any]()

		val resPath = getClass.getResource("/json_test.data").toURI.getPath
		val argMap = Map(
			"ops.path" -> resPath,
			"ops.column" -> "desc",
			"ops.exclude" -> "true",
			"ops.regex" -> """a+b+\d*\..*\w\w""")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("regex_filter", parseRegexFilterOp(argMap)))
			.addChild(PipelineStage("output", outputOp("desc", resultList)(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(List("aabcc.?"))(resultList.toList)
	}

	test("Test column select parse and operation") {
		var schema: StructType = null
		def schemaOp()(input: PipelineData) = {
			schema = input.srdd.schema
			input
		}

		val resPath = getClass.getResource("/json_test.data").toURI.getPath
		val argMap = Map(
			"ops.path" -> resPath,
			"ops.columns" -> "val,num")

		val rootStage = PipelineStage("load", parseLoadJsonDataOp(argMap))
		rootStage.addChild(PipelineStage("column_select", parseColumnSelectOp(argMap)))
			.addChild(PipelineStage("output", schemaOp()(_)))

		PipelineTree.execute(rootStage, sqlc)

		assertResult(schema.fields.size)(2)
		assert(schema.fieldNames.contains("val"))
		assert(schema.fieldNames.contains("num"))
	}

  test("Test column type conversion") {
    var schema: StructType = null
    def schemaOp()(input: PipelineData) = {
      schema = input.srdd.schema
      input
    }
    val resultListInt = ListBuffer[Any]()
    val resultListString = ListBuffer[Any]()

    try {
      // pipeline stage to create test data
      def createDataOp(count: Int)(input: PipelineData) = {
        val jsonData = for (x <- 0 until count; y <- 0 until count / 2) yield {
          val lon = -180.0 + (x / count.toFloat * 360.0)
          val lat = -45.0 + (y * 90.0 / (count / 2))
          var data = (x * count + y).toDouble
          if (x == 0 && y == 0){data = Double.MaxValue}
          if (x == 0 && y == 1){data = data + 0.5}

          s"""{"x":$lon, "y":$lat, "data":$data}\n"""
        }
        val srdd = sqlc.jsonRDD(sc.parallelize(jsonData))
        PipelineData(sqlc, srdd)
      }

      // Run the tile job
      val args = Map(
        "ops.xColumn" -> "x",
        "ops.yColumn" -> "y",
        "ops.name" -> "test.{i}.{v}",
        "ops.description" -> "a test description",
        "ops.prefix" -> "test_prefix",
        "ops.levels.0" -> "0",
        "ops.tileWidth" -> "4",
        "ops.tileHeight" -> "4",
        "ops.valueColumn" -> "data",
        "ops.valueType" -> "double",
        "ops.aggregationType" -> "sum")

      val rootStage = PipelineStage("create_data", createDataOp(8)(_))
      rootStage.addChild(PipelineStage("convert_int", convertColumnTypeOp("data", x => x(0).asInstanceOf[Double].toInt, IntegerType)(_)))
        .addChild(PipelineStage("output_schema", schemaOp()(_)))
        .addChild(PipelineStage("output_int", outputOp("data", resultListInt)(_)))
        .addChild(PipelineStage("convert_string", convertColumnTypeOp("data", x => x(0).asInstanceOf[Int].toString, StringType)(_)))
        .addChild(PipelineStage("output_string", outputOp("data", resultListString)(_)))
      PipelineTree.execute(rootStage, sqlc)

      // Check schema
      assertResult(schema.fields.size)(3)
      assert(schema.fields(2).name == "data")
      assert(schema.fields(2).dataType == IntegerType)

      // Double to Int
      assert(resultListInt(0) == Integer.MAX_VALUE) // Double to int conversion is capped at max int
      assert(resultListInt(1) == 1) // Double to int conversion takes only the integer part of the number
      assert(resultListInt(2) == 2)

      // Int to String
      assert(resultListString(3) == "3")

    }
  }

	test("Test geo heatmap parse and operation") {

		try {
			// pipeline stage to create test data
			def createDataOp(count: Int)(input: PipelineData) = {
				val jsonData = for (x <- 0 until count; y <- 0 until count/2) yield {
					val lon = -180.0 + (x / count.toFloat * 360.0)
					val lat = -45.0 + (y  * 90.0 / (count / 2))
					s"""{"x":$lon, "y":$lat, "data":${(x * count + y).toDouble}}\n"""
				}
				val srdd = sqlc.jsonRDD(sc.parallelize(jsonData))
				PipelineData(sqlc, srdd)
			}

			// Run the tile job
			val args = Map(
				"ops.xColumn" -> "x",
				"ops.yColumn" -> "y",
				"ops.name" -> "test.{i}.{v}",
				"ops.description" -> "a test description",
				"ops.prefix" -> "test_prefix",
				"ops.levels.0" -> "0",
				"ops.tileWidth" -> "4",
				"ops.tileHeight" -> "4",
				"ops.valueColumn" -> "data",
				"ops.valueType" -> "double",
				"ops.aggregationType" -> "sum")

			val rootStage = PipelineStage("create_data", createDataOp(8)(_))
			rootStage.addChild(PipelineStage("geo_heatmap_op", parseGeoHeatMapOp(args)))
			PipelineTree.execute(rootStage, sqlc)

			// Load the metadata and validate its contents - gives us an indication of whether or not the
			// job completed successfully, and if performed the expected operation.  There are more detailed
			// tests for the operations themselves.
			val tileIO = new LocalTileIO("avro")
			val metaData = tileIO.readMetaData("test.x.y.data").getOrElse(fail("Metadata not created"))

			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":0, "maximum":218}"""),
			                                   new JSONObject(metaData.getCustomMetaData("0").toString))
			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":0, "maximum":218}"""),
			                                   new JSONObject(metaData.getCustomMetaData("global").toString))

			val customMeta = metaData.getAllCustomMetaData
			assert(0 === customMeta.get("0.minimum"))
			assert(218 === customMeta.get("0.maximum"))
			assert(0 === customMeta.get("global.minimum"))
			assert(218 === customMeta.get("global.maximum"))
		} finally {
			// Remove the tile set we created
			def removeRecursively (file: File): Unit = {
				if (file.isDirectory) {
					file.listFiles().foreach(removeRecursively)
				}
				file.delete()
			}
			// If you want to look at the tile set (not remove it) comment out this line.
			removeRecursively(new File("test.x.y.data"))
		}
	}

	test("Test crossplot heatmap parse and operation") {

		try {
			// pipeline stage to create test data
			def createDataOp(count: Int)(input: PipelineData) = {
				val jsonData = for (x <- 0 until count; y <- 0 until count if y % 2 == 0) yield {
					s"""{"x":$x, "y":$y, "data":${(x * count + y)}}\n"""
				}
				val srdd = sqlc.jsonRDD(sc.parallelize(jsonData))
				PipelineData(sqlc, srdd)
			}

			// Run the tile job
			val args = Map(
				"ops.xColumn" -> "x",
				"ops.yColumn" -> "y",
				"ops.name" -> "test.{i}.{v}",
				"ops.description" -> "a test description",
				"ops.prefix" -> "test_prefix",
				"ops.levels.0" -> "0",
				"ops.tileWidth" -> "4",
				"ops.tileHeight" -> "4",
				"ops.valueColumn" -> "data",
				"ops.valueType" -> "long",
				"ops.aggregationType" -> "sum",
				"ops.minX" -> "0.0",
				"ops.minY" -> "0.0",
				"ops.maxX" -> "7.0",
				"ops.maxY" -> "7.0")

			val rootStage = PipelineStage("create_data", createDataOp(8)(_))
			rootStage.addChild(PipelineStage("crossplot_heatmap_op", parseCrossplotHeatmapOp(args)))
			PipelineTree.execute(rootStage, sqlc)

			// Load the metadata and validate its contents - gives us an indication of whether or not the
			// job completed successfully, and if performed the expected operation.  There are more detailed
			// tests for the operations themselves.
			val tileIO = new LocalTileIO("avro")
			val metaData = tileIO.readMetaData("test.x.y.data").getOrElse(fail("Metadata not created"))

			val bounds = metaData.getBounds
			assertResult(0.0)(bounds.getMinX)
			assertResult(0.0)(bounds.getMinY)
			assertResult(7.0)(round(bounds.getMaxX, 1))
			assertResult(6.0)(round(bounds.getMaxY, 1))

			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":8, "maximum":116}"""),
			                                   new JSONObject(metaData.getCustomMetaData("0").toString))
			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":8, "maximum":116}"""),
			                                   new JSONObject(metaData.getCustomMetaData("global").toString))

			val customMeta = metaData.getAllCustomMetaData
			assert(8 === customMeta.get("0.minimum"))
			assert(116 === customMeta.get("0.maximum"))
			assert(8 === customMeta.get("global.minimum"))
			assert(116 === customMeta.get("global.maximum"))
		} finally {
			// Remove the tile set we created
			def removeRecursively (file: File): Unit = {
				if (file.isDirectory) {
					file.listFiles().foreach(removeRecursively)
				}
				file.delete()
			}
			// If you want to look at the tile set (not remove it) comment out this line.
			removeRecursively(new File("test.x.y.data"))
		}
	}

	def round(d: Any, places: Int) = {
		BigDecimal(d.asInstanceOf[Double]).setScale(places, BigDecimal.RoundingMode.HALF_UP).toDouble
	}

	test("Test geo line-tiling parse and operation") {
		try {
			// pipeline stage to create test data
			def createDataOp(count: Int)(input: PipelineData) = {
				// Total pattern is:
				// +------------------------+
				// |                        |
				// |    4     3  2     1    |
				// |       4  3  2  1       |
				// |    5  5        0  0    |
				// |    6  6       11 11    |
				// |       7  8  9 10       |
				// |    7     8  9    10    |
				// |                        |
				// +------------------------+
				// Line format is: (t1x, t1y, b1x, b1y, t2x, t2y, b2x, b2y, v)
				val coords = List(
					(1, 1, 1, 3, 1, 1, 2, 3, 0),
					(1, 1, 1, 2, 1, 1, 2, 1, 1),
					(1, 1, 0, 2, 1, 1, 0, 1, 2),
					(0, 1, 3, 2, 0, 1, 3, 1, 3),
					(0, 1, 2, 2, 0, 1, 1, 1, 4),
					(0, 1, 2, 3, 0, 1, 1, 3, 5),
					(0, 0, 2, 0, 0, 0, 1, 0, 6),
					(0, 0, 2, 1, 0, 0, 1, 2, 7),
					(0, 0, 3, 1, 0, 0, 3, 2, 8),
					(1, 0, 0, 1, 1, 0, 0, 2, 9),
					(1, 0, 1, 1, 1, 0, 2, 2, 10),
					(1, 0, 1, 0, 1, 0, 2, 0, 11)
				)
				val pyramid = new WebMercatorTilePyramid
				val jsonData = coords.map{case (t1x, t1y, b1x, b1y, t2x, t2y, b2x, b2y, v) =>
					val t1 = new TileIndex(1, t1x, t1y, 4, 4)
					val b1 = new BinIndex(b1x, b1y)
					val cell1 = pyramid.getBinBounds(t1, b1)
					val x1 = cell1.getCenterX
					val y1 = cell1.getCenterY

					val t2 = new TileIndex(1, t2x, t2y, 4, 4)
					val b2 = new BinIndex(b2x, b2y)
					val cell2 = pyramid.getBinBounds(t2, b2)
					val x2 = cell2.getCenterX
					val y2 = cell2.getCenterY

					val value = v.toDouble
					s"""{"x1": $x1, "y1": $y1, "x2": $x2, "y2": $y2, "data": $value}"""
				}

				val srdd = sqlc.jsonRDD(sc.parallelize(jsonData))
				PipelineData(sqlc, srdd)
			}

			// Run the tile job
			val args = Map(
				"ops.x1Column" -> "x1",
				"ops.y1Column" -> "y1",
				"ops.x2Column" -> "x2",
				"ops.y2Column" -> "y2",
				"ops.name" -> "test.{i}.{v}",
				"ops.description" -> "a test description",
				"ops.prefix" -> "test_prefix",
				"ops.levels.0" -> "0,1",
				"ops.tileWidth" -> "4",
				"ops.tileHeight" -> "4",
				"ops.valueColumn" -> "data",
				"ops.valueType" -> "double",
				"ops.aggregationType" -> "sum",
				"ops.minimumSegmentLength" -> "0"
			)

			val rootStage = PipelineStage("create_data", createDataOp(8)(_))
			rootStage.addChild(PipelineStage("geo_line_tiling_op", parseGeoSegmentTilingOp(args)))
			PipelineTree.execute(rootStage, sqlc)

			// Load the metadata and validate its contents - gives us an indication of whether or not the
			// job completed successfully, and if performed the expected operation.  There are more detailed
			// tests for the operations themselves.
			val pyramidIO = new FileBasedPyramidIO(new FileSystemPyramidSource("", "avro"))
			val metaData = new PyramidMetaData(pyramidIO.readMetaData("test.x1.y1.x2.y2.data"))
			val serializer = new PrimitiveAvroSerializer[JavaDouble](classOf[JavaDouble], CodecFactory.bzip2Codec())
			val tile000 = pyramidIO.readTiles("test.x1.y1.x2.y2.data", serializer, Iterable(new TileIndex(0, 0, 0, 4, 4)).asJava)
			val tile100 = pyramidIO.readTiles("test.x1.y1.x2.y2.data", serializer, Iterable(new TileIndex(1, 0, 0, 4, 4)).asJava)
			val tile101 = pyramidIO.readTiles("test.x1.y1.x2.y2.data", serializer, Iterable(new TileIndex(1, 0, 1, 4, 4)).asJava)
			val tile110 = pyramidIO.readTiles("test.x1.y1.x2.y2.data", serializer, Iterable(new TileIndex(1, 1, 0, 4, 4)).asJava)
			val tile111 = pyramidIO.readTiles("test.x1.y1.x2.y2.data", serializer, Iterable(new TileIndex(1, 1, 1, 4, 4)).asJava)

			assertTileContents(List( 4.0,  3.0,  2.0,  1.0,
			                         5.0, 12.0,  3.0,  0.0,
			                         6.0, 21.0, 30.0, 11.0,
			                         7.0,  8.0,  9.0, 10.0), tile000.get(0))
			assertTileContents(List( 0.0,  6.0,  6.0,  0.0,
			                         0.0,  0.0,  7.0,  8.0,
			                         0.0,  7.0,  0.0,  8.0,
			                         0.0,  0.0,  0.0,  0.0), tile100.get(0))
			assertTileContents(List( 0.0, 11.0, 11.0,  0.0,
			                         9.0, 10.0,  0.0,  0.0,
			                         9.0,  0.0, 10.0,  0.0,
			                         0.0,  0.0,  0.0,  0.0), tile110.get(0))
			assertTileContents(List( 0.0,  0.0,  0.0,  0.0,
			                         0.0,  4.0,  0.0,  3.0,
			                         0.0,  0.0,  4.0,  3.0,
			                         0.0,  5.0,  5.0,  0.0), tile101.get(0))
			assertTileContents(List( 0.0,  0.0,  0.0,  0.0,
			                         2.0,  0.0,  1.0,  0.0,
			                         2.0,  1.0,  0.0,  0.0,
			                         0.0,  0.0,  0.0,  0.0), tile111.get(0))

			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":0, "maximum":30}"""),
			                                   new JSONObject(metaData.getCustomMetaData("0").toString))
			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":0, "maximum":11}"""),
			                                   new JSONObject(metaData.getCustomMetaData("1").toString))
			JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"minimum":0, "maximum":30}"""),
			                                   new JSONObject(metaData.getCustomMetaData("global").toString))

			val customMeta = metaData.getAllCustomMetaData
			assert(0 === customMeta.get("0.minimum"))
			assert(30 === customMeta.get("0.maximum"))
			assert(0 === customMeta.get("1.minimum"))
			assert(11 === customMeta.get("1.maximum"))
			assert(0 === customMeta.get("global.minimum"))
			assert(30 === customMeta.get("global.maximum"))
		} finally {
			// Remove the tile set we created
			def removeRecursively (file: File): Unit = {
				if (file.isDirectory) {
					file.listFiles().foreach(removeRecursively)
				}
				file.delete()
			}
			// If you want to look at the tile set (not remove it) comment out this line.
			removeRecursively(new File("test.x1.y1.x2.y2.data"))
		}
	}

  test("Test date field extraction") {
    val format = "yyyy DD"
    def createDataOp(count: Int)(input: PipelineData) = {
      val formatter = new SimpleDateFormat(format)
      val pyramid = new WebMercatorTilePyramid
      val jsonData = (1 to 365).map{day =>
        val date = new GregorianCalendar()
        date.set(Calendar.YEAR, 2000)
        date.set(Calendar.DAY_OF_YEAR, day)
        val formattedDate = formatter.format(date.getTime)
        s"""{"x": $day, "y": 1, "date": "$formattedDate"}"""
      }

      val srdd = sqlc.jsonRDD(sc.parallelize(jsonData))
      PipelineData(sqlc, srdd)
    }

    val rootStage = PipelineStage("create_data", createDataOp(8)(_))
    val days = ListBuffer[Any]()
    val weeks = ListBuffer[Any]()
    val months = ListBuffer[Any]()
    rootStage.addChild(PipelineStage("ConvertDates", parseDateOp("date", "parsedDate", format)(_)))
      .addChild(PipelineStage("GetDay", dateFieldOp("parsedDate", "day", Calendar.DAY_OF_YEAR)(_)))
      .addChild(PipelineStage("GetDay", dateFieldOp("parsedDate", "week", Calendar.WEEK_OF_YEAR)(_)))
      .addChild(PipelineStage("GetDay", dateFieldOp("parsedDate", "month", Calendar.MONTH)(_)))
      .addChild(new PipelineStage("output", outputOps(List("day"), days)(_)))
      .addChild(new PipelineStage("output", outputOps(List("week"), weeks)(_)))
      .addChild(new PipelineStage("output", outputOps(List("month"), months)(_)))
    PipelineTree.execute(rootStage, sqlc)

    assert((1 to 365).toList === days)
    // 2000 has 53 weeks - only one day in the first week.
    assert((1 to 53).toList === weeks.map(_.asInstanceOf[Int]).toSet.toList.sorted)
    // months are zero-based for some reason
    assert((0 to 11).toList === months.map(_.asInstanceOf[Int]).toSet.toList.sorted)
  }

  test("Test elapsed date field extraction") {
    val format = "yyyy DD"
    def createDataOp(count: Int)(input: PipelineData) = {
      val formatter = new SimpleDateFormat(format)
      val pyramid = new WebMercatorTilePyramid
      val jsonData = (1 to 365).map{day =>
        val date = new GregorianCalendar()
        date.set(Calendar.YEAR, 2000)
        date.set(Calendar.DAY_OF_YEAR, day)
        val formattedDate = formatter.format(date.getTime)
        s"""{"x": $day, "y": 1, "date": "$formattedDate"}"""
      }

      val srdd = sqlc.jsonRDD(sc.parallelize(jsonData))
      PipelineData(sqlc, srdd)
    }

    val rootStage = PipelineStage("create_data", createDataOp(8)(_))
    val days = ListBuffer[Any]()
    val weeks = ListBuffer[Any]()
    val months = ListBuffer[Any]()
    rootStage.addChild(PipelineStage("ConvertDates", parseDateOp("date", "parsedDate", format)(_)))
      .addChild(PipelineStage("GetDay", elapsedDateOp("parsedDate", "day", Calendar.DAY_OF_YEAR,
                                                      new GregorianCalendar(2000, 0, 5).getTime)(_)))
      .addChild(PipelineStage("GetDay", elapsedDateOp("parsedDate", "week", Calendar.WEEK_OF_YEAR,
                                                      new GregorianCalendar(2000, 0, 15).getTime)(_)))
      .addChild(PipelineStage("GetDay", elapsedDateOp("parsedDate", "month", Calendar.MONTH,
                                                      new GregorianCalendar(2000, 2, 1).getTime)(_)))
      .addChild(new PipelineStage("output", outputOps(List("day"), days)(_)))
      .addChild(new PipelineStage("output", outputOps(List("week"), weeks)(_)))
      .addChild(new PipelineStage("output", outputOps(List("month"), months)(_)))
    PipelineTree.execute(rootStage, sqlc)

    assert((-4 to 360).toList === days)
    // 2000 has 53 weeks - only one day in the first week.
    assert((-2 to 50).toList === weeks.map(_.asInstanceOf[Int]).toSet.toList.sorted)
    // months are zero-based for some reason
    assert((-2 to 9).toList === months.map(_.asInstanceOf[Int]).toSet.toList.sorted)
  }

  // This test is going to fail on windows if hadoop isn't properly installed. therefore, test to see if we're on
  // windows and that hadoop is properly installed (actually, let hadoop do those tests, and piggy-back on the
  // results), and if not, skip this test
  test("Test Load Parquet File") {
    if (!Shell.WINDOWS || null != Shell.WINUTILS) {
      // not on windows, or on windows with a properly installed hadoop - test can proceed.
      def SaveParquetDataOp(path: String)(input: PipelineData): PipelineData = {
        input.srdd.saveAsParquetFile(path)
        PipelineData(input.sqlContext, input.srdd)
      }

      val resultList = ListBuffer[Any]()
      val argMap = Map(
        "ops.path" -> getClass.getResource("/json_test.data").toURI.getPath,
        "ops.columns" -> "num,num_1",
        "ops.min" -> "2,2",
        "ops.max" -> "3,3")

      val tempFolder = "test.parquet.data"
      try
      {
        val rootStage = PipelineStage("load_json", parseLoadJsonDataOp(argMap))
        rootStage.addChild(PipelineStage("save_parquet", SaveParquetDataOp(tempFolder)))
          .addChild(PipelineStage("load_parquet", loadParquetDataOp(tempFolder, Some(10))))
          .addChild(PipelineStage("output", outputOp("num", resultList)(_)))

        PipelineTree.execute(rootStage, sqlc)

        assertResult(List(1, 2, 3))(resultList.toList)
      } finally {
        // Remove the tile set we created
        def removeRecursively(file: File): Unit = {
          if (file.isDirectory) {
            file.listFiles().foreach(removeRecursively)
          }
          file.delete()
        }
        // If you want to look at the tile set (not remove it) comment out this line.
        removeRecursively(new File(tempFolder))
      }
    }
  }
}
