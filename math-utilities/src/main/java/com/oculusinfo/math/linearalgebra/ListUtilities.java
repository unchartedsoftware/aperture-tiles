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
package com.oculusinfo.math.linearalgebra;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple utilities to act on lists of objects.
 * 
 * @author nkronenfeld
 */
public class ListUtilities {
    private static boolean equal (double a, double b, double epsilon) {
        return Math.abs(a-b) < epsilon;
    }

    /**
     * Join together 2 ordered lists of doubles into a single, ordered list
     * without duplicates.
     *
     * @param A The first list
     * @param B The second list
     * @param epsilon The difference within which two doubles are considered
     *            equal
     * @return The conjoined list
     */
    public static List<Double> joinLists (List<Double> A, List<Double> B,
                                          double epsilon) {
        List<Double> C = new ArrayList<Double>();
        int nA = 0;
        int NA = A.size();
        double a = A.get(nA);

        int nB = 0;
        int NB = B.size();
        double b = B.get(nB);

        while (nA < NA || nB < NB) {
            if (equal(a, b, epsilon)) {
                // A bit silly, since they are already shown equal, but just in
                // case of large epsilon, it'd be nice to be sure
                C.add((a + b) / 2);
                while (nA < NA && equal(a, A.get(nA), epsilon)) ++nA;
                a = (nA < NA ? A.get(nA) : Double.MAX_VALUE);
                while (nB < NB && equal(b, B.get(nB), epsilon)) ++nB;
                b = (nB < NB ? B.get(nB) : Double.MAX_VALUE);
            } else if (a < b) {
                C.add(a);
                ++nA;
                a = (nA < NA ? A.get(nA) : Double.MAX_VALUE);
            } else {
                C.add(b);
                ++nB;
                b = (nB < NB ? B.get(nB) : Double.MAX_VALUE);
            }
        }

        return C;
    }

    /**
     * Join together N ordered lists of doubles into a single, ordered list
     * without duplicates
     * 
     * @param lists The lists to be joined
     * @param epsilon The difference within which two doubles are considered
     *            equal
     */
    public static List<Double> joinNLists (List<List<Double>> lists,
                                           double epsilon) {
        if (null == lists)
            return null;
        if (0 == lists.size())
            return null;
        if (1 == lists.size())
            return lists.get(0);

        while (lists.size() > 1) {
            // This looks like it's looping over every member, but each time,
            // it's really replacing the current and next list with a single,
            // new, joined list. So, in actuallity, it's looping over pairs,
            // because replacement happens durring the loop.
            for (int i = 0; i < lists.size() - 1; ++i) {
                List<Double> A = lists.remove(i);
                List<Double> B = lists.remove(i);
                lists.add(i, joinLists(A, B, epsilon));
            }
        }
        return lists.get(0);
    }
}
