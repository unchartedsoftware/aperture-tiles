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

import java.util.ArrayList;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/** 
 * 	This transformer will take in JSON object representing bins of a double array tile and
 * 		will filter out all variables except the variables contained in the variable
 * 		array passed in during construction.  The double arrays passed back will be in the 
 * 		order that they are sequenced in the JSON array passed in through the constructor.
 * 
 */

public class FilterVarsDoubleArrayTileTransformer<T> implements TileTransformer<List<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterVarsDoubleArrayTileTransformer.class);
	
	private List<Integer> _variables = new ArrayList<>();
	
	public FilterVarsDoubleArrayTileTransformer(JSONObject variables){
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

}
