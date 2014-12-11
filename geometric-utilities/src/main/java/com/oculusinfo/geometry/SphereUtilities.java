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
package com.oculusinfo.geometry;

import com.oculusinfo.geometry.geodesic.Position;
import com.oculusinfo.math.algebra.AngleUtilities;
import com.oculusinfo.math.linearalgebra.Vector;

/**
 * A class of static methods acting on the unit sphere
 * 
 * In general for all these methods, by convention, we use the following
 * definitions
 * <dl>
 * <dt>theta
 * <dt>
 * <dd>The azimuth, that is, the angle of the projection of the point into the
 * X-Y plane when measured counterclockwise from the X axis. This would be the
 * same as the longitude, and should always be in radians, unless the method is
 * marked "Degrees"</dd>
 * <dt>phi</dt>
 * <dd>The polar angle, that is, the angle from the positive Z axis. This would
 * be the same as PI/2.0-latitude, and again, is always in radians unless the
 * method is marked "Degrees"</dd>
 * </dl>
 * 
 * @author nkronenfeld
 */
public class SphereUtilities {
    private static final double EPSILON = 1E-12;
    private static final Vector Z       = new Vector(0, 0, 1);
    private static final double HALF_PI = Math.PI / 2.0;

    private static boolean equal (double a, double b) {
        return Math.abs(a-b)<EPSILON;
    }

    public static Vector toUnitVector (double theta, double phi) {
        double sphi = Math.sin(phi);
        return new Vector(Math.cos(theta) * sphi, Math.sin(theta) * sphi,
                          Math.cos(phi));
    }
    private static double thetaFromUnitVector (Vector v) {
        return Math.atan2(v.coord(1), v.coord(0));
    }
    private static double phiFromUnitVector (Vector v) {
        double x = v.coord(0);
        double y = v.coord(1);
        double z = v.coord(2);
        return Math.atan2(Math.sqrt(x*x+y*y), z);
    }

    /**
     * Get the direction one must travel from A to get to B.
     * 
     * @param thetaA
     *            The azimuth of point A
     * @param phiA
     *            The polar angle of point A
     * @param thetaB
     *            The azimuth of point B
     * @param phiB
     *            The polar angle of point B
     * @return The angle from A to B, in radians, counterclockwise from due East
     */
    public static double getAzimuth (double thetaA, double phiA,
                                     double thetaB, double phiB) {
        if (equal(0.0, phiA))
            return -Math.PI / 2.0;
        if (equal(Math.PI, phiA))
            return Math.PI / 2.0;

        Vector A = toUnitVector(thetaA, phiA);
        Vector B = toUnitVector(thetaB, phiB);

        Vector AB = A.cross(B).cross(A);
        Vector AN = A.cross(Z).cross(A);
        Vector AE = AN.cross(A);

        double deltaX = AB.dot(AE);
        double deltaY = AB.dot(AN);
        return Math.atan2(deltaY, deltaX);
    }

    /**
     * Get the angular distance from A to B
     * @param thetaA
     *            The azimuth of point A
     * @param phiA
     *            The polar angle of point A
     * @param thetaB
     *            The azimuth of point B
     * @param phiB
     *            The polar angle of point B
     * @return The angular distance from A to B, in radians
     */
    public static double getDistance (double thetaA, double phiA,
                                      double thetaB, double phiB) {
        double cos = Math.sin(phiA)*Math.sin(phiB)*Math.cos(thetaA-thetaB) + Math.cos(phiA)*Math.cos(phiB);
        return Math.acos(cos);
    }
    public static double getDistance (Position a, Position b) {
        return getDistance(a.getLongitudeRadians(), HALF_PI-a.getLatitudeRadians(),
                           b.getLongitudeRadians(), HALF_PI-b.getLatitudeRadians());
    }

