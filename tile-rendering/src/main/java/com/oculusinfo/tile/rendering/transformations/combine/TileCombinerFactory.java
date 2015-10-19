/*
 * Copyright (c) 2015 Uncharted Software. http://www.uncharted.software/
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
package com.oculusinfo.tile.rendering.transformations.combine;

import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.JSONProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.tile.rendering.transformations.tile.*;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Factory class to create the standard types of Tile Combiners
 *
 */
public class TileCombinerFactory extends ConfigurableFactory<TileCombiner<?>> {

	public static final List<String> DATA_PATH = Collections.unmodifiableList(Arrays.asList("data"));
	public static final List<String> PYRAMID_IO_PATH = Collections.unmodifiableList( Arrays.asList( "data","pyramidio" ) );
	public static final List<String> SERIALIZER_PATH = Collections.unmodifiableList( Arrays.asList( "data","serializer" ) );

	public static StringProperty TILE_COMBINER_TYPE = new StringProperty("type",
		"The type of Combiner desired.",
		"identity");

	public static final StringProperty DATA_ID = new StringProperty("id",
		"The ID of the data source of the layer; exact format depends on how the layer is stored.",
		null);

	// There is no way to produce a Class<TileTransformer<?>> directly; the best one can do is fake it through erasure.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Class<TileCombiner<?>> getFactoryClass () {
		return (Class) TileCombiner.class;
	}

	public TileCombinerFactory(FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
							   FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
							   ConfigurableFactory<?> parent,
							   List<String> path) {
		this(null, pyramidIOFactoryProvider, serializationFactoryProvider, parent, path);
	}

	public TileCombinerFactory(String name,
							   FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
							   FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
							   ConfigurableFactory<?> parent,
							   List<String> path) {
		super(name, getFactoryClass(), parent, path);
		addProperty(TILE_COMBINER_TYPE);
		addProperty(DATA_ID, DATA_PATH);

		addChildFactory( pyramidIOFactoryProvider.createFactory(this, PYRAMID_IO_PATH) );
		addChildFactory( serializationFactoryProvider.createFactory(this, SERIALIZER_PATH) );
	}

	@Override
	protected TileCombiner<?> create () throws ConfigurationException {

		String transformerTypes = getPropertyValue(TILE_COMBINER_TYPE);

		if ("normalize".equals(transformerTypes)) {
			try {
				PyramidIO pyramidIO = produce( PyramidIO.class );
				TileSerializer<?> serializer = produce( TileSerializer.class );
				String dataId = getPropertyValue(DATA_ID);
				return new NormalizeTileCombiner(pyramidIO, serializer, dataId);
			} catch (Exception e) {
				return null;
			}
		} else {  // 'identity' or none passed in will give the default transformer
			return new IdentityTileCombiner();
		}
	}
}
