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
 */package com.oculusinfo.geometry.cartesian;

import com.oculusinfo.math.linearalgebra.TriDiagonalMatrix;
import com.oculusinfo.math.linearalgebra.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class CubicBSpline {
//    private boolean      _periodic;
    private int          _n;
    private List<Vector> _points;
    private List<Double> _times;

    public CubicBSpline (boolean periodic) {
//        _periodic = periodic;
        _points = new ArrayList<Vector>();
        _times = new ArrayList<Double>();
        _n = 0;
    }


    // TODO:
    // (1) Create a spline through a given set of points (requires tridiagonal matrix)
    //     http://www.particleincell.com/2012/bezier-splines/
    // (2) Implement distance algorithm for splines
    // (3) Implement mean algorithm for splines
    public static CubicBSpline fit (double[] times, Vector... points) {
        return fit(times, Arrays.asList(points));
    }
    public static CubicBSpline fit (double[] times, List<Vector> Ks) {
        // We use the procedure in
        // http://www.particleincell.com/2012/bezier-splines/ to find the
        // intermediate control points

        int n = Ks.size()-1;
        int d = Ks.get(0).size();
        if (n<2)
            return null;

        // Create our tri-diagonal matrix to invert
        double[] matrixEntries = new double[3*n-2];
        matrixEntries[0] = 2;
        matrixEntries[1] = 1;
        for (int i=1; i<n-1; ++i) {
            matrixEntries[3*i-1] = 1;
            matrixEntries[3*i-0] = 4;
            matrixEntries[3*i+1] = 1;
        }
        matrixEntries[3*n-4] = 2;
        matrixEntries[3*n-3] = 7;
        TriDiagonalMatrix M = new TriDiagonalMatrix(matrixEntries);

        // Create our target vectors
        List<Vector> Ys = new ArrayList<Vector>();
        Vector K0 = Ks.get(0);
        Vector K1 = Ks.get(1);
        Ys.add(K0.add(K1.scale(2)));
        for (int i=1; i<n-1; ++i) {
            Vector Ki = Ks.get(i);
            Vector Ki1 = Ks.get(i+1);
            Ys.add(Ki.scale(4).add(Ki1.scale(2)));
        }
        Vector Knm1 = Ks.get(n-1);
        Vector Kn = Ks.get(n);
        Ys.add(Knm1.scale(8).add(Kn));
        
        
        List<Vector> P1coords = new ArrayList<Vector>();
        List<Vector> P2coords = new ArrayList<Vector>();
        for (int i=0; i<d; ++i) {
            Vector Y = getCoordinateVector(Ys, i);
            Vector K = getCoordinateVector(Ks, i);
            Vector P1i = M.solve(Y);

            // last p2 entry is special
            double[] p2Data = new double[n];
            for (int j=0; j<n-1; ++j)
                p2Data[j] = 2*K.coord(j+1) - P1i.coord(j+1);
            p2Data[n-1] = 0.5*(K.coord(n) + P1i.coord(n-1));
            Vector P2i = new Vector(p2Data);
            
            P1coords.add(P1i);
            P2coords.add(P2i);
        }

        // These are coordinate vectors; revert to points
        List<Vector> splinePoints = new ArrayList<Vector>();
        for (int i=0; i<n; ++i) {
            splinePoints.add(Ks.get(i));
            splinePoints.add(getCoordinateVector(P1coords, i));
            splinePoints.add(getCoordinateVector(P2coords, i));
        }
        splinePoints.add(Ks.get(n));

        // Normalize the times.
        double t0 = times[0];
        double tn = times[n];
        double deltat = tn-t0;
        List<Double> splineTimes = new ArrayList<Double>();
        for (int i=0; i<=n; ++i) {
            splineTimes.add((times[i]-t0)/deltat);
        }


        CubicBSpline spline = new CubicBSpline(false);
        spline._points = splinePoints;
        spline._times = splineTimes;
        
        return spline;
    }

    static private Vector getCoordinateVector(List<Vector> Vs, int coordinate) {
        int n = Vs.size();
        double[] data = new double[n];
        for (int i=0; i<n; ++i) {
            data[i] = Vs.get(i).coord(coordinate);
        }
        return new Vector(data);
    }


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


    public int getNumSegments () {
        return (_points.size()-1)/3;
    }

    public Vector getControlPoint (int i, int j) {
        return _points.get(i*3+j);
    }

    public Vector getPoint (double relTime) {
        int n = getNumSegments();
        for (int i=0; i<n; ++i) {
            double t0 = _times.get(i);
            double t1 = _times.get(i+1);
            if (t0 <= relTime && relTime <= t1) {
                // this segment
                Vector P0 = getControlPoint(i, 0);
                Vector P1 = getControlPoint(i, 1);
                Vector P2 = getControlPoint(i, 2);
                Vector P3 = getControlPoint(i, 3);
                double t = (relTime-t0)/(t1-t0);
                double nt = (1-t);
                return P0.scale(nt*nt*nt)
                        .add(P1.scale(3*nt*nt*t))
                        .add(P2.scale(3*nt*t*t))
                        .add(P3.scale(t*t*t));
            }
        }
        return null;
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

    public void draw (Graphics2D g, double zeroX, double zeroY, double pixelsPerUnit, int pointsToDraw) {
        Point lastP = null;
        for (int i=0; i<pointsToDraw; ++i) {
            double t = i * 1.0 / pointsToDraw;
            Vector v = getPoint(t);
            Point p = new Point((int) Math.round((v.coord(0)-zeroX)*pixelsPerUnit),
                                (int) Math.round((zeroY-v.coord(1))*pixelsPerUnit));
            if (null != lastP) g.drawLine(lastP.x, lastP.y, p.x, p.y);
            lastP = p;
        }
    }
}
