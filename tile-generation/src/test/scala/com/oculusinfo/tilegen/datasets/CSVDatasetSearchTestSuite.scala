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
package com.oculusinfo.tilegen.datasets

import org.scalatest.FunSuite
import org.apache.spark.SharedSparkContext

class CSVDatasetSearchTestSuite extends FunSuite with SharedSparkContext {

  test("Test search specification") {
    // Create a 5-column dataset, with the ability to search on any column
    // Create our raw data
    val rawData = List(" 0,  8,  6, 10,  7",
                       " 1, 12, 11,  6, 10",
                       " 2,  3,  8,  0,  9",
                       " 3,  7,  4,  5,  5",
                       " 4, 11,  3,  3,  8",
                       " 5,  5,  2, 12,  3",
                       " 6,  9, 10,  4, 12",
                       " 7,  1,  9,  2,  6",
                       " 8, 10, 12,  9,  0",
                       " 9,  6,  1,  1,  4",
                       "10,  4,  7,  7, 11",
                       "11,  0,  0,  8,  2",
                       "12,  2,  5, 11,  1"
                       ) 
  }
}