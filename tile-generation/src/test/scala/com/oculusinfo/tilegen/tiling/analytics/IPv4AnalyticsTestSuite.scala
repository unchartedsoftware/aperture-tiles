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

package com.oculusinfo.tilegen.tiling.analytics



import java.lang.{Double => JavaDouble}

import scala.collection.JavaConverters._

import org.scalatest.FunSuite

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme.ipArrayToString
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme.longToIPArray
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme



class IPv4AnalyticsTestSuite extends FunSuite {
	// IP Testing
	// Here are the corners of levels 0 through 3, by IP address, tab-delimited (paste into excel for ease of use):
	//		ab.ff.ff.ff		af.ff.ff.ff		bb.ff.ff.ff		bf.ff.ff.ff		eb.ff.ff.ff		ef.ff.ff.ff		fb.ff.ff.ff		ff.ff.ff.ff
	//	a8.00.00.00		ac.00.00.00		b8.00.00.00		bc.00.00.00		e8.00.00.00		ec.00.00.00		f8.00.00.00		fc.00.00.00
	//		a3.ff.ff.ff		a7.ff.ff.ff		b3.ff.ff.ff		b7.ff.ff.ff		e3.ff.ff.ff		e7.ff.ff.ff		f3.ff.ff.ff		f7.ff.ff.ff
	//	a0.00.00.00		a4.00.00.00		b0.00.00.00		b4.00.00.00		e0.00.00.00		e4.00.00.00		f0.00.00.00		f4.00.00.00
	//		8b.ff.ff.ff		8f.ff.ff.ff		9b.ff.ff.ff		9f.ff.ff.ff		cb.ff.ff.ff		cf.ff.ff.ff		db.ff.ff.ff		df.ff.ff.ff
	//	88.00.00.00		8c.00.00.00		98.00.00.00		9c.00.00.00		c8.00.00.00		cc.00.00.00		d8.00.00.00		dc.00.00.00
	//		83.ff.ff.ff		87.ff.ff.ff		93.ff.ff.ff		97.ff.ff.ff		c3.ff.ff.ff		c7.ff.ff.ff		d3.ff.ff.ff		d7.ff.ff.ff
	//	80.00.00.00		84.00.00.00		90.00.00.00		94.00.00.00		c0.00.00.00		c4.00.00.00		d0.00.00.00		d4.00.00.00
	//		2b.ff.ff.ff		2f.ff.ff.ff		3b.ff.ff.ff		3f.ff.ff.ff		6b.ff.ff.ff		6f.ff.ff.ff		7b.ff.ff.ff		7f.ff.ff.ff
	//	28.00.00.00		2c.00.00.00		38.00.00.00		3c.00.00.00		68.00.00.00		6c.00.00.00		78.00.00.00		7c.00.00.00
	//		23.ff.ff.ff		27.ff.ff.ff		33.ff.ff.ff		37.ff.ff.ff		63.ff.ff.ff		67.ff.ff.ff		73.ff.ff.ff		77.ff.ff.ff
	//	20.00.00.00		24.00.00.00		30.00.00.00		34.00.00.00		60.00.00.00		64.00.00.00		70.00.00.00		74.00.00.00
	//		0b.ff.ff.ff		0f.ff.ff.ff		1b.ff.ff.ff		1f.ff.ff.ff		4b.ff.ff.ff		4f.ff.ff.ff		5b.ff.ff.ff		5f.ff.ff.ff
	//	08.00.00.00		0c.00.00.00		18.00.00.00		1c.00.00.00		48.00.00.00		4c.00.00.00		58.00.00.00		5c.00.00.00
	//		03.ff.ff.ff		07.ff.ff.ff		13.ff.ff.ff		17.ff.ff.ff		43.ff.ff.ff		47.ff.ff.ff		53.ff.ff.ff		57.ff.ff.ff
	//	00.00.00.00		04.00.00.00		10.00.00.00		14.00.00.00		40.00.00.00		44.00.00.00		50.00.00.00		54.00.00.00

