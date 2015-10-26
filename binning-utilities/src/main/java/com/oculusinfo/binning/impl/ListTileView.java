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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Allows you to view a tile with single values bins as one with bins of multiple values
 */
public class ListTileView<T> implements TileData<List<T>> {
	TileData<T> _data;

	public ListTileView(TileData<T> data) {
		_data = data;
	}

	@Override
	public TileIndex getDefinition () {
		return _data.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () {
		return Arrays.asList(_data.getDefaultValue());
	}

	@Override
	// method not implemented as this view is to be read only
	public void setBin(int x, int y, List<T> value)  {}

	@Override
	public List<T> getBin (int x, int y) {
		return Arrays.asList(_data.getBin(x, y));
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
