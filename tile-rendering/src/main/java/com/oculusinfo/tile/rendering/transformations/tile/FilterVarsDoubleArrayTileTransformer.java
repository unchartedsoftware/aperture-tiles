/*
 * Copyright (c) 2014 Oculus Info Inc.
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
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
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
import java.util.Arrays;
import java.util.List;

import static com.oculusinfo.tile.rendering.LayerConfiguration.*;


/**
 * 	This transformer will take in JSON or tileData object representing bins of a double array
 * 		tile and will filter out all variables except the variables contained in the variable
 * 		array passed in during construction.  The double arrays passed back will be in the
 * 		order that they are sequenced in the JSON or tileData array passed in.
 *
 */

public class FilterVarsDoubleArrayTileTransformer<T> extends BucketTileTransformer<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterVarsDoubleArrayTileTransformer.class);

	private List<Integer> _variables = new ArrayList<>();
	private List<Double> _minVals = null;
	private List<Double> _maxVals = null;

	public FilterVarsDoubleArrayTileTransformer(JSONObject variables){
		super(variables);
		// Get the JSONArray out of the variables JSONObject
		try {
			if (variables == null) {
				_variables = null;
			}
			else {
				// store as as Integer array
				JSONArray variablesJSON = variables.getJSONArray("variables");
				for (int i = 0; i < variablesJSON.length(); i++) {
					_variables.add(variablesJSON.getInt(i));
				}
			}
		}
		catch (JSONException e) {
			LOGGER.warn("Exception getting variables for filter variables transformer", e);
		}
	}

	// For each bin in the tile described by the input JSON, extract the values from the
	//	bin's array only at the indexes stored in the _variables list and build the resulting'
	// 	JSON based on this criteria
	@Override
	public JSONObject transform (JSONObject inputJSON) throws JSONException {
		JSONObject resultJSON;

		if ( _variables == null ) {
			resultJSON = inputJSON;
		}
		else {
			resultJSON = new JSONObject();
			resultJSON.put("level", inputJSON.getInt("level"));
			resultJSON.put("xIndex", inputJSON.getInt("xIndex"));
			resultJSON.put("yIndex", inputJSON.getInt("yIndex"));
			resultJSON.put("xBinCount", inputJSON.getInt("xBinCount"));
			resultJSON.put("yBinCount", inputJSON.getInt("yBinCount"));

			JSONArray bins = inputJSON.getJSONArray("values");
			JSONArray resultBins = new JSONArray();

			for (int binIndex = 0; binIndex < bins.length(); binIndex++) {
				JSONObject singleBin = bins.getJSONObject (binIndex);
				JSONArray valuesInBin = singleBin.getJSONArray("value");

				JSONObject resultSingleBin = new JSONObject();
				JSONArray resultValuesInSingleBin = new JSONArray();

				// just loop through variable indexes in _variables
				for (int varIndex = 0; varIndex < _variables.size(); varIndex++) {
					int arrayIndex = _variables.get(varIndex);
					if (arrayIndex < valuesInBin.length()) {
						JSONObject value = valuesInBin.getJSONObject(arrayIndex);
						JSONObject resultValue = new JSONObject();
						resultValue.put("value", value.getDouble("value"));
						resultValuesInSingleBin.put(resultValue);
					}
				}
				resultSingleBin.put("value", resultValuesInSingleBin);
				resultBins.put(resultSingleBin);
			}
			resultJSON.put("values", resultBins);

			if (inputJSON.has("meta")) {
				resultJSON.put("meta", inputJSON.getJSONObject("meta"));
			}
		}
		return resultJSON;
	}

    @Override
    public TileData<List<T>> transform (TileData<List<T>> inputData) throws Exception {

        //list of indices to keep
        TileData<List<T>> resultTile;

        //If there are none to keep, return empty list
        if (_variables == null) {
            resultTile = null;
        } else {
			TileIndex index = inputData.getDefinition();
			List<List<T>> rawData = DenseTileData.getData(inputData);
			List<List<T>> transformedData = new ArrayList<>(index.getXBins()*index.getYBins());
			for (List<T> rawEntry: rawData) {
				int size = rawEntry.size();
				List<T> transformedEntry = new ArrayList<>(_variables.size());

				for (int varIndex : _variables) {
					if (varIndex < size)
						transformedEntry.add(rawEntry.get(varIndex));
				}
				transformedData.add(transformedEntry);
			}

            resultTile = new DenseTileData<>(inputData.getDefinition(), transformedData);
        }

        return resultTile;
    }

	@Override
	public Pair<Double, Double> getTransformedExtrema(LayerConfiguration config) throws ConfigurationException {
		// Parse the mins and maxes for the buckets out of the supplied JSON.
		if (_minVals == null) {
			String layer = config.getPropertyValue(LAYER_ID);
			_minVals = parseExtremum(config, LEVEL_MINIMUMS, "minimum", layer, 0.0);
			_maxVals = parseExtremum(config, LEVEL_MAXIMUMS, "maximum", layer, 1000.0);
		}
		// Compute the min/max for the range of buckets.
		double minimumValue = Double.MIN_VALUE;
		double maximumValue = -Double.MAX_VALUE;
		for (Integer index: _variables) {
			Double val = _minVals.get(index);
			if (val < minimumValue) {
				minimumValue = val;
			}
			val = _maxVals.get(index);
			if (val > maximumValue) {
				maximumValue = val;
			}
		}
		return new Pair<>(minimumValue,  maximumValue);
	}

	// Extracts all the extrema
	private List<Double> parseExtremum (LayerConfiguration parameter, StringProperty property, String propName,
										String layer, Double def) {
		String rawValue = null;
		// Convert string into json object
		try {
			rawValue = parameter.getPropertyValue(property);
			JSONArray ex = new JSONArray(rawValue);
			List<Double> values = new ArrayList<>(ex.length());
			for (int i = 0; i < ex.length(); i++) {
				values.add(ex.getJSONObject(i).getDouble(propName));
			}
			return values;
		} catch (ConfigurationException e) {
			LOGGER.warn("Can not determine value for property "+propName+", defaulting to " + def);
			return Arrays.asList(def);
		} catch (NumberFormatException|NullPointerException e) {
			LOGGER.warn("Bad " + propName + " value " + rawValue + " for " + layer + ", defaulting to " + def);
			return Arrays.asList(def);
		} catch (JSONException e) {
			LOGGER.warn("JSON parse exception for property "+propName+", defaulting to "+def, e);
			return Arrays.asList(def);
		}
	}
}
