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
import scala.util.Try
	import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.{BinIndex, TileData, TileIndex}
import com.oculusinfo.binning.impl.{AOITilePyramid, WebMercatorTilePyramid, SparseTileData}
import com.oculusinfo.tilegen.tiling.analytics.{NumericSumBinningAnalytic, AnalysisDescription}
import com.oculusinfo.tilegen.util.EndPointsToLine
import org.apache.avro.file.CodecFactory
import org.apache.spark.SharedSparkContext
import org.apache.spark.rdd.RDD
import org.scalatest.FunSuite
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext



/**
 * Test class to test line binning performance.  All tests generally ignored, this suite
 * is designed for use with a profiler.
 *
 * Various helper classes are borrowed from RDDLineBinnerTests.
 */
object RDDLineBinnerPerformanceTests {
	/*
	 * Provide a basic but dense line tiling job to profile.
	 */
	def main (args: Array[String]): Unit = {
		val conf = new SparkConf(false)

		// Make sure to allow multiple contexts for testing
		conf.set("spark.driver.allowMultipleContexts", "true")

		val sc = new SparkContext("local", "test", conf)
		val sqlc = new SQLContext(sc)

		val pyramidId = "basic line binning"
		val minLength = 4
		val maxLength = 1024
		val binner = new UniversalBinner
		val tileIO = new TestTileIO
		val pyramid = new AOITilePyramid(-1.0, -1.0, 1.0, 1.0)
		val levels = List(10, 11, 12)

		// Create enough raw data to provide a suitably length line binning job.
		val steps = 5000
		def getPoint (n: Int): (Double, Double) = {
			val nn = (n + steps) % steps
			val angle = math.Pi * 2.0 / steps * n
			(math.cos(angle), math.sin(angle))
		}
		val forwardSteps = 100
		val localRawData = (1 to steps).flatMap { n1 =>
			val pt1 = getPoint(n1)
			                  (1 to forwardSteps).map { n2 =>
				                  val pt2 = getPoint(n1 + n2)
				                  new SegmentData(new Segment(pt1._1, pt1._2, pt2._1, pt2._2), 1.0)
			                  }
		}

		val rawData = sc.parallelize(localRawData)


		val coordFcn: SegmentData => Try[Segment] = segmentData => Try(segmentData.segment)
		val valueFcn: SegmentData => Try[Double] = segmentData => Try(segmentData.count)
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Double]] = None
		val dataAnalytics: Option[AnalysisDescription[SegmentData, Double]] = None

		val noDouble: Option[Double] = None
		val data: RDD[(Segment, Double, Option[Double])] =
			rawData.map(segData => (coordFcn(segData), valueFcn(segData), noDouble))
				.filter(seg => seg._1.isSuccess && seg._2.isSuccess).map(seg => (seg._1.get, seg._2.get, seg._3))

		val (locateFcn, populateFcn) = {
			val valueScaler: (Array[BinIndex], BinIndex, Double) => Double = (endpoints, bin, value) => value

			(StandardBinningFunctions.locateLine(new SegmentIndexScheme, pyramid, Some(minLength), Some(maxLength))(levels)(_),
			 StandardBinningFunctions.populateTileWithLineSegments(valueScaler)(_, _, _))
		}

		val startTime = System.currentTimeMillis

		val tiles = binner.processData[Segment, Double, Double, Double, JavaDouble](data,
			      new NumericSumBinningAnalytic[Double, JavaDouble](), tileAnalytics, dataAnalytics,
			      locateFcn, populateFcn)

		tileIO.writeTileSet(pyramid, pyramidId, tiles,
		                    new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                    tileAnalytics, dataAnalytics, "name", "description")

		val endTime = System.currentTimeMillis()
		println("Elapsed time: "+((endTime-startTime)/1000.0)+" seconds")
	}
}
