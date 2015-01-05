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

import com.oculusinfo.math.algebra.AngleUtilities;
import com.oculusinfo.math.linearalgebra.Vector;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;



/**
 * A class to describe (and allow math upon) a position on the earth
 * 
 * TODO: All calculations internally (except conversion) are still using
 * geodetic algorithms; they need to pay attention to the calculation
 * parameters.
 * 
 * @author Nathan
 */
public class Position implements Serializable {
    private static final long  serialVersionUID        = -4364498345989515044L;



    public static final double WGS84_EQUATORIAL_RADIUS = 6378137.0;        // ellipsoid equatorial getRadius, in meters
    public static final double WGS84_POLAR_RADIUS      = 6356752.3;        // ellipsoid polar getRadius, in meters
    public static final double WGS84_ES                = 0.00669437999013; // eccentricity squared, semi-major axis



    private boolean            _baseFormIsPolar;
    private Vector             _polar;     // Longutude, Latitude [, Elevation]
    private Vector             _cartesian; // X, Y, Z
    private boolean            _elevationUsed;

    /**
     * Create an object that represents a position on the surface of the Earth
     * using standard latitude and longitude. The resultant position will ignore
     * elevation.
     * 
     * @param longitude
     *            The longitude of the position, in degrees
     * @param latitude
     *            The latitude of the position, in degrees
     */
    public Position (double longitude, double latitude) {
        _polar = new Vector(AngleUtilities.intoRangeDegrees(0.0, longitude),
                            latitude);
        _cartesian = null;
        _elevationUsed = false;
        _baseFormIsPolar = true;
    }

    /**
     * Create an object that represents a position on the surface of the Earth
     * using standard latitude, longitude, and elevation.
     * 
     * @param longitude
     *            The longitude of the position, in degrees
     * @param latitude
     *            The latitude of the position, in degrees
     * @param elevation
     *            The elevation of the position, in meters above sea level
     */
    public Position (double longitude, double latitude, double elevation) {
        _polar = new Vector(AngleUtilities.intoRangeDegrees(0.0, longitude),
                            latitude, elevation);
        _cartesian = null;
        _elevationUsed = true;
        _baseFormIsPolar = true;
    }

    /**
     * Create an object that represents a position on the surface of the Earth,
     * using cartesian coordinates with the Y axis being the axis of rotation,
     * the XZ plane being the equator, with the Z axis pointed towards the
     * Greenwich meridian.
     * 
     * @param x
     *            The coordinate in the direction of (0 deg N, 90 deg E)
     * @param y
     *            The coordinate in the direction of the north pole
     * @param z
     *            The coordinate in the direction of (0 deg N, 0 deg E)
     * @param ignoreElevation
     *            if true, sets calculations using this position to ignore any
     *            elevation inherent in the position; if false, elevation will
     *            be taken into account
     */
    public Position (double x, double y, double z, boolean ignoreElevation) {
        _cartesian = new Vector(x, y, z);
        _polar = null;
        _elevationUsed = !ignoreElevation;
        _baseFormIsPolar = false;
    }

    /**
     * Copy a position, possibly changing from polar to cartesian or vice versa
     * 
     * @param oldForm
     *            The position being copied
     * @param polar
     *            True if the new position should be in polar coordinates, false
     *            if it should be in cartesian coordinates.
     */
    public Position (Position oldForm, boolean polar) {
        if (null == oldForm._polar) {
            _polar = null;
        } else {
            _polar = new Vector(oldForm._polar);
        }

        if (null == oldForm._cartesian) {
            _cartesian = null;
        } else {
            _cartesian = new Vector(oldForm._cartesian);
        }

        _elevationUsed = oldForm._elevationUsed;

        if (_baseFormIsPolar) updatePolar();
        else updateCartesian();
    }


