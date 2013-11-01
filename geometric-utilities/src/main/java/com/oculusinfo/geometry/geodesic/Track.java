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
package com.oculusinfo.geometry.geodesic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oculusinfo.math.linearalgebra.ListUtilities;

abstract public class Track {
    private PositionCalculationParameters _parameters;
    private List<Position>                _points;
    private List<Double>                  _parameterization;
    private double                        _length;
    private Track                         _reverse;
    private Map<String, Double>           _statistics;

    protected Track (PositionCalculationParameters parameters, Position... points) {
        this(parameters, Arrays.asList(points));
    }

    protected Track (PositionCalculationParameters parameters,
                     List<Position> points) {
        _parameters = parameters;
        _points = new ArrayList<Position>(points);
        _reverse = null;
        _statistics = null;

        calculateLengthParameterization();

        reduce();
    }

    protected Track (PositionCalculationParameters parameters,
                     List<Position> points,
                     List<Double> parameterization) {
        _parameters = parameters;
        _points = new ArrayList<Position>(points);
        _parameterization = new ArrayList<Double>(parameterization);
        _reverse = null;
        _statistics = null;

        calculateLength();

        reduce();
    }

    protected Track (Track oldTrack, PositionCalculationParameters newParameters) {
        _parameters = newParameters;
        _points = new ArrayList<Position>();
        _parameterization = new ArrayList<Double>();
        _reverse = null;
        _statistics = null;

        for (int i=0; i<oldTrack._points.size(); ++i) {
            Position p = new Position(oldTrack._points.get(i),
                                      !_parameters.getCalculationType().equals(PositionCalculationType.Cartesian3D));
            p.setPrecision(_parameters.getPrecision());
            _points.add(p);
            _parameterization.add(oldTrack._parameterization.get(i));
        }

        calculateLength();

        reduce();
    }

    public Track reverse () {
        if (null == _reverse) {
            List<Position> reversePoints = new ArrayList<Position>(_points);
            Collections.reverse(reversePoints);
            List<Double> reverseParameterization = new ArrayList<Double>(_parameterization);
            Collections.reverse(reverseParameterization);
            for (int i=0; i<reverseParameterization.size(); ++i)
                reverseParameterization.set(i, 1.0-reverseParameterization.get(i));
            _reverse = createTrack(reversePoints, reverseParameterization);
            _reverse._reverse = this;
        }
        return _reverse;
    }

    public PositionCalculationParameters getParameters () {
        return _parameters;
    }

    public List<Position> getPoints () {
        return _points;
    }

    public double getLength () {
        return _length;
    }

    protected List<Double> getParameterization () {
        return _parameterization;
    }

    /*
     * This is currently used only for filling in intermediate points for
     * cartesian tracks. As such, the length always stays the same. If it is
     * ever used to actually change the underlying geometry, it would have to
     * take a new length too.
     */
    protected void updatePoints (List<Position> points, List<Double> parameterization) {
        _points = points;
        _parameterization = parameterization;
    }

    /*
     * Universal precalculation step - figure out the length parameterization of the trajectory
     */
    private void calculateLengthParameterization () {
        _parameterization = new ArrayList<Double>(_points.size());

        calculateLength();
        double cumulativeLength = 0.0;

        Position lastPoint = null;
        for (Position point: _points) {
            if (0 == _length) {
                _parameterization.add(0.0);
            } else {
                if (null != lastPoint) {
                    cumulativeLength += getSegmentDistance(lastPoint, point);
                }
                _parameterization.add(cumulativeLength/_length);
                lastPoint = point;
            }
        }
    }

    /** Get the distance from the start to the end position */
    abstract protected double getSegmentDistance (Position start, Position end);
    /** Interpolate 100*t percent of the way from start to end */
    abstract protected Position interpolate (Position start, Position end, double t);
    /**
     * Get the amount of error that would be introduced by removing point b,
     * lieing between points a and c from this track
     */
    abstract protected double getRelativeError (Position a, Position b, Position c);
    
