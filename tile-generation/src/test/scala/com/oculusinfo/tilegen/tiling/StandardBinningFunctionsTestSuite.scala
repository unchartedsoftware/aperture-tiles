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



import scala.util.Try

import org.scalatest.FunSuite

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TileAndBinIndices
import com.oculusinfo.tilegen.util.ExtendedNumeric



/**
 * @author nkronenfeld
 */
class StandardBinningFunctionsTestSuite extends FunSuite {
	class Functions extends StandardArcBinningFunctions



	test("for vs while") {
		def time (f: () => Unit): Double = {
			val start = System.nanoTime()
			f()
			val end = System.nanoTime()
			                         (end-start)/1000000.0
		}
		val n = 100000000
		println("For comprehension: %.4fms".format(time(() => for (x <- 1 to n){})))
		println("While loop: %.4fms".format(time(() => {
			                                         var x=0
			                                         while (x < n) {x += 1}
		                                         })))
		println("While iterator: %.4fms".format(time(() => {
			                                             var x = 0
			                                             new WhileIterator(() => (x < n), () => x = x + 1).foreach(x => {})
		                                             })))
		var x = 0
		val wi = new WhileIterator(() => (x < n), () => x = x + 1)
		println("While iterator internals: %.4fms".format(time(() => wi.foreach(x => {}))))
	}



	test("Test various Bresneham line functions against each other") {
		val sortTiles: (TileIndex, TileIndex) => Boolean = (a, b) => {
			a.getX < b.getX || (a.getX == b.getX && a.getY < b.getY)
		}

		val sortBins: (BinIndex, BinIndex) => Boolean = (a, b) => {
			a.getX < b.getX || (a.getX == b.getX && a.getY < b.getY)
		}


		// Test a set of endpoints to make see if the calculation of tiles through simple
		// Bresneham and tiled Bresneham match
		def testEndpoints (start: BinIndex, end: BinIndex, sample: TileIndex) = {
			val bins = StandardBinningFunctions
				.linearUniversalBins(start, end)
				.map(TileIndex.universalBinIndexToTileBinIndex(sample, _))
			val binTiles = bins.map(_.getTile).toSet.toList.sortWith(sortTiles)

			val tiles = StandardBinningFunctions.linearTiles(start, end, sample).toSet.toList.sortWith(sortTiles)

			assert(binTiles == tiles)

			tiles.map{tile =>
				val subsetBins = bins.filter(_.getTile == tile).map(_.getBin).toList.sortWith(sortBins)
				val tileBins = StandardBinningFunctions.linearBinsForTile(start, end, tile).toList.sortWith(sortBins)

				assert(subsetBins == tileBins)
			}
		}

		// level 9: 131072 bins
		val sample= new TileIndex(9, 0, 0)

		Range(0, 256).foreach{offset =>
			// Long lines
			testEndpoints(new BinIndex(23309+offset, 55902), new BinIndex(24326+offset, 56447), sample)
			testEndpoints(new BinIndex(23309, 55902+offset), new BinIndex(24326, 56447+offset), sample)
			// Short, but multi-tile lines
			testEndpoints(new BinIndex(23309+offset, 55902), new BinIndex(23701+offset, 55793), sample)
			testEndpoints(new BinIndex(23309, 55902+offset), new BinIndex(23701, 55793+offset), sample)
			// Very short lines
			testEndpoints(new BinIndex(23309+offset, 55902), new BinIndex(23325+offset, 55912), sample)
			testEndpoints(new BinIndex(23309, 55902+offset), new BinIndex(23325, 55912+offset), sample)
		}
	}


