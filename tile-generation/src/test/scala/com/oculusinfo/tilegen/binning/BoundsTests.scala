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

package com.oculusinfo.tilegen.binning



import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import org.scalatest.FunSuite

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.AOITilePyramid

import com.oculusinfo.tilegen.util.Rectangle



class BoundsTestSuite extends FunSuite {
	// Check each bounds test in both directions
	test("Union - disjoint bordering") {
		val b1 = new Bounds(1, new Rectangle[Int](1, 1, 1, 1), None)
		val b2 = new Bounds(1, new Rectangle[Int](1, 1, 2, 2), None)

		assert((b1 union b2).get.level === 1)
		assert((b1 union b2).get.indexBounds === new Rectangle[Int](1, 1, 1, 2))
		assert((b2 union b1).get.level === 1)
		assert((b2 union b1).get.indexBounds === new Rectangle[Int](1, 1, 1, 2))
	}

	test("Union - overlaps") {
		val b1 = new Bounds(1, new Rectangle[Int](1, 2, 1, 2), None)
		val b2 = new Bounds(1, new Rectangle[Int](2, 3, 2, 3), None)
		val b3 = new Bounds(1, new Rectangle[Int](1, 2, 2, 3), None)
		val b4 = new Bounds(1, new Rectangle[Int](2, 3, 1, 2), None)

		assert((b1 union b2).isEmpty)
		assert((b2 union b1).isEmpty)
		assert((b3 union b4).isEmpty)
		assert((b4 union b3).isEmpty)
		assert((b1 union b3).get.indexBounds === new Rectangle[Int](1, 2, 1, 3))
		assert((b3 union b1).get.indexBounds === new Rectangle[Int](1, 2, 1, 3))
		assert((b1 union b4).get.indexBounds === new Rectangle[Int](1, 3, 1, 2))
		assert((b4 union b1).get.indexBounds === new Rectangle[Int](1, 3, 1, 2))
		assert((b2 union b3).get.indexBounds === new Rectangle[Int](1, 3, 2, 3))
		assert((b3 union b2).get.indexBounds === new Rectangle[Int](1, 3, 2, 3))
		assert((b2 union b4).get.indexBounds === new Rectangle[Int](2, 3, 1, 3))
		assert((b4 union b2).get.indexBounds === new Rectangle[Int](2, 3, 1, 3))
	}

	test("Union - full containment") {
		val b1 = new Bounds(1, new Rectangle[Int](1, 2, 1, 2), None)
		val b2 = new Bounds(1, new Rectangle[Int](0, 3, 0, 3), None)

		assert((b1 union b2).get eq b2)
		assert((b2 union b1).get eq b2)
	}

	test("Union - fully disjoint") {
		val b1 = new Bounds(1, new Rectangle[Int](1, 1, 1, 1), None)
		val b2 = new Bounds(1, new Rectangle[Int](2, 2, 2, 2), None)

		assert((b1 union b2).isEmpty)
		assert((b2 union b1).isEmpty)
	}

	test("Union - different levels") {
		val b1 = new Bounds(1, new Rectangle[Int](1, 1, 1, 1), None)
		val b2 = new Bounds(2, new Rectangle[Int](1, 1, 1, 1), None)

		assert((b1 union b2).isEmpty)
		assert((b2 union b1).isEmpty)
	}

	test("Union - rest") {
		val b4 = new Bounds(1, new Rectangle[Int](0, 1, 3, 4), None)
		val b3 = new Bounds(1, new Rectangle[Int](0, 1, 2, 3), None)
		val b2 = new Bounds(1, new Rectangle[Int](0, 1, 1, 2), Some(b4))
		val b1 = new Bounds(1, new Rectangle[Int](0, 1, 0, 1), Some(b3))

		val union = b1 union b2

		assert(union.get.indexBounds === new Rectangle[Int](0, 1, 0, 2))
		assert(union.get.next.get.indexBounds === new Rectangle[Int](0, 1, 2, 3))
		assert(union.get.next.get.next.get.indexBounds === new Rectangle[Int](0, 1, 3, 4))
	}

	test("Filter") {
		val b4 = new Bounds(1, new Rectangle[Int](0, 1, 3, 4), None)
		val b3 = new Bounds(1, new Rectangle[Int](0, 1, 2, 3), Some(b4))
		val b2 = new Bounds(1, new Rectangle[Int](0, 1, 1, 2), Some(b3))
		val b1 = new Bounds(1, new Rectangle[Int](0, 1, 0, 1), Some(b2))

		val filtered = b1.filter(_.indexBounds.minY > 1).get

		assert(2 == filtered.length)
		assert(filtered.indexBounds === new Rectangle[Int](0, 1, 2, 3))
		assert(filtered.next.get.indexBounds === new Rectangle[Int](0, 1, 3, 4))
	}

