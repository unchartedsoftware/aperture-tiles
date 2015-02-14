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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

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
													Arrays.asList(new RecentTweet("", _endTimeSecs, "", "")),
													_endTimeSecs);
	
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
													Arrays.asList(new RecentTweet("", _endTimeSecs, "", "")),
													_endTimeSecs);
			
		Assert.assertEquals(_sampleRecord, a);
	}
	
	//---- Adding a tweet to a record before the beginning of its valid time range
	@Test
	public void testAddTweetBeforeBeginning() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - (2678400L+1), "", ""); // 1 month + 1 sec from end time
		
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(_sampleRecord, a);
	}
	
	//---- Adding a tweet to a record after the end time
	@Test
	public void testAddTweetAfterEnd() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs + 1L, "", ""); // 1 sec after end time

		
		TwitterDemoTopicRecord a = TwitterDemoTopicRecord.addTweetToRecord(_sampleRecord, tweet1);
		Assert.assertEquals(_sampleRecord, a);
	}	
	
	//---- Adding a tweet to a record so it increments monthly count per day, but not quarter-daily or hourly
	@Test
	public void testAddTweetMonthly() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - (2678400L-1), "", ""); // 1 month - 1 sec from end time
		
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
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - (604800L-1), "", ""); // 7 days - 1 sec from end time
	
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
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - 1L, "", ""); // 1 sec prior to end time
	
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
	

	//---- construct a new record with a tweet before the beginning of its valid time range
	@Test
	public void testConstructRecordBeforeBeginning() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - (2678400L+1), "", ""); // 1 month + 1 sec from end time
		
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopic, Arrays.asList(tweet1), _endTimeSecs);
		Assert.assertEquals(a.getCountMonthly(), 0);
		Assert.assertEquals(a.getCountDaily(), null);
		Assert.assertEquals(a.getCountPer6hrs(), null);
		Assert.assertEquals(a.getCountPerHour(), null);
	}
	
	//---- construct a new record with a tweet after the end time
	@Test
	public void testConstructRecordAfterEnd() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs + 1L, "", ""); // 1 sec after end time

		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopic, Arrays.asList(tweet1), _endTimeSecs);
		Assert.assertEquals(a.getCountMonthly(), 0);
		Assert.assertEquals(a.getCountDaily(), null);
		Assert.assertEquals(a.getCountPer6hrs(), null);
		Assert.assertEquals(a.getCountPerHour(), null);
	}	
	
	//---- construct a new record with a tweet so it increments monthly count per day, but not quarter-daily or hourly
	@Test
	public void testConstructRecordMonthly() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - (2678400L-1), "", ""); // 1 month - 1 sec from end time
		
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopic, Arrays.asList(tweet1), _endTimeSecs);
		Assert.assertEquals(a.getCountMonthly(), _sampleRecord.getCountMonthly()+1);
		Assert.assertTrue(a.getCountDaily().get(30) == _sampleRecord.getCountDaily().get(30)+1);	// check count for last day of month
		for (int n=0; n<30; n++) {
			Assert.assertTrue(a.getCountDaily().get(n) == _sampleRecord.getCountDaily().get(n));
		}
		Assert.assertEquals(a.getCountPer6hrs(), _sampleRecord.getCountPer6hrs());
		Assert.assertEquals(a.getCountPerHour(), _sampleRecord.getCountPerHour());
	}
	
	
	//---- construct a new record with a tweet so it increments monthly count per day and quarter-daily, but not hourly
	@Test
	public void testConstructRecordQuarterDaily() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - (604800L-1), "", ""); // 7 days - 1 sec from end time
	
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopic, Arrays.asList(tweet1), _endTimeSecs);
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

	//---- construct a new record with a tweet so it increments monthly count per day, quarter-daily, and hourly
	@Test
	public void testConstructRecordHourly() {	
		
		RecentTweet tweet1 = new RecentTweet("Eu amo o futebol", _endTimeSecs - 1L, "", ""); // 1 sec prior to end time
	
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopic, Arrays.asList(tweet1), _endTimeSecs);
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
	
	//---- Adding two records
	@Test
    public void testRecordAggregation () {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 1, 
								Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
								Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0),
								Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0),
								Arrays.asList(new RecentTweet("Eu amo o futebol", _endTimeSecs - 1L, "barry", "")),	// 1 sec prior to end time
								_endTimeSecs);

		TwitterDemoTopicRecord b = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 1, 
								Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
								Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0),
								Arrays.asList(0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0),
								Arrays.asList(new RecentTweet("Nos todos amamos o futebol", _endTimeSecs - 3601L, "jorge", "")),	// 1 hr + 1 sec prior to end time
								_endTimeSecs);

		TwitterDemoTopicRecord c = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 2, 
								Arrays.asList(2, 0, 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
											0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
								Arrays.asList(2, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0),
								Arrays.asList(1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
										0, 0, 0, 0),
								Arrays.asList(new RecentTweet("Eu amo o futebol", _endTimeSecs - 1L, "barry", ""),
											  new RecentTweet("Nos todos amamos o futebol", _endTimeSecs - 3601L, "jorge", "")),
								_endTimeSecs);		

		Assert.assertEquals(c, TwitterDemoTopicRecord.addRecords(a, b));
    }
	
	//---- Adding records with different topics
	@Test(expected=IllegalArgumentException.class)
    public void testIllegalRecordAddition () {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 1, 
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("Eu amo o futebol", _endTimeSecs - 1L, "", "")),	// 1 sec prior to end time
													_endTimeSecs);
		
		TwitterDemoTopicRecord b = new TwitterDemoTopicRecord("hoquei", "hockey", 1, 
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("Todos nos gostamos de hoquei", _endTimeSecs - 3601L, "", "")),	// 1 hr + 1 sec prior to end time
													_endTimeSecs);
		
        TwitterDemoTopicRecord.addRecords(a, b);
    }

	
	//---- Min of two records
    @Test
    public void testMin() {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 15, 
													Arrays.asList(1, 0, 0, 0, 0, 0, 5, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 2, 3, 0),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("blah1", _endTimeSecs - 1000L, "", ""),
																new RecentTweet("blah2", _endTimeSecs - 2000L, "", ""),
																new RecentTweet("blah3", _endTimeSecs - 3000L, "", "")),
													_endTimeSecs);
		
		TwitterDemoTopicRecord b = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 17, 
													Arrays.asList(1, 0, 0, 0, 0, 5, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 8),
													Arrays.asList(0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 5, 0, 0, 0, 0),
													Arrays.asList(0, 0, 0, 0, 0, 0, 1, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("blah3", _endTimeSecs - 1500L, "", ""),
																new RecentTweet("blah4", _endTimeSecs - 2500L, "", ""),
																new RecentTweet("blah5", _endTimeSecs - 3500L, "", "")),
													_endTimeSecs);

		TwitterDemoTopicRecord c = new TwitterDemoTopicRecord(null, null, 15, 
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8),
													Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													new ArrayList<RecentTweet>(),
													_endTimeSecs);
		
        Assert.assertEquals(c, TwitterDemoTopicRecord.minOfRecords(a, b));
    }	
	
    
	//---- Max of two records
    @Test
    public void testMax() {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 15,
													Arrays.asList(1, 0, 0, 0, 0, 0, 5, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 2, 3, 0),
													Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("blah1", _endTimeSecs - 1000L, "", ""),
																new RecentTweet("blah2", _endTimeSecs - 2000L, "", ""),
																new RecentTweet("blah3", _endTimeSecs - 3000L, "", "")),
													_endTimeSecs);
		
		TwitterDemoTopicRecord b = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 17, 
													Arrays.asList(1, 0, 0, 0, 0, 5, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 8),
													Arrays.asList(0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 5, 0, 0, 0, 0),
													Arrays.asList(0, 0, 0, 0, 0, 0, 1, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("blah3", _endTimeSecs - 1500L, "", ""),
																new RecentTweet("blah4", _endTimeSecs - 2500L, "", ""),
																new RecentTweet("blah5", _endTimeSecs - 3500L, "", "")),
													_endTimeSecs);

		TwitterDemoTopicRecord c = new TwitterDemoTopicRecord(null, null, 17, 
													Arrays.asList(1, 0, 0, 0, 0, 5, 5, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 9),
													Arrays.asList(1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 5, 0, 2, 3, 0),
													Arrays.asList(1, 0, 0, 0, 0, 0, 1, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													new ArrayList<RecentTweet>(),
													_endTimeSecs);
        Assert.assertEquals(c, TwitterDemoTopicRecord.maxOfRecords(a, b));
    }  

    // Check string conversion
    @Test
    public void testStringConversion () {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord(_sampleTopic, _sampleTopicEnglish, 2, 
													Arrays.asList(2, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
																0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(2, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0),
													Arrays.asList(1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
															0, 0, 0, 0),
													Arrays.asList(new RecentTweet("abcdef", _endTimeSecs - 1000L, "bob", "neg"),
															new RecentTweet("abc\"\"\\\"\\\\\"\\\\\\\"def", _endTimeSecs - 2000L, "alice", "pos")),
													_endTimeSecs);    	

        String as = a.toString();
        TwitterDemoTopicRecord b = TwitterDemoTopicRecord.fromString(as);
        Assert.assertEquals(a, b);
//        Assert.assertEquals(a.getTopic(), b.getTopic());
//        Assert.assertEquals(a.getTopicEnglish(), b.getTopicEnglish());
//        Assert.assertEquals(a.getCountDaily(), b.getCountDaily());
//        Assert.assertEquals(a.getCountPer6hrs(), b.getCountPer6hrs());
//        Assert.assertEquals(a.getCountPerHour(), b.getCountPerHour());
//        Assert.assertEquals(a.getRecentTweets(), b.getRecentTweets());
//        Assert.assertTrue(a.getCountMonthly() == b.getCountMonthly());
//        Assert.assertTrue(a.getEndTime() == b.getEndTime());
    }
	
}
