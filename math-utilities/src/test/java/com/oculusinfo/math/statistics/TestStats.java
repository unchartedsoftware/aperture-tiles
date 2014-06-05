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

public class TestStats {
    private static final double EPSILON = 1E-12;

    @Test
    public void testMean () {
        StatTracker s = new StatTracker();
        s.addStat(0.0);
        s.addStat(1.0);
        s.addStat(2.0);
        Assert.assertEquals(1.0, s.mean(), EPSILON);
        s.addStat(3.0);
        Assert.assertEquals(1.5, s.mean(), EPSILON);
    }

    @Test
    public void testPopulationVariance () {
        StatTracker s = new StatTracker();
        s.addStat(0.0);
        s.addStat(1.0);
        s.addStat(2.0);
        // sum(-1.0^2 + 0.0^2 + 1.0^2)/3
        Assert.assertEquals(2.0/3.0, s.populationVariance(), EPSILON);

        s.addStat(3.0);
        // sum(-1.5^2 + -0.5^2 + 0.5^2 + 1.5^2)/4
        Assert.assertEquals(5.0/4.0, s.populationVariance(), EPSILON);
    }

    @Test
    public void testSampleVariance () {
        StatTracker s = new StatTracker();
        s.addStat(0.0);
        s.addStat(1.0);
        s.addStat(2.0);
        // sum(-1.0^2 + 0.0^2 + 1.0^2)/2
        Assert.assertEquals(1.0, s.sampleVariance(), EPSILON);

        // sum(-1.5^2 + -0.5^2 + 0.5^2 + 1.5^2)/3
        s.addStat(3.0);
        Assert.assertEquals(5.0/3.0, s.sampleVariance(), EPSILON);
    }

    @Test
    public void testReset () {
        StatTracker s = new StatTracker();
        s.addStat(0.0);
        s.addStat(1.0);
        s.addStat(2.0);
        s.addStat(3.0);
        Assert.assertEquals(1.5, s.mean(), EPSILON);

        s.reset();
        s.addStat(0.0);
        s.addStat(1.0);
        Assert.assertEquals(0.5, s.mean(), EPSILON);
    }

    @Test
    public void testNormalization () {
        StatTracker s = new StatTracker();
        Assert.assertTrue(Double.isNaN(s.normalizeValue(4.0)));
        Assert.assertTrue(Double.isNaN(s.normalizeValue(5.0)));
        s.addStat(4.0);
        Assert.assertTrue(Double.isNaN(s.normalizeValue(4.0)));
        Assert.assertTrue(Double.isNaN(s.normalizeValue(5.0)));
        s.addStat(4.0);
        Assert.assertTrue(Double.isInfinite(s.normalizeValue(3.0)) && s.normalizeValue(3.0) < 0.0);
        Assert.assertTrue(Double.isNaN(s.normalizeValue(4.0)));
        Assert.assertTrue(Double.isInfinite(s.normalizeValue(5.0)) && s.normalizeValue(5.0) > 0.0);
        s.addStat(2.0);
        Assert.assertEquals(1.0, s.normalizeValue(4.0), EPSILON);
        Assert.assertEquals(1.5, s.normalizeValue(5.0), EPSILON);
        s.addStat(6.0);
        Assert.assertEquals(0.5, s.normalizeValue(4.0), EPSILON);
        Assert.assertEquals(0.75, s.normalizeValue(5.0), EPSILON);
    }
}