    // ////////////////////////////////////////////////////////////////////////
    // Section: Conversion between polar and cartesian forms
    //
    private void updatePolar () {
        if (null != _polar) return;
        if (null == _cartesian) return;

        // According to
        // H. Vermeille, 
        // "An analytical method to transform geocentric into geodetic coordinates"
        // http://www.springerlink.com/content/3t6837t27t351227/fulltext.pdf
        // Journal of Geodesy, accepted 10/2010, not yet published

        // Note coordinates for this are rotation axis = Z, not rotation axis = Y.
        double X = _cartesian.coord(2);
        double Y = _cartesian.coord(0);
        double Z = _cartesian.coord(1);
        double XXpYY = X*X+Y*Y;
        double sqrtXXpYY = Math.sqrt(XXpYY);

        double a = WGS84_EQUATORIAL_RADIUS;
        double ra2 = 1/(a*a);
        double e2 = WGS84_ES;
        double e4 = e2*e2;

        // Step 1
        double p = XXpYY*ra2;
        double q = Z*Z*(1-e2)*ra2;
        double r = (p+q-e4)/6;

        double h;
        double phi;

        double evoluteBorderTest = 8*r*r*r+e4*p*q;
        if (evoluteBorderTest > 0 || q != 0) {
            double u;

            if (evoluteBorderTest > 0) {
                // Step 2: general case
                double rad1 = Math.sqrt(evoluteBorderTest);
                double rad2 = Math.sqrt(e4*p*q);
    
                // 10*e2 is my arbitrary decision of what Vermeille means by "near... the cusps of the evolute".
                if (evoluteBorderTest > 10*e2) {
                    double rad3 = Math.cbrt((rad1+rad2)*(rad1+rad2));
                    u = r + 0.5*rad3 + 2*r*r/rad3;
                } else {
                    u = r + 0.5*Math.cbrt((rad1+rad2)*(rad1+rad2))+0.5*Math.cbrt((rad1-rad2)*(rad1-rad2));
                }
            } else {
                // Step 3: near evolute
                double rad1 = Math.sqrt(-evoluteBorderTest);
                double rad2 = Math.sqrt(-8*r*r*r);
                double rad3 = Math.sqrt(e4*p*q);
                double atan = 2*Math.atan2(rad3, rad1+rad2)/3;
    
                u = -4*r*Math.sin(atan)*Math.cos(Math.PI/6+atan);
            }
    
            double v = Math.sqrt(u*u+e4*q);
            double w = e2*(u+v-q)/(2*v);
            double k = (u+v)/(Math.sqrt(w*w+u+v)+w);
            double D = k*sqrtXXpYY/(k+e2);
            double sqrtDDpZZ = Math.sqrt(D*D+Z*Z);

            h = (k+e2-1)*sqrtDDpZZ/k;
            phi = 2*Math.atan2(Z, sqrtDDpZZ+D);
        } else {
            // Step 4: singular disk
            double rad1 = Math.sqrt(1-e2);
            double rad2 = Math.sqrt(e2-p);
            double e = Math.sqrt(e2);

            h = -a*rad1*rad2/e;
            phi = rad2/(e*rad2+rad1*Math.sqrt(p));
        }

        // Compute lambda
        double lambda;
        double s2 = Math.sqrt(2);
        if ((s2-1)*Y < sqrtXXpYY+X) {
            // case 1 - -135deg < lambda < 135deg
            lambda = 2*Math.atan2(Y, sqrtXXpYY+X);
        } else if (sqrtXXpYY+Y < (s2+1)*X) {
            // case 2 - -225deg < lambda < 45deg
            lambda = -Math.PI*0.5+2*Math.atan2(X, sqrtXXpYY-Y);
        } else {
            // if (sqrtXXpYY-Y<(s2=1)*X) {  // is the test, if needed, but it's not
            // case 3: - -45deg < lambda < 225deg
            lambda = Math.PI*0.5 - 2*Math.atan2(X, sqrtXXpYY+Y);
        }

        if (_elevationUsed) {
            _polar = new Vector(AngleUtilities.intoRangeDegrees(0.0, Math.toDegrees(lambda)), Math.toDegrees(phi), h);
        } else {
            _polar = new Vector(AngleUtilities.intoRangeDegrees(0.0, Math.toDegrees(lambda)), Math.toDegrees(phi));
        }

        _polar.setPrecision(_cartesian.getPrecision());
    }

