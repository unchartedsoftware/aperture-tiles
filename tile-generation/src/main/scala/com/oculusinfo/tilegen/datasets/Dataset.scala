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



package com.oculusinfo.tilegen.datasets



import java.lang.{Integer => JavaInt}
import java.lang.{Double => JavaDouble}
import java.util.ArrayList
import java.util.Properties

import scala.collection.mutable.MutableList
import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.Time

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic
import com.oculusinfo.tilegen.tiling.IndexScheme




/**
 * A Dataset encapsulates all that is needed to retrieve data for binning.
 * The goal is that Datasets can be constructed (via a DatasetFactory) from
 * simple property files, which can be passed into a binning process from any
 * place that needs raw data to be binned.
 */
abstract class Dataset[IT: ClassTag, PT: ClassTag, DT: ClassTag, AT: ClassTag, BT] {
	val indexTypeTag = implicitly[ClassTag[IT]]
	val binTypeTag = implicitly[ClassTag[PT]]
	val dataAnalysisTypeTag = implicitly[ClassTag[DT]]
	val tileAnalysisTypeTag = implicitly[ClassTag[AT]]
	var _debug = true;



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

	def getNumXBins: Int = 256
	def getNumYBins: Int = 256

	def getConsolidationPartitions: Option[Int] = None
	
	def getIndexScheme: IndexScheme[IT]
	def getTileSerializer: TileSerializer[BT]

	/**
	 * Get an analytic that defines how to aggregate this data
	 */
	def getBinningAnalytic: BinningAnalytic[PT, BT]

	/**
	 * Creates a blank metadata describing this dataset
	 */
	def createMetaData (pyramidId: String): PyramidMetaData = {
		val tilePyramid = getTilePyramid
		val fullBounds = tilePyramid.getTileBounds(
			new TileIndex(0, 0, 0, getNumXBins, getNumYBins)
		)
		new PyramidMetaData(pyramidId,
		                    getDescription,
		                    getNumXBins, getNumYBins,
		                    tilePyramid.getTileScheme(),
		                    tilePyramid.getProjection(),
		                    null,
		                    fullBounds,
		                    new ArrayList[Pair[JavaInt, String]](),
		                    new ArrayList[Pair[JavaInt, String]]())
	}


	type STRATEGY_TYPE <: ProcessingStrategy[IT, PT, DT]

	protected var strategy: STRATEGY_TYPE
	def initialize (strategy: STRATEGY_TYPE): Unit = {
		this.strategy = strategy
	}
	
	
	/**
	 * Completely process this data set in some way.
	 *
	 * Note that these function may be serialized remotely, so any context-stored
	 * parameters must be serializable
	 */
	def process[OUTPUT] (fcn: (RDD[(IT, PT, Option[DT])]) => OUTPUT,
	                     completionCallback: Option[OUTPUT => Unit]): Unit = {
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.process(fcn, completionCallback)
		}
	}

	def getDataAnalytics: Option[AnalysisDescription[_, DT]]

	def getTileAnalytics: Option[AnalysisDescription[TileData[BT], AT]]

	def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: (RDD[(IT, PT, Option[DT])]) => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.transformRDD[OUTPUT_TYPE](fcn)
		}

	def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: (RDD[(IT, PT, Option[DT])]) => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.transformDStream[OUTPUT_TYPE](fcn)
		}
}

trait StreamingProcessor[IT, PT, DT] {
	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT, Option[DT])] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit
}

abstract class ProcessingStrategy[IT: ClassTag, PT: ClassTag, DT: ClassTag] {
	def process[OUTPUT] (fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                     completionCallback: Option[OUTPUT => Unit]): Unit

	def getDataAnalytics: Option[AnalysisDescription[_, DT]]

	def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE]

	def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE]
}

