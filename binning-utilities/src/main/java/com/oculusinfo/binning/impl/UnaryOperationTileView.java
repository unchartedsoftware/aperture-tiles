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
import com.oculusinfo.binning.util.UnaryOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UnaryOperationTileView<T extends Number> implements TileData<List<T>> {

	private final UnaryOperator _op;
	private final Number _errorValue;
	private final TileData<List<T>> _tileData;

	public UnaryOperationTileView(UnaryOperator.OPERATOR_TYPE operation, TileData<List<T>> tileData, Number errorValue) {
		_op = new UnaryOperator(operation);
		_tileData = tileData;
		_errorValue = errorValue;
	}

	@Override
	public TileIndex getDefinition() {
		return _tileData.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () {
		List<T> baseDefault = _tileData.getDefaultValue();
		if (null == baseDefault) return null;
		else {
			List<T> ourDefault = new ArrayList<>();
			for (T t: baseDefault) ourDefault.add((T) _op.calculate(t, _errorValue));
		}
		return null;
	}

	@Override
	public void setBin(int x, int y, List<T> value) {}

	@Override
	public List<T> getBin(int x, int y) {
		List<T> result = new ArrayList<>();
		for (T d:  _tileData.getBin(x, y)) {
			result.add((T) _op.calculate(d, _errorValue));
		}
		return result;
	}

	@Override
	public Collection<String> getMetaDataProperties() {
		return _tileData.getMetaDataProperties();
	}

	@Override
	public String getMetaData(String property) {
		return _tileData.getMetaData(property);
	}

	@Override
	public void setMetaData(String property, Object value) {
		_tileData.setMetaData(property, value);
	}
}
