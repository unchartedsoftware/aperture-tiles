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

import java.lang.{Byte => JavaByte}
import java.lang.{Short => JavaShort}
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}

trait SimpleConvertableTo[@specialized A] {
	implicit def fromByte (a: Byte): A
	implicit def fromShort (a: Short): A
	implicit def fromInt (a: Int): A
	implicit def fromLong (a: Long): A
	implicit def fromFloat (a: Float): A
	implicit def fromDouble (a: Double): A
	implicit def fromJavaInt (a: JavaInt): A
	implicit def fromJavaLong (a: JavaLong): A
	implicit def fromJavaFloat (a: JavaFloat): A
	implicit def fromJavaDouble (a: JavaDouble): A
}

trait SimpleConvertableToByte extends SimpleConvertableTo[Byte] {
	implicit def fromByte (a: Byte): Byte = a
	implicit def fromShort (a: Short): Byte = a.toByte
	implicit def fromInt (a: Int): Byte = a.toByte
	implicit def fromLong (a: Long): Byte = a.toByte
	implicit def fromFloat (a: Float): Byte = a.toByte
	implicit def fromDouble (a: Double): Byte = a.toByte
	implicit def fromJavaInt (a: JavaInt): Byte = a.byteValue()
	implicit def fromJavaLong (a: JavaLong): Byte = a.byteValue()
	implicit def fromJavaFloat (a: JavaFloat): Byte = a.byteValue()
	implicit def fromJavaDouble (a: JavaDouble): Byte = a.byteValue()
}

trait SimpleConvertableToShort extends SimpleConvertableTo[Short] {
	implicit def fromByte (a: Byte): Short = a.toShort
	implicit def fromShort (a: Short): Short = a
	implicit def fromInt (a: Int): Short = a.toShort
	implicit def fromLong (a: Long): Short = a.toShort
	implicit def fromFloat (a: Float): Short = a.toShort
	implicit def fromDouble (a: Double): Short = a.toShort
	implicit def fromJavaInt (a: JavaInt): Short = a.shortValue()
	implicit def fromJavaLong (a: JavaLong): Short = a.shortValue()
	implicit def fromJavaFloat (a: JavaFloat): Short = a.shortValue()
	implicit def fromJavaDouble (a: JavaDouble): Short = a.shortValue()
}

trait SimpleConvertableToInt extends SimpleConvertableTo[Int] {
	implicit def fromByte (a: Byte): Int = a.toInt
	implicit def fromShort (a: Short): Int = a.toInt
	implicit def fromInt (a: Int): Int = a
	implicit def fromLong (a: Long): Int = a.toInt
	implicit def fromFloat (a: Float): Int = a.toInt
	implicit def fromDouble (a: Double): Int = a.toInt
	implicit def fromJavaInt (a: JavaInt): Int = a.intValue
	implicit def fromJavaLong (a: JavaLong): Int = a.intValue
	implicit def fromJavaFloat (a: JavaFloat): Int = a.intValue
	implicit def fromJavaDouble (a: JavaDouble): Int = a.intValue
}

trait SimpleConvertableToLong extends SimpleConvertableTo[Long] {
	implicit def fromByte (a: Byte): Long = a.toLong
	implicit def fromShort (a: Short): Long = a.toLong
	implicit def fromInt (a: Int): Long = a.toLong
	implicit def fromLong (a: Long): Long = a
	implicit def fromFloat (a: Float): Long = a.toLong
	implicit def fromDouble (a: Double): Long = a.toLong
	implicit def fromJavaInt (a: JavaInt): Long = a.longValue
	implicit def fromJavaLong (a: JavaLong): Long = a.longValue
	implicit def fromJavaFloat (a: JavaFloat): Long = a.longValue
	implicit def fromJavaDouble (a: JavaDouble): Long = a.longValue
}

trait SimpleConvertableToFloat extends SimpleConvertableTo[Float] {
	implicit def fromByte (a: Byte): Float = a.toFloat
	implicit def fromShort (a: Short): Float = a.toFloat
	implicit def fromInt (a: Int): Float = a.toFloat
	implicit def fromLong (a: Long): Float = a.toFloat
	implicit def fromFloat (a: Float): Float = a
	implicit def fromDouble (a: Double): Float = a.toFloat
	implicit def fromJavaInt (a: JavaInt): Float = a.floatValue
	implicit def fromJavaLong (a: JavaLong): Float = a.floatValue
	implicit def fromJavaFloat (a: JavaFloat): Float = a.floatValue
	implicit def fromJavaDouble (a: JavaDouble): Float = a.floatValue
}

