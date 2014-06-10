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
package com.oculusinfo.tile.rendering.color.impl;

import com.oculusinfo.tile.rendering.color.FixedPoint;

import java.util.Arrays;



public class BRColorRamp extends AbstractColorRamp {

	public BRColorRamp (boolean inverted, double opacity) {
		super(inverted,
		      Arrays.asList(new FixedPoint(0.25, 0.0), new FixedPoint(0.50, 0.5), new FixedPoint(1.00, 1.0)),
		      Arrays.asList(new FixedPoint(0.25, 0.0), new FixedPoint(0.375, 0.25), new FixedPoint(0.625, 0.25), new FixedPoint(0.75, 0.0)),
		      Arrays.asList(new FixedPoint(0.00, 0.5), new FixedPoint(0.25, 1.0), new FixedPoint(0.75, 0.0)),
		      null,
		      opacity);
	}
}
