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
import java.lang.{Long => JavaLong}
import java.io.File
import org.apache.avro.util.Utf8

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
import com.oculusinfo.binning.io.impl.FileBasedPyramidIO
import com.oculusinfo.binning.io.impl.FileSystemPyramidSource
import com.oculusinfo.binning.io.impl.SQLitePyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.PairArrayAvroSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.factory.util.Pair
import com.oculusinfo.tilegen.spark.IntMaxAccumulatorParam
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer







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
	                              serializer: TileSerializer[BT],
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
		val metaData =
			combineMetaData(pyramider, baseLocation,
											levelSet.value.toSet,
											tileAnalytics, dataAnalytics,
											xbins.value, ybins.value,
											name, description)
		writeMetaData(baseLocation, metaData)
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
		tileAnalytics.map(AnalysisDescription.record(_, metaData))
		dataAnalytics.map(AnalysisDescription.record(_, metaData))

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
class LocalTileIO (extension: String = "avro", baseLocation: String = "") extends TileIO {
	def getPyramidIO : PyramidIO =
		new FileBasedPyramidIO(new FileSystemPyramidSource(baseLocation, extension))
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
			case "legacy" => new com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer()
			case "avro-double" => new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
			case "avro-int" => new PrimitiveAvroSerializer(classOf[JavaInt], CodecFactory.bzip2Codec())
			case "avro-long" => new PrimitiveAvroSerializer(classOf[JavaLong], CodecFactory.bzip2Codec())
			case "avro-double-array" => new PrimitiveArrayAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
			case "avro-string-array" => new PrimitiveArrayAvroSerializer(classOf[Utf8], CodecFactory.bzip2Codec())
			case "avro-string-int-pair-array" => new PairArrayAvroSerializer(classOf[String], classOf[JavaInt], CodecFactory.bzip2Codec())
			case "avro-string-double-pair-array" => new PairArrayAvroSerializer(classOf[String], classOf[JavaDouble], CodecFactory.bzip2Codec())
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
