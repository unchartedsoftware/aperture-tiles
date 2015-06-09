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
import com.oculusinfo.binning.impl.UnaryOperationTileView;
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
 * Created by Chris Bethune on 07/06/2015.
 */
public class AvgLogBucketTileTransformer<T extends Number> extends BucketTileTransformer<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvgLogBucketTileTransformer.class);

	public AvgLogBucketTileTransformer(JSONObject arguments){
		super(arguments);
	}

	@Override
	public JSONObject transform(JSONObject json) throws JSONException {
		LOGGER.warn("Not implemented");
		return json;
	}

	@Override
	public TileData<List<T>> transform(TileData<List<T>> data) throws Exception {
		AverageTileBucketView<T> avgView = new AverageTileBucketView<>(data, _startBucket, _endBucket);
		UnaryOperationTileView<T> logView = new UnaryOperationTileView<>(UnaryOperator.OPERATOR_TYPE.LOG_10, avgView, 0.0);
		return logView;
	}

	@Override
	public Pair<Double, Double> getTransformedExtrema(LayerConfiguration config) throws ConfigurationException {
		Pair<Double, Double> result = super.getTransformedExtrema(config);
		double min = result.getFirst() <= 0.0 ? 0.0 : Math.log10(result.getFirst());
		double max = result.getSecond() <= 0.0 ? 0.0 : Math.log10(result.getSecond());
		return new Pair<>(min, max);
	}
}
