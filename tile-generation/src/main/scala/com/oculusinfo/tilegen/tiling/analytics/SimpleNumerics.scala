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

/**
 * Consolidation of numerics to something fast and common.
 * 
 * Most ideas taken from 
 * http://www.azavea.com/blogs/labs/2011/06/scalas-numeric-type-class-pt-1/
 * http://www.azavea.com/blogs/labs/2011/06/scalas-numeric-type-class-pt-2/
 * https://github.com/azavea/numeric/blob/master/src/main/scala/com/azavea/math/Numeric.scala
 */
trait SimpleNumeric[@specialized(Int, Long, Float, Double) A] extends SimpleConvertableFrom[A] with SimpleConvertableTo[A] {
    def abs (a: A): A
    def plus (a: A, b: A): A
    def minus (a: A, b: A): A
    def times (a: A, b: A): A
    def div (a: A, b: A): A
    def min (a: A, b: A): A
    def max (a: A, b: A): A
    def zero: A
    def one: A
    def minValue: A
    def maxValue: A

    def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): A
    def toType[@specialized(Int, Long, Float, Double) B](a: A)(implicit c:SimpleConvertableTo[B]): B
}
object SimpleNumeric {
  implicit object IntIsSimpleNumeric extends IntIsSimpleNumeric
  implicit object LongIsSimpleNumeric extends LongIsSimpleNumeric
  implicit object FloatIsSimpleNumeric extends FloatIsSimpleNumeric
  implicit object DoubleIsSimpleNumeric extends DoubleIsSimpleNumeric

  def numeric[@specialized(Int, Long, Float, Double) A: SimpleNumeric]:SimpleNumeric[A] = implicitly[SimpleNumeric[A]]
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
    def zero: Double = 0
    def one: Double = 1
    def minValue: Double = Double.MinValue
    def maxValue: Double = Double.MaxValue

    def fromType[@specialized(Int, Long, Float, Double) B](b: B)(implicit c:SimpleConvertableFrom[B]): Double = c.toDouble(b)
    def toType[@specialized(Int, Long, Float, Double) B](a: Double)(implicit c:SimpleConvertableTo[B]): B = c.fromDouble(a)
}
