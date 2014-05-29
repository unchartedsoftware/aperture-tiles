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



package com.oculusinfo.tilegen.binning



import java.io.InputStream
import java.io.IOException
import java.lang.{Iterable => JavaIterable}
import java.lang.{Integer => JavaInt}
import java.util.{List => JavaList}
import java.util.Properties

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.Pair

import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor
import com.oculusinfo.tilegen.util.Rectangle




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class LiveStaticTilePyramidIO (sc: SparkContext) extends PyramidIO {
	private val datasets = MutableMap[String, Dataset[_, _, _]]()
	private val metaData = MutableMap[String, PyramidMetaData]()


	def initializeForWrite (pyramidId: String): Unit = {
	}

	def writeTiles[T] (pyramidId: String,
	                   tilePyramid: TilePyramid,
	                   serializer: TileSerializer[T],
	                   data: JavaIterable[TileData[T]]): Unit =
		throw new IOException("Can't write raw data")

	def writeMetaData (pyramidId: String,
	                   metaData: String): Unit =
		throw new IOException("Can't write raw data")

	def initializeForRead (pyramidId: String,
	                       width: Int, height: Int,
	                       dataDescription: Properties): Unit = {
		if (!datasets.contains(pyramidId)) {
			datasets.synchronized {
				if (!datasets.contains(pyramidId)) {
					datasets(pyramidId) =
						DatasetFactory.createDataset(sc, dataDescription,
						                             false, false, true, width, height)
				}
			}
		}
	}

	/*
	 * Convert a set of tiles to testable bounds.
	 *
	 * The returned bounds are in two potential forms, paired into a 2-tuble.
	 *
	 * The first element is the bounds, in tile indices.
	 */
	private def tilesToBounds (pyramid: TilePyramid,
	                           tiles: Iterable[TileIndex]): Bounds = {
		var mutableRows = MutableList[Bounds]()
		val bounds = tiles.map(tile =>
			((tile.getX, tile.getY()),
			 new Bounds(tile.getLevel(),
			            new Rectangle[Int](tile.getX(), tile.getX(),
			                               tile.getY(), tile.getY()),
			            None))
		).toSeq.sortBy(_._1).map(_._2).foreach(bounds =>
			{
				if (mutableRows.isEmpty) {
					mutableRows += bounds
				} else {
					val last = mutableRows.last
					val combination = last union bounds
					if (combination.isEmpty) {
						mutableRows += bounds
					} else {
						mutableRows(mutableRows.size-1) = combination.get
					}
				}
			}
		)

		val rows = mutableRows.foldRight(None: Option[Bounds])((bounds, rest) =>
			Some(new Bounds(bounds.level, bounds.indexBounds, rest))
		).getOrElse(null)

		if (null == rows) {
			null
		} else {
			// reduce returns None if no reduction is required
			rows.reduce.getOrElse(rows)
		}
	}

	def readTiles[BT] (pyramidId: String,
	                   serializer: TileSerializer[BT],
	                   javaTiles: JavaIterable[TileIndex]):
			JavaList[TileData[BT]] = {
		def inner[IT: ClassTag, PT: ClassTag]: JavaList[TileData[BT]] = {
			val tiles: Iterable[TileIndex] = javaTiles.asScala

			if (!datasets.contains(pyramidId) ||
				    tiles.isEmpty) {
				null
			} else {
				val dataset = datasets(pyramidId).asInstanceOf[Dataset[IT, PT, BT]]
				val indexScheme = dataset.getIndexScheme
				val binDescriptor = dataset.getBinDescriptor
				val pyramid = dataset.getTilePyramid

				val bins = tiles.head.getXBins()
				val bounds = tilesToBounds(pyramid, tiles)

				val boundsTest = bounds.getSerializableContainmentTest(pyramid,
				                                                       bins)
				val cartesianSpreaderFcn = bounds.getSpreaderFunction[PT](pyramid, bins)
				val spreaderFcn: IT => TraversableOnce[(TileIndex, BinIndex)] =
					index => {
						val cartesianIndex = indexScheme.toCartesian(index)

						val spread = cartesianSpreaderFcn(cartesianIndex._1, cartesianIndex._2)
						spread
					}

				val binner = new RDDBinner
				binner.debug = true

				val results = dataset.transformRDD[TileData[BT]](
					rdd => {
						binner.processData[IT, PT, BT](rdd, binDescriptor,
						                               spreaderFcn, bins)
					}
				).map(tile =>
					// Get the min and max for each tile while we're still distributed
					{
						val index = tile.getDefinition()
						var max = binDescriptor.defaultMax
						var min = binDescriptor.defaultMin
						for (x <- 0 until index.getXBins())
							for (y <- 0 until index.getYBins()) {
								val value = tile.getBin(x, y)
								max = binDescriptor.max(max, value)
								min = binDescriptor.min(min, value)
							}
						(tile, min, max)
					}
				).collect

				// Update metadata for these levels
				val datasetMetaData = getMetaData(pyramidId).get

				val mins = MutableMap[Int, BT]()
				datasetMetaData.getLevelMinimums().asScala.foreach{case (level, min) =>
					mins(level) = binDescriptor.stringToBin(min)
				}

				val maxs = MutableMap[Int, BT]()
				datasetMetaData.getLevelMaximums().asScala.foreach{case (level, max) =>
					maxs(level) = binDescriptor.stringToBin(max)
				}

				results.foreach{ case (tile, min, max) =>
					{
						val level = tile.getDefinition().getLevel()
						mins(level) = binDescriptor.min(mins.getOrElse(level,
						                                               binDescriptor.defaultMin),
						                                min)
						maxs(level) = binDescriptor.max(maxs.getOrElse(level,
						                                               binDescriptor.defaultMax),
						                                max)
					}
				}

				def convertAndSort (extrema: Seq[(Int, BT)]): JavaList[Pair[JavaInt, String]] =
					extrema.map{case (a, b) =>
						new Pair[JavaInt, String](new JavaInt(a), binDescriptor.binToString(b))
					}.sortBy(_.getFirst()).asJava

				val newDatasetMetaData =
					new PyramidMetaData(datasetMetaData.getName(),
					                    datasetMetaData.getDescription(),
					                    datasetMetaData.getTileSize(),
					                    datasetMetaData.getScheme(),
					                    datasetMetaData.getProjection(),
					                    datasetMetaData.getMinZoom(),
					                    datasetMetaData.getMaxZoom(),
					                    datasetMetaData.getBounds(),
					                    convertAndSort(mins.toSeq),
					                    convertAndSort(maxs.toSeq))
				metaData(pyramidId) = newDatasetMetaData

				// Finally, return our tiles
				results.map(_._1).toList.asJava
			}
		}

		inner
	}

	def getTileStream[T] (pyramidId: String, serializer: TileSerializer[T],
	                      tile: TileIndex): InputStream = {
		null
	}

	private def getMetaData (pyramidId: String): Option[PyramidMetaData] = {
		if (!metaData.contains(pyramidId) || null == metaData(pyramidId))
			if (datasets.contains(pyramidId))
				metaData(pyramidId) = datasets(pyramidId).createMetaData(pyramidId)
		metaData.get(pyramidId)
	}

	def readMetaData (pyramidId: String): String =
		getMetaData(pyramidId).map(_.toString).getOrElse(null)

	def removeTiles (id: String, tiles: JavaIterable[TileIndex]  ) : Unit =
		throw new IOException("removeTiles not currently supported for LiveStaticTilePyramidIO")
	
}
