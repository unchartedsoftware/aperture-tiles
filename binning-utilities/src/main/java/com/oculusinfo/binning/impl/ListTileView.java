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
public class ListTileView<T extends Number> implements TileData<List<T>> {
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
