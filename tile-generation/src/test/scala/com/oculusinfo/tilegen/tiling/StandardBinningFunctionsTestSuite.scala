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
			val bins = StandardBinningFunctions.linearUniversalBins(start, end).map(TileIndex.universalBinIndexToTileBinIndex(sample, _))
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
}
