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

import org.scalatest.FunSuite
import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TileAndBinIndices

/**
 * @author nkronenfeld
 */
class StandardBinningFunctionsTestSuite extends FunSuite {
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
		class Functions extends StandardArcBinningFunctions
		val functions = new Functions
		val s2 = math.sqrt(2)
		val s3 = math.sqrt(3)

		def assertArcInfo (expected: (Double, Double, Double, Double, Double, Seq[Int]),
		                   actual: (Int, Int, Int, Double, Double, Seq[Int])) {
			assert(math.round(expected._1).toInt === actual._1)
			assert(math.round(expected._2).toInt === actual._2)
			assert(math.round(expected._3).toInt === actual._3)
			assert(expected._4 === actual._4)
			assert(expected._5 === actual._5)
			assert(expected._6.toList === actual._6.toList)
		}
		var arcInfo: (Int, Int, Int, Double, Double, Seq[Int]) = null

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

			assert(ApproximateInt(0, math.ceil(epsilon*3).toInt) === arcInfo._1, "(X center coordinate differed)")
			assert(ApproximateInt(0, math.ceil(epsilon*3).toInt) === arcInfo._2, "(Y center coordinate differed)")
			assert(ApproximateInt(100, math.ceil(epsilon*2).toInt) === arcInfo._3, "(Radius differed)")
			// Tiny perturbations in rounding can cause huge perturbations in the slope (like
			// changing 1E6 to -1E3), so we really can't test slopes.
			val o1 = theta1/45
			val o2 = theta2/45
			val o1s = if (theta1%45 == 0) List(o1, (o1+1)%8) else List(o1)
			val o2s = if (theta2%45 == 0) List(o2, (o2+7)%8) else List(o2)

			val possibleOctants = for (oct1 <- o1s; oct2 <- o2s) yield
				if (oct2 < oct1) (oct2 to oct1).map(_ % 8).toList
				else (oct2 to (oct1 + 8)).map(_ % 8).toList
			val anyEqual = possibleOctants.map(_ == arcInfo._6.toList).reduce(_ || _)
			assert(possibleOctants.map(_ == arcInfo._6.toList).reduce(_ || _),
			       "Octants differed, got "+arcInfo._6.toList+", expected one of "+possibleOctants)
		}
	}



	test("Test simple arc") {
		class Functions extends StandardArcBinningFunctions
		val functions = new Functions
		val allBins = functions.arcUniversalBins(new BinIndex(-7, 12), new BinIndex(7, 12))
		val binList = allBins.toList
		val sortedBinList = binList.sortBy(_.getX)
		assert(15 === sortedBinList.size)
	}
}

case class ApproximateInt (i: Int, epsilon: Int) {
	override def toString = i+"+/-"+epsilon
	override def equals (that: Any): Boolean = {
		that match {
			case approx: ApproximateInt =>
				if (approx.epsilon > epsilon) approx.equals(i)
				else this.equals(approx.i)
				
			case exact: Int => i-epsilon <= exact && exact <= i+epsilon

			case _ => false
		}
	}
}
case class ApproximateDouble (d: Double, epsilon: Double) {
	override def toString = d+"+/-"+epsilon
	override def equals (that: Any): Boolean = {
		that match {
			case approx: ApproximateDouble =>
				if (approx.epsilon > epsilon) approx.equals(d)
				else this.equals(approx.d)
				
			case exact: Double => d-epsilon <= exact && exact <= d+epsilon

			case _ => false
		}
	}
}
