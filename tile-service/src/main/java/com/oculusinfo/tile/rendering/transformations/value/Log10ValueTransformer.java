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
	private final double _absMin;

	private final double _logMin;
	private final double _mappedLogMin;
	private final double _oneOverLogRange;

	public Log10ValueTransformer(double minRange, double maxRange) {
		// Default the zero level to anything lower than 1
		// This is for backwards compatibility when there was lack of support for values < 1
		// Setting to a lower value (e.g. 0.001) will inflate the log space dramatically in the low ranges
		// when the min range value is zero (common for count tile sets).
		this(minRange, maxRange, 1);
	}

	public Log10ValueTransformer(double minRange, double maxRange, double absMin){
		_min = minRange;
		_max = maxRange;
		_absMin = absMin;

		// constraint the range boundaries based on the
		// log minimum configured.
		if ( minRange < 0 ) {
			minRange = Math.min( minRange, -absMin );
			maxRange = ( maxRange < 0 )?
					Math.min( maxRange, -absMin ): // both neg
					Math.max( maxRange,  absMin ); // spans zero
		} else {
			// both positive
			minRange = Math.max( minRange, absMin );
			maxRange = Math.max( maxRange, absMin );
		}

		// cache derived constants for fast map calculations.
		double log0 = Math.log10(Math.abs( minRange ));
		double log1 = Math.log10(Math.abs( maxRange ));

		// find our abs log min and max - if spans, the zeroish value, else the smaller
		_logMin = minRange*maxRange < 0? Math.log10(absMin) : Math.min( log0, log1 );

		// establish the range
		double logRange = log0 - _logMin + log1 - _logMin;

		if (logRange > 0) {
			_oneOverLogRange = 1 / logRange;

			// now find mapped closest-to-zero value (between 0 and 1)
			_mappedLogMin = minRange >= 0? 0: maxRange <= 0? 1:
					(log0 - _logMin) * _oneOverLogRange;
		} else {
			_mappedLogMin = 0;
			_oneOverLogRange = 0;
		}
	}
	
	@Override
	public Double transform(Double value) {
		value = Math.max(Math.min(value, _max), _min);

		double absValue = Math.abs( value );

		return _mappedLogMin +
				// zero(ish)?
				( absValue <= _absMin? 0 :
						// or - direction * mapped log value
						( value > 0? 1 : -1 ) *
								( Math.log10( absValue ) - _logMin ) * _oneOverLogRange);
	}

	@Override
	public Double getMaximumValue () {
		return _max;
	}
}
