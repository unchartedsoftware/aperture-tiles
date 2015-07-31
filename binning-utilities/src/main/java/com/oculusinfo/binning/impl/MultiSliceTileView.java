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
 * This implementation of TileData takes a several TileDatas whose bins are lists of buckets, and presents
 * a view to single tile, whose bins are composed of the bins of each component tile.
 *
 * No effort is made to check for consistent lengths of buckets between bins; therefore setting bin values is
 * disallowed (since we don't know the length to apply to each component).
 *
 * @author nkronenfeld
 */
public class MultiSliceTileView<T> implements TileData<List<T>> {
	List<TileData<List<T>>> _components;
	public MultiSliceTileView (List<TileData<List<T>>> components) {
		TileIndex i0 = null;
		for (TileData<List<T>> component: components) {
			if (null == i0) i0 = component.getDefinition();
			else if (!i0.equals(component.getDefinition()))
				throw new IllegalArgumentException("Attempt to create a multislice tile view with incompatible components");
		}
		_components = Collections.unmodifiableList(new ArrayList<>(components));
	}

	@Override
	public TileIndex getDefinition() {
		return _components.get(0).getDefinition();
	}

	@Override
	public List<T> getDefaultValue() {
		List<T> ourDefault = new ArrayList<>();
		for (TileData<List<T>> component: _components) {
			List<T> componentDefault = component.getDefaultValue();
			if (null == componentDefault)
				// If one component has a null default, we can't aggregate any of the defaults properly.
				return null;
			ourDefault.addAll(componentDefault);
		}
		return ourDefault;
	}

	@Override
	public void setBin(int x, int y, List<T> value) {
		throw new UnsupportedOperationException("Setting values of multi-slice tile views isn't allowed - see class javadocs");
	}

	@Override
	public List<T> getBin(int x, int y) {
		List<T> binValue = new ArrayList<>();
		for (TileData<List<T>> component: _components) {
			List<T> componentBin = component.getBin(x, y);
			if (null == componentBin)
				// Because we don't know the standard bin length of each component, if any are null, we have to give
				// up.
				return null;
			else
				binValue.addAll(componentBin);
		}
		return binValue;
	}

	@Override
	public Collection<String> getMetaDataProperties() {
		return _components.get(0).getMetaDataProperties();
	}

	@Override
	public String getMetaData(String property) {
		return _components.get(0).getMetaData(property);
	}

	@Override
	public void setMetaData(String property, Object value) {
		// We could allow this, but for consistence with setBin, we don't.
		throw new UnsupportedOperationException("Setting values of multi-slice tile views isn't allowed - see class javadocs");
	}
}
