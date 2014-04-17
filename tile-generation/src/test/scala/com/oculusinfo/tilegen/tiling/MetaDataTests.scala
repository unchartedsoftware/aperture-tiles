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


import scala.collection.JavaConverters._

import java.io.File
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}
import java.awt.geom.Rectangle2D

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers



class MetadataTestSuite extends FunSuite with ShouldMatchers {
	val epsilon = 1E-10

	test("Metadata parsing") {
		val text = (
			"{\n" +
				"    \"name\":\"Foobar\",\n" +
				"    \"description\":\"Binned foobar data\",\n" +
				"    \"tilesize\":256,\n" +
				"    \"scheme\":\"TMS\",\n" +
				"    \"projection\":\"web mercator\",\n" +
				"    \"minzoom\":0,\n" +
				"    \"maxzoom\":2,\n" +
				"    \"bounds\": [ -180.000000, -85.051129, 180.000000, 85.051129 ],\n" +
				"    \"meta\": {\n" +
				"        \"levelMaximums\": {\n" +
				"            \"0\": \"1497547\",\n" +
				"            \"1\": \"748773\",\n" +
				"            \"2\": \"374386\"\n" +
				"        },\n" +
				"        \"levelMinimums\": {\n" +
				"            \"0\": \"0\",\n" +
				"            \"1\": \"2\",\n" +
				"            \"2\": \"4\"\n" +
				"        }\n" +
				"    }\n" +
				"}\n")
		val metaData = TileMetaData.parse(text).get
		metaData.name should be === "Foobar"
		metaData.description should be === "Binned foobar data"
		metaData.tileSize should be === 256
		metaData.scheme should be === "TMS"
		metaData.projection should be === "web mercator"
		metaData.minZoom should be === 0
		metaData.maxZoom should be === 2
		metaData.bounds.getMinX() should be (-180.0 plusOrMinus epsilon)
		metaData.bounds.getMaxX() should be (180.0 plusOrMinus epsilon)
		metaData.bounds.getMinY() should be (-85.051129 plusOrMinus epsilon)
		metaData.bounds.getMaxY() should be (85.051129 plusOrMinus epsilon)
		metaData.levelMaxes.size should be === 3
		metaData.levelMaxes(0)._1 should be === 0
		metaData.levelMaxes(0)._2 should be === "1497547"
		metaData.levelMaxes(1)._1 should be === 1
		metaData.levelMaxes(1)._2 should be === "748773"
		metaData.levelMaxes(2)._1 should be === 2
		metaData.levelMaxes(2)._2 should be === "374386"
		metaData.levelMins.size should be === 3
		metaData.levelMins(0)._1 should be === 0
		metaData.levelMins(0)._2 should be === "0"
		metaData.levelMins(1)._1 should be === 1
		metaData.levelMins(1)._2 should be === "2"
		metaData.levelMins(2)._1 should be === 2
		metaData.levelMins(2)._2 should be === "4"

		val md2 = metaData.addLevel(3, 6.toString, 187193.toString)
		md2.levelMaxes.size should be === 4
		md2.levelMaxes(0)._1 should be === 0
		md2.levelMaxes(0)._2 should be === "1497547"
		md2.levelMaxes(1)._1 should be === 1
		md2.levelMaxes(1)._2 should be === "748773"
		md2.levelMaxes(2)._1 should be === 2
		md2.levelMaxes(2)._2 should be === "374386"
		md2.levelMaxes(3)._1 should be === 3
		md2.levelMaxes(3)._2 should be === "187193"
		md2.levelMins.size should be === 4
		md2.levelMins(0)._1 should be === 0
		md2.levelMins(0)._2 should be === "0"
		md2.levelMins(1)._1 should be === 1
		md2.levelMins(1)._2 should be === "2"
		md2.levelMins(2)._1 should be === 2
		md2.levelMins(2)._2 should be === "4"
		md2.levelMins(3)._1 should be === 3
		md2.levelMins(3)._2 should be === "6"

		val md3 = md2.addLevel(4, 8.toString, 93596.toString)
		md3.levelMaxes.size should be === 5
		md3.levelMaxes(0)._1 should be === 0
		md3.levelMaxes(0)._2 should be === "1497547"
		md3.levelMaxes(1)._1 should be === 1
		md3.levelMaxes(1)._2 should be === "748773"
		md3.levelMaxes(2)._1 should be === 2
		md3.levelMaxes(2)._2 should be === "374386"
		md3.levelMaxes(3)._1 should be === 3
		md3.levelMaxes(3)._2 should be === "187193"
		md3.levelMaxes(4)._1 should be === 4
		md3.levelMaxes(4)._2 should be === "93596"
		md3.levelMins.size should be === 5
		md3.levelMins(0)._1 should be === 0
		md3.levelMins(0)._2 should be === "0"
		md3.levelMins(1)._1 should be === 1
		md3.levelMins(1)._2 should be === "2"
		md3.levelMins(2)._1 should be === 2
		md3.levelMins(2)._2 should be === "4"
		md3.levelMins(3)._1 should be === 3
		md3.levelMins(3)._2 should be === "6"
		md3.levelMins(4)._1 should be === 4
		md3.levelMins(4)._2 should be === "8"
	}

	test("Local metadata writing") {
		val io = new LocalTileIO("avro")
		val metaData = new TileMetaData("n", "d", 13, "s", "p", 1, 2,
		                                new Rectangle2D.Double(0, 0, 1, 2),
		                                List((0, "12"), (1, "9")),
		                                List((0, "100"), (1, "101")))
		// Create the directory into which to write the metadata
		val tmpDir = new File("./test-tmp")
		tmpDir.mkdirs()

		// Make sure it gets cleaned up
		try {
			// write it
			io.writeMetaData("./test-tmp", metaData)

			// read it back in again
			val reread = io.readMetaData("./test-tmp").get

			reread.name should be === metaData.name
			reread.description should be === metaData.description
			reread.tileSize should be === metaData.tileSize
			reread.scheme should be === metaData.scheme
			reread.projection should be === metaData.projection
			reread.minZoom should be === metaData.minZoom
			reread.maxZoom should be === metaData.maxZoom
			reread.bounds.getMinX() should be (metaData.bounds.getMinX() plusOrMinus epsilon)
			reread.bounds.getMaxX() should be (metaData.bounds.getMaxX() plusOrMinus epsilon)
			reread.bounds.getMinY() should be (metaData.bounds.getMinY() plusOrMinus epsilon)
			reread.bounds.getMaxY() should be (metaData.bounds.getMaxY() plusOrMinus epsilon)
			reread.levelMins.size should be === metaData.levelMins.size
			for (n <- 0 until reread.levelMins.size) {
				reread.levelMins(n)._1 should be === metaData.levelMins(n)._1
				reread.levelMins(n)._2 should be === metaData.levelMins(n)._2
			}
			reread.levelMaxes.size should be === metaData.levelMaxes.size
			for (n <- 0 until reread.levelMaxes.size) {
				reread.levelMaxes(n)._1 should be === metaData.levelMaxes(n)._1
				reread.levelMaxes(n)._2 should be === metaData.levelMaxes(n)._2
			}
		} finally {
			// Clean up our temporary output directory
			def delete (file: File): Unit = {
				println("Deleting "+file)
				if (file.exists()) {
					if (file.isDirectory()) {
						file.list().foreach(subFile => delete(new File(file, subFile)))
						if (0 == file.list().length)
							file.delete()
					} else {
						file.delete()
					}
				} else {
					println("\tDoesn't exist!")
				}
			}

			delete(tmpDir)
		}
	}
}
