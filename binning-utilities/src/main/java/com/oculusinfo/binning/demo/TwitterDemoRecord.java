/**
 * Copyright (c) 2013 Oculus Info Inc. http://www.oculusinfo.com/
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



import java.util.List;

import com.oculusinfo.binning.io.Pair;



public class TwitterDemoRecord {
    private String                   _tag;
    private int                      _count;
    private List<Integer>            _countBins;
    private int                      _positive;
    private List<Integer>            _positiveBins;
    private int                      _neutral;
    private List<Integer>            _neutralBins;
    private int                      _negative;
    private List<Integer>            _negativeBins;
    private List<Pair<String, Long>> _recentTweets;



    public TwitterDemoRecord (String tag,
                              Integer count, List<Integer> countBins,
                              Integer positive, List<Integer> positiveBins,
                              Integer neutral, List<Integer> neutralBins,
                              Integer negative, List<Integer> negativeBins,
                              List<Pair<String, Long>> recent) {
        _tag = tag;
        _count = count;
        _countBins = countBins;
        _positive = positive;
        _positiveBins = positiveBins;
        _neutral = neutral;
        _neutralBins = neutralBins;
        _negative = negative;
        _negativeBins = negativeBins;
        _recentTweets = recent;
    }

    public String getTag () {
        return _tag;
    }

    public int getCount () {
        return _count;
    }

    public List<Integer> getCountBins () {
        return _countBins;
    }

    public int getPositiveCount () {
        return _positive;
    }

    public List<Integer> getPositiveCountBins () {
        return _positiveBins;
    }

    public int getNeutralCount () {
        return _neutral;
    }

    public List<Integer> getNeutralCountBins () {
        return _neutralBins;
    }

    public int getNegativeCount () {
        return _negative;
    }

    public List<Integer> getNegativeCountBins () {
        return _negativeBins;
    }

    public List<Pair<String, Long>> getRecentTweets () {
        return _recentTweets;
    }



    private int getHash (Object obj) {
        if (null == obj) return 0;
        return obj.hashCode();
    }
    @Override
    public int hashCode () {
        return (((((((((getHash(_tag) * 2
                + _count) * 3
                + getHash(_countBins)) * 5
                + _positive) * 7
                + getHash(_positiveBins)) * 11
                + _neutral) * 13
                + getHash(_neutralBins)) * 17
                + _negative) * 19
                + getHash(_negativeBins)) * 21
                + getHash(_recentTweets));
    }

    @Override
    public boolean equals (Object obj) {
        if (this == obj) return true;
        if (null == obj) return false;
        if (!(obj instanceof TwitterDemoRecord)) return false;
        
        TwitterDemoRecord that = (TwitterDemoRecord) obj;
        return (objectsEqual(this._tag, that._tag) &&
                this._count == that._count &&
                listsEqual(this._countBins, that._countBins) &&
                this._positive == that._positive &&
                listsEqual(this._positiveBins, that._positiveBins) &&
                this._neutral == that._neutral &&
                listsEqual(this._neutralBins, that._neutralBins) &&
                this._negative == that._negative &&
                listsEqual(this._negativeBins, that._negativeBins) &&
                listsEqual(this._recentTweets, that._recentTweets));
    }

    private static boolean objectsEqual (Object a, Object b) {
        if (null == a) return null == b;
        return a.equals(b);
    }
    private static <T> boolean listsEqual (List<T> a, List<T> b) {
        if (null == a) return null == b;
        if (null == b) return false;
        if (a.size() != b.size()) return false;
        for (int i=0; i<a.size(); ++i) {
            if (!objectsEqual(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private <T> String mkString (List<T> list, String separator) {
        String result = "";
        for (int i=0; i<list.size(); ++i) {
            if (i>0) result = result + separator;
            result = result + list.get(i).toString();
        }
        return result;
    }
    @Override
    public String toString () {
        return ("{tag: \"" + _tag + "\", "+
                "count: "    + _count    + ", countList: ["    + mkString(_countBins,    ", ") + "], " +
                "positive: " + _positive + ", positiveList: [" + mkString(_positiveBins, ", ") + "], " +
                "neutral: "  + _neutral  + ", neutralList: ["  + mkString(_neutralBins,  ", ") + "], " +
                "negative: " + _negative + ", negativeList: [" + mkString(_negativeBins, ", ") + "], " +
                "recent: [" + mkString(_recentTweets, ", ")+"]}");
    }
}
