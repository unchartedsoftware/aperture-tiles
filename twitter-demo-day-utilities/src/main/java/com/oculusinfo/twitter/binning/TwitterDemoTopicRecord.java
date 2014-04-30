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

import com.oculusinfo.binning.util.Pair;

public class TwitterDemoTopicRecord implements Serializable {
	private static final long serialVersionUID = 1L;	//NOTE:  using default serialVersion ID
	
	private static final int NUM_DAYS = 31;			// days per month
	private static final int NUM_QUARTERDAYS = 28;	// quarter days per week
	private static final int NUM_HOURS = 24;		// hours per day	

	private String _topic; 							// Twitter topic in original language
	private String _topicEnglish; 					// Twitter topic in English
	private int _countMonthly; 						// total number of tweets per month with this topic
	private int[] _countDaily; 						// tweet count per day for the past month with this topic
	private int[] _countPer6hrs; 					// tweet count per six hours for last week with this topic
	private int[] _countPerHour; 					// tweet count per hour for last 24 hrs with this topic
	private List<Pair<String, Long>> _recentTweets; // 10 most recent tweets with this topic
	private long _endTimeSecs;						// end time (in secs) for this data record (so valid time window
													//    between endTimeSecs and endTimeSecs - 1 month

	public TwitterDemoTopicRecord(String topic, String topicEnglish,
			int countMonthly, int[] countDaily,
			int[] countPer6hrs, int[] countPerHour,
			List<Pair<String, Long>> recentTweets, long endTimeSecs) {
		
		_topic = topic;
		_topicEnglish = topicEnglish;
		_countMonthly = countMonthly;
		_recentTweets = recentTweets;
		_endTimeSecs = endTimeSecs;
		
		if (countDaily.length > NUM_DAYS) {
			throw new IllegalArgumentException("countDaily size cannot be > " + NUM_DAYS);
		}
		else {
			_countDaily = new int[NUM_DAYS];
			System.arraycopy(countDaily,0,_countDaily,0,_countDaily.length);
		}
		
		if (countPer6hrs.length > NUM_QUARTERDAYS) {
			throw new IllegalArgumentException("countPer6hrs size cannot be > " + NUM_QUARTERDAYS);
		}
		else {
			_countPer6hrs = new int[NUM_QUARTERDAYS];
			System.arraycopy(countPer6hrs,0,_countPer6hrs,0,_countPer6hrs.length);
		}
		
		if (countPerHour.length > NUM_HOURS) {
			throw new IllegalArgumentException("countPerHour size cannot be > " + NUM_HOURS);
		}
		else {
			_countPerHour = new int[NUM_HOURS];
			System.arraycopy(countPerHour,0,_countPerHour,0,_countPerHour.length);
		}
	}
	
