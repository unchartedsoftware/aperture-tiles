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


import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.impl.AverageTileBucketView;
import com.oculusinfo.binning.impl.BinaryOperationTileView;
import com.oculusinfo.binning.impl.UnaryOperationTileView;
import com.oculusinfo.binning.util.BinaryOperator;
import com.oculusinfo.binning.util.UnaryOperator;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 	This transformer takes in a range of buckets centered on val V, takes their average, and divides them by the average
 * 	of another range of buckets centered on val V.  The numerator range can be varied externally, while the denominator
 * 	range is fixed.  Log10 of the final result is returned.
 */
public class AvgDivSliceTileTransformer<T extends Number> implements TileTransformer<List<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger( AvgDivSliceTileTransformer.class );

	protected Integer _averageRange = 0;
	protected Integer _startBucket = 0;
	protected Integer _endBucket = 0;
	protected Integer _maxBuckets = 0;

	public AvgDivSliceTileTransformer(JSONObject arguments){
		if ( arguments != null ) {
			// get the start and end time range
			_averageRange = arguments.optInt("averageRange");
			_startBucket = arguments.optInt("startBucket");
			_endBucket = arguments.optInt("endBucket");
		} else {
			LOGGER.warn("No arguments passed in to filterbucket transformer");
		}
	}

	public void setMaxBuckets(int maxBuckets) {
		_maxBuckets = maxBuckets;
	}

	public int getAverageRange() { return _averageRange; }

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
		int numeratorSize = Math.min(_averageRange/2, _endBucket - _startBucket);
		int middle = _averageRange / 2;
		int numeratorStart = middle - (numeratorSize / 2);
		int numeratorEnd = numeratorStart + numeratorSize;

		// Divide average 1 by average 2 and apply log10 to the result.
		AverageTileBucketView<T> numerator = new AverageTileBucketView<>(inputData, numeratorStart, numeratorEnd);
		AverageTileBucketView<T> denominator = new AverageTileBucketView<>(inputData, 0, _averageRange);
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
