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
package com.oculusinfo.tile.rendering.transformations.value;

/**
 * A value transformer that transforms two-tailed infinite range into a finite
 * range.  The range assumes a center of zero, and min/max defined as
 * +/-(max(abs(expectedMin),abs(expectedMax)), which is then normalized to [-1, 1].
 * A default value of .15d is used to scale the curve in the x direction, which is
 * leaves it pretty close to the shape of the curve produced by log10.
 *
 * @author nkronenfeld
 */
public class SigmoidValueTransformer implements ValueTransformer<Double> {
	private double _distance;
	private double _scale;

	public SigmoidValueTransformer (double expectedMin, double expectedMax) {
		this(expectedMin, expectedMax, .15d);
	}

	public SigmoidValueTransformer (double expectedMin, double expectedMax, double scale) {
		_scale = scale;
		_distance = Math.max(Math.abs(expectedMin), Math.abs(expectedMax));
	}

	@Override
	public Double transform (Double value) {
		double scaledInput = value / (_scale * _distance);
		return (1/(1+Math.exp(-scaledInput)));
	}

	@Override
	public Double getMaximumValue () {
		return 1.0;
	}
}
