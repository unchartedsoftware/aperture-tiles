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

	private List<String>            	_topics;			// Top 10 topics (for past month) in original language
    private List<String>            	_topicsEnglish;		// Top 10 topics in English
    private int							_totalTweets;		// Count of all twitter messages.
    private List<Integer>           	_countMonthly;		// total number of tweets per month for each Top 10 Topic
    private List<List<Integer>>			_countDaily;		// tweet count per day for the past month for each Top 10 Topic
    private List<List<Integer>>			_countPer6hrs;		// tweet count per six hours for last week for each Top 10 Topic
    private List<List<Integer>>			_countPerHour;		// tweet count per hour for last 24 hrs for each Top 10 Topic
    private List<Pair<String, Long>>	_recentTweets;		// 10 most recent tweets
    
    public TwitterDemoTopicRecord (List<String> topics,
    								List<String> topicsEnglish,		
    								int totalTweets,	
    								List<Integer> countMonthly,
    								List<List<Integer>> countDaily,
    								List<List<Integer>> countPer6hrs,		
    								List<List<Integer>> countPerHour,
    								List<Pair<String, Long>> recentTweets) {
	   _topics = topics;			
	   _topicsEnglish = topicsEnglish;		
	   _totalTweets = totalTweets;
	   _countMonthly = countMonthly;
	   _countDaily = countDaily;		
	   _countPer6hrs = countPer6hrs;		
	   _countPerHour = countPerHour;
	   _recentTweets = recentTweets;	
	}
    
	public List<String> getTopics() {
		return _topics;
	}
	
	public List<String> getTopicsEnglish() {
		return _topicsEnglish;
	}	

	public int getTotalTweets() {
		return _totalTweets;
	}

	public List<Integer> getCountMonthly() {
		return _countMonthly;
	}

	public List<List<Integer>> getCountDaily() {
		return _countDaily;
	}
	
	public List<List<Integer>> getCountPer6hrs() {
		return _countPer6hrs;
	}
	
	public List<List<Integer>> getCountPerHour() {
		return _countPerHour;
	}	

	public List<Pair<String, Long>> getRecentTweets() {
		return _recentTweets;
	}
	

	
	
}







