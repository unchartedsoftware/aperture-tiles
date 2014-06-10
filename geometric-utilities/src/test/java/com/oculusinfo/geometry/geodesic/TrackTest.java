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
package com.oculusinfo.geometry.geodesic;

import com.oculusinfo.geometry.geodesic.tracks.GeodeticTrack;
import junit.framework.Assert;
import org.junit.Test;

public class TrackTest {
    private static final double EPSILON = 1E-12;
    private static final PositionCalculationParameters GEODETIC_PARAMETERS =
            new PositionCalculationParameters(PositionCalculationType.Geodetic, 0.0001, 1E-12, false);

    @Test
    public void testTrackDistance () {

        Position p0 = new Position(0, 0);
        Position p05 = new Position(0, 0.5);
        Position p1 = new Position(0, 1);
        Position p15 = new Position(0, 1.5);
        Position p2 = new Position(0, 2);
        Position p3 = new Position(0, 3);
        Position p4 = new Position(0, 4);
        double offset = p0.getAngularDistance(p1);

        GeodeticTrack base = new GeodeticTrack(GEODETIC_PARAMETERS, p0, p1, p2);
        GeodeticTrack unoffset = new GeodeticTrack(GEODETIC_PARAMETERS, p0, p1, p2);
        GeodeticTrack diffPoints = new GeodeticTrack(GEODETIC_PARAMETERS, p0, p05, p15, p2);
        GeodeticTrack hoffset = new GeodeticTrack(GEODETIC_PARAMETERS, p1, p2, p3);
        GeodeticTrack hoffset2 = new GeodeticTrack(GEODETIC_PARAMETERS, p2, p3, p4);
        GeodeticTrack voffset = new GeodeticTrack(GEODETIC_PARAMETERS, p0.offset(0, offset), p1.offset(0, offset),
                                        p2.offset(0, offset));

        // Check each distance in both directions
        Assert.assertEquals(0.0, base.getDistance(unoffset), EPSILON);
        Assert.assertEquals(0.0, unoffset.getDistance(base), EPSILON);

        Assert.assertEquals(0.0, base.getDistance(diffPoints), EPSILON);
        Assert.assertEquals(0.0, diffPoints.getDistance(base), EPSILON);

        double hoffsetDistance = base.getDistance(hoffset);
        Assert.assertTrue(hoffsetDistance > EPSILON);
        Assert.assertEquals(hoffsetDistance, hoffset.getDistance(base), EPSILON);

        Assert.assertEquals(hoffsetDistance, base.getDistance(voffset), EPSILON);
        Assert.assertEquals(hoffsetDistance, voffset.getDistance(base), EPSILON);

        Assert.assertEquals(hoffsetDistance, base.getDistance(hoffset2) / 2.0,
                            EPSILON);
        Assert.assertEquals(hoffsetDistance, hoffset2.getDistance(base) / 2.0,
                            EPSILON);
    }

    @Test
    public void testScaledTrackDistance () {
        Position p0 = new Position(0, 0);
        Position p1 = new Position(0, 1);
        Position p2 = new Position(0, 2);
        Position p4 = new Position(0, 4);

        double offset = p0.getAngularDistance(p1) / 10.0;
        GeodeticTrack smallA = new GeodeticTrack(GEODETIC_PARAMETERS,
                                                 p0.offset(0, offset),
                                                 p1.offset(0, offset),
                                                 p2.offset(0, offset));
        GeodeticTrack smallB = new GeodeticTrack(GEODETIC_PARAMETERS,
                                                 p0.offset(180, offset),
                                                 p1.offset(180, offset),
                                                 p2.offset(180, offset));

        GeodeticTrack thinA = new GeodeticTrack(GEODETIC_PARAMETERS,
                                                p0.offset(0, offset),
                                                p2.offset(0, offset),
                                                p4.offset(0, offset));
        GeodeticTrack thinB = new GeodeticTrack(GEODETIC_PARAMETERS,
                                                p0.offset(180, offset),
                                                p2.offset(180, offset),
                                                p4.offset(180, offset));

        GeodeticTrack largeA = new GeodeticTrack(GEODETIC_PARAMETERS,
                                                 p0.offset(0, 2 * offset),
                                                 p2.offset(0, 2 * offset),
                                                 p4.offset(0, 2 * offset));
        GeodeticTrack largeB = new GeodeticTrack(GEODETIC_PARAMETERS,
                                                 p0.offset(180, 2 * offset),
                                                 p2.offset(180, 2 * offset),
                                                 p4.offset(180, 2 * offset));

        double smallD = smallA.getDistance(smallB);
        double thinD = thinA.getDistance(thinB);
        double largeD = largeA.getDistance(largeB);

        Assert.assertEquals(smallD, largeD, EPSILON);
        Assert.assertEquals(smallD, thinD * 2.0, EPSILON);
    }


