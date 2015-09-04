package com.oculusinfo.tilegen.datasets



import java.util.{List => JavaList}

import scala.collection.JavaConverters._

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

	private def dataString[T] (tile: TileData[T]): String = {
		val yStrings: Seq[String] =
			for (y <- 0 until tile.getDefinition.getYBins) yield {
				val xStrings: Seq[String] =
					for (x <- 0 until tile.getDefinition.getXBins) yield {
						tile.getBin(x, y).toString
					}
				xStrings.mkString("[", ", ", "]")
			}
		yStrings.mkString("\n")
	}

	protected def assertTileContents[T] (expected: TileData[T], actual: TileData[_]): Unit = {
		assert(expected.getDefinition === actual.getDefinition)
		assert(expected.getDefaultValue === actual.getDefaultValue)
		// Check contents
		val index = expected.getDefinition
		val xBins = index.getXBins
		val yBins = index.getYBins
		val inequalityMessage = index+" expected:\n"+dataString(expected)+"\n\nactual:\n"+dataString(actual)+"\n"
		for (x <- 0 until xBins; y <- 0 until yBins) {
			val expectedBin = expected.getBin(x, y)
			val actualBin = actual.getBin(x, y)
			assert(expectedBin === actualBin, inequalityMessage)
		}
		// Check metadata
		if (null == expected.getMetaDataProperties)
			assert(null == actual.getMetaDataProperties)
		else {
			val expectedProperties = expected.getMetaDataProperties.asScala.toSet
			val actualProperties = actual.getMetaDataProperties.asScala.toSet
			assert((expectedProperties -- actualProperties).isEmpty)
			assert((actualProperties -- expectedProperties).isEmpty)
			expectedProperties.map(prop =>
				assert(expected.getMetaData(prop) === actual.getMetaData(prop))
			)
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
