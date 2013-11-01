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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;


public class TrackPlotter {
    private BufferedImage _image;
    private Graphics2D    _gc;

    private Double        _filtering;
    private Dimension     _size;
    private Rectangle2D   _bounds;
    private Color         _backgroundColor;

    public TrackPlotter (Dimension size, Rectangle2D bounds, Color backgroundColor) {
        _size = size;
        _bounds = bounds;
        _backgroundColor = backgroundColor;
        _filtering = null;

        reset();
    }

    /**
     * Sets a maximum drawn segment size
     * 
     * @param maxSegmentSize
     *            The size of the maximum drawn segment, in degrees. Null if no
     *            filtering is desired (default)
     */
    public void setFiltering (Double maxSegmentSize) {
        _filtering = maxSegmentSize;
    }

    public void reset () {
        _image = new BufferedImage(_size.width, _size.height,
                                   BufferedImage.TYPE_INT_ARGB);
        _gc = _image.createGraphics();
        _gc.setColor(_backgroundColor);
        _gc.fillRect(0, 0, _size.width, _size.height);
    }

    private Point scale (Position p) {
        return scale(p.getLongitude(), p.getLatitude());
    }

    private Point scale (double longitude, double latitude) {
        double rx = (longitude-_bounds.getMinX())/_bounds.getWidth();
        double ry = (latitude-_bounds.getMinY())/_bounds.getHeight();

        return new Point((int) Math.round(rx*_size.width),
                         (int) Math.round((1.0-ry)*_size.height));
    }

    public void addTrack (Track track, Color color) {
        if (null == _gc)
            throw new IllegalStateException("Attempt to draw a track after image was captured.");

        _gc.setColor(color);
        Point last = null;
        Position lastP = null;
        for (Position p: track.getPoints()) {
            Point current = scale(p);
            
            if (null != lastP
                && (null == _filtering || p.getAngularDistance(lastP) < _filtering)) {
                if (Math.abs(lastP.getLongitude()-p.getLongitude()) > 180.0) {
                    // Plot twice
                    double lon1, lon2, lat1, lat2;
                    if (lastP.getLongitude() < p.getLongitude()) {
                        lon1 = lastP.getLongitude();
                        lat1 = lastP.getLatitude();
                        lon2 = p.getLongitude();
                        lat2 = p.getLatitude();
                    } else {
                        lon1 = p.getLongitude();
                        lat1 = p.getLatitude();
                        lon2 = lastP.getLongitude();
                        lat2 = lastP.getLatitude();
                    }
                    Point p2down = scale(lon2-360, lat2);
                    Point p1 = scale(lon1, lat1);
                    Point p2 = scale(lon2, lat2);
                    Point p1up = scale(lon1+360, lat1);

                    _gc.drawLine(p2down.x, p2down.y, p1.x, p1.y);
                    _gc.drawLine(p2.x, p2.y, p1up.x, p1up.y);
                } else {
                    _gc.drawLine(last.x, last.y, current.x, current.y);
                }
            }
            lastP = p;
            last = current;
        }
    }

    public BufferedImage getImage () {
        if (null != _gc) {
            _gc.dispose();
            _gc = null;
        }
        return _image;
    }
}
