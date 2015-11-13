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

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for bucket transformers that provides extrema calculation.
 */
public abstract class BucketTileTransformer<T> implements TileTransformer<List<T>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BucketTileTransformer.class);

	protected Integer _startBucket = null;
	protected Integer _endBucket = null;

	private List<Double> _minVals = null;
	private List<Double> _maxVals = null;

	public BucketTileTransformer(JSONObject arguments){
		if ( arguments != null ) {
			// get the start and end time range
			_startBucket = arguments.optInt("startBucket");
			_endBucket = arguments.optInt("endBucket");
		} else {
			LOGGER.warn("No arguments passed in to transformer " + getClass().getSimpleName());
		}
	}


	@Override
	public Pair<Double, Double> getTransformedExtrema(LayerConfiguration config) throws ConfigurationException {
		return getRawExtrema(config);
	}

	@Override
	public Pair<Double, Double> getRawExtrema(LayerConfiguration config) throws ConfigurationException {
		// Parse the mins and maxes for the buckets out of the supplied JSON.
		if (_minVals == null) {
			String layer = config.getPropertyValue(LayerConfiguration.LAYER_ID);
			_minVals = parseExtremum(config, LayerConfiguration.LEVEL_MINIMUMS, "minimum", layer, 0.0);
			_maxVals = parseExtremum(config, LayerConfiguration.LEVEL_MAXIMUMS, "maximum", layer, 1000.0);
			if (_startBucket == null) {
				_startBucket = 0;
				_endBucket = _minVals.size();
			}
		}
		// Compute the min/max for the range of buckets.
		double minimumValue = Double.MIN_VALUE;
		double maximumValue = -Double.MAX_VALUE;
		if (_startBucket == _endBucket) {
			minimumValue = _minVals.get(_startBucket);
			maximumValue = _maxVals.get(_startBucket);
		} else {
			_endBucket = (_endBucket <= _minVals.size()) ? _endBucket : _minVals.size();
			for (int i = _startBucket; i < _endBucket; i++) {
				Double val = _minVals.get(i);
				if (val < minimumValue) {
					minimumValue = val;
				}
				val = _maxVals.get(i);
				if (val > maximumValue) {
					maximumValue = val;
				}
			}
		}
		return new Pair<>(minimumValue, maximumValue);
	}

	/**
	 * Extracts the extremum for a level from its LayerConfig
	 */
	private List<Double> parseExtremum (LayerConfiguration parameter, StringProperty property, String propName,
	                                          String layer, Double def) {
		String rawValue;
		try {
			rawValue = parameter.getPropertyValue(property);
		} catch (ConfigurationException e) {
			rawValue = null;
		}
		ArrayList<Double> values = null;

		// If the is no extremum info available return the default.
		if (rawValue == null) {
			values = new ArrayList<>(1);
			values.add(def);
			return values;
		}

		// Convert string into json object
		try {
			JSONArray ex = new JSONArray(rawValue);
			values = new ArrayList<>(ex.length());
			for (int i = 0; i < ex.length(); i++) {
				values.add(ex.getJSONObject(i).getDouble(propName));
			}
			return values;
		} catch (NumberFormatException|NullPointerException e) {
			LOGGER.warn("Bad " + propName + " value " + rawValue + " for " + layer + ", defaulting to " + def);
			values.clear();
			values.add(def);
			return values;
		} catch (JSONException e) {
			LOGGER.warn("JSON parse exception", e);
			values.clear();
			values.add(def);
			return values;
		}
	}
}
