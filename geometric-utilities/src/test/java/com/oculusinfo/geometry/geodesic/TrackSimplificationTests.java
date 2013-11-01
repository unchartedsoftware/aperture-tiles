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

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.geometry.geodesic.tracks.GeodeticTrack;

public class TrackSimplificationTests {
    // Test to make sure intermediate colinear points are removed. 
    @Test
    public void testInlineRemoval () {
        Position p0 = new Position(4, 28);
        Position p1 = new Position(-17, 42);
        double azimuth = p0.getAzimuth(p1);
        double distance = p0.getAngularDistance(p1);
        Position pa = p0.offset(azimuth, 0.25*distance);
        Position pb = p0.offset(azimuth, 0.50*distance);
        Position pc = p0.offset(azimuth, 0.75*distance);

        PositionCalculationParameters params = new PositionCalculationParameters(PositionCalculationType.Geodetic, 1E-12, 1E-12, false);
        Track track = new GeodeticTrack(params , p0, pa, pb, pc, p1);
        Assert.assertEquals(2, track.getPoints().size());
    }

    @Test
    public void testNearInlineRemoval () {
        Position p0 = new Position(4, 28);
        Position p1 = new Position(-17, 42);
        double azimuth = p0.getAzimuth(p1);
        double distance = p0.getAngularDistance(p1);
        Position pa = p0.offset(azimuth, 0.25*distance).offset(azimuth+90, distance*0.01);
        Position pb = p0.offset(azimuth, 0.50*distance).offset(azimuth+90, distance*0.01);
        Position pc = p0.offset(azimuth, 0.75*distance).offset(azimuth+90, distance*0.01);

        PositionCalculationParameters params = new PositionCalculationParameters(PositionCalculationType.Geodetic, 0.01, 1E-12, false);
        Track track = new GeodeticTrack(params , p0, pa, pb, pc, p1);
        Assert.assertEquals(2, track.getPoints().size());

        params = new PositionCalculationParameters(PositionCalculationType.Geodetic, 0.001, 1E-12, false);
        track = new GeodeticTrack(params , p0, pa, pb, pc, p1);
        Assert.assertEquals(4, track.getPoints().size());
    }

}
