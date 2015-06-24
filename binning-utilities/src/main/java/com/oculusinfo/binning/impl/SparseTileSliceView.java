/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
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



import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;



/**
 * This implementation of TileData takes a TileData whose bins are semantically lists of buckets -
 * but actually sparse maps from bucket index to value = and presents a view to a single slice of
 * it - the same bucket in each bin.
 *
 * @author nkronenfeld
 */
public class SparseTileSliceView<T> implements TileData<T> {
    private static final long serialVersionUID = 6508527738055941360L;



    private TileData<Map<Integer, T>> _base;
    private int                       _slice;
    private T                         _defaultValue;

    public SparseTileSliceView (TileData<Map<Integer, T>> base, int slice, T defaultValue) {
        _base = base;
        _slice = slice;
        _defaultValue = defaultValue;
    }



    @Override
    public TileIndex getDefinition () {
        return _base.getDefinition();
    }

	@Override
	public T getDefaultValue () { return _defaultValue; }

    @Override
    public void setBin (int x, int y, T value) {
        Map<Integer, T> originalValue = _base.getBin(x, y);
        Map<Integer, T> newValue = new HashMap<>(originalValue);
        newValue.put(_slice, value);
        _base.setBin(x, y, newValue);
    }

    @Override
    public T getBin (int x, int y) {
        Map<Integer, T> originalValue = _base.getBin(x, y);
        if (null != originalValue && originalValue.containsKey(_slice))
            return originalValue.get(_slice);
        return _defaultValue;
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
