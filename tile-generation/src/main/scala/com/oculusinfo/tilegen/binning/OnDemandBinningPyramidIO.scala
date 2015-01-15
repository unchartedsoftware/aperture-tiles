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



import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.lang.{Iterable => JavaIterable}
import java.lang.{Integer => JavaInt}
import java.util.{List => JavaList}
import java.util.Properties

import org.apache.spark.sql.SQLContext

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.Pair

import com.oculusinfo.tilegen.datasets._
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.util.{PropertiesWrapper, Rectangle}




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class OnDemandBinningPyramidIO (sqlc: SQLContext) extends PyramidIO {
	private val sc = sqlc.sparkContext
	private val datasets = MutableMap[String, Dataset[_, _, _, _, _]]()
	private val metaData = MutableMap[String, PyramidMetaData]()
	private var consolidationPartitions: Option[Int] = Some(1)
	def eliminateConsolidationPartitions: Unit =
		consolidationPartitions = None
	def setConsolidationPartitions (partitions: Int): Unit =
		consolidationPartitions = Some(partitions)
	def getConsolidationPartitions = consolidationPartitions

	def getDataset (pyramidId: String) = datasets(pyramidId)
	
	def initializeForWrite (pyramidId: String): Unit = {
	}

	def writeTiles[T] (pyramidId: String,
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
					// Note our tile width and height appropriately
					dataDescription.setProperty("oculus.binning.tileWidth", width.toString)
					dataDescription.setProperty("oculus.binning.tileHeight", height.toString)

					// Read our data
					val wrappedDesc = new PropertiesWrapper(dataDescription)
					// Read our CSV data
					val source = new CSVDataSource(wrappedDesc)
					// Read the CSV into a schema file
					val reader = new CSVReader(sqlc, source.getData(sc), wrappedDesc)
					// Unless the user has specifically said not to, cache processed data so as to make multiple runs
					// more efficient.
					val cache = wrappedDesc.getBoolean(
						"oculus.binning.caching.processed",
						"Cache the data, in a parsed and processed form, if true",
						Some(true))
					if (cache) reader.asSchemaRDD.cache()
					// Register it as a table
					val table = TilingTask.rectifyTableName("table "+pyramidId)
					reader.asSchemaRDD.registerTempTable(table)
					// Create our tiling task
					val newTask = TilingTask(sqlc, table, dataDescription)
					newTask.getTileAnalytics.map(_.addGlobalAccumulator(sc))
					newTask.getDataAnalytics.map(_.addGlobalAccumulator(sc))
					datasets(pyramidId) = newTask
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
		def inner[IT: ClassTag, PT: ClassTag, DT: ClassTag, AT: ClassTag]: JavaList[TileData[BT]] = {
			// Note that all tiles given _must_ have the same dimensions.
			if (!datasets.contains(pyramidId) || null == javaTiles || !javaTiles.iterator.hasNext) {
				null
			} else {
				val tiles: Iterable[TileIndex] = javaTiles.asScala
				val dataset = datasets(pyramidId).asInstanceOf[Dataset[IT, PT, DT, AT, BT]]
				val indexScheme = dataset.getIndexScheme
				val binningAnalytic = dataset.getBinningAnalytic
				val pyramid = dataset.getTilePyramid

				val xBins = tiles.head.getXBins()
				val yBins = tiles.head.getXBins()
				val bounds = tilesToBounds(pyramid, tiles)
				val levels = tiles.map(_.getLevel).toSet

				// Make sure to gather appropriate metadata
				dataset.getTileAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)
				dataset.getDataAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)

				val boundsTest = bounds.getSerializableContainmentTest(pyramid, xBins, yBins)
				val cartesianSpreaderFcn = bounds.getSpreaderFunction[PT](pyramid, xBins, yBins)
				val spreaderFcn: IT => TraversableOnce[(TileIndex, BinIndex)] =
					index => {
						val cartesianIndex = indexScheme.toCartesian(index)

						val spread = cartesianSpreaderFcn(cartesianIndex._1, cartesianIndex._2)
						spread
					}

				val binner = new RDDBinner
				binner.debug = true

				val results: Array[TileData[BT]] = dataset.transformRDD[TileData[BT]](
					rdd => {
						binner.processData[IT, PT, AT, DT, BT](rdd,
						                                       binningAnalytic,
						                                       dataset.getTileAnalytics,
						                                       dataset.getDataAnalytics,
						                                       spreaderFcn,
						                                       consolidationPartitions)
					}
				).collect

				// Update metadata for these levels
				val datasetMetaData = getMetaData(pyramidId).get

				val newDatasetMetaData =
					new PyramidMetaData(datasetMetaData.getName(),
					                    datasetMetaData.getDescription(),
					                    datasetMetaData.getTileSizeX(),
					                    datasetMetaData.getTileSizeY(),
					                    datasetMetaData.getScheme(),
					                    datasetMetaData.getProjection(),
					                    datasetMetaData.getValidZoomLevels(),
					                    datasetMetaData.getBounds(),
					                    null, null)
				dataset.getTileAnalytics.map(_.applyTo(newDatasetMetaData))
				dataset.getDataAnalytics.map(_.applyTo(newDatasetMetaData))
				newDatasetMetaData.addValidZoomLevels(
					results.map(tile =>
						new JavaInt(tile.getDefinition().getLevel())
					).toSet.asJava
				)

				metaData(pyramidId) = newDatasetMetaData

				// Finally, return our tiles
				results.toList.asJava
			}
		}

		inner
	}

	def getTileStream[BT] (pyramidId: String, serializer: TileSerializer[BT],
	                       tile: TileIndex): InputStream = {
		val results: JavaList[TileData[BT]] =
			readTiles(pyramidId, serializer, List[TileIndex](tile).asJava)

		if (null == results || 0 == results.size || null == results.get(0)) {
			null
		} else {
			val bos = new ByteArrayOutputStream;
			serializer.serialize(results.get(0), bos);
			bos.flush
			bos.close
			new ByteArrayInputStream(bos.toByteArray)
		}
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
		throw new IOException("removeTiles not currently supported for OnDemandBinningPyramidIO")
	
}