	test("Test arc initialization") {
		val functions = new Functions
		val s2 = math.sqrt(2)
		val s3 = math.sqrt(3)

		def assertArcInfo (expected: (Double, Double, Double, Double, Double, Seq[Int]),
		                   actual: (Double, Double, Double, Double, Double, Seq[(Int, Boolean, Boolean)])) {
			assert(expected._1 === actual._1)
			assert(expected._2 === actual._2)
			assert(expected._3 === actual._3)
			assert(expected._4 === actual._4)
			assert(expected._5 === actual._5)
			assert(expected._6.toList === actual._6.map(_._1).toList)
		}
		var arcInfo: (Double, Double, Double, Double, Double, Seq[(Int, Boolean, Boolean)]) = null

		// Test the 4 basic axis-crossing chords
		arcInfo = functions.initializeArc(new BinIndex(10, 10), new BinIndex(10, -10))
		assertArcInfo((10-10*s3, 0.0, 20.0, 1.0/s3, -1.0/s3, List(7, 0)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(-10, 10), new BinIndex(10, 10))
		assertArcInfo((0.0, 10-10*s3, 20.0, -s3, s3, List(1, 2)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(-10, -10), new BinIndex(-10, 10))
		assertArcInfo((10.0*s3-10.0, 0.0, 20.0, 1.0/s3, -1.0/s3, List(3, 4)), arcInfo)
		
		arcInfo = functions.initializeArc(new BinIndex(10, -10), new BinIndex(-10, -10))
		assertArcInfo((0.0, 10*s3-10.0, 20.0, -s3, s3, List(5, 6)), arcInfo)

		// Same thing, with reversed coordinate order
		arcInfo = functions.initializeArc(new BinIndex(10, -10), new BinIndex(10, 10))
		assertArcInfo(( 10.0 + 10.0*s3, 0.0, 20.0, 1.0/s3, -1.0/s3, List(3, 4)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(10, 10), new BinIndex(-10, 10))
		assertArcInfo((0.0,  10.0 + 10.0*s3, 20.0, -s3, s3, List(5, 6)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(-10, 10), new BinIndex(-10, -10))
		assertArcInfo((-10.0 - 10.0*s3, 0.0, 20.0, 1.0/s3, -1.0/s3, List(7, 0)), arcInfo)
		
		arcInfo = functions.initializeArc(new BinIndex(-10, -10), new BinIndex(10, -10))
		assertArcInfo((0.0, -10.0 - 10.0*s3, 20.0, -s3, s3, List(1, 2)), arcInfo)

		// Test the 4 basic diagonals
		val cp = 5.0 * s3 + 5.0
		val cm = 5.0 * s3 - 5.0
		arcInfo = functions.initializeArc(new BinIndex(0, 10), new BinIndex(10, 0))
		assertArcInfo((-cm, -cm, 10.0*s2, cp / cm, cm / cp, List(0, 1)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(-10, 0), new BinIndex(0, 10))
		assertArcInfo((cm, -cm, 10.0*s2, - cm / cp, - cp / cm, List(2, 3)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(0, -10), new BinIndex(-10, 0))
		assertArcInfo((cm, cm, 10.0*s2, cp / cm, cm / cp, List(4, 5)), arcInfo)

		arcInfo = functions.initializeArc(new BinIndex(10, 0), new BinIndex(0, -10))
		assertArcInfo((-cm, cm, 10.0*s2, - cm / cp, - cp / cm, List(6, 7)), arcInfo)


		// test all 0-centerd arcs in a circle
		val slopeEpsilon = 0.1
		// Our basic maximum point offset
		val epsilon = math.sqrt(2)/2

		(0 to 359).foreach{theta2 =>
			val theta1 = theta2+60
			val t1 = math.toRadians(theta1)
			val t2 = math.toRadians(theta2)
			val arcInfo = functions.initializeArc(new BinIndex(math.round(100*math.cos(t1)).toInt,
			                                                   math.round(100*math.sin(t1)).toInt),
			                                      new BinIndex(math.round(100*math.cos(t2)).toInt,
			                                                   math.round(100*math.sin(t2)).toInt))

			assert(ApproximateNumber(0.0, epsilon*3) === arcInfo._1, "(X center coordinate differed)")
			assert(ApproximateNumber(0.0, epsilon*3) === arcInfo._2, "(Y center coordinate differed)")
			assert(ApproximateNumber(100.0, epsilon*2) === arcInfo._3, "(Radius differed)")
			// Tiny perturbations in rounding can cause huge perturbations in the slope (like
			// changing 1E6 to -1E3), so we really can't test slopes.
			val o1 = theta1/45
			val o2 = theta2/45
			val o1s = if (theta1%45 == 0) List(o1, (o1+1)%8) else List(o1)
			val o2s = if (theta2%45 == 0) List(o2, (o2+7)%8) else List(o2)

			val possibleOctants = for (oct1 <- o1s; oct2 <- o2s) yield
				if (oct2 < oct1) (oct2 to oct1).map(_ % 8).toList
				else (oct2 to (oct1 + 8)).map(_ % 8).toList
			assert(possibleOctants.map(_ == arcInfo._6.map(_._1).toList).reduce(_ || _),
			       "Octants differed, got "+arcInfo._6.toList+", expected one of "+possibleOctants)
		}
	}



	private def bi (x: Int, y: Int): BinIndex = new BinIndex(x, y)
	private val binSorter: (BinIndex, BinIndex) => Boolean = (a, b) => {
		val angleA = math.atan2(a.getY, a.getX)
		val angleB = math.atan2(b.getY, b.getX)
		angleA < angleB
	}



	test("Test simple arcs - symetrical across axis") {
		val functions = new Functions
		val bins = functions.arcUniversalBins2(bi(-7, 12), bi(7, 12)).toList.sortWith(binSorter)

		// Make sure our arc bounds are correct
		assert(12 === bins.map(_.getY).reduce(_ min _))
		assert(14 === bins.map(_.getY).reduce(_ max _))
		assert(-7 === bins.map(_.getX).reduce(_ min _))
		assert(7 === bins.map(_.getX).reduce(_ max _))

		// Make sure the results are symetrical around the X axis
		bins.foreach(bin => bins.contains(new BinIndex(-bin.getX, bin.getY)))

		// Make sure there are no gaps
		bins.sliding(2).foreach{pair =>
			assert(math.abs(pair(1).getX-pair(0).getX) < 2, "Gap between "+pair(0)+" and "+pair(1))
			assert(math.abs(pair(1).getY-pair(0).getY) < 2, "Gap between "+pair(0)+" and "+pair(1))
		}

		// Make sure there are no duplicate points
		assert(bins.size === bins.toSet.size)

		// Make sure the results are all approximately 14 from (0, 12 - 7 sqrt(3)) (i.e., the real center)
		val idealY = 12.0 - 7.0 * math.sqrt(3)
		bins.foreach{bin =>
			val x = bin.getX
			val y = bin.getY - idealY
			val r = math.sqrt((x * x) + (y * y))
			assert(new ApproximateNumber(14.0, 0.75) === r)
		}
	}
	
	
	
	test("Test simple arcs - symetrical across diagonal") {
		val functions = new Functions
		val bins = functions.arcUniversalBins2(bi(7, 27), bi(27, 7)).toList.sortWith(binSorter)

		// Make sure our arc bounds are correct
		assert(7 === bins.map(_.getY).reduce(_ min _))
		assert(27 === bins.map(_.getY).reduce(_ max _))
		assert(7 === bins.map(_.getX).reduce(_ min _))
		assert(27 === bins.map(_.getX).reduce(_ max _))

		// Make sure the results are symetrical around the diagonal
		bins.foreach(bin => bins.contains(new BinIndex(bin.getY, bin.getX)))

		// Make sure there are no gaps
		bins.sliding(2).foreach{pair =>
			assert(math.abs(pair(1).getX-pair(0).getX) < 2, "Gap between "+pair(0)+" and "+pair(1))
			assert(math.abs(pair(1).getY-pair(0).getY) < 2, "Gap between "+pair(0)+" and "+pair(1))
		}

		// Make sure there are no duplicates
		assert(bins.size == bins.toSet.size)

		// Make sure the results are all the right distance from the true center.
		// The chord is 20 sqrt(2) long
		// so the distance from the chord to the center is 10 sqrt(6)
		// so the distance along each axis from the chord center to the center is 10 sqrt(3)
		val idealR = 20 * math.sqrt(2)
		val idealC = 17.0 - 10.0 * math.sqrt(3)
		bins.foreach{bin =>
			val x = bin.getX - idealC
			val y = bin.getY - idealC
			val r = math.sqrt((x * x) + (y * y))
			assert(new ApproximateNumber(idealR, 0.75) === r)
		}
	}
}

object ApproximateNumber {
	def apply[T: ExtendedNumeric] (target: T, epsilon: T) = new ApproximateNumber[T](target, epsilon)
}
class ApproximateNumber [T: ExtendedNumeric] (val target: T, val epsilon: T) {
	override def toString = target+"+/-"+epsilon
	override def equals (that: Any): Boolean = {
		val numeric = implicitly[ExtendedNumeric[T]]
		import numeric.mkNumericOps
		import numeric.mkOrderingOps

		that match {
			case approx: ApproximateNumber[T] =>
				if (approx.epsilon > epsilon) approx.equals(target)
				else this.equals(approx.target)

			case other => {
				Try({
					    val exact = numeric.fromAny(other)
					    (target-epsilon <= exact && exact <= target+epsilon)
				    }).getOrElse(false)
			}
		}
	}
}