	// Secondary constructor (for adding 1 new tweet to an empty record)
	public TwitterDemoTopicRecord(String topic, String topicEnglish,
			List<Pair<String, Long>> newTweet, long endTimeSecs) {
		
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
		float secsSinceEnd = (float)(endTimeSecs - newTweet.get(0).getSecond());	

		if (secsSinceEnd > 2.6784e6) { // 2678400 = 31*24*60*60
			// more than 1 month ago disregard this new tweet

		} else if (secsSinceEnd <= 0) {
			// before endTime so disregard this new tweet

		} else {
			// new tweet occurred within 1 month from endtime
			_topic = topic;
			_topicEnglish = topicEnglish;
			_countMonthly++;
			_recentTweets = newTweet;
			_endTimeSecs = endTimeSecs;
			_countDaily = new int[NUM_DAYS];
			_countPer6hrs = new int[NUM_QUARTERDAYS];
			_countPerHour = new int[NUM_HOURS];
			
			int hours = (int)(secsSinceEnd * 2.7778e-4);	//1/3600
			int quarterDays = (int)(secsSinceEnd * 4.6296e-5); // 1/21600 = 1/60*60*6;
			int days = (int)(secsSinceEnd * 1.1574e-5); //1/86400 = 1/60*60*24;

			if ((hours >= 0) && (hours < NUM_HOURS)) {
				//assert (countPerHour.size() == NUM_HOURS);
				_countPerHour[hours]++;
			}
			if ((quarterDays >= 0) && (quarterDays < NUM_QUARTERDAYS)) {
				//assert (countPer6hrs.size() == NUM_QUARTERDAYS);
				_countPer6hrs[quarterDays]++;
			}
			if ((days >= 0) && (days < NUM_DAYS)) {
				//assert (countDaily.size() == NUM_DAYS);
				_countDaily[days]++;
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

	public int[] getCountDaily() {
		return _countDaily;
	}

	public int[] getCountPer6hrs() {
		return _countPer6hrs;
	}

	public int[] getCountPerHour() {
		return _countPerHour;
	}

	public List<Pair<String, Long>> getRecentTweets() {
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
		return (this._topic == that._topic
				&& this._topicEnglish == that._topicEnglish
				&& this._countMonthly == that._countMonthly
				&& this._endTimeSecs == that._endTimeSecs
				&& arraysEqual(this._countDaily, that._countDaily)
				&& arraysEqual(this._countPer6hrs, that._countPer6hrs)
				&& arraysEqual(this._countPerHour, that._countPerHour)
				&& listsEqual(this._recentTweets, that._recentTweets));
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
	
	private static <T> boolean arraysEqual(int[] a, int[] b) {
		if (null == a)
			return null == b;
		if (null == b)
			return false;
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}	

//	private <T> String mkString(List<T> list, String separator) {
//		String result = "";
//		for (int i = 0; i < list.size(); ++i) {
//			if (i > 0)
//				result = result + separator;
//			result = result + list.get(i).toString();
//		}
//		return result;
//	}
	
	private <T> String mkStringFromArray(int[] array, String separator) {
		String result = "";
		for (int i = 0; i < array.length; ++i) {
			if (i > 0)
				result = result + separator;
			result = result + array[i];
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
				+ mkStringFromArray(_countDaily, ", ") + "], " + "countPer6hrs: ["
				+ mkStringFromArray(_countPer6hrs, ", ") + "], " + "countPerHour: ["
				+ mkStringFromArray(_countPerHour, ", ") + "], " + "recent: [");
		for (int i = 0; i < _recentTweets.size(); ++i) {
			Pair<String, Long> rt = _recentTweets.get(i);
			if (i > 0)
				result += ", ";
			result += "(" + escapeString(rt.getFirst()) + ", " + rt.getSecond()
					+ ")";
		}
		result += "], endTimeSecs: " + _endTimeSecs + "}";
		return result;
	}

//	private static String eatIntList(String from, List<Integer> result) {
//		int nextComma = from.indexOf(",");
//		int nextBracket = from.indexOf("]");
//		while (nextComma > 0 && nextComma < nextBracket) {
//			result.add(Integer.parseInt(from.substring(0, nextComma)));
//			from = from.substring(nextComma + 2);
//			nextComma = from.indexOf(",");
//			nextBracket = from.indexOf("]");
//		}
//		if (nextBracket > 0)
//			result.add(Integer.parseInt(from.substring(0, nextBracket)));
//		return from.substring(nextBracket);
//	}

	private static String eatIntArray(String from, int[] result) {
		int nextComma = from.indexOf(",");
		int nextBracket = from.indexOf("]");
		
		//String[] = from.substring(0, nextBracket).split(", ").;
		int n = 0;
		while (nextComma > 0 && nextComma < nextBracket) {
			if (n < result.length) {
				result[n] = Integer.parseInt(from.substring(0, nextComma));
			}
			n++;
			from = from.substring(nextComma + 2);
			nextComma = from.indexOf(",");
			nextBracket = from.indexOf("]");
		}
		if (nextBracket > 0)
			if (n < result.length) {
				result[n] = Integer.parseInt(from.substring(0, Math.min(nextComma, nextBracket)));
			}
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
		int[] countDaily = new int[NUM_DAYS];
		value = eatIntArray(value, countDaily);

		value = eat(value, "], countPer6hrs: [");
		int[] countPer6hrs = new int[NUM_QUARTERDAYS];
		value = eatIntArray(value, countPer6hrs);

		value = eat(value, "], countPerHour: [");
		int[] countPerHour = new int[NUM_HOURS];
		value = eatIntArray(value, countPerHour);

		value = eat(value, "], recent: [");
		List<Pair<String, Long>> recentTweets = new ArrayList<>();
		while (value.startsWith("(")) {
			value = eat(value, "(");
			end = getQuotedStringEnd(value);
			String tweet = unescapeString(value.substring(0, end));

			value = eat(value.substring(end), ", ");
			end = value.indexOf(")");
			long tweetCount = Long.parseLong(value.substring(0, end));

			recentTweets.add(new Pair<String, Long>(tweet, tweetCount));

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

	private static void addArrayInPlace(int[] summed,
			int[] newdata) {
		assert(summed.length == newdata.length);
		for (int i = 0; i < summed.length; ++i) {
			summed[i] += newdata[i];
		}
	}

	private static void addRecentTweetInPlace(
			LinkedList<Pair<String, Long>> accumulatedTweets,
			Pair<String, Long> newTweet) {
		ListIterator<Pair<String, Long>> i = accumulatedTweets.listIterator();
		int size = 0;
		while (true) {
			if (i.hasNext()) {
				Pair<String, Long> next = i.next();
				++size;

				if (next.getSecond() <= newTweet.getSecond()) {
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
			LinkedList<Pair<String, Long>> accumulatedTweets,
			List<Pair<String, Long>> newTweets) {
		for (Pair<String, Long> newTweet : newTweets) {
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
		int[] countDaily = new int[NUM_DAYS];
		System.arraycopy(records[0]._countDaily,0,countDaily,0,records[0]._countDaily.length);
		int[] countPer6hrs = new int[NUM_QUARTERDAYS];
		System.arraycopy(records[0]._countPer6hrs,0,countPer6hrs,0,records[0]._countPer6hrs.length);
		int[] countPerHour = new int[NUM_HOURS];
		System.arraycopy(records[0]._countPerHour,0,countPerHour,0,records[0]._countPerHour.length);

		LinkedList<Pair<String, Long>> recentTweets = new LinkedList<>(
				records[0]._recentTweets);
		long endTimeSecs = records[0]._endTimeSecs;

		for (int i = 1; i < records.length; ++i) {
			if (!objectsEqual(topic, records[i]._topic) || (endTimeSecs != records[i]._endTimeSecs))
				throw new IllegalArgumentException(
						"Cannot add twitter records for different topics or end times");

			countMonthly += records[i]._countMonthly;
			addArrayInPlace(countDaily, records[i]._countDaily);
			addArrayInPlace(countPer6hrs, records[i]._countPer6hrs);
			addArrayInPlace(countPerHour, records[i]._countPerHour);
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
			TwitterDemoTopicRecord record, Pair<String, Long> newTweet) {
		if (null == record)
			return null;
		
		String topic = record._topic;
		String topicEnglish = record._topicEnglish;
		int countMonthly = record._countMonthly;
		int[] countDaily = new int[NUM_DAYS];
		System.arraycopy(record._countDaily,0,countDaily,0,record._countDaily.length);
		int[] countPer6hrs = new int[NUM_QUARTERDAYS];
		System.arraycopy(record._countPer6hrs,0,countPer6hrs,0,record._countPer6hrs.length);
		int[] countPerHour = new int[NUM_HOURS];
		System.arraycopy(record._countPerHour,0,countPerHour,0,record._countPerHour.length);

		LinkedList<Pair<String, Long>> recentTweets = new LinkedList<>(
				record._recentTweets);
		long endTimeSecs = record._endTimeSecs;	
	
		float secsSinceEnd = (float)(endTimeSecs - newTweet.getSecond());	// time interval between new tweet and endTime
		
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
				//assert (countPerHour.size() == NUM_HOURS);
				countPerHour[hours]++;
			}
			if ((quarterDays >= 0) && (quarterDays < NUM_QUARTERDAYS)) {
				//assert (countPer6hrs.size() == NUM_QUARTERDAYS);
				countPer6hrs[quarterDays]++;
			}
			if ((days >= 0) && (days < NUM_DAYS)) {
				//assert (countDaily.size() == NUM_DAYS);
				countDaily[days]++;
			}
		}
		return new TwitterDemoTopicRecord(topic, topicEnglish, countMonthly,
				countDaily, countPer6hrs, countPerHour, recentTweets, endTimeSecs);		
	}

	private static TwitterDemoTopicRecord addTweetsToRecord(
			TwitterDemoTopicRecord record, List<Pair<String, Long>> newTweets) {

		for (Pair<String, Long> newTweet : newTweets) {
			record = addTweetToRecord(record, newTweet);
		}
		return record;
	}

	private static void minInPlace(int[] accumulatedMin,
			int[] newMin) {
		//assert(accumulatedMin.length == newMin.length);
		for (int i = 0; i < newMin.length; ++i) {
			accumulatedMin[i] = Math.min(accumulatedMin[i],newMin[i]);
		}
	}

	/**
	 * Get minimums of all counts across some number of records.
	 * 
	 * @param that
	 * @return
	 */
	public static TwitterDemoTopicRecord minOfRecords(
			TwitterDemoTopicRecord... records) {
		if (null == records || 0 == records.length)
			return null;

		int minCount = Integer.MAX_VALUE;
		//int[] minCountDaily = new int[NUM_DAYS];
		//int[] minCountPer6hrs = new int[NUM_QUARTERDAYS];
		//int[] minCountPerHour = new int[NUM_HOURS];
		int[] minCountDaily = records[0]._countDaily.clone();
		int[] minCountPer6hrs = records[0]._countPer6hrs.clone();
		int[] minCountPerHour = records[0]._countPerHour.clone();
		//minCountDaily
		//System.arraycopy(countDaily,0,_countDaily,0,_countDaily.length);
		for (TwitterDemoTopicRecord record : records) {
			if (null != record) {
				minCount = Math.min(minCount, record._countMonthly);
				minInPlace(minCountDaily, record._countDaily);
				minInPlace(minCountPer6hrs, record._countPer6hrs);
				minInPlace(minCountPerHour, record._countPerHour);
			}
		}
		return new TwitterDemoTopicRecord(null, null, minCount, minCountDaily,
				minCountPer6hrs, minCountPerHour,
				new ArrayList<Pair<String, Long>>(), 0);
	}
	
	private static void maxInPlace(int[] accumulatedMax,
			int[] newMax) {
		//assert(accumulatedMax.length == newMax.length);
		for (int i = 0; i < newMax.length; ++i) {
			accumulatedMax[i] = Math.max(accumulatedMax[i],newMax[i]);
		}
	}

	/**
	 * Get maximums of all counts across some number of records.
	 * 
	 * @param that
	 * @return
	 */
	public static TwitterDemoTopicRecord maxOfRecords(
			TwitterDemoTopicRecord... records) {
		int maxCount = Integer.MIN_VALUE;
		int[] maxCountDaily = new int[NUM_DAYS];
		int[] maxCountPer6hrs = new int[NUM_QUARTERDAYS];
		int[] maxCountPerHour = new int[NUM_HOURS];
		for (TwitterDemoTopicRecord record : records) {
			maxCount = Math.max(maxCount, record._countMonthly);
			maxInPlace(maxCountDaily, record._countDaily);
			maxInPlace(maxCountPer6hrs, record._countPer6hrs);
			maxInPlace(maxCountPerHour, record._countPerHour);
		}
		return new TwitterDemoTopicRecord(null, null, maxCount, maxCountDaily,
				maxCountPer6hrs, maxCountPerHour,
				new ArrayList<Pair<String, Long>>(), 0);
	}

}