    private void updateCartesian () {
        if (null != _cartesian) return;
        if (null == _polar) return;

        // from WorldWind: ElipsoidalGlobe.geodeticToCartesian
        double latitude = getLatitudeRadians();
        double cosLat = Math.cos(latitude);
        double sinLat = Math.sin(latitude);
        double longitude = getLongitudeRadians();
        double cosLon = Math.cos(longitude);
        double sinLon = Math.sin(longitude);
        double elevation = (_elevationUsed ? getElevation() : 0.0);

        // Get the radius (in meters) of the vertical in prime meridian
        double rpm = WGS84_EQUATORIAL_RADIUS / Math.sqrt(1.0 - WGS84_ES * sinLat * sinLat);

        // Y axis is the axis of rotation, just like in WorldWind.
        double x = (rpm + elevation) * cosLat * sinLon;
        double z = (rpm + elevation) * cosLat * cosLon;
        double y = (rpm * (1.0 - WGS84_ES) + elevation) * sinLat;

        _cartesian = new Vector(x, y, z);
        _cartesian.setPrecision(_polar.getPrecision());
    }



    // ////////////////////////////////////////////////////////////////////////
    // Section: accessors
    //
    public void setPrecision (double precision) {
        if (null != _polar) _polar.setPrecision(precision);
        if (null != _cartesian) _cartesian.setPrecision(precision);
    }

    public double getPrecision () {
        if (_baseFormIsPolar) return _polar.getPrecision();
        else return _cartesian.getPrecision();
    }

    public double getLongitude () {
        updatePolar();
        return _polar.coord(0);
    }

    public double getLongitudeRadians () {
        return Math.toRadians(getLongitude());
    }

    public double getLatitude () {
        updatePolar();
        return _polar.coord(1);
    }

    public double getLatitudeRadians () {
        return Math.toRadians(getLatitude());
    }

    /**
     * Gets the elevation of this position.
     * 
     * @return The elevation of this position in meters
     * @throws UnsupportedOperationException
     *             if this position doesn't specify an elevation.
     */
    public double getElevation () {
        if (!_elevationUsed)
            throw new UnsupportedOperationException("Attempt to get elevation of elevationless position");
        updatePolar();
        return _polar.coord(2);
    }

    public boolean hasElevation () {
        return _elevationUsed;
    }

    public Vector getAsCartesian () {
        updateCartesian();
        return _cartesian;
    }



    // ////////////////////////////////////////////////////////////////////////
    // Section: Calculations
    //
    public double getDistanceMeters (Position p) {
        double dR = Math.toRadians(getAngularDistance(p));
        // estimate radius based on average latitude
        double radius = getEarthRadius((getLatitude()+p.getLatitude())/2.0);
        return dR*radius;
    }
    /**
     * Gets the angular distance between this point and the other, in degrees
     * 
     * @param p
     *            The other point
     * @return The angular distance, in degrees, between this and p
     */
    public double getAngularDistance (Position p) {
        double lon1 = getLongitudeRadians();
        double lat1 = getLatitudeRadians();
        double lon2 = AngleUtilities.intoRangeDegrees(lon1, p.getLongitudeRadians());
        double lat2 = p.getLatitudeRadians();

        // Taken from WorldWind's LatLon class:
        double epsilon = getPrecision();
        if (Math.abs(lon1 - lon2) < epsilon && Math.abs(lat1 - lat2) < epsilon)
            return 0.0;

        // Taken from "Map Projections - A Working Manual", page 30, equation
        // 5-3a.
        // The traditional d=2*asin(a) form has been replaced with
        // d=2*atan2(sqrt(a), sqrt(1-a))
        // to reduce rounding errors with large distances.
        double a = Math.sin((lat2 - lat1) / 2.0)
                   * Math.sin((lat2 - lat1) / 2.0) + Math.cos(lat1)
                   * Math.cos(lat2) * Math.sin((lon2 - lon1) / 2.0)
                   * Math.sin((lon2 - lon1) / 2.0);
        double distanceRadians = 2.0 * Math.atan2(Math.sqrt(a),
                                                  Math.sqrt(1 - a));

        if (Double.isNaN(distanceRadians))
            return 0.0;
        else
            return Math.toDegrees(distanceRadians);
    }

    public double getAngularDistanceSquared (Position other) {
        double d = getAngularDistance(other);
        return d*d;
    }

    public double getCartesianDistance (Position p) {
        return getAsCartesian().getDistance(p.getAsCartesian());
    }

    public double getCartesianDistanceSquared (Position p) {
        return getAsCartesian().getDistanceSquared(p.getAsCartesian());
    }

