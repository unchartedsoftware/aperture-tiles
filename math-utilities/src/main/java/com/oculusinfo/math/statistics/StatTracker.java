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
package com.oculusinfo.math.statistics;



/**
 * This class encapsulates the one-pass tracking of mean, standard deviation,
 * min, and max of a series of values.
 * 
 * @author nkronenfeld
 */
public class StatTracker {
	private int    _n;
	private double _sumX;
	private double _sumXSquared;
	private double _min;
	private double _max;



	public StatTracker () {
		reset();
	}

	/**
	 * Clear all current stats
	 */
	public void reset () {
		_n = 0;
		_sumX = 0;
		_sumXSquared = 0;
		_min = Double.NaN;
		_max = Double.NaN;
	}

	/**
	 * Add a datum to the series tracked. Statistics on this datum are stored,
	 * but the datum itself is forgotten.
	 */
	public void addStat (double value) {
		++_n;
		_sumX += value;
		_sumXSquared += value*value;
		if (Double.isNaN(_min) || value < _min)
			_min = value;
		if (Double.isNaN(_max) || value > _max)
			_max = value;
	}

	/**
	 * @return The number of data given to the tracker.
	 */
	public int numItems () {
		return _n;
	}

	/**
	 * @return The total value given to the tracker
	 */
	public double total () {
		return _sumX;
	}

	/**
	 * @return The number of data (i.e., number of calls to addStat) given to
	 *         the tracker
	 */
	public int sampleSize () {
		return _n;
	}

	/**
	 * @return The mean of all data given to the tracker.
	 */
	public double mean () {
		return _sumX/_n;
	}

	/**
	 * @return The highest value of all data given to the tracker.
	 */
	public double max () {
		return _max;
	}

	/**
	 * @return The lowest value of all data given to the tracker.
	 */
	public double min () {
		return _min;
	}

	/**
	 * Normalize a value to fit in the range we've tracked
	 * 
	 * @return 0 if <code>value</code> is at the minimum tracked, 1 if at the
	 *         max, linear interpolations thereof for other values, and NaN if
	 *         less than two values have been tracked
	 */
	public double normalizeValue (double value) {
		if (2 > _n) return Double.NaN;
		return (value-_min)/(_max-_min);
	}

	/**
	 * @return The variance in the tracked data as a complete population.
	 */
	public double populationVariance () {
		double mean = mean();
		return _sumXSquared/_n - mean*mean;
	}

	/**
	 * @return The standard deviation in the tracked data as a complete population.
	 */
	public double populationStandardDeviation () {
		return Math.sqrt(populationVariance());
	}

	/**
	 * @return The variance in the tracked data as a sample.
	 */
	public double sampleVariance () {
		double mean = mean();
		return _sumXSquared/(_n-1) - mean*mean*_n/(_n-1);
	}

	/**
	 * @return The standard deviation in the tracked data as a sample.
	 */
	public double sampleStandardDeviation () {
		return Math.sqrt(sampleVariance());
	}
}