abstract class StaticProcessingStrategy[IT: ClassTag, PT: ClassTag, DT: ClassTag] (sc: SparkContext)
		extends ProcessingStrategy[IT, PT, DT] {
	private val rdd = getData

	protected def getData: RDD[(IT, PT, Option[DT])]

	final def process[OUTPUT] (fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
		val result = fcn(rdd)
		completionCallback.map(_(result))
	}

	final def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		fcn(rdd)

	final def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		throw new Exception("Attempt to call DStream transform on RDD processor")
}

abstract class StreamingProcessingStrategy[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		extends ProcessingStrategy[IT, PT, DT] {
	private val dstream = getData

	protected def getData: DStream[(IT, PT, Option[DT])]

	private final def internalProcess[OUTPUT] (rdd: RDD[(IT, PT, Option[DT])],
	                                           fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
		val result = fcn(rdd)
		completionCallback.map(_(result))
	}

	def process[OUTPUT] (fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                     completionCallback: Option[(OUTPUT => Unit)] = None): Unit = {
		dstream.foreachRDD(internalProcess(_, fcn, completionCallback))
	}

	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT, Option[DT])] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit = {
		dstream.foreachRDD{(rdd, time) =>
			internalProcess(rdd, fcn(time), completionCallback.map(_(time)))
		}
	}
	
	final def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		throw new Exception("Attempt to call RDD transform on DStream processor")

	final def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		dstream.transform(fcn)
}



object DatasetFactory {
	private def newDataset[IT: ClassTag, PT: ClassTag, DT: ClassTag, AT: ClassTag, BT]
		(sc: SparkContext,
		 cacheRaw: Boolean,
		 cacheFilterable: Boolean,
		 cacheProcessed: Boolean,
		 indexer: CSVIndexExtractor[IT],
		 valuer: CSVValueExtractor[PT, BT],
		 dataAnalytics: Option[AnalysisDescription[(IT, PT), DT]],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 tileWidth: Int,
		 tileHeight: Int,
		 levels: Seq[Seq[Int]],
		 properties: CSVRecordPropertiesWrapper):
			CSVDataset[IT, PT, DT, AT, BT] = {
		val dataset = new CSVDataset(indexer, valuer,
		                             dataAnalytics, tileAnalytics,
		                             tileWidth, tileHeight, levels, properties)
		dataset.initialize(sc, cacheRaw, cacheFilterable, cacheProcessed)
		dataset
	}

	private def addGenericAnalytics[IT, PT, DT, AT, BT]
		(sc: SparkContext,
		 cacheRaw: Boolean,
		 cacheFilterable: Boolean,
		 cacheProcessed: Boolean,
		 indexer: CSVIndexExtractor[IT],
		 valuer: CSVValueExtractor[PT, BT],
		 tileWidth: Int,
		 tileHeight: Int,
		 levels: Seq[Seq[Int]],
		 properties: CSVRecordPropertiesWrapper,
		 dataAnalytic: AnalysisWithTag[(IT, PT), DT],
		 tileAnalytic: AnalysisWithTag[TileData[BT], AT]):
			CSVDataset[IT, PT, DT, AT, BT] = {
		newDataset(sc, cacheRaw, cacheFilterable, cacheProcessed,
		           indexer,
		           valuer,
		           dataAnalytic.analysis,
		           tileAnalytic.analysis,
		           tileWidth,
		           tileHeight,
		           levels,
		           properties)(indexer.indexTypeTag,
		                       valuer.valueTypeTag,
		                       dataAnalytic.analysisTypeTag,
		                       tileAnalytic.analysisTypeTag)
	}

