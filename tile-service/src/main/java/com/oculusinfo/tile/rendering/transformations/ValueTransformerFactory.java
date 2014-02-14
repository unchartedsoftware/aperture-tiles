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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering.transformations;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.tile.util.JsonUtilities;

/**
 * A factory for creating {@link IValueTransformer} objects.
 * 
 * @author cregnier
 *
 */
public class ValueTransformerFactory {

	private static final Logger logger = LoggerFactory.getLogger(ValueTransformerFactory.class);
	
	/**
	 * The default transform type.
	 */
	public static final String DEFAULT_TRANSFORM_NAME = "linear";
	
	/**
	 * Creates a new {@link IValueTransformer} based on the parameters in tranformParams.
	 *  
	 * @param transformParams
	 * 	This object can be a couple of different types.
	 *  <br><br>String: Treats the transformParams as just a simple name. For example: "log10", or "linear"
	 *  <br><br>{@link JSONObject}: Treats the transformParams as a group of parameters to supply data to
	 *  the selected value transformer. The 'name' parameter is required or else
	 *  {@link #DEFAULT_TRANSFORM_NAME} will be used.
	 *  <br><br>{@link JSONArray}: Treats the transformParams as a single element String or JSONObject.
	 *  <br><br>Anything else: Treats the transformParams as the default type. 
	 * @param levelMin
	 * 	The minimum value seen in the level data.
	 * @param levelMax
	 * 	The maximum value seen in the level data.
	 * @return
	 * 	Returns the new {@link IValueTransformer}
	 */
	public static IValueTransformer create(Object transformParams, double levelMin, double levelMax) {
		IValueTransformer t;
		
		String name = JsonUtilities.getName(transformParams);
		if (name == null) {
			logger.warn("layers.transform name was invalid. Using default.");
			name = DEFAULT_TRANSFORM_NAME;
		}
		
		//if the transformParams is a JSONObject then cast it, else use an empty one (null object pattern)
		JSONObject transform = (transformParams instanceof JSONObject)? (JSONObject)transformParams : new JSONObject();
		
		if(name.equalsIgnoreCase("log10")){ 
			t = new Log10ValueTransformer(levelMax);
		}else if (name.equalsIgnoreCase("minmax")) {
			double min = JsonUtilities.getDoubleOrElse(transform, "min", levelMin);
			double max = JsonUtilities.getDoubleOrElse(transform, "max", levelMax);
			t = new LinearCappedValueTransformer(min, max);
		}else { //if 'linear'
			t = new LinearCappedValueTransformer(levelMin, levelMax);
		}
		return t;
	}

}
