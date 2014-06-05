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

import junit.framework.Assert;
import org.junit.Test;

public class TestTriDiagonalMatrix {
    private static final double EPSILON = 1E-12;



    @Test
    public void testMultiplication () {
        TriDiagonalMatrix M = new TriDiagonalMatrix(1, 1,
                                                    1, 1, 1,
                                                       1, 1, 1,
                                                          1, 1);
        Vector X = new Vector(1, 2, 3, 4);
        Vector Y = M.times(X);
        Assert.assertEquals(4, Y.size());
        Assert.assertEquals(3, Y.coord(0), EPSILON);
        Assert.assertEquals(6, Y.coord(1), EPSILON);
        Assert.assertEquals(9, Y.coord(2), EPSILON);
        Assert.assertEquals(7, Y.coord(3), EPSILON);

        M = new TriDiagonalMatrix(1, 2,
                                  3, 4, 5,
                                     6, 7, 8,
                                        9, 8, 7,
                                           6, 5, 4,
                                              3, 2);
        X = new Vector(1, 2, 3, 4, 5, 6);
        Y = M.times(X);
        Assert.assertEquals(6, Y.size());
        Assert.assertEquals(5, Y.coord(0), EPSILON);
        Assert.assertEquals(26, Y.coord(1), EPSILON);
        Assert.assertEquals(65, Y.coord(2), EPSILON);
        Assert.assertEquals(94, Y.coord(3), EPSILON);
        Assert.assertEquals(73, Y.coord(4), EPSILON);
        Assert.assertEquals(27, Y.coord(5), EPSILON);

        M = new TriDiagonalMatrix(1, 2,
                                  1, 2);
        X = new Vector(3, 4);
        Y = M.times(X);
        Assert.assertEquals(2,  Y.size());
        Assert.assertEquals(11, Y.coord(0), EPSILON);
        Assert.assertEquals(11, Y.coord(1), EPSILON);
    }


    @Test
    public void test1DSolving () {
        TriDiagonalMatrix M = new TriDiagonalMatrix(0);
        Vector D = new Vector(0);
        Vector X = M.solve(D);
        Assert.assertEquals(new Vector(1), X);

        D = new Vector(1);
        X = M.solve(D);
        Assert.assertEquals(new Vector(Double.NaN), X);

        M = new TriDiagonalMatrix(1);
        D = new Vector(0);
        X = M.solve(D);
        Assert.assertEquals(new Vector(0), X);

        D = new Vector(1);
        X = M.solve(D);
        Assert.assertEquals(new Vector(1), X);
    }



    @Test
    public void test2DSolving () {
        Vector vNaN = new Vector(Double.NaN, Double.NaN);
        TriDiagonalMatrix M;
        Vector D;
        Vector X;

        M = new TriDiagonalMatrix(0, 0, 0, 0);
        D = new Vector(0, 0);
        X = M.solve(D);
        Assert.assertEquals(2, X.size());
        Assert.assertFalse(Double.isNaN(X.coord(0)));
        Assert.assertFalse(Double.isNaN(X.coord(1)));

        D = new Vector(1, 0);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(0, 1);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);


        M = new TriDiagonalMatrix(0, 1, 0, 1);
        D = new Vector(1, 0);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(0, 1);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(2, 2);
        X = M.solve(D);
        Assert.assertEquals(2, X.size());
        Assert.assertFalse(Double.isNaN(X.coord(0)));
        Assert.assertEquals(2.0, X.coord(1), EPSILON);


        M = new TriDiagonalMatrix(1, 1, 0, 0);
        D = new Vector(4, 0);
        X = M.solve(D);
        Assert.assertEquals(2, X.size());
        Assert.assertEquals(4.0, X.coord(0) + X.coord(1), EPSILON);

        D = new Vector(1, 0);
        X = M.solve(D);
        Assert.assertEquals(2, X.size());
        Assert.assertEquals(1.0, X.coord(0) + X.coord(1), EPSILON);

        D = new Vector(0, 1);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(1, 1);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);


        M = new TriDiagonalMatrix(1, 0, 1, 0);
        D = new Vector(1, 0);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(0, 1);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(2, 2);
        X = M.solve(D);
        Assert.assertEquals(2, X.size());
        Assert.assertEquals(2.0, X.coord(0), EPSILON);
        Assert.assertFalse(Double.isNaN(X.coord(1)));


        M = new TriDiagonalMatrix(0, 0, 1, 1);
        D = new Vector(1, 0);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(0, 4);
        X = M.solve(D);
        Assert.assertEquals(2, X.size());
        Assert.assertEquals(4.0, X.coord(0) + X.coord(1), EPSILON);

        D = new Vector(1, 1);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);


        M = new TriDiagonalMatrix(0, 1, 2, 3);
        D = new Vector(2, 12);
        X = M.solve(D);
        Assert.assertEquals(new Vector(3, 2), X);


        M = new TriDiagonalMatrix(1, 0, 2, 3);
        D = new Vector(3, 12);
        X = M.solve(D);
        Assert.assertEquals(new Vector(3, 2), X);


        M = new TriDiagonalMatrix(2, 1, 0, 3);
        D = new Vector(8, 6);
        X = M.solve(D);
        Assert.assertEquals(new Vector(3, 2), X);


        M = new TriDiagonalMatrix(3, 1, 2, 0);
        D = new Vector(11, 6);
        X = M.solve(D);
        Assert.assertEquals(new Vector(3, 2), X);


        M = new TriDiagonalMatrix(1, 2, 3, 4);
        D = new Vector(7, 17);
        X = M.solve(D);
        Assert.assertEquals(new Vector(3, 2), X);
    }



    @Test
    public void test3DSolving () {
        Vector vNaN = new Vector(Double.NaN, Double.NaN, Double.NaN);
        TriDiagonalMatrix M;
        Vector D;
        Vector X, X0, X1;

        M = new TriDiagonalMatrix(0, 1,
                                  0, 1, 2,
                                     3, 4);
        D = new Vector(4, 7, 17);
        X = M.solve(D);
        Assert.assertEquals(vNaN, X);

        D = new Vector(3, 7, 17);
        X = M.solve(D);
        Assert.assertEquals(3, X.size());
        Assert.assertFalse(Double.isNaN(X.coord(0)));
        Assert.assertEquals(3.0, X.coord(1), EPSILON);
        Assert.assertEquals(2.0, X.coord(2), EPSILON);

        M = new TriDiagonalMatrix(0, 1,
                                  2, 3, 4,
                                     5, 6);
        X0 = new Vector(1, 2, 3);
        D = M.times(X0);
        X1 = M.solve(D);
        Assert.assertEquals(X0, X1);

        M = new TriDiagonalMatrix(1, 2,
                                  0, 3, 4,
                                     5, 6);
        X0 = new Vector(1, 2, 3);
        D = M.times(X0);
        X1 = M.solve(D);
        Assert.assertEquals(X0, X1);

        M = new TriDiagonalMatrix(1, 2,
                                  3, 4, 5,
                                     6, 7);
        X0 = new Vector(1, 2, 3);
        D = M.times(X0);
        X1 = M.solve(D);
        Assert.assertEquals(X0, X1);
    }
}
