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
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.impl.SerializationTypeChecker;
import com.oculusinfo.tile.rendering.transformations.tile.TileTransformer;
import com.oculusinfo.tile.rest.layer.LayerService;
import com.oculusinfo.tile.util.AvroJSONConverter;
import com.oculusinfo.tile.util.TileDataView;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Singleton
public class TileServiceImpl implements TileService {
	private static final Logger LOGGER = LoggerFactory.getLogger(TileServiceImpl.class);
	private static final Color COLOR_BLANK = new Color(255,255,255,0);

	private LayerService _layerService;

	@Inject
	public TileServiceImpl ( LayerService layerService ) {
        _layerService = layerService;
	}


	private <T> TileData<T> tileDataForIndex(TileIndex index, String dataId, TileSerializer<T> serializer, PyramidIO pyramidIO, int coarseness) throws IOException {
		TileData<T> data = null;
		if (coarseness > 1) {
			int coarsenessFactor = (int)Math.pow(2, coarseness - 1);

			// Coarseness support:
			// Find the appropriate tile data for the given level and coarseness
			java.util.List<TileData<T>> tileDatas = null;
			TileIndex scaleLevelIndex = null;

			// need to get the tile data for the level of the base level minus the coarseness
			for (int coarsenessLevel = coarseness - 1; coarsenessLevel >= 0; --coarsenessLevel) {
				scaleLevelIndex = new TileIndex(index.getLevel() - coarsenessLevel,
						(int) Math.floor(index.getX() / coarsenessFactor),
						(int) Math.floor(index.getY() / coarsenessFactor));

				tileDatas = pyramidIO.readTiles(dataId, serializer, Collections.singleton(scaleLevelIndex));
				if (tileDatas.size() >= 1) {
					//we got data for this level so use it
					break;
				}
			}

			// Missing tiles are commonplace and we didn't find any data up the tree either.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
				LOGGER.info("Missing tile " + index + " for layer data id " + dataId);
				return null;
			}

			// We're using a scaled tile so wrap in a view class that will make the source data look like original tile we're looking for
			data = TileDataView.fromSourceAbsolute(tileDatas.get(0), index);
		} else {
			// No coarseness - use requested tile
			java.util.List<TileData<T>> tileDatas = pyramidIO.readTiles(dataId, serializer, Collections.singleton(index));
			if (!tileDatas.isEmpty()) {
				data = tileDatas.get(0);
			}
		}

		return data;
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
            // get layer configuration
			LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );
            // set level extrema
            PyramidMetaData metadata = _layerService.getMetaData( layer );
            String minimum = metadata.getCustomMetaData(""+index.getLevel(), "minimum");
            String maximum = metadata.getCustomMetaData(""+index.getLevel(), "maximum");
            config.setLevelProperties( index, minimum, maximum );

            // produce the tile renderer from the configuration
			TileDataImageRenderer<?> tileRenderer = config.produce(TileDataImageRenderer.class);
			bi = renderTileImage(config, layer, index, tileSet, tileRenderer);

		} catch (ConfigurationException e) {
			LOGGER.warn("No renderer specified for tile request. "+ e.getMessage());
		} catch (IllegalArgumentException e) {
            LOGGER.info("Renderer configuration not recognized.");
		} catch (Exception e) {
			LOGGER.warn("Tile is corrupt: " + layer + ":" + index);
			LOGGER.warn("Tile error: ", e);
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

	private <T> BufferedImage renderTileImage (LayerConfiguration config, String layer,
	                                           TileIndex index, Iterable<TileIndex> tileSet,
	                                           TileDataImageRenderer<T> renderer) throws ConfigurationException, IOException {
        // prepare for rendering
		config.prepareForRendering(layer, index, tileSet);

		String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
		PyramidIO pyramidIO = config.produce(PyramidIO.class);
		TileSerializer<T> serializer = SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
		                                                                      renderer.getAcceptedBinClass(),
		                                                                      renderer.getAcceptedTypeDescriptor());

		int coarseness = config.getPropertyValue(LayerConfiguration.COARSENESS);
		TileData<T> data = tileDataForIndex(index, dataId, serializer, pyramidIO, coarseness);

		if (data != null) {
			return renderer.render(data, config);
		} else {
			return null;
		}
	}

	@Override
	public JSONObject getTileObject( String layer, TileIndex index, Iterable<TileIndex> tileSet, JSONObject query) {
		try {
            // get layer configuration
		    LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );
            // get data source id, and produce pyramid io and serializer
            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
		    PyramidIO pyramidIO = config.produce(PyramidIO.class);
			TileSerializer<?> serializer = config.produce(TileSerializer.class);
            // prepare for rendering
			config.prepareForRendering(layer, index, tileSet);
            // pull tile data from pyramid io
			InputStream tile = pyramidIO.getTileStream( dataId, serializer, index );
			if (null == tile) {
                return null;
            }
            // produce transformer, return transformed de-serialized data
			TileTransformer<?> transformer = config.produce(TileTransformer.class);
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
