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

import com.oculusinfo.math.linearalgebra.Vector;
import junit.framework.Assert;
import org.junit.Test;

public class SphereUtilityTests {
    private static final double EPSILON = 1E-12;
    @Test
    public void testSphereUnitVectorCalculation () {
        Assert.assertEquals(new Vector( 0,  0,  1), SphereUtilities.toUnitVector(0, 0));
        Assert.assertEquals(new Vector( 0,  0, -1), SphereUtilities.toUnitVector(0, Math.PI));
        Assert.assertEquals(new Vector( 1,  0,  0), SphereUtilities.toUnitVector(0, Math.PI/2.0));
        Assert.assertEquals(new Vector(-1,  0,  0), SphereUtilities.toUnitVector(Math.PI, Math.PI/2.0));
        Assert.assertEquals(new Vector( 0,  1,  0), SphereUtilities.toUnitVector(Math.PI/2.0, Math.PI/2.0));
        Assert.assertEquals(new Vector( 0, -1,  0), SphereUtilities.toUnitVector(-Math.PI/2.0, Math.PI/2.0));
    }

    @Test
    public void testAzimuth () {
        double frontTheta = 0;
        double frontPhi = Math.PI/2.0;
        double rightTheta = Math.PI/2.0;
        double rightPhi = Math.PI/2.0;
        double northTheta = 0;
        double northPhi = 0;
        double southTheta = 0;
        double southPhi = Math.PI;

        Assert.assertEquals(0, SphereUtilities.getAzimuth(frontTheta, frontPhi, rightTheta, rightPhi), EPSILON);
        Assert.assertEquals(Math.PI, SphereUtilities.getAzimuth(rightTheta, rightPhi, frontTheta, frontPhi), EPSILON);

        Assert.assertEquals(Math.PI/2.0, SphereUtilities.getAzimuth(frontTheta, frontPhi, northTheta, northPhi), EPSILON);
        Assert.assertEquals(Math.PI/2.0, SphereUtilities.getAzimuth(rightTheta, rightPhi, northTheta, northPhi), EPSILON);
        Assert.assertEquals(-Math.PI/2.0, SphereUtilities.getAzimuth(northTheta, northPhi, frontTheta, frontPhi), EPSILON);
        Assert.assertEquals(-Math.PI/2.0, SphereUtilities.getAzimuth(northTheta, northPhi, rightTheta, rightPhi), EPSILON);

        Assert.assertEquals(-Math.PI/2.0, SphereUtilities.getAzimuth(frontTheta, frontPhi, southTheta, southPhi), EPSILON);
        Assert.assertEquals(-Math.PI/2.0, SphereUtilities.getAzimuth(rightTheta, rightPhi, southTheta, southPhi), EPSILON);
        Assert.assertEquals(Math.PI/2.0, SphereUtilities.getAzimuth(southTheta, southPhi, frontTheta, frontPhi), EPSILON);
        Assert.assertEquals(Math.PI/2.0, SphereUtilities.getAzimuth(southTheta, southPhi, rightTheta, rightPhi), EPSILON);

        Assert.assertEquals(Math.PI/4.0, SphereUtilities.getAzimuth(0.0*Math.PI, 0.50*Math.PI, 0.5*Math.PI, 0.25*Math.PI), EPSILON);
        Assert.assertEquals(-Math.PI, SphereUtilities.getAzimuth(0.50*Math.PI, 0.25*Math.PI, 0.0*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(-Math.PI/4.0, SphereUtilities.getAzimuth(0.0*Math.PI, 0.50*Math.PI, 0.5*Math.PI, 0.75*Math.PI), EPSILON);
        Assert.assertEquals(Math.PI, SphereUtilities.getAzimuth(0.50*Math.PI, 0.75*Math.PI, 0.0*Math.PI, 0.50*Math.PI), EPSILON);
    }

    @Test
    public void testDistance () {
        Assert.assertEquals(0.00*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.10*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.10*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.20*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.20*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.30*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.30*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.40*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.40*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.50*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI), EPSILON);

        Assert.assertEquals(0.50*Math.PI, SphereUtilities.getDistance(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.25*Math.PI), EPSILON);
    }

    @Test
    public void testTriangleArea () {
        // Polar pie slices
        Assert.assertEquals(0.00*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.10*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.50*Math.PI, 0.10*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.20*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.50*Math.PI, 0.20*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.30*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.50*Math.PI, 0.30*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.40*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.50*Math.PI, 0.40*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.50*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI), EPSILON);

        // Equatorial pie slices
        Assert.assertEquals(0.00*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI), EPSILON);
        Assert.assertEquals(0.10*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.40*Math.PI), EPSILON);
        Assert.assertEquals(0.20*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.30*Math.PI), EPSILON);
        Assert.assertEquals(0.30*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.20*Math.PI), EPSILON);
        Assert.assertEquals(0.40*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.10*Math.PI), EPSILON);
        Assert.assertEquals(0.50*Math.PI, SphereUtilities.getTriangleArea(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.00*Math.PI), EPSILON);
    }

    @Test
    public void testInterpolation () {
        Assert.assertEquals(new Vector(0.00*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.0));
        Assert.assertEquals(new Vector(0.10*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.2));
        Assert.assertEquals(new Vector(0.20*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.4));
        Assert.assertEquals(new Vector(0.30*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.6));
        Assert.assertEquals(new Vector(0.40*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.8));
        Assert.assertEquals(new Vector(0.50*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 0.50*Math.PI, 1.0));

        Assert.assertEquals(new Vector(0.00*Math.PI, 0.5*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.0));
        Assert.assertEquals(new Vector(0.00*Math.PI, 0.4*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.2));
        Assert.assertEquals(new Vector(0.00*Math.PI, 0.3*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.4));
        Assert.assertEquals(new Vector(0.00*Math.PI, 0.2*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.6));
        Assert.assertEquals(new Vector(0.00*Math.PI, 0.1*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 0.8));
        Assert.assertEquals(new Vector(0.00*Math.PI, 0.0*Math.PI), SphereUtilities.interpolate(0.00*Math.PI, 0.50*Math.PI, 0.00*Math.PI, 0.00*Math.PI, 1.0));
    }
}
