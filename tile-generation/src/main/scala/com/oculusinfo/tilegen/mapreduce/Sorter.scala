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

package com.oculusinfo.tilegen.mapreduce



import java.lang.{Iterable => JavaIterable}

import scala.collection.JavaConverters._

import org.apache.hadoop.conf.Configured
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{Text, LongWritable}
import org.apache.hadoop.mapreduce.{Job, Mapper, Reducer}
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat, TextInputFormat}
import org.apache.hadoop.mapreduce.lib.output.{FileOutputFormat, TextOutputFormat}
import org.apache.hadoop.util.{Tool, ToolRunner}

import com.oculusinfo.binning.PyramidComparator
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid


class PyramidSortKey extends LongWritable {
	override def hashCode =
		((get() >> 33) & 0x7fffffffl).toInt
}

class PyramidSortMapper extends Mapper[LongWritable, Text, PyramidSortKey, Text] {
	val pyramid = new AOITilePyramid(-2.0, -2.0, 2.0, 2.0)
	val comparator = new PyramidComparator(pyramid)
	val outputKey: PyramidSortKey = new PyramidSortKey()
	val outputValue: Text = new Text()

	override def map (key: LongWritable, value: Text,
	                  context: Mapper[LongWritable, Text,
	                                  PyramidSortKey, Text]#Context): Unit = {
		val record = value.toString()
		val fields = record.split('\t')
		val (x, y) = (fields(0).toDouble, fields(1).toDouble)
		outputKey.set(comparator.getComparisonKey(x, y))
		outputValue.set(value)
		context.write(outputKey, outputValue)
	}
}

class PyramidSortReducer extends Reducer[PyramidSortKey, Text, Text, Text] {
	val keyText: Text = new Text()
	override def reduce (key: PyramidSortKey, values: JavaIterable[Text],
	                     context: Reducer[PyramidSortKey, Text, Text, Text]#Context): Unit = {
		keyText.set(""+key.get())
		values.asScala.foreach(text =>
			context.write(text, keyText)
		)
	}
}



class PyramidSorter extends Configured with Tool {
	override def run (args: Array[String]): Int = {
		val job = Job.getInstance(getConf(), "Pyramid Sorter")

		job.setJarByClass(this.getClass)
		job.setMapOutputKeyClass(classOf[PyramidSortKey])
		job.setMapOutputValueClass(classOf[Text])
		job.setMapperClass(classOf[PyramidSortMapper])
		job.setReducerClass(classOf[PyramidSortReducer])
		job.setOutputKeyClass(classOf[Text])
		job.setOutputValueClass(classOf[Text])
		FileInputFormat.addInputPath(job, new Path(args(0)))
		FileOutputFormat.setOutputPath(job, new Path(args(1)))

		if (job.waitForCompletion(true))
			return 0
		else
			return 1
	}
}
object PyramidSorter {
	def main (args: Array[String]): Unit = {
		System.exit(ToolRunner.run(new PyramidSorter(), args))
	}
}
