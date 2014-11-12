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
package com.oculusinfo.tile.rest.tile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.transformations.tile.TileTransformer;
import com.oculusinfo.tile.rest.layer.LayerService;
import com.oculusinfo.tile.util.AvroJSONConverter;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author dgray
 *
 */
@Singleton
public class TileServiceImpl implements TileService {
	private static final Logger LOGGER = LoggerFactory.getLogger(TileServiceImpl.class);
	private static final Color COLOR_BLANK = new Color(255,255,255,0);

	private LayerService _layerService;

	@Inject
	public TileServiceImpl ( LayerService layerService ) {
        _layerService = layerService;
	}


	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getTile(int, double, double)
	 */
	@Override
	public BufferedImage getTileImage( String layer, TileIndex index, Iterable<TileIndex> tileSet, JSONObject query ) {
		int width = 256;
		int height = 256;
		BufferedImage bi = null;

		try {

			LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );

            // set level extrema
            PyramidMetaData metadata = _layerService.getMetaData( layer );
            String minimum = metadata.getCustomMetaData(""+index.getLevel(), "minimum");
            String maximum = metadata.getCustomMetaData(""+index.getLevel(), "maximum");
            config.setLevelProperties( index, minimum, maximum );

			// Record image dimensions in case of error. 
			width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
			height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);

			TileDataImageRenderer tileRenderer = config.produce(TileDataImageRenderer.class);

			config.prepareForRendering(layer, index, tileSet);

			bi = tileRenderer.render(config);
		} catch (ConfigurationException e) {
			LOGGER.warn("No renderer specified for tile request. "+ e.getMessage());
		} catch (IllegalArgumentException e) {
            LOGGER.info("Renderer configuration not recognized.");
        }

		if (bi == null){
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bi.createGraphics();
			g.setColor(COLOR_BLANK);
			g.fillRect(0, 0, 256, 256);
			g.dispose();
		}

		return bi;
	}

	@Override
	public JSONObject getTileObject( String layer, TileIndex index, Iterable<TileIndex> tileSet, JSONObject query) {
		try {
		    LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );

            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
		    PyramidIO pyramidIO = config.produce(PyramidIO.class);
			TileSerializer<?> serializer = config.produce(TileSerializer.class);

			config.prepareForRendering(layer, index, tileSet);

			InputStream tile = pyramidIO.getTileStream( dataId, serializer, index );
			if (null == tile) {
                return null;
            }
			TileTransformer transformer = config.produce(TileTransformer.class);
			JSONObject deserializedJSON = AvroJSONConverter.convert(tile);
            return transformer.transform(deserializedJSON);
			
		} catch (IOException | JSONException | ConfigurationException e) {
			LOGGER.warn("Exception getting tile for {}", index, e);
		}  catch (IllegalArgumentException e) {
            LOGGER.info("Renderer configuration not recognized.");
        }
		return null;
	}
}
