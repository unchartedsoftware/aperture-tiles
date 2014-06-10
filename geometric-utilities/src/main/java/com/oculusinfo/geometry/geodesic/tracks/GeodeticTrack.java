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
package com.oculusinfo.geometry.geodesic.tracks;

import com.oculusinfo.geometry.geodesic.Position;
import com.oculusinfo.geometry.geodesic.PositionCalculationParameters;
import com.oculusinfo.geometry.geodesic.PositionCalculationType;
import com.oculusinfo.geometry.geodesic.Track;
import com.oculusinfo.math.algebra.AngleUtilities;

import java.util.List;

public class GeodeticTrack extends Track {
    public GeodeticTrack (PositionCalculationParameters parameters,
                          Position... points) {
        super(parameters, points);
    }

    public GeodeticTrack (PositionCalculationParameters parameters,
                          List<Position> points) {
        super(parameters, points);
    }

    public GeodeticTrack (PositionCalculationParameters parameters,
                          List<Position> points,
                          List<Double> parameterization) {
        super(parameters, points, parameterization);
    }

    public GeodeticTrack (Track oldTrack) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Geodetic,
                                                oldTrack.getParameters().getAllowedError(),
                                                oldTrack.getParameters().getPrecision(),
                                                oldTrack.getParameters().ignoreDirection()));
    }

    public GeodeticTrack (Track oldTrack, double allowedError, double precision, boolean ignoreDirection) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Geodetic, allowedError, precision, ignoreDirection));
    }

    @Override
    protected double getSegmentDistance (Position start, Position end) {
        return start.getAngularDistance(end);
    }

    @Override
    protected Position interpolate (Position start, Position end, double t) {
        if (null == start || null == end)
            return null;

        double lenSeg = start.getAngularDistance(end);
        double azSeg = start.getAzimuth(end);
        return start.offset(azSeg, lenSeg * t);
//        Vector vs = start.getAsCartesian();
//        Vector ve = end.getAsCartesian();
//        Vector vavg = vs.add(ve.subtract(vs).scale(t));
//        Position p = new Position(vavg.coord(0), vavg.coord(1), vavg.coord(2), true);
//        if (start.hasElevation()) {
//            double es = start.getElevation();
//            double ee = end.getElevation();
//            double e = es + (ee-es)*t;
//            p = new Position(p.getLongitude(), p.getLatitude(), e);
//        } else {
//            p = new Position(p.getLongitude(), p.getLatitude());
//        }
//        return p;
    }

    @Override
    protected double getRelativeError (Position a, Position b, Position c) {
        // Get the area of the triangle subtended by the three, divided by
        // the square of the length of AC
        double dab = a.getAzimuth(b);
        double dac = AngleUtilities.intoRangeDegrees(dab, a.getAzimuth(c));
        double da = Math.abs(dab-dac);
        double dbc = b.getAzimuth(c);
        double dba = AngleUtilities.intoRangeDegrees(dbc, b.getAzimuth(a));
        double db = Math.abs(dbc-dba);
        double dca = c.getAzimuth(a);
        double dcb = AngleUtilities.intoRangeDegrees(dca, c.getAzimuth(b));
        double dc = Math.abs(dca-dcb);

        double triangleArea = Math.toRadians(da+db+dc-180); // area of triangle/r62
        double longSide = Math.toRadians(a.getAngularDistance(c)); // length of long side
        return triangleArea/(longSide*longSide);
    }

    @Override
    protected Track createTrack (List<Position> points) {
        return new GeodeticTrack(getParameters(), points);
    }

    @Override
    protected Track createTrack (List<Position> points,
                                 List<Double> parameterization) {
        return new GeodeticTrack(getParameters(), points,
                                 parameterization);
    }
}
