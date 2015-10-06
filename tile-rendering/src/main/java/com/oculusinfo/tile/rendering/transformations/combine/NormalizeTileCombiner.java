package com.oculusinfo.tile.rendering.transformations.combine;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.*;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.BinaryOperator;
import com.oculusinfo.binning.util.TileIOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Combiner class to normalize one dataset against another
 */
public class NormalizeTileCombiner<T> implements TileCombiner<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(NormalizeTileCombiner.class);

	TileSerializer<?> _serializer = null;
	PyramidIO _pyramidIO = null;
	private String _dataId = null;

	public NormalizeTileCombiner(PyramidIO pyramidIO, TileSerializer<?> serializer, String dataId){
		_dataId = dataId;
		_pyramidIO = pyramidIO;
		_serializer = serializer;
	}

	private int getBucketCount(TileData<List<?>> data) throws JSONException {
		int size = data.getBin(0,0).size();

		if (size == 0) {
			// Try to get from metadata
			String maxArray = data.getMetaData("maximum array");

			if (maxArray != null) {
				size = new JSONArray(maxArray).length();
			}
		}

		return size;
	}

	public TileData<T> combine(TileData<T> data, TileIndex index, TileSerializer<T> serializer, int coarseness, JSONObject tileProperties) throws IOException, JSONException {
		TileData normalizingData = TileIOUtils.tileDataForIndex(index, _dataId, _serializer, _pyramidIO, coarseness, tileProperties);

		// Need to make sure TileData is of type List<?> for use with BinaryOperationTileView
		if (!(data.getDefaultValue() instanceof List<?>)) {
			data = new ListTileView(data);
		}

		if (!(normalizingData.getDefaultValue() instanceof List<?>)) {
			normalizingData = new ListTileView(normalizingData);
		}

		// Both datasets need to have the same number of buckets for BinaryOperationTileView
		int normalizeSize = getBucketCount((TileData<List<?>>)normalizingData);
		int dataSize = getBucketCount((TileData<List<?>>)data);

		if (dataSize < normalizeSize) {
			data = new ScaledTileView(data, normalizeSize);
		} else if (normalizeSize < dataSize) {
			normalizingData = new ScaledTileView(normalizingData, dataSize);
		}

		return new BinaryOperationTileView(
			data, normalizingData, BinaryOperator.OPERATOR_TYPE.DIVIDE, 0.0);
	}
}
