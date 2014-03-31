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
 
package com.oculusinfo.tilegen.tiling



import java.lang.{Double => JavaDouble}


import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.MissingArgumentException
import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam
import com.oculusinfo.tilegen.spark.SparkConnector

import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid




/**
 * This binner class wraps RDDBinner with some object-oriented trappings
 * that make creating a new data set significantly easier.
 *
 * The various descriptions and operations on the data set are encapsulated
 * in standard objects which each focus on one aspect.
 *
 * @param name
 *        The name of the data set
 * @param whoami
 *        The name of the user doing the binning (so it can display on the
 *        job name in Spark)
 * @param source
 *        A helper class encapsulating how the data is retrieved.  This does
 *        not need to be serializable.
 * @param parser
 *        A helper class to parse raw data lines into useful records.  This
 *        does need to be serializable.
 * @param extractor
 *        A helper class to extract numeric information from records, so that
 *        the results can actually be binned.
 */
class ObjectifiedBinner[T: ClassManifest] (name: String,
                                           whoami: String,
                                           source: DataSource,
                                           parser: RecordParser[T],
                                           extractor: FieldExtractor[T])
extends ObjectifiedBinnerBase[T](source, parser, extractor) {
  def usage: Unit = {
    println("Usage: "+this.getClass().getName()+" <x-axis variable> <y-axis variable> <levels...>")
    println("Allowed variables:")
    extractor.getValidFieldList.foreach(field => println("\t"+field))
  }


  def doBinning (args: Array[String],
                 consolidationPartitions: Option[Int] = None): Unit = {
    val argParser = new ArgumentParser(args);

    try {
      // Figure out our axis variables
      val xVar = argParser.getString("x",
                                     "The variable to use as the X "
	                                     +"axis when binning")
      val yVar = argParser.getString("y",
                                     "The variable to use as the Y "
	                                     +"axis when binning")

      // Figure out what value to bin
      val resultField = 
        argParser.getString("result",
                            "The variable to use as the bin "
	                            +"contents.  Defaults to a simple count "
	                            +"of records in that bin.",
                            Some("count"))

      val localExtractor = extractor // localized to avoid the need for serialization
      val resultFcn = if ("count" == resultField) {
        // No result field was passed in - just count instances
        t: T => new ValueOrException(Some(1.0), None)
      } else {
        // The user passed in a field as the third argument - use it as a
        // result field (but convert to a java double, because that's what
        // our serializer will need)
        t: T => localExtractor.getFieldValue(resultField)(t)
      }

      // Figure out what levels to bin
      val levels = argParser.getIntSeq("levels",
                                       "A list of comma-separated "
	                                       +"levels to bin")

      // And some other utility classes
      val jobName = (name+"bin tiling "+xVar+" vs "+yVar+", levels "
                     +levels.mkString(",")+" ("+whoami+")")

      val sc = argParser.getSparkConnector().getSparkContext(jobName)
      val tileIO = TileIO.fromArguments(argParser)

      if (!extractor.isValidField(xVar) || !extractor.isValidField(yVar)) {
        usage
        return
      }

      println("Extracting binned tiles for the "+name+" data set")
      println("\tX axis: "+xVar)
      println("\tY axis: "+yVar)
      println("\tResult field: "+resultField)
      println("\tLevels: "+levels.mkString(", "))



      doBinning(sc, tileIO,
                name, xVar, yVar, resultField, resultFcn,
                Seq(levels), consolidationPartitions)
    } catch {
      case e: MissingArgumentException => {
        println("Binning Argument exception: "+e.getMessage())
        argParser.usage
      }
    }
  }
}

