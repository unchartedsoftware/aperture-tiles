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

//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.LinkedList;
import java.util.List;
//import java.util.ListIterator;
import com.oculusinfo.binning.util.Pair;

public class TwitterDemoTopicRecord {
	//TODO do we need make this class serializable?? (as in TwitterDemoRecord class)

	private String            			_topic;				// Twitter topic in original language
    private String            			_topicEnglish;		// Twitter topic in English
    private int           				_countMonthly;		// total number of tweets per month with this topic
    private List<Integer>				_countDaily;		// tweet count per day for the past month with this topic
    private List<Integer>				_countPer6hrs;		// tweet count per six hours for last week with this topic
    private List<Integer>				_countPerHour;		// tweet count per hour for last 24 hrs with this topic
    private List<Pair<String, Long>>	_recentTweets;		// 10 most recent tweets with this topic
    
	public TwitterDemoTopicRecord() {	// default constructor
		   _topic = null;			
		   _topicEnglish = null;		
		   _countMonthly = 0;
		   _countDaily = null;		
		   _countPer6hrs = null;		
		   _countPerHour = null;
		   _recentTweets = null;		
	}
    
    public TwitterDemoTopicRecord (String topic,
    								String topicEnglish,			
    								int countMonthly,
    								List<Integer> countDaily,
    								List<Integer> countPer6hrs,		
    								List<Integer> countPerHour,
    								List<Pair<String, Long>> recentTweets) {
	   _topic = topic;			
	   _topicEnglish = topicEnglish;		
	   _countMonthly = countMonthly;
	   _countDaily = countDaily;		
	   _countPer6hrs = countPer6hrs;		
	   _countPerHour = countPerHour;
	   _recentTweets = recentTweets;	
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

	public List<Pair<String, Long>> getRecentTweets() {
		return _recentTweets;
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
				&& listsEqual(this._countDaily, that._countDaily)
				&& listsEqual(this._countPer6hrs, that._countPer6hrs)
				&& listsEqual(this._countPerHour, that._countPerHour)
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
}







