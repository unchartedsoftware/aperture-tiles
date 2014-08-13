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
import java.lang.{Integer => JavaInt}
import java.io.File

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.{HashSet => MutableSet}
import scala.util.{Try, Success, Failure}

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.avro.file.CodecFactory

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.impl.FileSystemPyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.StringArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.StringDoublePairArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.tilegen.datasets.ValueDescription
import com.oculusinfo.tilegen.spark.IntMaxAccumulatorParam
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import com.oculusinfo.binning.io.impl.SQLitePyramidIO







object TileIO {
	/**
	 * Just get the default type of TileIO one would get with an empty argument
	 * list.
	 */
	def default: TileIO =
		fromArguments(new ArgumentParser(Array[String]()))

	/**
	 * A standard way of creating a tile IO from command-line arguments
	 */
	def fromArguments (argParser: KeyValueArgumentSource): TileIO = {
		argParser.getString(Array("io", "oculus.tileio.type"),
		                    "TileIO type - where to put tiles.  Legal values are "+
			                    "hbase, sqlite, or file (DEFAULT).",
		                    Some("file")
		) match {
			case "hbase" => new HBaseTileIO(
				argParser.getString(Array("zookeeperquorum", "hbase.zookeeper.quorum"),
				                    "The name of the zookeeper quorum machine",
				                    None),
				argParser.getString(Array("zookeeperport", "hbase.zookeeper.port"),
				                    "The port on which zookeeper is listening",
				                    Some("2181")),
				argParser.getString(Array("hbasemaster", "hbase.master"),
				                    "The master machine for hbase",
				                    None)
			)
			case "sqlite" => new SqliteTileIO(
				argParser.getString(Array("sqlitepath", "oculus.tileio.sqlite.path"),
				                    "The path to the sqlite database",
				                    Some(""))
			)
			case _ => new LocalTileIO(
				argParser.getString(Array("tileextension", "oculus.tileio.file.extension"),
				                    "The extension used for each tile file.  Default is "+
					                    "\"avro\"",
				                    Some("avro"))
			)
		}
	}
}

trait TileIO extends Serializable {
	/**
	 * Method to get the i/o class used to read and write pyramids for this IO scheme
	 */
	def getPyramidIO : PyramidIO

	/**
	 * Read a set of tiles, of the indicated levels
	 */
	def readTileSet[T] (sc: SparkContext,
	                    serializer: TileSerializer[T],
	                    baseLocation: String,
	                    levels: Seq[Int]): RDD[TileData[T]] = {
		val tileSets: Seq[RDD[TileIndex]] = levels.map(level =>
			{
				val range = sc.parallelize(Range(0, 1 << level),
				                           1 << ((level-10) max 0))
				range.cartesian(range).map(p => new TileIndex(level, p._1, p._2))
			}
		)
		val tiles: RDD[TileIndex] = tileSets.reduce(_ union _)
		tiles.mapPartitions(iter =>
			{
				val pyramidIO = getPyramidIO
				// read tiles in large, but manageable, groups
				val giter = iter.grouped(1024)
				giter.flatMap(someTiles =>
					{
						pyramidIO.readTiles(baseLocation, serializer, someTiles)
					}
				)
			}
		)
	}

