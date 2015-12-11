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
import java.util.{ArrayList, Properties}


import scala.collection.mutable.{Map => MutableMap}

import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.factory.ConfigurableFactory
import com.oculusinfo.factory.providers.FactoryProvider
import com.oculusinfo.factory.util.Pair
import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.binning.{BinIndex, TileData, TileIndex}
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.{BinningParameters, StandardBinningFunctions, TileIO, UniversalBinner}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag



object TilingTask {
	/**
	 * From a list of name pieces, construct a string that can be used as a table name when registering a DataFrame
	 * with a SQLContext.
	 *
	 * Basically, this strips out non-alphanumerics, and concatenates the remainder using camel-case.
	 *
	 * @param rawName The desired name
	 * @return The reduced and standardized name
	 */
	def rectifyTableName (rawName: String): String =
		rawName.split("[^a-zA-Z0-9]+").map(_.toLowerCase.capitalize).mkString("")

	/**
	 * Create a standard tiling task from necessary ingredients
	 * @param sqlc A SQL context in which the data, in the form of a DataFrame, has been registered
	 * @param table The table name as which the data has been registered
	 * @param config A configuration object describing how the data is to be tiled
	 * @return A tiling task that can be used to take the data and produce a tile pyramid
	 */
	def apply (sqlc: SQLContext,
						 table: String,
						 config: Properties):
			TilingTask[_, _, _, _] = {
		val jsonConfig = JsonUtilities.propertiesObjToJSON(config)

		val indexerFactory = IndexExtractorFactory(null,
																							 java.util.Arrays.asList("oculus", "binning", "index"),
																							 IndexExtractorFactory.defaultFactory)
		indexerFactory.readConfiguration(jsonConfig)

		val valuerFactory = ValueExtractorFactory(null,
																							java.util.Arrays.asList("oculus", "binning", "value"),
																							ValueExtractorFactory.defaultFactory)
		valuerFactory.readConfiguration(jsonConfig)

		val deferredPyramidFactory = new DeferredTilePyramidFactory(null, java.util.Arrays.asList("oculus", "binning", "projection"))
		deferredPyramidFactory.readConfiguration(jsonConfig)

		val configFactory = new TilingTaskParametersFactory(null, java.util.Arrays.asList("oculus", "binning"))
		configFactory.readConfiguration(jsonConfig)

		val analyzerFactory = new AnalyticExtractorFactory(null, java.util.Arrays.asList("oculus", "binning", "analytics"))
		analyzerFactory.readConfiguration(jsonConfig)

		val indexer = indexerFactory.produce(classOf[IndexExtractor])
		val valuer = valuerFactory.produce(classOf[ValueExtractor[_, _]])
		val analyzer = analyzerFactory.produce(classOf[AnalyticExtractor])
		val deferredPyramid = deferredPyramidFactory.produce(classOf[DeferredTilePyramid])
		val taskConfig = configFactory.produce(classOf[TilingTaskParameters])


		// Tell the tilng task constructor about the processing type tag
		def withValueTags[T: ClassTag, JT] (
			valuer: ValueExtractor[T, JT]): TilingTask[T, _, _, JT] = {
			val dataAnalyticFields = analyzer.fields
			val dataAnalytics = analyzer.dataAnalytics
			val tileAnalytics = analyzer.tileAnalytics(valuer.getTileAnalytics ++ indexer.getTileAnalytics)

			// Tell the tiling task constructor about the analytic type tags
			def withTilingTags[AT: ClassTag, DT: ClassTag] (dataAnalytics: AnalysisWithTag[Seq[Any], DT],
																											tileAnalytics: AnalysisWithTag[TileData[JT], AT]):
					TilingTask[T, DT, AT, JT] = {
				new StaticTilingTask[T, DT, AT, JT](sqlc, table, taskConfig, indexer, valuer, deferredPyramid,
																						dataAnalyticFields, dataAnalytics.analysis, tileAnalytics.analysis).initialize()
			}
			withTilingTags(dataAnalytics, tileAnalytics)
		}

		// Construct the tiling task, with all needed tags
		withValueTags(valuer)
	}

