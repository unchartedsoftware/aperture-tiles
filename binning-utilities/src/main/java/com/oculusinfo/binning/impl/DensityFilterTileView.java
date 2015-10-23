/*
 * Copyright (c) 2015 Uncharted Software. http://www.uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Filters out data bellow a certain threshold by replacing data with specified default "null" value.
 * Handles buckets and accepts a list of bucket ranges. If for every range, the sum of the bucket values is less than
 * the threshold, that bin is filtered out.
 */
public class DensityFilterTileView<T extends Number> implements TileData<List<T>> {
	private static final long serialVersionUID = 1234567890L;

	private TileData<List<T>> _base = null;
	private List<List<Integer>> _buckedRanges = null;
	private double _threshold = 0;
	private Number _defaultValue = null;


	public DensityFilterTileView (TileData<List<T>> base, List<List<Integer>> buckedRanges, double threshold, Number defaultValue) {
		_base = base;
		_buckedRanges = buckedRanges;
		_threshold = threshold;
		_defaultValue = defaultValue;
	}

	@Override
	public TileIndex getDefinition () {
		return _base.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () { return _base.getDefaultValue(); }

	@Override
	// method not implemented as this view is to be read only
	public void setBin(int x, int y, List<T> value)  {}


	@SuppressWarnings("unchecked")
	@Override
	public List<T> getBin (int x, int y) {
		List<T> binContents = _base.getBin(x, y);
		int binSize = binContents.size();
		Boolean foundGreaterThan = false;

		for (List<Integer> range : _buckedRanges) {
			int start = range.get(0);
			int end = range.get(1);
			start = (start >= 0) ? start : 0;
			end = (end < binSize) ? end : binSize;

			double density = 0;
			for(int i = start; i < end; i++) {
				Number value = binContents.get(i);
				density = density + value.doubleValue();
			}

			// Only return the bin if it achieves a minimum density over the range
			if (density >= _threshold) {
				foundGreaterThan = true;
			}
		}
		if (foundGreaterThan) {
			return binContents;
		}
		// If all ranges are below the default "null" the bin contents
		return Arrays.asList((T)_defaultValue);
	}

	@Override
	public Collection<String> getMetaDataProperties () {
		return _base.getMetaDataProperties();
	}

	@Override
	public String getMetaData (String property) {
		return _base.getMetaData(property);
	}

	@Override
	public void setMetaData (String property, Object value) {
		_base.setMetaData(property, value);
	}

}