	/**
	 * Write all tiles contained in the given data
	 */
	def writeTileSet[BT, AT, DT] (pyramider: TilePyramid,
	                              baseLocation: String,
	                              data: RDD[TileData[BT]],
	                              valueDesc: ValueDescription[BT],
	                              tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	                              dataAnalytics: Option[AnalysisDescription[_, DT]],
	                              name: String = "unknown",
	                              description: String = "unknown"): Unit = {

		// Do any needed initialization
		getPyramidIO.initializeForWrite(baseLocation)

		// Record and report the total number of tiles we write, because it's
		// basically free and easy
		val tileCount = data.context.accumulator(0)
		// Record all levels we write
		val levelSet = data.context.accumulableCollection(MutableSet[Int]())
		// record tile sizes
		val xbins = data.context.accumulator(0)(new IntMaxAccumulatorParam)
		val ybins = data.context.accumulator(0)(new IntMaxAccumulatorParam)

		println("Writing tile set from")
		println(data.toDebugString)
		println("Writing tile set to "+baseLocation)

		// write each tile, storing away info we'll need to write the metadata
		data.mapPartitions(_.grouped(1024)).foreach(group =>
			{
				val pyramidIO = getPyramidIO
				val serializer = valueDesc.getSerializer
				// Write out tje group of tiles
				pyramidIO.writeTiles(baseLocation, serializer, group)

				// And collect stats on them
				group.foreach(tile =>
					{
						val index = tile.getDefinition()
						val level = index.getLevel()

						// Update count, level bounds, tile sizes
						tileCount += 1
						levelSet += level
						xbins += index.getXBins
						ybins += index.getYBins
					}
				)
			}
		)
		println("Input tiles: "+tileCount)
		println("X bins: "+xbins.value)
		println("Y bins: "+ybins.value)
		println("Input levels: "+levelSet.value)

		// Don't alter metadata if there was no data added.
		// Ideally, we'd still alter levels.
		if (tileCount.value > 0) {
			val metaData =
				combineMetaData(pyramider, baseLocation,
				                levelSet.value.toSet,
				                tileAnalytics, dataAnalytics,
				                xbins.value, ybins.value,
				                name, description)
			writeMetaData(baseLocation, metaData)
		}
	}
	
	/**
	 * Takes a map of levels to (mins, maxes) and combines them with the current metadata
	 * that already exists, or creates a new one if none exists.
	 */
	def combineMetaData[BT, DT, AT](pyramider: TilePyramid,
	                                baseLocation: String,
	                                newLevels: Set[Int],
	                                tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	                                dataAnalytics: Option[AnalysisDescription[_, DT]],
	                                tileSizeX: Int,
	                                tileSizeY: Int,
	                                name: String = "unknown",
	                                description: String = "unknown"): PyramidMetaData = {
		val bounds = pyramider.getTileBounds(new TileIndex(0, 0, 0))
		val projection = pyramider.getProjection()
		val scheme = pyramider.getTileScheme()
		val oldMetaData = readMetaData(baseLocation)

		var metaData = oldMetaData match {
			case None => {
				new PyramidMetaData(name, description, tileSizeX, tileSizeY,
				                    scheme, projection,
				                    newLevels.toList.sorted.map(new JavaInt(_)).asJava,
				                    bounds,
				                    null, null)
			}
			case Some(metaData) => {
				metaData.addValidZoomLevels(newLevels.map(new JavaInt(_)))
				metaData
			}
		}
		tileAnalytics.map(_.applyTo(metaData))
		dataAnalytics.map(_.applyTo(metaData))

		metaData
	}

	def readMetaData (baseLocation: String): Option[PyramidMetaData] =
		try {
			Some(new PyramidMetaData(getPyramidIO.readMetaData(baseLocation)))
		} catch {
			case e: Exception => None
		}

	def writeMetaData (baseLocation: String, metaData: PyramidMetaData): Unit =
		getPyramidIO.writeMetaData(baseLocation, metaData.toString)
	
}


/**
 * Read and write tiles from the local file system
 */
class LocalTileIO (extension: String) extends TileIO {
	def getPyramidIO : PyramidIO =
		new FileSystemPyramidIO("", extension)
}


/**
 * Read and write tiles from the sqlite db
 */
class SqliteTileIO (path: String) extends TileIO {
	def getPyramidIO : PyramidIO =
		new SQLitePyramidIO(path)
}





object TileSerializerChooser {
	def fromArguments (argParser: KeyValueArgumentSource): TileSerializer[_] =
		getSerializer(argParser.getString("serializer",
		                                  "The type of tile serializer to use",
		                                  Some("avro-double")))

	def getSerializer (serializerType: String): TileSerializer[_] =
		serializerType match {
			case "legacy" => new BackwardCompatibilitySerializer()
			case "avro-double" => new DoubleAvroSerializer(CodecFactory.bzip2Codec())
			case "avro-double-array" => new DoubleArrayAvroSerializer(CodecFactory.bzip2Codec())
			case "avro-string-array" => new StringArrayAvroSerializer(CodecFactory.bzip2Codec())
			case "avro-string-int-pair-array" => new StringIntPairArrayAvroSerializer(CodecFactory.bzip2Codec())
			case "avro-string-double-pair-array" => new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
			case _ => throw new IllegalArgumentException("Illegal serializer type "+serializerType)
		}
}




