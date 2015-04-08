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

package com.oculusinfo.tilegen.tiling.analytics



import java.lang.{Integer => JavaInt}
import java.util.{List => JavaList}
import scala.collection.JavaConverters._
import org.scalatest.FunSuite
import org.apache.spark.SharedSparkContext
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.TileIndex
import org.json.JSONObject
import scala.util.Try



class InSituAnalyticTestSuite extends FunSuite with SharedSparkContext {
	test("Array tile analytic output") {
		val analytic = new ArrayTileAnalytic[Int](new NumericSumTileAnalytic[Int](Some("inner")), Some("outer"))
		val analyticDesc = new MonolithicAnalysisDescription(((i: Int) => Seq(i)), analytic)
		
		analyticDesc.addGlobalAccumulator(sc)
		analyticDesc.addLevelAccumulator(sc, 0)
		analyticDesc.addLevelAccumulator(sc, 1)

		analyticDesc.accumulate(new TileIndex(0, 0, 0, 4, 4), Seq(1, 0, 0, 0))
		analyticDesc.accumulate(new TileIndex(0, 0, 0, 4, 4), Seq(1, 1, 0, 0))
		analyticDesc.accumulate(new TileIndex(1, 0, 0, 4, 4), Seq(0, 1, 0, 0))
		analyticDesc.accumulate(new TileIndex(1, 1, 0, 4, 4), Seq(0, 0, 1, 0))
		analyticDesc.accumulate(new TileIndex(1, 0, 1, 4, 4), Seq(0, 0, 1, 1))
		analyticDesc.accumulate(new TileIndex(1, 1, 1, 4, 4), Seq(0, 0, 0, 1))
		val pyramid = new WebMercatorTilePyramid
		val metaData = new PyramidMetaData("name", "description", 4, 4,
		                                   pyramid.getTileScheme(),
		                                   pyramid.getProjection(),
		                                   Seq(Int.box(0), Int.box(1)).asJava,
		                                   pyramid.getTileBounds(new TileIndex(0, 0, 0)),
		                                   null, null)
		AnalysisDescription.record(analyticDesc, metaData)

		def getInnerValues (from: String): List[Int] = {
			val json = new JSONObject(from).getJSONArray("outer")
			Range(0, json.length()).map(n =>
				json.getInt(n)
			).toList
		}
		val global = getInnerValues(metaData.getCustomMetaData("global"))
		val level0= getInnerValues(metaData.getCustomMetaData("0"))
		val level1 = getInnerValues(metaData.getCustomMetaData("1"))
		assert(List(2, 2, 2, 2) === global)
		assert(List(2, 1, 0, 0) === level0)
		assert(List(0, 1, 2, 2) === level1)
	}
}
