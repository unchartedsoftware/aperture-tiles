/**
 * Copyright (c) 2013 Oculus Info Inc. 
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
package com.oculusinfo.geometry.cartesian;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.oculusinfo.math.linearalgebra.Vector;



public class ProgramaticSplineTest {

    @Test
    public void testSplineCalculation () {
        Vector[] K = new Vector[] {new Vector(0.25, 0.25),
                                   new Vector(0.75, 0.25),
                                   new Vector(0.75, 0.75),
                                   new Vector(0.25, 0.75)};

        List<Vector> P1 = new ArrayList<Vector>();
        List<Vector> P2 = new ArrayList<Vector>();
        computeControlPoints(Arrays.asList(K), P1, P2);
        CubicBSpline s = CubicBSpline.fit(new double[] {0, 1, 2, 3}, K);

        Assert.assertEquals(3, s.getNumSegments());
        Assert.assertEquals(     K[0], s.getControlPoint(0, 0));
        Assert.assertEquals(P1.get(0), s.getControlPoint(0, 1));
        Assert.assertEquals(P2.get(0), s.getControlPoint(0, 2));
        Assert.assertEquals(     K[1], s.getControlPoint(1, 0));
        Assert.assertEquals(P1.get(1), s.getControlPoint(1, 1));
        Assert.assertEquals(P2.get(1), s.getControlPoint(1, 2));
        Assert.assertEquals(     K[2], s.getControlPoint(2, 0));
        Assert.assertEquals(P1.get(2), s.getControlPoint(2, 1));
        Assert.assertEquals(P2.get(2), s.getControlPoint(2, 2));
        Assert.assertEquals(     K[3], s.getControlPoint(2, 3));
    }

    /* computes control points given knots K, this is the brain of the operation */
    void computeControlPoints (List<Vector> K, List<Vector> P1, List<Vector> P2) {
        int n = K.size() - 1;

        /* rhs vector */
        double[] a = new double[n];
        double[] b = new double[n];
        double[] c = new double[n];
        Vector[] r = new Vector[n];

        /* left most segment */
        a[0] = 0;
        b[0] = 2;
        c[0] = 1;
        r[0] = K.get(0).add(K.get(1).scale(2));

        /* internal segments */
        for (int i = 1; i < n - 1; i++) {
            a[i] = 1;
            b[i] = 4;
            c[i] = 1;
            r[i] = K.get(i).scale(4).add(K.get(i + 1).scale(2));
        }

        /* right segment */
        a[n - 1] = 2;
        b[n - 1] = 7;
        c[n - 1] = 0;
        r[n - 1] = K.get(n - 1).scale(8).add(K.get(n));

        /* solves Ax=b with the Thomas algorithm (from Wikipedia) */
        for (int i = 1; i < n; i++) {
            double m = a[i] / b[i - 1];
            b[i] = b[i] - m * c[i - 1];
            r[i] = r[i].add(r[i - 1].scale(-m));
        }

        P1.add(r[n - 1].scale(1.0 / b[n - 1]));
        for (int i = n - 2; i >= 0; --i)
            P1.add(0, r[i].add(P1.get(0).scale(-c[i])).scale(1.0 / b[i]));

        /* we have p1, now compute p2 */
        for (int i = 0; i < n - 1; i++)
            P2.add(K.get(i + 1).scale(2).subtract(P1.get(i + 1)));

        P2.add(K.get(n).add(P1.get(n - 1)).scale(0.5));
    }
}
