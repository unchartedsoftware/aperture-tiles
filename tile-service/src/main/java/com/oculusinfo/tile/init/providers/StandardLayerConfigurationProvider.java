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
import com.oculusinfo.tile.rendering.transformations.tile.TileTransformer;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.factory.providers.AbstractFactoryProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformer;

import java.util.List;

public class StandardLayerConfigurationProvider extends AbstractFactoryProvider<LayerConfiguration>{

    private FactoryProvider<PyramidIO> _pyramidIOFactoryProvider;
    private FactoryProvider<TilePyramid> _tilePyramidFactoryProvider;
    private FactoryProvider<TileSerializer<?>> _serializationFactoryProvider;
    private FactoryProvider<TileDataImageRenderer<?>> _rendererFactoryProvider;
    private FactoryProvider<TileTransformer<?>> _tileTransformerFactoryProvider;
    private FactoryProvider<ValueTransformer<?>> _valueTransformerFactoryProvider;

    @Inject
    public StandardLayerConfigurationProvider( FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                                               FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
                                               FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
                                               FactoryProvider<TileDataImageRenderer<?>> rendererFactoryProvider,
                                               FactoryProvider<TileTransformer<?>> tileTransformerFactoryProvider,
                                               FactoryProvider<ValueTransformer<?>> valueTransformerFactoryProvider) {

        _pyramidIOFactoryProvider = pyramidIOFactoryProvider;
        _tilePyramidFactoryProvider = tilePyramidFactoryProvider;
        _serializationFactoryProvider = serializationFactoryProvider;
        _rendererFactoryProvider = rendererFactoryProvider;
        _tileTransformerFactoryProvider = tileTransformerFactoryProvider;
        _valueTransformerFactoryProvider = valueTransformerFactoryProvider;
    }

    @Override
    public ConfigurableFactory<LayerConfiguration> createFactory (String name,
                                                                  ConfigurableFactory<?> parent,
                                                                  String path) {
        return new LayerConfiguration( _pyramidIOFactoryProvider,
                                      _tilePyramidFactoryProvider,
                                      _serializationFactoryProvider,
                                      _rendererFactoryProvider,
                                      _tileTransformerFactoryProvider,
                                      _valueTransformerFactoryProvider );
    }
}
