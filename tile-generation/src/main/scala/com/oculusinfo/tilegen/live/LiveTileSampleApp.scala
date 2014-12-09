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

package com.oculusinfo.tilegen.live



import org.apache.spark._
import org.apache.spark.SparkContext._

import java.awt.image.BufferedImage
import java.io.File
import java.lang.{Double => JavaDouble}
import javax.imageio.ImageIO

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid

import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.tiling.analytics.NumericSumBinningAnalytic



object LiveTileSampleApp {
	def main (args: Array[String]): Unit = {
		// get and cache our data
		val argParser = new ArgumentParser(args)

		try {
			val sc = argParser.getSparkConnector().createContext(Some("tile generator"))
			val dataFile = argParser.getString("s", "The source data file to read")
			val data = sc.textFile(dataFile).map(s =>
				{
					val fields = s.split('\t')
					(fields(0).toDouble, fields(1).toDouble, fields(2).toDouble)
				}
			).cache
			val imageDir = argParser.getString("d", "The destination directory into which to put images")
			val pyramid = new AOITilePyramid(-1.0, -1.0, 1.0, 1.0)
			val analytic = new NumericSumBinningAnalytic[Double, JavaDouble]()
			val generator = new LiveTileGenerator[Double, JavaDouble](data, pyramid, analytic)

			var done = false
			do {
				println("Enter tile level, x, and y, all comma-separated.")
				println("Enter a blank line to end")
				val userLine = readLine()
				if (userLine.trim().isEmpty) {
					done = true
				} else {
					val userVals = userLine.split(',')
					val level = userVals(0).trim.toInt
					val x = userVals(1).trim.toInt
					val y = userVals(2).trim.toInt

					val startTime = System.currentTimeMillis
					val tile = generator.getTile(level, x, y)
					val endTime = System.currentTimeMillis
					println("Generated tile ("+level+", "+x+", "+y+") in "+
						        ((endTime-startTime)/1000.0)+" seconds")
					val image = tileToImage(tile)
					ImageIO.write(image, "png",
					              new File("%s/tile-%d-%d-%d.png".format(imageDir, level, x, y)))
				}
			} while (!done)
				} catch {
			case e: Exception => {
				println("Error in real-time tile generation")
				println("\t"+e.getMessage)
				argParser.usage
			}
		}
	}

	def scale (min: Double, x: JavaDouble, max: Double) = {
		if (x.isNaN) 0
		else (((x.doubleValue-min)/(max-min))*255.999).toInt
	}

	def binColorFcn (value: Option[JavaDouble],
	                 min: Double, max: Double): Int = {
		val result = scale(min, value.getOrElse(new JavaDouble(Double.NaN)), max)
		new java.awt.Color(result, result, result).getRGB()
	}

	def tileToImage (tile: TileData[JavaDouble]): BufferedImage = {
		val index = tile.getDefinition()
		val width = index.getXBins()
		val height = index.getYBins()

		val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

		var min = Double.MaxValue
		var max = Double.MinValue
		Range(0, width).foreach(x =>
			Range(0, height).foreach(y => {
				                         min = min.min(tile.getBin(x, y))
				                         max = max.max(tile.getBin(x, y))
			                         })
		)
		Range(0, width).foreach(x =>
			Range(0, height).foreach(y =>
				{
					val value = tile.getBin(x, y)
					image.setRGB(x, y, binColorFcn(Some(value), min, max))
				}
			)
		)

		image
	}
}
