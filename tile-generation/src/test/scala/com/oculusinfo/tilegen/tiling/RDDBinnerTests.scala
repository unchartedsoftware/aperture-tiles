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

package com.oculusinfo.tilegen.tiling



import java.lang.{Double => JavaDouble}

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite

import org.apache.avro.file.CodecFactory

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.{BinIndex, TileData, TileIndex}
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer

import com.oculusinfo.tilegen.datasets.CountValueExtractor
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.NumericSumBinningAnalytic



class RDDBinnerTestSuite extends FunSuite with SharedSparkContext {
	test("Simple binning") {
		val data = sc.parallelize(Range(0, 8)).map(n =>
			((n.toDouble, (7-n).toDouble), 1.0)
		)

		val binner = new RDDBinner
		val tileIO = new TestTileIO
		val pyramid = new AOITilePyramid(0.0, 0.0, 7.9999, 7.9999)
		val pyramidId = "simple test"

		val coordFcn: (((Double, Double), Double)) => Try[(Double, Double)] = record => Try(record._1)
		val valueFcn: (((Double, Double), Double)) => Try[Double] = record => Try(record._2)
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Double]] = None
		val dataAnalytics: Option[AnalysisDescription[((Double, Double), Double), Double]] = None

		binner.binAndWriteData(data,
		                       coordFcn,
		                       valueFcn,
		                       new CartesianIndexScheme,
		                       new NumericSumBinningAnalytic[Double, JavaDouble](),
		                       tileAnalytics,
		                       dataAnalytics,
		                       new CountValueExtractor(),
		                       pyramid,
		                       None,
		                       pyramidId,
		                       tileIO,
		                       List(List(1)),
		                       xBins=4, yBins=4)

		val tile00 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 0, 4, 4))
		assert(tile00.isEmpty)
		val tile11 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 1, 4, 4))
		assert(tile11.isEmpty)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile01 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 1, 4, 4))
		assert(tile01.isDefined)
		assert(tile01.get.getData.asScala.map(_.toString.toDouble) ===
			       List[Double](1.0, 0.0, 0.0, 0.0,
			                    0.0, 1.0, 0.0, 0.0,
			                    0.0, 0.0, 1.0, 0.0,
			                    0.0, 0.0, 0.0, 1.0))
		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 4, 4))
		assert(tile10.isDefined)
		assert(tile10.get.getData.asScala.map(_.toString.toDouble) ===
			       List[Double](1.0, 0.0, 0.0, 0.0,
			                    0.0, 1.0, 0.0, 0.0,
			                    0.0, 0.0, 1.0, 0.0,
			                    0.0, 0.0, 0.0, 1.0))
	}

	test("One-dimensional binning") {
		val data = sc.parallelize(Range(0, 3)).map(n =>
			((n.toDouble, (7-n).toDouble), 1.0)
		)

		val binner = new RDDBinner
		val tileIO = new TestTileIO
		val pyramid = new AOITilePyramid(0.0, 0.0, 7.9999, 7.9999)
		val pyramidId = "1-d test"

		val coordFcn: (((Double, Double), Double)) => Try[(Double, Double)] = record => Try(record._1)
		val valueFcn: (((Double, Double), Double)) => Try[Double] = record => Try(record._2)
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Double]] = None
		val dataAnalytics: Option[AnalysisDescription[((Double, Double), Double), Double]] = None

		binner.binAndWriteData(data,
		                       coordFcn,
		                       valueFcn,
		                       new DensityStripIndexScheme,
		                       new NumericSumBinningAnalytic[Double, JavaDouble](),
		                       tileAnalytics,
		                       dataAnalytics,
		                       new CountValueExtractor(),
		                       pyramid,
		                       None,
		                       pyramidId,
		                       tileIO,
		                       List(List(1)),
		                       xBins=4, yBins=1)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 0, 4, 1))
		assert(tile10.isDefined)
		assert(tile10.get.getData.asScala.map(_.toString.toDouble) ===
			       List[Double](1.0, 1.0, 1.0, 0.0))
		val tile11 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 4, 1))
		assert(!tile11.isDefined)
	}
}
