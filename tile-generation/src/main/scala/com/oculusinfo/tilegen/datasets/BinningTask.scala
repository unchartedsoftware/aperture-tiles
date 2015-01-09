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

import com.oculusinfo.binning.impl.{AOITilePyramid, WebMercatorTilePyramid}
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.binning.{TileIndex, TileData, TilePyramid}
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.tilegen.spark.{DoubleMaxAccumulatorParam, DoubleMinAccumulatorParam}
import com.oculusinfo.tilegen.tiling.{CartesianSchemaIndexScheme, IndexScheme}
import com.oculusinfo.tilegen.tiling.analytics.{NumericSumBinningAnalytic, AnalysisDescription, BinningAnalytic}
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import org.apache.avro.file.CodecFactory
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SchemaRDD}
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag


/**
 * A BinningTask encapsulates all the information needed to construct a tile pyramid
 *
 * For this first iteration, this will be basically a transfer of the old Dataset into the new types.
 *
 * Future tasks:
 * <ul>
 * <li> Eliminate IndexExtractor - just take the index columns and pass them to the IndexingScheme, which will have to
 * take Array[Any].  The configuration will have to specify the IndexingScheme instead. </li>
 * <li> Eliminate ValueExtractor - again, just take value columns and pass them to the BinningAnalytic. The binning
 * analytic will have to be specified instead. We may need some standard transformers to prepare input for the
 * binning analytic.</li>
 * <li> Specify Tile Analytics explicitly? </li>
 * <li> Change data analytics to work more like binning analytics, with specified columns as inputs. </li>
 * <li> Standard typed transformations based on spark.sql DataTypes.</li>
 * </ul>
 *
 * @param sqlc The SQL context in which to run
 * @param table The table in that SQL context containing our raw data.
 * @param config An object specifying the configuration details of this task.
 * @param indexer An object to extract the index value(s) from the raw data
 * @param valuer An object to extract the binnable value(s) from the raw data
 * @param analyzer An object to extract data analytic value(s) from the raw data
 * @param pyramidLevels The levels of the tile pyramid this tiling task is expecting to calculate.
 * @param tileWidth The width, in bins, of any tile this task calculates.
 * @param tileHeight The height, in bins, of any tile this task calculates.
 *
 * @tparam PT The processing value type used by this binning task when calculating bin values.
 * @tparam BT The final bin type used by this binning task when writing tiles.
 * @tparam AT The type of tile analytic used by this binning task.
 * @tparam DT The type of data analytic used by this binning task.
 */