    @Test
    public void testTrackAveraging () {
        Position pa0 = new Position(0.0, 0.0);
        Position pa1 = new Position(1.0, 0.0);
        Position pa2 = new Position(2.0, 0.0);
        Track ta = new GeodeticTrack(GEODETIC_PARAMETERS, pa0, pa1, pa2);
        Position pb0 = new Position(0.0, 1.0);
        Position pb1 = new Position(1.0, 1.0);
        Position pb2 = new Position(2.0, 1.0);
        Track tb = new GeodeticTrack(GEODETIC_PARAMETERS, pb0, pb1, pb2);
        Position pc0 = new Position(0.0, -1.0);
        Position pc1 = new Position(1.0, -1.0);
        Position pc2 = new Position(2.0, -1.0);
        Track tc = new GeodeticTrack(GEODETIC_PARAMETERS, pc0, pc1, pc2);

        Assert.assertEquals(ta, tb.weightedAverage(tc, 1, 1));
        Assert.assertEquals(ta, tc.weightedAverage(tb, 1, 1));
        Assert.assertEquals(ta, ta.weightedAverage(tb.weightedAverage(tc, 1, 1), 1, 2));
        Assert.assertEquals(ta, ta.weightedAverage(tc.weightedAverage(tb, 1, 1), 1, 2));
        Assert.assertEquals(ta, tb.weightedAverage(ta.weightedAverage(tc, 1, 1), 1, 2));
        Assert.assertEquals(ta, tb.weightedAverage(tc.weightedAverage(ta, 1, 1), 1, 2));
        Assert.assertEquals(ta, tc.weightedAverage(ta.weightedAverage(tb, 1, 1), 1, 2));
        Assert.assertEquals(ta, tc.weightedAverage(tb.weightedAverage(ta, 1, 1), 1, 2));
    }



