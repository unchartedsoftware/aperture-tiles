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



package com.oculusinfo.math.statistics;

import junit.framework.Assert;
import org.junit.Test;

public class PoissonDistributionTests {
    // Test that our Poisson distribution creates the distribution it says it does.
    @Test
    public void testPoissonDistributionMeanAndBounds () {
        PoissonDistribution distribution = new PoissonDistribution(10);
        int N = 100000;
        double total = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int n = 0; n < N; ++n) {
          double sample = distribution.sample();
          total = total + sample;
          if (sample < min) min = sample;
          if (sample > max) max = sample;
        }
        double mean = total/N;

        System.out.println("Generated "+N+" numbers in a poisson distribution.");
        System.out.println("\tMean was "+mean);
        System.out.println("\tMinimum was "+min);
        System.out.println("\tMaximum was "+max);

        // With N=100000, minimum is going to almost always be 0, and max is
        // almost always going to be >25
        // And the mean is going to be lambda +-.1% (or, with lambda=10, +- 0.01)
        // We leave a little slop just to make sure this never really fails, since
        // it is a probabilistic test.
        Assert.assertTrue(mean > 9.9);
        Assert.assertTrue(mean < 10.1);
        Assert.assertTrue(min < 2);
        Assert.assertTrue(max > 20);
    }
}
