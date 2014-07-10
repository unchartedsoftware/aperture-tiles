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



import com.oculusinfo.binning.impl.AOITilePyramid



trait IndexScheme[T] {
	def toCartesian (t: T): (Double, Double)
	
	//TODO -- toCartesianEndpoints is only used for RDDLineBinner 
	//so ideally this should be moved to LineSegmentIndexScheme in RDDLineBinner?
	def toCartesianEndpoints (t: T): (Double, Double, Double, Double)
}

class CartesianIndexScheme extends IndexScheme[(Double, Double)] with Serializable {
	def toCartesian (coords: (Double, Double)): (Double, Double) = coords
	def toCartesianEndpoints (coords: (Double, Double)): (Double, Double, Double, Double) = (coords._1, coords._1, coords._2, coords._2) 	//TODO -- redundant, see note above
}

object IPv4ZCurveIndexScheme {
	def getDefaultIPPyramid =
		new AOITilePyramid(0, 0, 0x10000L.toDouble, 0x10000L.toDouble)

	def ipArrayToLong (ip: Array[Byte]): Long =
		ip.map(_.toLong & 0xffL).foldLeft(0L)(256L*_+_)

	def longToIPArray (ip: Long): Array[Byte] =
		Array[Byte]((0xff & (ip >> 24)).toByte,
		            (0xff & (ip >> 16)).toByte,
		            (0xff & (ip >>  8)).toByte,
		            (0xff &  ip       ).toByte)

	def ipArrayToString (ip: Array[Byte]): String =
		ip.map(_.toInt & 0xff).mkString(".")

	def stringToIPArray (ip: String): Array[Byte] =
		ip.split("\\.").map(_.toInt.toByte)

	def reverse (x: Double, y: Double): Array[Byte] = {
		val xL = x.toLong
		val yL = y.toLong
		val yExpand = Range(0, 16).map(i => ((yL >> i) & 0x1L) << (2*i+1)).reduce(_ + _)
		val xExpand = Range(0, 16).map(i => ((xL >> i) & 0x1L) << (2*i  )).reduce(_ + _)
		val ipAddress = xExpand + yExpand
		longToIPArray(ipAddress)
	}
}
class IPv4ZCurveIndexScheme extends IndexScheme[Array[Byte]] with Serializable {
	def toCartesian (ipAddress: Array[Byte]): (Double, Double) = {
		def getXDigit (byte: Byte): Long =
			(((byte & 0x40) >> 3) |
				 ((byte & 0x10) >> 2) |
				 ((byte & 0x04) >> 1) |
				 ((byte & 0x01))).toLong

		def getYDigit (byte: Byte): Long =
			(((byte & 0x80) >> 4) |
				 ((byte & 0x20) >> 3) |
				 ((byte & 0x08) >> 2) |
				 ((byte & 0x02) >> 1)).toLong

		ipAddress.map(byte => (getXDigit(byte), getYDigit(byte)))
			.foldLeft((0.0, 0.0))((a, b) =>
			(16.0*a._1+b._1, 16.0*a._2+b._2)
		)
	}
	def toCartesianEndpoints (ipAddress: Array[Byte]): (Double, Double, Double, Double) = (0, 0, 0, 0) 	//TODO -- redundant, see note above
}

trait TimeIndexScheme[T] extends IndexScheme[T] {
	def extractTime (t: T): Double
}

/**
 * Assumes the coords coming in are (Date, X, Y), so this just throws away the date field.
 */
class TimeRangeCartesianIndexScheme extends TimeIndexScheme[(Double, Double, Double)] with Serializable {
	def toCartesian (coords: (Double, Double, Double)): (Double, Double) = (coords._2, coords._3)
	def extractTime (coords: (Double, Double, Double)): Double = coords._1
	def toCartesianEndpoints (coords: (Double, Double, Double)): (Double, Double, Double, Double) = (coords._1, coords._2, coords._3, coords._3) 	//TODO -- redundant, see note above
}


