package com.oculusinfo.tilegen.util

import java.lang.{Byte => JavaByte}
import java.lang.{Short => JavaShort}
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}

/**
 * TypeConversion is a simple class to encapsulate bidirectional conversion 
 * between two types
 * 
 * The most typical case is converting between scala and java primitive types,
 * and we make these conversions implicit for ease of use.
 * 
 * @tparam S The first type
 * @tparam T The second type
 */
trait TypeConversion[S, T] extends Serializable {
	def forwards (n: S): T
	def backwards (n: T): S
}
object TypeConversion {
	implicit object IntPair extends TypeConversion[Int, JavaInt] {
		def forwards (n: Int): JavaInt = Int.box(n)
		def backwards (n: JavaInt): Int = Int.unbox(n)
	}
	implicit object LongPair extends TypeConversion[Long, JavaLong] {
		def forwards (n: Long): JavaLong = Long.box(n)
		def backwards (n: JavaLong): Long = Long.unbox(n)
	}
	implicit object FloatPair extends TypeConversion[Float, JavaFloat] {
		def forwards (n: Float): JavaFloat = Float.box(n)
		def backwards (n: JavaFloat): Float = Float.unbox(n)
	}
	implicit object DoublePair extends TypeConversion[Double, JavaDouble] {
		def forwards (n: Double): JavaDouble = Double.box(n)
		def backwards (n: JavaDouble): Double = Double.unbox(n)
	}
}
