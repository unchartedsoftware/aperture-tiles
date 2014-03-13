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
package com.oculusinfo.tile.rendering;



import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.IntegerProperty;
import com.oculusinfo.factory.ListProperty;
import com.oculusinfo.factory.StringProperty;
import com.oculusinfo.factory.TileIndexProperty;
import com.oculusinfo.tile.init.FactoryProvider;



/**
 * A tile-service-specific, local factory - the root factory of all factorys
 * associated with the tile image service.
 * 
 * @author nkronenfeld
 */
public class RenderParameterFactory extends ConfigurableFactory<RenderParameter> {
    public static final StringProperty         LAYER_NAME       = new StringProperty("layer",
                                                                                     "The ID of the layer; exact format depends on how the layer is stored.",
                                                                                     null);
    public static final StringProperty         TRANSFORM        = new StringProperty("transform",
                                                                                     "The transformation to apply to the data before display",
                                                                                     "linear", new String[] {"linear", "log10"});
    public static final ListProperty<Integer>  LEGEND_RANGE     = new ListProperty<>(new IntegerProperty("", "", -1),
                                                                                     "legendRange",
                                                                                     "The value bounds to use for coloration");
    public static final IntegerProperty        CURRENT_IMAGE    = new IntegerProperty("currentImage",
                                                                                      "used to determine which of a series of potential images should be displayed.  Ordinarily, this parameter is for programatic use only.",
                                                                                      -1);
    public static final IntegerProperty        COARSENESS       = new IntegerProperty("coarseness",
                                                                                      "Used by the standard heatmap renderer to allow the client to specify getting coarser tiles than needed, for efficiency (if needed)", 1);

    public static final IntegerProperty        LINE_NUMBER      = new IntegerProperty("lineNumber", "For use by the server only", 0);
    public static final IntegerProperty        OUTPUT_WIDTH     = new IntegerProperty("outputEidth", "For use by the server only", 256);
    public static final IntegerProperty        OUTPUT_HEIGHT    = new IntegerProperty("outputHeight", "For use by the server only", 256);
    public static final StringProperty         LEVEL_MINIMUMS   = new StringProperty("levelMinimums", "For use by the server only", null);
    public static final StringProperty         LEVEL_MAXIMUMS   = new StringProperty("levelMaximums", "For use by the server only", null);
    public static final TileIndexProperty      TILE_COORDINATE  = new TileIndexProperty("tileCoordinate", "For use by the server only", null);

    public RenderParameterFactory (FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                                   FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
                                   FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
                                   ConfigurableFactory<?> parent,
                                   List<String> path) {
        this(pyramidIOFactoryProvider, serializationFactoryProvider, rendererFactoryProvider, null, parent, path);
    }

    public RenderParameterFactory (FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                                   FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
                                   FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
                                   String name,
                                   ConfigurableFactory<?> parent,
                                   List<String> path) {
        super(name, RenderParameter.class, parent, path);

        addProperty(LAYER_NAME);
        addProperty(TRANSFORM);
        addProperty(LEGEND_RANGE);
        addProperty(CURRENT_IMAGE);
        addProperty(OUTPUT_WIDTH);
        addProperty(OUTPUT_HEIGHT);
        addProperty(LEVEL_MINIMUMS);
        addProperty(LEVEL_MAXIMUMS);
        addProperty(TILE_COORDINATE);
        addProperty(LINE_NUMBER);
        addProperty(COARSENESS);

        addChildFactory(rendererFactoryProvider.createFactory(this, Collections.singletonList("renderer")));
        addChildFactory(pyramidIOFactoryProvider.createFactory(this, Collections.singletonList("pyramidio")));
        addChildFactory(serializationFactoryProvider.createFactory(this, Collections.singletonList("serializer")));
    }

    @Override
    protected RenderParameter create () {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        for (ConfigurationProperty<?> property: getProperties()) {
            addPropertyValue(property, parameterMap);
        }
        return new RenderParameter(parameterMap);
    }

    private <T> void addPropertyValue (ConfigurationProperty<T> property,
                                       Map<String, Object> parameterMap) {
        if (hasPropertyValue(property)) {
            T value = getPropertyValue(property);
            parameterMap.put(property.getName(), value);
        } else if (null != property.getDefaultValue()) {
            System.out.println("Defaulting value for "+property.getName());
            parameterMap.put(property.getName(), property.getDefaultValue());
        } else {
            System.out.println("Couldn't find value for "+property.getName());
        }
        if (hasPropertyValue(property) || null != property.getDefaultValue()) {
        }
    }
}