trait SimpleConvertableToDouble extends SimpleConvertableTo[Double] {
	implicit def fromByte (a: Byte): Double = a.toDouble
	implicit def fromShort (a: Short): Double = a.toDouble
	implicit def fromInt (a: Int): Double = a.toDouble
	implicit def fromLong (a: Long): Double = a.toDouble
	implicit def fromFloat (a: Float): Double = a.toDouble
	implicit def fromDouble (a: Double): Double = a
	implicit def fromJavaInt (a: JavaInt): Double = a.doubleValue
	implicit def fromJavaLong (a: JavaLong): Double = a.doubleValue
	implicit def fromJavaFloat (a: JavaFloat): Double = a.doubleValue
	implicit def fromJavaDouble (a: JavaDouble): Double = a.doubleValue
}




trait SimpleConvertableFrom[@specialized A] {
	implicit def toByte (a: A): Byte
	implicit def toShort (a: A): Short
	implicit def toInt (a: A): Int
	implicit def toLong (a: A): Long
	implicit def toFloat (a: A): Float
	implicit def toDouble (a: A): Double
	implicit def toJavaInt (a: A): JavaInt = new JavaInt(toInt(a))
	implicit def toJavaLong (a: A): JavaLong = new JavaLong(toLong(a))
	implicit def toJavaFloat (a: A): JavaFloat = new JavaFloat(toFloat(a))
	implicit def toJavaDouble (a: A): JavaDouble = new JavaDouble(toDouble(a))
}

trait SimpleConvertableFromByte extends SimpleConvertableFrom[Byte] {
	implicit def toByte (a: Byte): Byte = a
	implicit def toShort (a: Byte): Short = a.toShort
	implicit def toInt (a: Byte): Int = a.toInt
	implicit def toLong (a: Byte): Long = a.toLong
	implicit def toFloat (a: Byte): Float = a.toFloat
	implicit def toDouble (a: Byte): Double = a.toDouble
}

trait SimpleConvertableFromShort extends SimpleConvertableFrom[Short] {
	implicit def toByte (a: Short): Byte = a.toByte
	implicit def toShort (a: Short): Short = a
	implicit def toInt (a: Short): Int = a.toInt
	implicit def toLong (a: Short): Long = a.toLong
	implicit def toFloat (a: Short): Float = a.toFloat
	implicit def toDouble (a: Short): Double = a.toDouble
}

trait SimpleConvertableFromInt extends SimpleConvertableFrom[Int] {
	implicit def toByte (a: Int): Byte = a.toByte
	implicit def toShort (a: Int): Short = a.toShort
	implicit def toInt (a: Int): Int = a
	implicit def toLong (a: Int): Long = a.toLong
	implicit def toFloat (a: Int): Float = a.toFloat
	implicit def toDouble (a: Int): Double = a.toDouble
}

trait SimpleConvertableFromLong extends SimpleConvertableFrom[Long] {
	implicit def toByte (a: Long): Byte = a.toByte
	implicit def toShort (a: Long): Short = a.toShort
	implicit def toInt (a: Long): Int = a.toInt
	implicit def toLong (a: Long): Long = a
	implicit def toFloat (a: Long): Float = a.toFloat
	implicit def toDouble (a: Long): Double = a.toDouble
}

trait SimpleConvertableFromFloat extends SimpleConvertableFrom[Float] {
	implicit def toByte (a: Float): Byte = a.toByte
	implicit def toShort (a: Float): Short = a.toShort
	implicit def toInt (a: Float): Int = a.toInt
	implicit def toLong (a: Float): Long = a.toLong
	implicit def toFloat (a: Float): Float = a
	implicit def toDouble (a: Float): Double = a.toDouble
}

trait SimpleConvertableFromDouble extends SimpleConvertableFrom[Double] {
	implicit def toByte (a: Double): Byte = a.toByte
	implicit def toShort (a: Double): Short = a.toShort
	implicit def toInt (a: Double): Int = a.toInt
	implicit def toLong (a: Double): Long = a.toLong
	implicit def toFloat (a: Double): Float = a.toFloat
	implicit def toDouble (a: Double): Double = a
}
