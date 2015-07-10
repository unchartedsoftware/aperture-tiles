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

import org.json.JSONObject

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.{Set => MutableSet}
import scala.collection.mutable.Stack
import scala.reflect.ClassTag
import scala.util.Try

import org.apache.spark.Accumulable
import org.apache.spark.AccumulableParam
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext

import grizzled.slf4j.Logging

import com.oculusinfo.binning._
import com.oculusinfo.binning.TileData.StorageType
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.tilegen.datasets.{CSVDataSource, CSVReader, TilingTask}
import com.oculusinfo.tilegen.util.PropertiesWrapper
import com.oculusinfo.tilegen.tiling.analytics.{TileAnalytic, AnalysisDescription}




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class OnDemandAccumulatorPyramidIO (sqlc: SQLContext) extends PyramidIO with Logging {
	private val sc = sqlc.sparkContext
	private val tasks = MutableMap[String, TilingTask[_, _, _, _]]()
	private val metaData = MutableMap[String, PyramidMetaData]()
	private val accStore = new AccumulatorStore

	def getTask (pyramidId: String) = tasks(pyramidId)

	// Shared in package only for testing purposes
	private [binning] def debugAccumulatorStore = accStore

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
	                   javaTiles: JavaIterable[TileIndex]): JavaList[TileData[BT]] = {
		if (!tasks.contains(pyramidId) || null == javaTiles || !javaTiles.iterator.hasNext) {
			null
		} else {
			val tiles = javaTiles.asScala.toArray
			val task = tasks(pyramidId).asInstanceOf[TilingTask[_, _, _, BT]]

			val results = readTilesAndTasks(pyramidId, serializer, tiles, task)
			updateMetaData(pyramidId, results)
			results.asJava
		}
	}

	def readTilesAndTasks[PT: ClassTag, DT: ClassTag, AT: ClassTag, BT] (
		pyramidId: String, serializer: TileSerializer[BT],
		tiles: Array[TileIndex], task: TilingTask[PT, DT, AT, BT]):
			Seq[TileData[BT]] =
	{
		val analytic = task.getBinningAnalytic
		val tileAnalytics = task.getTileAnalytics
		val dataAnalytics = task.getDataAnalytics
		val xBins = tiles(0).getXBins
		val yBins = tiles(0).getYBins



		// Get a map from tile level to a list of tiles on that level, with associated
		// accumulables.
		val levels = tiles.map(_.getLevel).toSet
		val add: (PT, PT) => PT = analytic.aggregate

		val tileData = levels.map(level =>
			(level,
			 tiles.filter(tile => level == tile.getLevel).map(tile =>
				 (tile -> accStore.reserve(sc, add, xBins, yBins))
			 ).toMap
			)
		).toMap

		// Make sure to gather appropriate global metadata
		tileAnalytics.map(analytic =>
			levels.map(level => analytic.addLevelAccumulator(sc, level))
		)
		dataAnalytics.map(analytic =>
			levels.map(level => analytic.addLevelAccumulator(sc, level))
		)

		// Make sure to gather appropriate tile metadata
		val dataAnalyticAccumulators: Option[Map[TileIndex, TileAccumulableInfo[DT]]] = dataAnalytics.map(da =>
			{
				tiles.map(index =>
					{
						(index, accStore.reserve(sc, da.analytic.aggregate, 1, 1))
					}
				).toMap
			}
		)
		val unitBin = new BinIndex(0, 0)

		// Run over data set, looking for points in those tiles at those levels
		val indexScheme = task.getIndexScheme
		val pyramid = task.getTilePyramid
		val identity: RDD[(Seq[Any], PT, Option[DT])] => RDD[(Seq[Any], PT, Option[DT])] =
			rdd => rdd
		val tileType = task.getTileType
		task.transformRDD(identity).foreach{case (index, value, analyticValue) =>
			{
				Try(indexScheme.toCartesian(index)).foreach{case (x, y) =>
					tileData.foreach{case (level, tileInfos) =>
						{
							val tile = pyramid.rootToTile(x, y, level, xBins, yBins)
							if (tileInfos.contains(tile)) {
								val bin = pyramid.rootToBin(x, y, tile)
								// update bin value
								// Can't recover from an accumulator aggregation exception (we
								// don't know what it has added in, and what it hasn't), so just
								// move on if we get one.
								Try(tileInfos(tile).accumulable += (bin, value))
								// update data analytic value
								dataAnalytics.foreach(da =>
									{
										analyticValue.foreach(dataAnalyticValue =>
											{
												val daAccumulable = dataAnalyticAccumulators.get.apply(tile).accumulable
												// Similarly to bin accumulators, data analytic accumulators really can't
												// recover from an exception, so just wrap it and move on.
												Try(daAccumulable += (unitBin, dataAnalyticValue))
											}
										)
									}
								)
							}
						}
					}
				}
			}
		}

		// We've got aggregates of each tile's data; convert to tiles.
		val results = tileData.flatMap(_._2).flatMap{case (index, data) =>
			{
				if (data.accumulable.value.isEmpty) {
					Seq[TileData[BT]]()
				} else {
					val tileData = data.accumulable.value
					val typeToUse = tileType.getOrElse(
						if (tileData.size > xBins*yBins/2) StorageType.Dense
						else StorageType.Sparse
					)
					val defaultBinValue =
						analytic.finish(analytic.defaultProcessedValue)
					val tile: TileData[BT] = typeToUse match {
						case StorageType.Dense => new DenseTileData[BT](index, defaultBinValue)
						case StorageType.Sparse => new SparseTileData[BT](index, defaultBinValue)
					}

					// Put the proper value into each bin
					tileData.foreach{case (bin, value) =>
						tile.setBin(bin.getX, bin.getY, analytic.finish(value))
					}

					// Apply data analytics
					dataAnalytics.map(da =>
						{
							val tileData = dataAnalyticAccumulators.get.apply(index).accumulable.value.get(unitBin)
							tileData.foreach(analyticValue =>
								{
									da.accumulate(index, analyticValue)
									AnalysisDescription.record(analyticValue, da, tile)
								}
							)
						}
					)

					// Apply tile analytics
					tileAnalytics.map(ta =>
						{
							// Figure out the value for this tile
							val analyticValue = ta.convert(tile)
							// Add it into any appropriate accumulators
							ta.accumulate(tile.getDefinition(), analyticValue)
							// And store it in the tile's metadata
							AnalysisDescription.record(analyticValue, ta, tile)
						}
					)

					Seq[TileData[BT]](tile)
				}
			}
		}.toSeq

		// Free up the accumulables we've used
		tileData.flatMap(_._2).foreach{case (index, data) =>
			accStore.release(data)
		}

		info("Read "+results.size+" tiles.")
		info("\t"+accStore.inUseCount+" accumulators in use")
		info("\t"+accStore.inUseData+" bins locked in in-use accumulators")
		info("\t"+accStore.availableCount+" accumulators available")
		info("\t"+accStore.availableData+" bins size locked in available accumulators")

		results
	}

	def updateMetaData[BT] (pyramidId: String, tiles: Iterable[TileData[BT]]) = {
		// Update metadata for these levels
		val taskMetaData = getMetaData(pyramidId).get
		val task = tasks(pyramidId).asInstanceOf[TilingTask[_, _, _, BT]]

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
			tiles.map(tile =>
				new JavaInt(tile.getDefinition().getLevel())
			).toSet.asJava
		)

		metaData(pyramidId) = newTaskMetaData
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
		throw new IOException("removeTiles not currently supported for OnDemandAccumulatorPyramidIO")
}


