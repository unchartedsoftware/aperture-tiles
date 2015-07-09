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


import java.util.ArrayList;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.impl.FilterTileBucketView;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 	This transformer will take in JSON or tileData object representing bins of a double array
 * 		tile and will filter out all counts in the bins representing the time buckets indicated
 * 		in the configuration.  The double arrays passed back will be in the order that they are
 * 		sequenced in the JSON/Tile array passed in.
 *
 */

public class FilterByBucketTileTransformer<T> extends BucketTileTransformer<T> {

	public FilterByBucketTileTransformer(JSONObject arguments){
		super(arguments);
	}


	@Override
	public JSONObject transform (JSONObject inputJSON) throws JSONException {
		return inputJSON; // not implemented
	}

	/*
	 * (non-Javadoc)
	 * @see com.oculusinfo.tile.rendering.transformations.tile.TileTransformer#transform(com.oculusinfo.binning.TileData)
	 *
	 * Note: This transformer explicitly transforms all tiles into a dense tile format.  If a sparse tile is
	 * 			passed in, the values not explicitly represented will be set to null.
	 */
	@Override
	public TileData<List<T>> transform (TileData<List<T>> inputData) throws Exception {
		if ( _startBucket != null && _endBucket != null ) {
			if ( _startBucket > _endBucket ) {
				throw new IllegalArgumentException("Filter by time transformer arguments are invalid.  start bucket: " + _startBucket + ", end bucket: " + _endBucket);
			}
		}
		return new FilterTileBucketView<>(inputData, _startBucket, _endBucket);
	}
}
