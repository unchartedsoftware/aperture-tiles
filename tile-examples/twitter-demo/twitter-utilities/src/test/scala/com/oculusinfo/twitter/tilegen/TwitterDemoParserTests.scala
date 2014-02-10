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



import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

import scala.collection.JavaConverters._

import org.scalatest.FunSuite



class TwitterDemoParserTestSuite extends FunSuite {
  private def makeCalendar (year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) = {
    import Calendar._
    val calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0:00"))
    calendar.set(year, month, day, hour, minute, second)
    calendar
  }

  private def assertDatesEqual (expected: Calendar, actual: Date): Unit = {
    import Calendar._
    val actualC = new GregorianCalendar(TimeZone.getTimeZone("GMT+0:00"))
    actualC.setTime(actual)

    assert(expected.get(YEAR)         === actualC.get(YEAR))
    assert(expected.get(MONTH)        === actualC.get(MONTH))
    assert(expected.get(DAY_OF_MONTH) === actualC.get(DAY_OF_MONTH))
    assert(expected.get(HOUR_OF_DAY)  === actualC.get(HOUR_OF_DAY))
    assert(expected.get(MINUTE)       === actualC.get(MINUTE))
    assert(expected.get(SECOND)       === actualC.get(SECOND))
  }

  test("Base line parsing") {
    val recordParser = new TwitterDemoRecordParser(Long.MinValue, Long.MaxValue, 1)



    val line1 = "Thu Jan 02 11:20:42 +0000 2014	44707096	EarlJuice4Sale	418703390403616768	\"@DedicatedDiva_: @EarlJuice4Sale :( I'm going to be so tired today.\"true get you one of them 5 hour energy drinks	#	33.4183218	-94.0153997	United States	Texarkana, AR	city	Neutral	53.1386915991"
    val parsedLine1 = recordParser.parseLine(line1)
    val expectedDate1 = makeCalendar(2014, 0, 2, 11, 20, 42)

    assertDatesEqual(expectedDate1, parsedLine1.createdAt)
    assert("44707096" === parsedLine1.userId)
    assert("EarlJuice4Sale" === parsedLine1.userName)
    assert("418703390403616768" === parsedLine1.tweetId)
    assert("\"@DedicatedDiva_: @EarlJuice4Sale :( I'm going to be so tired today.\"true get you one of them 5 hour energy drinks" === parsedLine1.text)
    assert(List[String]() === parsedLine1.tags.toList)
    assert(33.4183218 === parsedLine1.latitude)
    assert(-94.0153997 === parsedLine1.longitude)
    assert("United States" === parsedLine1.country)
    assert("Texarkana, AR" === parsedLine1.placeName)
    assert("city" === parsedLine1.placeType)
    assert(Sentiment.neutral === parsedLine1.sentiment)
    assert(53.1386915991 === parsedLine1.confidence)



    val line2 = "Thu Jan 02 11:20:51 +0000 2014	249818911	TotalTrafficHOU	418703429247451136	#SouthSide accident blocking the exit ramp on S Sam Houston Tollway WB at Wayside #traffic http://t.co/vuUN2yElDh	#SouthSide#traffic#	29.60034	-95.31786	United States	Houston, TX	city	Negative	86.6215533476"
    val parsedLine2 = recordParser.parseLine(line2)
    val expectedDate2 = makeCalendar(2014, 0, 2, 11, 20, 51)

    assertDatesEqual(expectedDate2, parsedLine2.createdAt)
    assert("249818911" === parsedLine2.userId)
    assert("TotalTrafficHOU" === parsedLine2.userName)
    assert("418703429247451136" === parsedLine2.tweetId)
    assert("#SouthSide accident blocking the exit ramp on S Sam Houston Tollway WB at Wayside #traffic http://t.co/vuUN2yElDh" === parsedLine2.text)
    assert(List[String]("SouthSide", "traffic") === parsedLine2.tags.toList)
    assert(29.60034 === parsedLine2.latitude)
    assert(-95.31786 === parsedLine2.longitude)
    assert("United States" === parsedLine2.country)
    assert("Houston, TX" === parsedLine2.placeName)
    assert("city" === parsedLine2.placeType)
    assert(Sentiment.negative === parsedLine2.sentiment)
    assert(86.6215533476 === parsedLine2.confidence)



    val line3 = "Thu Jan 02 11:20:53 +0000 2014	513811634	CarlyMalburg	418703438428381184	Well that drive was interesting. #hatedrivinginsnow	#hatedrivinginsnow#	42.3674625	-83.0843326	United States	Detroit, MI	city	Positive	89.5754982914"
    val parsedLine3 = recordParser.parseLine(line3)
    val expectedDate3 = makeCalendar(2014, 0, 2, 11, 20, 53)

    assertDatesEqual(expectedDate3, parsedLine3.createdAt)
    assert("513811634" === parsedLine3.userId)
    assert("CarlyMalburg" === parsedLine3.userName)
    assert("418703438428381184" === parsedLine3.tweetId)
    assert("Well that drive was interesting. #hatedrivinginsnow" === parsedLine3.text)
    assert(List[String]("hatedrivinginsnow") === parsedLine3.tags.toList)
    assert(42.3674625 === parsedLine3.latitude)
    assert(-83.0843326 === parsedLine3.longitude)
    assert("United States" === parsedLine3.country)
    assert("Detroit, MI" === parsedLine3.placeName)
    assert("city" === parsedLine3.placeType)
    assert(Sentiment.positive === parsedLine3.sentiment)
    assert(89.5754982914 === parsedLine3.confidence)
  }


