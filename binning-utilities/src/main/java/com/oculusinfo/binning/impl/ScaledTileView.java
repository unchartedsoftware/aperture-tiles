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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of TileData that scales the number of bin values to a desired bin size by making duplicates
 * of the value where needed. Allows operations to be performed on two datasets with a different number of bin values.
 */
public class ScaledTileView<T> implements TileData<List<T>> {

	TileData<List<T>> _data;
	int _desiredBuckets;

	public ScaledTileView(TileData<List<T>> data, int desiredBuckets) {
		_data = data;
		_desiredBuckets = desiredBuckets;
	}

	@Override
	public TileIndex getDefinition () {
		return _data.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () {
		return _data.getDefaultValue();
	}

	@Override
	// method not implemented as this view is to be read only
	public void setBin(int x, int y, List<T> value)  {}

	@Override
	public List<T> getBin (int x, int y) {
		List<T> bin = _data.getBin(x, y);
		List<T> results = new ArrayList(_desiredBuckets);
		double multiple = _desiredBuckets / (double)bin.size();
		double count = 0;

		// Stretch the data list to match the desired bucket count
		for (int i = 0; i < bin.size(); i++) {
			count += multiple;

			while (count > 1) {
				results.add(bin.get(i));
				count -= 1.0;
			}
		}

		if (results.size() < _desiredBuckets) {
			results.add(bin.get(bin.size()-1));
		}

		return results;
	}

	@Override
	public Collection<String> getMetaDataProperties () {
		return _data.getMetaDataProperties();
	}

	@Override
	public String getMetaData (String property) {
		return _data.getMetaData(property);
	}

	@Override
	public void setMetaData (String property, Object value) {
		_data.setMetaData(property, value);
	}
}
