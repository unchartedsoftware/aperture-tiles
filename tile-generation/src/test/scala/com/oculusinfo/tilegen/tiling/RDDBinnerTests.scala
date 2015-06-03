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

import com.oculusinfo.binning.TileData.StorageType
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.tilegen.datasets.TileAssertions

import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite

import org.apache.avro.file.CodecFactory

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning._
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData

import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.NumericSumBinningAnalytic



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
}