  test("Tag parsing") {
    val startDate = makeCalendar(2014, 0, 1, 0, 0, 0)
    val endDate = makeCalendar(2014, 0, 4, 0, 0, 0)
    val recordParser = new TwitterDemoRecordParser(startDate.getTimeInMillis(),
						   endDate.getTimeInMillis(),
						   3)
    val records1 = recordParser.getRecordsByTag("Wed Jan 01 12:00:00 +0000 2014\tuid\tuname\ttid\tTweet Text\t#abc#def#ghi#\t2.0\t1.0\tCanada\tToronto\tcity\tPositive\t50.0")
    assert(1 === records1.size)
    records1.foreach(records => {
      assert(1.0 === records._1)
      assert(2.0 === records._2)
      assert(3 === records._3.size)
      assert(List("abc", "def", "ghi") === records._3.map(_._1).toList.sorted)
      assert(List("abc", "def", "ghi") === records._3.map(_._2.getTag()).toList.sorted)
      records._3.map(_._2).foreach(record => {
	assert(1 === record.getCount())
	assert(1 === record.getPositiveCount())
	assert(0 === record.getNeutralCount())
	assert(0 === record.getNegativeCount())
	assert(List(1, 0, 0) === record.getCountBins().asScala.toList)
	assert(List(1, 0, 0) === record.getPositiveCountBins().asScala.toList)
	assert(List(0, 0, 0) === record.getNeutralCountBins().asScala.toList)
	assert(List(0, 0, 0) === record.getNegativeCountBins().asScala.toList)
	assert(1 === record.getRecentTweets().size())
	assert("Tweet Text" === record.getRecentTweets().get(0).getFirst())
      })
    })


    val records2 = recordParser.getRecordsByTag("Wed Jan 02 12:00:00 +0000 2014\tuid\tuname\ttid\tTweet Text\t#abc#def#ghi#\t0.0\t0.0\tCanada\tToronto\tcity\tPositive\t50.0")
    assert(1 === records2.size)
    records2.foreach(records => {
      assert(List("abc", "def", "ghi") === records._3.map(_._1).toList.sorted)
      assert(List("abc", "def", "ghi") === records._3.map(_._2.getTag()).toList.sorted)
      records._3.map(_._2).foreach(record => {
	assert(List(0, 1, 0) === record.getCountBins().asScala.toList)
	assert(List(0, 1, 0) === record.getPositiveCountBins().asScala.toList)
	assert(List(0, 0, 0) === record.getNeutralCountBins().asScala.toList)
	assert(List(0, 0, 0) === record.getNegativeCountBins().asScala.toList)
      })
    })


    val records3 = recordParser.getRecordsByTag("Wed Jan 03 12:00:00 +0000 2014\tuid\tuname\ttid\tTweet Text\t#abc#def#ghi#\t0.0\t0.0\tCanada\tToronto\tcity\tPositive\t50.0")
    assert(1 === records3.size)
    records3.foreach(records => {
      assert(List("abc", "def", "ghi") === records._3.map(_._1).toList.sorted)
      assert(List("abc", "def", "ghi") === records._3.map(_._2.getTag()).toList.sorted)
      records._3.map(_._2).foreach(record => {
	assert(List(0, 0, 1) === record.getCountBins().asScala.toList)
	assert(List(0, 0, 1) === record.getPositiveCountBins().asScala.toList)
	assert(List(0, 0, 0) === record.getNeutralCountBins().asScala.toList)
	assert(List(0, 0, 0) === record.getNegativeCountBins().asScala.toList)
      })
    })
  }


  test("Word parsing") {
    val recordParser = new TwitterDemoRecordParser(Long.MinValue, Long.MaxValue-1, 1)
    val records = recordParser.getRecordsByWord("Wed Jan 02 12:00:00 +0000 2014\tuid\tuname\ttid\tHad we known about Moliere, \"Parts of places possible\" might never have seen the light of day four quatre cuatro, 123 456 1 2 3 4 5 6 1.2 1,000 1E-4 you're abc123 http https &amp; &lt; &gt;\t#abc#def#ghi#\t0.0\t0.0\tCanada\tToronto\tcity\tPositive\t50.0", recordParser.getStopWordList)
    assert(1 === records.size)
    records.foreach(record => {
      assert(List("abc123", "day", "light", "moliere", "seen") === record._3.map(_._1).toList.sorted)
      assert(List("abc123", "day", "light", "moliere", "seen") === record._3.map(_._2.getTag()).toList.sorted)
    })
  }
}