	/**
	 * Appropriately backtick-escape a Spark SQL field name.
	 *
	 * Note: Backtick escape characters will break fields with nested arrays.
	 * E.g. `a.b[0].c[0]` will break,
	 * but a.b[0].c[0] or `a.b`[0].`c`[0] will work.
	 *
	 * Note that "[" and "]" charaters are valid field name characters, so we just can't
	 * tokenize around those characters. For now, just deal with the known problematic
	 * case that required the backtick characters in the first place - names that start
	 * with underscores.
	 *
	 * @param fieldName The name of the field to escape
	 * @return The appropriately-escaped field name
	 */
	def backtickEscapeFieldName(fieldName: String): String = {
		if (fieldName.forall(_.isDigit)) fieldName
		else if (fieldName.length > 0 && fieldName.charAt(0) == '_') "`" + fieldName + "`" // escape field names that start with underscore
		else fieldName
	}
}
/**
 * A TilingTask encapsulates all the information needed to construct a tile pyramid
 *
 * For this first iteration, this will be basically a transfer of the old Dataset into the new types.
 *
 * Future tasks:
 * <ul>
 * <li> Eliminate IndexExtractor - just take the index columns and pass them to the IndexingScheme, which will have to
 * take Array[Any].	The configuration will have to specify the IndexingScheme instead. </li>
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
 * @param deferredPyramid The pyramid type used to store tile data
 * @param tileAnalytics An object that runs analytics locally on each tile
 * @param dataAnalytics An object that run analytics across tiles in each level
 *
 * @tparam PT The processing value type used by this tiling task when calculating bin values.
 * @tparam DT The type of data analytic used by this tiling task.
 * @tparam AT The type of tile analytic used by this tiling task.
 * @tparam BT The final bin type used by this tiling task when writing tiles.
 */
