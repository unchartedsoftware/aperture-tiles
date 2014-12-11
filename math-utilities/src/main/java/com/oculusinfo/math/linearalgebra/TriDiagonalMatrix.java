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



/**
 * A specific matrix type where only the diagonal and the entries just before
 * and after the diagonal are non-zero.
 * 
 * This type of matrix is especially useful in spline fitting.
 * 
 * @author nkronenfeld
 */
public class TriDiagonalMatrix {
    private int      _n;
    // The n-1 entries below the diagonal; the array is of size n, though, and
    // the first entry is ignored, for consistency with standard nomenclature
    private double[] _a;
    // The n diagonal entries
    private double[] _b;
    // The n-1 entries above the diagonal
    private double[] _c;

    private double EPSILON;
    
    public TriDiagonalMatrix (double... entries) {
        EPSILON = 1E-12;

        // There should be 3n-2 entries
        int en = entries.length;
        if (1 != (en % 3))
            throw new IndexOutOfBoundsException("Tridiagonal matrices must have (3n-2) entries");

        _n = (en + 2) / 3;
        _a = new double[_n];
        _b = new double[_n];
        _c = new double[_n - 1];
        for (int i = 0; i < _n; ++i) {
            _b[i] = entries[3 * i];
            if (i < _n - 1) {
                _c[i] = entries[3 * i + 1];
                _a[i + 1] = entries[3 * i + 2];
            }
        }
    }

    /**
     * Set the precision for equality and zero tests for this matrix. If any
     * calculation yields two numbers closer than the given precision, they are
     * deemed equal. If any calculation yields a number less than the given
     * precision, it is deemed zero.
     * 
     * @param precision
     *            The precision for calculations with this matrix.
     */
    public void setPrecision (double precision) {
        EPSILON = precision;
    }


    /**
     * Find the X for which this*X=d
     * 
     * Taken from @see <a href="http://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm></a>, but modified to
     * use recursion instead of iteration, and thereby to handle degenerate
     * cases cleanly.
     * 
     * @param d
     *            The result (<code>d</code>) in the above equation
     * @return The <code>X</code> in the above equation
     */
    public Vector solve (Vector d) {
        if (d.size() != _n)
            throw new IllegalArgumentException("Attempt to find tri-diagonal solution with improper-sized vector");

        double[] x = new double[_n];
        solve(d, x, 0,
              (_n > 0 ? _b[0] : 0),
              (_n > 1 ? _c[0] : 0),
              (_n > 0 ? d.coord(0) : 0));
        //solve3(d, x, 1, _b[0], _c[0], 0, d.coord(0));
        return new Vector(x);
    }

    private void solve (Vector d, double[] x, int currentColumn, double b0, double c0, double d0) {
        // Solve the case M x = d for x, where we (the tri-diagonal matrix) is m
        //
        // We do this recursively, solving one place at a time, pretending in
        // each case we are at the top left of the matrix.
        //
        // So, in each case, we have one of three cases, depending on whether
        // there are one, two, or three-or-more rows left to solve.
        int rowsLeft = _n-currentColumn;
        if (0 == rowsLeft) {
            return; // Nothing left to solve
        } else if (1 == rowsLeft) {
            solveSingleRow(d, x, currentColumn, b0, c0, d0);
        } else if (2 == rowsLeft) {
            solveDoubleRow(d, x, currentColumn, b0, c0, d0);
        } else {
            solveTripleRow(d, x, currentColumn, b0, c0, d0);
        }
    }

    private double anythingIfNotNaN (double testValue) {
        return ifNotNaN(testValue, 1.0);
    }
    private double ifNotNaN (double testValue, double value) {
        if (Double.isNaN(testValue)) return Double.NaN;
        else return value;
    }

    private void solveSingleRow (Vector d, double[] x, int currentColumn, double b0, double c0, double d0) {
        if (currentColumn != (_n-1))
            throw new IllegalArgumentException("Attempt to solve a single row when not on the last row.");

        // If we have only one row, we have:
        //     | b0 | | x0 | = | d0 |
        // and the solution is trivial: x0 = d0/b0
        if (Math.abs(b0) < EPSILON) {
            if (Math.abs(d0) < EPSILON) {
                // Anything will work
                x[currentColumn] = 1;
            } else {
                // Nothing will work
                x[currentColumn] = Double.NaN;
            }
        } else {
            x[currentColumn] = d0/b0;
        }
    }

