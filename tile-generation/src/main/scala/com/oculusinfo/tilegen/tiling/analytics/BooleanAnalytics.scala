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



import java.lang.{Boolean => JavaBoolean}
import java.lang.{Long => JavaLong}
import java.util.{List => JavaList}

import org.json.{JSONArray, JSONObject}

import scala.collection.JavaConverters._
import scala.collection.immutable.BitSet



//
// In all these boolean classes, we use an Option[Boolean] instead of a plain boolean
// so that we can differentiate between no data and false.
//

/**
 * A simple analytic to perform boolean operations
 *
 * @param add The boolean operation to perform when aggregating.
 */
class BooleanAnalytic (add: (Boolean, Boolean) => Boolean) extends Analytic[Option[Boolean]] {
	def aggregate (a: Option[Boolean], b: Option[Boolean]) =
		if (a.isDefined && b.isDefined) Some(add(a.get, b.get))
		else if (a.isDefined) a
		else if (b.isDefined) b
		else None
	def defaultProcessedValue: Option[Boolean] = None
	def defaultUnprocessedValue: Option[Boolean] = None
}

/**
 * A binning version of {@see BooleanAnalytic}.
 *
 * Like BooleanAnalytic, it takes the operation to perform as an arguement
 *
 * @param add The boolean operation to perform when aggregating.
 * @param defaultValue The value to use in bins where no value was found
 */
class BooleanBinningAnalytic (add: (Boolean, Boolean) => Boolean,
                              defaultValue: Boolean = false)
		extends BooleanAnalytic(add)
		with BinningAnalytic[Option[Boolean], JavaBoolean]
{
	def finish (v: Option[Boolean]): JavaBoolean =
		Boolean.box(v.getOrElse(defaultValue))
}

/**
 * A binning version of {@see BooleanAnalytic}.
 *
 * @param analyticName The name by which this analytic will be known in
 * metadata.
 */
class BooleanTileAnalytic (analyticName: String, add: (Boolean, Boolean) => Boolean)
		extends BooleanAnalytic(add)
		with TileAnalytic[Option[Boolean]]
{
	def name = analyticName
}




/**
 * An analytic to take advantage of Scala's BitSet class - essentially an
 * efficient set of booleans
 *
 * @param add The boolean operation to perform on individual values in the
 * sets when aggregating them
 */
class BitSetAnalytic (add: (Boolean, Boolean) => Boolean) extends Analytic[BitSet] {
	def aggregate (a: BitSet, b: BitSet): BitSet =
		(a | b).flatMap(n => if (add(a(n), b(n))) BitSet(n) else BitSet())
	def defaultProcessedValue: BitSet = BitSet()
	def defaultUnprocessedValue: BitSet = BitSet()
}

/**
 * {@see BitSetAnalytic}; a binning analytic based on that
 *
 * Unlike {@see BooleanBinningAnalytic}, this class doesn't need a default
 * value - there's no way to use one.  The default is always, essentially,
 * false.
 *
 * Values are written to tiles in the same efficient manner they are kept
 * in the BitSet - as an array of Long values, 64 per entry (so a bitset of 320
 * boolean values stores as an array of 5 Longs).
 *
 * @param add The boolean operation to perform on individual values in the
 * sets when aggregating them
 */
class BitSetBinningAnalytic (add: (Boolean, Boolean) => Boolean)
		extends BitSetAnalytic(add)
		with BinningAnalytic[BitSet, JavaList[JavaLong]]
{
	def finish (value: BitSet): JavaList[JavaLong] =
		value.toBitMask.toSeq.map(Long.box(_)).asJava
}

/**
 * A Tile Analytic version of {@link BitSetAnalytic}.
 *
 * When writing to metadata, values are written not as booleans, but as an
 * array of the positions that are true.
 */
class BitSetTileAnalytic (analyticName: String, add: (Boolean, Boolean) => Boolean)
		extends BitSetAnalytic(add)
		with TileAnalytic[BitSet]
{
	def name = analyticName
	override def storableValue (value: BitSet, location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val result = new JSONObject()
		val subRes = new JSONArray()
		value.foreach(v => subRes.put(v))
		result.put(name, subRes)
		Some(result)
	}
}
