package com.oculusinfo.tilegen.datasets



import java.util.{List => JavaList}

import org.scalatest.Assertions

import com.oculusinfo.binning.TileData



/**
 * Simple common tile test used by several tests.
 */
trait TileAssertions extends Assertions {
	/**
	 * Test that a given tile has the expected bin values
	 * @param expected The expected bin values.  The size of this list must match the size of the tile
	 * @param tile The tile to test
	 * @tparam T The type of bin value
	 */
	protected def assertTileContents[T] (expected: List[T], tile: TileData[_])  : Unit = {
		val index = tile.getDefinition
		val xBins = index.getXBins
		val yBins = index.getYBins
		for (x <- 0 until xBins; y <- 0 until yBins) {
			val i = x+xBins*y
			assert(expected(i) === tile.getBin(x, y))
		}
	}

  protected def assertListTileContents[T] (expected: List[List[T]], tile: TileData[_]): Unit = {
    val index = tile.getDefinition
    val xBins = index.getXBins
    val yBins = index.getYBins
    for (x <- 0 until xBins; y <- 0 until yBins) {
      val i = x+xBins*y
      val bin = tile.getBin(x, y).asInstanceOf[JavaList[_]]
      val expectedBin = expected(i)
      assert(expectedBin.size === bin.size)
      for (n <- 0 until bin.size) {
        assert(expectedBin(n) === bin.get(n))
      }
    }
  }
}