    /**
     * Gets the angle from this point to the other.
     * 
     * @param p
     *            The other point.
     * @return The angle from this to p, in degree, with north=0, east=90
     */
    public double getAzimuth (Position p) {
        double lon1 = getLongitudeRadians();
        double lat1 = getLatitudeRadians();
        double lon2 = AngleUtilities.intoRangeRadians(lon1, p.getLongitudeRadians());
        double lat2 = p.getLatitudeRadians();

        // Taken from WorldWind's LatLon class:
        double epsilon = getPrecision();
        if (Math.abs(lon1-lon2) < epsilon && Math.abs(lat1-lat2) < epsilon)
            return 0.0;

        if (Math.abs(lon1-lon2) < epsilon) {
            if (lat1 > lat2) return 180.0;
            else return 0.0;
        }

        // Taken from "Map Projections - A Working Manual", page 30, equation 5-4b.
        // The atan2() function is used in place of the traditional atan(y/x) to simplify the case when x==0.
        double y = Math.cos(lat2) * Math.sin(lon2 - lon1);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                   * Math.cos(lat2) * Math.cos(lon2 - lon1);
        double azimuthRadians = Math.atan2(y, x);

        if (Double.isNaN(azimuthRadians))
            return 0.0;
        else
            return Math.toDegrees(azimuthRadians);
    }

    /**
     * Get the point offset from this point by the given distance at the given
     * azimuth.
     * 
     * @param azimuthDegrees
     *            The angle, in degree, with N=0, E=90, in which the desired
     *            offset point lies
     * @param distanceDegrees
     *            The angular distance, in degrees, by which to offset this
     *            point.
     * @return The point offset from this one as described.
     */
    public Position offset (double azimuthDegrees, double distanceDegrees) {
        double lon = getLongitudeRadians();
        double lat = getLatitudeRadians();
        double azimuth = Math.toRadians(azimuthDegrees);
        double distance = Math.toRadians(distanceDegrees);

        // Taken from WorldWind's LatLon class:
        if (distance == 0)
            return this;

        // Taken from "Map Projections - A Working Manual", page 31, equation 5-5 and 5-6.
        double endLatRadians = Math.asin(Math.sin(lat) * Math.cos(distance)
                                         + Math.cos(lat) * Math.sin(distance)
                                         * Math.cos(azimuth));
        double endLonRadians = lon
                               + Math.atan2(Math.sin(distance)
                                                    * Math.sin(azimuth),
                                            Math.cos(lat) * Math.cos(distance)
                                                    - Math.sin(lat)
                                                    * Math.sin(distance)
                                                    * Math.cos(azimuth));

        if (Double.isNaN(endLatRadians) || Double.isNaN(endLonRadians))
            return this;

        Position p = new Position(Math.toDegrees(endLonRadians),
                                  Math.toDegrees(endLatRadians));
        p.setPrecision(getPrecision());
        return p;
    }

    /**
     * Get the point on the great circle from a to b at the given latitude
     * 
     * @param p1
     *            The segment start point
     * @param p2
     *            The segment end point
     * @param latitude
     *            The latitude of the desired result (in degrees)
     * @return A point on the great circle between A and B at the given
     *         latitude, or null if the shortest-path segment of the great
     *         circle from a to b doesn't intersect that latitude. If the given
     *         latitude is intersected multiple times, the first is returned.
     */
    public static Position greatCircleAtLatitude (Position p1, Position p2, double latitude) {
        // from http://williams.best.vwh.net/avform.htm#Par
        double lon1 = p1.getLongitudeRadians();
        double lat1 = p1.getLatitudeRadians();
        double lon2 = AngleUtilities.intoRangeRadians(lon1, p2.getLongitudeRadians());
        double lat2 = p2.getLatitudeRadians();

        double lat3 = Math.toRadians(latitude);

        double sinlat1 = Math.sin(lat1);
        double coslat1 = Math.cos(lat1);
        double sinlat2 = Math.sin(lat2);
        double coslat2 = Math.cos(lat2);
        double sinlat3 = Math.sin(lat3);
        double coslat3 = Math.cos(lat3);

        double dl = lon1-lon2;
        double sindl = Math.sin(dl);
        double cosdl = Math.cos(dl);

        double A = sinlat1 * coslat2 * coslat3 * sindl;
        double B = sinlat1 * coslat2 * coslat3 * cosdl - coslat1 * sinlat2 * coslat3;
        double C = coslat1 * coslat2 * sinlat3 * sindl;

        double baseLon = Math.atan2(B,  A);

        if (C*C > (A*A + B*B)) {
            // No crossing at that latitude
            return null;
        } else {
            double dlon = Math.acos(C/Math.sqrt(A*A+B*B));
            double centerLon = (lon1+lon2)/2.0;
            double targetLon1 = AngleUtilities.intoRangeRadians(centerLon, (lon1+dlon+baseLon));
            Position r1 = new Position(Math.toDegrees(targetLon1), latitude);
            double targetLon2 = AngleUtilities.intoRangeRadians(centerLon, (lon1-dlon+baseLon));
            Position r2 = new Position(Math.toDegrees(targetLon2), latitude);

            boolean good1, good2;
            if (lon1 < lon2) {
                good1 = (lon1 <= targetLon1 && targetLon1 <= lon2);
                good2 = (lon1 <= targetLon2 && targetLon2 <= lon2);
            } else {
                good1 = (lon2 <= targetLon1 && targetLon1 <= lon1);
                good2 = (lon2 <= targetLon2 && targetLon2 <= lon1);
            }

            if (good1 && good2) {
                double d1 = p1.getAngularDistance(r1);
                double d2 = p1.getAngularDistance(r2);
                if (d1 < d2) {
                    return r1;
                } else {
                    return r2;
                }
            } else if (good1) {
                return r1;
            } else if (good2) {
                return r2;
            } else {
                return null;
            }
        }
    }

