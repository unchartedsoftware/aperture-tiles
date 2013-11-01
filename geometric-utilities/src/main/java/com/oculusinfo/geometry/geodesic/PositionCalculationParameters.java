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






public class PositionCalculationParameters {
    private PositionCalculationType _type;
    private double                  _allowedError;
    private double                  _epsilon;
    private boolean                 _ignoreDirection;

    /**
     * Create a set of parameters that determine how geodetic calculations are
     * performed
     * 
     * @param type
     *            The way in which the calculations are to be performed (i.e.,
     *            using what system and approcimation)
     * @param allowedError
     *            The proportion of error allowed in approximations
     * @param precision
     *            The amount of error allowed in equality tests
     */
    public PositionCalculationParameters (PositionCalculationType type,
                                          double allowedError, double precision,
                                          boolean ignoreDirection) {
        _type = type;
        _allowedError = allowedError;
        _epsilon = precision;
        _ignoreDirection = ignoreDirection;
    }

    /**
     * Get the way in which the calculations are to be performed (i.e., using
     * what system and approcimation)
     * 
     * @see PositionCalculationType
     */
    public PositionCalculationType getCalculationType () {
        return _type;
    }

    /**
     * Get the proportion of error allowed in approximations
     */
    public double getAllowedError () {
        return _allowedError;
    }

    /**
     * Get the amount of error allowed in equality tests
     */
    public double getPrecision () {
        return _epsilon;
    }

    /**
     * If true, the direction in which a track goes should be ignored when
     * calculating distance between tracks.
     */
    public boolean ignoreDirection () {
        return _ignoreDirection;
    }
}
