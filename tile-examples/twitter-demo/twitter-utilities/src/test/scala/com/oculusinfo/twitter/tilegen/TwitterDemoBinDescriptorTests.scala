/**
 * Copyright (c) 2013 Oculus Info Inc.
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
 
package com.oculusinfo.twitter.tilegen



import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import scala.collection.JavaConverters._

import org.scalatest.FunSuite

import com.oculusinfo.binning.util.Pair

import com.oculusinfo.twitter.binning.TwitterDemoRecord



class TwitterDemoTestSuite extends FunSuite {
  def jIntList (entries: Int*): JavaList[JavaInt] =
    entries.toList.map(new JavaInt(_)).asJava

  def tweetList(pairs: (String, Long)*): JavaList[Pair[String, JavaLong]] =
    pairs.map(p => new Pair[String, JavaLong](p._1, new JavaLong(p._2))).toList.asJava

  test("Aggregation") {
    val bd = new TwitterDemoBinDescriptor

    val record1 = Map(
      "abc" -> new TwitterDemoRecord("abc", 6, jIntList(2, 4),
                                     1, jIntList(0, 1), 2, jIntList(1, 1), 3, jIntList(1, 2),
                                     tweetList(("Recent tweet 12 of abc", 12),
                                               ("Recent tweet 10 of abc", 10))),
      "def" -> new TwitterDemoRecord("def", 9, jIntList(4, 5),
                                     2, jIntList(1, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 12 of def", 12),
                                               ("Recent tweet 10 of def", 10))))
    val record2 = Map(
      "abc" -> new TwitterDemoRecord("abc", 18, jIntList(8, 10),
                                     5, jIntList(2, 3), 6, jIntList(3, 3), 7, jIntList(3, 4),
                                     tweetList(("Recent tweet 11 of abc", 11),
                                               ("Recent tweet  9 of abc",  9))),
      "ghi" -> new TwitterDemoRecord("ghi", 9, jIntList(4, 5),
                                     2, jIntList(1, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 11 of ghi", 11),
                                               ("Recent tweet  9 of ghi",  9))))

    val expected = Map(
      "abc" -> new TwitterDemoRecord("abc", 24, jIntList(10, 14),
                                     6, jIntList(2, 4), 8, jIntList(4, 4), 10, jIntList(4, 6),
                                     tweetList(("Recent tweet 12 of abc", 12),
                                               ("Recent tweet 11 of abc", 11),
                                               ("Recent tweet 10 of abc", 10),
                                               ("Recent tweet  9 of abc",  9))),
      "def" -> new TwitterDemoRecord("def", 9, jIntList(4, 5),
                                     2, jIntList(1, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 12 of def", 12),
                                               ("Recent tweet 10 of def", 10))),
      "ghi" -> new TwitterDemoRecord("ghi", 9, jIntList(4, 5),
                                     2, jIntList(1, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 11 of ghi", 11),
                                               ("Recent tweet  9 of ghi",  9))))

    assert(expected === bd.aggregateBins(record1, record2))
  }

  test("Convert") {
    val bd = new TwitterDemoBinDescriptor

    val record1 = Map(
      "abc" -> new TwitterDemoRecord("abc", 6, jIntList(2, 4),
                                     1, jIntList(0, 1), 2, jIntList(1, 1), 3, jIntList(1, 2),
                                     tweetList(("Recent tweet 12 of abc", 12),
                                               ("Recent tweet 10 of abc", 10))),
      "def" -> new TwitterDemoRecord("def", 9, jIntList(4, 5),
                                     2, jIntList(1, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 12 of def", 12),
                                               ("Recent tweet 10 of def", 10))))

    val expected = List(
      new TwitterDemoRecord("def", 9, jIntList(4, 5),
                            2, jIntList(1, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                            tweetList(("Recent tweet 12 of def", 12),
                                      ("Recent tweet 10 of def", 10))),
      new TwitterDemoRecord("abc", 6, jIntList(2, 4),
                            1, jIntList(0, 1), 2, jIntList(1, 1), 3, jIntList(1, 2),
                            tweetList(("Recent tweet 12 of abc", 12),
                                      ("Recent tweet 10 of abc", 10))))

    assert(expected === bd.convert(record1).asScala)
  }

  test("Convert many tags") {
    val bd = new TwitterDemoBinDescriptor

    val record = Map(
      "ab" -> new TwitterDemoRecord("ab", 10, jIntList(10), 10, jIntList(10),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "cd" -> new TwitterDemoRecord("cd", 12, jIntList(12), 12, jIntList(12),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "ef" -> new TwitterDemoRecord("ef",  8, jIntList( 8),  8, jIntList( 8),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "gh" -> new TwitterDemoRecord("gh", 14, jIntList(14), 14, jIntList(14),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "ij" -> new TwitterDemoRecord("ij",  6, jIntList( 6),  6, jIntList( 6),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "kl" -> new TwitterDemoRecord("kl", 16, jIntList(16), 16, jIntList(16),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "mn" -> new TwitterDemoRecord("mn",  4, jIntList( 4),  4, jIntList( 4),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "op" -> new TwitterDemoRecord("op", 18, jIntList(18), 18, jIntList(18),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "wr" -> new TwitterDemoRecord("qr",  2, jIntList( 2),  2, jIntList( 2),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "st" -> new TwitterDemoRecord("st",  3, jIntList( 3),  3, jIntList( 3),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "uv" -> new TwitterDemoRecord("uv",  5, jIntList( 5),  5, jIntList( 5),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "wx" -> new TwitterDemoRecord("wx", 20, jIntList(20), 20, jIntList(20),
                                    0, jIntList(0), 0, jIntList(0), tweetList()),
      "yz" -> new TwitterDemoRecord("yz",  1, jIntList( 1),  1, jIntList( 1),
                                    0, jIntList(0), 0, jIntList(0), tweetList()))

    val expected = List(
      new TwitterDemoRecord("wx", 20, jIntList(20), 20, jIntList(20),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("op", 18, jIntList(18), 18, jIntList(18),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("kl", 16, jIntList(16), 16, jIntList(16),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("gh", 14, jIntList(14), 14, jIntList(14),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("cd", 12, jIntList(12), 12, jIntList(12),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("ab", 10, jIntList(10), 10, jIntList(10),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("ef",  8, jIntList( 8),  8, jIntList( 8),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("ij",  6, jIntList( 6),  6, jIntList( 6),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("uv",  5, jIntList( 5),  5, jIntList( 5),
                            0, jIntList(0), 0, jIntList(0), tweetList()),
      new TwitterDemoRecord("mn",  4, jIntList( 4),  4, jIntList( 4),
                            0, jIntList(0), 0, jIntList(0), tweetList())
    )

    assert(expected === bd.convert(record).asScala)
  }

  test("Convert type") {
    val bd = new TwitterDemoBinDescriptor

    val record = Map(
      "abc" -> new TwitterDemoRecord("abc", 6, jIntList(2, 4),
                                     1, jIntList(0, 1), 2, jIntList(1, 1), 3, jIntList(1, 2),
                                     tweetList(("Recent tweet 12 of abc", 12),
                                               ("Recent tweet 10 of abc", 10))))
    val converted = bd.convert(record)
    assert(converted.isInstanceOf[JavaList[_]])
  }

  test("Maximum") {
    val bd = new TwitterDemoBinDescriptor

    val record1 = Map(
      "abc" -> new TwitterDemoRecord("abc", 17, jIntList(9, 8),
                                     7, jIntList(4, 3), 2, jIntList(2, 0), 8, jIntList(3, 5),
                                     tweetList(("Recent tweet 12 of abc", 12),
                                               ("Recent tweet 10 of abc", 10))),
      "def" -> new TwitterDemoRecord("def", 13, jIntList(4, 9),
                                     6, jIntList(1, 5), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 12 of def", 12),
                                               ("Recent tweet 10 of def", 10))))
    val record2 = Map(
      "abc" -> new TwitterDemoRecord("abc", 18, jIntList(8, 10),
                                     5, jIntList(1, 4), 6, jIntList(1, 5), 7, jIntList(6, 1),
                                     tweetList(("Recent tweet 11 of abc", 11),
                                               ("Recent tweet  9 of abc",  9))),
      "ghi" -> new TwitterDemoRecord("ghi", 14, jIntList(9, 5),
                                     6, jIntList(5, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 11 of ghi", 11),
                                               ("Recent tweet  9 of ghi",  9))))

    val expected = List(
      new TwitterDemoRecord(null, 18, jIntList(9, 10),
                            7, jIntList(5, 5), 6, jIntList(2, 5), 8, jIntList(6, 5),
                            tweetList()))



    assert(expected === bd.max(bd.convert(record1), bd.convert(record2)).asScala.toList)
  }

  test("Minimum") {
    val bd = new TwitterDemoBinDescriptor

    val record1 = Map(
      "abc" -> new TwitterDemoRecord("abc", 17, jIntList(9, 8),
                                     7, jIntList(4, 3), 2, jIntList(2, 0), 8, jIntList(3, 5),
                                     tweetList(("Recent tweet 12 of abc", 12),
                                               ("Recent tweet 10 of abc", 10))),
      "def" -> new TwitterDemoRecord("def", 13, jIntList(4, 9),
                                     6, jIntList(1, 5), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 12 of def", 12),
                                               ("Recent tweet 10 of def", 10))))
    val record2 = Map(
      "abc" -> new TwitterDemoRecord("abc", 18, jIntList(8, 10),
                                     5, jIntList(1, 4), 6, jIntList(1, 5), 7, jIntList(6, 1),
                                     tweetList(("Recent tweet 11 of abc", 11),
                                               ("Recent tweet  9 of abc",  9))),
      "ghi" -> new TwitterDemoRecord("ghi", 14, jIntList(9, 5),
                                     6, jIntList(5, 1), 3, jIntList(1, 2), 4, jIntList(2, 2),
                                     tweetList(("Recent tweet 11 of ghi", 11),
                                               ("Recent tweet  9 of ghi",  9))))

    val expected = List(
      new TwitterDemoRecord(null, 13, jIntList(4, 5),
                            5, jIntList(1, 1), 2, jIntList(1, 0), 4, jIntList(2, 1),
                            tweetList()))



    assert(expected === bd.min(bd.convert(record1), bd.convert(record2)).asScala.toList)
  }
}
