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

import java.util.ArrayList;
import java.util.List;

import com.oculusinfo.geometry.geodesic.Position;
import com.oculusinfo.geometry.geodesic.PositionCalculationParameters;
import com.oculusinfo.geometry.geodesic.PositionCalculationType;
import com.oculusinfo.geometry.geodesic.Track;
import com.oculusinfo.math.linearalgebra.Vector;

public class Cartesian3DTrack extends Track {




    public Cartesian3DTrack (PositionCalculationParameters parameters,
                             Position... points) {
        super(parameters, points);
        fillInNeededPoints();
    }

    public Cartesian3DTrack (PositionCalculationParameters parameters,
                             List<Position> points) {
        super(parameters, points);
        fillInNeededPoints();
    }

    public Cartesian3DTrack (PositionCalculationParameters parameters,
                             List<Position> points,
                             List<Double> parameterization) {
        super(parameters, points, parameterization);
        fillInNeededPoints();
    }

    public Cartesian3DTrack (Track oldTrack) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Cartesian3D,
                                                oldTrack.getParameters().getAllowedError(),
                                                oldTrack.getParameters().getPrecision(),
                                                oldTrack.getParameters().ignoreDirection()));
        fillInNeededPoints();
    }

    public Cartesian3DTrack (Track oldTrack, double allowedError, double precision, boolean ignoreDirection) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Cartesian3D, allowedError, precision, ignoreDirection));
        fillInNeededPoints();
    }



    private void fillInNeededPoints () {
        List<Position> filledPoints = new ArrayList<Position>();
        List<Double> filledParameterization = new ArrayList<Double>();

        // Calculate the maximum chord length we're allowed with a given error
        // For a chord of angular length theta, the distance from the sphere
        // to the chord at the mid point is
        //     R (1 - cos(theta/2))
        // whereas the length of the chord is
        //     R sin(theta/2)
        // So the error ratio is
        //     (1 - cos(theta/2)) / sin(theta/2)
        // say psi = theta/2
        //     (1 - cos(psi)) / sin(psi)
        // we want to know the max psi for a given error ratio R
        //     R = (1 - cos(psi)) / sin(psi)
        // a = cos(psi)
        //       = (1 - a) / (sqrt(1-a^2)
        //     R sqrt(1-a^2) = 1-a
        //     R^2 (1-a^2) = 1 - 2a + a^2
        //     R^2 - R^2 a^2 = 1 - 2a + a^2
        //     (1+R^2) a^2 - 2 a + (1-R^2) = 0
        //     a = (2 +/- sqrt(4 - (1+R^2) (1-R^2))) / (2*(1+R^2))
        //       = (2 +/- sqrt(4 - (4 - R^4))) / (2 + 2R^2)
        //       = (2 +/- sqrt(4 - 4 + R^4)) / 2(1+R^2)
        //       = (2 +/- R^2) / (2 + 2 R^2)
        // A larger a yields a smaller psi, so we want the + branch
        
        double R = getParameters().getAllowedError();
        double a = (2+R*R)/(2+2*R*R);
        double psi = Math.acos(a);
        double theta = Math.toDegrees(2*psi);
        


        Position lastPos = null;
        double lastT = 0.0;
        List<Position> points = getPoints();
        List<Double> parameterization = getParameterization();
        for (int p=0; p < points.size(); ++p) {
            Position pos = points.get(p);
            double t = parameterization.get(p);

            if (null != lastPos) {
                double angularDistance = pos.getAngularDistance(lastPos);
                int minIncrements = (int) Math.ceil(angularDistance/theta);
                if (minIncrements > 1) {
                    // We need to add points in between to maintain our minimum
                    // error. Just space them evenly.
                    double timeDistance = t-lastT;
                    double azimuth = lastPos.getAzimuth(pos);
                    for (int i=1; i<minIncrements; ++i) {
                        double incrementDistance = i*angularDistance/minIncrements;
                        Position incrementPosition = lastPos.offset(azimuth, incrementDistance);
                        filledPoints.add(incrementPosition);
                        filledParameterization.add(lastT+i*timeDistance/minIncrements);
                    }
                }
            }
            filledPoints.add(pos);
            filledParameterization.add(t);

            lastPos = pos;
            lastT = t;
        }

        updatePoints(filledPoints, filledParameterization);
    }



    @Override
    protected double getSegmentDistance (Position start, Position end) {
        return start.getCartesianDistance(end);
    }

    @Override
    protected Position interpolate (Position start, Position end, double t) {
        Vector sV = start.getAsCartesian();
        Vector eV = end.getAsCartesian();
        Vector cartesianResult = sV.scale(1 - t).add(eV.scale(t));
        Position p = new Position(cartesianResult.coord(0), cartesianResult.coord(1),
                                  cartesianResult.coord(2), !start.hasElevation());
        p.setPrecision(start.getPrecision());
        return p;
    }

    @Override
    protected double getRelativeError (Position a, Position b, Position c) {
        Vector vA = a.getAsCartesian();
        Vector vB = b.getAsCartesian();
        Vector vC = c.getAsCartesian();

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
        return new Cartesian3DTrack(getParameters(), points);
    }

    @Override
    protected Track createTrack (List<Position> path,
                                 List<Double> parameterization) {
        return new Cartesian3DTrack(getParameters(), path, parameterization);
    }
}
