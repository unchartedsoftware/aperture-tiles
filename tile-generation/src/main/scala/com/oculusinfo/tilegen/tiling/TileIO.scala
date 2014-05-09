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
import java.io.File
import scala.collection.JavaConversions._
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
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer
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
		argParser.getString("io",
		                    "TileIO type - either hbase "
			                    +"or file (DEFAULT).\n",
		                    Some("file")) match {
			case "hbase" => new HBaseTileIO(
				argParser.getString("zookeeperquorum",
				                    "The name of the zookeeper quorum machine"),
				argParser.getString("zookeeperport",
				                    "The port on which zookeeper is listening",
				                    Some("2181")),
				argParser.getString("hbasemaster",
				                    "The master machine for hbase"));
			case _ => new LocalTileIO(
				argParser.getString("tileextension",
				                    "The extension used for each tile file.  Default is \"avro\"",
				                    Some("avro")))
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
	def writeTileSet[PT, BT] (pyramider: TilePyramid,
	                          baseLocation: String,
	                          data: RDD[TileData[BT]],
	                          binDesc: BinDescriptor[PT, BT],
	                          name: String = "unknown",
	                          description: String = "unknown"): Unit = {
		// Do any needed initialization
		getPyramidIO.initializeForWrite(baseLocation)

		// Set up some accumulators to figure out needed metadata
		val minMaxAccumulable = new LevelMinMaxAccumulableParam[BT](binDesc.min,
		                                                            binDesc.defaultMin,
		                                                            binDesc.max,
		                                                            binDesc.defaultMax)
		val minMaxAccum = data.context.accumulable(minMaxAccumulable.zero(Map()))(minMaxAccumulable)
		// And this is just for reporting, because it's basically free and easy
		val tileCount = data.context.accumulator(0)

		println("Writing tile set from")
		println(data.toDebugString)

		// write each tile, storing away info we'll need to write the metadata
		data.mapPartitions(_.grouped(1024)).foreach(group =>
			{
				val pyramidIO = getPyramidIO
				// Write out tje group of tiles
				pyramidIO.writeTiles(baseLocation, pyramider, binDesc.getSerializer, group)

				// And collect stats on them
				group.foreach(tile =>
					{
						val index = tile.getDefinition()

						// Update minimum and maximum values for metadata
						val level = index.getLevel()
						tileCount += 1
						for (x <- 0 until index.getXBins()) {
							for (y <- 0 until index.getYBins()) {
								minMaxAccum += (level -> tile.getBin(x, y))
							}
						}
					}
				)
			}
		)

		// Calculate overall tile set characteristics
		val sampleTile = data.first.getDefinition()
		val tileSize = sampleTile.getXBins()
		val bounds = pyramider.getTileBounds(new TileIndex(0, 0, 0))

		val projection = pyramider.getProjection()
		val scheme = pyramider.getTileScheme()
		val minMax = minMaxAccum.value
		val oldMetaData = readMetaData(baseLocation)

		println("Calculating metadata")
		println("Input tiles: "+tileCount)

		var metaData = oldMetaData match {
			case None => {
				new TileMetaData(name, description, tileSize, scheme, projection,
				                 minMax.keys.min, minMax.keys.max,
				                 bounds,
				                 minMax.map(p => (p._1, p._2._1.toString)).toList.sortBy(_._1),
				                 minMax.map(p => (p._1, p._2._2.toString)).toList.sortBy(_._1))
			}
			case Some(metaData) => {
				var newMetaData = metaData
				minMax.foreach(mm =>
					newMetaData = newMetaData.addLevel(mm._1, mm._2._1.toString, mm._2._2.toString)
				)
				newMetaData
			}
		}

		writeMetaData(baseLocation, metaData)
	}

	def readMetaData (baseLocation: String): Option[TileMetaData] = {
		TileMetaData.parse(getPyramidIO.readMetaData(baseLocation))
	}

	def writeMetaData (baseLocation: String, metaData: TileMetaData): Unit =
		getPyramidIO.writeMetaData(baseLocation, metaData.toString)
}

class LevelMinMaxAccumulableParam[T] (minFcn: (T, T) => T, defaultMin: T,
                                      maxFcn: (T, T) => T, defaultMax: T)
		extends AccumulableParam[Map[Int, (T, T)], (Int, T)]
		with Serializable {
	private val defaultValue = (defaultMin, defaultMax)
	def addAccumulator (currentValue: Map[Int, (T, T)],
	                    addition: (Int, T)): Map[Int, (T, T)] = {
		val level = addition._1
		val value = addition._2
		val curMinMax = currentValue.getOrElse(level, defaultValue)
		currentValue + ((level, (minFcn(curMinMax._1, value), maxFcn(curMinMax._2, value))))
	}

	def addInPlace (a: Map[Int, (T, T)], b: Map[Int, (T, T)]): Map[Int, (T, T)] = {
		val keys = a.keySet union b.keySet
		keys.map(key =>
			{
				val aVal = a.getOrElse(key, defaultValue)
				val bVal = b.getOrElse(key, defaultValue)

				(key -> (minFcn(aVal._1, bVal._1), maxFcn(aVal._2, bVal._2)))
			}
		).toMap
	}

	def zero (initialValue: Map[Int, (T, T)]): Map[Int, (T, T)] =
		Map[Int, (T, T)]()
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
		testEquality("Tile sizes",    metaData1.tileSize,   metaData2.tileSize)
		testEquality("Schemes",       metaData1.scheme,     metaData2.scheme)
		testEquality("Projections",   metaData1.projection, metaData2.projection)
		testEquality("Minimum zooms", metaData1.minZoom,    metaData2.minZoom)
		testEquality("Maximum zooms", metaData1.maxZoom,    metaData2.maxZoom)
		testEquality("Bounds",        metaData1.bounds,     metaData2.bounds)
		testEquality("Number of minimum entries",
		             metaData1.levelMins.size, metaData2.levelMins.size)
		Range(0, metaData1.levelMins.size min metaData2.levelMins.size).foreach(n =>
			{
				testEquality("Min frequency entry "+n+" level",
				             metaData1.levelMins(n)._1,
				             metaData2.levelMins(n)._1)
				testEquality("Min frequency entry "+n+" frequency",
				             metaData1.levelMins(n)._2,
				             metaData2.levelMins(n)._2)
			}
		)
		testEquality("Number of maximum entries",
		             metaData1.levelMaxes.size, metaData2.levelMaxes.size)
		Range(0, metaData1.levelMaxes.size min metaData2.levelMaxes.size).foreach(n =>
			{
				testEquality("Maximum entry "+n+" level",
				             metaData1.levelMaxes(n)._1,
				             metaData2.levelMaxes(n)._1)
				testEquality("Maximum entry "+n+" value",
				             metaData1.levelMaxes(n)._2,
				             metaData2.levelMaxes(n)._2)
			}
		)
		println("Tile differences:")
		if (collectedDiffs.isEmpty)
			println("No differences found!")
		else {
			println("Differences: ")
			collectedDiffs.foreach(println(_))
		}
	}
}