    /** Create a track of the current type from the listed points */
    abstract protected Track createTrack (List<Position> points);
    /** Create a track of the current type from the listed points, whose parameterization is already calculated. */
    abstract protected Track createTrack (List<Position> path, List<Double> parameterization);

    /*
     * Get the relative importance of point b relative to points a and c.
     * Basically, this is a measure of how far b is from each; it will be 1 when
     * b is equidistant from each, and will go up from there. We do this so as
     * to preserve corners.
     * 
     * The relative importance should be 0 if b is a duplicate of a or c.
     */
    private double getRelativeImportance (Position a, Position b, Position c) {
        double dab = getSegmentDistance(a, b);
        double dbc = getSegmentDistance(b, c);

        if (dab < getParameters().getPrecision()) return 0; // duplicate point, doesn't matter at all
        if (dbc < getParameters().getPrecision()) return 0; // duplicate point, doesn't matter at all

        return Math.max(dab/dbc, dbc/dab);
    }



    private boolean closerToReverse (Track them) {
        // First try matching endpoints
        Position ourStart = _points.get(0);
        Position ourEnd = _points.get(_points.size()-1);

        List<Position> theirPoints = them.getPoints();
        Position theirStart = theirPoints.get(0);
        Position theirEnd = theirPoints.get(theirPoints.size()-1);

        double dss = getSegmentDistance(ourStart, theirStart);
        double dse = getSegmentDistance(ourStart, theirEnd);
        double des = getSegmentDistance(ourEnd, theirStart);
        double dee = getSegmentDistance(ourEnd, theirEnd);

        double requiredConfidence = 0.5;
        return (dse/dee < requiredConfidence && des / dss < requiredConfidence);
    }

    public double getDistance (Track them) {
        if (_parameters.ignoreDirection()) {
            if (closerToReverse(them))
                return getDistanceWithDirection(them.reverse());
            else
                return getDistanceWithDirection(them);
        } else {
            return getDistanceWithDirection(them);
        }
    }

    private double getDistanceWithDirection (Track them) {
        List<Double> ourParameterization = getParameterization();
        List<Double> theirParameterization = them.getParameterization();
        List<Double> joinedParameterization = ListUtilities.joinLists(ourParameterization,
                                                                      theirParameterization,
                                                                      _parameters.getPrecision());

        Position pALast = null;
        Position pBLast = null;
        double dLast = 0;
        double totalDistance = 0.0;
        for (double d : joinedParameterization) {
            Position pA = getLengthParamterizedPoint(d);
            Position pB = them.getLengthParamterizedPoint(d);
            if (null != pALast) {
                double startDistance = getSegmentDistance(pALast, pBLast);
                double endDistance = getSegmentDistance(pA, pB);
                totalDistance += (startDistance + endDistance) / 2
                                 * (d - dLast);
            }

            dLast = d;
            pALast = pA;
            pBLast = pB;
        }

        return totalDistance / ((_length + them._length) / 2.0);
    }



    public Track weightedAverage (Track them, double ourWeight, double theirWeight) {
        if (_parameters.ignoreDirection()) {
            if (closerToReverse(them))
                return weightedAverageWithDirection(them.reverse(), ourWeight, theirWeight);
            else
                return weightedAverageWithDirection(them, ourWeight, theirWeight);
        } else {
            return weightedAverageWithDirection(them, ourWeight, theirWeight);
        }
    }
    protected Track weightedAverageWithDirection (Track them, double ourWeight, double theirWeight) {
        double theirRelWeight = theirWeight/(ourWeight+theirWeight);

        // Get all parameterization points
        List<Double> ourParameterization = getParameterization();
        List<Double> theirParameterization = them.getParameterization();
        List<Double> joinedParameterization = ListUtilities.joinLists(ourParameterization,
                                                                      theirParameterization,
                                                                      _parameters.getPrecision());

        // Average the tracks along each parameterization point
        List<Position> meanPath = new ArrayList<Position>();
        for (double d: joinedParameterization) {
            Position pUs = getLengthParamterizedPoint(d);
            Position pThem = them.getLengthParamterizedPoint(d);
            Position weightedMean = interpolate(pUs, pThem, theirRelWeight);
            meanPath.add(weightedMean);
        }

        return createTrack(meanPath);
    }

