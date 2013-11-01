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

import java.awt.geom.Rectangle2D;

import junit.framework.Assert;

import org.junit.Test;

import com.oculusinfo.geometry.geodesic.Position;
import com.oculusinfo.math.linearalgebra.Vector;

public class PositionTests {
    private static final double EPSILON = 1E-12;

    @Test
    public void testOffsetCalculations () {
        Position A = new Position(0.0, 0.0);
        Position B = A.offset(56.487, 12.354);
        Assert.assertEquals(56.487, A.getAzimuth(B), EPSILON);
        Assert.assertEquals(12.354, A.getAngularDistance(B), EPSILON);

        A = new Position(-81.43, 42.12);
        B = A.offset(11.987, 123.456);
        Assert.assertEquals(11.987, A.getAzimuth(B), EPSILON);
        Assert.assertEquals(123.456, A.getAngularDistance(B), EPSILON);

        A = new Position(-81.43, 42.12);
        B = A.offset(-11.987, 123.456);
        Assert.assertEquals(-11.987, A.getAzimuth(B), EPSILON);
        Assert.assertEquals(123.456, A.getAngularDistance(B), EPSILON);

        A = new Position(-81.43, 42.12);
        B = A.offset(210.987, 123.456);
        Assert.assertEquals(-149.013, A.getAzimuth(B), EPSILON);
        Assert.assertEquals(123.456, A.getAngularDistance(B), EPSILON);

        A = new Position(-81.43, 42.12);
        B = A.offset(-11.987, 220.22);
        Assert.assertEquals(168.013, A.getAzimuth(B), EPSILON);
        Assert.assertEquals(139.78, A.getAngularDistance(B), EPSILON);

        A = new Position(0.0, 90.0);
        B = A.offset(180, 90);
        Assert.assertEquals(0.0, B.getLatitude(), EPSILON);

        A = new Position(0.0, -90.0);
        B = A.offset(0, 90);
        Assert.assertEquals(0.0, B.getLatitude(), EPSILON);

        A = new Position(0.0, 0.0);
        B = A.offset(90, 90);
        Assert.assertEquals(new Position(90.0, 0.0), B);
    }



    @Test
    public void testLineLatitudeIntersection () {
        // Tests finding of the point on the line between two points at a given latitude
        Position a = new Position(23.0, 12.0);
        Position b = new Position(45.0, 61.0);
        Position x = Position.greatCircleAtLatitude(a, b, 32.0);
        Assert.assertEquals(32.0, x.getLatitude(), EPSILON);
        double azAB = a.getAzimuth(b);
        double azAX = a.getAzimuth(x);
        Assert.assertEquals(azAB, azAX, EPSILON);
        double azBA = b.getAzimuth(a);
        double azBX = b.getAzimuth(x);
        Assert.assertEquals(azBA, azBX, EPSILON);
    }

    @Test
    public void testLineLongitudeIntersection () {
        // Tests finding of the point on the line between two points at a given latitude
        Position a = new Position(23.0, 12.0);
        Position b = new Position(45.0, 61.0);
        Position x = Position.greatCircleAtLongitude(a, b, 32.0);
        Assert.assertEquals(32.0, x.getLongitude(), EPSILON);
        double azAB = a.getAzimuth(b);
        double azAX = a.getAzimuth(x);
        Assert.assertEquals(azAB, azAX, EPSILON);
        double azBA = b.getAzimuth(a);
        double azBX = b.getAzimuth(x);
        Assert.assertEquals(azBA, azBX, EPSILON);
    }

    @Test
    public void testGreatCircleBounds () {
        // Take the great circle which is at 45 degrees at (0, 0). Take the
        // points 85 and 95 degrees ahead (up), so that the max is greater than
        // them, and make sure bounds includes the max.
        Position base = new Position(0.0, 0.0);
        Position p1 = base.offset(45.0, 85.0);
        Position p2 = base.offset(45.0, 95.0);
        Rectangle2D bounds = Position.getGreatCircleLonLatBounds(p1, p2);
        Assert.assertEquals(p1.getLongitude(), bounds.getMinX(), EPSILON);
        Assert.assertEquals(p2.getLongitude(), bounds.getMaxX(), EPSILON);
        Assert.assertEquals(p1.getLatitude(), bounds.getMinY(), EPSILON);
        Assert.assertEquals(45.0, bounds.getMaxY(), EPSILON);

        // Same thing, southern hemisphere - and start from some odd number around
        base = new Position(23.0, 0.0);
        p1 = base.offset(-45.0, 85.0);
        p2 = base.offset(-45.0, 95.0);
        bounds = Position.getGreatCircleLonLatBounds(p1, p2);
        Assert.assertEquals(p2.getLongitude(), bounds.getMinX(), EPSILON);
        Assert.assertEquals(p1.getLongitude(), bounds.getMaxX(), EPSILON);
        Assert.assertEquals(p1.getLatitude(), bounds.getMaxY(), EPSILON);
        Assert.assertEquals(-45.0, bounds.getMinY(), EPSILON);
    }