    public static Position greatCircleAtLongitude (Position a, Position b,
                                                  double longitude) {
        double abAz = a.getAzimuth(b);
        double azTan = Math.tan(Math.toRadians(abAz));
        double latA = a.getLatitudeRadians();
        double dLon = Math.toRadians(longitude)-a.getLongitudeRadians();
        double y = Math.sin(dLon) + azTan*Math.sin(latA)*Math.cos(dLon);
        double x = azTan*Math.cos(latA);
        double latitudeRad = Math.atan2(y, x);
        double latitude = Math.toDegrees(latitudeRad);
        return new Position(longitude, latitude);
    }

    public static Rectangle2D getGreatCircleLonLatBounds (Position p1, Position p2) {
        double epsilon = p1.getPrecision();

        // from http://williams.best.vwh.net/avform.htm#Par
        double lon1 = p1.getLongitude();
        double lat1 = p1.getLatitude();
        double lon2 = AngleUtilities.intoRangeDegrees(lon1, p2.getLongitude());
        double lat2 = p2.getLatitude();

        // Check for the case where the two points define the equator
        if (Math.abs(lat1) < epsilon && Math.abs(lat2) < epsilon) {
            double minLon = Math.min(lon1, lon2);
            double maxLon = Math.max(lon1, lon2);
            return new Rectangle2D.Double(minLon, 0.0, maxLon-minLon, 0.0);
        }

        // Not the equator; therefore it crosses the equator. Figure out where -
        // max and min will be half way in between
        //
        // The rest of this code is adapted from  greatCircleAtLatitude 
        // (i.e., http://williams.best.vwh.net/avform.htm#Par)
        lon1 = Math.toRadians(lon1);
        lat1 = Math.toRadians(lat1);
        lon2 = Math.toRadians(lon2);
        lat2 = Math.toRadians(lat2);
        double lat3 = 0.0;

        double sinlat1 = Math.sin(lat1);
        double coslat1 = Math.cos(lat1);
        double sinlat2 = Math.sin(lat2);
        double coslat2 = Math.cos(lat2);
        double sinlat3 = Math.sin(lat3);
        double coslat3 = Math.cos(lat3);

        double dl = lon1-lon2;
        double sindl = Math.sin(dl);
        double cosdl = Math.cos(dl);

        double A = sinlat1 * coslat2 * coslat3 * sindl;
        double B = sinlat1 * coslat2 * coslat3 * cosdl - coslat1 * sinlat2 * coslat3;
        double C = coslat1 * coslat2 * sinlat3 * sindl;
        assert(0.0 == C);

        // C should be 0, therefore dlon should be 0. lon1+baseLon should
        // therefore be an equatorial crossing point. The max is therefore PI/2
        // away in the direction of either point.
        double baseLon = Math.atan2(B,  A);
        Position equatorialCrossing = new Position(Math.toDegrees(lon1 + baseLon - Math.PI/2.0), 0.0);
        double direction = equatorialCrossing.getAzimuth(p1);
        Position maxPt, minPt;
        if (direction > 0) {
            maxPt = equatorialCrossing.offset(-direction, 90);
            minPt = equatorialCrossing.offset(-direction, -90);
        } else {
            maxPt = equatorialCrossing.offset(direction, 90);
            minPt = equatorialCrossing.offset(direction, -90);
        }

        double minLon = Math.min(p1.getLongitude(), p2.getLongitude());
        double maxLon = Math.max(AngleUtilities.intoRangeDegrees(minLon, p1.getLongitude()),
                                 AngleUtilities.intoRangeDegrees(minLon, p2.getLongitude()));
        double minLat = Math.min(p1.getLatitude(), p2.getLatitude());
        double maxLat = Math.max(p1.getLatitude(), p2.getLatitude());

        double centerLon = (minLon+maxLon)/2.0;
        double minPtLon = AngleUtilities.intoRangeDegrees(centerLon, minPt.getLongitude());
        if (minLon < minPtLon && minPtLon < maxLon) {
            minLat = minPt.getLatitude();
        }
        double maxPtLon = AngleUtilities.intoRangeDegrees(centerLon, maxPt.getLongitude());
        if (minLon < maxPtLon && maxPtLon < maxLon) {
            maxLat = maxPt.getLatitude();
        }
        return new Rectangle2D.Double(minLon, minLat, maxLon-minLon, maxLat-minLat);
    }



