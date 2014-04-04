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

import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapred.TableOutputFormat
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.mapred.JobConf


import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.rdd.NewHadoopRDD

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.impl.HBasePyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.util.ArgumentParser



/**
 * Read and write tiles from HBase
 * 
 * When reading from or writing to any cluster-based storage mechanism, for 
 * efficiency's sake, we want to pay attention to data locality - i.e., we want 
 * to read data on the machine the data is on, and write it directly to the 
 * machine where it goes.
 * 
 * In the case of HBase, this requires overriding small portions of several 
 * basic TileIO methods - which essentially requires us to simply rewrite them.
 */
class HBaseTileIO (zookeeperQuorum: String,
                   zookeeperPort: String,
                   hbaseMaster: String) extends TileIO {
	// We are going to need access to HBasePyramidIO constants and static
	// methods, for column names, and row ID formation and parsing.
	import com.oculusinfo.binning.io.impl.HBasePyramidIO._



	// We still use the basic HBasePyramidIO for table initialization and
	// the like.  Plus, the basic TileIO interface requires we implement
	// this.
	def getPyramidIO : HBasePyramidIO =
		new HBasePyramidIO(zookeeperQuorum, zookeeperPort, hbaseMaster)




	/**
	 * Read a set of tiles.
	 * 
	 * We can do a little better than the standard; if levels is null, we can 
	 * just read all tiles.
	 * 
	 * Note that this uses the new Hadoop API
	 */
	override def readTileSet[T] (sc: SparkContext,
	                             serializer: TileSerializer[T],
	                             baseLocation: String,
	                             levels: Seq[Int]): RDD[TileData[T]] = {
		val pyramidIO = getPyramidIO

		// We need some TableInputFormat constants in here.
		import org.apache.hadoop.hbase.mapred.TableInputFormat._

		val conf = getPyramidIO.getConfiguration()
		conf.set(TableInputFormat.INPUT_TABLE, baseLocation)
		val admin = new HBaseAdmin(conf)

		if (!admin.isTableAvailable(baseLocation)) {
			sc.parallelize(List[TileData[T]]())
		} else {
			val hBaseRDD = sc.newAPIHadoopRDD(conf,
			                                  classOf[TableInputFormat],
			                                  classOf[ImmutableBytesWritable],
			                                  classOf[Result])
			val tiles = hBaseRDD.map(_._2).map(result =>
				{
					val key = new String(result.getRow())
					val value = result.getValue(TILE_COLUMN.getFamily(),
					                            TILE_COLUMN.getQualifier())
					serializer.deserialize(
						tileIndexFromRowId(key),
						new ByteArrayInputStream(value)
					)
				}
			)

			if (null == levels) {
				tiles
			} else {
				val levelSet = levels.toSet
				tiles.filter(tile => levelSet.contains(tile.getDefinition().getLevel()))
			}
		}
	}


	/**
	 * Write a tile set directly to HBase.
	 * 
	 * Note that this uses the old Hadoop API
	 */
	override def writeTileSet[PT, BT] (pyramider: TilePyramid,
	                                   baseLocation: String,
	                                   data: RDD[TileData[BT]],
	                                   binDesc: BinDescriptor[PT, BT],
	                                   name: String = "unknown",
	                                   description: String = "unknown"): Unit = {
		val pyramidIO = getPyramidIO

		// We need some TableOutputFormat constants in here.
		import org.apache.hadoop.hbase.mapred.TableOutputFormat._

		// Do any needed table initialization
		pyramidIO.initializeForWrite(baseLocation)

		// Set up some accumulators to figure out needed metadata
		val minMaxAccumulable = new LevelMinMaxAccumulableParam[BT](binDesc.min,
		                                                            binDesc.defaultMin,
		                                                            binDesc.max,
		                                                            binDesc.defaultMax)
		val minMaxAccum = data.context.accumulable(minMaxAccumulable.zero(Map()))(minMaxAccumulable)
		// And this is just for reporting, because it's basically free and easy
		val tileCount = data.context.accumulator(0)

		// Turn each tile into a table row, noting mins, maxes, and counts as
		// we go.  Note that none of the min/max/count accumulation is actually
		// done until the file is writting - this just sets it up, it doesn't
		// run it
		val HBaseTiles = data.map(tile =>
			{
				val index = tile.getDefinition()
				val level = index.getLevel()

				// Update count, minimums, and maximums
				tileCount += 1
				for (x <- 0 until index.getXBins())
					for (y <- 0 until index.getYBins())
						minMaxAccum += (level -> tile.getBin(x, y))

				// Create a Put (a table write object) that will write this tile
				val baos = new ByteArrayOutputStream()
				binDesc.getSerializer.serialize(tile, pyramider, baos);
				baos.close
				baos.flush

				val put = new Put(rowIdFromTileIndex(index).getBytes())
				put.add(TILE_COLUMN.getFamily(),
				        TILE_COLUMN.getQualifier(),
				        baos.toByteArray())

				(new ImmutableBytesWritable, put)
			}
		)


		// Configure our write job
		val configuration = pyramidIO.getConfiguration()

		val jobConfig = new JobConf(configuration, this.getClass)
		jobConfig.setOutputFormat(classOf[TableOutputFormat])
		jobConfig.set(TableOutputFormat.OUTPUT_TABLE, baseLocation)

		// Write tiles.
		// This also populates the count, min, and max accumulators set up
		// above.
		HBaseTiles.saveAsHadoopDataset(jobConfig)
		println("Input tiles: "+tileCount)



		// Now that we've written tiles  (therefore actually run our data mapping),
		// our accumulators should be set, and we can update our metadata
		println("Calculating metadata")
		// Don't alter metadata if there was no data added.
		// Ideally, we'd still alter levels, but that's stored in our min/max list,
		// so since we have no min/max info for levels with no data, we just don't store them.
		if (tileCount.value > 0) {
			val oldMetaData = readMetaData(baseLocation)

			val sampleTile = data.first.getDefinition()
			val tileSize = sampleTile.getXBins()

			val scheme = pyramider.getTileScheme()
			val projection = pyramider.getProjection()
			val bounds = pyramider.getTileBounds(new TileIndex(0, 0, 0))

			val minMax = minMaxAccum.value

			var metaData = oldMetaData match {
				case None => {
					new TileMetaData(name, description, tileSize, scheme, projection,
					                 minMax.keys.min, minMax.keys.max,
					                 bounds,
					                 minMax.map(p =>
						                 (p._1, p._2._1.toString)).toList.sortBy(_._1),
					                 minMax.map(p =>
						                 (p._1, p._2._2.toString)).toList.sortBy(_._1))
				}
				case Some(metaData) => {
					var newMetaData = metaData
					minMax.foreach(mm =>
						newMetaData = newMetaData.addLevel(mm._1,
						                                   mm._2._1.toString,
						                                   mm._2._2.toString)
					)
					newMetaData
				}
			}
			writeMetaData(baseLocation, metaData)
		}
	}
}


object CountHBaseRowsByLevel {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(Array("-io", "hbase") ++ args)
		val tileIO = TileIO.fromArguments(argParser)
		val serializer = TileSerializerChooser.fromArguments(argParser)
		val table = argParser.getString("table", "The name of the table to read")
		val sc = argParser.getSparkConnector().getSparkContext("Testing table equality")

		tileIO.readTileSet(sc, serializer, table, null).map(tile =>
			{
				(tile.getDefinition().getLevel(), 1)
			}
		).reduceByKey(_ + _).collect.foreach(println(_))
	}
}
