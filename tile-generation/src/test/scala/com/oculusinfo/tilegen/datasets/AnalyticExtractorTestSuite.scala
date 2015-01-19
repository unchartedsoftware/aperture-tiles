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
 * FITNESS FOR A`IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.tilegen.datasets



import java.lang.{Double => JavaDouble}
import java.util
import java.util.Properties

import com.oculusinfo.binning.{TileData, TileIndex}
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.tilegen.tiling.analytics._
import org.apache.spark.SparkContext
import org.json.JSONObject
import org.scalatest.FunSuite

import scala.reflect.ClassTag


/**
 * Created by nkronenfeld on 1/9/2015.
 */
class AnalyticExtractorTestSuite extends FunSuite {
	test("Data analytic construction") {
		val config = new JSONObject(
			""+
				"{\n"+
				"   \"analytics\": {\n"+
				"       \"data\": [\n"+
				"           {\n"+
				"		        \"analytic\": \""+classOf[MaxDataAnalytic].getName+"\",\n"+
				"               \"fields\": [\"a\"]\n"+
				"           },\n"+
				"           {\n"+
				"               \"analytic\": \""+classOf[MinDataAnalytic].getName+"\",\n"+
				"               \"fields\": [\"b\"]\n"+
				"           },\n"+
				"           {\n"+
				"               \"analytic\": \""+classOf[ExtremaDataAnalytic].getName+"\",\n"+
				"               \"fields\": [\"c\", \"d\"]\n"+
				"           }\n"+
				"       ],\n"+
				"       \"tile\": [\n"+
				"           {\n"+
				"		        \"analytic\": \""+classOf[MaxTileAnalytic].getName+"\"\n"+
				"           },\n"+
				"           {\n"+
				"               \"analytic\": \""+classOf[MinTileAnalytic].getName+"\"\n"+
				"           },\n"+
				"           {\n"+
				"               \"analytic\": \""+classOf[ExtremaTileAnalytic].getName+"\"\n"+
				"           }\n"+
				"       ]\n"+
				"   }\n"+
				"}"
		)

		val factory = new AnalyticExtractorFactory(null, util.Arrays.asList("analytics"))
		factory.readConfiguration(config)
		val extractor = factory.produce(classOf[AnalyticExtractor])

		assert(List("a", "b", "c", "d") === extractor.fields.toList)

		val dataAnalytics = extractor.dataAnalytics.analysis.get.asInstanceOf[CompositeAnalysisDescription[_, _, _]]
		assert(3 === dataAnalytics.countComponents)
		def checkDataAnalytic[T] (analytic: Any, expected: Class[T]): Unit = {
			assert(analytic.isInstanceOf[DataAnalyticWrapper[_]])
			assert(expected.isInstance(analytic.asInstanceOf[DataAnalyticWrapper[_]].base))
		}
		checkDataAnalytic(dataAnalytics.getComponent(0), classOf[MaxDataAnalytic])
		checkDataAnalytic(dataAnalytics.getComponent(1), classOf[MinDataAnalytic])
		checkDataAnalytic(dataAnalytics.getComponent(2), classOf[ExtremaDataAnalytic])

		val tileAnalytics = extractor.tileAnalytics(Seq()).analysis.get.asInstanceOf[CompositeAnalysisDescription[_, _, _]]
		assert(3 === tileAnalytics.countComponents)
		assert(tileAnalytics.getComponent(0).isInstanceOf[MaxTileAnalytic])
		assert(tileAnalytics.getComponent(1).isInstanceOf[MinTileAnalytic])
		assert(tileAnalytics.getComponent(2).isInstanceOf[ExtremaTileAnalytic])
	}
}

class MaxDataAnalytic
		extends MonolithicAnalysisDescription[Seq[Any], Double](s => s(0).asInstanceOf[Double],
		                                                        new NumericMaxTileAnalytic[Double]())
{
}

class MinDataAnalytic
		extends MonolithicAnalysisDescription[Seq[Any], Double](s => s(0).asInstanceOf[Double],
		                                                        new NumericMinTileAnalytic[Double]())
{
}

class ExtremaDataAnalytic
		extends CompositeAnalysisDescription[Seq[Any], Double, Double](
	new MonolithicAnalysisDescription[Seq[Any], Double](s => s(0).asInstanceOf[Double],
	                                                    new NumericMinTileAnalytic[Double]()),
	new MonolithicAnalysisDescription[Seq[Any], Double](s => s(1).asInstanceOf[Double],
	                                                    new NumericMaxTileAnalytic[Double]())
)
{
	// This will be wrapped, so we don't need ot override component introspection
}

class MaxTileAnalytic
		extends AnalysisDescriptionTileWrapper[JavaDouble, Double](_.doubleValue, new NumericMaxTileAnalytic[Double]()) {}

class MinTileAnalytic
		extends AnalysisDescriptionTileWrapper[JavaDouble, Double](_.doubleValue, new NumericMaxTileAnalytic[Double]()) {}

class ExtremaTileAnalytic
		extends AnalysisDescriptionTileWrapper[JavaDouble, (Double, Double)](
	d => (d.doubleValue(), d.doubleValue()),
	new ComposedTileAnalytic[Double, Double](new NumericMinTileAnalytic[Double](),
	                                         new NumericMaxTileAnalytic[Double]())
)
{
}
