package com.oculusinfo.binning.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Allows you to scale tiles with bins containing a smaller number of values to one with a greater number of values
 */
public class ScaledTileView<T extends Number> implements TileData<List<T>> {

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
				results.add(bin.get(i));	// TODO: Instead of just copying values may want to interpolate between data points
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
