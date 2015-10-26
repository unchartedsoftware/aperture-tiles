/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
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
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A TileTransformer is an interface that can take a JSON representation of
 * 		a tile and perform a transform on it. This can include a filter on the data or
 *		perform an action on all sets of the data in a uniform way.  The resulting tile
 *		is passed back in JSON format
 *
 * @author tlachapelle
 */
public interface TileTransformer<T> {

	/**
	 * Transforms the tile data in JSON format based on transform type and returns result
	 *
	 * @param json representing the tile data in JSON form to be transformed
	 * @return JSONObject representing the fully transformed tile based on the transform type
	 */
	JSONObject transform(JSONObject json) throws JSONException;


	/**
	 * Same transformation on the raw tile form
	 * @param data The tile data to be transformed
	 * @return TileData containing the transformed data
	 * @throws Exception
	 */
	//takes tile data x returns tile data x generified on function level
	TileData<T> transform(TileData<T> data) throws Exception;


	/**
	 * Computes transformed level extrema from a layer configuration.
	 *
	 * @param config
	 * @return Pair<Double, Double> - extrema for tile
	 * @throws ConfigurationException
	 */
	Pair<Double, Double> getTransformedExtrema(LayerConfiguration config) throws ConfigurationException;

	/**
	 * Returns the raw extrema from a layer configuration
	 *
	 * @param config
	 * @return Pair<Double, Double> - extrema for tile
	 * @throws ConfigurationException
	 */
	Pair<Double, Double> getRawExtrema(LayerConfiguration config) throws ConfigurationException;
}





