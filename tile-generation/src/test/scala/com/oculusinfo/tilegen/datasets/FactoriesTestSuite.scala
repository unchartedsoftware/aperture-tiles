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


import java.sql.{Date, Timestamp}
import java.util
import java.util.Properties
import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.tilegen.tiling.analytics.{NumericMaxBinningAnalytic, NumericSumBinningAnalytic, NumericMinBinningAnalytic}
import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.spark.SharedSparkContext
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.sql.types.TimestampType
import org.scalatest.FunSuite
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer




class FactoriesTestSuite extends FunSuite with SharedSparkContext {
	test("Test CSV Reading") {
		val rawProps = new Properties()
		rawProps.setProperty("oculus.binning.source.location", "hdfs://localhost/data-location")
		rawProps.setProperty("oculus.binning.source.partitions", "13")
		rawProps.setProperty("oculus.binning.name", "sample name")
		rawProps.setProperty("oculus.binning.parsing.a.index", "0")
		rawProps.setProperty("oculus.binning.parsing.a.fieldType", "int")
		rawProps.setProperty("oculus.binning.parsing.e.index", "3")
		rawProps.setProperty("oculus.binning.parsing.e.fieldType", "double")
		rawProps.setProperty("oculus.binning.parsing.d.index", "7")
		rawProps.setProperty("oculus.binning.parsing.d.fieldType", "date")
		rawProps.setProperty("oculus.binning.parsing.d.dateFormat", "yyyy-MM-dd HH:mm:ss")
		rawProps.setProperty("oculus.binning.parsing.c.index", "5")
		rawProps.setProperty("oculus.binning.parsing.c.fieldType", "long")
		rawProps.setProperty("oculus.binning.parsing.separator", "\t")

		val data = sc.parallelize(List("1\ta\tb\t1.1\tc\t1\td\t2001-01-01 01:01:01\te",
		                               "2\tf\tg\t2.2\th\t2\ti\t2002-02-02 02:02:02\tj",
		                               "3\tk\tl\t3.3\tm\t3\tn\t2003-03-03 03:03:03\to",
		                               "4\tp\tq\t4.4\tr\t4\ts\t2004-04-04 04:04:04\tt",
		                               "5\tu\tv\t5.5\tw\t5\tx\t2005-05-05 05:05:05\ty"))
		val props = new PropertiesWrapper(rawProps)
		val reader = new CSVReader(sqlc, data, props)
		val srdd = reader.asDataFrame
		assert(4 === srdd.schema.fields.size)
		assert("a" === srdd.schema.fields(0).name)
		assert(IntegerType === srdd.schema.fields(0).dataType);
		assert("e" === srdd.schema.fields(1).name)
		assert(DoubleType === srdd.schema.fields(1).dataType)
		assert("c" === srdd.schema.fields(2).name)
		assert(LongType === srdd.schema.fields(2).dataType)
		assert("d" === srdd.schema.fields(3).name)
		assert(TimestampType === srdd.schema.fields(3).dataType)

		val sLocal = srdd.map(row => {
			                      val r = row
			                      val res = (row(0).asInstanceOf[Int],
			                                 row(1).asInstanceOf[Double],
			                                 row(2).asInstanceOf[Long],
			                                 row(3).asInstanceOf[java.util.Date])
			                      res
		                      }).collect
	}

