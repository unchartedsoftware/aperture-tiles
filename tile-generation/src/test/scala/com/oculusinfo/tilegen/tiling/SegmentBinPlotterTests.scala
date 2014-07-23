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


import org.apache.spark.SparkContext
import org.apache.spark.SharedSparkContext

import org.scalatest.FunSuite

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.AOITilePyramid



class SegmentBinPlotterTestSuite extends FunSuite with SharedSparkContext {
	val expectedValues: Map[TileIndex, String] =
		Map(
			(new TileIndex(0, 0, 0),
			 "+--------------------------------+\n"+
				 "|                                |\n"+
				 "|.                             o |\n"+
				 "| .                           o  |\n"+
				 "|  .                         o   |\n"+
				 "|   .                       o    |\n"+
				 "|    .                     o     |\n"+
				 "|     .                   o      |\n"+
				 "|      .                 o       |\n"+
				 "|       ooooooooooooooooO        |\n"+
				 "|       o               o        |\n"+
				 "|       o         .     o        |\n"+
				 "|       o         o     o        |\n"+
				 "|       o         o .   o        |\n"+
				 "|       o    .    oo    o        |\n"+
				 "|       o     .   O     o        |\n"+
				 "|       o      . o      o        |\n"+
				 "|       o       O       o        |\n"+
				 "|       o      o .      o        |\n"+
				 "|       o     o   .     o        |\n"+
				 "|       o    o     .    o        |\n"+
				 "|       o               o        |\n"+
				 "|       o               o        |\n"+
				 "|       o  ooooooooooo  o        |\n"+
				 "|       o               o        |\n"+
				 "|       ooooooooooooooooo        |\n"+
				 "|      o                 .       |\n"+
				 "|     o                   .      |\n"+
				 "|    o                     .     |\n"+
				 "|   o                       .    |\n"+
				 "|  o                         .   |\n"+
				 "| o                           .  |\n"+
				 "|o                             . |\n"+
				 "+--------------------------------+\n"),
			(new TileIndex(1, 0, 0),
			 "+--------------------------------+\n"+
				 "|               o              .O|\n"+
				 "|               o              o.|\n"+
				 "|               o             o  |\n"+
				 "|               o            o   |\n"+
				 "|               o           o    |\n"+
				 "|               o          o     |\n"+
				 "|               o         o      |\n"+
				 "|               o        o       |\n"+
				 "|               o       .        |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o   .oooooooooooo|\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               Ooooooooooooooooo|\n"+
				 "|              o                 |\n"+
				 "|             o                  |\n"+
				 "|            o                   |\n"+
				 "|           o                    |\n"+
				 "|          o                     |\n"+
				 "|         o                      |\n"+
				 "|        o                       |\n"+
				 "|       o                        |\n"+
				 "|      o                         |\n"+
				 "|     o                          |\n"+
				 "|    o                           |\n"+
				 "|   o                            |\n"+
				 "|  o                             |\n"+
				 "| o                              |\n"+
				 "|o                               |\n"+
				 "+--------------------------------+\n"),
			(new TileIndex(1, 0, 1),
			 "+--------------------------------+\n"+
				 "|                                |\n"+
				 "|.                               |\n"+
				 "|..                              |\n"+
				 "| ..                             |\n"+
				 "|  ..                            |\n"+
				 "|   ..                           |\n"+
				 "|    ..                          |\n"+
				 "|     ..                         |\n"+
				 "|      ..                        |\n"+
				 "|       ..                       |\n"+
				 "|        ..                      |\n"+
				 "|         ..                     |\n"+
				 "|          ..                    |\n"+
				 "|           ..                   |\n"+
				 "|            ..                  |\n"+
				 "|             ..                 |\n"+
				 "|              ..                |\n"+
				 "|               Ooooooooooooooooo|\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o                |\n"+
				 "|               o       ..       |\n"+
				 "|               o        ..      |\n"+
				 "|               o         ..     |\n"+
				 "|               o          ..    |\n"+
				 "|               o           ..   |\n"+
				 "|               o            ..  |\n"+
				 "|               o             .. |\n"+
				 "+--------------------------------+\n"),
			(new TileIndex(1, 1, 0),
			 "+--------------------------------+\n"+
				 "|              o                 |\n"+
				 "|.             o                 |\n"+
				 "|..            o                 |\n"+
				 "| ..           o                 |\n"+
				 "|  ..          o                 |\n"+
				 "|   ..         o                 |\n"+
				 "|    ..        o                 |\n"+
				 "|     ..       o                 |\n"+
				 "|      .       o                 |\n"+
				 "|              o                 |\n"+
				 "|              o                 |\n"+
				 "|              o                 |\n"+
				 "|ooooooooooo   o                 |\n"+
				 "|              o                 |\n"+
				 "|              o                 |\n"+
				 "|              o                 |\n"+
				 "|ooooooooooooooO.                |\n"+
				 "|               ..               |\n"+
				 "|                ..              |\n"+
				 "|                 ..             |\n"+
				 "|                  ..            |\n"+
				 "|                   ..           |\n"+
				 "|                    ..          |\n"+
				 "|                     ..         |\n"+
				 "|                      ..        |\n"+
				 "|                       ..       |\n"+
				 "|                        ..      |\n"+
				 "|                         ..     |\n"+
				 "|                          ..    |\n"+
				 "|                           ..   |\n"+
				 "|                            ..  |\n"+
				 "|                             .. |\n"+
				 "+--------------------------------+\n"),
			(new TileIndex(1, 1, 1),
			 "+--------------------------------+\n"+
				 "|                                |\n"+
				 "|                                |\n"+
				 "|                             o  |\n"+
				 "|                            o   |\n"+
				 "|                           o    |\n"+
				 "|                          o     |\n"+
				 "|                         o      |\n"+
				 "|                        o       |\n"+
				 "|                       o        |\n"+
				 "|                      o         |\n"+
				 "|                     o          |\n"+
				 "|                    o           |\n"+
				 "|                   o            |\n"+
				 "|                  o             |\n"+
				 "|                 o              |\n"+
				 "|                o               |\n"+
				 "|               o                |\n"+
				 "|ooooooooooooooO                 |\n"+
				 "|              o                 |\n"+
				 "|              o                 |\n"+
				 "|              o                 |\n"+
				 "|   o          o                 |\n"+
				 "|   o          o                 |\n"+
				 "|   o          o                 |\n"+
				 "|   o          o                 |\n"+
				 "|   o  o       o                 |\n"+
				 "|   o o        o                 |\n"+
				 "|   oo         o                 |\n"+
				 "|   O          o                 |\n"+
				 "|  o           o                 |\n"+
				 "| o            o                 |\n"+
				 "|o             o                 |\n"+
				 "+--------------------------------+\n")
		)

