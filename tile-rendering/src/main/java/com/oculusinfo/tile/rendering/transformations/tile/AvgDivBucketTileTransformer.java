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


import java.util.Arrays;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.impl.AverageTileBucketView;
import com.oculusinfo.binning.impl.BinaryOperationTileView;

import com.oculusinfo.binning.impl.UnaryOperationTileView;
import com.oculusinfo.binning.util.BinaryOperator;
import com.oculusinfo.binning.util.UnaryOperator;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 	This transformer takes in a range of buckets centered on val V, takes their average, and divides them by the average
 * 	of another range of buckets centered on val V.  The numerator range can be varied externally, while the denominator
 * 	range is fixed.  Log10 of the final result is returned.
 */
public class AvgDivBucketTileTransformer<T extends Number> extends BucketTileTransformer<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger( AvgDivBucketTileTransformer.class );

	protected Integer _averageRange = 0;

	public AvgDivBucketTileTransformer(JSONObject arguments){
		super(arguments);
		if ( arguments != null ) {
			// get the start and end time range
			_averageRange = arguments.optInt("averageRange");
		} else {
			LOGGER.warn("No arguments passed in to filterbucket transformer");
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
			if ( _startBucket > _endBucket ) {
				throw new IllegalArgumentException("Average filter by time transformer arguments are invalid");
			}
		}
		// calculate the start and end of the average to compare
		int halfRange = (int) Math.floor(_averageRange/2);
		JSONArray maxBuckets = new JSONArray(inputData.getMetaData("maximum array"));
		int lastBucket = maxBuckets.length()-1;

		int startA = 0;
		int endA = 0;
		// if range is larger than number of buckets, use all buckets for average
		if ( _averageRange > lastBucket ) {
			endA = lastBucket;
		} else {  // otherwise, calculate the start and end by calculating the centre of the range
			int centreBucket = _startBucket == _endBucket ? _startBucket : _startBucket + halfRange;
			if ( centreBucket + halfRange > lastBucket ) {
				startA = lastBucket - _averageRange + 1;
				endA = lastBucket;
			} else if ( centreBucket - halfRange < 0 ) {
				endA = centreBucket - halfRange < 0 ? _averageRange - 1: centreBucket + halfRange - 1;
			} else {
				startA = centreBucket - halfRange;
				endA = centreBucket + halfRange - 1;
			}
			// if the _average range is odd, we need to add one to the end of the range, unless we are overflowing, then we subtract one from the start
			if ( (_averageRange & 1) != 0 ) {
				if ( endA == lastBucket ) {
					startA = startA - 1 < 0 ? 0 : startA - 1;
				} else {
					endA++;
				}
			}
		}

		// Divide average 1 by average 2 and apply log10 to the result.
		AverageTileBucketView<T> numerator = new AverageTileBucketView<>(inputData, _startBucket, _endBucket);
		AverageTileBucketView<T> denominator = new AverageTileBucketView<>(inputData, startA, endA);
		BinaryOperationTileView<T> binaryOpView = new BinaryOperationTileView<>(
			numerator, denominator, BinaryOperator.OPERATOR_TYPE.DIVIDE, 1.0);
		return new UnaryOperationTileView<>(UnaryOperator.OPERATOR_TYPE.LOG_10, binaryOpView, -(Math.log10(_averageRange)));
	}

	@Override
	public Pair<Double, Double> getTransformedExtrema(LayerConfiguration config) throws ConfigurationException {
		double ext = Math.log10(_averageRange);
		return new Pair<>(-ext, ext);
	}
}
