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
import org.scalatest.FunSuite
import org.apache.avro.file.CodecFactory
import org.apache.spark.SharedSparkContext
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.metadata.PyramidMetaData


/**
 * A simple HBaseTileIO implementation for tests.  This test is specific to a particular table on an internal
 *  HBase server. Only run this test if server is available.
 */
class TestHBaseTileIO extends FunSuite with SharedSparkContext {
	ignore("read in a tile using HBaseTileIO") {
		val quorum 	= "hadoop-s1.oculus.local"
		val port 	= "2181"
		val master 	= "hadoop-s1.oculus.local:60000"
		val baseLocation = "test1.nyc_pickup.longitude.latitude"
		  
		val tileIO =  new HBaseTileIO(quorum, port, master)
		val serializer = new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())

		val tiles = tileIO.readTileSet(sc, serializer, baseLocation, null).collect()
		val metadata = tileIO.readMetaData(baseLocation).get
		
		// specific for this Table
		assert(6 == metadata.getMaxZoom())

		// option to print tile for debugging
		val printer = new TilePrinter()
		tiles.foreach( t =>
			{
				// check all levels are valid
				assert(t.getDefinition().getLevel() == metadata.getValidZoomLevels().get(t.getDefinition().getLevel()).intValue())
				
				assert( metadata.getTileSizeX() == t.getDefinition().getXBins() )
				assert( metadata.getTileSizeY() == t.getDefinition().getYBins() )
				
				println()
				println(t.getDefinition())
				printer.printTile(t)
			}
		)
	}
}