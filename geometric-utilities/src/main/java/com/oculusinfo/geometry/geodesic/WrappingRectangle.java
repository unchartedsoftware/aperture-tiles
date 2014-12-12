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

/**
 * A rectangle in longitude/latitude space that takes world wrap into account
 * 
 * @author nkronenfeld
 */
public class WrappingRectangle {
    private double _epsilon; // double equality factor
    private double _centerLon;
    private double _minLon;
    private double _minLat;
    private double _maxLon;
    private double _maxLat;

    private WrappingRectangle (double centerLon, double minLon, double maxLon,
                               double minLat, double maxLat) {
        _centerLon = centerLon;
        _minLon = minLon;
        _maxLon = maxLon;
        _minLat = minLat;
        _maxLat = maxLat;
    }

    public WrappingRectangle (Position a, Position b) {
        _minLat = Math.min(a.getLatitude(), b.getLatitude());
        _maxLat = Math.max(a.getLatitude(), b.getLatitude());
        double alon = a.getLongitude();
        double blon = AngleUtilities.intoRangeDegrees(alon, b.getLongitude());
        _centerLon = (alon + blon) / 2.0;
	_minLon = Math.min(alon, blon);
	_maxLon = Math.max(alon, blon);

	_epsilon = 1E-12;
    }

    /**
     * Sets how close another WrappingRectangle has to be to this one to count 
     * as equal
     */
    public void setPrecision (double precision) {
	_epsilon = precision;
    }

    /**
     * Indicates if this rectangle contains the given position. Elevation is
     * ignored for containment purposes (perhaps TODO could be to allow for
     * elevation min and max, if needed).
     */
    public boolean contains (Position p) {
        double lat = p.getLatitude();
        if (lat > _maxLat || lat < _minLat)
            return false;

        double lon = AngleUtilities.intoRangeDegrees(_centerLon,
                                                     p.getLongitude());
        if (lon > _maxLon || lon < _minLon)
            return false;

        return true;
    }

    /**
     * Get a rectangle with the same center as this one, but expanded by the
     * input border size.
     * 
     * @param borderSize
     *            The size of the border to be added, in degrees.
     */
    public WrappingRectangle addBorder (double borderSize) {
        return new WrappingRectangle(_centerLon,
                                     _minLon - borderSize,
                                     _maxLon + borderSize,
                                     _minLat - borderSize,
                                     _maxLat + borderSize);
    }

    /**
     * Gets a rectangle with the same center as this one, but scaled by the
     * given amount.
     * 
     * @param scale The scale factor
     * @return The WrappingRectangle scaled by scale
     */
    public WrappingRectangle scale (double scale) {
        double halfWidth = (_maxLon-_minLon)/2.0;

        double centerLat = (_maxLat+_minLat)/2.0;
        double halfHeight = (_maxLat-_minLat)/2.0;

        return new WrappingRectangle(_centerLon,
                                     _centerLon-halfWidth*scale,
                                     _centerLon+halfWidth*scale,
                                     centerLat-halfHeight*scale,
                                     centerLat+halfHeight*scale);
    }

    @Override
    public boolean equals (Object obj) {
	if (this == obj) return true;
	if (null == obj) return false;
	if (!(obj instanceof WrappingRectangle)) return false;

	WrappingRectangle rect = (WrappingRectangle) obj;

	if (Math.abs(_minLat-rect._minLat) > _epsilon) return false;
	if (Math.abs(_maxLat-rect._maxLat) > _epsilon) return false;
	double oCenter = AngleUtilities.intoRangeDegrees(_centerLon, rect._centerLon);
	if (Math.abs(oCenter-_centerLon) > _epsilon) return false;
	double oMinLon = AngleUtilities.intoRangeDegrees(_minLon, rect._minLon);
	if (Math.abs(oMinLon-_minLon) > _epsilon) return false;
	double oMaxLon = AngleUtilities.intoRangeDegrees(_maxLon, rect._maxLon);
	if (Math.abs(oMaxLon-_maxLon) > _epsilon) return false;
	return true;
    }

    @Override
    public String toString () {
	return String.format("[[%.5f, %.5f] to [%.5f, %.5f] (center=%.4f)]",
			     _minLon, _minLat, _maxLon, _maxLat, _centerLon);
    }
}
