/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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

import scala.collection.mutable.{HashSet => MutableSet}

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{ConnectionFactory, Result}

import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapred.TableOutputFormat
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.mapred.JobConf


import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.impl.{HBaseSlicedPyramidIO, HBasePyramidIO}
import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.spark.IntMaxAccumulatorParam
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
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
class HBaseTileIO ( zookeeperQuorum: String,
                    zookeeperPort: String,
                    hbaseMaster: String,
										slicing: Boolean = false) extends TileIO {
	// We are going to need access to HBasePyramidIO constants and static
	// methods, for column names, and row ID formation and parsing.
	import com.oculusinfo.binning.io.impl.HBasePyramidIO._



	// We still use the basic HBasePyramidIO for table initialization and
	// the like.  Plus, the basic TileIO interface requires we implement
	// this.
	def getPyramidIO : HBasePyramidIO =
		if (slicing) new HBaseSlicedPyramidIO(zookeeperQuorum, zookeeperPort, hbaseMaster)
		else new HBasePyramidIO(zookeeperQuorum, zookeeperPort, hbaseMaster)




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

		val conf = pyramidIO.getConfiguration()
		val connection = ConnectionFactory.createConnection(conf);
		conf.set(TableInputFormat.INPUT_TABLE, baseLocation)
		val admin = connection.getAdmin

		if (!admin.isTableAvailable(TableName.valueOf(baseLocation))) {
			sc.parallelize(List[TileData[T]]())
		} else {
			val hBaseRDD = sc.newAPIHadoopRDD(conf,
			                                  classOf[TableInputFormat],
			                                  classOf[ImmutableBytesWritable],
			                                  classOf[Result])
			val tiles = hBaseRDD.map(_._2).flatMap(result =>
                {
                    val key = new String(result.getRow())
                    if ("metadata" == key) None
                    else {
                        val value = result.getValue(TILE_COLUMN.getFamily(),
                                                    TILE_COLUMN.getQualifier())
                        Some(serializer.deserialize(
                            tileIndexFromRowId(key),
                            new ByteArrayInputStream(value)
                        ))
                    }
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
	override def writeTileSet[BT, AT, DT] (pyramider: TilePyramid,
	                                       baseLocation: String,
	                                       data: RDD[TileData[BT]],
	                                       serializer: TileSerializer[BT],
	                                       tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	                                       dataAnalytics: Option[AnalysisDescription[_, DT]],
	                                       name: String = "unknown",
	                                       description: String = "unknown"): Unit = {
		val pyramidIO = getPyramidIO

		// We need some TableOutputFormat constants in here.
		import org.apache.hadoop.hbase.mapred.TableOutputFormat._

		// Do any needed table initialization
		pyramidIO.initializeForWrite(baseLocation)

		// Record and report the total number of tiles we write, because it's
		// basically free and easy
		val tileCount = data.context.accumulator(0)
		// Record the levels we write
		val levelSet = data.context.accumulableCollection(MutableSet[Int]())
		// record tile sizes
		val xbins = data.context.accumulator(0)(new IntMaxAccumulatorParam)
		val ybins = data.context.accumulator(0)(new IntMaxAccumulatorParam)

    val putter = pyramidIO.getPutter

		// Turn each tile into a table row, noting mins, maxes, and counts as
		// we go.  Note that none of the min/max/count accumulation is actually
		// done until the file is writting - this just sets it up, it doesn't
		// run it
		val HBaseTiles = data.mapPartitions(iter =>
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

						val put = putter.getPutForTile(tile, serializer)

						(new ImmutableBytesWritable, put)
					}
				)
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
		println("Input levels: "+levelSet.value)
		println("X bins: "+xbins.value)
		println("Y bins: "+ybins.value)



		// Don't alter metadata if there was no data added.
		// Ideally, we'd still alter levels

		println("Calculating metadata")
		val metaData =
			combineMetaData(pyramider, baseLocation,
											levelSet.value.toSet,
											tileAnalytics, dataAnalytics,
											xbins.value, ybins.value,
											name, description)
		writeMetaData(baseLocation, metaData)

	}
}


object CountHBaseRowsByLevel {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(Array("-io", "hbase") ++ args)
		val tileIO = TileIO.fromArguments(argParser)
		val serializer = TileSerializerChooser.fromArguments(argParser)
		val table = argParser.getString("table", "The name of the table to read")
		val sc = argParser.getSparkConnector().createContext(Some("Testing table equality"))

		tileIO.readTileSet(sc, serializer, table, null).map(tile =>
			{
				(tile.getDefinition().getLevel(), 1)
			}
		).reduceByKey(_ + _).collect.foreach(println(_))
	}
}