    @Test
    public void testConversionToCartesian () {
        double cartesianEpsilon = 1E-8; // Approximately a cm.

        Position pEq1 = new Position(0.0, 0.0);
        Vector v = pEq1.getAsCartesian();
        assertEquals(0.0, v.coord(0), cartesianEpsilon);
        assertEquals(0.0, v.coord(1), cartesianEpsilon);
        assertEquals(Position.WGS84_EQUATORIAL_RADIUS, v.coord(2), cartesianEpsilon);

        Position pEq2 = new Position(90.0, 0.0);
        v = pEq2.getAsCartesian();
        assertEquals(Position.WGS84_EQUATORIAL_RADIUS, v.coord(0), cartesianEpsilon);
        assertEquals(0.0, v.coord(1), cartesianEpsilon);
        assertEquals(0.0, v.coord(2), cartesianEpsilon);

        Position pEq3 = new Position(180.0, 0.0);
        v = pEq3.getAsCartesian();
        assertEquals(0.0, v.coord(0), cartesianEpsilon);
        assertEquals(0.0, v.coord(1), cartesianEpsilon);
        assertEquals(-Position.WGS84_EQUATORIAL_RADIUS, v.coord(2), cartesianEpsilon);

        Position pEq4 = new Position(270.0, 0.0);
        v = pEq4.getAsCartesian();
        assertEquals(-Position.WGS84_EQUATORIAL_RADIUS, v.coord(0), cartesianEpsilon);
        assertEquals(0.0, v.coord(1), cartesianEpsilon);
        assertEquals(0.0, v.coord(2), cartesianEpsilon);

        Position pEq5 = new Position(45.0, 0.0);
        v = pEq5.getAsCartesian();
        assertEquals(Position.WGS84_EQUATORIAL_RADIUS/Math.sqrt(2), v.coord(0), cartesianEpsilon);
        assertEquals(0.0, v.coord(1), cartesianEpsilon);
        assertEquals(Position.WGS84_EQUATORIAL_RADIUS/Math.sqrt(2), v.coord(2), cartesianEpsilon);


        Position pN = new Position(0.0, 90.0);
        v = pN.getAsCartesian();
        assertEquals(0.0,  v.coord(0), cartesianEpsilon);
        assertEquals(Position.WGS84_POLAR_RADIUS,  v.coord(1), cartesianEpsilon);
        assertEquals(0.0,  v.coord(2), cartesianEpsilon);

        Position pS = new Position(0.0, -90.0);
        v = pS.getAsCartesian();
        assertEquals(0.0,  v.coord(0), cartesianEpsilon);
        assertEquals(-Position.WGS84_POLAR_RADIUS,  v.coord(1), cartesianEpsilon);
        assertEquals(0.0,  v.coord(2), cartesianEpsilon);

        Position pHalfN = new Position(0.0, 45.0);
        v = pHalfN.getAsCartesian();
        assertEquals(0.0,  v.coord(0), cartesianEpsilon);
        // Really rough
        assertEquals(Position.WGS84_POLAR_RADIUS/Math.sqrt(2),  v.coord(1), 5E-3);
        assertEquals(Position.WGS84_EQUATORIAL_RADIUS/Math.sqrt(2), v.coord(2), 5E-3);
    }

    @Test
    public void testConversionFromCartesian () {
        double cartesianEpsilon = 1E-8; // Approximately a cm.

        Position pEq1 = new Position(0.0, 0.0, Position.WGS84_EQUATORIAL_RADIUS, true);
        assertEquals(0.0, pEq1.getLongitude(), cartesianEpsilon);
        assertEquals(0.0, pEq1.getLatitude(), cartesianEpsilon);
        
        Position pEq2 = new Position(Position.WGS84_EQUATORIAL_RADIUS, 0.0, 0.0, true);
        assertEquals(90.0, pEq2.getLongitude(), cartesianEpsilon);
        assertEquals(0.0, pEq2.getLatitude(), cartesianEpsilon);
        
        Position pEq3 = new Position(0.0, 0.0, -Position.WGS84_EQUATORIAL_RADIUS, true);
        assertEquals(-180.0, pEq3.getLongitude(), cartesianEpsilon);
        assertEquals(0.0, pEq3.getLatitude(), cartesianEpsilon);
        
        Position pEq4 = new Position(-Position.WGS84_EQUATORIAL_RADIUS, 0.0, 0.0, true);
        assertEquals(-90.0, pEq4.getLongitude(), cartesianEpsilon);
        assertEquals(0.0, pEq4.getLatitude(), cartesianEpsilon);
        
        Position pEq5 = new Position(Position.WGS84_EQUATORIAL_RADIUS/Math.sqrt(2), 0.0, 
                                 Position.WGS84_EQUATORIAL_RADIUS/Math.sqrt(2), true);
        assertEquals(45.0, pEq5.getLongitude(), cartesianEpsilon);
        assertEquals(0.0, pEq5.getLatitude(), cartesianEpsilon);


        Position pN = new Position(0.0, Position.WGS84_POLAR_RADIUS, 0.0, true);
        assertEquals(90.0, pN.getLatitude(), cartesianEpsilon);

        Position pS = new Position(0.0, -Position.WGS84_POLAR_RADIUS, 0.0, true);
        assertEquals(-90.0, pS.getLatitude(), cartesianEpsilon);

        Position pHalfN = new Position(0.0, 
                                   Position.WGS84_POLAR_RADIUS/Math.sqrt(2),
                                   Position.WGS84_EQUATORIAL_RADIUS/Math.sqrt(2),
                                   true);
        assertEquals(0.0, pHalfN.getLongitude(), cartesianEpsilon);
        // Really rough
        assertEquals(45.0, pHalfN.getLatitude(), 5E-3);
    }


    private static void assertEquals (double expected, double actual, double proportionalErrorAllowed) {
        if (Math.abs(expected) < 1.0) {
            Assert.assertEquals(expected, actual, proportionalErrorAllowed);
        } else {
            Assert.assertEquals(String.format("Expected %.4f, got %.4f", expected, actual),
                                0.0, (actual-expected)/expected, proportionalErrorAllowed);
        }
    }
}
