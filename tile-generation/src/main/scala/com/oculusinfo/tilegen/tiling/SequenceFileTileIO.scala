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



import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{HashSet => MutableSet}
import scala.util.Try

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.BytesWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.SequenceFile

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.impl.FileBasedPyramidIO
import com.oculusinfo.binning.io.impl.FileSystemPyramidSource
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData

import com.oculusinfo.tilegen.spark.IntMaxAccumulatorParam
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.MissingArgumentException






/**
 * Read and write tiles from HDFS
 * 
 * This version of TileIO reads and writes files directly from HDFS as 
 * SequenceFiles.
 * 
 * These are key/value pair files; the key will be the tile index, and the 
 * value, a binary blob of the tile data.
 * 
 * Sets of key/value pairs are written in numerically increasing 
 * subdirectories, so they can be read out the same way, with later writes
 * overwriting earlier.
 * 
 * MetaData is written as a separate file, in the base directory with the 
 * numerically increasing subdirectories.
 * 
 * @param connection The connection location at which to find HDFS 
 * (typcially "hdfs://hostname:port")
 */
class SequenceFileTileIO (connection: String) extends TileIO {
	import SequenceFileTileIO._


	private def fs = getFS(connection)

	def getFullLocation (baseLocation: String) =
		new Path(if (connection.endsWith("/")) connection+baseLocation
		         else connection+"/"+baseLocation)

	def getBlockLocation (block: Int, baseLocation: String) =
		getFullLocation(baseLocation).toString+"/"+block

	def checkBaseLocation (fs: FileSystem, baseLocation: String, create: Boolean = false): Unit = {
		val path = getFullLocation(baseLocation)
		if (!fs.exists(path)) {
			if (create) {
				fs.mkdirs(path)
			} else {
				throw new IllegalArgumentException("Base location "+baseLocation+" does not exist")
			}
		} else if (!fs.isDirectory(path)) {
			throw new IllegalArgumentException("Base location "+baseLocation+" is not a directory")
		}
	}





	def getPyramidIO : PyramidIO = null

	override def readTileSet[T] (sc: SparkContext,
	                             serializer: TileSerializer[T],
	                             baseLocation: String,
	                             levels: Seq[Int]): RDD[TileData[T]] = {
		// Find all numeric sub-directories of our base location
		checkBaseLocation(fs, baseLocation)
		val fullLocation = getFullLocation(baseLocation)

		val blocks = getBlocks(fs, fullLocation)
		blocks.map(block =>
			{
				sc.sequenceFile(block.getName, classOf[Text], classOf[BytesWritable], 1)
					.map(pair =>
					{
						val key = pair._1.toString
						val value = pair._2.getBytes
						val index = TileIndex.fromString(key)

						serializer.deserialize(index, new ByteArrayInputStream(value))
					}
				)
			}
		).fold(sc.emptyRDD[TileData[T]])(_ union _)
	}


	/**
	 * Write a tile set directly to an HDFS sequence file
	 */
	override def writeTileSet[BT, AT, DT] (pyramider: TilePyramid,
	                                       baseLocation: String,
	                                       data: RDD[TileData[BT]],
	                                       serializer: TileSerializer[BT],
	                                       tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	                                       dataAnalytics: Option[AnalysisDescription[_, DT]],
	                                       name: String = "unknown",
	                                       description: String = "unknown"): Unit = {
		checkBaseLocation(fs, baseLocation, true)

		// Record and report the total number of tiles we write, because it's
		// basically free and easy
		val tileCount = data.context.accumulator(0)
		// Record the levels we write
		val levelSet = data.context.accumulableCollection(MutableSet[Int]())
		// record tile sizes
		val xbins = data.context.accumulator(0)(new IntMaxAccumulatorParam)
		val ybins = data.context.accumulator(0)(new IntMaxAccumulatorParam)


		// Turn each tile into a table row, noting mins, maxes, and counts as
		// we go.  Note that none of the min/max/count accumulation is actually
		// done until the file is writting - this just sets it up, it doesn't
		// run it
		val tileSequence: RDD[(String, Array[Byte])] = data.mapPartitions(iter =>
			{
				iter.map(tile =>
					{
						val index = tile.getDefinition()
						val level = index.getLevel()

						// Update count, level bounds, tile sizes
						tileCount += 1
						levelSet += level
						xbins += index.getXBins
						ybins += index.getYBins

						// Create a Put (a table write object) that will write this tile
						val baos = new ByteArrayOutputStream()
						serializer.serialize(tile, baos);
						baos.close
						baos.flush


						(index.toString, baos.toByteArray)
					}
				)
			}
		)


		// Figure out where to put our new data
		val fullLocation = getFullLocation(baseLocation)
		val maxBlock = getBlocks(fs, fullLocation).map(block => getBlockNum(block)).fold(-1)(_ max _)
		val block = maxBlock+1
		val blockLocation = getBlockLocation(block, baseLocation)
		println("Saving block "+block+" to "+blockLocation)
		tileSequence.saveAsSequenceFile(blockLocation)



		// TODO: Actually write the data!
		// (And: figure out in how many partitions to write it)
		println("Input tiles: "+tileCount)
		println("Input levels: "+levelSet.value)
		println("X bins: "+xbins.value)
		println("Y bins: "+ybins.value)



		// Don't alter metadata if there was no data added.
		// Ideally, we'd still alter levels
		val metaData =
			combineMetaData(pyramider, baseLocation,
											levelSet.value.toSet,
											tileAnalytics, dataAnalytics,
											xbins.value, ybins.value,
											name, description)
		writeMetaData(baseLocation, metaData)
	}

