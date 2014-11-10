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
package com.oculusinfo.tile.init.providers;

import java.io.IOException;
import java.util.List;

import com.oculusinfo.tile.rest.annotation.filter.AnnotationFilter;
import com.oculusinfo.binning.io.AnnotationIO;
import com.oculusinfo.binning.TilePyramid;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.transformation.TileTransformer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rest.tile.caching.CachingPyramidIO;
import com.oculusinfo.tile.rest.tile.caching.CachingPyramidIO.LayerDataChangedListener;

@Singleton
public class CachingLayerConfigurationProvider implements FactoryProvider<LayerConfiguration>{
	private static final Logger LOGGER = LoggerFactory.getLogger(CachingLayerConfigurationProvider.class);

    private FactoryProvider<PyramidIO> _pyramidIOFactoryProvider;
    private FactoryProvider<AnnotationIO> _annotationIOFactoryProvider;
    private FactoryProvider<TilePyramid> _tilePyramidFactoryProvider;
    private FactoryProvider<TileSerializer<?>> _serializationFactoryProvider;
    private FactoryProvider<TileDataImageRenderer> _rendererFactoryProvider;
    private FactoryProvider<TileTransformer> _tileTransformerFactoryProvider;
    private FactoryProvider<AnnotationFilter> _filterFactoryProvider;
    private FactoryProvider<PyramidIO> _cachingProvider;
	private CachingPyramidIO _pyramidIO;

    @Inject
    public CachingLayerConfigurationProvider( FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                                              FactoryProvider<AnnotationIO> annotationIOFactoryProvider,
                                              FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
                                              FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
                                              FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
                                              FactoryProvider<TileTransformer> tileTransformerFactoryProvider,
                                              FactoryProvider<AnnotationFilter> filterFactoryProvider ) {

        _pyramidIOFactoryProvider = pyramidIOFactoryProvider;
        _annotationIOFactoryProvider = annotationIOFactoryProvider;
        _tilePyramidFactoryProvider = tilePyramidFactoryProvider;
        _serializationFactoryProvider = serializationFactoryProvider;
        _rendererFactoryProvider = rendererFactoryProvider;
        _tileTransformerFactoryProvider = tileTransformerFactoryProvider;
        _filterFactoryProvider = filterFactoryProvider;
        _cachingProvider = new CachingPyramidIOProvider();
		_pyramidIO = new CachingPyramidIO();
    }

	public void addLayerListener (LayerDataChangedListener listener) {
		_pyramidIO.addLayerListener(listener);
	}

	public void removeLayerListener (LayerDataChangedListener listener) {
		_pyramidIO.removeLayerListener(listener);
	}

	@Override
	public ConfigurableFactory<LayerConfiguration> createFactory (List<String> path) {
		return new CachingLayerConfiguration(null, path);
	}

	@Override
	public ConfigurableFactory<LayerConfiguration> createFactory (ConfigurableFactory<?> parent,
	                                                              List<String> path) {
		return new CachingLayerConfiguration(parent, path);
	}

	@Override
	public ConfigurableFactory<LayerConfiguration> createFactory (String factoryName,
	                                                              ConfigurableFactory<?> parent,
	                                                              List<String> path) {
		return new CachingLayerConfiguration(factoryName, parent, path);
	}


	private class CachingLayerConfiguration extends LayerConfiguration {
		public CachingLayerConfiguration (ConfigurableFactory<?> parent,
		                                  List<String> path) {
			super(_pyramidIOFactoryProvider,
                  _annotationIOFactoryProvider,
                  _tilePyramidFactoryProvider,
                  _serializationFactoryProvider,
                  _rendererFactoryProvider,
                  _tileTransformerFactoryProvider,
                  _filterFactoryProvider,
                  parent, path);
		}


		public CachingLayerConfiguration (String name, ConfigurableFactory<?> parent,
		                                  List<String> path) {
			super(_pyramidIOFactoryProvider,
                  _annotationIOFactoryProvider,
                  _tilePyramidFactoryProvider,
                  _serializationFactoryProvider,
                  _rendererFactoryProvider,
                  _tileTransformerFactoryProvider,
                  _filterFactoryProvider,
                  name, parent, path);
		}

		@Override
		public void prepareForRendering (String layer,
		                                 TileIndex tile,
		                                 Iterable<TileIndex> tileSet) {
			try {
				TileSerializer<?> serializer = produce(TileSerializer.class);
				_pyramidIO.requestTiles(layer, serializer, tileSet);
			} catch (IOException e) {
				LOGGER.warn("Error requesting tile set", e);
			} catch (ConfigurationException e) {
				LOGGER.warn("Error requesting tile set", e);
			}
		}
	}

	private class CachingPyramidIOFactory extends ConfigurableFactory<PyramidIO> {
		private ConfigurableFactory<?>         _parent;
		private ConfigurableFactory<PyramidIO> _baseFactory;
		private boolean                        _baseInitialized;



		CachingPyramidIOFactory (ConfigurableFactory<?> parent,
		                         List<String> path,
		                         ConfigurableFactory<PyramidIO> base) {
			this(null, parent, path, base);
		}

		CachingPyramidIOFactory (String name,
		                         ConfigurableFactory<?> parent,
		                         List<String> path,
		                         ConfigurableFactory<PyramidIO> base) {
			super(name, PyramidIO.class, parent, path);
			_parent = parent;
			_baseFactory = base;
			_baseInitialized = false;

			addProperty(PyramidIOFactory.INITIALIZATION_DATA);
		}

		@Override
		public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
			super.readConfiguration(rootNode);
			_baseFactory.readConfiguration(rootNode);
		}

		private void setupBasePyramidIO () {
			if (!_baseInitialized) {
				String pyramidId = _parent.getPropertyValue(LayerConfiguration.LAYER_ID);
				_pyramidIO.setupBasePyramidIO(pyramidId, _baseFactory);
				_baseInitialized = true;
			}
		}

		@Override
		protected PyramidIO create () {
			setupBasePyramidIO();
			return _pyramidIO;
		}
        
	}
	private class CachingPyramidIOProvider implements FactoryProvider<PyramidIO> {
		@Override
		public ConfigurableFactory<PyramidIO> createFactory (List<String> path) {
			return new CachingPyramidIOFactory(null, path, _pyramidIOFactoryProvider.createFactory(path));
		}

		@Override
		public ConfigurableFactory<PyramidIO> createFactory (ConfigurableFactory<?> parent,
		                                                     List<String> path) {
			return new CachingPyramidIOFactory(parent, path, _pyramidIOFactoryProvider.createFactory(parent, path));
		}

		@Override
		public ConfigurableFactory<PyramidIO> createFactory (String factoryName,
		                                                     ConfigurableFactory<?> parent,
		                                                     List<String> path) {
			return new CachingPyramidIOFactory(parent, path, _pyramidIOFactoryProvider.createFactory(factoryName, parent, path));
		}
	}
}
