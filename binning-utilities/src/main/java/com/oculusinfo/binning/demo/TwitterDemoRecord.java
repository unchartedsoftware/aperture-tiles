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



import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
                              int count, List<Integer> countBins,
                              int positive, List<Integer> positiveBins,
                              int neutral, List<Integer> neutralBins,
                              int negative, List<Integer> negativeBins,
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

    private static void addInPlace (List<Integer> accumulatedSum, List<Integer> newAddend) {
        for (int i=0; i<newAddend.size(); ++i) {
            if (i >= accumulatedSum.size()) {
                accumulatedSum.add(newAddend.get(i));
            } else {
                accumulatedSum.set(i, accumulatedSum.get(i) + newAddend.get(i));
            }
        }
    }

    private static void addRecentTweetInPlace (LinkedList<Pair<String, Long>> accumulatedTweets,
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

    private static void addRecentTweetsInPlace (LinkedList<Pair<String, Long>> accumulatedTweets,
                                                List<Pair<String, Long>> newTweets) {
        for (Pair<String, Long> newTweet: newTweets) {
            addRecentTweetInPlace(accumulatedTweets, newTweet);
        }
    }

    /**
     * Combine two records, summing all counts, and keeping the 10 most recent
     * tweets. Tags must match for records to be combined.
     */
    public static TwitterDemoRecord addRecords (TwitterDemoRecord... records) {
        if (null == records || 0 == records.length)
            return null;

        String tag = records[0]._tag;
        int count = records[0]._count;
        int positive = records[0]._positive;
        int neutral = records[0]._neutral;
        int negative = records[0]._negative;
        List<Integer> countBins = new ArrayList<>(records[0]._countBins);
        List<Integer> positiveBins = new ArrayList<>(records[0]._positiveBins);
        List<Integer> neutralBins = new ArrayList<>(records[0]._neutralBins);
        List<Integer> negativeBins = new ArrayList<>(records[0]._negativeBins);
        LinkedList<Pair<String, Long>> recentTweets = new LinkedList<>(records[0]._recentTweets);

        for (int i=1; i<records.length; ++i) {
            if (!objectsEqual(tag,  records[i]._tag))
                throw new IllegalArgumentException("Cannot add twitter records for different tags");

            count += records[i]._count;
            positive += records[i]._positive;
            neutral += records[i]._neutral;
            negative += records[i]._negative;
            addInPlace(countBins, records[i]._countBins);
            addInPlace(positiveBins, records[i]._positiveBins);
            addInPlace(neutralBins, records[i]._neutralBins);
            addInPlace(negativeBins, records[i]._negativeBins);
            addRecentTweetsInPlace(recentTweets, records[i]._recentTweets);
        }
        return new TwitterDemoRecord(tag,
                                     count, countBins,
                                     positive, positiveBins,
                                     neutral, neutralBins,
                                     negative, negativeBins,
                                     recentTweets);
    }

    

    private static void minInPlace (List<Integer> accumulatedMin, List<Integer> newMin) {
        for (int i=0; i < newMin.size(); ++i) {
            if (i >= accumulatedMin.size()) {
                accumulatedMin.add(newMin.get(i));
            } else {
                accumulatedMin.set(i, Math.min(accumulatedMin.get(i), newMin.get(i)));
            }
        }
    }

    /**
     * Get minimums of all counts across some number of records.
     * @param that
     * @return
     */
    public static TwitterDemoRecord minOfRecords (TwitterDemoRecord... records) {
        if (null == records || 0 == records.length)
            return null;

        int minCount = Integer.MAX_VALUE;
        int minPositive = Integer.MAX_VALUE;
        int minNeutral = Integer.MAX_VALUE;
        int minNegative = Integer.MAX_VALUE;
        List<Integer> minCountBins = new ArrayList<>();
        List<Integer> minPositiveBins = new ArrayList<>();
        List<Integer> minNeutralBins = new ArrayList<>();
        List<Integer> minNegativeBins = new ArrayList<>();
        for (TwitterDemoRecord record: records) {
            minCount = Math.min(minCount, record._count);
            minPositive = Math.min(minPositive, record._positive);
            minNeutral = Math.min(minNeutral, record._neutral);
            minNegative = Math.min(minNegative, record._negative);
            minInPlace(minCountBins, record._countBins);
            minInPlace(minPositiveBins, record._positiveBins);
            minInPlace(minNeutralBins, record._neutralBins);
            minInPlace(minNegativeBins, record._negativeBins);
        }
        return new TwitterDemoRecord(null,
                                     minCount, minCountBins,
                                     minPositive, minPositiveBins,
                                     minNeutral, minNeutralBins,
                                     minNegative, minNegativeBins,
                                     new ArrayList<Pair<String, Long>>());
    }

    

    private static void maxInPlace (List<Integer> accumulatedMax, List<Integer> newMax) {
        for (int i=0; i < newMax.size(); ++i) {
            if (i >= accumulatedMax.size()) {
                accumulatedMax.add(newMax.get(i));
            } else {
                accumulatedMax.set(i, Math.max(accumulatedMax.get(i), newMax.get(i)));
            }
        }
    }

    /**
     * Get maximums of all counts across some number of records.
     * @param that
     * @return
     */
    public static TwitterDemoRecord maxOfRecords (TwitterDemoRecord... records) {
        int maxCount = Integer.MIN_VALUE;
        int maxPositive = Integer.MIN_VALUE;
        int maxNeutral = Integer.MIN_VALUE;
        int maxNegative = Integer.MIN_VALUE;
        List<Integer> maxCountBins = new ArrayList<>();
        List<Integer> maxPositiveBins = new ArrayList<>();
        List<Integer> maxNeutralBins = new ArrayList<>();
        List<Integer> maxNegativeBins = new ArrayList<>();
        for (TwitterDemoRecord record: records) {
            maxCount = Math.max(maxCount, record._count);
            maxPositive = Math.max(maxPositive, record._positive);
            maxNeutral = Math.max(maxNeutral, record._neutral);
            maxNegative = Math.max(maxNegative, record._negative);
            maxInPlace(maxCountBins, record._countBins);
            maxInPlace(maxPositiveBins, record._positiveBins);
            maxInPlace(maxNeutralBins, record._neutralBins);
            maxInPlace(maxNegativeBins, record._negativeBins);
        }
        return new TwitterDemoRecord(null,
                                     maxCount, maxCountBins,
                                     maxPositive, maxPositiveBins,
                                     maxNeutral, maxNeutralBins,
                                     maxNegative, maxNegativeBins,
                                     new ArrayList<Pair<String, Long>>());
    }
}
