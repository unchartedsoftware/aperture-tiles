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
import com.oculusinfo.binning.util.Operator;




/**
 * This implementation of TileData takes a two TileData views/tiles whose bins are single values (such as an average)
 *  that are compared to each other using the operator passed in as a string.
 *
 */
public class OperationTileBucketView<T> implements TileData<List<T>> {
	private static final long serialVersionUID = 1234567890L;

	private TileData<T> _opTileAverage 	= null;
	private TileData<T> _opTileCompare 	= null;
	private Operator 	_op 			= null;


	public OperationTileBucketView (TileData<T> opTileAverage, TileData<T> opTileCompare, String op) {
		_opTileAverage = opTileAverage;
		_opTileCompare = opTileCompare;

		_op = new Operator(op);
		
		if (   getDefinition().getXBins() != _opTileAverage.getDefinition().getXBins()
			|| getDefinition().getYBins() != _opTileAverage.getDefinition().getYBins()) {
			throw new IllegalArgumentException("Constructor for OperationTileBucketView: arguments are invalid. Tiles to compare are incompatible");
		}
	}


	@Override
	public TileIndex getDefinition () {
		return _opTileCompare.getDefinition();
	}


	@Override
	// method not implemented as this view is to be read only
	public void setBin(int x, int y, List<T> value)  {
		if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public List<T> getBin (int x, int y) {
		if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
		List<T> result = new ArrayList<>();
		result.add( (T)_op.Calculate( (Number)_opTileCompare.getBin(x, y), (Number)_opTileAverage.getBin(x, y) ) );
		return result;
	}


	@Override
	public Collection<String> getMetaDataProperties () {
		return _opTileCompare.getMetaDataProperties();
	}


	@Override
	public String getMetaData (String property) {
		return _opTileCompare.getMetaData(property);
	}


	@Override
	public void setMetaData (String property, Object value) {
		_opTileCompare.setMetaData(property, value);
	}

}