abstract class BinningTask[PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
	(sqlc: SQLContext,
	 table: String,
	 config: BinningTaskParameters,
	 indexer: IndexExtractor,
	 valuer: ValueExtractor[PT, BT],
	 deferredPyramid: DeferredTilePyramid,
	 analyzer: AnalyticExtractor[BT, AT, DT],
	 pyramidLevels: Seq[Seq[Int]],
	 val tileWidth: Int = 256,
	 val tileHeight: Int = 256)
		extends Dataset[Seq[Any], PT, DT, AT, BT] {
	/** Get the name by which the tile pyramid produced by this task should be known. */
	val getName = {
		val pyramidName = if (config.prefix.isDefined) config.prefix.get + "." + config.name
		                  else config.name

		pyramidName + "." + indexer.name + "." + valuer.name
	}


	/** Get a description of the tile pyramid produced by this task. */
	def getDescription = config.description

	/** The levels this task is intended to tile, in groups that should be tiled together */
	def getLevels = pyramidLevels

	/** The tile pyramid */
	def getTilePyramid = {
		deferredPyramid.getTilePyramid(getAxisBounds)
	}

	/** Inheritors may override this to disallow auto-bounds calculations when they make no sense. */
	protected def allowAutoBounds = true

	/** The number of bins per tile, along the X axis, in tiles produced by this task */
	override def getNumXBins = tileWidth

	/** The number of bins per tile, along the Y axis, in tiles produced by this task */
	override def getNumYBins = tileHeight

	/** Get the number of partitions to use when reducing data to tiles */
	override def getConsolidationPartitions = config.consolidationPartitions

	/** Get the scheme used to determine axis values for our tiles */
	def getIndexScheme = indexer.indexScheme

	/** Get the serializer used to serialize our tiles */
	def getTileSerializer = valuer.serializer

	/** Get the analytic used to aggregate the bin data for our tiles */
	def getBinningAnalytic = valuer.binningAnalytic

	/** Get the data analytics to be used and inserted into our tiles */
	def getDataAnalytics: Option[AnalysisDescription[_, DT]] = analyzer.dataAnalytics

	/** Get the tile analytics to apply to and record in our tiles */
	def getTileAnalytics: Option[AnalysisDescription[TileData[BT], AT]] = analyzer.tileAnalytics

	/**
	 * Creates a blank metadata describing this dataset
	 */
	override def createMetaData(pyramidId: String): PyramidMetaData = {
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


	private def transformRDD[T](transformation: RDD[(Seq[Any], PT, Option[DT])] => RDD[T]): RDD[T] = {
		null
	}

	private lazy val axisBounds = getAxisBounds()

	private def getAxisBounds(): (Double, Double, Double, Double) = {
		val selectStmt =
			indexer.fields.flatMap(field => List("min(" + field + ")", "max(" + field + ")"))
				.mkString("SELECT ", ", ", " FROM " + table)
		val bounds = sqlc.sql(selectStmt).take(1)(0)
		val minBounds = bounds.grouped(2).map(_(0)).toSeq
		val maxBounds = bounds.grouped(2).map(_(1)).toSeq
		val (minX, minY) = indexer.indexScheme.toCartesian(minBounds)
		val (maxX, maxY) = indexer.indexScheme.toCartesian(maxBounds)
		(minX, maxX, minY, maxY)
	}
}
class StaticBinningTask[PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
	(sqlc: SQLContext,
	 table: String,
	 config: BinningTaskParameters,
	 indexer: IndexExtractor,
	 valuer: ValueExtractor[PT, BT],
	 deferredPyramid: DeferredTilePyramid,
	 analyzer: AnalyticExtractor[BT, AT, DT],
	 pyramidLevels: Seq[Seq[Int]],
	 tileWidth: Int = 256,
	 tileHeight: Int = 256)
		extends BinningTask[PT, AT, DT, BT](sqlc, table, config, indexer, valuer, deferredPyramid, analyzer, pyramidLevels, tileWidth, tileHeight)
{
	type STRATEGY_TYPE = StaticBinningTaskProcessingStrategy
	override protected var strategy: STRATEGY_TYPE = null
	def initialize (): Unit = {
		initialize(new StaticBinningTaskProcessingStrategy())
	}
	class StaticBinningTaskProcessingStrategy
			extends StaticProcessingStrategy[Seq[Any], PT, DT](sqlc.sparkContext)
	{
		def getDataAnalytics: Option[AnalysisDescription[_, DT]] = analyzer.dataAnalytics

		protected def getData: RDD[(Seq[Any], PT, Option[DT])] = {
			val allFields = indexer.fields ++ valuer.fields ++ analyzer.fields

			val selectStmt =
				allFields.mkString("SELECT ", ", ", " FROM "+table)

			val data = sqlc.sql(selectStmt)

			val indexFields = indexer.fields.length
			val valueFields = valuer.fields.length
			val localValuer = valuer
			val analytics = analyzer.dataAnalytics
			data.map(row =>
				{
					val index = row.take(indexFields)

					val values = row.drop(indexFields).take(valueFields)
					val value = localValuer.convert(values)

					val analyticInputs = row.drop(indexFields+valueFields)
					val analysis = analytics.map(analytic => analytic.convert(analyticInputs))

					(index, value, analysis)
				}
			)
		}
	}
}








/**
 * A class to encapsulate the data analytics needed by a tiling task, and the information needed to obtain the
 * data to run them.
 */

abstract class AnalyticExtractor[BT, AT: ClassTag, DT: ClassTag] {
	def fields: Seq[String]

	def tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]]

	def dataAnalytics: Option[AnalysisDescription[Seq[Any], DT]]
}