    /**
     * Get the angle ABC (i.e., the angle at point B between the lines BA and
     * BC)
     * 
     * @param thetaA
     *            The azimuth of point A
     * @param phiA
     *            The polar angle fo point A
     * @param thetaB
     *            The azimuth of point B
     * @param phiB
     *            The polar angle fo point B
     * @param thetaC
     *            The azimuth of point C
     * @param phiC
     *            The polar angle fo point C
     * @return The angle ABC, in radians
     */
    public static double getAngle (double thetaA, double phiA,
                                   double thetaB, double phiB,
                                   double thetaC, double phiC) {
        if (equal(0.0, phiB) || equal(Math.PI, phiB))
            return Math.abs(thetaA-thetaC);

        double dbc = getAzimuth(thetaB, phiB, thetaC, phiC);
        double dba = getAzimuth(thetaB, phiB, thetaA, phiA);
        dba = AngleUtilities.intoRangeRadians(dbc, dba);
        return Math.abs(dbc-dba);
    }
    /**
     * Get the angular area subtended by the triangle formed by the given three
     * points
     * 
     * @param thetaA
     *            The azimuth of point A
     * @param phiA
     *            The polar angle fo point A
     * @param thetaB
     *            The azimuth of point B
     * @param phiB
     *            The polar angle fo point B
     * @param thetaC
     *            The azimuth of point C
     * @param phiC
     *            The polar angle fo point C
     * @return The area subtended by the triangle (A, B, C), in steradians
     */
    public static double getTriangleArea (double thetaA, double phiA,
                                          double thetaB, double phiB,
                                          double thetaC, double phiC) {
        if (equal(thetaA, thetaB) && equal(phiA, phiB)) return 0.0;
        if (equal(thetaB, thetaC) && equal(phiB, phiC)) return 0.0;
        if (equal(thetaC, thetaA) && equal(phiC, phiA)) return 0.0;

        double da = getAngle(thetaC, phiC, thetaA, phiA, thetaB, phiB);
        double db = getAngle(thetaA, phiA, thetaB, phiB, thetaC, phiC);
        double dc = getAngle(thetaB, phiB, thetaC, phiC, thetaA, phiA);

        return (da+db+dc-Math.PI);
    }
    public static double getTriangleArea (Position a, Position b, Position c) {
        return getTriangleArea(a.getLongitudeRadians(),
                               HALF_PI-a.getLatitudeRadians(),
                               b.getLongitudeRadians(),
                               HALF_PI-b.getLatitudeRadians(),
                               c.getLongitudeRadians(),
                               HALF_PI-c.getLatitudeRadians());
    }

    /**
     * Interpolate from A to B
     * 
     * @param thetaA
     *            The azimuth of point A
     * @param phiA
     *            The polar angle of point A
     * @param thetaB
     *            The azimuth of point B
     * @param phiB
     *            The polar angle of point B
     * @param t
     *            The proportion of the way from A to B desired. t=0 should
     *            return A, t=1 should return B.
     * @return A vector containing the azimuth in coordinate 0 and the polar
     *         angle in coordinate 1.
     */
    public static Vector interpolate (double thetaA, double phiA,
                                      double thetaB, double phiB,
                                      double t) {
        // Using SLERP (Spherical Linear intERPolation)
        // Get the angle between the points
        double omega = getDistance(thetaA, phiA, thetaB, phiB);
        double sinOmega = Math.sin(omega);

        double coeffA = Math.sin((1-t)*omega)/sinOmega;
        double coeffB = Math.sin(t*omega)/sinOmega;

        Vector A = toUnitVector(thetaA, phiA);
        Vector B = toUnitVector(thetaB, phiB);
        Vector result = A.scale(coeffA).add(B.scale(coeffB));
        return new Vector(thetaFromUnitVector(result), phiFromUnitVector(result));
    }
    public static Position interpolate (Position a, Position b, double t) {
        Vector result = SphereUtilities.interpolate(a.getLongitudeRadians(), HALF_PI-a.getLatitudeRadians(),
                                                    b.getLongitudeRadians(), HALF_PI-b.getLatitudeRadians(),
                                                    t);
        Position p = new Position(Math.toDegrees(result.coord(0)),
                                  Math.toDegrees(HALF_PI-result.coord(1)));
        p.setPrecision(a.getPrecision());
        return p;
    }
}
