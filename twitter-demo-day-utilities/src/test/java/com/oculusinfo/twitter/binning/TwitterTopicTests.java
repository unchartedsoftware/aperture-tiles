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
package com.oculusinfo.twitter.binning;

import java.util.Arrays;
import java.util.List;

import com.oculusinfo.binning.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TwitterTopicTests {

	private long _endTimeSecs = 2000000000L;			// arbitrary end time (epoch format in sec) for these JUnit tests
	private String _sampleTopic = "futebol";			// sample topic for these tests (in portuguese)
	private String _sampleTopicEnglish = "football";	// sample topic in English
	
	// sample record with no counts and a given end time
	private TwitterDemoTopicRecord _sampleRecord = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 0, 
													Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new Pair<String, Long>("", _endTimeSecs)),
													_endTimeSecs);

//	@Test
//	// Test an empty record
//	public void testEmptyRecord() {
//		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord();
//
//		Assert.assertEquals(null, a.getTopic());
//		Assert.assertEquals(null, a.getTopicEnglish());
//		Assert.assertEquals(0, a.getCountMonthly());
//		Assert.assertEquals(null, a.getCountDaily());
//		Assert.assertEquals(null, a.getCountPer6hrs());
//		Assert.assertEquals(null, a.getCountPerHour());
//		Assert.assertEquals(null, a.getRecentTweets());
//	}
	
	//---- Create a topic with no counts and an end time.
	@Test
	public void testCreateTopicWithNoCounts() {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord("futebol", "football", 0, 
				Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
				Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0),
				Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0),
				Arrays.asList(new Pair<String, Long>("", _endTimeSecs)),
				_endTimeSecs);
			
		Assert.assertEquals(_sampleRecord, a);
	}
	
	//---- Adding a tweet to a record before the beginning of its valid time range
	@Test
	public void testAddTweetBeforeBeginning() {	
		
		Pair<String, Long> tweet1 = new Pair<String, Long>("Eu amo o futebol", _endTimeSecs - (2678400L+1)); // 1 month + 1 sec from end time
	
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(_sampleRecord, a);
	}
	
	//---- Adding a tweet to a record after the end time
	@Test
	public void testAddTweetAfterEnd() {	
		
		Pair<String, Long> tweet1 = new Pair<String, Long>("Eu amo o futebol", _endTimeSecs + 1L); // 1 sec after end time
	
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(_sampleRecord, a);
	}	
	
	//---- Adding a tweet to a record so it increments monthly count per day, but not quarter-daily or hourly
	@Test
	public void testAddTweetMonthly() {	
		
		Pair<String, Long> tweet1 = new Pair<String, Long>("Eu amo o futebol", _endTimeSecs - (2678400L-1)); // 1 month - 1 sec from end time
	
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(a.getCountMonthly(), _sampleRecord.getCountMonthly()+1);
		Assert.assertTrue(a.getCountDaily().get(30) == _sampleRecord.getCountDaily().get(30)+1);	// check count for last day of month
		for (int n=0; n<30; n++) {
			Assert.assertTrue(a.getCountDaily().get(n) == _sampleRecord.getCountDaily().get(n));
		}
		Assert.assertEquals(a.getCountPer6hrs(), _sampleRecord.getCountPer6hrs());
		Assert.assertEquals(a.getCountPerHour(), _sampleRecord.getCountPerHour());
	}
	
	
	//---- Adding a tweet to a record so it increments monthly count per day and quarter-daily, but not hourly
	@Test
	public void testAddTweetQuarterDaily() {	
		
		Pair<String, Long> tweet1 = new Pair<String, Long>("Eu amo o futebol", _endTimeSecs - (604800L-1)); // 7 days - 1 sec from end time
	
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(a.getCountMonthly(), _sampleRecord.getCountMonthly()+1);
		for (int n=0; n<31; n++) {
			if (n!=6)
				Assert.assertTrue(a.getCountDaily().get(n) == _sampleRecord.getCountDaily().get(n));
			else
				Assert.assertTrue(a.getCountDaily().get(n) == _sampleRecord.getCountDaily().get(n)+1);	// check count for 7 days from end			
		}
		for (int n=0; n<28; n++) {
			if (n!=27)
				Assert.assertTrue(a.getCountPer6hrs().get(n) == _sampleRecord.getCountPer6hrs().get(n));
			else
				Assert.assertTrue(a.getCountPer6hrs().get(n) == _sampleRecord.getCountPer6hrs().get(n)+1);	// check last quarter-daily count			
		}		
		Assert.assertEquals(a.getCountPerHour(), _sampleRecord.getCountPerHour());
	}	

	//---- Adding a tweet to a record so it increments monthly count per day, quarter-daily, and hourly
	@Test
	public void testAddTweetHourly() {	
		
		Pair<String, Long> tweet1 = new Pair<String, Long>("Eu amo o futebol", _endTimeSecs - 1L); // 1 sec prior to end time
	
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(a.getCountMonthly(), _sampleRecord.getCountMonthly()+1);
		for (int n=0; n<31; n++) {
			if (n!=0)
				Assert.assertTrue(a.getCountDaily().get(n) == _sampleRecord.getCountDaily().get(n));
			else
				Assert.assertTrue(a.getCountDaily().get(n) == _sampleRecord.getCountDaily().get(n)+1);		
		}
		for (int n=0; n<28; n++) {
			if (n!=0)
				Assert.assertTrue(a.getCountPer6hrs().get(n) == _sampleRecord.getCountPer6hrs().get(n));
			else
				Assert.assertTrue(a.getCountPer6hrs().get(n) == _sampleRecord.getCountPer6hrs().get(n)+1);			
		}
		for (int n=0; n<24; n++) {
			if (n!=0)
				Assert.assertTrue(a.getCountPerHour().get(n) == _sampleRecord.getCountPerHour().get(n));
			else
				Assert.assertTrue(a.getCountPerHour().get(n) == _sampleRecord.getCountPerHour().get(n)+1);			
		}	
	}	
	
	//TODO ...
	//add a few few tests for recent tweets too - just look at the tests for TwitterDemoRecord, they'll be pretty similar, I think.	    
}
