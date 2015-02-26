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
 * This object generates data for mulitple simultaneous Julia sets, which can then be 
 * binned using an inverse maximum aggregator or inverse average aggregator to allow
 * someone to investigate the Julia sets.
 */
object MultiJuliaSetGenerator {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)
		try {
			val jobName = "Julia Set Generation"
			val sc = argParser.getSparkConnector().createContext(Some(jobName))

			val cRealMin = argParser.getDouble("realmin",
			                                   "The minimum value for the real portion of the parameter defining the Julia set to generate.")
			val cRealMax = argParser.getDouble("realmax",
			                                   "The maximum value for the real portion of the parameter defining the Julia set to generate.")
			val cRealSteps = argParser.getInt("realsteps",
			                                  "The number of steps of real valuse to generate.")
			val cImagMin = argParser.getDouble("imagmin",
			                                   "The minimum value for the real portion of the parameter defining the Julia set to generate.")
			val cImagMax = argParser.getDouble("imagmax",
			                                   "The maximum value for the real portion of the parameter defining the Julia set to generate.")
			val cImagSteps = argParser.getInt("imagsteps",
			                                  "The number of steps of real valuse to generate.")
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


			val realValues = if (1 == cRealSteps) Seq(cRealMin)
			else (0 until cRealSteps).map(n => cRealMin + n * (cRealMax - cRealMin) / (cRealSteps-1))

			val imagValues = if (1 == cImagSteps) Seq(cImagMin)
			else (0 until cImagSteps).map(n => cImagMin + n * (cImagMax - cImagMin) / (cImagSteps-1))

			println
			println
			println
			println("real values: "+realValues)
			println("Real steps: "+cRealSteps+", min: "+cRealMin+", max: "+cRealMax)
			println("imaginary values: "+imagValues)
			println("imaginary stpes: "+cImagSteps+", min: "+cImagMin+", max: "+cImagMax)
			println
			println
			println

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
							val N = 1000

							val coords = "%.6f\t%.6f\t%d\t".format(zReal, zImag, N)

							val julias = for (r <- realValues; c <- imagValues) yield {
								val C = new Complex(r, c)
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
								"%d\t%.6f\t%.6f".format(top, Z.real, 1.0/Z.real)
							}
							println("There are "+julias.size+" for point "+zReal+"+i"+zImag)

							// Output is in columns:
							//   1: real coordinate
							//   2: imaginary coordinate
							//   3: max number of iterations
							//   1+3n : number of iterations for julia function n
							//   2+3n : end value for julia function n
							//   3+3n : inverse of end value for julia function n
							coords + julias.mkString("\t")
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
