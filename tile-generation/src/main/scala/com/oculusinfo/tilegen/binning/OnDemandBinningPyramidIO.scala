/*
 * Copyright (c) 2015 Uncharted Software Inc.
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
import org.json.JSONObject

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData

import com.oculusinfo.tilegen.datasets.{CSVReader, CSVDataSource, TilingTask}
import com.oculusinfo.tilegen.tiling.UniversalBinner
import com.oculusinfo.tilegen.util.{PropertiesWrapper, Rectangle}
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.StandardBinningFunctions
import com.oculusinfo.tilegen.tiling.BinningParameters




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class OnDemandBinningPyramidIO (sqlc: SQLContext) extends PyramidIO {
	private val sc = sqlc.sparkContext
	private val tasks = MutableMap[String, TilingTask[_, _, _, _]]()
	private val metaData = MutableMap[String, PyramidMetaData]()
	private var consolidationPartitions: Option[Int] = Some(1)
	def eliminateConsolidationPartitions: Unit =
		consolidationPartitions = None
	def setConsolidationPartitions (partitions: Int): Unit =
		consolidationPartitions = Some(partitions)
	def getConsolidationPartitions = consolidationPartitions

	def getTask (pyramidId: String) = tasks(pyramidId)

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
		if (!tasks.contains(pyramidId)) {
			tasks.synchronized {
				if (!tasks.contains(pyramidId)) {
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
					// Register it as a table
					val table = TilingTask.rectifyTableName("table "+pyramidId)
					reader.asDataFrame.registerTempTable(table)
					if (cache) sqlc.cacheTable(table)

					// Create our tiling task
					val newTask = TilingTask(sqlc, table, dataDescription)
					newTask.getTileAnalytics.map(_.addGlobalAccumulator(sc))
					newTask.getDataAnalytics.map(_.addGlobalAccumulator(sc))
					tasks(pyramidId) = newTask
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

	/**
	 * Direct programatic initialization.
	 *
	 * Temporary route until we get full pipeline configuration
	 */
	def initializeDirectly (pyramidId: String, task: TilingTask[_, _, _, _]): Unit ={
		if (!tasks.contains(pyramidId)) {
			tasks.synchronized {
				if (!tasks.contains(pyramidId)) {
					task.getTileAnalytics.map(_.addGlobalAccumulator(sc))
					task.getDataAnalytics.map(_.addGlobalAccumulator(sc))
					tasks(pyramidId) = task
				}
			}
		}
	}

  def readTiles[BT] (pyramidId: String,
                     serializer: TileSerializer[BT],
                     javaTiles: JavaIterable[TileIndex],
                     properties: JSONObject): JavaList[TileData[BT]] = {
    readTiles( pyramidId, serializer, javaTiles )
  }

	def readTiles[BT] (pyramidId: String,
	                   serializer: TileSerializer[BT],
	                   javaTiles: JavaIterable[TileIndex]):
			JavaList[TileData[BT]] = {
		def inner[PT: ClassTag, DT: ClassTag, AT: ClassTag]: JavaList[TileData[BT]] = {
			// Note that all tiles given _must_ have the same dimensions.
			if (!tasks.contains(pyramidId) || null == javaTiles || !javaTiles.iterator.hasNext) {
				null
			} else {
				val tiles: Iterable[TileIndex] = javaTiles.asScala
				val task = tasks(pyramidId).asInstanceOf[TilingTask[PT, DT, AT, BT]]
				val indexScheme = task.getIndexScheme
				val binningAnalytic = task.getBinningAnalytic
				val pyramid = task.getTilePyramid

				val xBins = tiles.head.getXBins()
				val yBins = tiles.head.getXBins()
				val bounds = tilesToBounds(pyramid, tiles)
				val levels = tiles.map(_.getLevel).toSet

				// Make sure to gather appropriate metadata
				task.getTileAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)
				task.getDataAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)

				val boundsTest = bounds.getSerializableContainmentTest(pyramid, xBins, yBins)
				val cartesianSpreaderFcn = bounds.getSpreaderFunction[PT](pyramid, xBins, yBins)
				val locaterFcn: Seq[Any] => Traversable[(TileIndex, Array[BinIndex])] =
					index => {
						val cartesianIndex = indexScheme.toCartesian(index)

						val spread = cartesianSpreaderFcn(cartesianIndex._1, cartesianIndex._2)
						spread.map(r => (r._1, Array(r._2)))
					}

				val binner = new UniversalBinner

				val results: Array[TileData[BT]] = task.transformRDD[TileData[BT]](
					rdd => {
						binner.processData[Seq[Any], PT, AT, DT, BT](rdd,
						                                             binningAnalytic,
						                                             task.getTileAnalytics,
						                                             task.getDataAnalytics,
						                                             locaterFcn,
						                                             StandardBinningFunctions.populateTileIdentity,
						                                             new BinningParameters(tileType = task.getTileType,
						                                                                   maxPartitions = consolidationPartitions))
					}
				).collect

				// Update metadata for these levels
				val taskMetaData = getMetaData(pyramidId).get

				val newTaskMetaData =
					new PyramidMetaData(taskMetaData.getName(),
					                    taskMetaData.getDescription(),
					                    taskMetaData.getTileSizeX(),
					                    taskMetaData.getTileSizeY(),
					                    taskMetaData.getScheme(),
					                    taskMetaData.getProjection(),
					                    taskMetaData.getValidZoomLevels(),
					                    taskMetaData.getBounds(),
					                    null, null)
				task.getTileAnalytics.map(AnalysisDescription.record(_, newTaskMetaData))
				task.getDataAnalytics.map(AnalysisDescription.record(_, newTaskMetaData))
				newTaskMetaData.addValidZoomLevels(
					results.map(tile =>
						new JavaInt(tile.getDefinition().getLevel())
					).toSet.asJava
				)

				metaData(pyramidId) = newTaskMetaData

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
			if (tasks.contains(pyramidId))
				metaData(pyramidId) = tasks(pyramidId).createMetaData(pyramidId)
		metaData.get(pyramidId)
	}

	def readMetaData (pyramidId: String): String =
		getMetaData(pyramidId).map(_.toString).getOrElse(null)

	def removeTiles (id: String, tiles: JavaIterable[TileIndex]  ) : Unit =
		throw new IOException("removeTiles not currently supported for OnDemandBinningPyramidIO")

}
