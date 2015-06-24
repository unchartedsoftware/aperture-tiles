/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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
import java.util.Collections;
import java.util.List;

/**
 * This implementation of TileData takes a TileData whose bins are lists of buckets, and presents
 * a view to a single slice of it - the same bucket in each bin.
 *
 * @author nkronenfeld
 */
public class DenseTileMultiSliceView<T> implements TileData<List<T>> {
	private TileData<List<T>> _base;
	private List<Integer>     _slices;
	public DenseTileMultiSliceView (TileData<List<T>> base, List<Integer> slices) {
		_base = base;
		_slices = Collections.unmodifiableList(new ArrayList<>(slices));
	}
	public DenseTileMultiSliceView (TileData<List<T>> base, int minSlice, int maxSlice) {
		_base = base;
		List<Integer> slices = new ArrayList<>();
		for (int i=minSlice; i <= maxSlice; ++i) slices.add(i);
		_slices = Collections.unmodifiableList(slices);
	}

	@Override
	public TileIndex getDefinition() {
		return _base.getDefinition();
	}

	@Override
	public void setBin(int x, int y, List<T> value) {
		assert(value.size() == _slices.size());
		List<T> binOrig = _base.getBin(x, y);
		try {
			for (int i=0; i < _slices.size(); ++i) {
				binOrig.set(_slices.get(i), value.get(i));
			}
		} catch (UnsupportedOperationException e) {
			// List is probably unmodifiable; just create a new one then.
			// Create a copy
			List<T> newValue = new ArrayList<>(binOrig);
			// Modify our values
			for (int i=0; i < _slices.size(); ++i) {
				newValue.set(_slices.get(i), value.get(i));
			}
			// Replace into the original
			// Original was unmodifiable, make this unmodifiable too
			_base.setBin(x, y, Collections.unmodifiableList(newValue));
		} catch (NullPointerException e) {
			// No original list; make one.
			List<T> newValue = new ArrayList<>();
			for (int i=0; i < _slices.size(); ++i) {
				int slice = _slices.get(i);
				while (newValue.size() <= slice) newValue.add(null);
				newValue.set(slice, value.get(i));
			}
			_base.setBin(x, y, newValue);
		}
	}

	@Override
	public List<T> getBin(int x, int y) {
		List<T> origValue = _base.getBin(x, y);
		if (null == origValue) return null;
		List<T> result = new ArrayList<>(_slices.size());
		for (int slice: _slices) {
			if (origValue.size() <= slice) result.add(null);
			else result.add(origValue.get(slice));
		}
		return result;
	}

	@Override
	public Collection<String> getMetaDataProperties() {
		return _base.getMetaDataProperties();
	}

	@Override
	public String getMetaData(String property) {
		return _base.getMetaData(property);
	}

	@Override
	public void setMetaData(String property, Object value) {
		_base.setMetaData(property, value);
	}
}
