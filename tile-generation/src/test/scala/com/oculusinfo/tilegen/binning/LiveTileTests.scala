/**
 * Copyright (c) 2013 Oculus Info Inc.
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
 
package com.oculusinfo.tilegen.binning



import java.lang.{Double => JavaDouble}

import scala.collection.JavaConverters._

import org.scalatest.FunSuite

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex

import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor



class LiveTileTestSuite extends FunSuite with SharedSparkContext {
  test("Simple binning") {
    val data = sc.parallelize(Range(0, 8)).map(n =>
      (n.toDouble, (7-n).toDouble, 1.0)
    )

    val pyramid = new AOITilePyramid(0.0, 0.0, 7.9999, 7.9999)
    val pyramidId = "simple test"
    val pyramidIo = new LiveStaticTilePyramidIO(sc)
    pyramidIo.initializeForRead(pyramidId, pyramid, 4, data, new StandardDoubleBinDescriptor)

    val tile00 = pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 0, 0, 4, 4)).asJava)
    assert(tile00.isEmpty)
    val tile11 = pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 1, 1, 4, 4)).asJava)
    assert(tile11.isEmpty)

    // Noting that visually, the tiles should look exactly as we enter them here.
    val tile01: TileData[_] =
      pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 0, 1, 4, 4)).asJava).get(0)
    assert(tile01.getDefinition.getXBins() === 4)
    assert(tile01.getDefinition.getYBins() === 4)
    assert(tile01.getData.asScala.map(_.toString.toDouble) ==
	   List[Double](1.0, 0.0, 0.0, 0.0,
			0.0, 1.0, 0.0, 0.0,
			0.0, 0.0, 1.0, 0.0,
			0.0, 0.0, 0.0, 1.0))
    val tile10: TileData[_] =
	pyramidIo.readTiles(pyramidId, null, List(new TileIndex(1, 1, 0, 4, 4)).asJava).get(0)
    assert(tile10.getDefinition.getXBins() === 4)
    assert(tile10.getDefinition.getYBins() === 4)
    assert(tile10.getData.asScala.map(_.toString.toDouble) ==
	   List[Double](1.0, 0.0, 0.0, 0.0,
			0.0, 1.0, 0.0, 0.0,
			0.0, 0.0, 1.0, 0.0,
			0.0, 0.0, 0.0, 1.0))
  }
}
