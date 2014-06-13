/*A
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



import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

import scala.collection.JavaConverters._

import org.scalatest.FunSuite

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.tiling.AnalysisDescription
import com.oculusinfo.tilegen.tiling.CompositeAnalysisDescription
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.TestPyramidIO
import com.oculusinfo.tilegen.tiling.TestTileIO

import com.oculusinfo.twitter.binning.TwitterDemoRecord



class TwitterDemoTilingTestSuite extends FunSuite with SharedSparkContext {
	private val dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZ yyyy")
	private val startTime = getTime(2014, 0, 2, 0, 0, 0)
	private val endTime = getTime(2014, 0, 2, 23, 59, 59)
	private val bins = 12
	private val userInfos = Map(
		"a" -> (1, "United States", "St. Louis, MO", "city"),
		"b" -> (2, "United States", "Springfield, KY", "city"),
		"c" -> (3, "United States", "Washington, D.C.", "city"),
		"d" -> (4, "Canada", "Dundas, ON, CA", "town"),
		"e" -> (5, "Canada", "Toronto, ON, CA", "city")
	)

	private var lastTweetId = 0

	// Data generation functions
	private def getTime (year: Int, month: Int, day: Int,
	                     hour: Int, minute: Int, second: Int): Long = {
		val calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0.00"))
		calendar.set(year, month, day, hour, minute, second)
		return calendar.getTime().getTime()
	}

	private def getTime (bin: Int): Long = {
		((bin+0.5)*(endTime-startTime)/bins + startTime).toLong
	}

	private def nextTweetId: Int = {
		lastTweetId = lastTweetId + 1
		lastTweetId
	}
	
	
	def createRecord (bin: Int, userName: String, text: String, lat: Double, lon: Double,
	                  sentiment: Sentiment.SentimentType, confidence: Double): String = {
		val (userId, country, placeName, placeType) = userInfos(userName)
		val tweetId = nextTweetId
		val tags = text.split("[^a-zA-Z0-9#]")
			.filter(_.startsWith("#"))
			.map(_.substring(1))
			.mkString("#", "#", "#")
		
		"%s\t%d\t%s\t%d\t%s\t%s\t%.6f\t%.6f\t%s\t%s\t%s\t%s\t%.4f".format(
			dateParser.format(new Date(getTime(bin))),
			userId,
			userName,
			tweetId,
			text,
			tags,
			lat,
			lon,
			country,
			placeName,
			placeType,
			sentiment.toString,
			confidence)
	}


	test("Test record creation") {
		val line = createRecord(0, "a", "abc bcd cde def #fgh #ghi", 0.0, 0.0,
		                        Sentiment.positive, 50.0)

		val localParser = new TwitterDemoRecordParser(startTime, endTime, bins)


		// Check word parsing
		var records = localParser.getRecordsByWord(line, localParser.getStopWordList)

		assert(1 === records.size)
		var record = records(0)
		assert(0.0 === record._1._1)
		assert(0.0 === record._1._2)

		var tags = record._2.toList.sortBy(_._1)
		assert(6 === tags.size)
		assert(List("abc", "bcd", "cde", "def", "fgh", "ghi") === tags.map(_._1))
		assert(List("abc", "bcd", "cde", "def", "fgh", "ghi") === tags.map(_._2.getTag()))
		tags.map(_._2).foreach(tagRecord => {
			                       assert(1 === tagRecord.getCount())
			                       assert(List(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getCountBins().asScala.toList)
			                       assert(1 === tagRecord.getPositiveCount())
			                       assert(List(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getPositiveCountBins().asScala.toList)
			                       assert(0 === tagRecord.getNeutralCount())
			                       assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getNeutralCountBins().asScala.toList)
			                       assert(0 === tagRecord.getNegativeCount())
			                       assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getNegativeCountBins().asScala.toList)
			                       assert(1 === tagRecord.getRecentTweets.size)
			                       assert("abc bcd cde def #fgh #ghi" === tagRecord.getRecentTweets.get(0).getFirst())
		                       })

		// Check tag parsing
		records = localParser.getRecordsByTag(line)
		assert(1 === records.size)
		record = records(0)
		assert(0.0 === record._1._1)
		assert(0.0 === record._1._2)

		tags = record._2.toList.sortBy(_._1)
		assert(2 === tags.size)
		assert(List("fgh", "ghi") === tags.map(_._1))
		assert(List("fgh", "ghi") === tags.map(_._2.getTag()))
		tags.map(_._2).foreach(tagRecord => {
			                       assert(1 === tagRecord.getCount())
			                       assert(List(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getCountBins().asScala.toList)
			                       assert(1 === tagRecord.getPositiveCount())
			                       assert(List(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getPositiveCountBins().asScala.toList)
			                       assert(0 === tagRecord.getNeutralCount())
			                       assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getNeutralCountBins().asScala.toList)
			                       assert(0 === tagRecord.getNegativeCount())
			                       assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				                              === tagRecord.getNegativeCountBins().asScala.toList)
			                       assert(1 === tagRecord.getRecentTweets.size)
			                       assert("abc bcd cde def #fgh #ghi" === tagRecord.getRecentTweets.get(0).getFirst())
		                       })
	}


	test("Test record binning") {
		import Sentiment._
		val specs = List(( 5, 0, "a", "this is the first tweet      #abc #bcd"),
		                 ( 9, 0, "b", "this is the second tweet     #bcd #cde"),
		                 (10, 0, "c", "this is the third tweet      #cde #def"),
		                 ( 8, 0, "d", "this is the fourth tweet     #def #efg"),
		                 ( 5, 0, "e", "this is the fifth tweet      #efg #fgh"),
		                 ( 3, 0, "a", "this is the sixth tweet      #fgh #ghi"),
		                 ( 4, 0, "b", "this is the seventh tweet    #ghi #hij"),
		                 ( 5, 0, "c", "this is the eighth tweet     #hij #ijk"),
		                 ( 7, 0, "d", "this is the ninth tweet      #ijk #jkl"),
		                 ( 8, 0, "e", "this is the tenth tweet      #jkl #klm"),
		                 ( 2, 0, "a", "this is the eleventh tweet   #klm #lmn"),
		                 ( 1, 0, "b", "this is the twelvth tweet    #lmn #mno"),
		                 ( 1, 0, "c", "this is the thirteenth tweet #mno #nop"),
		                 (10, 0, "d", "this is the fourteenth tweet #nop #opq"),
		                 ( 6, 0, "e", "this is the fifteenth tweet  #opq #pqr"))

		val localData = specs.flatMap(spec => {
			                              val ll = Range(0, spec._1).map(n => createRecord(0+spec._2, spec._3, spec._4,
				         -45.0, -90.0, positive, 50.0))
			                              val ul = Range(0, spec._1).map(n => createRecord(1+spec._2, spec._3, spec._4,
				         45.0, -90.0, positive, 50.0))
			                              val lr = Range(0, spec._1).map(n => createRecord(2+spec._2, spec._3, spec._4,
				         -45.0, 90.0, positive, 50.0))
			                              // Create the upper left quadrant with a different number of records
			                              val ur = Range(0, (11-spec._1)).map(n => createRecord(3+spec._2, spec._3, spec._4,
				              45.0, 90.0, positive, 50.0))
			                              ll union lr union ul  union ur
		                              })
		val localStartTime = startTime
		val localEndTime = endTime
		val localBins = bins
		val levelBounds = List(0, 1)
		val minAnalysis = new TwitterDemoListAnalysis(
			sc, new TwitterMinRecordAnalytic,
			Range(levelBounds(0), levelBounds(1)+1).map(level =>
				(level+".min" -> ((index: TileIndex) => (level == index.getLevel())))
			).toMap + ("global.min" -> ((index: TileIndex) => true))
		)

		val maxAnalysis = new TwitterDemoListAnalysis(
			sc, new TwitterMaxRecordAnalytic,
			Range(levelBounds(0), levelBounds(1)+1).map(level =>
				(level+".max" -> ((index: TileIndex) => (level == index.getLevel())))
			).toMap + ("global.max" -> ((index: TileIndex) => true))
		)

		val tileAnalytics =
			Some(new CompositeAnalysisDescription(minAnalysis, maxAnalysis))
		val dataAnalytics: Option[AnalysisDescription[((Double, Double), Map[String, TwitterDemoRecord]), Int]] = None
		val data = sc.parallelize(localData).mapPartitions(i =>
			{
				val parser = new TwitterDemoRecordParser(localStartTime,
				                                         localEndTime,
				                                         localBins)
				i.flatMap(line => parser.getRecordsByTag(line))
			}
		).map(p => (p._1, p._2, dataAnalytics.map(_.convert(p))))

		val tilePyramid = new WebMercatorTilePyramid
		val binner = new RDDBinner
		val tiles = binner.processDataByLevel(data,
		                                      new CartesianIndexScheme,
		                                      new TwitterDemoBinningAnalytic,
		                                      tileAnalytics,
		                                      dataAnalytics,
		                                      tilePyramid,
		                                      List(0, 1),
		                                      bins=1).collect
		assert(5 === tiles.size)
		val recordsByTile = tiles.map(tile => (tile.getDefinition,
		                                       tile.getBin(0, 0).asScala.toList)).toMap

		// Level 0 totals:
		// cde: 60    def: 58    opq: 54    jkl: 52
		// bcd: 50    efg: 48    ijk: 46    nop: 44
		// klm: 42    hij: 40    fgh: 38    ghi: 36
		// lmn: 28    mno: 26    pqr: 23    abc: 21
		val record000 = recordsByTile(new TileIndex(0, 0, 0, 1, 1))
		assert(List("cde", "def", "opq", "jkl", "bcd", "efg", "ijk", "nop", "klm", "hij")
			       === record000.map(_.getTag()))
		assert(List(60, 58, 54, 52, 50, 48, 46, 44, 42, 40)
			       === record000.map(_.getCount()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record000.map(_.getCountBins().get(0)))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record000.map(_.getCountBins().get(1)))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record000.map(_.getCountBins().get(2)))
		assert(List( 3,  4,  6,  7,  8,  9, 10, 11, 12, 13)
			       === record000.map(_.getCountBins().get(3)))
		Range(4, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record000.map(_.getCountBins().get(n)))
		)
		assert(List(60, 58, 54, 52, 50, 48, 46, 44, 42, 40)
			       === record000.map(_.getPositiveCount()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record000.map(_.getPositiveCountBins().get(0)))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record000.map(_.getPositiveCountBins().get(1)))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record000.map(_.getPositiveCountBins().get(2)))
		assert(List( 3,  4,  6,  7,  8,  9, 10, 11, 12, 13)
			       === record000.map(_.getPositiveCountBins().get(3)))
		Range(4, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record000.map(_.getPositiveCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record000.map(_.getNeutralCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record000.map(_.getNegativeCountBins().get(n)))
		)

		// Totals for level 1 with numbers given:
		// cde: 19    def: 18    opq: 16    jkl: 15
		// bcd: 14    efg: 13    ijk: 12    nop: 11
		// klm: 10    hij:  9    fgh:  8    ghi:  7
		// pqr:  6    abc:  5    lmn:  3    mno:  2
		val record100 = recordsByTile(new TileIndex(1, 0, 0, 1, 1))
		assert(List("cde", "def", "opq", "jkl", "bcd", "efg", "ijk", "nop", "klm", "hij")
			       === record100.map(_.getTag()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record100.map(_.getCount()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record100.map(_.getCountBins().get(0)))
		Range(1, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record100.map(_.getCountBins().get(n)))
		)
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record100.map(_.getPositiveCountBins().get(0)))
		Range(1, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record100.map(_.getPositiveCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record100.map(_.getNeutralCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record100.map(_.getNegativeCountBins().get(n)))
		)

		val record101 = recordsByTile(new TileIndex(1, 0, 1, 1, 1))
		assert(List("cde", "def", "opq", "jkl", "bcd", "efg", "ijk", "nop", "klm", "hij")
			       === record101.map(_.getTag()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record101.map(_.getCount()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record101.map(_.getCountBins().get(1)))
		      (Range(0, 1) union Range(2, 12)).map(n =>
			      assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				             === record101.map(_.getCountBins().get(n)))
		      )
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record101.map(_.getPositiveCountBins().get(1)))
		      (Range(0, 1) union Range(2, 12)).map(n =>
			      assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				             === record101.map(_.getPositiveCountBins().get(n)))
		      )
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record101.map(_.getNeutralCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record101.map(_.getNegativeCountBins().get(n)))
		)

		val record110 = recordsByTile(new TileIndex(1, 1, 0, 1, 1))
		assert(List("cde", "def", "opq", "jkl", "bcd", "efg", "ijk", "nop", "klm", "hij")
			       === record110.map(_.getTag()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record110.map(_.getCount()))
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record110.map(_.getCountBins().get(2)))
		      (Range(0, 2) union Range(3, 12)).map(n =>
			      assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				             === record110.map(_.getCountBins().get(n)))
		      )
		assert(List(19, 18, 16, 15, 14, 13, 12, 11, 10, 9)
			       === record110.map(_.getPositiveCountBins().get(2)))
		      (Range(0, 2) union Range(3, 12)).map(n =>
			      assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				             === record110.map(_.getPositiveCountBins().get(n)))
		      )
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record110.map(_.getNeutralCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record110.map(_.getNegativeCountBins().get(n)))
		)

		// Totals for level 1 with (11-number given)L
		// mno: 20    lmn: 19    ghi: 15    fgh: 14
		// hij: 13    klm: 12    nop: 11    ijk: 10
		// efg:  9    bcd:  8    jkl:  7    abc:  6
		// opq:  6    pqr:  5    def:  4    cde:  3
		val record111 = recordsByTile(new TileIndex(1, 1, 1, 1, 1))
		assert(List("mno", "lmn", "ghi", "fgh", "hij", "klm", "nop", "ijk", "efg", "bcd")
			       === record111.map(_.getTag()))
		assert(List(20, 19, 15, 14, 13, 12, 11, 10, 9, 8)
			       === record111.map(_.getCount()))
		assert(List(20, 19, 15, 14, 13, 12, 11, 10, 9, 8)
			       === record111.map(_.getCountBins().get(3)))
		      (Range(0, 3) union Range(4, 12)).map(n =>
			      assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				             === record111.map(_.getCountBins().get(n)))
		      )
		assert(List(20, 19, 15, 14, 13, 12, 11, 10, 9, 8)
			       === record111.map(_.getPositiveCountBins().get(3)))
		      (Range(0, 3) union Range(4, 12)).map(n =>
			      assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				             === record111.map(_.getPositiveCountBins().get(n)))
		      )
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record111.map(_.getNeutralCountBins().get(n)))
		)
		Range(0, 12).map(n =>
			assert(List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
				       === record111.map(_.getNegativeCountBins().get(n)))
		)
	}


	test("Metadata serialization") {
		import Sentiment._
		val specs = List(( 5, 0, "a", "this is the first tweet      #abc #bcd"),
		                 ( 9, 0, "b", "this is the second tweet     #bcd #cde"),
		                 (10, 0, "c", "this is the third tweet      #cde #def"),
		                 ( 8, 0, "d", "this is the fourth tweet     #def #efg"),
		                 ( 5, 0, "e", "this is the fifth tweet      #efg #fgh"),
		                 ( 3, 0, "a", "this is the sixth tweet      #fgh #ghi"),
		                 ( 4, 0, "b", "this is the seventh tweet    #ghi #hij"),
		                 ( 5, 0, "c", "this is the eighth tweet     #hij #ijk"),
		                 ( 7, 0, "d", "this is the ninth tweet      #ijk #jkl"),
		                 ( 8, 0, "e", "this is the tenth tweet      #jkl #klm"),
		                 ( 2, 0, "a", "this is the eleventh tweet   #klm #lmn"),
		                 ( 1, 0, "b", "this is the twelvth tweet    #lmn #mno"),
		                 ( 1, 0, "c", "this is the thirteenth tweet #mno #nop"),
		                 (10, 0, "d", "this is the fourteenth tweet #nop #opq"),
		                 ( 6, 0, "e", "this is the fifteenth tweet  #opq #pqr"))

		val localData = specs.flatMap(spec =>
			{
				val ll = Range(0, spec._1).map(n =>
					createRecord(0+spec._2, spec._3, spec._4,
					             -45.0, -90.0, positive, 50.0))
				val ul = Range(0, spec._1).map(n =>
					createRecord(1+spec._2, spec._3, spec._4,
					             45.0, -90.0, positive, 50.0))
				val lr = Range(0, spec._1).map(n =>
					createRecord(2+spec._2, spec._3, spec._4,
					             -45.0, 90.0, positive, 50.0))
				// Create the upper left quadrant with a different number of records
				val ur = Range(0, (11-spec._1)).map(n =>
					createRecord(3+spec._2, spec._3, spec._4,
					             45.0, 90.0, positive, 50.0))
				ll union lr union ul  union ur
			}
		)
		val localStartTime = startTime
		val localEndTime = endTime
		val localBins = bins
		val levelBounds = List(0, 4)
		val minAnalysis = new TwitterDemoListAnalysis(
			sc, new TwitterMinRecordAnalytic,
			Range(levelBounds(0), levelBounds(1)+1).map(level =>
				(level+"" -> ((index: TileIndex) => (level == index.getLevel())))
			).toMap + ("global" -> ((index: TileIndex) => true))
		)

		val maxAnalysis = new TwitterDemoListAnalysis(
			sc, new TwitterMaxRecordAnalytic,
			Range(levelBounds(0), levelBounds(1)+1).map(level =>
				(level+"" -> ((index: TileIndex) => (level == index.getLevel())))
			).toMap + ("global" -> ((index: TileIndex) => true))
		)

		val tileAnalytics =
			Some(new CompositeAnalysisDescription(minAnalysis, maxAnalysis))
		val dataAnalytics: Option[AnalysisDescription[((Double, Double), Map[String, TwitterDemoRecord]), Int]] = None
		val data = sc.parallelize(localData).mapPartitions(i =>
			{
				val parser = new TwitterDemoRecordParser(localStartTime,
				                                         localEndTime,
				                                         localBins)
				i.flatMap(line => parser.getRecordsByTag(line))
			}
		).map(p => (p._1, p._2, dataAnalytics.map(_.convert(p))))

		val tilePyramid = new WebMercatorTilePyramid
		val binner = new RDDBinner
		val tio = new TestTileIO

		Range(0, 4).map(level =>
			{
				val tiles = binner.processDataByLevel(data,
				                                      new CartesianIndexScheme,
				                                      new TwitterDemoBinningAnalytic,
				                                      tileAnalytics,
				                                      dataAnalytics,
				                                      tilePyramid,
				                                      List(level),
				                                      bins=1)

				tio.writeTileSet(tilePyramid,
				                 "abc",
				                 tiles,
				                 new TwitterDemoValueDescription,
				                 tileAnalytics,
				                 dataAnalytics)
			}
		)

		val mdo = tio.readMetaData("abc")
		assert(mdo.isDefined)
		val md = mdo.get

		println()
		println()
		println()
		println(md)
		println()
		println()
		println()

		assert(null != md.getCustomMetaData("global", "minimum"))
		assert(null != md.getCustomMetaData("0",      "minimum"))
		assert(null != md.getCustomMetaData("1",      "minimum"))
		assert(null != md.getCustomMetaData("2",      "minimum"))
		assert(null != md.getCustomMetaData("3",      "minimum"))
		assert(null != md.getCustomMetaData("global", "maximum"))
		assert(null != md.getCustomMetaData("0",      "maximum"))
		assert(null != md.getCustomMetaData("1",      "maximum"))
		assert(null != md.getCustomMetaData("2",      "maximum"))
		assert(null != md.getCustomMetaData("3",      "maximum"))
//		assert(List(0, 1, 2, 3) ===
//			       md.getLevelMinimums().keySet().asScala.toList.map(_.intValue).sorted)
//		assert(List(0, 1, 2, 3) ===
//			       md.getLevelMaximums().keySet().asScala.toList.map(_.intValue).sorted)
	}
}
