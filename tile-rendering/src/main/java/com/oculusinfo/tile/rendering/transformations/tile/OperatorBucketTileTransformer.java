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
import com.oculusinfo.binning.impl.AverageTileBucketView;
import com.oculusinfo.binning.impl.OperationTileBucketView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 	This transformer will take in JSON or tileData object representing bins of a double array
 * 		tile and will filter out all counts in the bins representing the time buckets indicated
 * 		in the configuration.  The double arrays passed back will be in the order that they are
 * 		sequenced in the JSON/Tile array passed in.
 *
 */

public class OperatorBucketTileTransformer<T> implements TileTransformer<List<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger( OperatorBucketTileTransformer.class );

	private Integer _averageRange 	= 0;
	private Integer _startBucket 	= 0;
	private Integer _endBucket 		= 0;
	private String  _operator 		= "";


	public OperatorBucketTileTransformer(JSONObject arguments){
		if ( arguments != null ) {
			// get the start and end time range
			_averageRange 	= arguments.optInt("averageRange");
			_startBucket 	= arguments.optInt("startBucket");
			_endBucket 		= arguments.optInt("endBucket");

			// get the operator
			_operator = arguments.optString("operator");
		} else {
			LOGGER.warn("No arguements passed in to filterbucket transformer");
		}
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
			if ( _startBucket < 0 || _startBucket > _endBucket ) {
				throw new IllegalArgumentException("Average filter by time transformer arguments are invalid");
			}
		}
		// calculate the start and end of the average to compare
		int halfRange = (int) Math.floor(_averageRange/2);
		JSONArray maxBuckets = new JSONArray(inputData.getMetaData("maximum array"));
		int lastBucket = maxBuckets.length()-1;
		
		// if we start at 0 we still need to use _averageRange of buckets for the first average tile view so we use range 0 to (_averageRange-1)
		int startA = _startBucket - halfRange < 0 ? _startBucket : _startBucket - halfRange;
		int endA = (_endBucket + halfRange -1) > lastBucket ? lastBucket : (_endBucket + halfRange -1);
		
		// if the range overflows, we need to move the start back until the entire _averageRange number of buckets is used for the first average tile view
		if ( (startA + _averageRange) - 1 > endA ) {
			startA = (endA - _averageRange + 1) < 0 ? 0 : (endA - _averageRange + 1);
		}		
		// if the end underflows we need to set the end to be minumum of start + averageRange
		if ( endA - (_averageRange-1) < 0 ) {
			endA = startA + (_averageRange-1) < lastBucket ? startA + (_averageRange-1) : lastBucket;
		}
		
		// if the _average range is odd, we need to add one to the end of the range, unless we are overflowing, then we subtract one from the start
		if ( (_averageRange & 1) != 0 ) {
			if ( endA == lastBucket ) {
				startA = startA - 1 < 0 ? 0 : startA - 1;
			} else {
				endA++;
			}
		}
		
		AverageTileBucketView<T> opTileA = new AverageTileBucketView<>(inputData, startA, endA);
		AverageTileBucketView<T> opTileB = new AverageTileBucketView<>(inputData, _startBucket, _endBucket);
		return new OperationTileBucketView<>( opTileA, opTileB, _operator );
	}

}
