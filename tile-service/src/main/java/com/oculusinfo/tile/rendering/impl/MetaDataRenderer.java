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
import java.util.Collections;
import java.util.List;

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

public class MetaDataRenderer implements TileDataImageRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataRenderer.class);

    @Override
    public BufferedImage render (LayerConfiguration config) {
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
        String layer = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
        TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);

        int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
        int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
        PyramidIO pyramidIO = config.produce(PyramidIO.class);

        List<TileData<T>> tiles = pyramidIO.readTiles(layer, serializer, Collections.singleton(index));

        bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        // Transparent background
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, width, height);

        if (!tiles.isEmpty()) {
            List<String> texts = new ArrayList<>();

            for (TileData<?> tile: tiles) {
                for (String property: tile.getMetaDataProperties()) {
                    texts.add(property+": "+tile.getMetaData(property));
                }
            }

            int n = texts.size();

            g.setClip(null);

            int centerX = width/2;
            int centerY = height/2;
            g.setColor(new Color(255, 255, 128, 192));
            for (int i=0; i<n; ++i) {
                double offset = (2*i + 1 - n) / 2.0;
                int baseline = (int) Math.round(centerY + offset * 16 - 2);
                g.drawString(texts.get(i), centerX, baseline);
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