    private void solveDoubleRow (Vector d, double[] x, int currentColumn, double b0, double c0, double d0) {
        if (currentColumn != (_n-2))
            throw new IllegalArgumentException("Attempt to solve a double row when not on the second to last row.");

        // If we have two rows, we have:
        //    | b0 c0 | | x0 | _ | d0 |
        //    | a1 b1 | | x1 | - | d1 |
        // or
        //    b0 x0 + c0 x1 = d0
        //    a1 x0 + b1 x1 = d1
        //
        // which can, of course, be solved one of two ways:
        //    a1 b0 x0 + a1 c0 x1 = a1 d0
        //    a1 b0 x0 + b0 b1 x1 = b0 d1
        //    (a1 c0 - b0 b1) x1 = (a1 d0 - b0 d1)
        // or
        //    b0 b1 x0 + b1 c0 x1 = b1 d0
        //    a1 c0 x0 + b1 c0 x1 = c0 d1
        //    (b0 b1 - a1 c0) x0 = (b1 d0 - c0 d1)
        //
        // in either case, we require
        //     b0 b1 - a1 c0 != 0
        // (i.e., non-zero determinate)
        //
        // If the determinant is 0, then the two rows are dependent, and we
        // depend on the solution also being in the same proportion to be able
        // to solve them.
        double a1 = _a[currentColumn+1];
        double b1 = _b[currentColumn+1];
        double d1 = d.coord(currentColumn+1);
        double determinate = b0 * b1 - a1 * c0;
        if (Math.abs(determinate) < EPSILON) {
            if (Math.abs(b0) < EPSILON &&
                Math.abs(c0) < EPSILON &&
                Math.abs(a1) < EPSILON &&
                Math.abs(b1) < EPSILON) {
                // We are the zero matrix. This is fine if D is the zero vector, in which case anything will work; otherwise, there is no solution
                if (Math.abs(d0) < EPSILON && Math.abs(d1) < EPSILON) {
                    x[currentColumn] = 1;
                    x[currentColumn+1] = 1;
                    return;
                } else {
                    x[currentColumn] = Double.NaN;
                    x[currentColumn+1] = Double.NaN;
                    return;
                }
            } else {
                // figure out the proportion

                // All these are solved the same way, we just need to pick an
                // order based on a known non-zero element.
                //
                // If they are solvable, they are solved assuming the non-solved coordinate is 1. 
                if (Math.abs(a1) >= EPSILON) {
                    // a1 > 0
                    double sln = solveDegenerate2D(a1, b1, d1, b0, c0, d0);
                    x[currentColumn] = sln;
                    x[currentColumn+1] = anythingIfNotNaN(sln);
                } else if (Math.abs(b0) >= EPSILON) {
                    // b0 > 0
                    double sln = solveDegenerate2D(b0, c0, d0, b1, a1, d1);
                    x[currentColumn] = sln;
                    x[currentColumn+1] = anythingIfNotNaN(sln);
                } else if (Math.abs(c0) >= EPSILON) {
                    // c0 > 0
                    double sln = solveDegenerate2D(c0, b0, d0, b1, a1, d1);
                    x[currentColumn] = anythingIfNotNaN(sln);
                    x[currentColumn+1] = sln;
                } else {
                    // b1 > 0
                    // This case is never actually reached - it only can be if
                    // the determinate is non-zero or all are 0
                    double sln = solveDegenerate2D(b1, a1, d1, c0, b0, d0);
                    x[currentColumn] = anythingIfNotNaN(sln);
                    x[currentColumn+1] = sln;
                }
            }
        } else {
            // From above: 
            //    (a1 c0 - b0 b1) x1 = (a1 d0 - b0 d1)
            //    (b0 b1 - a1 c0) x0 = (b1 d0 - c0 d1)
            x[currentColumn+0] = (b1 * d0 - c0 * d1) / determinate; 
            x[currentColumn+1] = (b0 * d1 - a1 * d0) / determinate;
            return;
        }
    }
    private double solveDegenerate2D (double a0, double b0, double d0, double a1, double b1, double d1) {
        // a0 is assumed to be non-zero
        double rowRatio = a1/a0;
        double dRatio = d1/d0;
        if (Math.abs(rowRatio-dRatio) > EPSILON)
            return Double.NaN;
        else
            // a0 x0 + b0 x1 = d0;
            // x0 = (d0 - b0 x1) / a0
            // assume x1 is 1
            return (d0 - b0) / a0;
    }

