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

package com.oculusinfo.tilegen.examples.datagen



import org.apache.spark._
import org.apache.spark.SparkContext._

import java.awt.geom.Point2D

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.BinIterator
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData

import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.util.ArgumentParser

/**
 * A sample data generation object that generates a bunch of lines
 * through the center of the area of interest.
 */
object LineStarSampleGenerator {
	def endPointsToLine (startX: Double, startY: Double,
	                     endX: Double,   endY: Double,
	                     stepSize: Double): IndexedSeq[(Double, Double)] = {
		// Bresenham's algorithm, as per wikipedia
		var steep = math.abs(endY-startY) > math.abs(endX-startX)

		var (x0, y0, x1, y1) =
			if (steep) {
				if (startY > endY) {
					(endY, endX, startY, startX)
				} else {
					(startY, startX, endY, endX)
				}
			} else {
				if (startX > endX) {
					(endX, endY, startX, startY)
				} else {
					(startX, startY, endX, endY)
				}
			}

		val deltax = x1-x0
		val deltay = math.abs(y1-y0)
		var error = deltax/2
		var y = y0
		val ystep = if (y0 < y1) stepSize else -stepSize

		Range(0, ((x1-x0)/stepSize).ceil.toInt).map(n => x0+n*stepSize).map(x =>
			{
				val ourY = y
				error = error - deltay
				if (error < 0) {
					y = y + ystep
					error = error + deltax
				}

				if (steep) (ourY, x)
				else (x, ourY)
			}
		)
	}

	def main (args: Array[String]): Unit = {
		// Draw a bunch of lines across the AoI all through the center
		val argParser = new ArgumentParser(args)

		

		val fileName = argParser.getString("f",
		                                   "The file to which to write the sample data")
		val topLevel = argParser.getInt(
			"top",
			"The level at which our raidal lines will first fill the area of interest.  At all "
				+"levels at this level and above, every pixel in the data set will have a count of at "
				+"least one.  At levels below this, there will be empty pixels",
			Option(0))
		val bottomLevel = argParser.getInt(
			"bottom",
			"The lowest level at which a line will display as continuous, with no breaks.",
			Option(10))

		val sc = argParser.getSparkConnector().createContext(Some("Create sample data for live tile demonstration"))

		val linesPerSide = 256 << topLevel
		val linePartitions = if (bottomLevel < 6) 1 else (1 << (bottomLevel-6))
		val pixelsPerLine = 256 << bottomLevel
		val increment = 2.0/pixelsPerLine

		val lineIndex = sc.makeRDD(Range(0, linesPerSide), linePartitions)


		val data = lineIndex.flatMap(n =>
			{
				// Get a start position from -1 to 1
				val startPos = n.toDouble/linesPerSide.toDouble * 2.0 - 1.0
				val xLine = endPointsToLine(-1.0, -startPos, 1.0, startPos, increment)
				val yLine = endPointsToLine(-startPos, -1.0, startPos, 1.0, increment)
				xLine union yLine
			}
		).map(p =>
			"%.8f\t%.8f\t1.0".format(p._1, p._2)
		)
		data.saveAsTextFile(fileName)
	}
}
