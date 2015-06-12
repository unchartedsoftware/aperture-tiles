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



import com.oculusinfo.tilegen.util.Rectangle

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid



/**
 * A set of tiles that can be reduced used to test for point containment efficiently
 */
private[binning]
class Bounds (val level: Int,
              val indexBounds: Rectangle[Int],
              val next: Option[Bounds]) {

	private def getLocalAreaBounds (pyramid: TilePyramid, xBins: Int, yBins: Int): Rectangle[Double] = {
		def rectFromIdx (level: Int, x: Int, y: Int): Rectangle[Double] = {
			val idx = new TileIndex(level, x, y, xBins, yBins)
			Rectangle.fromJava(pyramid.getTileBounds(idx))
		}

		// Get the bounding rectangle for each corner tile of our bounded area
		val imm = rectFromIdx(level, indexBounds.minX, indexBounds.minY)
		val iMm = rectFromIdx(level, indexBounds.maxX, indexBounds.minY)
		val imM = rectFromIdx(level, indexBounds.minX, indexBounds.maxY)
		val iMM = rectFromIdx(level, indexBounds.maxX, indexBounds.maxY)

		// Combine them
		(imm union iMM union iMm union imM)
	}

	/**
	 * Get a test that can be serialized across the network, and used on each
	 * node to test the relevancy of any given data point
	 */
	def getSerializableContainmentTest (pyramid: TilePyramid, xBins: Int = 256, yBins: Int = 256):
			(Double, Double) => Boolean = {
		val ourAreaBounds = getLocalAreaBounds(pyramid, xBins, yBins)
		val continuedContainmentTest = next.map(_.getSerializableContainmentTest(pyramid, xBins, yBins))

		(x: Double, y: Double) =>
		ourAreaBounds.contains(x, y) || continuedContainmentTest.map(_(x, y)).getOrElse(false)
	}

	/**
	 * Get a function that can be passed to RDDBinner.processData, that will 
	 * spread individual data points across any relevent tiles within our 
	 * described bounds.  This function can be serialized across the network.
	 * 
	 * @param pyramid A tile pyramid describing the tile/bin structure of space
	 * @param xBins The number of bins per tile along the horizontal axis
	 * @param yBins The number of bins per tile along the vertical axis
	 */
	def getSpreaderFunction[T] (pyramid: TilePyramid, xBins: Int = 256, yBins: Int = 256) :
			(Double, Double) => Traversable[(TileIndex, BinIndex)] =
		getSpreaderFunctionInternal[T](pyramid, xBins, yBins)

	private def getSpreaderFunctionInternal[T] (pyramid: TilePyramid, xBins: Int = 256, yBins: Int = 256) :
			(Double, Double) => Seq[(TileIndex, BinIndex)] = {
		val ourAreaBounds = getLocalAreaBounds(pyramid, xBins, yBins)
		val continuedSpreaderFcn = next.map(_.getSpreaderFunctionInternal[T](pyramid, xBins, yBins))
		val localLevel = level

		(x, y) => {
			val optionRest = continuedSpreaderFcn.map(_(x, y))
			val rest = if (optionRest.isDefined) optionRest.get else Seq[(TileIndex, BinIndex)]()
			if (ourAreaBounds.contains(x, y)) {
				val tile = pyramid.rootToTile(x, y, localLevel, xBins, yBins)
				val bin = pyramid.rootToBin(x, y, tile)
				(tile, bin) +: rest
			} else {
				rest
			}
		}
		
	}



	/*
	 * Calculates a single bounds object that represents the union of the top 
	 * element of this and that bounds lists.
	 * 
	 * @return None if the top elements cannot be strictly combined.
	 */
	private def unionTop (that: Bounds): Option[Bounds] = {
		// We can only combine if bounds are on the same level, and match in
		// either X or y
		if (this.level != that.level) {
			// Levels unequal, therefore bounds are not unionable
			None
		} else if (this.indexBounds.contains(that.indexBounds)) {
			// this contains that - return this
			Some(this)
		} else if (that.indexBounds.contains(this.indexBounds)) {
			// that contains this - return that
			Some(that)
		} else if (this.indexBounds.xRange == that.indexBounds.xRange) {
			// X bounds are equal
			if (this.indexBounds.yRange == that.indexBounds.yRange) {
				// Y bounds are also equal - rectagles are equal
				Some(this)
			} else if (this.indexBounds.maxY < that.indexBounds.minY-1 ||
				           this.indexBounds.minY > that.indexBounds.maxY+1) {
				// Y bounds are disjoint - no overlap
				None
			} else {
				// Y bounds overlap - return union
				Some(new Bounds(this.level,
				                this.indexBounds union that.indexBounds,
				                None))
			}
		} else if (this.indexBounds.yRange == that.indexBounds.yRange) {
			// Y bounds are equal.  Note X bounds cannot be equal if we got this far.
			if (this.indexBounds.maxX < that.indexBounds.minX-1 ||
				    this.indexBounds.minX > that.indexBounds.maxX+1) {
				// X bounds are disjoint - no overlap
				None
			} else {
				// X bounds overlap - return union
				Some(new Bounds(this.level,
				                this.indexBounds union that.indexBounds,
				                None))
			}
		} else {
			None
		}
	}

	private def unionRest  (that: Bounds): Option[Bounds] = {
		this.next.map(_.append(that.next)).orElse(that.next)
	}

	/**
	 * This method attempts to combine the top elements of this and that, linked 
	 * to the union of the remainder of each.  Both this and that should be 
	 * sorted at first for this to be a reasonable thing to do, and even then, 
	 * usage is complex; most users should simply use reduce instead.
	 * 
	 * @return None if the top elements cannot be combined.
	 */
	def union (that: Bounds): Option[Bounds] = {
		val combo = unionTop(that)
		if (combo.isEmpty) {
			None
		} else {
			// Check for strict containment and and empty rest in the contained bounds
			if (combo.get.eq(this) && that.next.isEmpty) {
				Some(this)
			} else if (combo.get.eq(that) && this.next.isEmpty) {
				Some(that)
			} else {
				val rest = unionRest(that)
				Some(new Bounds(combo.get.level, combo.get.indexBounds, rest))
			}
		}
	}

	/**
	 * Filter out any bounds in this chain that don't fulfil the given filter function
	 */
	def filter (filterFcn: Bounds => Boolean): Option[Bounds] = {
		// @return (filtered bounds, whether any changes have been made)
		def internal (bounds: Bounds): (Option[Bounds], Boolean) = {
			val filteredNextWithChanged: Option[(Option[Bounds], Boolean)] =
				bounds.next.map(internal(_))
			val isNextChanged = filteredNextWithChanged.map(_._2).getOrElse(false)
			val filteredNext = filteredNextWithChanged.map(_._1).getOrElse(None)

			if (filterFcn(bounds)) {
				if (isNextChanged) {
					// Something lower-down changed
					(Some(new Bounds(bounds.level, bounds.indexBounds, filteredNext)),
					 true)
				} else {
					// No changes so far
					(Some(bounds), false)
				}
			} else {
				(filteredNext, true)
			}
		}

		internal(this)._1
	}


	/**
	 * Take any bounds in our 'next' that can be combined with us, and combine them
	 *
	 * @return a reduced version of this, or None if no reduction is required.
	 */
	def reduce: Option[Bounds] = {
		// Check if we can be combined with any of our sub-bounds
		var cur = next
		var subBoundIndex = 0
		while (cur.isDefined) {
			val combo = unionTop(cur.get)
			if (combo.isDefined) {
				// Checking the rest
				// Yes we can - combine, and reduce the remainder
				val notCur = (b: Bounds) => (b != cur.get)
				val filteredRest = next.map(_.filter(notCur)).getOrElse(None)
				val fullCombo = new Bounds(combo.get.level, combo.get.indexBounds,
				                           filteredRest)
				return Some(fullCombo.reduce.getOrElse(fullCombo))
			}
			cur = cur.get.next
			subBoundIndex = subBoundIndex+1
		}

		// No we can't - try to combine our subbounds together then
		if (next.isDefined) {
			val newNext = next.get.reduce
			if (newNext.isDefined) {
				return Some(new Bounds(level, indexBounds, newNext))
			}
		}

		None
	}

	/**
	 * Returns the number of bounds inn this chain
	 */
	def length: Int =
		1 + next.map(_.length).getOrElse(0)

	/**
	 * Make a copy of this bounds chain
	 */
	def copy: Bounds =
		new Bounds(level, indexBounds, next.map(_.copy))

	/**
	 * Make a new bounds chain that appends the given suffix to this bounds chain
	 */
	def append (that: Option[Bounds]): Bounds =
		if (that.isEmpty) {
			this
		} else {
			new Bounds(level, indexBounds, next.map(_.append(that)).orElse(that))
		}



	// //////////////////////////////////////////////////////////////////////////
	// Section: Iteratable implementation (even though we aren't one)
	//
	def iterator: Iterator[Bounds] = new BoundsIterator(this)



	// //////////////////////////////////////////////////////////////////////////
	// Section: Any overrides
	//
	override def toString: String = {
		"level "+level+": "+indexBounds+next.map(b => " + "+b.toString).getOrElse("")
	}
	override def hashCode: Int = {
		def getHash (something: Any): Int =
			if (null == something) 0 else something.hashCode

		(level +
			 5 * (getHash(indexBounds) +
				      7 * getHash(next)))
	}
	override def equals (other: Any) = other match {
		case that: Bounds =>
			(this.level == that.level &&
				 this.indexBounds == that.indexBounds &&
				 this.next.isDefined == that.next.isDefined &&
				 (this.next.isEmpty || this.next.get == that.next.get))
		case _ => false
	}
}


private[binning]
class BoundsIterator (private var bounds: Bounds) extends Iterator[Bounds] {
	def hasNext: Boolean = (null != bounds)
	def next(): Bounds = {
		val res = bounds
		bounds = bounds.next.getOrElse(null)
		res
	}
}
