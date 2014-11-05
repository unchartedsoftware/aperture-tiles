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

import com.google.inject.Inject;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.transformation.TileTransformer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;

import java.util.List;

public class StandardLayerConfigurationProvider implements FactoryProvider<LayerConfiguration>{

    @Inject
    private FactoryProvider<PyramidIO> _pyramidIOFactoryProvider;
    @Inject
    private FactoryProvider<AnnotationIO> _annotationIOFactoryProvider;
    @Inject
    private FactoryProvider<TilePyramid> _tilePyramidFactoryProvider;
    @Inject
    private FactoryProvider<TileSerializer<?>> _serializationFactoryProvider;
    @Inject
    private FactoryProvider<TileDataImageRenderer> _rendererFactoryProvider;
    @Inject
    private FactoryProvider<TileTransformer> _tileTransformerFactoryProvider;
    @Inject
    private FactoryProvider<AnnotationFilter> _filterFactoryProvider;


    @Override
    public ConfigurableFactory<LayerConfiguration> createFactory (List<String> path) {
        return new LayerConfiguration(_pyramidIOFactoryProvider,
                                      _annotationIOFactoryProvider,
                                      _tilePyramidFactoryProvider,
                                      _serializationFactoryProvider,
                                      _rendererFactoryProvider, 
                                      _tileTransformerFactoryProvider,
                                      _filterFactoryProvider,
                                      null, path);
    }

    @Override
    public ConfigurableFactory<LayerConfiguration> createFactory (ConfigurableFactory<?> parent,                                                     List<String> path) {
        return new LayerConfiguration(_pyramidIOFactoryProvider,
                                      _annotationIOFactoryProvider,
                                      _tilePyramidFactoryProvider,
                                      _serializationFactoryProvider,
                                      _rendererFactoryProvider,
                                      _tileTransformerFactoryProvider,
                                      _filterFactoryProvider,
                                      parent, path);
    }

    @Override
    public ConfigurableFactory<LayerConfiguration> createFactory (String factoryName,
                                                                  ConfigurableFactory<?> parent,
                                                                  List<String> path) {
        return new LayerConfiguration(_pyramidIOFactoryProvider,
                                      _annotationIOFactoryProvider,
                                      _tilePyramidFactoryProvider,
                                      _serializationFactoryProvider,
                                      _rendererFactoryProvider,
                                      _tileTransformerFactoryProvider,
                                      _filterFactoryProvider,
                                      factoryName, parent, path);
    }
}
