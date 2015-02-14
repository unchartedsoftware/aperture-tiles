/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.twitter.binning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class TwitterDemoTopicRecord implements Serializable {
	private static final long serialVersionUID = 1L;	//NOTE:  using default serialVersion ID
	
	private static final int NUM_DAYS = 31;			// days per month
	private static final int NUM_QUARTERDAYS = 28;	// quarter days per week
	private static final int NUM_HOURS = 24;		// hours per day	

	private String _topic; 							// Twitter topic in original language
	private String _topicEnglish; 					// Twitter topic in English
	private int _countMonthly; 						// total number of tweets per month with this topic
	private List<Integer> _countDaily; 				// tweet count per day for the past month with this topic
	private List<Integer> _countPer6hrs; 			// tweet count per six hours for last week with this topic
	private List<Integer> _countPerHour; 			// tweet count per hour for last 24 hrs with this topic
	private List<RecentTweet> _recentTweets; // 10 most recent tweets with this topic
	private long _endTimeSecs;						// end time (in secs) for this data record (so valid time window
													//    between endTimeSecs and endTimeSecs - 1 month

	public TwitterDemoTopicRecord(String topic, String topicEnglish,
			int countMonthly, List<Integer> countDaily,
			List<Integer> countPer6hrs, List<Integer> countPerHour,
			List<RecentTweet> recentTweets, long endTimeSecs) {
		
		_topic = topic;
		_topicEnglish = topicEnglish;
		_countMonthly = countMonthly;
		_recentTweets = recentTweets;
		_endTimeSecs = endTimeSecs;
				
		if (countDaily.size() > NUM_DAYS) {
			throw new IllegalArgumentException("countDaily size cannot be > " + NUM_DAYS);
		}
		else if (countPer6hrs.size() > NUM_QUARTERDAYS) {
			throw new IllegalArgumentException("countPer6hrs size cannot be > " + NUM_QUARTERDAYS);
		}
		else if (countPerHour.size() > NUM_HOURS) {
			throw new IllegalArgumentException("countPerHour size cannot be > " + NUM_HOURS);
		}
				
		initTopicArrays(countDaily, countPer6hrs, countPerHour);		
	}
	
	// Secondary constructor (for adding 1 new tweet to an empty record)
	public TwitterDemoTopicRecord(String topic, String topicEnglish,
			List<RecentTweet> newTweet, long endTimeSecs) {
		
		_topic = null;
		_topicEnglish = null;
		_countMonthly = 0;
		_recentTweets = null;
		_endTimeSecs = 0;
		_countDaily = null;
		_countPer6hrs = null;
		_countPerHour = null;
		
		// time interval between new tweet and endTime
		//assert(newTweet.size()==1);
		float secsSinceEnd = (float)(endTimeSecs - newTweet.get(0).getTime());

		if (secsSinceEnd > 2.6784e6) { // 2678400 = 31*24*60*60
			// more than 1 month ago disregard this new tweet

		} else if (secsSinceEnd <= 0) {
			// before endTime so disregard this new tweet

		} else {
			// new tweet occurred within 1 month from endtime
			_topic = topic;
			_topicEnglish = topicEnglish;
			_countMonthly=1;
			_recentTweets = newTweet;
			_endTimeSecs = endTimeSecs;
			initTopicArrays(null, null, null);

			int hours = (int)(secsSinceEnd * 2.7778e-4);	//1/3600
			int quarterDays = (int)(secsSinceEnd * 4.6296e-5); // 1/21600 = 1/60*60*6;
			int days = (int)(secsSinceEnd * 1.1574e-5); //1/86400 = 1/60*60*24;

			if ((hours >= 0) && (hours < NUM_HOURS)) {
				_countPerHour.set(hours, 1);
			}
			if ((quarterDays >= 0) && (quarterDays < NUM_QUARTERDAYS)) {
				_countPer6hrs.set(quarterDays, 1);
			}
			if ((days >= 0) && (days < NUM_DAYS)) {
				_countDaily.set(days, 1);
			}
		}
	}
	
	private void initTopicArrays(List<Integer> countDaily, List<Integer> countPer6hrs, List<Integer> countPerHour) {
	
		_countDaily = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
									0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
									0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		if (countDaily != null) {
			for (int n=0; n<countDaily.size(); n++) {
				_countDaily.set(n, countDaily.get(n));
			}
		}

		_countPer6hrs = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
									  0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
								 	  0, 0, 0, 0, 0, 0, 0, 0);
		if (countPer6hrs != null) {
			for (int n=0; n<countPer6hrs.size(); n++) {
				_countPer6hrs.set(n, countPer6hrs.get(n));
			}
		}
		
		_countPerHour = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
									  0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
									  0, 0, 0, 0);
		if (countPerHour != null) {
			for (int n=0; n<countPerHour.size(); n++) {
				_countPerHour.set(n, countPerHour.get(n));
			}
		}
	}

	public String getTopic() {
		return _topic;
	}

	public String getTopicEnglish() {
		return _topicEnglish;
	}

	public int getCountMonthly() {
		return _countMonthly;
	}

	public List<Integer> getCountDaily() {
		return _countDaily;
	}

	public List<Integer> getCountPer6hrs() {
		return _countPer6hrs;
	}

	public List<Integer> getCountPerHour() {
		return _countPerHour;
	}

	public List<RecentTweet> getRecentTweets() {
		return _recentTweets;
	}
	
	public long getEndTime() {
		return _endTimeSecs;
	}

    private int getHash (Object obj) {
        if (null == obj) return 0;
        return obj.hashCode();
    }
    
    @Override
    public int hashCode () {
        return (getHash(_topic));
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (null == obj)
			return false;
		if (!(obj instanceof TwitterDemoTopicRecord))
			return false;

		TwitterDemoTopicRecord that = (TwitterDemoTopicRecord) obj;
		
		if ((this.getTopic()!= null) && (!this.getTopic().equals(that.getTopic()))) {
			return false;
		}
		if ((this.getTopicEnglish()!= null) && (!this.getTopicEnglish().equals(that.getTopicEnglish()))) {
			return false;
		}
							
		return (this.getCountMonthly() == that.getCountMonthly()
				&& this.getEndTime() == that.getEndTime()
				&& listsEqual(this.getCountDaily(), that.getCountDaily())
				&& listsEqual(this.getCountPer6hrs(), that.getCountPer6hrs())
				&& listsEqual(this.getCountPerHour(), that.getCountPerHour())
				&& listsEqual(this.getRecentTweets(), that.getRecentTweets()));
	}

	private static boolean objectsEqual(Object a, Object b) {
		if (null == a)
			return null == b;
		return a.equals(b);
	}

	private static <T> boolean listsEqual(List<T> a, List<T> b) {
		if (null == a)
			return null == b;
		if (null == b)
			return false;
		if (a.size() != b.size())
			return false;
		for (int i = 0; i < a.size(); ++i) {
			if (!objectsEqual(a.get(i), b.get(i)))
				return false;
		}
		return true;
	}
	
	private <T> String mkString(List<T> list, String separator) {
		String result = "";
		for (int i = 0; i < list.size(); ++i) {
			if (i > 0)
				result = result + separator;
			result = result + list.get(i).toString();
		}
		return result;
	}
	
	private static String escapeString(String string) {
		if (null == string)
			return "null";
		else
			return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"")
					+ "\"";
	}

	private static String unescapeString(String string) {
		if (null == string)
			return null;
		if ("null".equals(string))
			return null;

		// Remove start and end quote
		string = string.substring(1, string.length() - 1);
		// Replace escaped characters
		return string.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private static String eat(String from, String prefix) {
		if (from.startsWith(prefix)) {
			return from.substring(prefix.length());
		}
		throw new IllegalArgumentException("String " + from
				+ " didn't begin with expected prefix " + prefix);
	}

	private static int getQuotedStringEnd(String from) {
		if (from.startsWith("null"))
			return 4;
		if (!from.startsWith("\""))
			throw new IllegalArgumentException(
					"Quoted string didn't start with quote");
		int lastQuote = 0;
		while (true) {
			lastQuote = from.indexOf("\"", lastQuote + 1);
			if (lastQuote < 0)
				throw new IllegalArgumentException(
						"Couldn't find the end of quoted string");
			int slashes = 0;
			for (int i = lastQuote - 1; i >= 0; --i) {
				if ('\\' == from.charAt(i)) {
					++slashes;
				} else {
					break;
				}
			}
			if (0 == (slashes % 2)) {
				// final quote - we're done
				return lastQuote + 1;
			}
		}
	}

	@Override
	public String toString() {
		String result = ("{topic: " + escapeString(_topic) + ", "
				+ "topicEnglish: " + escapeString(_topicEnglish) + ", "
				+ "countMonthly: " + _countMonthly + ", " + "countDaily: ["
				+ mkString(_countDaily, ", ") + "], " + "countPer6hrs: ["
				+ mkString(_countPer6hrs, ", ") + "], " + "countPerHour: ["
				+ mkString(_countPerHour, ", ") + "], " + "recent: [");
		for (int i = 0; i < _recentTweets.size(); ++i) {
			RecentTweet rt = _recentTweets.get(i);
			if (i > 0)
				result += ", ";
			result += "(" + escapeString(rt.getText()) + ", " + rt.getTime() + ", " + escapeString(rt.getUser()) + ", "
					+ escapeString(rt.getSentiment()) + ")";
		}
		result += "], endTimeSecs: " + _endTimeSecs + "}";
		return result;
	}

	private static String eatIntList(String from, List<Integer> result) {
		int nextComma = from.indexOf(",");
		int nextBracket = from.indexOf("]");
		while (nextComma > 0 && nextComma < nextBracket) {
			result.add(Integer.parseInt(from.substring(0, nextComma)));
			from = from.substring(nextComma + 2);
			nextComma = from.indexOf(",");
			nextBracket = from.indexOf("]");
		}
		if (nextBracket > 0)
			result.add(Integer.parseInt(from.substring(0, nextBracket)));
		return from.substring(nextBracket);
	}

	public static TwitterDemoTopicRecord fromString(String value) {
		value = eat(value, "{topic: ");
		int end = getQuotedStringEnd(value);
		String topic = unescapeString(value.substring(0, end));

		value = eat(value.substring(end), ", topicEnglish: ");
		end = getQuotedStringEnd(value);
		String topicEnglish = unescapeString(value.substring(0, end));

		value = eat(value.substring(end), ", countMonthly: ");
		end = value.indexOf(", countDaily: [");
		int countMonthly = Integer.parseInt(value.substring(0, end));

		value = eat(value.substring(end), ", countDaily: [");
		List<Integer>countDaily = new ArrayList<>();
		value = eatIntList(value, countDaily);

		value = eat(value, "], countPer6hrs: [");
		List<Integer>countPer6hrs = new ArrayList<>();
		value = eatIntList(value, countPer6hrs);

		value = eat(value, "], countPerHour: [");
		List<Integer>countPerHour = new ArrayList<>();
		value = eatIntList(value, countPerHour);

		value = eat(value, "], recent: [");
		List<RecentTweet> recentTweets = new ArrayList<>();
		while (value.startsWith("(")) {
			value = eat(value, "(");
			end = getQuotedStringEnd(value);
			String tweet = unescapeString(value.substring(0, end));

			value = eat(value.substring(end), ", ");
			end = value.indexOf(",");
			long tweetCount = Long.parseLong(value.substring(0, end));

			value = eat(value.substring(end), ", ");
			end = value.indexOf(",");
			String user = unescapeString(value.substring(0, end));

			value = eat(value.substring(end), ", ");
			end = value.indexOf(")");
			String sentiment = unescapeString(value.substring(0, end));

			recentTweets.add(new RecentTweet(tweet, tweetCount, user, sentiment));

			value = value.substring(end + 1);
			if (value.startsWith(", "))
				value = eat(value, ", ");
		}
		
		value = eat(value, "], endTimeSecs: ");
		end = value.indexOf("}");
		long endTimeSecs = Long.parseLong(value.substring(0, end));

		return new TwitterDemoTopicRecord(topic, topicEnglish, countMonthly,
				countDaily, countPer6hrs, countPerHour, recentTweets, endTimeSecs);
	}
	
	private static void addInPlace(List<Integer> accumulatedSum,
			List<Integer> newAddend) {
		for (int i = 0; i < newAddend.size(); ++i) {
			if (i >= accumulatedSum.size()) {
				accumulatedSum.add(newAddend.get(i));
			} else {
				accumulatedSum.set(i, accumulatedSum.get(i) + newAddend.get(i));
			}
		}
	}

	private static void addRecentTweetInPlace(
			LinkedList<RecentTweet> accumulatedTweets,
			RecentTweet newTweet) {
		ListIterator<RecentTweet> i = accumulatedTweets.listIterator();
		int size = 0;
		while (true) {
			if (i.hasNext()) {
				RecentTweet next = i.next();
				++size;

				if (next.getTime() <= newTweet.getTime()) {
					// Insert the new tweet...
					i.previous();
					i.add(newTweet);
					++size;
					i.next();

					// ... and trim the list to 10 elements
					while (i.hasNext() && size < 10) {
						i.next();
						++size;
					}
					while (i.hasNext()) {
						i.next();
						i.remove();
					}

					return;
				}
			} else {
				i.add(newTweet);
				return;
			}
		}
	}

	private static void addRecentTweetsInPlace(
			LinkedList<RecentTweet> accumulatedTweets,
			List<RecentTweet> newTweets) {
		for (RecentTweet newTweet : newTweets) {
			addRecentTweetInPlace(accumulatedTweets, newTweet);
		}
	}

	/**
	 * Combine two records, summing all counts, and keeping the 10 most recent
	 * tweets. Tags must match for records to be combined.
	 */
	public static TwitterDemoTopicRecord addRecords(
			TwitterDemoTopicRecord... records) {
		if (null == records || 0 == records.length)
			return null;

		String topic = records[0]._topic;
		String topicEnglish = records[0]._topicEnglish;
		int countMonthly = records[0]._countMonthly;
		List<Integer> countDaily = new ArrayList<>(records[0]._countDaily);
		List<Integer> countPer6hrs = new ArrayList<>(records[0]._countPer6hrs);
		List<Integer> countPerHour = new ArrayList<>(records[0]._countPerHour);
		LinkedList<RecentTweet> recentTweets = new LinkedList<>(
				records[0]._recentTweets);
		long endTimeSecs = records[0]._endTimeSecs;

		for (int i = 1; i < records.length; ++i) {
			if (!objectsEqual(topic, records[i]._topic) || (endTimeSecs != records[i]._endTimeSecs))
				throw new IllegalArgumentException(
						"Cannot add twitter records for different topics or end times");

			countMonthly += records[i]._countMonthly;
			addInPlace(countDaily, records[i]._countDaily);
			addInPlace(countPer6hrs, records[i]._countPer6hrs);
			addInPlace(countPerHour, records[i]._countPerHour);
			addRecentTweetsInPlace(recentTweets, records[i]._recentTweets);
		}
		return new TwitterDemoTopicRecord(topic, topicEnglish, countMonthly,
				countDaily, countPer6hrs, countPerHour, recentTweets, endTimeSecs);
	}

	/**
	 * Add a tweet to an existing record, summing counts as needed, and keeping
	 * the 10 most recent tweets. endTimeSecs variable is used to determine if
	 * the newTweet is within the valid time window (i.e., between endTime to
	 * endTime - 1 month)
	 */
	public static TwitterDemoTopicRecord addTweetToRecord(
			TwitterDemoTopicRecord record, RecentTweet newTweet) {
		if (null == record)
			return null;
		
		String topic = record._topic;
		String topicEnglish = record._topicEnglish;
		int countMonthly = record._countMonthly;
		List<Integer> countDaily = new ArrayList<>(record._countDaily);
		List<Integer> countPer6hrs = new ArrayList<>(record._countPer6hrs);
		List<Integer> countPerHour = new ArrayList<>(record._countPerHour);

		LinkedList<RecentTweet> recentTweets = new LinkedList<>(
				record._recentTweets);
		long endTimeSecs = record._endTimeSecs;	
	
		float secsSinceEnd = (float)(endTimeSecs - newTweet.getTime());	// time interval between new tweet and endTime
		
		if (secsSinceEnd > 2.6784e6) { // 2678400 = 31*24*60*60
			// more than 1 month ago disregard this new tweet
			return new TwitterDemoTopicRecord(topic, topicEnglish,
					countMonthly, countDaily, countPer6hrs, countPerHour,
					recentTweets, endTimeSecs);
		} else if (secsSinceEnd <= 0) {
			// before endTime so disregard this new tweet
			return new TwitterDemoTopicRecord(topic, topicEnglish,
					countMonthly, countDaily, countPer6hrs, countPerHour,
					recentTweets, endTimeSecs);
		} else {
			// new tweet occurred within 1 month from endtime
			countMonthly++;
			addRecentTweetInPlace(recentTweets, newTweet);
			int hours = (int)(secsSinceEnd * 2.7778e-4);	//1/3600
			int quarterDays = (int)(secsSinceEnd * 4.6296e-5); // 1/21600 = 1/60*60*6;
			int days = (int)(secsSinceEnd * 1.1574e-5); //1/86400 = 1/60*60*24;

			if ((hours >= 0) && (hours < NUM_HOURS)) {
				countPerHour.set(hours, countPerHour.get(hours) + 1);
			}
			if ((quarterDays >= 0) && (quarterDays < NUM_QUARTERDAYS)) {
				countPer6hrs.set(quarterDays, countPer6hrs.get(quarterDays) + 1);
			}
			if ((days >= 0) && (days < NUM_DAYS)) {
				countDaily.set(days, countDaily.get(days) + 1);
			}
		}
		return new TwitterDemoTopicRecord(topic, topicEnglish, countMonthly,
				countDaily, countPer6hrs, countPerHour, recentTweets, endTimeSecs);		
	}

	private static void minInPlace(List<Integer> accumulatedMin,
			List<Integer> newMin) {
		for (int i = 0; i < newMin.size(); ++i) {
			if (i >= accumulatedMin.size()) {
				accumulatedMin.add(newMin.get(i));
			} else {
				accumulatedMin.set(i,
						Math.min(accumulatedMin.get(i), newMin.get(i)));
			}
		}
	}

	/**
	 * Get minimums of all counts across some number of records.
	 */
	public static TwitterDemoTopicRecord minOfRecords(
			TwitterDemoTopicRecord... records) {
		if (null == records || 0 == records.length)
			return null;

		int minCount = Integer.MAX_VALUE;
		List<Integer> minCountDaily = new ArrayList<>();
		List<Integer> minCountPer6hrs = new ArrayList<>();
		List<Integer> minCountPerHour = new ArrayList<>();
		long minEndTime = Long.MAX_VALUE;

		for (TwitterDemoTopicRecord record : records) {
			if (null != record) {
				minCount = Math.min(minCount, record._countMonthly);
				minInPlace(minCountDaily, record._countDaily);
				minInPlace(minCountPer6hrs, record._countPer6hrs);
				minInPlace(minCountPerHour, record._countPerHour);
				minEndTime = Math.min(minEndTime, record._endTimeSecs);
			}
		}
		return new TwitterDemoTopicRecord(null, null, minCount, minCountDaily,
				minCountPer6hrs, minCountPerHour,
				new ArrayList<RecentTweet>(), minEndTime);
	}
	
	private static void maxInPlace(List<Integer> accumulatedMax,
			List<Integer> newMax) {
		for (int i = 0; i < newMax.size(); ++i) {
			if (i >= accumulatedMax.size()) {
				accumulatedMax.add(newMax.get(i));
			} else {
				accumulatedMax.set(i,
						Math.max(accumulatedMax.get(i), newMax.get(i)));
			}
		}
	}

	/**
	 * Get maximums of all counts across some number of records.
	 */
	public static TwitterDemoTopicRecord maxOfRecords(
			TwitterDemoTopicRecord... records) {
		int maxCount = Integer.MIN_VALUE;
		List<Integer> maxCountDaily = new ArrayList<>();
		List<Integer> maxCountPer6hrs = new ArrayList<>();
		List<Integer> maxCountPerHour = new ArrayList<>();
		long maxEndTime = Long.MIN_VALUE;

		for (TwitterDemoTopicRecord record : records) {
			maxCount = Math.max(maxCount, record._countMonthly);
			maxInPlace(maxCountDaily, record._countDaily);
			maxInPlace(maxCountPer6hrs, record._countPer6hrs);
			maxInPlace(maxCountPerHour, record._countPerHour);
			maxEndTime = Math.max(maxEndTime, record._endTimeSecs);
		}
		return new TwitterDemoTopicRecord(null, null, maxCount, maxCountDaily,
				maxCountPer6hrs, maxCountPerHour,
				new ArrayList<RecentTweet>(), maxEndTime);
	}

}