	test("Line segment binner: point version") {
		val data = sc.parallelize(List(((0.0, 0.0), (1.0, 1.0))
			                               ,((1.0, 1.0), (3.0, 1.0))
			                               ,((3.0, 1.0), (4.0, 0.0))
			                               ,((0.0, 4.0), (1.0, 3.0))
			                               ,((1.0, 3.0), (3.0, 3.0))
			                               ,((3.0, 3.0), (4.0, 4.0))
			                               ,((1.0, 1.0), (1.0, 3.0))
			                               ,((3.0, 1.0), (3.0, 3.0))
			                               ,((1.5, 1.5), (2.5, 2.5))
			                               ,((1.5, 2.5), (2.5, 1.5))
			                               ,((2.25, 2.25), (2.25, 2.75))
			                               ,((1.25, 1.25), (2.75, 1.25))
		                          ))
		val pyramider = new AOITilePyramid(0.0, 0.0, 4.1, 4.1)
/*
		val segmentBinner = new LineSegmentPointBinner(pyramider);
		val tiles = segmentBinner.processData(data, List(0, 1)).collect()
		val printer = new TilePrinter
		tiles.foreach(tile =>
			{
				assert(expectedValues.contains(tile.getDefinition()))
				val actual = printer.tileToString(tile)
				val expected = expectedValues(tile.getDefinition())
				assert(actual == expected)
			}
		)
 */
	}
}