	test("Reduce") {
		val b4 = new Bounds(1, new Rectangle[Int](0, 1, 3, 4), None)
		val b3 = new Bounds(1, new Rectangle[Int](0, 1, 2, 3), Some(b4))
		val b2 = new Bounds(1, new Rectangle[Int](0, 1, 1, 2), Some(b3))
		val b1 = new Bounds(1, new Rectangle[Int](0, 1, 0, 1), Some(b2))

		val reduction = b1.reduce

		assert(reduction.get.indexBounds === new Rectangle[Int](0, 1, 0, 4))
		assert(reduction.get.next.isEmpty)
	}

	test("Trivial reduce") {
		val b = new Bounds(1, new Rectangle[Int](0, 0, 0, 0), None)
		assert(b.reduce.isEmpty)
	}

	test("Containment test - containment ") {
		val b4 = new Bounds(4, new Rectangle[Int](12, 16, 12, 16), None)
		val b3 = new Bounds(4, new Rectangle[Int]( 8, 12,  8, 12), Some(b4))
		val b2 = new Bounds(4, new Rectangle[Int]( 4,  8,  4,  8), Some(b3))
		val b1 = new Bounds(4, new Rectangle[Int]( 0,  4,  0,  4), Some(b2))

		val pyramid = new AOITilePyramid(0.0, 0.0, 1.0, 1.0)
		val containmentTest = b1.getSerializableContainmentTest(pyramid)

		// diagonal
		assert(containmentTest(0.000, 0.000))    // b1 corner
		assert(containmentTest(0.125, 0.125))    // b1 center
		assert(containmentTest(0.250, 0.250))    // b1-b2 corner
		assert(containmentTest(0.375, 0.375))    // b2 center
		assert(containmentTest(0.500, 0.500))    // b2-b3 corner
		assert(containmentTest(0.625, 0.625))    // b3 center
		assert(containmentTest(0.750, 0.750))    // b3-b4 corner
		assert(containmentTest(0.875, 0.875))    // b4 center
		assert(containmentTest(1.000, 1.000))    // b4 corner

		// Outer corners
		assert(containmentTest(0.00, 0.25))      // b1
		assert(containmentTest(0.25, 0.00))
		assert(containmentTest(0.25, 0.50))      // b2
		assert(containmentTest(0.50, 0.25))
		assert(containmentTest(0.50, 0.75))      // b3
		assert(containmentTest(0.75, 0.50))
		assert(containmentTest(0.75, 1.00))      // b4
		assert(containmentTest(1.00, 0.75))

		// Outside
		assert(!containmentTest(0.125, 0.375))
		assert(!containmentTest(0.375, 0.125))
		assert(!containmentTest(0.375, 0.625))
		assert(!containmentTest(0.625, 0.375))
		assert(!containmentTest(0.625, 0.875))
		assert(!containmentTest(0.875, 0.625))
	}

	test("Containment test - serialization") {
		val b = new Bounds(1, new Rectangle[Int](0, 0, 0, 0), None)

		val initialContainmentTest = {
			// Put pyramid in a buried block where our deserialized variable won't be
			// able to find it - just to be sure
			val pyramid = new AOITilePyramid(0.0, 0.0, 1.0, 1.0)
			b.getSerializableContainmentTest(pyramid)
		}

		assert(initialContainmentTest(0.25, 0.25))
		assert(!initialContainmentTest(0.25, 0.75))
		assert(!initialContainmentTest(0.75, 0.25))
		assert(!initialContainmentTest(0.75, 0.75))

		// Try serializing and deserializing it
		val baos = new ByteArrayOutputStream()
		val oas = new ObjectOutputStream(baos)
		oas.writeObject(initialContainmentTest)
		oas.flush()
		oas.close()
		baos.flush()
		baos.close()

		val serializedData = baos.toByteArray

		val bais = new ByteArrayInputStream(serializedData)
		val ois = new ObjectInputStream(bais)

		val streamedContainmentTest = ois.readObject.asInstanceOf[(Double, Double) => Boolean]

		assert(streamedContainmentTest(0.25, 0.25))
		assert(!streamedContainmentTest(0.25, 0.75))
		assert(!streamedContainmentTest(0.75, 0.25))
		assert(!streamedContainmentTest(0.75, 0.75))
	}


