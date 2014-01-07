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



package com.oculusinfo.tilegen.datasets



import java.util.Properties

import scala.collection.mutable.MutableList

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD



import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.TileMetaData



/**
 * A Dataset encapsulates all that is needed to retrieve data for binning.
 * The goal is that Datasets can be constructed (via a DatasetFactory) from
 * simple property files, which can be passed into a binning process from any
 * place that needs raw data to be binned.
 */
abstract class Dataset[BT: ClassManifest, PT] {
  val binTypeManifest = implicitly[ClassManifest[BT]]



  /**
   * Get a name for this dataset
   */
  def getName: String

  /**
   * Get a description of this dataset
   */
  def getDescription: String

  def getLevels: Seq[Seq[Int]]

  def getTilePyramid: TilePyramid

  def getBins: Int = 256

  def getConsolidationPartitions: Option[Int] = None

  /**
   * Gets the data associated with this dataset, in a form that is ready for binning
   *
   * @param sc The Spark context in which the data should be retrieved
   * @param cache If true, the data should be cached at an appropriate point.
   *              If false, the caching state is unspecified, not necessarily
   *              uncached.
   */
  def getData (sc: SparkContext, cache: Boolean): RDD[(Double, Double, BT)]

  /**
   * Get a bin descriptor that can be used to bin this data
   */
  def getBinDescriptor: BinDescriptor[BT, PT]

  /**
   * Creates a blank metadata describing this dataset
   */
  def createMetaData (pyramidId: String): TileMetaData = {
    val tileSize = getBins
    val tilePyramid = getTilePyramid
    val fullBounds = tilePyramid.getTileBounds(new TileIndex(0, 0, 0, tileSize, tileSize))
    new TileMetaData(pyramidId,
		     getDescription,
                     tileSize,
                     tilePyramid.getTileScheme(),
                     tilePyramid.getProjection(),
                     0,
                     scala.Int.MaxValue,
                     fullBounds,
                     MutableList[(Int, String)](),
                     MutableList[(Int, String)]())
  }


}



object DatasetFactory {
  def createDataset (dataDescription: Properties,
		     tileSize: Int = 256): Dataset[_, _] =
    return new CSVDataset(dataDescription, tileSize)
}
