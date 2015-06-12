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

import java.util.Date

import scala.Range
import scala.collection.mutable.ArrayBuffer

import com.oculusinfo.binning.impl.AOITilePyramid


trait IndexScheme[T] {
	def toCartesian (t: T): (Double, Double)

	//TODO -- toCartesianEndpoints is only used for RDDLineBinner
	//so ideally this should be moved to LineSegmentIndexScheme in RDDLineBinner?
	def toCartesianEndpoints (t: T): (Double, Double, Double, Double)
}

trait TimeIndexScheme[T] extends IndexScheme[T] {
	def extractTime (t: T): Double
}

trait NumberConverter {
	@throws(classOf[IllegalArgumentException])
	def asDouble (x: Any): Double = {
		x match {
			case null => throw new IllegalArgumentException
			case c: Byte => c.toDouble
			case c: Short => c.toDouble
			case c: Int => c.toDouble
			case c: Long => c.toDouble
			case c: Float => c.toDouble
			case c: Double => c.toDouble
			case c: Date => c.getTime
		}
	}
}

class CartesianSchemaIndexScheme extends IndexScheme[Seq[Any]] with NumberConverter with Serializable {
	private def checkForZero (coords: Seq[Any], index: Int): Double =
		asDouble(coords.lift(index).getOrElse(0.0))

	def toCartesian (coords: Seq[Any]): (Double, Double) =
		(checkForZero(coords, 0), checkForZero(coords, 1))

	def toCartesianEndpoints (coords: Seq[Any]): (Double, Double, Double, Double) =
		(checkForZero(coords, 0), checkForZero(coords, 1), checkForZero(coords, 2), checkForZero(coords, 3))
}

class TimeRangeCartesianSchemaIndexScheme (startDate: Double, secsPerPeriod: Double)
		extends TimeIndexScheme[Seq[Any]] with NumberConverter with Serializable
{
	val msPerTimeRange = secsPerPeriod * 1000
	def toCartesian (coords: Seq[Any]): (Double, Double) = (asDouble(coords(1)), asDouble(coords(2)))
	def extractTime (coords: Seq[Any]): Double = {
		def floorDate(d: Double) = Math.floor((d - startDate) / msPerTimeRange) * msPerTimeRange + startDate
		floorDate(asDouble(coords(0)))
	}
	def toCartesianEndpoints (coords: Seq[Any]): (Double, Double, Double, Double) =
		//TODO -- redundant, see note above
		(asDouble(coords(1)), asDouble(coords(2)), asDouble(coords(4)), asDouble(coords(5)))
}

class IPv4ZCurveSchemaIndexScheme extends IndexScheme[Seq[Any]] with Serializable {
	def toCartesian (values: Seq[Any]): (Double, Double) = {
		val value = values(0)
		val ipAddress: Array[Byte] = value match {
			case s: String => s.split("\\.").map(_.trim.toByte)
			case a: ArrayBuffer[_] => a.asInstanceOf[ArrayBuffer[Byte]].toArray
		}
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
	def toCartesianEndpoints (values: Seq[Any]): (Double, Double, Double, Double) = (0, 0, 0, 0) 	//TODO -- redundant, see note above
}



class CartesianIndexScheme extends IndexScheme[(Double, Double)] with Serializable {
	def toCartesian (coords: (Double, Double)): (Double, Double) = coords
	def toCartesianEndpoints (coords: (Double, Double)): (Double, Double, Double, Double) = (coords._1, coords._1, coords._2, coords._2) 	//TODO -- redundant, see note above
}

class DensityStripIndexScheme extends IndexScheme[(Double, Double)] with Serializable {
	def toCartesian(coords: (Double, Double)): (Double, Double) = (coords._1, 0)
	def toCartesianEndpoints (coords: (Double, Double)): (Double, Double, Double, Double) = (coords._1, coords._1, 0, 0)
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

/**
 * Assumes the coords coming in are (Date, X, Y), so this just throws away the date field.
 */
class TimeRangeCartesianIndexScheme extends TimeIndexScheme[(Double, Double, Double)] with Serializable {
	def toCartesian (coords: (Double, Double, Double)): (Double, Double) = (coords._2, coords._3)
	def extractTime (coords: (Double, Double, Double)): Double = coords._1
	def toCartesianEndpoints (coords: (Double, Double, Double)): (Double, Double, Double, Double) = (coords._1, coords._2, coords._3, coords._3) 	//TODO -- redundant, see note above
}
