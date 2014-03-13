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

package com.oculusinfo.tile.rendering;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.ColorRampFactory;
import com.oculusinfo.tile.rendering.impl.DoublesImageRenderer;
import com.oculusinfo.tile.rendering.impl.DoublesSeriesImageRenderer;
import com.oculusinfo.tile.rendering.impl.DoublesStatisticImageRenderer;
import com.oculusinfo.tile.rendering.impl.TopAndBottomTextScoresImageRenderer;
import com.oculusinfo.tile.rendering.impl.TopTextScoresImageRenderer;

public class ImageRendererFactory extends ConfigurableFactory<TileDataImageRenderer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageRendererFactory.class);



    private static StringProperty RENDERER_TYPE = new StringProperty("type",
                                                                     "The type of renderer that will be used to render the data on the server",
                                                                     "heatmap",
                                                                     new String[] {"heatmap", "toptextscores", "textscores", "doubleeseries",
                                                                                   "doublestatistics" }
                                                                     );



    private ConfigurableFactory<?> _parent;
    public ImageRendererFactory (ConfigurableFactory<?> parent,
                                 List<String> path) {
        this(null, parent, path);
    }

    public ImageRendererFactory (String name, ConfigurableFactory<?> parent,
                                 List<String> path) {
        super(name, TileDataImageRenderer.class, parent, path);

        _parent = parent;

        addProperty(RENDERER_TYPE);
        addChildFactory(new ColorRampFactory(this, new ArrayList<String>()));
    }

    // All this method does is check types programatically, at least as much as is actually possible; as such, it supercedes the warnings hidden here.  This is inherently somewhat dangerous, but there's not much we can do about it.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> TileSerializer<T> checkBinClass (TileSerializer<?> serializer, Class<T> expectedBinClass, TypeDescriptor expandedExpectedBinClass) throws ConfigurationException {
        if (null == serializer) {
            throw new ConfigurationException("No serializer given for renderer");
        }
        if (!expandedExpectedBinClass.equals(serializer.getBinTypeDescription())) {
            throw new ConfigurationException("Serialization type does not match rendering type.  Serialization class was "+serializer.getBinTypeDescription()+", renderer type was "+expandedExpectedBinClass);
        }
        return (TileSerializer) serializer;
    }

    @Override
    protected TileDataImageRenderer create () {
        String rendererType = getPropertyValue(RENDERER_TYPE);

        try {
            PyramidIO pyramidIO = _parent.getNewGood(PyramidIO.class);
            TileSerializer<?> serializer = _parent.getNewGood(TileSerializer.class);
            ColorRamp colorRamp = getNewGood(ColorRamp.class);

    		rendererType = rendererType.toLowerCase();

            if ("heatmap".equals(rendererType)) {
                return new DoublesImageRenderer(pyramidIO,
                                                checkBinClass(serializer,
                                                              DoublesImageRenderer.getRuntimeBinClass(),
                                                              DoublesImageRenderer.getRuntimeTypeDescriptor()),
                                                colorRamp);
            } else if ("toptextscores".equals(rendererType)) {
                return new TopTextScoresImageRenderer(pyramidIO,
                                                      checkBinClass(serializer,
                                                                    TopTextScoresImageRenderer.getRuntimeBinClass(),
                                                                    TopTextScoresImageRenderer.getRuntimeTypeDescriptor()),
                                                      colorRamp);
            } else if ("textscores".equals(rendererType)) {
                return new TopAndBottomTextScoresImageRenderer(pyramidIO,
                                                               checkBinClass(serializer,
                                                                             TopTextScoresImageRenderer.getRuntimeBinClass(),
                                                                             TopTextScoresImageRenderer.getRuntimeTypeDescriptor()),
                                                               colorRamp);
            } else if ("doubleeseries".equals(rendererType)) {
                return new DoublesSeriesImageRenderer(
                                                      pyramidIO,
                                                      checkBinClass(serializer,
                                                                    DoublesSeriesImageRenderer.getRuntimeBinClass(),
                                                                    DoublesSeriesImageRenderer.getRuntimeTypeDescriptor()),
                                                      colorRamp);
            } else if ("doublestatistics".equals(rendererType)) {
                return new DoublesStatisticImageRenderer(pyramidIO, checkBinClass(serializer,
                                                                                  DoublesStatisticImageRenderer.getRuntimeBinClass(),
                                                                                  DoublesStatisticImageRenderer.getRuntimeTypeDescriptor()));
            } else {
                return null;
            }
                
        } catch (ConfigurationException e) {
            LOGGER.warn("Error getting server-side tile renderer.", e);
            return null;
        }
	}
}
