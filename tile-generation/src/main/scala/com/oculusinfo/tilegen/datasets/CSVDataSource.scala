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



import java.lang.{Double => JavaDouble}
import java.util.Properties

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Time

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic
import com.oculusinfo.tilegen.util.{KeyValueArgumentSource, ArgumentParser, PropertiesWrapper}


/**
 * A simple data source for binning of generic CSV data based on a
 * property-style configuration file
 */
class CSVDataSource (properties: KeyValueArgumentSource) {
	def getDataFiles: Seq[String] = properties.getStringPropSeq(
		"oculus.binning.source.location",
		"The hdfs file name from which to get the CSV data.  Either a directory, all "+
			"of whose contents should be part of this dataset, or a single file.")

	def getIdealPartitions: Option[Int] = properties.getIntOption(
		"oculus.binning.source.partitions",
		"The number of partitions to use when reducing data, if needed")

	/**
	 * Actually retrieve the data.
	 * This can be overridden if the data is not a simple file or set of files,
	 * but normally shouldn't be touched. 
	 */
	def getData (sc: SparkContext): RDD[String] =
		// For each file, attempt create an RDD, then immediately force an
		// exception in the case it does not exist. Union all RDDs together.
		getDataFiles.map{ file =>
			Try(
				{
					var tmp = if ( getIdealPartitions.isDefined ) {
						sc.textFile( file, getIdealPartitions.get )
					} else {
						sc.textFile( file )
					}
					tmp.partitions // force exception if file does not exist
					tmp
				}
			).getOrElse( sc.emptyRDD[String] )
		}.reduce(_ union _)
}
