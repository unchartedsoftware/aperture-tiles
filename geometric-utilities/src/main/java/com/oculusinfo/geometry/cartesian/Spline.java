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
package com.oculusinfo.geometry.cartesian;

import com.oculusinfo.math.linearalgebra.Vector;

import java.util.ArrayList;
import java.util.List;


public class Spline {
    private boolean      _periodic;
    private int          _n;
    private List<Vector> _points;
    private List<Double> _times;

    public Spline (boolean periodic) {
        _periodic = periodic;
        _points = new ArrayList<Vector>();
        _times = new ArrayList<Double>();
        _n = 0;
    }


    // TODO:
    // (1) Create a spline through a given set of points (requires tridiagonal matrix)
    //     http://www.particleincell.com/2012/bezier-splines/
    // (2) Implement distance algorithm for splines
    // (3) Implement mean algorithm for splines


    /**
     * Add a new control point into the spline, in the proper order.
     * 
     * @param time
     *            The time at which the point occurs
     * @param point
     *            The point to add
     */
    public void addPoint (double time, Vector point) {
        // Figure out at what index to add the time
        for (int i = 0; i < _n; ++i) {
            if (time < _times.get(i)) {
                // Add before point i.
                _points.add(i, point);
                _times.add(i, time);
                ++_n;
                return;
            }
        }

        // New last point
        _points.add(point);
        _times.add(time);
        ++_n;
    }

    /*
     * This method only exists to use the _periodic field, which is here for
     * future expansion, and isn't actually used yet.
     */
    protected boolean isPeriodic () {
        return _periodic;
    }

    /*
     * Get t_i for a given i
     */
    private double t (int i) {
        if (i < 0)
            throw new IndexOutOfBoundsException("Can't get t_i for i<0");
        if (i >= _times.size())
            throw new IndexOutOfBoundsException("Can't get t_i for i>n (i="+i+", n="+_n+")");

        double t0 = _times.get(0);
        double tn = _times.get(_times.size()-1);
        double ti = _times.get(i);
        return (ti-t0)/(tn-t0);
    }

    /*
     * Get the i at which t lies between t_floor(i) and t_ceil(i)
     */
    protected double i (double t) {
        if (t < 0)
            throw new IndexOutOfBoundsException("Attempt to get i for t<0");
        if (t >= 1)
            throw new IndexOutOfBoundsException("Attempt to get i for t>1");
        double lastti = 0;
        for (int i=0; i<_times.size(); ++i) {
            double ti = t(i);
            if (ti > t) {
                return i + (t-lastti)/(ti-lastti);
            }
        }
        // should never get here
        throw new RuntimeException("Weird i-calculation error in spline calculation - no valid i found");
    }
}
