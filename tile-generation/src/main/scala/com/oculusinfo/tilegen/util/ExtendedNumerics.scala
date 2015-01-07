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

package com.oculusinfo.tilegen.util



import scala.math.Numeric
import scala.math.Ordering



/**
 * This class attempts to extend the standard scala Numerics class with a few 
 * extra useful functions - like division, validity, max and min values, etc.
 * 
 * @tparam T The numeric value type represented
 */
trait ExtendedNumeric[T] extends Numeric[T] {
	// A name by which to know this numeric type in error messages and the like
	def name: String

	// Extended conversion methods
	// Numeric has fromInt, and toX for all appropriate X.
	def fromLong (x: Long): T
	def fromFloat (x: Float): T
	def fromDouble (x: Double): T
	def fromString (s: String): T

	// Extended numeric methods
	def div (x: T, y: T): T
	def isNaN (x: T): Boolean
	def minValue: T
	def maxValue: T
	def getNumericClass: Class[_ <: T]

	class ExtendedOps (lhs: T) extends Ops(lhs) {
		def / (rhs: T)  = div(lhs, rhs)
		def > (rhs: T)  = gt(lhs, rhs)
		def >= (rhs: T) = gteq(lhs, rhs)
		def < (rhs: T)  = lt(lhs, rhs)
		def <= (rhs: T) = lteq(lhs, rhs)
	}
	implicit def mkExtendedNumericOps (lhs: T): ExtendedOps = new ExtendedOps(lhs)
}



object ExtendedNumeric {
	trait ExtendedInt extends ExtendedNumeric[Int] with Numeric.IntIsIntegral {
		def name = "int"

		def div (n: Int, m: Int): Int = n / m

		def fromLong (n: Long): Int = n.toInt
		def fromFloat (n: Float): Int = n.toInt
		def fromDouble (n: Double): Int = n.toInt
		def fromString (s: String): Int = s.toInt

		def isNaN (n: Int): Boolean = false
		def minValue: Int = Int.MinValue
		def maxValue: Int = Int.MaxValue
		def getNumericClass = classOf[Int]
	}
	implicit object ExtendedInt extends ExtendedInt with Ordering.IntOrdering

	trait ExtendedLong extends ExtendedNumeric[Long] with Numeric.LongIsIntegral {
		def name = "long"

		def div (n: Long, m: Long): Long = n / m
		
		def fromLong (n: Long): Long = n
		def fromFloat (n: Float): Long = n.toLong
		def fromDouble (n: Double): Long = n.toLong
		def fromString (s: String): Long = s.toLong
		
		def isNaN (n: Long): Boolean = false
		def minValue: Long = Long.MinValue
		def maxValue: Long = Long.MaxValue
		def getNumericClass = classOf[Long]
	}
	implicit object ExtendedLong extends ExtendedLong with Ordering.LongOrdering

	trait ExtendedFloat extends ExtendedNumeric[Float] with Numeric.FloatIsFractional {
		def name = "float"

		def fromLong (x: Long): Float = x.toFloat
		def fromFloat (x: Float): Float = x
		def fromDouble (x: Double): Float = x.toFloat
		def fromString (s: String): Float = s.toFloat

		def isNaN (x: Float): Boolean = x.isNaN
		def minValue: Float = Float.MinValue
		def maxValue: Float = Float.MaxValue
		def getNumericClass = classOf[Float]
	}
	implicit object ExtendedFloat extends ExtendedFloat with Ordering.FloatOrdering

	trait ExtendedDouble extends ExtendedNumeric[Double] with Numeric.DoubleIsFractional {
		def name = "double"

		def fromLong (x: Long): Double = x.toDouble
		def fromFloat (x: Float): Double = x.toDouble
		def fromDouble (x: Double): Double = x
		def fromString (s: String): Double = s.toDouble

		def isNaN (x: Double): Boolean = x.isNaN
		def minValue: Double = Double.MinValue
		def maxValue: Double = Double.MaxValue
		def getNumericClass = classOf[Double]
	}
	implicit object ExtendedDouble extends ExtendedDouble with Ordering.DoubleOrdering
}
