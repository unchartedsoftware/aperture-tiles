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



import java.util.Properties
import scala.collection.mutable.MutableList
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.TileMetaData
import com.oculusinfo.tilegen.tiling.BinDescriptor
import org.apache.spark.streaming.Time




/**
 * A Dataset encapsulates all that is needed to retrieve data for binning.
 * The goal is that Datasets can be constructed (via a DatasetFactory) from
 * simple property files, which can be passed into a binning process from any
 * place that needs raw data to be binned.
 */
abstract class Dataset[IT: ClassManifest, PT: ClassManifest, BT] {
	val binTypeManifest = implicitly[ClassManifest[PT]]
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
	
	def isDensityStrip: Boolean = false

	/**
	 * Get a bin descriptor that can be used to bin this data
	 */
	def getBinDescriptor: BinDescriptor[PT, BT]

	/**
	 * Creates a blank metadata describing this dataset
	 */
	def createMetaData (pyramidId: String): TileMetaData = {
		val tileSize = (getNumXBins max getNumYBins)
		val tilePyramid = getTilePyramid
		val fullBounds = tilePyramid.getTileBounds(
			new TileIndex(0, 0, 0, getNumXBins, getNumYBins)
		)
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


	type STRATEGY_TYPE <: ProcessingStrategy[IT, PT]

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
	def process[OUTPUT] (fcn: (RDD[(IT, PT)]) => OUTPUT,
	                     completionCallback: Option[OUTPUT => Unit]): Unit = {
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.process(fcn, completionCallback)
		}
	}

	def transformRDD[OUTPUT_TYPE: ClassManifest]
		(fcn: (RDD[(IT, PT)]) => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.transformRDD[OUTPUT_TYPE](fcn)
		}

	def transformDStream[OUTPUT_TYPE: ClassManifest]
		(fcn: (RDD[(IT, PT)]) => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.transformDStream[OUTPUT_TYPE](fcn)
		}
}

trait StreamingProcessor[IT, PT] {
	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT)] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit
}

abstract class ProcessingStrategy[IT: ClassManifest, PT: ClassManifest] {
	def process[OUTPUT] (fcn: RDD[(IT, PT)] => OUTPUT,
	                     completionCallback: Option[OUTPUT => Unit]): Unit

	def transformRDD[OUTPUT_TYPE: ClassManifest]
		(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE]

	def transformDStream[OUTPUT_TYPE: ClassManifest]
		(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE]
}

abstract class StaticProcessingStrategy[IT: ClassManifest, PT: ClassManifest] (sc: SparkContext)
		extends ProcessingStrategy[IT, PT] {
	private val rdd = getData

	protected def getData: RDD[(IT, PT)]

	final def process[OUTPUT] (fcn: RDD[(IT, PT)] => OUTPUT,
	                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
		val result = fcn(rdd)
		completionCallback.map(_(result))
	}

	final def transformRDD[OUTPUT_TYPE: ClassManifest]
		(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		fcn(rdd)

	final def transformDStream[OUTPUT_TYPE: ClassManifest]
		(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		throw new Exception("Attempt to call DStream transform on RDD processor")
}

abstract class StreamingProcessingStrategy[IT: ClassManifest, PT: ClassManifest]
		extends ProcessingStrategy[IT, PT] {
	private val dstream = getData

	protected def getData: DStream[(IT, PT)]

	private final def internalProcess[OUTPUT] (rdd: RDD[(IT, PT)], fcn: RDD[(IT, PT)] => OUTPUT,
	                                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
		val result = fcn(rdd)
		completionCallback.map(_(result))
	}

	def process[OUTPUT] (fcn: RDD[(IT, PT)] => OUTPUT,
	                     completionCallback: Option[(OUTPUT => Unit)] = None): Unit = {
		dstream.foreach(internalProcess(_, fcn, completionCallback))
	}

	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT)] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit = {
		dstream.foreach{(rdd, time) =>
			internalProcess(rdd, fcn(time), completionCallback.map(_(time)))
		}
	}
	
	final def transformRDD[OUTPUT_TYPE: ClassManifest]
		(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		throw new Exception("Attempt to call RDD transform on DStream processor")

	final def transformDStream[OUTPUT_TYPE: ClassManifest]
		(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		dstream.transform(fcn)
}



object DatasetFactory {
	def createDataset (sc: SparkContext,
	                   dataDescription: Properties,
	                   cacheRaw: Boolean,
	                   cacheFilterable: Boolean,
	                   cacheProcessed: Boolean,
	                   tileWidth: Int = 256,
	                   tileHeight: Int = 256): Dataset[(Double, Double), _, _] = {
		val dataset = new CSVDataset(dataDescription, tileWidth, tileHeight)
		dataset.initialize(sc, cacheRaw, cacheFilterable, cacheProcessed)
		dataset
	}
}