class ObjectifiedBinnerBase[T: ClassManifest] (source: DataSource,
                                               parser: RecordParser[T],
                                               extractor: FieldExtractor[T]) {
  // Set to true to get user-readable output describing what is binned in the log
  var debug: Boolean = false
  // Set to false to avoid actual execution of binning; with debug set to true and 
  // this false, one is told exactly what would be binned if it were run, without
  // the bother of actually running anything
  var execute: Boolean = true

  private def getAxisBounds (data: RDD[T],
                             xCoordFcn: T => ValueOrException[Double],
                             yCoordFcn: T => ValueOrException[Double]): (Double, Double, Double, Double) = {
    val coordinates = data.map(r =>
      // Extract our axis variables
      (xCoordFcn(r), yCoordFcn(r))
    ).filter(coordPair =>
      // Filter out unsuccessful field extractions
      coordPair._1.hasValue && coordPair._2.hasValue
    ).map(coordPair =>
      (coordPair._1.get, coordPair._2.get)
    )

    // Figure out the bounds of our values
    val minXAccum = data.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
    val maxXAccum = data.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)
    val minYAccum = data.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
    val maxYAccum = data.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)
    coordinates.foreach(p => {
      val (x, y) = p
      minXAccum += x
      maxXAccum += x
      minYAccum += y
      maxYAccum += y
    })

    val minX = minXAccum.value
    val maxX = maxXAccum.value
    val minY = minYAccum.value
    val maxY = maxYAccum.value

    (minX, maxX, minY, maxY)
  }

  /**
   * Utility function to help using classes find the probably max requested
   * level, so as to help them calculate the required number of consolidation
   * partitions.
   */
  def getMaxLevel (args: Array[String]): Int =
    args.map(arg => {
      try {
        Some(arg.toInt)
      } catch {
        case e: Exception => None
      }
    }).filter(_.isDefined).map(_.get).reduce(_ max _)


  /**
   * Actually bin the required data
   */
  def doBinning (sc: SparkContext,
                 tileIO: TileIO,
                 pyramidName: String,
                 xVar: String,
                 yVar: String,
                 resultField: String,
                 resultFcn: T => ValueOrException[Double],
                 levelSets: Seq[Seq[Int]],
                 consolidationPartitions: Option[Int]): Unit = {
    // localize all fields so binner doesn't need to be serializable
    val localParser = parser
    val localExtractor = extractor

    if (debug) {
      println("Pyramid name: "+pyramidName)
      println("X variable: "+xVar)
      println("Y variable: "+yVar)
      println("Z variable: "+resultField)
      println("Level sets:")
      levelSets.foreach(levelSet => println("\t"+levelSet.mkString(", ")))
    }


    val startTime = System.currentTimeMillis()

    var rawData = source.getData(sc)


    val data = rawData.mapPartitions(iter => 
      // Parse the records from the raw data
      localParser.parseRecords(iter, xVar, yVar)
    ).filter(r =>
      // Filter out unsuccessful parsings
      r.hasValue
    ).map(_.get)

    val (minX, maxX, minY, maxY) =
      if (execute) {
        getAxisBounds(data, localExtractor.getFieldValue(xVar), localExtractor.getFieldValue(yVar))
      } else {
        (Double.NaN, Double.NaN, Double.NaN, Double.NaN)
      }

    val maxLevel = levelSets.map(_.reduce(_ max _)).reduce(_ max _)
    // include a fraction of a bin extra in the bounds, so the max goes on the 
    // right side of the last tile, rather than forming an extra tile.
    val epsilon = (1.0/(1 << maxLevel))/(256.0*256.0)
    val newXMax = maxX+(maxX-minX)*epsilon
    val newYMax = maxY+(maxY-minY)*epsilon
    println("\n\n\nGot bounds: %.4f to %.4f (%.4f) x, %.4f to %.4f (%.4f) y".format(minX, maxX, newXMax, minY, maxY, newYMax))

    // Actually do binning
    val pyramider = localExtractor.getTilePyramid(xVar, minX, newXMax,
                                                  yVar, minY, newYMax)
    val binner = new RDDBinner
    val table = pyramidName+"."+xVar+"."+yVar+(if ("count".equals(resultField)) "" else "."+resultField)

    binner.debug = debug
   
    if (execute) {
      println("Running binner")
      val getSearchRecords = (iter: Iterator[T]) => {
        iter.map(t => (localExtractor.getFieldValue(xVar)(t),
                       localExtractor.getFieldValue(yVar)(t),
                       resultFcn(t)))
      }
      binner.binAndWriteData(data,
                             getSearchRecords,
                             new StandardDoubleBinDescriptor,
                             pyramider,
                             consolidationPartitions,
                             table, tileIO,
                             levelSets,
                             name = table,
                             description = "Binned "+pyramidName+" data showing "+xVar+" vs. "+yVar)
      println("Done running binner")
    }

    val endTime = System.currentTimeMillis()

    if (debug) {
      println("Extracting "+pyramidName+" tiles")
      println("\tX axis: "+xVar)
      println("\tY axis: "+yVar)
      println("\tResult field: "+resultField)
      println("\tLevels: "+levelSets.map(_.mkString(", ")).mkString("; "))
      println("X axis: %s\tmin=%.4f\tmax=%.4f".format(xVar, minX, maxX))
      println("Y axis: %s\tmin=%.4f\tmax=%.4f".format(yVar, minY, maxY))
      println("Wrote to table "+table)
      println("Total processing time (including connection): "
              +((endTime-startTime)/60000.0)+" minutes")
    }
  }
}



