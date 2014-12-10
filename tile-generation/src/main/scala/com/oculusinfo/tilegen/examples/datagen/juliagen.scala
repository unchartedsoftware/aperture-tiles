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

package com.oculusinfo.tilegen.examples.datagen


import scala.util.control.Breaks

// import com.oculusinfo.math.statistics.PoissonDistribution

import com.oculusinfo.tilegen.util.{ArgumentParser, MissingArgumentException}



/**
 * This object generates data for a Julia set, which can then be binned using
 * an inverse maximum aggregator or inverse average aggregator to allow
 * someone to investigate the Julia set.
 */
object JuliaSetGenerator {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)
		try {
			val jobName = "Julia Set Generation"
			val sc = argParser.getSparkConnector().createContext(Some(jobName))

			val cReal = argParser.getDouble("real",
			                                "The real portion of the "
				                                +"parameter defining the "
				                                +"Julia set to generate.")
			val cImag = argParser.getDouble("imag",
			                                "The imaginary portion of the "
				                                +"parameter defining the Julia "
				                                +"set to generate.")
			val minR = argParser.getDouble("minreal",
			                               "The minimum real value of the "
				                               +"input domain.",
			                               Some(-2.0))
			val maxR = argParser.getDouble("maxreal",
			                               "The maximum real value of the "
				                               +"input domain",
			                               Some(2.0))
			val minI = argParser.getDouble("minimag",
			                               "The minimum imaginary value of "
				                               +"the input domain",
			                               Some(-2.0))
			val maxI = argParser.getDouble("maximag",
			                               "The maximum imaginary value of "
				                               +"the input domain",
			                               Some(2.0))
			val samples = argParser.getLong("samples",
			                                "The number of sample points "
				                                +"to generate in our Julia set. "
				                                +"There will be some duplication "
				                                +"- point choice is random.  Also, "
				                                +"note that there can be no more "
				                                +"than about 2g samples per "
				                                +"partition",
			                                Some(10000000l))
			val partitions = argParser.getInt("partitions",
			                                  "The number of partitions "
				                                  +"into which to break up "
				                                  +"the data set.",
			                                  Some(5))
			val meanIterations =
				argParser.getInt("iter",
				                 "The average number of iterations to use "
					                 +"for each sample",
				                 Some(100))
			val outputFile = argParser.getString("output",
			                                     "The location to which to "
				                                     +"output the generated "
				                                     +"data.",
			                                     Some("julia"))


			val C = new Complex(cReal, cImag)
			//      val poisson = new PoissonDistribution(meanIterations)
			sc.parallelize(Range(0, partitions), partitions).flatMap(p =>
				{
					// Don't actually care about n, it's just there to make sure
					// the partition exists
					Range(0, (samples/partitions).toInt).iterator.map(s =>
						{
							// Again, don't care about m, just making sure there are the
							// right number of samples
							val zReal = math.random*(maxR-minR)+minR
							val zImag = math.random*(maxI-minI)+minI
							//          val N = poisson.sample
							val N = 1000
							var Z = new Complex(zReal, zImag)
							var top = 0
							val loop = new Breaks
							loop.breakable {
								for (n <- 0 until N) {
									Z = Z.square + C
									if (math.abs(Z.real) < 2.0) top = top + 1
									else loop.break
								}
							}
							// Output is in columns:
							//   1: real coordinate
							//   2: imaginary coordinate
							//   3: number of iterations
							//   4: julia function value
							//   5: inverse of julia function value (1/f)
							"%.6f\t%.6f\t%d\t%d\t%.6f\t%.6f".format(zReal, zImag, top, N,
							                                        Z.real, 1.0/Z.real)
						}
					)
				}
			).saveAsTextFile(outputFile)
		} catch {
			case e: MissingArgumentException => {
				println("JuliaSetGenerator - Generate a Julia Set")
				println("Argument exception: "+e.getMessage())
				argParser.usage
			}
		}
	}
}

class Complex (val real: Double, val imaginary: Double) extends Serializable {
	/** Complex conjugate */
	def bar: Complex =
		new Complex(this.real, -this.imaginary)


	def + (that: Complex): Complex =
		new Complex(this.real + that.real,
		            this.imaginary + that.imaginary)

	def - (that: Complex): Complex =
		new Complex(this.real - that.real,
		            this.imaginary - that.imaginary)

	def * (that: Complex): Complex =
		new Complex(this.real * that.real - this.imaginary * that.imaginary,
		            this.real*that.imaginary + this.imaginary * that.real)

	def / (that: Double): Complex =
		new Complex(this.real/that, this.imaginary/that)

	// a   a   b'   a b'
	// - = - * -- = ----
	// b   b   b'   b b'
	//
	// where b' represents the complex conjugate of b (so b b' is real)
	def / (that: Complex): Complex = {
		val thatbar = that.bar
		val denom = (that * thatbar).real
		(this * thatbar) / denom
	}

	def square: Complex =
		this * this
}
