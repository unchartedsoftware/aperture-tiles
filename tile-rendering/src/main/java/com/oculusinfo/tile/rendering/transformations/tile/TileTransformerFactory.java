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

import java.util.List;

import com.oculusinfo.factory.ConfigurationException;
import org.json.JSONObject;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.JSONProperty;
import com.oculusinfo.factory.properties.StringProperty;


/**
 * Factory class to create the standard types of Tile Transformers
 *
 * @author tlachapelle
 */

public class TileTransformerFactory extends ConfigurableFactory<TileTransformer<?>> {


	public static StringProperty TILE_TRANSFORMER_TYPE 	= new StringProperty("type",
		"The type of Transformer desired.",
		"identity");

	public static JSONProperty INITIALIZATION_DATA = new JSONProperty("data",
		"Data to be passed to the tile transformer for read initialization",
		null);

	// There is no way to produce a Class<TileTransformer<?>> directly; the best one can do is fake it through erasure.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Class<TileTransformer<?>> getFactoryClass () {
		return (Class) TileTransformer.class;
	}


	public TileTransformerFactory( ConfigurableFactory<?> parent,
								   List<String> path ) {
		this(null, parent, path);
	}

	public TileTransformerFactory( String name,
								   ConfigurableFactory<?> parent,
								   List<String> path) {
		super(name, getFactoryClass(), parent, path);
		addProperty(TILE_TRANSFORMER_TYPE);
		addProperty(INITIALIZATION_DATA);
	}

	@Override
	protected TileTransformer<?> create () throws ConfigurationException {

		String transformerTypes = getPropertyValue(TILE_TRANSFORMER_TYPE);

		if ("filtervars".equals(transformerTypes)) {
			JSONObject variables = getPropertyValue(INITIALIZATION_DATA);
			return new FilterVarsDoubleArrayTileTransformer<>(variables);
		} else if ("filterbucket".equals(transformerTypes)) {
			JSONObject arguments = getPropertyValue(INITIALIZATION_DATA);
			return new FilterByBucketTileTransformer<>(arguments);
		} else if ("filtertopicbucket".equals(transformerTypes)) {
			JSONObject arguments = getPropertyValue(INITIALIZATION_DATA);
			return new FilterTopicByBucketTileTransformer<>(arguments);
		} else if ("avgdivbucket".equals(transformerTypes)) {
			JSONObject arguments = getPropertyValue(INITIALIZATION_DATA);
			return new AvgDivBucketTileTransformer<>(arguments);
		} else if ("avglogbucket".equals(transformerTypes)) {
			JSONObject arguments = getPropertyValue(INITIALIZATION_DATA);
			return new AvgLogBucketTileTransformer<>(arguments);
		} else {  // 'identity' or none passed in will give the default transformer
			return new IdentityTileTransformer<Object>();
		}
	}
}
