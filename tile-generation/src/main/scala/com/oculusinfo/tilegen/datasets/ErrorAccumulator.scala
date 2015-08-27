
/**
 * Copyright © 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.datasets

import org.apache.spark.{AccumulableParam, Accumulable}
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex


/**
 * An Accumulator used for recording the errors associated with lines rejected by CSVReader
 * Instantiated and used here: com.oculusinfo.tilegen.pipeline.PipelineOperations$#loadCsvDataWithErrorsOp
 *
 * ErrorCollectorAccumulable
 *
 * For more info on these accumulators see:
 * https://spark.apache.org/docs/1.0.0/api/scala/index.html#org.apache.spark.Accumulable
 * https://spark.apache.org/docs/1.0.0/api/scala/index.html#org.apache.spark.AccumulableParam
 *
 * Created by llay on 28/7/2015.
 */
object ErrorAccumulator {

	/**
	 * Accumulator for bad data characterization
	 *
	 * To add a new custom collector to this accumulator:
	 *	- create concrete class that extends CustomCollector
	 *	- when instantiating a StatCollectorAccumulable ensure an instance of your class is passed into the initialValue parameter
	 *
	 * See https://spark.apache.org/docs/1.0.0/api/scala/index.html#org.apache.spark.Accumulable (/AccumulableParam) for more info
	 */
	class ErrorCollectorAccumulable(val initialValue: ListBuffer[CustomCollector]) extends Accumulable[ListBuffer[CustomCollector], (String, Throwable)](initialValue, new ErrorCollectorAccumulableParam) {
	}

	/**
	 * A helper for ErrorCollectorAccumulable.
	 */
	class ErrorCollectorAccumulableParam extends AccumulableParam[ListBuffer[CustomCollector], (String, Throwable)]() {
		// Add additional data to the accumulator value.
		override def addAccumulator(r: ListBuffer[CustomCollector], t: (String, Throwable)): ListBuffer[CustomCollector] = {
			r.foreach(s => s.addRow(t))
			r
		}

		// Merge two accumulated values together.
		override def addInPlace(r1: ListBuffer[CustomCollector], r2: ListBuffer[CustomCollector]): ListBuffer[CustomCollector] = {
			for (i <- 0 until r1.length) {
				r1(i).merge(r2(i))
			}
			r1
		}

		// Return the "zero" (identity) value for an accumulator type, given its initial value.
		override def zero(initialValue: ListBuffer[CustomCollector]): ListBuffer[CustomCollector] = {
			initialValue
		}
	}

	/**
	 * The base class for custom error info generation.
	 * Accumulated by ErrorCollectorAccumulable
	 */
	abstract class CustomCollector extends Serializable {
		def addRow(r: (String, Throwable)): Unit

		// Add a row of the rdd into this stat collector
		def merge(accum: CustomCollector): Unit

		// merge two SummaryErrors of the same type together (for parallel processing)
		def getError: collection.mutable.Map[String, Int]
	}

	/**
	 * Custom stat collector: Aggregates the errors and provides a count for each one
	 */
	class ErrorCollector extends CustomCollector {
		val errors = collection.mutable.Map[String, Int]().withDefaultValue(0)

		override def addRow(r: (String, Throwable)): Unit = {
			// strip source line from number exception
			val prefix = """^java.lang.NumberFormatException: For input string: (.*)""".r
			try {
				val prefix(suffix) = r._2.toString
				errors("java.lang.NumberFormatException") += 1
			} catch {
				case e: scala.MatchError =>
					errors(r._2.toString) += 1
			}
		}

		override def getError = {
			errors
		}

		override def merge(accum: CustomCollector) = {
			// Since scala won't let you pass in a subclass, we have to do this ugly casting
			// Would much prefer "accum: NumRecordsError"
			val a: ErrorCollector = accum.asInstanceOf[ErrorCollector]
			a.errors.foreach { case (error, count) =>
				errors(error) += count
			}
		}
	}
}