/**
 * A helper class encapsulating how the data is retrieved.  This does not need
 * to be serializable.
 */
trait DataSource {
  /**
   * Get the source location of the data to be binned.  This is the only method
   * that needs to be overridden, and is used when the data is in a simple file
   * or set of files.
   */
  def getDataFiles: Seq[String]

  /**
   * Get the ideal number of partitions into which the data should be split.
   * 
   * None indicates that it doesn't matter, and the binner should just use the
   * default.
   *
   * The default is None.
   */
  def getIdealPartitions: Option[Int] = None

  /**
   * Actually retrieve the data.
   * This can be overridden if the data is not a simple file or set of files,
   * but normally shouldn't be touched.
   */
  def getData (sc: SparkContext): RDD[String] =
    if (getIdealPartitions.isDefined) {
      getDataFiles.map(sc.textFile(_, getIdealPartitions.get)).reduce(_ union _)
    } else {
      getDataFiles.map(sc.textFile(_)).reduce(_ union _)
    }
}



/**
 * A helper class to parse raw data lines into useful records.  This does need
 * to be serializable.
 */
abstract class RecordParser[T: ClassManifest] extends Serializable {
  /**
   * Parse a partition a raw data file into instances of the record type we
   * want.
   *
   * The variables that will be needed are passed in so that extra lengthy
   * parsing can be avoided on unused fields
   *
   * @param raw
   *        The raw string form of the record
   * @param variables
   *        The variables to be extracted
   */
  def parseRecords (raw: Iterator[String], Variables: String*): Iterator[ValueOrException[T]]
}



/**
 * A helper class to extract numeric information from records, so that the
 * results can actually be binned.  This does need to be serializable.
 */
abstract class FieldExtractor[T: ClassManifest] extends Serializable {
  /**
   * Used only for telling the user what possibilities are allowed, so these
   * strings can include comments and explanations
   */
  def getValidFieldList: List[String]
  /**
   * Determine if a field is a valid axis variable.
   */
  def isValidField (field: String): Boolean
  /**
   * Specifies if a given field always returns a constant value.  If the Y
   * variable always returns a constant value, a density strip tile will be
   * used instead of a standard tile.
   */
  def isConstantField (field: String): Boolean
  /**
   * Determine the value of a single axis variable of a single record.  The
   * results of this are undefined if isValidField(field) is false.
   */
  def getFieldValue (field: String)(record: T): ValueOrException[Double]


  /**
   * Get the pyramidding scheme to be used with the given X and Y axis.
   *
   * The default is to create an Area-of-Interest tile pyramid based on the
   * passed-in bounds.
   */
  def getTilePyramid (xField: String, minX: Double, maxX: Double,
                      yField: String, minY: Double, maxY: Double): TilePyramid =
    new AOITilePyramid(minX, minY, maxX, maxY)
}