	test("Test field aggregation construction") {
		val props = new Properties()
		props.setProperty("oculus.binning.source.location", "hdfs://localhost/data-location")
		props.setProperty("oculus.binning.source.partitions", "13")
		props.setProperty("oculus.binning.name", "sample name")
		props.setProperty("oculus.binning.parsing.a.index", "0")
		props.setProperty("oculus.binning.parsing.a.fieldType", "double")
		props.setProperty("oculus.binning.parsing.a.fieldAggregation", "meAn")
		props.setProperty("oculus.binning.parsing.separator", "\t")
		props.setProperty("oculus.binning.parsing.b.index", "1")
		props.setProperty("oculus.binning.parsing.b.fieldType", "float")
		props.setProperty("oculus.binning.parsing.c.index", "2")
		props.setProperty("oculus.binning.parsing.c.fieldType", "Median")


		// Check that the default aggregation is sum
		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.field", "a")
		props.setProperty("oculus.binning.value.valueType", "double")
		val factory1 = ValueExtractorFactory(null, util.Arrays.asList("oculus", "binning", "value"))
		factory1.readConfiguration(JsonUtilities.propertiesObjToJSON(props))
		val valext1 = factory1.produce(classOf[ValueExtractor[_, _]])
		assert(valext1.isInstanceOf[FieldValueExtractor[_, _]])
		assert(valext1.binningAnalytic.isInstanceOf[NumericSumBinningAnalytic[_, _]])

		// Check min and max aggregation
		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.field", "b")
		props.setProperty("oculus.binning.value.valueType", "float")
		props.setProperty("oculus.binning.value.aggregation", "min")
		val factory2 = ValueExtractorFactory(null, util.Arrays.asList("oculus", "binning", "value"))
		factory2.readConfiguration(JsonUtilities.propertiesObjToJSON(props))
		val valext2 = factory2.produce(classOf[ValueExtractor[_, _]])
		assert(valext2.isInstanceOf[FieldValueExtractor[_, _]])
		assert(valext2.binningAnalytic.isInstanceOf[NumericMinBinningAnalytic[_, _]])

		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.field", "b")
		props.setProperty("oculus.binning.value.valueType", "float")
		props.setProperty("oculus.binning.value.aggregation", "max")
		val factory3 = ValueExtractorFactory(null, util.Arrays.asList("oculus", "binning", "value"))
		factory3.readConfiguration(JsonUtilities.propertiesObjToJSON(props))
		val valext3 = factory3.produce(classOf[ValueExtractor[_, _]])
		assert(valext3.isInstanceOf[FieldValueExtractor[_, _]])
		assert(valext3.binningAnalytic.isInstanceOf[NumericMaxBinningAnalytic[_, _]])

		// Check mean aggregation
		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.field", "c")
		props.setProperty("oculus.binning.value.valueType", "double")
		props.setProperty("oculus.binning.value.aggregation", "mean")
		val factory4 = ValueExtractorFactory(null, util.Arrays.asList("oculus", "binning", "value"))
		factory4.readConfiguration(JsonUtilities.propertiesObjToJSON(props))
		val valext4 = factory4.produce(classOf[ValueExtractor[_, _]])
		assert(valext4.isInstanceOf[MeanValueExtractor[_]])

		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.field", "c")
		props.setProperty("oculus.binning.value.valueType", "double")
		props.setProperty("oculus.binning.value.aggregation", "average")
		val factory5 = ValueExtractorFactory(null, util.Arrays.asList("oculus", "binning", "value"))
		factory5.readConfiguration(JsonUtilities.propertiesObjToJSON(props))
		val valext5 = factory5.produce(classOf[ValueExtractor[_, _]])
		assert(valext5.isInstanceOf[MeanValueExtractor[_]])
	}

	test("Test kryo value extractors") {
		val props = new Properties()
		props.setProperty("oculus.binning.source.location", "hdfs://localhost/data-location")
		props.setProperty("oculus.binning.source.partitions", "13")
		props.setProperty("oculus.binning.name", "sample name")
		props.setProperty("oculus.binning.parsing.a.index", "0")
		props.setProperty("oculus.binning.parsing.a.fieldType", "double")
		props.setProperty("oculus.binning.parsing.a.fieldAggregation", "meAn")
		props.setProperty("oculus.binning.parsing.separator", "\t")
		props.setProperty("oculus.binning.parsing.b.index", "1")
		props.setProperty("oculus.binning.parsing.b.fieldType", "float")
		props.setProperty("oculus.binning.parsing.c.index", "2")
		props.setProperty("oculus.binning.parsing.c.fieldType", "Median")
		props.setProperty("oculus.binning.value.type", "field")
		props.setProperty("oculus.binning.value.field", "a")
		props.setProperty("oculus.binning.value.valueType", "double")
		props.setProperty("oculus.binning.value.serializer.type", "double-k")

		val factory = ValueExtractorFactory(null, util.Arrays.asList("oculus", "binning", "value"))
		factory.readConfiguration(JsonUtilities.propertiesObjToJSON(props))
		val valext = factory.produce(classOf[ValueExtractor[_, _]])

		assert(valext.isInstanceOf[FieldValueExtractor[_, _]])
		assert(valext.binningAnalytic.isInstanceOf[NumericSumBinningAnalytic[_, _]])
		assert(valext.serializer.isInstanceOf[KryoSerializer[_]])
	}
}
