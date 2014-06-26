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

import com.oculusinfo.geometry.SphereUtilities;
import com.oculusinfo.geometry.geodesic.Position;
import com.oculusinfo.geometry.geodesic.PositionCalculationParameters;
import com.oculusinfo.geometry.geodesic.PositionCalculationType;
import com.oculusinfo.geometry.geodesic.Track;

import java.util.List;

public class SphericalTrack extends Track {

    public SphericalTrack (PositionCalculationParameters parameters,
                           Position... points) {
        super(parameters, points);
    }

    public SphericalTrack (PositionCalculationParameters parameters,
                           List<Position> points) {
        super(parameters, points);
    }

    public SphericalTrack (PositionCalculationParameters parameters,
                           List<Position> points,
                           List<Double> parameterization) {
        super(parameters, points, parameterization);
    }

    public SphericalTrack (Track oldTrack) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Spherical,
                                                oldTrack.getParameters().getAllowedError(),
                                                oldTrack.getParameters().getPrecision(),
                                                oldTrack.getParameters().ignoreDirection()));
    }

    public SphericalTrack (Track oldTrack, double allowedError, double precision, boolean ignoreDirection) {
        super(oldTrack,
              new PositionCalculationParameters(PositionCalculationType.Spherical, allowedError, precision, ignoreDirection));
    }

    @Override
    protected double getSegmentDistance (Position a, Position b) {
        return SphereUtilities.getDistance(a, b);
    }

    @Override
    protected Position interpolate (Position start, Position end, double t) {
        return SphereUtilities.interpolate(start, end, t);
    }

    @Override
    protected double getRelativeError (Position a, Position b, Position c) {
        double triangleArea = SphereUtilities.getTriangleArea(a, b, c);
        double longSide = SphereUtilities.getDistance(a, b);
        return triangleArea/(longSide*longSide);
    }

    @Override
    protected Track createTrack (List<Position> points) {
        return new SphericalTrack(getParameters(), points);
    }

    @Override
    protected Track createTrack (List<Position> points,
                                 List<Double> parameterization) {
        return new SphericalTrack(getParameters(), points, parameterization);
    }
}