case class TileAccumulableInfo[PT: ClassTag] (
	accumulable: Accumulable[MutableMap[BinIndex, PT], (BinIndex, PT)],
	param: TileAccumulableParam[PT]
) {}

class TileAccumulableParam[PT] (private var width: Int,
                                private var height: Int,
                                private var add: (PT, PT) => PT)
		extends AccumulableParam[MutableMap[BinIndex, PT], (BinIndex, PT)]
{
	def reset (w: Int, h: Int, a: (PT, PT) => PT): Unit = {
		width = w
		height = h
		add = a
	}
	def addAccumulator (r: MutableMap[BinIndex, PT], t: (BinIndex, PT)):
			MutableMap[BinIndex, PT] = {
		val (index, value) = t
		r(index) = r.get(index).map(valueR => add(valueR, value)).getOrElse(value)
		r
	}

	def addInPlace (r1: MutableMap[BinIndex, PT], r2: MutableMap[BinIndex, PT]):
			MutableMap[BinIndex, PT] = {
		r2.foreach{case (index, value2) =>
			{
				r1(index) = r1.get(index).map(value1 => add(value1, value2)).getOrElse(value2)
			}
		}
		r1
	}
	def zero (initialValue: MutableMap[BinIndex, PT]): MutableMap[BinIndex, PT] =
		MutableMap[BinIndex, PT]()
}

class AccumulatorStore {
	// Make sure functions only work on the client thread that created the store
	private val origin = Thread.currentThread
	private val inUse = MutableMap[ClassTag[_], MutableSet[TileAccumulableInfo[_]]]()
	private val available = MutableMap[ClassTag[_], Stack[TileAccumulableInfo[_]]]()

	// debug info - number of accumulators reserved, in use, etc.
	def inUseCount = inUse.map(_._2.size).fold(0)(_ + _)
	def availableCount = available.map(_._2.size).fold(0)(_ + _)
	def totalCount = inUseCount + availableCount

	// debug info - number of bins of data in reserved, in use, etc. accumulators
	def inUseData =
		inUse.map(_._2.map(_.accumulable.value.size).fold(0)(_ + _))
			.fold(0)(_ + _)
	def availableData =
		available.map(_._2.map(_.accumulable.value.size).fold(0)(_ + _))
			.fold(0)(_ + _)
	def totalData = inUseData + availableData

	def reserve[PT] (sc: SparkContext,
	                 add: (PT, PT) => PT,
	                 xBins: Int,
	                 yBins: Int)(implicit evidence: ClassTag[PT]): TileAccumulableInfo[PT] = {
		val info: TileAccumulableInfo[PT] =
			available.synchronized {
				if (!available.contains(evidence) || available(evidence).isEmpty) {
					// None available, create a new one.
					val param = new TileAccumulableParam[PT](xBins, yBins, add)
					val accum = sc.accumulable(MutableMap[BinIndex, PT]())(param)
					new TileAccumulableInfo[PT](accum, param)(evidence)
				} else {
					val reusable = available(evidence).pop.asInstanceOf[TileAccumulableInfo[PT]]
					reusable.param.reset(xBins, yBins, add)
					reusable
				}
			}
		inUse.synchronized {
			if (!inUse.contains(evidence)) {
				inUse(evidence) = MutableSet[TileAccumulableInfo[_]]()
			}
		}
		inUse(evidence) += info
		info
	}
	def release[PT] (info: TileAccumulableInfo[PT])(implicit evidence: ClassTag[PT]): Unit = {
		inUse(evidence) -= info
		info.accumulable.setValue(info.param.zero(info.accumulable.value))
		available.synchronized {
			if (!available.contains(evidence)) {
				available(evidence) = Stack[TileAccumulableInfo[_]]()
			}
		}
		available(evidence).push(info)
	}
}
