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


import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.tilegen.spark.{DoubleMaxAccumulatorParam, DoubleMinAccumulatorParam}
import com.oculusinfo.tilegen.tiling.IndexScheme
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SchemaRDD

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
 * @param data The data on which this task is run.
 * @param config An object specifying the configuration details of this task.
 * @param indexer An object to extract the index value(s) from the raw data
 * @param valuer An object to extract the binnable value(s) from the raw data
 * @param pyramidLevels The levels of the tile pyramid this tiling task is expecting to calculate.
 * @param tileWidth The width, in bins, of any tile this task calculates.
 * @param tileHeight The height, in bins, of any tile this task calculates.
 *
 * @tparam IT The index type used by this binning task when calculating bins coordinates.
 * @tparam PT The processing value type used by this binning task when calculating bin values.
 * @tparam BT The final bin type used by this binning task when writing tiles.
 * @tparam AT The type of tile analytic used by this binning task.
 * @tparam DT The type of data analytic used by this binning task.
 */
abstract class BinningTask[IT: ClassTag, PT: ClassTag, BT, AT: ClassTag, DT: ClassTag]
(data: SchemaRDD,
 config: KeyValueArgumentSource,
 indexer: IndexExtractor[IT],
 valuer: ValueExtractor,
 pyramidLevels: Seq[Seq[Int]],
 val tileWidth: Int = 256,
 val tileHeight: Int = 256) {
	val indexTypeTag = implicitly[ClassTag[IT]]
	val binTypeTag = implicitly[ClassTag[PT]]
	val dataAnalysisTypeTag = implicitly[ClassTag[DT]]
	val tileAnalysisTypeTag = implicitly[ClassTag[AT]]

	/** Get the name by which the tile pyramid produced by this task should be known. */
	val name = {
		val name = config.getString("oculus.binning.name",
		                            "The name of the tileset",
		                            Some("unknown"))
		val prefix = config.getStringOption("oculus.binning.prefix",
		                                    "A prefix to add to the tile pyramid ID")
		val pyramidName = if (prefix.isDefined) prefix.get + "." + name
		else name

		pyramidName + "." + indexer.name + "." + valuer.name
	}


	/** Get a description of the tile pyramid produced by this task. */
	def description =
		config.getStringOption("oculus.binning.description", "The description to put in the tile metadata")
				.getOrElse("Binned " + name + " data showing " + indexer.description)

	/** The levels this task is intended to tile, in groups that should be tiled together */
	def levels = pyramidLevels

	/** The tile pyramid */
	def tilePyramid = {
		val extractor = new FieldExtractor(config)
		val autoBounds = (
				allowAutoBounds &&
						config.getBoolean("oculus.binning.projection.autobounds",
						                  "If true, calculate tile pyramid bounds automatically; " +
								                  "if false, use values given by properties",
						                  Some(true))
				)
		val (minX, maxX, minY, maxY) =
			if (autoBounds) {
				axisBounds
			} else {
				(0.0, 0.0, 0.0, 0.0)
			}

		extractor.getTilePyramid(autoBounds, "", minX, maxX, "", minY, maxY)
	}

	private lazy val axisBounds = getAxisBounds()


	/** Inheritors may override this to disallow auto-bounds calculations when they make no sense. */
	protected def allowAutoBounds = true


	private def transformRDD[T](transformation: RDD[(IT, PT, Option[DT])] => RDD[T]): RDD[T] = {
		null
	}
	private def getAxisBounds(): (Double, Double, Double, Double) = {
		val localIndexer = indexer
		val cartesianConversion = localIndexer.indexScheme.toCartesian(_)
		val toCartesian: RDD[(IT, PT, Option[DT])] => RDD[(Double, Double)] =
			rdd => rdd.map(_._1).map(cartesianConversion)
		val coordinates = transformRDD(toCartesian)

		// Figure out our axis bounds
		val minXAccum = coordinates.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
		val maxXAccum = coordinates.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)
		val minYAccum = coordinates.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
		val maxYAccum = coordinates.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)

		config.setDistributedComputation(true)
		coordinates.foreach(p => {
			val (x, y) = p
			minXAccum += x
			maxXAccum += x
			minYAccum += y
			maxYAccum += y
		}
		)
		config.setDistributedComputation(false)

		val minX = minXAccum.value
		val maxX = maxXAccum.value
		val minY = minYAccum.value
		val maxY = maxYAccum.value

		// Include a fraction of a bin extra in the bounds, so the max goes on the
		// right side of the last tile, rather than forming an extra tile.
		val maxLevel = {
			if (levels.isEmpty) 18
			else levels.map(_.reduce(_ max _)).reduce(_ max _)
		}
		val epsilon = (1.0 / (1 << maxLevel))
		val adjustedMaxX = maxX + (maxX - minX) * epsilon / (tileWidth * tileWidth)
		val adjustedMaxY = maxY + (maxY - minY) * epsilon / (tileHeight * tileHeight)

		(minX, adjustedMaxX, minY, adjustedMaxY)
	}
}

abstract class IndexExtractor[IT] {
	def name: String

	def description: String

	def indexScheme: IndexScheme[IT]
}

abstract class ValueExtractor {
	def name: String
}

class FieldExtractor (config: KeyValueArgumentSource) {
	def getTilePyramid(autoBounds: Boolean,
	                   xField: String, minX: Double, maxX: Double,
	                   yField: String, minY: Double, maxY: Double): TilePyramid = null
}