abstract class TilingTask[PT: ClassTag, DT: ClassTag, AT: ClassTag, BT]
	(sqlc: SQLContext,
	 table: String,
	 config: TilingTaskParameters,
	 indexer: IndexExtractor,
	 valuer: ValueExtractor[PT, BT],
	 deferredPyramid: DeferredTilePyramid,
	 dataAnalyticFields: Seq[String],
	 dataAnalytics: Option[AnalysisDescription[Seq[Any], DT]],
	 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]])
{
	val binTypeTag = implicitly[ClassTag[PT]]
	val dataAnalysisTypeTag = implicitly[ClassTag[DT]]
	val tileAnalysisTypeTag = implicitly[ClassTag[AT]]

	type STRATEGY_TYPE <: ProcessingStrategy[Seq[Any], PT, DT]
	protected var strategy: STRATEGY_TYPE



	/** Get the name by which the tile pyramid produced by this task should be known. */
	val getName = {

		var transformedName = config.name
		transformedName = valuer.getTransformedName(transformedName)

		transformedName = indexer.getTransformedName(transformedName)

		val pyramidName = if (config.prefix.isDefined) config.prefix.get + "." + transformedName
		else transformedName

		pyramidName

	}


	/** Get a description of the tile pyramid produced by this task. */
	def getDescription = config.description

	/** The levels this task is intended to tile, in groups that should be tiled together */
	def getLevels = config.levels

	/** The tile pyramid */
	def getTilePyramid = {
		deferredPyramid.getTilePyramid(getAxisBounds)
	}

	/** Needed just for the TimeRangeBinner */
	def getIndexer = indexer


	/** Inheritors may override this to disallow auto-bounds calculations when they make no sense. */
	protected def allowAutoBounds = true

	/** The number of bins per tile, along the X axis, in tiles produced by this task */
	def getNumXBins = config.tileWidth

	/** The number of bins per tile, along the Y axis, in tiles produced by this task */
	def getNumYBins = config.tileHeight

	/** Get the number of partitions to use when reducing data to tiles */
	def getConsolidationPartitions = config.consolidationPartitions

	/** Get the type of tile storage to create when this task creates tiles */
	def getTileType = config.tileType

	/** Get the scheme used to determine axis values for our tiles */
	def getIndexScheme = indexer.indexScheme

	/** Get the serializer used to serialize our tiles */
	def getTileSerializer = valuer.serializer

	/** Get the analytic used to aggregate the bin data for our tiles */
	def getBinningAnalytic = valuer.binningAnalytic

	/** Get the data analytics to be used and inserted into our tiles */
	def getDataAnalytics: Option[AnalysisDescription[Seq[Any], DT]] = dataAnalytics

	/** Get the tile analytics to apply to and record in our tiles */
	def getTileAnalytics: Option[AnalysisDescription[TileData[BT], AT]] = tileAnalytics

	/**
	 * Creates a blank metadata describing this tiling task
	 */
	def createMetaData(pyramidId: String): PyramidMetaData = {
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

	/**
	 * Actually perform tiling, and save tiles.
	 * @param tileIO An object that knows how to save tiles.
	 */
	def doTiling (tileIO: TileIO): Unit = {
		doParameterizedTiling(
			tileIO,
			StandardBinningFunctions.locateIndexOverLevels(getIndexScheme, getTilePyramid, getNumXBins, getNumYBins),
			StandardBinningFunctions.populateTileIdentity
		)
	}

	def doParameterizedTiling (tileIO: TileIO,
														 locFcn: Traversable[Int] => Seq[Any] => Traversable[(TileIndex, Array[BinIndex])],
														 popFcn: (TileIndex, Array[BinIndex], PT) => MutableMap[BinIndex, PT]): Unit = {
		val binner = new UniversalBinner
		val sc = sqlc.sparkContext

		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))

		getLevels.map{levels =>
			tileAnalytics.map(analytic => levels.map(level => analytic.addLevelAccumulator(sc, level)))
			dataAnalytics.map(analytic => levels.map(level => analytic.addLevelAccumulator(sc, level)))

			val procFcn: RDD[(Seq[Any], PT, Option[DT])] => Unit =
				rdd => {
					val tiles = binner.processData[Seq[Any], PT, AT, DT, BT](rdd, getBinningAnalytic, tileAnalytics, dataAnalytics,
																																	 locFcn(levels), popFcn,
						BinningParameters(true, getNumXBins, getNumYBins, getConsolidationPartitions, getConsolidationPartitions, None))

					tileIO.writeTileSet(getTilePyramid, getName, tiles, getTileSerializer,
						tileAnalytics, dataAnalytics, getName, getDescription)
				}

			process(procFcn, None)
		}
	}

	// Axis-related methods and fields
	private lazy val axisBounds = getAxisBounds()

	private def getAxisBounds(): (Double, Double, Double, Double) = {

		val selectStmt =
			indexer.fields.flatMap(unescapedField => {
				val field = TilingTask.backtickEscapeFieldName(unescapedField)
				List("min(" + field + ")", "max(" + field + ")") })
				.mkString("SELECT ", ", ", " FROM " + table)
		val bounds = sqlc.sql(selectStmt).take(1)(0)
		if (bounds.toSeq.map(_ == null).reduce(_ || _))
			throw new Exception("No parsable data found")
		val fields = indexer.fields.size
		val minBounds: Seq[Any] = (1 to fields).map(n => bounds((n-1)*2))
		val maxBounds: Seq[Any] = (1 to fields).map(n => bounds(n*2-1))
		val (minX, minY) = indexer.indexScheme.toCartesian(minBounds)
		val (maxX, maxY) = indexer.indexScheme.toCartesian(maxBounds)
		val (rangeX, rangeY) = (maxX-minX, maxY-minY)

		val maxLevel = {
			if (config.levels.isEmpty) 18
			else config.levels.flatten.reduce(_ max _)
		}
		val maxBins = (config.tileHeight max config.tileWidth)

		// An epsilon of around 2% of a single bin
		val epsilon = ((1.0/(1L << (maxLevel+6))))/maxBins
		(minX, maxX+epsilon*rangeX, minY, maxY+epsilon*rangeY)
	}



	// Strategy facades
	/**
	 * Called to transform the data represented by this TilingTask, assuming it is an RDD
	 * @param fcn The function to apply to this task's data
	 * @tparam OUTPUT_TYPE The output type of the resultant RDD
	 * @return An RDD of this task's data, transformed by the given function.
	 */
	def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: (RDD[(Seq[Any], PT, Option[DT])]) => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized tiling task "+getName)
		} else {
			strategy.transformRDD[OUTPUT_TYPE](fcn)
		}

	/**
	 * Called to transform the data represented by this TilingTask, assuming it is a DStream
	 * @param fcn The function to apply to this task's data
	 * @tparam OUTPUT_TYPE The output type of the resultant DStream
	 * @return A DStream of this task's data, transformed by the given function.
	 */
	def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: (RDD[(Seq[Any], PT, Option[DT])]) => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized tiling task "+getName)
		} else {
			strategy.transformDStream[OUTPUT_TYPE](fcn)
		}

	/**
	 * Completely process this data set in some way, whether it is an RDD or a DStream
	 *
	 * Note that these function may be serialized remotely, so any context-stored
	 * parameters must be serializable
	 */
	def process[OUTPUT] (fcn: (RDD[(Seq[Any], PT, Option[DT])]) => OUTPUT,
											 completionCallback: Option[OUTPUT => Unit]): Unit = {
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized tiling task "+getName)
		} else {
			strategy.process(fcn, completionCallback)
		}
	}
}
class StaticTilingTask[PT: ClassTag, DT: ClassTag, AT: ClassTag, BT]
	(sqlc: SQLContext,
	 table: String,
	 config: TilingTaskParameters,
	 indexer: IndexExtractor,
	 valuer: ValueExtractor[PT, BT],
	 deferredPyramid: DeferredTilePyramid,
	 dataAnalyticFields: Seq[String],
	 dataAnalytics: Option[AnalysisDescription[Seq[Any], DT]],
	 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]])
		extends TilingTask[PT, DT, AT, BT](sqlc, table, config, indexer, valuer, deferredPyramid,
																			 dataAnalyticFields, dataAnalytics, tileAnalytics)
{
	type STRATEGY_TYPE = StaticTilingTaskProcessingStrategy
	override protected var strategy: STRATEGY_TYPE = null
	def initialize (): TilingTask[PT, DT, AT, BT] = {
		strategy = new StaticTilingTaskProcessingStrategy()
		this
	}
	class StaticTilingTaskProcessingStrategy
			extends StaticProcessingStrategy[Seq[Any], PT, DT](sqlc.sparkContext)
	{
		protected def getData: RDD[(Seq[Any], PT, Option[DT])] = {
			val allFields = indexer.fields ++ valuer.fields ++ dataAnalyticFields
			val allFieldsEscaped = allFields.map(v => TilingTask.backtickEscapeFieldName(v))

			val selectStmt =
				allFieldsEscaped.mkString("SELECT ", ", ", " FROM "+table)

			val data = sqlc.sql(selectStmt)

			val indexFields = indexer.fields.length
			val valueFields = valuer.fields.length
			val localDataAnalytics = dataAnalytics
			val localValuer = valuer
			val mappedData: RDD[(Seq[Any], PT, Option[DT])] = data.map(row =>
				{
					val index = (0 until indexFields).map(n => row(n))

					val values = (indexFields until (indexFields+valueFields)).map(n => row(n))
					val value = localValuer.convert(values)

					val analyticInputs = ((indexFields+valueFields) until row.length).map(n => row(n))
					val analysis = localDataAnalytics.map(analytic => analytic.convert(analyticInputs))

					(index, value, analysis)
				}
			)

			// If set, filter bins that are out of the level 0 tile bounds
			if (config.filterToRegion) {
				val iScheme = indexer.indexScheme
				val area = getTilePyramid.getTileBounds(new TileIndex(0, 0, 0))

				mappedData.filter(lineSeq => {
					val (x, y) = iScheme.toCartesian(lineSeq._1)

					area.contains(x, y)
				})
			} else {
				mappedData
			}
		}

		override def getDataAnalytics: Option[AnalysisDescription[_, DT]] = dataAnalytics
	}
}

object StandardScalingFunctions {
	def identityScale[T]: (Array[BinIndex], BinIndex, T) => T = (endpoints, bin, value) => value
}
