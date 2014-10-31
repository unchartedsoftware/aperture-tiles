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


import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}



/**
 * Consolidation of numerics to something fast and common.
 * 
 * Most ideas taken from 
 * http://www.azavea.com/blogs/labs/2011/06/scalas-numeric-type-class-pt-1/
 * http://www.azavea.com/blogs/labs/2011/06/scalas-numeric-type-class-pt-2/
 * https://github.com/azavea/numeric/blob/master/src/main/scala/com/azavea/math/Numeric.scala
 * 
 * This is supposed to be faster with @specialized, but my test (in 
 * SimpleNumericSpeedTestSuite) don't show any improvement.  I'll do more 
 * thorough testing later, but for now, except that we have a few extra pieces
 * (like minValue, maxValue, and isNaN), it appears we could just use normal
 * scala numerics - NDK
 */
// trait SimpleNumeric[@specialized(Int, Long, Float, Double) A] extends SimpleConvertableFrom[A] with SimpleConvertableTo[A] with Serializable {
trait SimpleNumeric[A] extends SimpleConvertableFrom[A] with SimpleConvertableTo[A] with Serializable {
	/** Absolute value */
	def abs (a: A): A
	/** Addition */
	def plus (a: A, b: A): A
	/** Subtraction */
	def minus (a: A, b: A): A
	/** Multiplication */
	def times (a: A, b: A): A
	/** Division */
	def div (a: A, b: A): A
	/** Minimum of two numbers */
	def min (a: A, b: A): A
	/** Maximum of two numbers */
	def max (a: A, b: A): A
	/** a > b */
	def gt (a: A, b: A): Boolean
	/** a < b */
	def lt (a: A, b: A): Boolean
	/** a >= b */
	def ge (a: A, b: A): Boolean
	/** a <= b */
	def le (a: A, b: A): Boolean
	/** is it really a number? */
	def isNaN (a: A): Boolean
	/** Typed zero */
	def zero: A
	/** Typed one */
	def one: A
	/** Minimum possible typed value */
	def minValue: A
	/** Maximum possible typed value */
	def maxValue: A

	def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): A
	def toType[@specialized(Int, Long, Float, Double) B](a: A)(implicit c:SimpleConvertableTo[B]): B
}
object SimpleNumeric {
	implicit object IntIsSimpleNumeric extends IntIsSimpleNumeric
	implicit object LongIsSimpleNumeric extends LongIsSimpleNumeric
	implicit object FloatIsSimpleNumeric extends FloatIsSimpleNumeric
	implicit object DoubleIsSimpleNumeric extends DoubleIsSimpleNumeric

	// def numeric[@specialized(Int, Long, Float, Double) A: SimpleNumeric]:SimpleNumeric[A] = implicitly[SimpleNumeric[A]]
	def numeric[A: SimpleNumeric]:SimpleNumeric[A] = implicitly[SimpleNumeric[A]]
}



trait IntIsSimpleNumeric
		extends SimpleNumeric[Int] with SimpleConvertableToInt with SimpleConvertableFromInt {
	def abs (a: Int): Int = scala.math.abs(a)
	def plus (a: Int, b: Int): Int = a + b
	def minus (a: Int, b: Int): Int = a - b
	def times (a: Int, b: Int): Int = a * b
	def div (a: Int, b: Int): Int = a / b
	def min (a: Int, b: Int): Int = a min b
	def max (a: Int, b: Int): Int = a max b
	def gt (a: Int, b: Int): Boolean = a > b
	def lt (a: Int, b: Int): Boolean = a < b
	def ge (a: Int, b: Int): Boolean = a >= b
	def le (a: Int, b: Int): Boolean = a <= b
	def isNaN (a: Int): Boolean = false
	def zero: Int = 0
	def one: Int = 1
	def minValue: Int = Int.MinValue
	def maxValue: Int = Int.MaxValue

	def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): Int = c.toInt(b)
	def toType[@specialized(Int, Long, Float, Double) B](a: Int)(implicit c:SimpleConvertableTo[B]): B = c.fromInt(a)
}

trait LongIsSimpleNumeric
		extends SimpleNumeric[Long] with SimpleConvertableToLong with SimpleConvertableFromLong {
	def abs (a: Long): Long = scala.math.abs(a)
	def plus (a: Long, b: Long): Long = a + b
	def minus (a: Long, b: Long): Long = a - b
	def times (a: Long, b: Long): Long = a * b
	def div (a: Long, b: Long): Long = a / b
	def min (a: Long, b: Long): Long = a min b
	def max (a: Long, b: Long): Long = a max b
	def gt (a: Long, b: Long): Boolean = a > b
	def lt (a: Long, b: Long): Boolean = a < b
	def ge (a: Long, b: Long): Boolean = a >= b
	def le (a: Long, b: Long): Boolean = a <= b
	def isNaN (a: Long): Boolean = false
	def zero: Long = 0
	def one: Long = 1
	def minValue: Long = Long.MinValue
	def maxValue: Long = Long.MaxValue

	def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): Long = c.toLong(b)
	def toType[@specialized(Int, Long, Float, Double) B](a: Long)(implicit c:SimpleConvertableTo[B]): B = c.fromLong(a)
}

trait FloatIsSimpleNumeric
		extends SimpleNumeric[Float] with SimpleConvertableToFloat with SimpleConvertableFromFloat {
	def abs (a: Float): Float = scala.math.abs(a)
	def plus (a: Float, b: Float): Float = a + b
	def minus (a: Float, b: Float): Float = a - b
	def times (a: Float, b: Float): Float = a * b
	def div (a: Float, b: Float): Float = a / b
	def min (a: Float, b: Float): Float = a min b
	def max (a: Float, b: Float): Float = a max b
	def gt (a: Float, b: Float): Boolean = a > b
	def lt (a: Float, b: Float): Boolean = a < b
	def ge (a: Float, b: Float): Boolean = a >= b
	def le (a: Float, b: Float): Boolean = a <= b
	def isNaN (a: Float): Boolean = JavaFloat.isNaN(a)
	def zero: Float = 0
	def one: Float = 1
	def minValue: Float = Float.MinValue
	def maxValue: Float = Float.MaxValue

	def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): Float = c.toFloat(b)
	def toType[@specialized(Int, Long, Float, Double) B](a: Float)(implicit c:SimpleConvertableTo[B]): B = c.fromFloat(a)
}

trait DoubleIsSimpleNumeric
		extends SimpleNumeric[Double] with SimpleConvertableToDouble with SimpleConvertableFromDouble {
	def abs (a: Double): Double = scala.math.abs(a)
	def plus (a: Double, b: Double): Double = a + b
	def minus (a: Double, b: Double): Double = a - b
	def times (a: Double, b: Double): Double = a * b
	def div (a: Double, b: Double): Double = a / b
	def min (a: Double, b: Double): Double = a min b
	def max (a: Double, b: Double): Double = a max b
	def gt (a: Double, b: Double): Boolean = a > b
	def lt (a: Double, b: Double): Boolean = a < b
	def ge (a: Double, b: Double): Boolean = a >= b
	def le (a: Double, b: Double): Boolean = a <= b
	def isNaN (a: Double): Boolean = JavaDouble.isNaN(a)
	def zero: Double = 0
	def one: Double = 1
	def minValue: Double = Double.MinValue
	def maxValue: Double = Double.MaxValue

	def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): Double = c.toDouble(b)
	def toType[@specialized(Int, Long, Float, Double) B](a: Double)(implicit c:SimpleConvertableTo[B]): B = c.fromDouble(a)
}