    protected String getLabel () {
        return "Trajectory";
    }
    @Override
    public String toString () {
        StringBuffer result = new StringBuffer();
        result.append(getLabel());
        result.append("<");
        result.append(getParameters().getCalculationType());
        result.append(">[");
        for (int i=0; i<_points.size(); ++ i) {
            if (0 < i) result.append(", ");
            result.append(_points.get(i));
        }
        result.append("]");
        return result.toString();
    }

    @Override
    public boolean equals (Object that) {
        if (this == that) return true;
        if (null == that) return false;
        if (!(that instanceof Track)) return false;

        Track track = (Track) that;
        return getDistance(track) < getParameters().getPrecision();
    }



    /*
     * Remove points that don't add anything significant to this path.
     * Insignificance is defined by _parameter.getAllowedError(), and the
     * definition of {@link #getRelativeError}.
     */
    private void reduce () {
        double minImportance = Math.sqrt(1.0/_parameters.getAllowedError());
        // Remove points that don't contribute much
        int i=0;
        while (i<_points.size()-2) {
            Position a = _points.get(i);
            Position b = _points.get(i+1);
            Position c = _points.get(i+2);
            boolean remove = (a.equals(b) || c.equals(b));
            if (!remove) {
                double relativeError = getRelativeError(a, b, c);
                double relativeImportance = getRelativeImportance(a, b, c);
                remove = (relativeError < _parameters.getAllowedError());
                if (remove && minImportance < relativeImportance)
                    remove = false;
                if (remove) {
                    getRelativeError(a, b, c);
                    getRelativeImportance(a, b, c);
                }
            }
            if (remove) {
                _points.remove(i+1);
                _parameterization.remove(i+1);
            } else {
                ++i;
            }
        }

        // finally, check for duplication in the last two points
        int n = _points.size();
        if (n>1) {
            Position a = _points.get(n-2);
            Position b = _points.get(n-1);
            if (a.equals(b)) {
                _points.remove(n-2);
                _parameterization.remove(n-2);
            }
        }
    }



    // Should only be called for initialization of _length; after initial
    // setting, that should be trusted
    private void calculateLength () {
        _length = 0.0;
        if (null == _points || _points.size() < 2)
            return;

        Position lastPoint = null;
        for (Position point : _points) {
            if (null != lastPoint)
                _length += getSegmentDistance(point, lastPoint);
            lastPoint = point;
        }
    }

    protected Position getLengthParamterizedPoint (double parameter) {
        if (parameter < 0.0 || 1.0 < parameter)
            throw new IllegalArgumentException("Length paramterization parameter must be between 0 and 1");

        // Only one point; return it.
        int N = _parameterization.size();
        if (0 == N) return null;
        if (1 == N) return _points.get(0);

        int n;
        for (n = 0; n < N - 1; ++n) {
            if (_parameterization.get(n + 1) > parameter)
                break;
        }

        double startD = _parameterization.get(n);
        Position start = _points.get(n);
        if (Math.abs(parameter - startD) < _parameters.getPrecision())
            return start;

        double endD = _parameterization.get(n + 1);
        Position end = _points.get(n+1);
        if (Math.abs(parameter-endD) < _parameters.getPrecision())
            return end;

        double pSeg = (parameter - startD) / (endD - startD);
        return interpolate(start, end, pSeg);
    }


    public void addStatistic (String statName, double stat) {
        if (null == _statistics)
            _statistics = new HashMap<String, Double>();

        _statistics.put(statName, stat);
    }

    public Map<String, Double> getStatistics () {
        return _statistics;
    }
}
