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

import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.spark.sql.SQLContext

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Set => MutableSet}
import scala.collection.mutable.Stack
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import org.apache.spark.Accumulable
import org.apache.spark.AccumulableParam
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.tilegen.datasets._
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import org.apache.log4j.Level




/**
 * This class reads and caches a data set for live queries of its tiles
 */
class OnDemandAccumulatorPyramidIO (sqlc: SQLContext) extends PyramidIO {
	private val sc = sqlc.sparkContext
	private val datasets = MutableMap[String, Dataset[_, _, _, _, _]]()
	private val metaData = MutableMap[String, PyramidMetaData]()
	private val accStore = new AccumulatorStore

	def getDataset (pyramidId: String) = datasets(pyramidId)

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

	def initializeDirectly (pyramidId: String, dataset: Dataset[_, _, _, _, _]): Unit ={
		if (!datasets.contains(pyramidId)) {
			datasets.synchronized {
				if (!datasets.contains(pyramidId)) {
					dataset.getTileAnalytics.map(_.addGlobalAccumulator(sc))
					dataset.getDataAnalytics.map(_.addGlobalAccumulator(sc))
					datasets(pyramidId) = dataset
				}
			}
		}
	}

	def readTiles[BT] (pyramidId: String,
	                   serializer: TileSerializer[BT],
	                   javaTiles: JavaIterable[TileIndex]): JavaList[TileData[BT]] = {
		if (!datasets.contains(pyramidId) || null == javaTiles || !javaTiles.iterator.hasNext) {
			null
		} else {
			val tiles = javaTiles.asScala.toArray
			val dataset = datasets(pyramidId).asInstanceOf[Dataset[_, _, _, _, BT]]

			val results = readTilesAndDataset(pyramidId, serializer, tiles, dataset)
			updateMetaData(pyramidId, results)
			results.asJava
		}
	}

	def readTilesAndDataset[IT: ClassTag, PT: ClassTag, DT: ClassTag, AT: ClassTag, BT] (
		pyramidId: String, serializer: TileSerializer[BT],
		tiles: Array[TileIndex], dataset: Dataset[IT, PT, DT, AT, BT]):
			Seq[TileData[BT]] =
	{
		val analytic = dataset.getBinningAnalytic
		val tileAnalytics = dataset.getTileAnalytics
		val dataAnalytics = dataset.getDataAnalytics.asInstanceOf[Option[AnalysisDescription[(IT, PT), DT]]]
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
		val indexScheme = dataset.getIndexScheme
		val pyramid = dataset.getTilePyramid
		val identity: RDD[(IT, PT, Option[DT])] => RDD[(IT, PT, Option[DT])] =
			rdd => rdd
		dataset.transformRDD(identity).foreach{case (index, value, analyticValue) =>
			{
				val (x, y) = indexScheme.toCartesian(index)

				tileData.foreach{case (level, tileInfos) =>
					{
						val tile = pyramid.rootToTile(x, y, level, xBins, yBins)
						if (tileInfos.contains(tile)) {
							val bin = pyramid.rootToBin(x, y, tile)
							// update bin value
							tileInfos(tile).accumulable += (bin, value)
							// update data analytic value
							dataAnalytics.foreach(da =>
								{
									val dataAnalyticValue = da.convert((index, value))
									val daAccumulable = dataAnalyticAccumulators.get.apply(tile).accumulable
									daAccumulable += (unitBin, dataAnalyticValue)
								}
							)
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
					val tile = new TileData[BT](index)

					// Put the proper default in all bins
					val defaultBinValue =
						analytic.finish(analytic.defaultProcessedValue)
					for (x <- 0 until xBins) {
						for (y <- 0 until yBins) {
							tile.setBin(x, y, defaultBinValue)
						}
					}

					// Put the proper value into each bin
					data.accumulable.value.foreach{case (bin, value) =>
						tile.setBin(bin.getX, bin.getY, analytic.finish(value))
					}

					// Apply data analytics
					dataAnalytics.map(da =>
						{
							val tileData = dataAnalyticAccumulators.get.apply(index).accumulable.value.get(unitBin)
							tileData.foreach(analyticValue =>
								{
									da.accumulate(index, analyticValue)
									val tileMetaData = da.analytic.toMap(analyticValue)
									tileMetaData.map{case (key, value) =>
										tile.setMetaData(key, value)
									}
								}
							)
						}
					)

					// Apply tile analytics
					tileAnalytics.map(analytic =>
						{
							// Figure out the value for this tile
							val analyticValue = analytic.convert(tile)
							// Add it into any appropriate accumulators
							analytic.accumulate(tile.getDefinition(), analyticValue)
							// And store it in the tile's metadata
							analytic.analytic.toMap(analyticValue).map{case (key, value) =>
								tile.setMetaData(key, value)
							}
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

		println("Read "+results.size+" tiles.")
		println("\t"+accStore.inUseCount+" accumulators in use")
		println("\t"+accStore.inUseData+" bins locked in in-use accumulators")
		println("\t"+accStore.availableCount+" accumulators available")
		println("\t"+accStore.availableData+" bins size locked in available accumulators")

		results
	}

	def updateMetaData[BT] (pyramidId: String, tiles: Iterable[TileData[BT]]) = {
		// Update metadata for these levels
		val datasetMetaData = getMetaData(pyramidId).get
		val dataset = datasets(pyramidId).asInstanceOf[Dataset[_, _, _, _, BT]]

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
			tiles.map(tile =>
				new JavaInt(tile.getDefinition().getLevel())
			).toSet.asJava
		)

		metaData(pyramidId) = newDatasetMetaData
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
