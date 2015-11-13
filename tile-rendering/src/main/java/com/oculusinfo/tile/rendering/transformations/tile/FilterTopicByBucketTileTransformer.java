/*
 * Copyright (c) 2015 Uncharted Software.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering.transformations.tile;

import java.util.List;

import com.oculusinfo.binning.TileData;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 	This transformer is specifically designed for twitter data.  The information to be transformed
 *  	will be stored in the tile's metadata and represent time buckets of twitter keywords that
 *  	will include the number of times referenced within the time bucket it is found in as well
 *  	as the ten most recent tweets for that bucketed time frame.
 *
 */
public class FilterTopicByBucketTileTransformer<T> extends BucketTileTransformer<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger( FilterTopicByBucketTileTransformer.class );

	public FilterTopicByBucketTileTransformer(JSONObject arguments){
		super(arguments);
	}

	@Override
	public JSONObject transform (JSONObject inputJSON) throws JSONException {
		JSONObject resultJSON = new JSONObject(inputJSON);
		if (inputJSON.has("meta")) {
			resultJSON.put("meta", filterKeywordMetadata( inputJSON.getJSONObject("meta") ));
		}
		return resultJSON;
	}

	@Override
	public TileData<List<T>> transform (TileData<List<T>> inputData) throws Exception {
		TileData<List<T>> resultTile = inputData;

		// add in metadata to the tile
		JSONObject metadata = new JSONObject(inputData.getMetaData("meta"));
		JSONObject filteredMetadata = null;
		if ( metadata.length() > 0 ) {
			filteredMetadata = filterKeywordMetadata(metadata);
			if (null != filteredMetadata) {
				resultTile.setMetaData("meta", filteredMetadata);
			}
		}

		return resultTile;
	}


	public JSONObject filterKeywordMetadata(JSONObject inputMetadata) throws JSONException {
		if ( _startBucket != null && _endBucket != null ) {
			if ( _startBucket > _endBucket ) {
				throw new IllegalArgumentException("Filter by keyword bucket transformer arguments are invalid.  start time bucket: " + _startBucket + ", end time bucket: " + _endBucket);
			}
		}

		JSONObject map = inputMetadata.getJSONObject("map");
		JSONArray bins = map.getJSONArray("bins");

		JSONArray filteredBins = new JSONArray();

		int binSize = bins.length();
		int start = ( _startBucket != null ) ? _startBucket : 0;
		int end = ( _endBucket != null ) ? _endBucket : (binSize-1);

		for ( int i=0; i<binSize; i++ ) {
			if ( i >= start && i <= end ) {
				filteredBins.put(i, bins.optJSONArray(i));
			} else {
				filteredBins.put(i, (JSONArray)null);
			}
		}
		map.put("bins", filteredBins);

		return inputMetadata;
	}

	@Override
	public Pair<Double, Double> getTransformedExtrema(LayerConfiguration config) throws ConfigurationException {
		return new Pair<>(0.0d, 0.0);
	}

}