	def assertTBSetsEquivalent (label: String, xBins: Int, yBins: Int,
	                            actual: TraversableOnce[(TileIndex, BinIndex)],
	                            expected: Set[(Int, Int, Int, Int, Int)]): Unit = {
		actual.foreach(indices =>
			{
				assert(xBins === indices._1.getXBins())
				assert(yBins === indices._1.getYBins())
			}
		)
		val expectedSet = expected.map(datum =>
			{
				(new TileIndex(datum._1, datum._2, datum._3, xBins, yBins),
				 new BinIndex(datum._4, datum._5))
			}
		)

		val actualSet = actual.toSet

		val extra = actualSet.diff(expectedSet)
		val missing = expectedSet.diff(actualSet)

		assert(extra.isEmpty && missing.isEmpty,
		       "\n"+label+": Missing values: "+missing+"\nextra values: "+extra)
	}

	test("Spreading function - mapping") {

		// Level 0 1x1
		// level 1 2x2
		// level 2 4x4
		// level 3 8x8
		// level 4 16x16
		val b4 = new Bounds(4, new Rectangle[Int](0, 0, 0, 0), None)
		val b3 = new Bounds(4, new Rectangle[Int](2, 2, 2, 2), Some(b4))
		val b2 = new Bounds(3, new Rectangle[Int](0, 1, 0, 1), Some(b3))
		val b1 = new Bounds(2, new Rectangle[Int](0, 1, 0, 1), Some(b2))

		val pyramid = new AOITilePyramid(0.0, 0.0, 16.0, 16.0)
		val bins = 4
		val spreaderFcn = b1.getSpreaderFunction[Double](pyramid, bins, bins)

		assertTBSetsEquivalent("0, 0", bins, bins,
		                       spreaderFcn(0, 0),
		                       Set((2, 0, 0, 0, 3),
		                           (3, 0, 0, 0, 3),
		                           (4, 0, 0, 0, 3)))

		assertTBSetsEquivalent("2, 2", bins, bins,
		                       spreaderFcn(2, 2),
		                       Set((4, 2, 2, 0, 3),
		                           (3, 1, 1, 0, 3),
		                           (2, 0, 0, 2, 1)))

		assertTBSetsEquivalent("2.25, 1.25", bins, bins,
		                       spreaderFcn(2.25, 1.25),
		                       Set((3, 1, 0, 0, 1),
		                           (2, 0, 0, 2, 2)))

		assertTBSetsEquivalent("6.5, 1.5", bins, bins,
		                       spreaderFcn(6.5, 1.5),
		                       Set((2, 1, 0, 2, 2)))

		assertTBSetsEquivalent("9, 9", bins, bins,
		                       spreaderFcn(9.0, 9.0),
		                       Set[(Int, Int, Int, Int, Int)]())
	}

	test("Spreading function - serialization") {
		val b = new Bounds(1, new Rectangle[Int](0, 0, 0, 0), None)

		val initialSpreaderFcn = {
			// Put pyramid and bins in a buried block where our deserialized variable
			// won't be able to find them - just to be sure
			val pyramid = new AOITilePyramid(0.0, 0.0, 1.0, 1.0)
			val bins = 4
			b.getSpreaderFunction[Double](pyramid, bins, bins)
		}

		assertTBSetsEquivalent("initial", 4, 4,
		                       initialSpreaderFcn(0, 0),
		                       Set((1, 0, 0, 0, 3)))

		// Try serializing and deserializing it
		val baos = new ByteArrayOutputStream()
		val oas = new ObjectOutputStream(baos)
		oas.writeObject(initialSpreaderFcn)
		oas.flush()
		oas.close()
		baos.flush()
		baos.close()

		val serializedData = baos.toByteArray

		val bais = new ByteArrayInputStream(serializedData)
		val ois = new ObjectInputStream(bais)

		def getStreamedSpreaderFcn[T] =
			ois.readObject.asInstanceOf[(Double, Double) => TraversableOnce[(TileIndex, BinIndex)]]
		val streamedSpreaderFcn = getStreamedSpreaderFcn[Double]

		assertTBSetsEquivalent("serialized", 4, 4,
		                       streamedSpreaderFcn(0, 0),
		                       Set((1, 0, 0, 0, 3)))
	}
}

