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
package com.oculusinfo.geometry.cartesian;

/**
 * A fourth-order spline that allows fitting of points and velocities, and
 * matching of acceleration, at join points.
 * 
 * The general formula for the spline is a basic parameterized fourth-order
 * polynomial, by segment (s):
 *     x_s_n(t) = a_s_n t^4 + b_s_n t^3 + c_s_n t^2 + d_s_n t + e_s_n
 *    x'_s_n(t) = 4 a_s_n t^3 + 3 b_s_n t^2 + 2 c_s_n t + d_s_n
 *    x"_s_n(t) = 12 a_s_n t^2 + 6 b_s_n t + 2 c_s_n
 * 
 * We are trying to match:
 *      x_s_n(0) = p0_s_n       X_s(0) is the first point of segment s
 *     x'_s_n(0) = v0_s_n      X'_s(0) is the velocity at the first point of the segment s
 *      x_s_n(1) = p1_s_n       X_s(1) is the last point of the segment s
 *     x'_s_n(1) = v1_s_n      X'_s(1) is the velocity at the last point of the segment s
 *        p1_s_n = p0_s+1_n    The last point of segment s matches the first point of segment s+1
 *        v1_s_n = v0_s+1_n    The velocity at the last point of segment s matches the velocity at the first point of segment s+1
 *     x"_s_n(1) = x"_s+1_n(0) The acceleration at the last point of segment s matches the acceleration at the first point of segment s+1
 * 
 * This give us our 5 unknowns
 * 
 *     e_s_n = p0_s_n
 *     d_s_n = v0_s_n
 *     
 * 
 * @author Nathan
 */
public class QuarticSpline {

}
