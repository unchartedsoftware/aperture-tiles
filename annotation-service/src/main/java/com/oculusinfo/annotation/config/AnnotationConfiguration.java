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
package com.oculusinfo.annotation.config;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;



import com.oculusinfo.binning.*;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.init.FactoryProvider;


public class AnnotationConfiguration extends ConfigurableFactory<AnnotationConfiguration> {
	
	public static final List<String> TILE_PYRAMID_PATH = Collections.singletonList("pyramid");    
    public static final List<String> PYRAMID_IO_PATH = Collections.unmodifiableList(Arrays.asList("data","pyramidio"));
    public static final List<String> SERIALIZER_PATH = Collections.unmodifiableList(Arrays.asList("data","serializer"));


	public AnnotationConfiguration (FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
	                                FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
	                                FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
	                                ConfigurableFactory<?> parent,
	                                List<String> path) {
		this(pyramidIOFactoryProvider, serializationFactoryProvider,
				tilePyramidFactoryProvider, null, parent, path);
	}


	public AnnotationConfiguration (FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
		                            FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
		                            FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
		                            String name, ConfigurableFactory<?> parent,
		                            List<String> path) {
		super(name, AnnotationConfiguration.class, parent, path);

		addChildFactory(tilePyramidFactoryProvider.createFactory(this, TILE_PYRAMID_PATH));
		addChildFactory(pyramidIOFactoryProvider.createFactory(this, PYRAMID_IO_PATH));
		addChildFactory(serializationFactoryProvider.createFactory(this, SERIALIZER_PATH));
	}

	@Override
	protected AnnotationConfiguration create () {
		return this;
	}

	@Override
	public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
		super.readConfiguration(rootNode);
	}

	@Override
	public void readConfiguration (Properties properties) throws ConfigurationException {
		super.readConfiguration(properties);
	}

}
