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



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.BinaryOperator;




/**
 * This implementation of TileData takes a two TileData views/tiles whose bins are single values (such as an average)
 *  that are compared to each other using the operator passed in as a string.
 *
 */
public class BinaryOperationTileView<T extends Number> implements TileData<List<T>> {
	private static final long serialVersionUID = 1234567890L;

	private TileData<List<T>> _tileData1 = null;
	private TileData<List<T>> _tileData2 = null;
	private BinaryOperator _op 			= null;

	private final Number _errorValue;

	public BinaryOperationTileView(TileData<List<T>> tileData1, TileData<List<T>> tileData2, BinaryOperator.OPERATOR_TYPE op,
								   Number errorValue) {
		_errorValue = errorValue;
		_tileData1 = tileData1;
		_tileData2 = tileData2;

		_op = new BinaryOperator(op);

		if (   getDefinition().getXBins() != _tileData1.getDefinition().getXBins()
			|| getDefinition().getYBins() != _tileData1.getDefinition().getYBins()) {
			throw new IllegalArgumentException("Constructor for BinaryOperationTileBucketView: " +
				"arguments are invalid. Tiles to compare are incompatible");
		}
	}


	@Override
	public TileIndex getDefinition () {
		return _tileData2.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () {
		List<T> default1 = _tileData1.getDefaultValue();
		List<T> default2 = _tileData2.getDefaultValue();
		if (null == default1 || null == default2) return null;
		else {
			List<T> ourDefault = new ArrayList<>();
			for (int i=0; i<Math.min(default1.size(), default2.size()); ++i) {
				ourDefault.add((T) _op.calculate(default1.get(i), default2.get(i), _errorValue));
			}
			return ourDefault;
		}
	}

	@Override
	// method not implemented as this view is to be read only
	public void setBin(int x, int y, List<T> value)  {}


	@SuppressWarnings("unchecked")
	@Override
	public List<T> getBin (int x, int y) {
		List<T> result = new ArrayList<>();
		for (int i = 0; i < _tileData1.getBin(x, y).size(); i++) {
			result.add((T) _op.calculate(_tileData1.getBin(x, y).get(i), _tileData2.getBin(x, y).get(i), _errorValue));
		}
		return result;
	}


	@Override
	public Collection<String> getMetaDataProperties () {
		return _tileData2.getMetaDataProperties();
	}


	@Override
	public String getMetaData (String property) {
		return _tileData2.getMetaData(property);
	}


	@Override
	public void setMetaData (String property, Object value) {
		_tileData2.setMetaData(property, value);
	}

}
