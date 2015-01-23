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
package com.oculusinfo.tile.rendering.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.oculusinfo.binning.util.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;

public class MetaDataRenderer implements TileDataImageRenderer<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataRenderer.class);
    private static final double TEXT_SCALE_FACTOR = 6.0;

    private List<String> _components;
    private double _horizontalAlignment;
    private double _verticalAlignment;

    public MetaDataRenderer (List<String> components, double halign, double valign) {
        _components = components;
        _horizontalAlignment = halign;
        _verticalAlignment = valign;
    }

    @Override
    public Class<Object> getAcceptedBinClass() {
        return null;
    }

    @Override
    public TypeDescriptor getAcceptedTypeDescriptor() {
        return new TypeDescriptor(getAcceptedBinClass());
    }

    @Override
    public BufferedImage render (TileData<Object> _unused_, LayerConfiguration config) {
        try {
            TileSerializer<?> serializer = config.produce(TileSerializer.class);

            return renderInternal(config, serializer);
        } catch (ConfigurationException|IOException e) {
            LOGGER.warn("Error producing tile", e);
        }

        return null;
    }

    private <T> BufferedImage renderInternal (LayerConfiguration config, TileSerializer<T> serializer) throws ConfigurationException, IOException {
        BufferedImage bi;
        String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
        TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);

        int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
        int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
        PyramidIO pyramidIO = config.produce(PyramidIO.class);

        List<TileData<T>> tiles = pyramidIO.readTiles(dataId, serializer, Collections.singleton(index));

        bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        // Transparent background
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        if (!tiles.isEmpty()) {
            List<String> texts = new ArrayList<>();

            for (TileData<?> tile: tiles) {
                Collection<String> metadataProperties = tile.getMetaDataProperties();
                if (null == _components || _components.isEmpty()) {
                    // No preset components to show; just show everything.
                    for (String property: metadataProperties) {
                        texts.add(property+": "+tile.getMetaData(property));
                    }
                } else {
                    // Show preset components in the order they appear in our component list
                    for (String component: _components) {
                        if (metadataProperties.contains(component))
                            texts.add(component+": "+tile.getMetaData(component));
                    }
                }
            }

            int n = texts.size();

            g.setClip(null);

            int baselineX = (int) (width*_horizontalAlignment);
            int baselineY = (int) (height*_verticalAlignment);
//            int centerX = width/2;
//            int centerY = height/2;
            g.setColor(new Color(255, 255, 128, 192));
            
            int maxlen = 0;
            for (int i=0; i<n; ++i) {
            	maxlen = Math.max(maxlen, texts.get(i).length());
            }
            int posX = baselineX - (int) ((maxlen*_horizontalAlignment)*TEXT_SCALE_FACTOR);
//            int posX = centerX - maxlen / 2 * 6;
            
            for (int i=0; i<n; ++i) {
                double offset = i+(1-n)*_verticalAlignment;
//                double offset = (2*i + 1 - n) / 2.0;
                int baseline = (int) Math.round(baselineY + offset * 16 - 2);
                g.drawString(texts.get(i), posX, baseline);
            }
        }

        return bi;
    }

    @Override
    public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
        return 0;
    }

    @Override
    public Pair<Double, Double> getLevelExtrema (LayerConfiguration config) throws ConfigurationException {
        return new Pair<Double, Double>(0.0, 0.0);
    }
}