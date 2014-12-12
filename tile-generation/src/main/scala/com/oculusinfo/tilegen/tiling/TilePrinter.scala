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



import java.lang.{Double => JavaDouble}
import scala.collection.mutable.MutableList
import org.apache.avro.file.CodecFactory
import com.oculusinfo.binning.TileData
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer



/**
 * A class that prints out a tile as ASCII graphic art, so one can easily see
 * what (very approximately) the tile contains.
 */
object TilePrinter {
	def main (args: Array[String]) = {
		val argParser = new ArgumentParser(args)

		val sc = argParser.getSparkConnector().createContext(Some("Tile Edges Test"))

		val baseLocation =
			argParser.getString("loc",
			                    "The location from which to get the data")
		val level = argParser.getInt("level",
		                             "The level of data to print")
		val tileIO = TileIO.fromArguments(argParser)

		val tiles = tileIO.readTileSet(sc, new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec()), baseLocation,
		                               List(level)).collect()

		val printer = new TilePrinter()
		tiles.foreach(t =>
			{
				println()
				println(t.getDefinition())
				printer.printTile(t)
			}
		)
	}
}
class TilePrinter {
	def printTile (tile: TileData[JavaDouble]): Unit = {
		println(tileToString(tile))
	}

	def tileToString (tile: TileData[JavaDouble]): String = {
		var maxValue = 0.0
		var minValue = 10000.0

		val allRows = new MutableList[MutableList[Double]]()

		for (MY <- 0 until 32) {
			val row = new MutableList[Double]()
			for (MX <- 0 until 32) {
				var total = 0.0
				for (my <- 0 until 8) {
					for (mx <- 0 until 8) {
						val x = MX*8+mx;
						val y = MY*8+my;
						val value = tile.getBin(x, y)
						total = total + value
					}
				}

				if (total < minValue) minValue = total
				if (total > maxValue) maxValue = total
				row += total
			}
			allRows += row
		}

		var result = "+--------------------------------+\n"
		for (row <- allRows) {
			result += "|"
			for (value <- row) {
				val scaledValue = ((value-minValue)/(maxValue-minValue)*4.0).toInt
				if (0 == scaledValue) result += " "
				else if (1 == scaledValue) result += "."
				else if (2 == scaledValue) result += "o"
				else result += "O"
			}
			result += "|\n"
		}
		result += "+--------------------------------+\n"

		result
	}
}
