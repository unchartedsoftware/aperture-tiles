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
package com.oculusinfo.sparktile.init;

import java.util.List;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.TileSerializerFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.providers.AbstractFactoryProvider;
import com.oculusinfo.tilegen.graph.analytics.GraphAnalyticsRecord;
import com.oculusinfo.tilegen.graph.analytics.GraphAnalyticsAvroSerializer;

public class GraphTileSerializationFactory extends ConfigurableFactory<TileSerializer<List<GraphAnalyticsRecord>>> {
	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Class<TileSerializer<List<GraphAnalyticsRecord>>> getGenericSerializerClass () {
		return (Class) TileSerializer.class;
	}

	public GraphTileSerializationFactory (ConfigurableFactory<?> parent, List<String> path) {
		super("[graphanalyticsrecord]-a", getGenericSerializerClass(), parent, path);
	}

	@Override
	protected TileSerializer<List<GraphAnalyticsRecord>> create () throws ConfigurationException {
		return new GraphAnalyticsAvroSerializer(TileSerializerFactory.getCodecFactory(this));
	}

	public static class GraphTileSerializationFactoryDelegate extends AbstractFactoryProvider<TileSerializer<?>> {
		@Override
		public ConfigurableFactory<? extends TileSerializer<?>> createFactory (String name,
		                                                                       ConfigurableFactory<?> parent,
		                                                                       List<String> path) {
			return new GraphTileSerializationFactory(parent, path);
		}
	}
}
