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



import java.lang.{Double => JavaDouble, Integer => JavaInt}

import com.oculusinfo.binning.TileData.StorageType
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.util.JSONUtilitiesTests
import com.oculusinfo.tilegen.datasets.TileAssertions
import org.apache.spark.rdd.RDD
import org.json.JSONObject

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite

import org.apache.avro.file.CodecFactory

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning._
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData

import com.oculusinfo.tilegen.tiling.analytics.{MonolithicAnalysisDescription, NumericSumTileAnalytic, AnalysisDescription, NumericSumBinningAnalytic}


class RDDBinnerTestSuite extends FunSuite with SharedSparkContext with TileAssertions {
	test("Simple binning") {
		val data = sc.parallelize(Range(0, 8)).map(n =>
			((n.toDouble, (7-n).toDouble), 1.0)
		)

		val binner = new UniversalBinner
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
		                       new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                       pyramid,
		                       None,
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
		assertTileContents(List[Double](1.0, 0.0, 0.0, 0.0,
		                                0.0, 1.0, 0.0, 0.0,
		                                0.0, 0.0, 1.0, 0.0,
		                                0.0, 0.0, 0.0, 1.0), tile01.get)
		// Only 1/4 full - should be sparse
		assert(tile01.get.isInstanceOf[SparseTileData[_]])

		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 4, 4))
		assert(tile10.isDefined)
		assertTileContents(List[Double](1.0, 0.0, 0.0, 0.0,
		                                0.0, 1.0, 0.0, 0.0,
		                                0.0, 0.0, 1.0, 0.0,
		                                0.0, 0.0, 0.0, 1.0), tile10.get)
		// Only 1/4 full - should be sparse
		assert(tile10.get.isInstanceOf[SparseTileData[_]])
	}

	test("One-dimensional binning") {
		val data = sc.parallelize(Range(0, 3)).map(n =>
			((n.toDouble, (7-n).toDouble), 1.0)
		)

		val binner = new UniversalBinner
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
		                       new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                       pyramid,
		                       None,
		                       None,
		                       pyramidId,
		                       tileIO,
		                       List(List(1)),
		                       xBins=4, yBins=1)

		// Noting that visually, the tiles should look exactly as we enter them here.
		val tile10 = tileIO.getTile(pyramidId, new TileIndex(1, 0, 0, 4, 1))
		assert(tile10.isDefined)
		assertTileContents(List[Double](1.0, 1.0, 1.0, 0.0), tile10.get)
		// 3/4 full - should be dense
		assert(tile10.get.isInstanceOf[DenseTileData[_]])

		val tile11 = tileIO.getTile(pyramidId, new TileIndex(1, 1, 0, 4, 1))
		assert(!tile11.isDefined)
	}

	test("Test tile density selection") {
		val f = false
		val t = true
		// We are going to tile on level 1
		// source represents the bins with values
		// So tile (1, 0, 0) should be exactly half full - and default to sparse
		//    tile (1, 1, 0) should be one more than half full - and default to dense
		//    tile (1, 0, 1) should be totally full - and default to dense
		//    tile (1, 1, 1) should be nearly empty - and default to sparse
		val source = List(
			List(t, t, t, t, f, f, f, f),
			List(t, t, t, t, f, f, f, f),
			List(t, t, t, t, f, f, f, f),
			List(t, t, t, t, t, f, f, f),
			List(t, f, f, f, t, f, f, f),
			List(t, t, f, f, t, t, f, f),
			List(t, t, f, f, t, t, f, f),
			List(t, t, t, f, t, t, t, t)
		)
		val rawData = source.map(_.zipWithIndex).zipWithIndex.flatMap { case (xList, y) =>
			xList.flatMap { case (full, x) => if (full) Some(((x.toDouble, 7.0-y.toDouble), 1.0)) else None}
		}
		val data = sc.parallelize(rawData)

		val binner = new UniversalBinner
		val pyramid = new AOITilePyramid(0.0, 0.0, 7.9999, 7.9999)

		val coordFcn: (((Double, Double), Double)) => Try[(Double, Double)] = record => Try(record._1)
		val valueFcn: (((Double, Double), Double)) => Try[Double] = record => Try(record._2)
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Double]] = None
		val dataAnalytics: Option[AnalysisDescription[((Double, Double), Double), Double]] = None

		// First, default heuristic for dense vs sparse
		val tileIODefault = new TestTileIO
		val autoId = "heuristic test"
		binner.binAndWriteData(data, coordFcn, valueFcn, new CartesianIndexScheme,
		                       new NumericSumBinningAnalytic[Double, JavaDouble](), tileAnalytics, dataAnalytics,
		                       new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                       pyramid, None,
		                       None,
		                       autoId, tileIODefault, List(List(1)), xBins=4, yBins=4)
		assert(tileIODefault.getTile(autoId, new TileIndex(1, 0, 0, 4, 4)).get.isInstanceOf[SparseTileData[_]])
		assert(tileIODefault.getTile(autoId, new TileIndex(1, 1, 0, 4, 4)).get.isInstanceOf[DenseTileData[_]])
		assert(tileIODefault.getTile(autoId, new TileIndex(1, 0, 1, 4, 4)).get.isInstanceOf[DenseTileData[_]])
		assert(tileIODefault.getTile(autoId, new TileIndex(1, 1, 1, 4, 4)).get.isInstanceOf[SparseTileData[_]])

		// Now try mandatory dense tiles
		val tileIODense = new TestTileIO
		val denseId = "dense test"
		binner.binAndWriteData(data, coordFcn, valueFcn, new CartesianIndexScheme,
		                       new NumericSumBinningAnalytic[Double, JavaDouble](), tileAnalytics, dataAnalytics,
		                       new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                       pyramid, None,
		                       Some(StorageType.Dense),
		                       denseId, tileIODense, List(List(1)), xBins=4, yBins=4)
		assert(tileIODense.getTile(denseId, new TileIndex(1, 0, 0, 4, 4)).get.isInstanceOf[DenseTileData[_]])
		assert(tileIODense.getTile(denseId, new TileIndex(1, 1, 0, 4, 4)).get.isInstanceOf[DenseTileData[_]])
		assert(tileIODense.getTile(denseId, new TileIndex(1, 0, 1, 4, 4)).get.isInstanceOf[DenseTileData[_]])
		assert(tileIODense.getTile(denseId, new TileIndex(1, 1, 1, 4, 4)).get.isInstanceOf[DenseTileData[_]])

		// Now try mandatory sparse tiles
		val tileIOSparse = new TestTileIO
		val sparseId = "sparse test"
		binner.binAndWriteData(data, coordFcn, valueFcn, new CartesianIndexScheme,
		                       new NumericSumBinningAnalytic[Double, JavaDouble](), tileAnalytics, dataAnalytics,
		                       new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()),
		                       pyramid, None,
		                       Some(StorageType.Sparse),
		                       sparseId, tileIOSparse, List(List(1)), xBins=4, yBins=4)
		assert(tileIOSparse.getTile(sparseId, new TileIndex(1, 0, 0, 4, 4)).get.isInstanceOf[SparseTileData[_]])
		assert(tileIOSparse.getTile(sparseId, new TileIndex(1, 1, 0, 4, 4)).get.isInstanceOf[SparseTileData[_]])
		assert(tileIOSparse.getTile(sparseId, new TileIndex(1, 0, 1, 4, 4)).get.isInstanceOf[SparseTileData[_]])
		assert(tileIOSparse.getTile(sparseId, new TileIndex(1, 1, 1, 4, 4)).get.isInstanceOf[SparseTileData[_]])
	}


	test("Test data analytics") {
		val data: RDD[((Double, Double), Int, Option[Int])] =
			sc.parallelize(List(((1.0, 1.0), 1, Some(1)),
			                    ((2.0, 2.0), 1, Some(2)),
			                    ((1.0, 1.0), 1, Some(3)),
			                    ((2.0, 2.0), 1, Some(4))
			               ))
		val binner = new UniversalBinner
		val pyramid = new AOITilePyramid(0, 0, 3.9999, 3.9999)
		val indexer = new CartesianIndexScheme

		val tileAnalytic: AnalysisDescription[Int, Int] =
			new MonolithicAnalysisDescription[Int, Int](
				n => n,
				new NumericSumTileAnalytic[Int](Some("sum"))
			)
		// Just to make sure things work properly, tile over levels 0 and 1 (for double the global sum), but don't record
		// level 1, and do try to record level 2, just to make sure the missing one isn't written, and the present one
		// has no data.
		tileAnalytic.addGlobalAccumulator(sc)
		tileAnalytic.addLevelAccumulator(sc, 0)
		tileAnalytic.addLevelAccumulator(sc, 2)

		val tiles = binner.processData(
			data,
			new NumericSumBinningAnalytic[Int, JavaInt](),
			None,
			Some(tileAnalytic),
			StandardBinningFunctions.locateIndexOverLevels(indexer, pyramid, 4, 4)(List(0, 1)),
			StandardBinningFunctions.populateTileIdentity,
			new BinningParameters(true, 4, 4)
		).collect
		JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"0": {"sum": 10}, "2": {"sum": 0}, "global": {"sum": 20}}"""),
		                                   tileAnalytic.accumulatedResults)
	}

	// Test the tiling speed of the universal binner versus the old RDDBinner.
	ignore("Test tiling speed") {
		def time (f: () => Unit): Double = {
			val start = System.nanoTime()
			f()
			val end = System.nanoTime()
			(end-start)/1000000.0
		}

		val oldBinner = new RDDBinner
		oldBinner.debug = true
		val newBinner = new UniversalBinner

		// IT, PT, option[DT]
		val intNone: Option[Int] = None
		val data = sc.parallelize(1 to 10, 10)
			.mapPartitions(i => (1 to 100000).map(n => ((math.random, math.random), math.random, intNone)).toIterator)
		data.cache
		data.count

		val index = new CartesianIndexScheme
		val analytic = new NumericSumBinningAnalytic[Double, JavaDouble]()
		val tileAnalytics: Option[AnalysisDescription[TileData[JavaDouble], Int]] = None
		val dataAnalytics: Option[AnalysisDescription[Int, Int]] = None
		val pyramid: TilePyramid = new AOITilePyramid(0.0, 0.0, 1.0, 1.0)
		val levels = Seq(0,1,2,3,4,5,6,7,8,9)

		val oldTime1 = time(() => oldBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)
		val oldTime2 = time(() => oldBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)

		val newTime1 = time(() => newBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)
		val newTime2 = time(() => newBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)
		val newTime3 = time(() => newBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)
		val newTime4 = time(() => newBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)

		val oldTime3 = time(() => oldBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)
		val oldTime4 = time(() => oldBinner.processDataByLevel(data, index, analytic, tileAnalytics, dataAnalytics, pyramid, levels).count)

		println("Old time 1: "+oldTime1)
		println("Old time 2: "+oldTime2)
		println("Old time 3: "+oldTime3)
		println("Old time 4: "+oldTime4)
		println
		println("New time 1: "+newTime1)
		println("New time 2: "+newTime2)
		println("New time 3: "+newTime3)
		println("New time 4: "+newTime4)
	}
}
