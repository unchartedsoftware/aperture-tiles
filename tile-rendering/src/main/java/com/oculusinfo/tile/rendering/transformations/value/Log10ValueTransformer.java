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

public class Log10ValueTransformer implements ValueTransformer<Double> {
	private final double _min;
	private final double _max;

	private final double _logMin;
	private final double _oneOverLogRange;

	public Log10ValueTransformer(double minRange, double maxRange) {
		_min = minRange;
		_max = maxRange;


		if (_min <= 0) {
			throw new IllegalArgumentException("Log 10 transformer does not support ranges that include zero or negative numbers");
		}

		_logMin = Math.log10(minRange);
		double logMax = Math.log10(maxRange);

		_oneOverLogRange = 1 / (logMax - _logMin);
	}

	@Override
	public Double transform(Double value) {
		// Out of range is clamped
		return ( Math.log10( Math.max(Math.min(value, _max), _min)) - _logMin ) * _oneOverLogRange;
	}

	@Override
	public Double getMaximumValue () {
		return _max;
	}
}
