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
package com.oculusinfo.math.algebra;

/**
 * Simple utilities for dealing with angles.
 * 
 * @author nkronenfeld
 */
public class AngleUtilities {
    public static double fromDMS (double degrees, double minutes, double seconds) {
        return degrees+minutes/60.0+seconds/3600.0;
    }

    /**
     * Returns the input angle put into the 360 degree range centered on the
     * input center, <code>[center-180, center+180)</code>
     * 
     * @param center
     *            The center of the desired output range
     * @param angle
     *            The angle to return in the given range
     * @return <code>angle</code>, but specified in the given range, in degrees.
     */
    public static double intoRangeDegrees (double center, double angle) {
        return intoRange(center, angle, 360);
    }

    /**
     * Returns the input angle put into the 2pi radian range centered on the
     * input center, <code>[center-pi, center+pi)</code>
     * 
     * @param center
     *            The center of the desired output range
     * @param angle
     *            The angle to return in the given range
     * @return <code>angle</code>, but specified in the given range, in radians.
     */
    public static double intoRangeRadians (double center, double angle) {
        return intoRange(center, angle, Math.PI * 2.0);
    }

    /**
     * Essentially number mod modulus, but with the ability to specify the
     * output range.
     * 
     * @param center
     *            The center of the desired output range
     * @param number
     *            The number in question
     * @param modulus
     *            The modulus - i.e., the width of the range
     * @return the equivalent of number, mod modulus, centered on the given
     *            center (i.e., in the range [center-modulus/2,
     *            center+modulus/2))
     */
    public static double intoRange (double center, double number, double modulus) {
        return number - modulus * Math.round((number - center) / modulus);
    }
}
