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



import scala.reflect.ClassTag

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.PyramidComparator
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid

import com.oculusinfo.tilegen.util.ArgumentParser



object TileSortingTest {
	def main (args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)


		val connector = argParser.getSparkConnector()
		val sc = connector.createContext(Some("Test Pyramid Sort"))

		val source = argParser.getString("s", "source location")
		val destination = argParser.getString("d", "destination location")


		val pyramid = new AOITilePyramid(-2.0, -2.0, 2.0, 2.0)
		val coordFcn: String => (Double, Double) = record => {
			val fields = record.split('\t')
			(fields(0).toDouble, fields(1).toDouble)
		}
		val sorter = new TileSorter

		val data = sc.textFile(source)
		val partitions = argParser.getInt("p", "number of partitions",
		                                  Some(data.partitions.length))
		val sortedData = sorter.sortDatasetByTile(data, pyramid, coordFcn)

		println("Coalescing "+sortedData.partitions.length+
			        " of sorted data into "+partitions+
			        " partitions for writing")
		sortedData.coalesce(partitions).saveAsTextFile(destination)
	}
}

class TileSorter {
	def sortDatasetByTile[T: ClassTag] (data: RDD[T],
	                                    pyramid: TilePyramid,
	                                    coordFcn: T => (Double, Double)):
			RDD[T] = {
		val comparator = new PyramidComparator(pyramid)
		data.map(r =>
			{
				val coords = coordFcn(r)
				(comparator.getComparisonKey(coords._1, coords._2), r)
			}
		).sortByKey().map(_._2)
	}
}
