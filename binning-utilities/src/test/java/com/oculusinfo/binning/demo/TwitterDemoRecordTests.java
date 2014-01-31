/**
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
package com.oculusinfo.binning.demo;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.io.Pair;

public class TwitterDemoRecordTests {
    @Test
    public void testRecordAggregation () {
        TwitterDemoRecord a = new TwitterDemoRecord("abc",
                                                    3, Arrays.asList(5, 0, 0, 0),
                                                    7, Arrays.asList(0, 11, 0, 0),
                                                    13, Arrays.asList(0, 0, 17, 0),
                                                    19, Arrays.asList(0, 0, 0, 23),
                                                    Arrays.asList(new Pair<String, Long>("recent 11", 11l),
                                                                  new Pair<String, Long>("recent 5", 5l),
                                                                  new Pair<String, Long>("recent 4", 4l),
                                                                  new Pair<String, Long>("recent 3", 3l),
                                                                  new Pair<String, Long>("recent 2", 2l),
                                                                  new Pair<String, Long>("recent 1", 1l)));
        TwitterDemoRecord b = new TwitterDemoRecord("abc",
                                                    29, Arrays.asList(0, 0, 0, 31),
                                                    37, Arrays.asList(0, 0, 41, 0),
                                                    43, Arrays.asList(0, 47, 0, 0),
                                                    53, Arrays.asList(59, 0, 0, 0),
                                                    Arrays.asList(new Pair<String, Long>("recent 12", 12l),
                                                                  new Pair<String, Long>("recent 10", 10l),
                                                                  new Pair<String, Long>("recent 9", 9l),
                                                                  new Pair<String, Long>("recent 8", 8l),
                                                                  new Pair<String, Long>("recent 7", 7l),
                                                                  new Pair<String, Long>("recent 6", 6l)));

        TwitterDemoRecord c = new TwitterDemoRecord("abc",
                                                    32, Arrays.asList(5, 0, 0, 31),
                                                    44, Arrays.asList(0, 11, 41, 0),
                                                    56, Arrays.asList(0, 47, 17, 0),
                                                    72, Arrays.asList(59, 0, 0, 23),
                                                    Arrays.asList(new Pair<String, Long>("recent 12", 12l),
                                                                  new Pair<String, Long>("recent 11", 11l),
                                                                  new Pair<String, Long>("recent 10", 10l),
                                                                  new Pair<String, Long>("recent 9", 9l),
                                                                  new Pair<String, Long>("recent 8", 8l),
                                                                  new Pair<String, Long>("recent 7", 7l),
                                                                  new Pair<String, Long>("recent 6", 6l),
                                                                  new Pair<String, Long>("recent 5", 5l),
                                                                  new Pair<String, Long>("recent 4", 4l),
                                                                  new Pair<String, Long>("recent 3", 3l)));
        Assert.assertEquals(c, TwitterDemoRecord.addRecords(a, b));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testIllegalRecordAddition () {
        TwitterDemoRecord a = new TwitterDemoRecord("abc",
                                                    3, Arrays.asList(5, 0, 0, 0),
                                                    7, Arrays.asList(0, 11, 0, 0),
                                                    13, Arrays.asList(0, 0, 17, 0),
                                                    19, Arrays.asList(0, 0, 0, 23),
                                                    Arrays.asList(new Pair<String, Long>("recent 11", 11l),
                                                                  new Pair<String, Long>("recent 5", 5l),
                                                                  new Pair<String, Long>("recent 4", 4l),
                                                                  new Pair<String, Long>("recent 3", 3l),
                                                                  new Pair<String, Long>("recent 2", 2l),
                                                                  new Pair<String, Long>("recent 1", 1l)));
        TwitterDemoRecord b = new TwitterDemoRecord("def",
                                                    29, Arrays.asList(0, 0, 0, 31),
                                                    37, Arrays.asList(0, 0, 41, 0),
                                                    43, Arrays.asList(0, 47, 0, 0),
                                                    53, Arrays.asList(59, 0, 0, 0),
                                                    Arrays.asList(new Pair<String, Long>("recent 12", 12l),
                                                                  new Pair<String, Long>("recent 10", 10l),
                                                                  new Pair<String, Long>("recent 9", 9l),
                                                                  new Pair<String, Long>("recent 8", 8l),
                                                                  new Pair<String, Long>("recent 7", 7l),
                                                                  new Pair<String, Long>("recent 6", 6l)));
        TwitterDemoRecord.addRecords(a, b);
    }

    @Test
    public void testMin () {
        TwitterDemoRecord a = new TwitterDemoRecord("abc",
                                                    10, Arrays.asList(4, 3, 2, 1),
                                                    14, Arrays.asList(5, 4, 3, 2),
                                                    18, Arrays.asList(6, 5, 4, 3),
                                                    22, Arrays.asList(7, 6, 5, 4),
                                                    Arrays.asList(new Pair<String, Long>("recent 11", 11l),
                                                                  new Pair<String, Long>("recent 5", 5l),
                                                                  new Pair<String, Long>("recent 4", 4l),
                                                                  new Pair<String, Long>("recent 3", 3l),
                                                                  new Pair<String, Long>("recent 2", 2l),
                                                                  new Pair<String, Long>("recent 1", 1l)));
        TwitterDemoRecord b = new TwitterDemoRecord("def",
                                                    14, Arrays.asList(2, 3, 4, 5),
                                                    18, Arrays.asList(3, 4, 5, 6),
                                                    22, Arrays.asList(4, 5, 6, 7),
                                                    26, Arrays.asList(5, 6, 7, 8),
                                                    Arrays.asList(new Pair<String, Long>("recent 12", 12l),
                                                                  new Pair<String, Long>("recent 10", 10l),
                                                                  new Pair<String, Long>("recent 9", 9l),
                                                                  new Pair<String, Long>("recent 8", 8l),
                                                                  new Pair<String, Long>("recent 7", 7l),
                                                                  new Pair<String, Long>("recent 6", 6l)));

        TwitterDemoRecord c = new TwitterDemoRecord(null,
                                                    10, Arrays.asList(2, 3, 2, 1),
                                                    14, Arrays.asList(3, 4, 3, 2),
                                                    18, Arrays.asList(4, 5, 4, 3),
                                                    22, Arrays.asList(5, 6, 5, 4),
                                                    new ArrayList<Pair<String, Long>>());
        Assert.assertEquals(c, TwitterDemoRecord.minOfRecords(a, b));
    }

    @Test
    public void testMax () {
        TwitterDemoRecord a = new TwitterDemoRecord("abc",
                                                    10, Arrays.asList(4, 3, 2, 1),
                                                    14, Arrays.asList(5, 4, 3, 2),
                                                    18, Arrays.asList(6, 5, 4, 3),
                                                    22, Arrays.asList(7, 6, 5, 4),
                                                    Arrays.asList(new Pair<String, Long>("recent 11", 11l),
                                                                  new Pair<String, Long>("recent 5", 5l),
                                                                  new Pair<String, Long>("recent 4", 4l),
                                                                  new Pair<String, Long>("recent 3", 3l),
                                                                  new Pair<String, Long>("recent 2", 2l),
                                                                  new Pair<String, Long>("recent 1", 1l)));
        TwitterDemoRecord b = new TwitterDemoRecord("def",
                                                    14, Arrays.asList(2, 3, 4, 5),
                                                    18, Arrays.asList(3, 4, 5, 6),
                                                    22, Arrays.asList(4, 5, 6, 7),
                                                    26, Arrays.asList(5, 6, 7, 8),
                                                    Arrays.asList(new Pair<String, Long>("recent 12", 12l),
                                                                  new Pair<String, Long>("recent 10", 10l),
                                                                  new Pair<String, Long>("recent 9", 9l),
                                                                  new Pair<String, Long>("recent 8", 8l),
                                                                  new Pair<String, Long>("recent 7", 7l),
                                                                  new Pair<String, Long>("recent 6", 6l)));

        TwitterDemoRecord c = new TwitterDemoRecord(null,
                                                    14, Arrays.asList(4, 3, 4, 5),
                                                    18, Arrays.asList(5, 4, 5, 6),
                                                    22, Arrays.asList(6, 5, 6, 7),
                                                    26, Arrays.asList(7, 6, 7, 8),
                                                    new ArrayList<Pair<String, Long>>());
        Assert.assertEquals(c, TwitterDemoRecord.maxOfRecords(a, b));
    }
}
