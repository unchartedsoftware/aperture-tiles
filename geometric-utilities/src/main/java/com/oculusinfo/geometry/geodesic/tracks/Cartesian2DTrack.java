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
package com.oculusinfo.geometry.geodesic.tracks;

import java.util.List;

import com.oculusinfo.geometry.geodesic.Position;
import com.oculusinfo.geometry.geodesic.PositionCalculationParameters;
import com.oculusinfo.geometry.geodesic.PositionCalculationType;
import com.oculusinfo.geometry.geodesic.Track;
import com.oculusinfo.math.linearalgebra.Vector;

public class Cartesian2DTrack extends Track {
    public Cartesian2DTrack (PositionCalculationParameters parameters,
                             Position... points) {
        super(parameters, points);
    }

    public Cartesian2DTrack (PositionCalculationParameters parameters,
                             List<Position> points) {
        super(parameters, points);
    }

    public Cartesian2DTrack (PositionCalculationParameters parameters,
                             List<Position> points,
                             List<Double> parameterization) {
        super(parameters, points, parameterization);
    }

    public Cartesian2DTrack (Track oldTrack) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Cartesian2D,
                                                oldTrack.getParameters().getAllowedError(),
                                                oldTrack.getParameters().getPrecision(),
                                                oldTrack.getParameters().ignoreDirection()));
    }

    public Cartesian2DTrack (Track oldTrack, double allowedError, double precision, boolean ignoreDirection) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Cartesian2D, allowedError, precision, ignoreDirection));
    }

    @Override
    protected double getSegmentDistance (Position start, Position end) {
        double dx = end.getLongitude()-start.getLongitude();
        double dy = end.getLatitude()-start.getLatitude();
        return Math.sqrt(dx*dx+dy*dy);
    }

    @Override
    protected Position interpolate (Position start, Position end, double t) {
        // start and end really should be Positions, but we're treating them as
        // straight vectors anyway.
        Position p = new Position((1-t)*start.getLongitude()+t*end.getLongitude(),
                                  (1-t)*start.getLatitude()+t*end.getLatitude());
        p.setPrecision(start.getPrecision());
        return p;
    }

    @Override
    protected double getRelativeError (Position a, Position b, Position c) {
        Vector vA = new Vector(a.getLongitude(), a.getLatitude());
        Vector vB = new Vector(b.getLongitude(), b.getLatitude());
        Vector vC = new Vector(c.getLongitude(), c.getLatitude());

        // a, b, and c really should be Positions, but we're treating them as
        // straight vectors anyway.
        Vector ac = vC.subtract(vA);
        Vector ab = vB.subtract(vA);
        double lac2 = ac.vectorLengthSquared();
        Vector bPerp = ab.subtract(ac.scale(ab.dot(ac)/lac2));
        double lbPerp = bPerp.vectorLength();
        double lac = Math.sqrt(lac2);

        return lbPerp/lac;
    }

    @Override
    protected Track createTrack (List<Position> points) {
        return new Cartesian2DTrack(getParameters(), points);
    }

    @Override
    protected Track createTrack (List<Position> points,
                                 List<Double> parameterization) {
        return new Cartesian2DTrack(getParameters(), points,
                                    parameterization);
    }
}