	test("IPv4 CIDR Block Analytic - theoretical blocks") {
		import IPv4ZCurveIndexScheme._

		val indexScheme = new IPv4ZCurveIndexScheme
		val pyramid = IPv4ZCurveIndexScheme.getDefaultIPPyramid
		val converter = IPv4Analytics.getCIDRBlock(pyramid)(_)

		for (i <- 1 to 16) {
			// We get the tile whose upper right hand corner is the center of the space
			// By our above chart, it's bounds will be:
			//	level 1: 00.00.00.00 to 3f.ff.ff.ff (CIDR should be: 00.00.00.00/2)
			//	level 2: 30.00.00.00 to 3f.ff.ff.ff (CIDR should be: 30.00.00.00/4)
			//	level 3: 33.00.00.00 to 3f.ff.ff.ff (CIDR should be: 33.00.00.00/6)
			// Generalizing, looks like level N should be
			//	Block: 2i
			//	Address: longToIp((ffffffff00000000 >> (2*i)) & 3fffffff)
			val center = (1 << (i-1)) - 1
			val index = new TileIndex(i, center, center)
			val tile = new DenseTileData[Int](index)
			val cidrBlock = converter(tile)

			val expected = ipArrayToString(longToIPArray((0xffffffff00000000L >> (2*i)) & 0x3fffffffL))+"/"+(2*i)
			assert(expected === cidrBlock)
		}
	}

	test("IPv4 CIDR Block Analytic - empirical blocks") {
		import IPv4ZCurveIndexScheme._

		val indexScheme = new IPv4ZCurveIndexScheme
		val pyramid = IPv4ZCurveIndexScheme.getDefaultIPPyramid
		val converter = IPv4Analytics.getCIDRBlock(pyramid)(_)

		// Can't test level 32 - tile pyramids won't work at level 32, because
		// tile indices are stored as integers instead of longs.
		for (i <- 0 to 16) {
			val fullAddress1 = longToIPArray(0xffffffffL)
			val fullAddress2 = longToIPArray((0xffffffff00000000L >> i) & 0xffffffffL)
			val fullAddress3 = longToIPArray(0xffffffffL >> (2*i))
			val fullAddress4 = longToIPArray((0x100000000L >> i) & 0xffffffffL)
			val expectedFull = ipArrayToString(fullAddress2)
			val expectedOne = ipArrayToString(fullAddress4)

			val cartesian1 = indexScheme.toCartesian(fullAddress1)
			val index1 = pyramid.rootToTile(cartesian1._1, cartesian1._2, i)
			val tile1 = new DenseTileData[Int](index1)
			val value1 = converter(tile1)
			val expected1 = ipArrayToString(longToIPArray((0xffffffff00000000L >> (2*i)) & 0xffffffff))+"/"+(2*i)
			assert(expected1 === value1)

			val cartesian2 = indexScheme.toCartesian(fullAddress2)
			val index2 = pyramid.rootToTile(cartesian2._1, cartesian2._2, i)
			val tile2 = new DenseTileData[Int](index2)
			val value2 = converter(tile2)
			val expected2 = ipArrayToString(longToIPArray((0xffffffff00000000L >> i) & 0xffffffff))+"/"+(2*i)
			assert(expected2 === value2)

			val cartesian3 = indexScheme.toCartesian(fullAddress3)
			val index3 = pyramid.rootToTile(cartesian3._1, cartesian3._2, i)
			val tile3 = new DenseTileData[Int](index3)
			val value3 = converter(tile3)
			val expected3 = "0.0.0.0/"+(2*i)
			assert(expected3 === value3)

			val cartesian4 = indexScheme.toCartesian(fullAddress4)
			val index4 = pyramid.rootToTile(cartesian4._1, cartesian4._2, i)
			val tile4 = new DenseTileData[Int](index4)
			val value4 = converter(tile4)
			val expected4 = expectedOne+"/"+(2*i)
			assert(expected4 === value4)
		}
	}