/**
 * Read and rewrite metadata for a table.
 * 
 * This should update the metadata to the latest format, in case of a format change.
 */
object FixMetaData {
	def main (args: Array[String]): Unit = {
		val tileIO = TileIO.fromArguments(new ArgumentParser(args))
		val table = args(1)

		val metaData = tileIO.readMetaData(table)
		if (metaData.isDefined)
			tileIO.writeMetaData(table, metaData.get)
	}
}

/**
 * Test that two sets of tables are identical
 */
object TestTableEquality {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)
		val tileIO = TileIO.fromArguments(argParser)
		val sc = argParser.getSparkConnector().getSparkContext("Testing table equality")

		val serializerType = args(1)
		val table1: String = args(2)
		val table2: String = args(3)
		val levels = Range(4, args.length).map(n => args(n).toInt).toList

		println("Comparing tables "+table1+" and "+table2+" for levels "+levels.mkString(",")+" with serialization type "+serializerType)

		if ("legacy" == serializerType) {
			compareTables[JavaDouble](sc, tileIO,
			                          table1, new BackwardCompatibilitySerializer(),
			                          table2, new DoubleAvroSerializer(CodecFactory.bzip2Codec()),
			                          levels)
		} else {
			val serializer = TileSerializerChooser.getSerializer(args(2))
			compareTables(sc, tileIO, serializer, table1, table2, levels)
		}
	}

	def compareTables[T] (sc: SparkContext,
	                      tileIO: TileIO,
	                      serializer: TileSerializer[T],
	                      table1: String,
	                      table2: String,
	                      levels: Seq[Int]): Unit =
		compareTables(sc, tileIO, table1, serializer, table2, serializer, levels)

	def compareTables[T] (sc: SparkContext,
	                      tileIO: TileIO,
	                      table1: String,
	                      serializer1: TileSerializer[T],
	                      table2: String,
	                      serializer2: TileSerializer[T],
	                      levels: Seq[Int]): Unit = {
		// first check if meta-data is the same
		val metaData1 = tileIO.readMetaData(table1).get
		val metaData2 = tileIO.readMetaData(table2).get

		val tiles1 = tileIO.readTileSet(sc, serializer1, table1, levels).map(data =>
			(data.getDefinition(), (1, data))
		)
		val tiles2 = tileIO.readTileSet(sc, serializer2, table2, levels).map(data =>
			(data.getDefinition(), (2, data))
		)

		val differences = tiles1.union(tiles2).groupByKey().map(record =>
			{
				val index = record._1
				val tiles = record._2.toMap
				if (tiles.contains(1) && tiles.contains(2)) {
					val tile1 = tiles(1)
					val tile2 = tiles(2)

					val errors = Range(0, index.getXBins()).flatMap(x =>
						Range(0, index.getYBins()).map(y =>
							{
								if (tile1.getBin(x, y) == tile2.getBin(x, y)) None
								else Some("Bin["+x+","+y+"] differ!")
							}
						)
					).filter(_.isDefined).map(_.get)

					if (errors.isEmpty) None
					else Some(index)
				} else if (tiles.contains(1) || tiles.contains(2)) {
					Some(index)
				} else {
					None
				}
			}
		).filter(_.isDefined).map(_.get)

		val collectedDiffs = differences.collect()


		def testEquality[T] (description: String, a: T, b: T): Unit =
			if (!a.equals(b)) println(description+" differ") else println(description+" match")
		testEquality("Tile size (x)", metaData1.getTileSizeX(),  metaData2.getTileSizeX())
		testEquality("Tile size (y)", metaData1.getTileSizeY(),  metaData2.getTileSizeY())
		testEquality("Schemes",       metaData1.getScheme(),     metaData2.getScheme())
		testEquality("Projections",   metaData1.getProjection(), metaData2.getProjection())
		testEquality("Minimum zooms", metaData1.getMinZoom(),    metaData2.getMinZoom())
		testEquality("Maximum zooms", metaData1.getMaxZoom(),    metaData2.getMaxZoom())
		testEquality("Bounds",        metaData1.getBounds(),     metaData2.getBounds())
		println("Tile differences:")
		if (collectedDiffs.isEmpty)
			println("No differences found!")
		else {
			println("Differences: ")
			collectedDiffs.foreach(println(_))
		}
	}
}
