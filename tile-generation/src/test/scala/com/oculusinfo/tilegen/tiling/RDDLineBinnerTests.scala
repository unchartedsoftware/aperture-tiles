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

import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import org.apache.avro.file.CodecFactory

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.Try

import org.scalatest.FunSuite

import org.apache.spark.SharedSparkContext
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.NumericSumBinningAnalytic
import com.oculusinfo.tilegen.util.EndPointsToLine



case class SegmentIndexScheme() extends IndexScheme[Segment] with Serializable {
	def toCartesian (segment: Segment) = (segment.p0x, segment.p0y)
	def toCartesianEndpoints (segment: Segment) = (segment.p0x, segment.p0y, segment.p1x, segment.p1y)
}

case class Segment(p0x: Double, p0y: Double, p1x: Double, p1y: Double) extends Serializable

case class SegmentData(segment: Segment, count: Double) extends Serializable


class RDDLineBinnerTestSuite extends FunSuite with SharedSparkContext {
	def wikipediaGetPoints (start: BinIndex, end: BinIndex): (Boolean, Int, Int, Int, Int) = {
		// The un-scala-like version from wikipedia
		var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())
		var steep = math.abs(y1 - y0) > math.abs(x1 - x0)

		var tmpInt = 0
		if (steep) {
			tmpInt = y0		//swap x0, y0
			y0 = x0
			x0 = tmpInt
			tmpInt = y1		//swap x1, y1
			y1 = x1
			x1 = tmpInt
		}
		if (x0 > x1) {
			tmpInt = x1		//swap x0, x1
			x1 = x0
			x0 = tmpInt
			tmpInt = y0		//swap y0, y1
			y0 = y1
			y1 = tmpInt
		}
		(steep, x0, y0, x1, y1)
	}

	/**
	 * Re-order coords of two endpoints for efficient implementation of Bresenham's line algorithm
	 */
	def getPoints (start: BinIndex, end: BinIndex): (Boolean, Int, Int, Int, Int) = {
		val xs = start.getX()
		val xe = end.getX()
		val ys = start.getY()
		val ye = end.getY()
		val steep = (math.abs(ye - ys) > math.abs(xe - xs))

		if (steep) {
			if (ys > ye) {
				(steep, ye, xe, ys, xs)
			} else {
				(steep, ys, xs, ye, xe)
			}
		} else {
			if (xs > xe) {
				(steep, xe, ye, xs, ys)
			} else {
				(steep, xs, ys, xe, ye)
			}
		}
	}



	test("Bresenham Alternatives") {
		// Make sure our scala-like version matches the unscala-like one from wikipedia.
		for (w <- 0 to 10;
		     x <- 0 to 10;
		     y <- 0 to 10;
		     z <- 0 to 10) {
			val b1 = new BinIndex(w, x)
			val b2 = new BinIndex(y, z)
			assert(getPoints(b1, b2) === wikipediaGetPoints(b1, b2))
		}
	}


	/*
	 * Tests the basic line binning case, where we create a line across our world bounds
	 * and our removal distance threshold is set such that it is rendered in its entirety.
	 */
	test("Basic line binning") {
		val pyramidId = "basic line binning"
		val tileIO = runNewLineBinning(pyramidId, 513, true)

		val tile00 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 0, 256, 256))
		assert(tile00.isDefined)
		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 256, 256))
		assert(tile10.isDefined)

		val tile01 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 1, 256, 256))
		assert(tile01.isEmpty)
		val tile11 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 1, 256, 256))
		assert(tile11.isEmpty)

		assert(tile00.get.isInstanceOf[SparseTileData[_]])
		for (x <- 0 to 255) assert(tile00.get.getBin(x, 14) == 1.0 && tile10.get.getBin(x, 14) == 1.0)
	}



	/*
	 * Tests the faded end line binning case, where we create a line across our world bounds
	 * and our removal distance threshold is set such that only the end of the line is rendered.
	 * The ends should be faded from the end point using an exponential function.
	 */
	test("Line binning with fade") {
		val pyramidId = "faded line binning"
		val tileIO = runNewLineBinning(pyramidId, 256, false)

		val tile00 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 0, 256, 256))
		assert(tile00.isDefined)
		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 256, 256))
		assert(tile10.isDefined)

		val tile01 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 1, 4, 4))
		assert(tile01.isEmpty)
		val tile11 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 1, 4, 4))
		assert(tile11.isEmpty)

		assert(tile00.get.isInstanceOf[SparseTileData[_]])
		assert(tile10.get.isInstanceOf[SparseTileData[_]])

		for (x <- 1 to 255) {
			val v0x = tile00.get.getBin(x, 14).toString.toDouble
			val v0x1 = tile00.get.getBin(x-1, 14).toString.toDouble
			assert(v0x < v0x1, "Tile 00, Bin %d, %.4f ! < %.4f".format(x, v0x, v0x1))

			val v1x = tile10.get.getBin(x, 14).toString.toDouble
			val v1x1 = tile10.get.getBin(x-1, 14).toString.toDouble
			assert(v1x > v1x1, "Tile 10, Bin %d, %.4f ! > %.4f".format(x, v1x, v1x1))
		}
	}



	/*
	 * Tests the faded end line binning case, where we create a line across our world bounds
	 * and our removal distance threshold is set such that only the end of the line is rendered.
	 */
	test("Line binning with removed line") {
		val pyramidId = "removed line binning"
		val tileIO = runNewLineBinning(pyramidId, 4, false)

		val tile00 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 0, 4, 4))
		assert(tile00.isEmpty)
		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 4, 4))
		assert(tile10.isEmpty)
		val tile01 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 1, 4, 4))
		assert(tile01.isEmpty)
		val tile11 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 1, 4, 4))
		assert(tile11.isEmpty)
	}



	/*
	 * Helper function to run line binning based on a mercator tile pyramid.
	 */
	def runLineBinning (pyramidId: String, maxLength: Int, showEnds: Boolean) = {
		// First row of data has value at the start and end.  Segment binner
		// should create a line between the two.
		val data = sc.parallelize(List(new SegmentData(new Segment(-180.0, -10.0, 180.0, -10.0), 1.0)))

		val binner = new RDDLineBinner(1, maxLength, showEnds)
		val tileIO = new TestTileIO
		val pyramid = new WebMercatorTilePyramid

		val coordFcn: SegmentData => Try[Segment] = segmentData => Try(segmentData.segment)
		val valueFcn: SegmentData => Try[Double] = segmentData => Try(segmentData.count)
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Double]] = None
		val dataAnalytics: Option[AnalysisDescription[SegmentData, Double]] = None

		val lineDrawer = new EndPointsToLine(maxLength, 256, 256)

		binner.binAndWriteData(
			data,
			coordFcn,
			valueFcn,
			new SegmentIndexScheme,
			new NumericSumBinningAnalytic[Double, JavaDouble](),
			tileAnalytics,
			dataAnalytics,
			new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
			pyramid,
			None,
			None,
			pyramidId,
			tileIO,
			List(List(1)),
			lineDrawer.endpointsToLineBins,
			xBins=256, yBins=256)
		tileIO
	}

	def runNewLineBinning (pyramidId: String, maxLength: Int, wholeLine: Boolean) = {
		// First row of data has value at the start and end.  Segment binner
		// should create a line between the two.
		val rawdata = sc.parallelize(List(new SegmentData(new Segment(-180.0, -10.0, 180.0, -10.0), 1.0)))

		val binner = new UniversalBinner
		val tileIO = new TestTileIO
		val pyramid = new WebMercatorTilePyramid

		val coordFcn: SegmentData => Try[Segment] = segmentData => Try(segmentData.segment)
		val valueFcn: SegmentData => Try[Double] = segmentData => Try(segmentData.count)
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Double]] = None
		val dataAnalytics: Option[AnalysisDescription[SegmentData, Double]] = None

		val lineDrawer = new EndPointsToLine(maxLength, 256, 256)

		val noDouble: Option[Double] = None
		val data: RDD[(Segment, Double, Option[Double])] =
			rawdata.map(segData => (coordFcn(segData), valueFcn(segData), noDouble))
				.filter(seg => seg._1.isSuccess && seg._2.isSuccess).map(seg => (seg._1.get, seg._2.get, seg._3))

		val (locateFcn, populateFcn) =
			if (wholeLine) {
				val valueScaler: (Array[BinIndex], BinIndex, Double) => Double = (endpoints, bin, value) => value

				(StandardBinningFunctions.locateLine(new SegmentIndexScheme, pyramid, Some(1), Some(maxLength))(List(1))(_),
				 StandardBinningFunctions.populateTileWithLineSegments(valueScaler)(_, _, _))
			} else {
				val valueScaler: (Array[BinIndex], BinIndex, Double) => Double = (endpoints, bin, value) => {
					val d0 = math.abs(endpoints(0).getX - bin.getX) max math.abs(endpoints(0).getY - bin.getY)
					val d1 = math.abs(endpoints(1).getX - bin.getX) max math.abs(endpoints(1).getY - bin.getY)
					val d = d0 min d1
					val scale = (1.0 - (d.toDouble / maxLength.toDouble)).max(0.0).min(1.0) // Limit to from 0 to 1.

					value*scale
				}

				(StandardBinningFunctions.locateLineLeaders(new SegmentIndexScheme, pyramid, None, maxLength)(List(1))(_),
				 StandardBinningFunctions.populateTileWithLineLeaders(maxLength, valueScaler)(_, _, _))
			}
		val tiles = binner.processData[Segment, Double, Double, Double, JavaDouble](data,
			      new NumericSumBinningAnalytic[Double, JavaDouble](), tileAnalytics, dataAnalytics,
			      locateFcn, populateFcn)

		tileIO.writeTileSet(pyramid, pyramidId, tiles,
		                    new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                    tileAnalytics, dataAnalytics, "name", "description")

		tileIO
	}
}