	override def readMetaData (baseLocation: String): Option[PyramidMetaData] = {
		val metaDataLocation = getMetaDataPath(getFullLocation(baseLocation))
		if (fs.exists(metaDataLocation) && fs.isFile(metaDataLocation)) {
			val reader = new BufferedReader(new InputStreamReader(fs.open(metaDataLocation)))
			val metaData = Stream.continually(reader.readLine()).takeWhile(_ != null).mkString("\n")
			reader.close
			Some(new PyramidMetaData(metaData))
		} else {
			None
		}
	}


	override def writeMetaData (baseLocation: String, metaData: PyramidMetaData): Unit = {
		val metaDataLocation = getMetaDataPath(getFullLocation(baseLocation))
		val writer = new BufferedWriter(new OutputStreamWriter(fs.create(metaDataLocation, true),
		                                                       "UTF-8"))
		writer.write(metaData.toString)
		writer.flush
		writer.close
	}
}
object SequenceFileTileIO {
	def getFS (connection: String) = {
		val config = new Configuration
		config.set("fs.defaultFS", connection)
		FileSystem.newInstance(config)
	}

	def getBlockNum (block: Path): Int =
		block.getName.split("/").last.toInt

	def getBlocks (fs: FileSystem,
	               path: Path): List[Path] = {
		if (!fs.isDirectory(path))
			throw new IllegalArgumentException(
				"SequenceFileTileIO base locations must be a directory")

		val blocks = MutableList[Path]()
		val i = fs.listLocatedStatus(path)
		while (i.hasNext) {
			val next = i.next
			if (next.isDirectory) {
				val blockPath = next.getPath
				Try(getBlockNum(blockPath)).foreach(blockNum => blocks += blockPath)
			}
		}

		blocks.toList
	}

	def getMetaDataPath (location: Path) =
		new Path(location, "metadata")

	def localizeFiles[T] (hdfsLoc: String, fsLoc: String, serializer: TileSerializer[T]): Unit = {
		// Make sure our source location exists
		val fs = getFS(hdfsLoc)
		val hdfsBase = new Path(hdfsLoc)
		if (!fs.exists(hdfsBase)) throw new IllegalArgumentException("Nonexistent source location "+hdfsLoc)

		// Set up our local io
		val fsio = new FileBasedPyramidIO(new FileSystemPyramidSource(null, "avro"))
		fsio.initializeForWrite(fsLoc)

		// Copy over metadata
		val metaDataPath = getMetaDataPath(hdfsBase)
		if (fs.exists(metaDataPath) && fs.isFile(metaDataPath)) {
			val reader = new BufferedReader(new InputStreamReader(fs.open(metaDataPath)))
			val metaData = Stream.continually(reader.readLine()).takeWhile(_ != null).mkString("\n")
			fsio.writeMetaData(fsLoc, metaData)
			reader.close
		}

		// Write blocks over, one by one
		getBlocks(fs, hdfsBase).sortBy(getBlockNum(_)).foreach(block =>
			{
				println("Looking at block "+block)
				val fileIter = fs.listFiles(block, false)
				while (fileIter.hasNext) {
					val status = fileIter.next
					val path = status.getPath
					if ("_SUCCESS" != path.getName) {
						val reader = new SequenceFile.Reader(fs.getConf, SequenceFile.Reader.file(path))
						val key = new Text
						val value = new BytesWritable
						while (reader.next(key, value)) {
							val index = TileIndex.fromString(key.toString)
							val bais = new ByteArrayInputStream(value.getBytes)
							val tile = serializer.deserialize(index, bais)
							fsio.writeTiles(fsLoc, serializer, List(tile).asJava)
						}
						reader.close
					}
				}
			}
		)

	}
}

object LocalizeTileSet {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)
		try {
			val source = argParser.getString("source",
			                                 "The full source location of the tile set in HDFS")
			val destination = argParser.getString("dest",
			                                      "The location to which to write the tile set on "+
				                                      "the local file system, relative to the "+
				                                      "current working directory from which the "+
				                                      "localizer is run")
			val serializer = TileSerializerChooser.fromArguments(argParser)

			SequenceFileTileIO.localizeFiles(source, destination, serializer)
		} catch {
			case e: MissingArgumentException => {
				println("LocalizeTileSet - pull a sequence-file-based tile set from HBase into a local ")
				println("file system")
				println("Argument exception: "+e.getMessage())
				argParser.usage
			}
		}
	}
}
