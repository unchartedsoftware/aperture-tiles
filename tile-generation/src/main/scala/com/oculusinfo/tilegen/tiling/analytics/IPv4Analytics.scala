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



import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import org.json.JSONObject


/**
 * This analytic stores the CIDR block represented by a given tile.
 */
object IPv4Analytics extends Serializable {
	import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme._

	private val EPSILON = 1E-10
	def getCIDRBlock (pyramid: TilePyramid)(tile: TileData[_]): String = {
		// Figure out the IP address of our corners
		val index = tile.getDefinition()
		val bounds = pyramid.getTileBounds(index)
		val llAddr = ipArrayToLong(reverse(bounds.getMinX(), bounds.getMinY()))
		val urAddr = ipArrayToLong(reverse(bounds.getMaxX()-EPSILON, bounds.getMaxY()-EPSILON))

		// Figure out how many significant bits they have in common
		val significant = 0xffffffffL & ~(llAddr ^ urAddr)
		// That is the number of blocks
		val block = 32 -
			(for (i <- 0 to 32) yield (i, ((1L << i) & significant) != 0))
			.find(_._2)
			.getOrElse((32, false))._1
		// And apply that to either to get the common address
		val addr = longToIPArray(llAddr & significant)
		ipArrayToString(addr)+"/"+block
	}

	def getIPAddress (pyramid: TilePyramid, max: Boolean)(tile: TileData[_]): Long = {
		val index = tile.getDefinition()
		val bounds = pyramid.getTileBounds(index)
		if (max) {
			ipArrayToLong(reverse(bounds.getMaxX()-EPSILON, bounds.getMaxY()-EPSILON))
		} else {
			ipArrayToLong(reverse(bounds.getMinX(), bounds.getMinY()))
		}
	}

	/**
	 * Get an analysis description for an analysis that stores the CIDR block
	 * of an IPv4-indexed tile, with an arbitrary tile pyramid.
	 */
	def getCIDRBlockAnalysis[BT] (pyramid: TilePyramid = getDefaultIPPyramid):
			AnalysisDescription[TileData[BT], String] =
		new TileOnlyMonolithicAnalysisDescription[TileData[BT], String](
			getCIDRBlock(pyramid),
			new StringAnalytic("CIDR Block"))


	def getMinIPAddressAnalysis[BT] (pyramid: TilePyramid = getDefaultIPPyramid):
			AnalysisDescription[TileData[BT], Long] =
		new MonolithicAnalysisDescription[TileData[BT], Long](
			getIPAddress(pyramid, false),
			new TileAnalytic[Long] {
				def name = "Minimum IP Address"
				def aggregate (a: Long, b: Long): Long = a min b
				def defaultProcessedValue: Long = 0L
				def defaultUnprocessedValue: Long = 0xffffffffL
				override def storableValue (value: Long, location: TileAnalytic.Locations.Value): Option[JSONObject] = {
					val result = new JSONObject()
					result.put(name, ipArrayToString(longToIPArray(value)))
					Some(result)
				}
			})

	def getMaxIPAddressAnalysis[BT] (pyramid: TilePyramid = getDefaultIPPyramid):
			AnalysisDescription[TileData[BT], Long] =
		new MonolithicAnalysisDescription[TileData[BT], Long](
			getIPAddress(pyramid, true),
			new TileAnalytic[Long] {
				def name = "Maximum IP Address"
				def aggregate (a: Long, b: Long): Long = a max b
				def defaultProcessedValue: Long = 0xffffffffL
				def defaultUnprocessedValue: Long = 0L
				override def storableValue (value: Long, location: TileAnalytic.Locations.Value): Option[JSONObject] = {
					val result = new JSONObject()
					result.put(name, ipArrayToString(longToIPArray(value)))
					Some(result)
				}
			})
}