	// CreateDataset, but with labeled types
	private def createDatasetGeneric[IT, PT, BT] (sc: SparkContext,
	                                              cacheRaw: Boolean,
	                                              cacheFilterable: Boolean,
	                                              cacheProcessed: Boolean,
	                                              indexer: CSVIndexExtractor[IT],
	                                              valuer: CSVValueExtractor[PT, BT],
	                                              tileWidth: Int,
	                                              tileHeight: Int,
	                                              levels: Seq[Seq[Int]],
	                                              properties: CSVRecordPropertiesWrapper):
			CSVDataset[IT, PT, _, _, BT] = {
		val indexDataAnalytics =
			indexer.getDataAnalytics.asInstanceOf[Seq[AnalysisDescription[(IT, PT), _]]]
		val valueDataAnalytics =
			valuer.getDataAnalytics.asInstanceOf[Seq[AnalysisDescription[(IT, PT), _]]]
		val dataAnalyticDescs = indexDataAnalytics ++ valueDataAnalytics
		val dataAnalytic = CSVDataAnalyticExtractor.consolidate(properties, dataAnalyticDescs)

		val tileAnalyticDescs: Seq[AnalysisDescription[TileData[BT], _]] =
			(indexer.getTileAnalytics.asInstanceOf[Seq[AnalysisDescription[TileData[BT], _]]] ++
				 valuer.getTileAnalytics.asInstanceOf[Seq[AnalysisDescription[TileData[BT], _]]])
		val tileAnalytic = CSVTileAnalyticExtractor.consolidate(properties, tileAnalyticDescs)

		addGenericAnalytics(sc, cacheRaw, cacheFilterable, cacheProcessed,
		                    indexer, valuer, tileWidth, tileHeight, levels, properties,
		                    dataAnalytic, tileAnalytic)
	}

	def createDataset (sc: SparkContext,
	                   dataDescription: Properties,
	                   width: Option[Int] = None,
	                   height: Option[Int] = None): Dataset[_, _, _, _, _] = {

		// Wrap parameters more usefully
		val properties = new CSVRecordPropertiesWrapper(dataDescription)

		val levels = properties.getStringPropSeq("oculus.binning.levels",
		                                         "The levels to bin").map(lvlString =>
			{
				lvlString.split(',').map(levelRange =>
					{
						val extrema = levelRange.split('-')

						if ((0 == extrema.size) || (levelRange==""))
							Seq[Int]()
						else if (1 == extrema.size)
							Seq[Int](extrema(0).trim.toInt)
						else
							Range(extrema(0).trim.toInt, extrema(1).trim.toInt+1).toSeq
					}
				).fold(Seq[Int]())(_ ++ _)
			}
		).filter(levelSeq =>
			levelSeq != Seq[Int]()	// discard empty entries
		)
		// Determine index and value information
		val indexer = CSVIndexExtractor.fromProperties(properties)
		val valuer = CSVValueExtractor.fromProperties(properties, CSVValueExtractor.standardFactories)

		val cacheRaw = properties.getBoolean("oculus.binning.caching.raw",
		                                     "Whether or not to cache the raw data for multiple raw "+
			                                     "data requests.",
		                                     Some(false))
		val cacheFilterable = properties.getBoolean("oculus.binning.caching.filterable",
		                                            "Whether or not to cache parsed, but unprocessed, data, "+
			                                            "for use in searching raw data",
		                                            Some(false))
		val cacheProcessed = properties.getBoolean("oculus.binning.caching.processed",
		                                           "Whether or not to cache parsed, processed data for "+
			                                           "multiple binning runs",
		                                           Some(false))
		val xBins = width.getOrElse(properties.getInt("oculus.binning.xbins",
		                                              "The number of bins per tile along the horizontal axis",
		                                              Some(indexer.getDefaultXBins)))
		val yBins = height.getOrElse(properties.getInt("oculus.binning.ybins",
		                                               "The number of bins per tile along the vertical axis",
		                                               Some(indexer.getDefaultYBins)))

		println("Creating dataset")
		println("\tRaw data caching: "+cacheRaw)
		println("\tFilterable data caching: "+cacheFilterable)
		println("\tProcessed data caching: "+cacheProcessed)
		println("\tX bins per tile: "+xBins)
		println("\tY bins per tile: "+yBins)
		createDatasetGeneric(sc, cacheRaw, cacheFilterable, cacheProcessed,
		                     indexer, valuer, xBins, yBins, levels, properties)
	}
}