	test("IPv4 Min/Max analytics") {
		import IPv4ZCurveIndexScheme._

		val maxes = List(
			"03.ff.ff.ff", "07.ff.ff.ff", "13.ff.ff.ff", "17.ff.ff.ff", "43.ff.ff.ff", "47.ff.ff.ff", "53.ff.ff.ff", "57.ff.ff.ff",
			"0b.ff.ff.ff", "0f.ff.ff.ff", "1b.ff.ff.ff", "1f.ff.ff.ff", "4b.ff.ff.ff", "4f.ff.ff.ff", "5b.ff.ff.ff", "5f.ff.ff.ff",
			"23.ff.ff.ff", "27.ff.ff.ff", "33.ff.ff.ff", "37.ff.ff.ff", "63.ff.ff.ff", "67.ff.ff.ff", "73.ff.ff.ff", "77.ff.ff.ff",
			"2b.ff.ff.ff", "2f.ff.ff.ff", "3b.ff.ff.ff", "3f.ff.ff.ff", "6b.ff.ff.ff", "6f.ff.ff.ff", "7b.ff.ff.ff", "7f.ff.ff.ff",
			"83.ff.ff.ff", "87.ff.ff.ff", "93.ff.ff.ff", "97.ff.ff.ff", "c3.ff.ff.ff", "c7.ff.ff.ff", "d3.ff.ff.ff", "d7.ff.ff.ff",
			"8b.ff.ff.ff", "8f.ff.ff.ff", "9b.ff.ff.ff", "9f.ff.ff.ff", "cb.ff.ff.ff", "cf.ff.ff.ff", "db.ff.ff.ff", "df.ff.ff.ff",
			"a3.ff.ff.ff", "a7.ff.ff.ff", "b3.ff.ff.ff", "b7.ff.ff.ff", "e3.ff.ff.ff", "e7.ff.ff.ff", "f3.ff.ff.ff", "f7.ff.ff.ff",
			"ab.ff.ff.ff", "af.ff.ff.ff", "bb.ff.ff.ff", "bf.ff.ff.ff", "eb.ff.ff.ff", "ef.ff.ff.ff", "fb.ff.ff.ff", "ff.ff.ff.ff")
		val mins = List(
			"00.00.00.00", "04.00.00.00", "10.00.00.00", "14.00.00.00", "40.00.00.00", "44.00.00.00", "50.00.00.00", "54.00.00.00",
			"08.00.00.00", "0c.00.00.00", "18.00.00.00", "1c.00.00.00", "48.00.00.00", "4c.00.00.00", "58.00.00.00", "5c.00.00.00",
			"20.00.00.00", "24.00.00.00", "30.00.00.00", "34.00.00.00", "60.00.00.00", "64.00.00.00", "70.00.00.00", "74.00.00.00",
			"28.00.00.00", "2c.00.00.00", "38.00.00.00", "3c.00.00.00", "68.00.00.00", "6c.00.00.00", "78.00.00.00", "7c.00.00.00",
			"80.00.00.00", "84.00.00.00", "90.00.00.00", "94.00.00.00", "c0.00.00.00", "c4.00.00.00", "d0.00.00.00", "d4.00.00.00",
			"88.00.00.00", "8c.00.00.00", "98.00.00.00", "9c.00.00.00", "c8.00.00.00", "cc.00.00.00", "d8.00.00.00", "dc.00.00.00",
			"a0.00.00.00", "a4.00.00.00", "b0.00.00.00", "b4.00.00.00", "e0.00.00.00", "e4.00.00.00", "f0.00.00.00", "f4.00.00.00",
			"a8.00.00.00", "ac.00.00.00", "b8.00.00.00", "bc.00.00.00", "e8.00.00.00", "ec.00.00.00", "f8.00.00.00", "fc.00.00.00")

		val pyramid = IPv4ZCurveIndexScheme.getDefaultIPPyramid
		val minConverter = IPv4Analytics.getIPAddress(pyramid, false)(_)
		val maxConverter = IPv4Analytics.getIPAddress(pyramid, true)(_)

		// Level 3
		for (x <- 0 to 7) {
			for (y <- 0 to 7) {
				val index = new TileIndex(3, x, y)
				val tile = new DenseTileData[Int](index)

				val minIP = longToIPArray(minConverter(tile))
				assert(mins(x+8*y) === "%02x.%02x.%02x.%02x".format(minIP(0), minIP(1), minIP(2), minIP(3)))
				val maxIP = longToIPArray(maxConverter(tile))
				assert(maxes(x+8*y) === "%02x.%02x.%02x.%02x".format(maxIP(0), maxIP(1), maxIP(2), maxIP(3)))
			}
		}

		// Level 2
		for (x <- 0 to 3) {
			for (y <- 0 to 3) {
				val index = new TileIndex(2, x, y)
				val tile = new DenseTileData[Int](index)

				val minIP = longToIPArray(minConverter(tile))
				assert(mins(2*x+16*y) === "%02x.%02x.%02x.%02x".format(minIP(0), minIP(1), minIP(2), minIP(3)))
				val maxIP = longToIPArray(maxConverter(tile))
				assert(maxes(2*x+16*y+1+8) === "%02x.%02x.%02x.%02x".format(maxIP(0), maxIP(1), maxIP(2), maxIP(3)))
			}
		}

		// Level 1
		for (x <- 0 to 1) {
			for (y <- 0 to 1) {
				val index = new TileIndex(1, x, y)
				val tile = new DenseTileData[Int](index)

				val minIP = longToIPArray(minConverter(tile))
				assert(mins(4*x+32*y) === "%02x.%02x.%02x.%02x".format(minIP(0), minIP(1), minIP(2), minIP(3)))
				val maxIP = longToIPArray(maxConverter(tile))
				assert(maxes(4*x+32*y+3+24) === "%02x.%02x.%02x.%02x".format(maxIP(0), maxIP(1), maxIP(2), maxIP(3)))
			}
		}
	}
}
