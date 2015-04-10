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


import java.util.{List => JavaList, Date}
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}

import com.oculusinfo.factory.properties.StringProperty

import scala.math.Numeric
import scala.math.Ordering

import com.oculusinfo.factory.ConfigurableFactory

import scala.reflect.ClassTag


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
	def fromNumber (x: Number): T
	def fromString (s: String): T
	// Attempt to turn anything into the current numeric type, throwing an exception if the type of the input is
	// inconvertable
	def fromAny (a: Any): T

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
		def fromNumber (n: Number): Int = n.intValue()
		def fromString (s: String): Int = s.toInt
		def fromAny (a: Any): Int =
			a match {
				case null => throw new IllegalArgumentException
				case c: Byte => c.toInt
				case c: Short => c.toInt
				case c: Int => c
				case c: Long => c.toInt
				case c: Float => c.toInt
				case c: Double => c.toInt
				case c: Date => c.getTime.toInt
				case c: String => c.toInt
			}

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
		def fromNumber (n: Number): Long = n.longValue()
		def fromString (s: String): Long = s.toLong
		def fromAny (a: Any): Long =
			a match {
				case null => throw new IllegalArgumentException
				case c: Byte => c.toLong
				case c: Short => c.toLong
				case c: Int => c.toLong
				case c: Long => c
				case c: Float => c.toLong
				case c: Double => c.toLong
				case c: Date => c.getTime
				case c: String => c.toLong
			}

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
		def fromNumber (x: Number): Float = x.floatValue()
		def fromString (s: String): Float = s.toFloat
		def fromAny (a: Any): Float =
			a match {
				case null => throw new IllegalArgumentException
				case c: Byte => c.toFloat
				case c: Short => c.toFloat
				case c: Int => c.toFloat
				case c: Long => c.toFloat
				case c: Float => c
				case c: Double => c.toFloat
				case c: Date => c.getTime.toFloat
				case c: String => c.toFloat
			}

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
		def fromNumber (x: Number): Double = x.doubleValue()
		def fromString (s: String): Double = s.toDouble
		def fromAny (a: Any): Double =
			a match {
				case null => throw new IllegalArgumentException
				case c: Byte => c.toDouble
				case c: Short => c.toDouble
				case c: Int => c.toDouble
				case c: Long => c.toDouble
				case c: Float => c.toDouble
				case c: Double => c
				case c: Date => c.getTime.toDouble
				case c: String => c.toDouble
			}

		def isNaN (x: Double): Boolean = x.isNaN
		def minValue: Double = Double.MinValue
		def maxValue: Double = Double.MaxValue
		def getNumericClass = classOf[Double]
	}
	implicit object ExtendedDouble extends ExtendedDouble with Ordering.DoubleOrdering
}


/** A quick case class to encapsulate an extended numeric and type conversion of related types together. */
private[util] case class ExtendedNumericWithConversion[T: ClassTag, JT]
	(implicit n: ExtendedNumeric[T], c: TypeConversion[T, JT])
{
	def tag = implicitly[ClassTag[T]]
	def numeric = n
	def conversion = c
}
object NumericallyConfigurableFactory {
	private[util] val NUMERIC_TYPE_PROPERTY =
		new StringProperty("valueType", "The type of numeric value used by this value extractor", "double",
		                   Array("int", "long", "float", "double"))

}

/**
 * An abstract factory that allows sub-factories to create objects of generic numeric types.
 *
 * All arguments are pass-throughs to {@see ConfigurableFactory}.
 */
abstract class NumericallyConfigurableFactory[T]
	(name: String, factoryType: Class[T], parent: ConfigurableFactory[_], path: JavaList[String], isSingleton: Boolean = false)
		extends ConfigurableFactory[T](name, factoryType, parent, path, isSingleton) {
	def this(factoryType: Class[T], parent: ConfigurableFactory[_], path: JavaList[String]) =
		this("", factoryType, parent, path)
	def this(factoryType: Class[T], parent: ConfigurableFactory[_], path: JavaList[String], isSingleton: Boolean) =
		this("", factoryType, parent, path, isSingleton)

	import NumericallyConfigurableFactory._
	addProperty(NUMERIC_TYPE_PROPERTY)

	/**
	 * Run something with what is, in its calling place, an ExtendedNumeric of unspecified type, this time with
	 * the generification specified
	 */
	protected def withNumericType[RT] (fcn: ExtendedNumericWithConversion[_, _] => RT): RT = {
		val typeName = getPropertyValue(NUMERIC_TYPE_PROPERTY)
		val numericWithConversion: ExtendedNumericWithConversion[_, _] = typeName match {
			case "int" => new ExtendedNumericWithConversion[Int, JavaInt]()
			case "long" => new ExtendedNumericWithConversion[Long, JavaLong]()
			case "float" => new ExtendedNumericWithConversion[Float, JavaFloat]()
			case "double" => new ExtendedNumericWithConversion[Double, JavaDouble]()
		}
		fcn(numericWithConversion)
	}

	/**
	 * This function serves the purpose of the {@link ConfigurableFactory#create} function in normal factories.
	 * It includes generic numeric types to allow factories to create objects generified with the appropriate generic
	 * numeric.
	 * @param tag A ClassTag of the scala numeric base type
	 * @param numeric The scala extended numeric object that represents the type to use
	 * @param conversion A conversion object between the scala and java numeric types
	 * @tparam ST The scala extended numeric type to use
	 * @tparam JT The java numeric type to use
	 * @return The factory to be returned by ConfigurableFactory.create.
	 */
	protected def typedCreate[ST, JT] (tag: ClassTag[ST],
	                                   numeric: ExtendedNumeric[ST],
	                                   conversion: TypeConversion[ST, JT]): T

	override protected final def create(): T = {
		def extractNumerics[ST, JT] (nc: ExtendedNumericWithConversion[ST, JT]): T = {
			typedCreate(nc.tag, nc.numeric, nc.conversion)
		}
		withNumericType(extractNumerics(_))
	}
}
