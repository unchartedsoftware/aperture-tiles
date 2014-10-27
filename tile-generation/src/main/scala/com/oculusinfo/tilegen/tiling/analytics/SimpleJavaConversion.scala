package com.oculusinfo.tilegen.tiling.analytics

import java.lang.{Byte => JavaByte}
import java.lang.{Short => JavaShort}
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}

trait ScalaJavaTypePair[ST, JT] extends Serializable {
	def asJava (n: ST): JT
	def asScala (n: JT): ST
}
object ScalaJavaTypePair {
	implicit object IntPair extends ScalaJavaTypePair[Int, JavaInt] {
		def asJava (n: Int): JavaInt = Int.box(n)
		def asScala (n: JavaInt): Int = Int.unbox(n)
	}
	implicit object LongPair extends ScalaJavaTypePair[Long, JavaLong] {
		def asJava (n: Long): JavaLong = Long.box(n)
		def asScala (n: JavaLong): Long = Long.unbox(n)
	}
	implicit object FloatPair extends ScalaJavaTypePair[Float, JavaFloat] {
		def asJava (n: Float): JavaFloat = Float.box(n)
		def asScala (n: JavaFloat): Float = Float.unbox(n)
	}
	implicit object DoublePair extends ScalaJavaTypePair[Double, JavaDouble] {
		def asJava (n: Double): JavaDouble = Double.box(n)
		def asScala (n: JavaDouble): Double = Double.unbox(n)
	}
}
