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

import java.util.List;

import com.oculusinfo.binning.util.Pair;

import org.junit.Assert;
import org.junit.Test;

public class TwitterTopicTests {
//    @Test
//    public void exampleTest () {
//        Assert.assertEquals(1, 1);
//    }
	
	@Test
	public void testEmptyRecord() {
		TwitterDemoTopicRecord a = new TwitterDemoTopicRecord();

		Assert.assertEquals(null, a.getTopic());
		Assert.assertEquals(null, a.getTopicEnglish());
		Assert.assertEquals(0, a.getCountMonthly());
		Assert.assertEquals(null, a.getCountDaily());
		Assert.assertEquals(null, a.getCountPer6hrs());
		Assert.assertEquals(null, a.getCountPerHour());
		Assert.assertEquals(null, a.getRecentTweets());
	}
	
    
	//TODO ...
    // Needed Tests:
    //  Create a topic with no counts and a given end time (test)
    //  Add a tweet to it in the following time ranges:
    //    Before the begining of its valid time range (test)
    //    At the bounds of the time range where it should register on the monthly by day list, but not on the other two (test)
    //    At the bounds of the time range where it should register on both the monthly by day, and the weekly by quarter day lists, but not on the other (test)
    //    At the bounds of the time range where it should register on all three lists (test)
    //    After the last valid time range (test)
    // In all cases, make sure all counts are updated or left alone appropriately.
    // Keep test cases small, clean, and obvious (as much as is reasonably possible, anyway)
	    
}