    @Test
    public void testProblemCase000 () {
        Track testTrack = new GeodeticTrack(GEODETIC_PARAMETERS,
                                            new Position(104.100900, 1.254993),
                                            new Position(-70.739240, -23.433430),
                                            new Position(-70.714260, -23.467820),
                                            new Position(-170.580200, 7.582428),
                                            new Position(-170.580200, 7.582428),
                                            new Position(131.741700, 33.995240),
                                            new Position(131.767800, 33.927540),
                                            new Position(131.788900, 33.876170),
                                            new Position(131.797500, 33.853820),
                                            new Position(131.805600, 33.831810),
                                            new Position(131.808700, 33.822670),
                                            new Position(131.822100, 33.782880),
                                            new Position(131.855500, 33.695430),
                                            new Position(131.864100, 33.674860),
                                            new Position(131.895900, 33.575180),
                                            new Position(131.923300, 33.496730),
                                            new Position(131.937100, 33.461940),
                                            new Position(131.980000, 33.345490),
                                            new Position(132.129700, 33.144430),
                                            new Position(132.167700, 33.096230),
                                            new Position(132.172600, 33.089670),
                                            new Position(132.211200, 33.039430),
                                            new Position(132.221400, 33.026410),
                                            new Position(134.633500, 33.051310),
                                            new Position(134.661000, 33.057940),
                                            new Position(134.786800, 33.087150),
                                            new Position(135.074300, 33.155940),
                                            new Position(135.293600, 33.206040),
                                            new Position(135.328200, 33.213520),
                                            new Position(135.351300, 33.218400),
                                            new Position(135.556200, 33.268330),
                                            new Position(135.794400, 33.322950),
                                            new Position(135.833400, 33.339650),
                                            new Position(135.987000, 33.404810),
                                            new Position(136.033800, 33.422660),
                                            new Position(136.033800, 33.422660),
                                            new Position(136.080700, 33.439760),
                                            new Position(136.125900, 33.454980),
                                            new Position(136.159700, 33.466010),
                                            new Position(136.205600, 33.480520),
                                            new Position(136.249400, 33.494620),
                                            new Position(136.310700, 33.516810),
                                            new Position(136.393800, 33.548510),
                                            new Position(136.859200, 33.716100),
                                            new Position(137.601400, 33.986450),
                                            new Position(140.175900, 34.824590),
                                            new Position(140.203800, 34.838630),
                                            new Position(140.246500, 34.860540),
                                            new Position(140.279000, 34.876520),
                                            new Position(140.306500, 34.890130),
                                            new Position(140.335100, 34.904310),
                                            new Position(140.360200, 34.916450),
                                            new Position(-126.333500, 48.491730),
                                            new Position(-126.281300, 48.489250),
                                            new Position(-126.238900, 48.485860),
                                            new Position(-126.186300, 48.483170),
                                            new Position(-126.144200, 48.481200),
                                            new Position(-126.096200, 48.479970),
                                            new Position(-126.043400, 48.477480),
                                            new Position(-125.990500, 48.475520),
                                            new Position(-125.953800, 48.475570),
                                            new Position(-125.902600, 48.475930),
                                            new Position(-125.856800, 48.476150),
                                            new Position(-125.797300, 48.475530),
                                            new Position(-125.755200, 48.474490),
                                            new Position(-125.717300, 48.473670),
                                            new Position(-125.668500, 48.473270),
                                            new Position(-125.618100, 48.473490),
                                            new Position(-125.578200, 48.473920),
                                            new Position(-125.527600, 48.474450),
                                            new Position(-125.479400, 48.474500),
                                            new Position(-125.430900, 48.474960),
                                            new Position(-125.381100, 48.476290),
                                            new Position(-125.345700, 48.477190),
                                            new Position(-125.296400, 48.475930),
                                            new Position(-125.253500, 48.474870),
                                            new Position(-125.202900, 48.474420),
                                            new Position(-125.155000, 48.474720),
                                            new Position(-125.116300, 48.475270),
                                            new Position(-125.060100, 48.476590),
                                            new Position(-125.025400, 48.476830),
                                            new Position(-124.989300, 48.475700),
                                            new Position(-124.942600, 48.474940),
                                            new Position(-124.877100, 48.474450),
                                            new Position(-124.836400, 48.474530),
                                            new Position(-124.785000, 48.474320),
                                            new Position(-124.728000, 48.475910),
                                            new Position(-124.693400, 48.467170),
                                            new Position(-124.651200, 48.453630),
                                            new Position(-124.608700, 48.439580),
                                            new Position(-124.567800, 48.425600),
                                            new Position(-124.525600, 48.412520),
                                            new Position(-124.493500, 48.402770),
                                            new Position(-124.445100, 48.387520),
                                            new Position(-124.402200, 48.374150),
                                            new Position(-124.360900, 48.361360),
                                            new Position(-124.325300, 48.350200),
                                            new Position(-124.281500, 48.336180),
                                            new Position(-124.250300, 48.326270),
                                            new Position(-124.201600, 48.311290),
                                            new Position(-124.159000, 48.298090),
                                            new Position(-124.126200, 48.287920),
                                            new Position(-124.078000, 48.272860),
                                            new Position(-124.044300, 48.262480),
                                            new Position(-123.997700, 48.247750),
                                            new Position(-123.962600, 48.236770),
                                            new Position(-123.924500, 48.226100),
                                            new Position(-123.884300, 48.225100),
                                            new Position(-123.853700, 48.223330),
                                            new Position(-123.815800, 48.221380),
                                            new Position(-123.786300, 48.220650),
                                            new Position(-123.750100, 48.220720),
                                            new Position(-123.722000, 48.221090),
                                            new Position(-123.685300, 48.221730),
                                            new Position(-123.649800, 48.222570),
                                            new Position(-123.611600, 48.223480),
                                            new Position(-123.584300, 48.224450),
                                            new Position(-123.547100, 48.226210),
                                            new Position(-123.512600, 48.227150),
                                            new Position(-123.486900, 48.229390),
                                            new Position(-123.456800, 48.244900),
                                            new Position(-123.445700, 48.257940),
                                            new Position(-123.432500, 48.272880),
                                            new Position(-123.424000, 48.285650),
                                            new Position(-123.415700, 48.299290),
                                            new Position(-123.410100, 48.312630),
                                            new Position(-123.404800, 48.328360),
                                            new Position(-123.400100, 48.338230),
                                            new Position(-123.395100, 48.346930),
                                            new Position(-123.167300, 48.455230),
                                            new Position(-123.177200, 48.492000),
                                            new Position(-123.187300, 48.512330),
                                            new Position(-123.202200, 48.538830),
                                            new Position(-123.215600, 48.569960),
                                            new Position(-123.221000, 48.588910),
                                            new Position(-123.221100, 48.616000),
                                            new Position(-123.222900, 48.648450),
                                            new Position(-123.244600, 48.675360),
                                            new Position(-123.247900, 48.698350),
                                            new Position(-123.211300, 48.710220),
                                            new Position(-123.135500, 48.731840),
                                            new Position(-123.102900, 48.742890),
                                            new Position(-123.078100, 48.750290),
                                            new Position(-123.041800, 48.763900),
                                            new Position(-123.011700, 48.777790),
                                            new Position(-122.990900, 48.792760),
                                            new Position(-122.993000, 48.814900),
                                            new Position(-123.015800, 48.839150),
                                            new Position(-123.040100, 48.864490),
                                            new Position(-123.168800, 48.955790),
                                            new Position(-123.201900, 48.978820),
                                            new Position(-123.340200, 49.095520),
                                            new Position(-123.336100, 49.117770),
                                            new Position(-123.327900, 49.148310),
                                            new Position(-123.319200, 49.216650),
                                            new Position(-123.320700, 49.249960),
                                            new Position(-123.308500, 49.272420),
                                            new Position(-123.271200, 49.295910),
                                            new Position(-123.240400, 49.304440),
                                            new Position(-123.178900, 49.313730),
                                            new Position(-123.151600, 49.316160),
                                            new Position(-123.112300, 49.304550),
                                            new Position(-123.116100, 49.307700),
                                            new Position(-123.117700, 49.308820),
                                            new Position(-123.130500, 49.312120),
                                            new Position(-123.161700, 49.318730),
                                            new Position(-123.194700, 49.319700),
                                            new Position(-123.235600, 49.318720),
                                            new Position(-123.278700, 49.313200),
                                            new Position(-123.358200, 49.221150),
                                            new Position(-123.369300, 49.195370),
                                            new Position(-123.379300, 49.171400),
                                            new Position(-123.392000, 49.141190),
                                            new Position(-123.400100, 49.113110),
                                            new Position(-123.402500, 49.091700),
                                            new Position(-123.387800, 49.063320),
                                            new Position(-123.374900, 49.043050),
                                            new Position(-123.346900, 49.020180),
                                            new Position(-123.316600, 48.999950),
                                            new Position(-123.285200, 48.980320),
                                            new Position(-123.254800, 48.961590),
                                            new Position(-123.164800, 48.905630),
                                            new Position(-123.134800, 48.886670),
                                            new Position(-123.112500, 48.871790),
                                            new Position(-123.084100, 48.852640),
                                            new Position(-123.055900, 48.832130),
                                            new Position(-123.022800, 48.811230),
                                            new Position(-123.024800, 48.787990),
                                            new Position(-123.052500, 48.772240),
                                            new Position(-123.087400, 48.759970),
                                            new Position(-123.122000, 48.746750),
                                            new Position(-123.149600, 48.736410),
                                            new Position(-123.189100, 48.724230),
                                            new Position(-123.211700, 48.715780),
                                            new Position(-123.246200, 48.705010),
                                            new Position(-123.258800, 48.685790),
                                            new Position(-123.253500, 48.660550),
                                            new Position(-123.249300, 48.642900),
                                            new Position(-123.242900, 48.619780),
                                            new Position(-123.234900, 48.595850),
                                            new Position(-123.229600, 48.570850),
                                            new Position(-123.224000, 48.546440),
                                            new Position(-123.221300, 48.521020),
                                            new Position(-123.218600, 48.492260),
                                            new Position(-123.210700, 48.460400),
                                            new Position(-123.205400, 48.430700),
                                            new Position(-123.230000, 48.408860),
                                            new Position(-123.270100, 48.397240),
                                            new Position(-123.302000, 48.386980),
                                            new Position(-123.340100, 48.388170),
                                            new Position(-123.368100, 48.392020),
                                            new Position(-123.390300, 48.389450),
                                            new Position(-123.424000, 48.362590),
                                            new Position(-123.444900, 48.342620),
                                            new Position(-123.465800, 48.314080),
                                            new Position(-123.480200, 48.283510),
                                            new Position(-123.502500, 48.254180),
                                            new Position(-123.536500, 48.250290),
                                            new Position(-123.583000, 48.258740),
                                            new Position(-123.621700, 48.268220),
                                            new Position(-123.660100, 48.276440),
                                            new Position(-123.706100, 48.284060),
                                            new Position(-123.744900, 48.287610),
                                            new Position(-123.795900, 48.289010),
                                            new Position(-123.840100, 48.289850),
                                            new Position(-123.889200, 48.293190),
                                            new Position(-123.923500, 48.295680),
                                            new Position(-123.966100, 48.305640),
                                            new Position(-123.999500, 48.316110),
                                            new Position(-124.054600, 48.334000),
                                            new Position(-124.086100, 48.344200),
                                            new Position(-124.124800, 48.355270),
                                            new Position(-124.179100, 48.368750),
                                            new Position(-124.223300, 48.377300),
                                            new Position(-124.257400, 48.383670),
                                            new Position(-124.304600, 48.393230),
                                            new Position(-124.350700, 48.405730),
                                            new Position(-124.385500, 48.417570),
                                            new Position(-124.427600, 48.433570),
                                            new Position(-124.466400, 48.447700),
                                            new Position(-124.511300, 48.461730),
                                            new Position(-124.559100, 48.475420),
                                            new Position(-124.591100, 48.483880),
                                            new Position(-124.636800, 48.494910),
                                            new Position(-124.671200, 48.502540),
                                            new Position(-124.721700, 48.513100),
                                            new Position(-124.767400, 48.514120),
                                            new Position(-124.804600, 48.513730),
                                            new Position(-124.843300, 48.513310),
                                            new Position(-124.895000, 48.517450),
                                            new Position(-124.936300, 48.519690),
                                            new Position(-124.984300, 48.515400),
                                            new Position(-125.022800, 48.513220),
                                            new Position(-125.059800, 48.510930),
                                            new Position(-125.112900, 48.507970),
                                            new Position(-125.148800, 48.507470),
                                            new Position(-125.196600, 48.504410),
                                            new Position(-125.245100, 48.502920),
                                            new Position(-125.283100, 48.501440),
                                            new Position(-125.327400, 48.499710),
                                            new Position(-125.364400, 48.499790),
                                            new Position(-125.464000, 48.499980),
                                            new Position(-125.512800, 48.502790),
                                            new Position(-125.549300, 48.505870),
                                            new Position(-125.810700, 48.543510),
                                            new Position(-126.455900, 48.640510),
                                            new Position(-126.496600, 48.646410),
                                            new Position(-126.535500, 48.652510),
                                            new Position(-126.572500, 48.658440),
                                            new Position(-126.623700, 48.665610),
                                            new Position(-126.644400, 48.667710),
                                            new Position(-126.705800, 48.675400),
                                            new Position(-126.726000, 48.678830),
                                            new Position(-126.781700, 48.688590),
                                            new Position(-165.453100, 54.393850),
                                            new Position(-166.934400, 54.447530),
                                            new Position(-167.159900, 54.445010),
                                            new Position(-167.434600, 54.440690),
                                            new Position(147.573800, 43.888900),
                                            new Position(147.536600, 43.867060),
                                            new Position(147.402600, 43.796630),
                                            new Position(146.813000, 43.479550),
                                            new Position(146.781000, 43.460320),
                                            new Position(146.709400, 43.413670),
                                            new Position(146.458400, 43.263400),
                                            new Position(145.367900, 42.741540),
                                            new Position(145.309400, 42.715260),
                                            new Position(145.270200, 42.694840),
                                            new Position(145.247600, 42.684040),
                                            new Position(145.211100, 42.669760),
                                            new Position(145.191000, 42.663870),
                                            new Position(145.152500, 42.648370),
                                            new Position(145.119200, 42.633690),
                                            new Position(145.085900, 42.619230),
                                            new Position(145.062200, 42.609560),
                                            new Position(145.029800, 42.596120),
                                            new Position(144.993000, 42.580060),
                                            new Position(144.218900, 42.248350),
                                            new Position(144.133200, 42.214290),
                                            new Position(144.104000, 42.203180),
                                            new Position(144.073100, 42.190520),
                                            new Position(144.041900, 42.176660),
                                            new Position(144.013000, 42.163550),
                                            new Position(143.989700, 42.153080),
                                            new Position(143.967400, 42.143550),
                                            new Position(143.926000, 42.124540),
                                            new Position(143.899900, 42.114010),
                                            new Position(143.864400, 42.098250),
                                            new Position(143.841900, 42.087990),
                                            new Position(143.809100, 42.073160),
                                            new Position(143.781000, 42.061230),
                                            new Position(143.766800, 42.055410),
                                            new Position(143.719800, 42.035820),
                                            new Position(143.701000, 42.027480),
                                            new Position(143.653200, 42.004950),
                                            new Position(143.621400, 41.989630),
                                            new Position(143.600000, 41.980050),
                                            new Position(143.563100, 41.962560),
                                            new Position(143.453700, 41.911890),
                                            new Position(131.889500, 36.440410),
                                            new Position(123.537200, 29.109300),
                                            new Position(123.501400, 29.071240),
                                            new Position(123.323200, 28.868010),
                                            new Position(123.284200, 28.817520),
                                            new Position(123.268100, 28.797290),
                                            new Position(123.002500, 28.504250),
                                            new Position(122.876200, 28.374270),
                                            new Position(122.752100, 28.218910),
                                            new Position(122.735700, 28.202560),
                                            new Position(122.714500, 28.186060),
                                            new Position(122.678000, 28.170470),
                                            new Position(122.654500, 28.144810),
                                            new Position(122.639900, 28.125350),
                                            new Position(122.623400, 28.102700),
                                            new Position(122.603200, 28.075350),
                                            new Position(122.496800, 27.941000),
                                            new Position(122.472000, 27.908770),
                                            new Position(122.410800, 27.838780),
                                            new Position(122.327000, 27.734660),
                                            new Position(122.283800, 27.655810),
                                            new Position(122.278900, 27.646900),
                                            new Position(122.251200, 27.598540),
                                            new Position(122.151100, 27.430800),
                                            new Position(122.137100, 27.404900),
                                            new Position(122.130600, 27.383670),
                                            new Position(122.103400, 27.257050),
                                            new Position(122.097700, 27.247500),
                                            new Position(121.966900, 27.144210),
                                            new Position(121.933900, 27.128010),
                                            new Position(121.901200, 27.112000),
                                            new Position(121.890700, 27.106840),
                                            new Position(121.856800, 27.090500),
                                            new Position(121.830500, 27.078300),
                                            new Position(121.779800, 27.050300),
                                            new Position(121.772000, 27.046530),
                                            new Position(121.731900, 27.036280),
                                            new Position(121.699600, 27.008130),
                                            new Position(121.672300, 26.990120),
                                            new Position(121.652300, 26.978690),
                                            new Position(121.614000, 26.955180),
                                            new Position(121.596500, 26.939960),
                                            new Position(121.568300, 26.908620),
                                            new Position(121.551700, 26.890250),
                                            new Position(121.533700, 26.869760),
                                            new Position(121.517200, 26.850820),
                                            new Position(121.500400, 26.832070),
                                            new Position(121.482500, 26.811670),
                                            new Position(121.462900, 26.789270),
                                            new Position(121.443900, 26.767570),
                                            new Position(121.413400, 26.734730),
                                            new Position(121.390400, 26.708400),
                                            new Position(121.382800, 26.699640),
                                            new Position(121.349300, 26.661160),
                                            new Position(121.330100, 26.639560),
                                            new Position(121.304600, 26.612100),
                                            new Position(121.266400, 26.568070),
                                            new Position(121.196000, 26.489630),
                                            new Position(121.160300, 26.448750),
                                            new Position(121.117700, 26.400420),
                                            new Position(120.916900, 26.178560),
                                            new Position(119.439900, 24.794640),
                                            new Position(119.323900, 24.706530),
                                            new Position(119.292800, 24.690160),
                                            new Position(119.271300, 24.677350),
                                            new Position(119.226800, 24.645060),
                                            new Position(119.207500, 24.630590),
                                            new Position(119.147700, 24.585180),
                                            new Position(119.077700, 24.531980),
                                            new Position(118.889800, 24.393180),
                                            new Position(118.738400, 24.269870),
                                            new Position(118.704300, 24.237810),
                                            new Position(118.534300, 24.110120),
                                            new Position(118.524800, 24.104030),
                                            new Position(118.485600, 24.078000),
                                            new Position(118.476200, 24.071860),
                                            new Position(118.447500, 24.053910),
                                            new Position(118.420600, 24.037320),
                                            new Position(118.402500, 24.025870),
                                            new Position(118.373600, 24.005160),
                                            new Position(118.365900, 23.998820),
                                            new Position(118.321500, 23.960940),
                                            new Position(118.302900, 23.944940),
                                            new Position(118.273400, 23.919920),
                                            new Position(118.265900, 23.913570),
                                            new Position(118.233100, 23.887800),
                                            new Position(118.221700, 23.879030),
                                            new Position(118.189700, 23.853820),
                                            new Position(118.170700, 23.838570),
                                            new Position(118.155900, 23.826820),
                                            new Position(118.132600, 23.808030),
                                            new Position(118.111700, 23.791210),
                                            new Position(118.086100, 23.770870),
                                            new Position(118.053600, 23.746950),
                                            new Position(118.039900, 23.738390),
                                            new Position(118.016500, 23.727950),
                                            new Position(117.989600, 23.706820),
                                            new Position(117.961700, 23.683790),
                                            new Position(117.931100, 23.660370),
                                            new Position(117.900400, 23.636600),
                                            new Position(117.863000, 23.609770),
                                            new Position(117.833600, 23.591540),
                                            new Position(117.802200, 23.560040),
                                            new Position(117.793800, 23.554700),
                                            new Position(117.765500, 23.534090),
                                            new Position(117.735800, 23.514530),
                                            new Position(117.698200, 23.491310),
                                            new Position(117.671500, 23.469100),
                                            new Position(117.625100, 23.432330),
                                            new Position(117.395300, 23.252160),
                                            new Position(117.301600, 23.179080),
                                            new Position(117.029500, 22.982480),
                                            new Position(117.005700, 22.972450),
                                            new Position(116.978500, 22.962500),
                                            new Position(116.892600, 22.912070),
                                            new Position(116.884600, 22.906020),
                                            new Position(116.827200, 22.849490),
                                            new Position(116.809500, 22.830650),
                                            new Position(116.797500, 22.816980),
                                            new Position(115.837300, 22.418750),
                                            new Position(115.730300, 22.392790),
                                            new Position(115.721300, 22.391190),
                                            new Position(115.484000, 22.340620),
                                            new Position(115.465300, 22.336410),
                                            new Position(115.425200, 22.328010),
                                            new Position(115.394700, 22.323230),
                                            new Position(115.370200, 22.318160),
                                            new Position(115.325600, 22.310560),
                                            new Position(115.299500, 22.303570),
                                            new Position(115.265200, 22.293060),
                                            new Position(115.239600, 22.286420),
                                            new Position(115.210800, 22.279260),
                                            new Position(115.186400, 22.272980),
                                            new Position(115.149700, 22.264350),
                                            new Position(115.136600, 22.261430),
                                            new Position(115.106100, 22.254970),
                                            new Position(115.070700, 22.246690),
                                            new Position(115.047200, 22.242360),
                                            new Position(115.021100, 22.236710),
                                            new Position(115.001600, 22.232850),
                                            new Position(114.965100, 22.224210),
                                            new Position(114.934400, 22.217030),
                                            new Position(114.878400, 22.187420),
                                            new Position(114.850300, 22.185300),
                                            new Position(114.819700, 22.184680),
                                            new Position(114.787000, 22.179490),
                                            new Position(114.763900, 22.174130),
                                            new Position(114.747000, 22.169690),
                                            new Position(114.706500, 22.161410),
                                            new Position(114.683800, 22.164610),
                                            new Position(114.652100, 22.158890),
                                            new Position(114.637400, 22.155240),
                                            new Position(114.613500, 22.148770),
                                            new Position(114.588700, 22.141340),
                                            new Position(114.543900, 22.127460),
                                            new Position(114.517000, 22.119420),
                                            new Position(114.482700, 22.108690),
                                            new Position(114.447800, 22.095750),
                                            new Position(114.285200, 22.083940),
                                            new Position(114.276100, 22.082780),
                                            new Position(114.220800, 22.062130),
                                            new Position(114.200300, 22.066890),
                                            new Position(114.153400, 22.065070),
                                            new Position(114.101700, 22.071600),
                                            new Position(114.010200, 22.076670),
                                            new Position(113.967000, 22.079690),
                                            new Position(113.950400, 22.077680),
                                            new Position(113.921700, 22.089280),
                                            new Position(113.905900, 22.091620),
                                            new Position(113.895500, 22.095580),
                                            new Position(113.885000, 22.091980),
                                            new Position(113.881300, 22.091330));

        System.out.println(testTrack.getPoints().size());
    }
}