    @Override
    public boolean equals (Object obj) {
        if (this == obj) return true;
        if (null == obj) return false;
        if (!(obj instanceof Position)) return false;
        Position p = (Position) obj;

        if (_baseFormIsPolar) {
            double epsilon = getPrecision();
            if (Math.abs(getLongitude() - p.getLongitude()) >= epsilon)
                return false;
            if (Math.abs(getLatitude() - p.getLatitude()) >= epsilon)
                return false;
            if (_elevationUsed) {
                if (!p._elevationUsed)
                    return false;
                if (Math.abs(getElevation() - p.getElevation()) >= epsilon)
                    return false;
            }
        } else {
            if (!_cartesian.equals(p.getAsCartesian())) return false;
        }
        return true;
    }

    @Override
    public String toString () {
        if (_baseFormIsPolar) {
            double lon = getLongitude();
            String lonMarker = "E";
            if (lon < 0) {
                lon = -lon;
                lonMarker = "W";
            }
            int lonDegrees = (int) Math.floor(lon);
            int lonMinutes = (int) Math.floor((lon-lonDegrees)*60.0);
            int milliLonSeconds = (int) Math.round(((lon-lonDegrees)*60.0-lonMinutes)*60000.0);
            if (60000 == milliLonSeconds) {
                milliLonSeconds = 0;
                lonMinutes++;
                if (60 <= lonMinutes) {
                    lonMinutes -= 60;
                    ++lonDegrees;
                }
            }
    
            double lat = getLatitude();
            String latMarker = "N";
            if (lat < 0) {
                lat = -lat;
                latMarker = "S";
            }
            int latDegrees = (int) Math.floor(lat);
            int latMinutes = (int) Math.floor((lat-latDegrees)*60.0);
            int milliLatSeconds = (int) Math.round(((lat-latDegrees)*60.0-latMinutes)*60000.0);
            if (60000 == milliLatSeconds) {
                milliLatSeconds = 0;
                latMinutes++;
                if (60 <= latMinutes) {
                    latMinutes -= 60;
                    ++latDegrees;
                }
            }
    
            return String.format("%d\u00b0%d'%.3f\"%s, %d\u00b0%d'%.3f\"%s",
                                 lonDegrees, lonMinutes, milliLonSeconds/1000.0, lonMarker,
                                 latDegrees, latMinutes, milliLatSeconds/1000.0, latMarker);
        } else {
            return _cartesian.toString();
        }
    }

    /**
     * Get the radius of the earth at this point.
     */
    public double getEarthRadius () {
        return getEarthRadius(getLatitude());
    }

    public static double getEarthRadius (double latitude) {
        // From WorldWind, EllipsoidalGlobe.getRadiusAt(...)
        return new Position(0.0, latitude).getAsCartesian().vectorLength();
    }
}