    private void solveTripleRow (Vector d, double[] x, int currentColumn, double b0, double c0, double d0) {
        // If we have three or more rows, we have:
        //    | b0 c0  0 ... | | x0 |   | d0 |
        //    | a1 b1 c1 ... | | x1 |   | d1 |
        //    |      .       | | x2 | = | d2 |
        //    |      .       | |  . |   |  . |
        //    |      .       | |  . |   |  . |
        //
        // or, put another way:
        // b0 x0 + c0 x1 = d0
        // a1 x0 + b1 x1 + c1 x2 = d1

        double a1 = _a[currentColumn+1];
        double b1 = _b[currentColumn+1];
        double c1 = _c[currentColumn+1];
        double d1 = d.coord(currentColumn+1);
        if (Math.abs(b0) < EPSILON && Math.abs(a1) < EPSILON) {
            // b0 and a1 are both zero
            // First, skip this row and just go on
            solve(d, x, currentColumn+1, b1, c1, d1);
            // We just need to make sure that our input row does work
            if (Math.abs(c0 * x[currentColumn+1] - d0) >= EPSILON) {
                // Nope; doesn't work.
                for (int i=currentColumn; i<_n; ++i)
                    x[i] = Double.NaN;
                return;
            } else {
                x[currentColumn] = anythingIfNotNaN(x[currentColumn+1]);
                return;
            }
        } else if (Math.abs(b0) < EPSILON) {
            // b0 is 0

            // First, skip the next row and just go on
            solve(d, x, currentColumn+1, c0, 0, d0);
            // Now, use the next row to solve our current value 
            double x1 = x[currentColumn+1];
            double x2 = x[currentColumn+2];
            // a1 x0 + b1 x1 + c1 x2 = d1
            // a1 is known not to be zero, so we shouldn't have any problems.
            x[currentColumn] = (d1 - b1 * x1 - c1 * x2) / a1;
            return;
        } else if (Math.abs(a1) < EPSILON) {
            // a1 is 0

            // first, skip this row and just go on
            solve(d, x, currentColumn+1, b1, c1, d1);
            // Now, use the current row to solve our current value
            // b0 is known not to be zero, so we shouldn't have any problems.
            double x1 = x[currentColumn+1];
            // b0 x0 + c0 x1 = d0
            x[currentColumn] = (d0 - c0 * x1) / b0;
            return;
        } else {
            // neither is 0

            // a1 x0 + b1 x1 + c1 x2 = d1
            // b0 x0 + c0 x1 = d0
            //
            // a1 b0 x0 + b0 b1 x1 + b0 c1 x2 = b0 d1
            // a1 b0 x0 + a1 c0 x1 = a1 d0
            //
            // (b0 b1 - a1 c0) x1 + (b0 c1) x2 = (b0 d1 - a1 d0);
            //
            // We've removed x0!
            solve(d, x, currentColumn+1,
                  (b0 * b1 - a1 * c0),
                  (b0 * c1),
                  (b0 * d1 - a1 * d0));
            // Now we back-solve to get x0
            // b0 is known not to be zero, so we shouldn't have any problems.
            double x1 = x[currentColumn+1];
            // b0 x0 + c0 x1 = d0
            x[currentColumn] = (d0 - c0 * x1) / b0;
            return;
        }
    }

    /**
     * Find this*X
     * 
     * @param X
     *            The vector by which we are being multiplied
     * @return The result of multiplying us by <code>X</code>
     */
    public Vector times (Vector X) {
        if (X.size() != _n)
            throw new IllegalArgumentException("Illegal result - matrix multiplication can't result in a vector of size "
                                                       + X.size());

        double[] r = new double[_n];
        for (int i = 0; i < _n; ++i) {
            double entry = 0;
            if (i > 0)
                entry += _a[i] * X.coord(i - 1);
            entry += _b[i] * X.coord(i);
            if (i < _n - 1)
                entry += _c[i] * X.coord(i + 1);
            r[i] = entry;
        }
        return new Vector(r);
    }
}
