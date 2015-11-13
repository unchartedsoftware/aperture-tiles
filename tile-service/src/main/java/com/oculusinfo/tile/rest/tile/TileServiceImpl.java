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
import com.oculusinfo.binning.impl.SubTileDataView;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.AvroJSONConverter;
import com.oculusinfo.binning.util.TileIOUtils;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.transformations.combine.TileCombiner;
import com.oculusinfo.tile.rendering.transformations.tile.TileTransformer;
import com.oculusinfo.tile.rest.layer.LayerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;


@Singleton
public class TileServiceImpl implements TileService {
	private static final Logger LOGGER = LoggerFactory.getLogger( TileServiceImpl.class );
	private static final Color COLOR_BLANK = new Color( 255, 255, 255, 0 );

	private LayerService _layerService;

	@Inject
	public TileServiceImpl( LayerService layerService ) {
		_layerService = layerService;
	}

	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getTile(int, double, double)
	 */
	@Override
	public BufferedImage getTileImage( String layer, TileIndex index, Iterable<TileIndex> tileSet, JSONObject query ) {
		BufferedImage bi = null;

		// get layer configuration
		LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );

		try {
			// set level extrema
			PyramidMetaData metadata = _layerService.getMetaData( layer );
			String minimum = metadata.getCustomMetaData( "" + index.getLevel(), "minimum" );
			String maximum = metadata.getCustomMetaData( "" + index.getLevel(), "maximum" );
			config.setLevelProperties( index, minimum, maximum );
			// produce the tile renderer from the configuration
			TileDataImageRenderer<?> tileRenderer = config.produce( TileDataImageRenderer.class );
			bi = renderTileImage( config, layer, index, tileSet, tileRenderer );

		} catch ( ConfigurationException e ) {
			LOGGER.warn( "No renderer specified for tile request. " + e.getMessage() );
		} catch ( IllegalArgumentException e ) {
			LOGGER.info( "Renderer configuration not recognized." );
		} catch ( Exception e ) {
			LOGGER.warn( "Tile is corrupt: " + layer + ":" + index );
			LOGGER.warn( "Tile error: ", e );
		}

		// always return a blank tile if there is no data
		if ( bi == null ) {
			int outputWidth = 256;
			int outputHeight = 256;
			try {
				outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
				outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
			} catch (ConfigurationException e) {
				LOGGER.warn("Error reading image height or width; defaulting to "+outputWidth+" x "+outputHeight, e);
			}
			bi = new BufferedImage( outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB );
			Graphics2D g = bi.createGraphics();
			g.setColor( COLOR_BLANK );
			g.fillRect( 0, 0, 256, 256 );
			g.dispose();
		}
		return bi;
	}

	private <T> BufferedImage renderTileImage( LayerConfiguration config, String layer,
											   TileIndex index, Iterable<TileIndex> tileSet,
											   TileDataImageRenderer<T> renderer ) throws ConfigurationException, IOException, Exception {
		// prepare for rendering
		config.prepareForRendering( layer, index, tileSet );

		// get data source id, and produce the pyramidio and serializer
		// these are all common points of failure in the config, so explicitly log these
		String dataId = config.getPropertyValue( LayerConfiguration.DATA_ID );
		if ( dataId == null ) {
			LOGGER.error( "Could not determine data id for layer:" + layer + ", please confirm that it has been configured correctly." );
			return null;
		}
		PyramidIO pyramidIO = config.produce( PyramidIO.class );
		if ( dataId == null ) {
			LOGGER.error( "Could not produce pyramidio for layer:" + layer + ", please confirm that it has been configured correctly and data is avalable." );
			return null;
		}
		@SuppressWarnings("unchecked")
		TileSerializer<T> serializer = config.produce( TileSerializer.class );
		if ( serializer == null ) {
			LOGGER.error( "Could not produce tile serializer, please confirm that it has been configured correctly." );
		}

		int coarseness = config.getPropertyValue( LayerConfiguration.COARSENESS );

		@SuppressWarnings("unchecked")
		TileTransformer<T> tileTransformer = config.produce(TileTransformer.class);
		TileCombiner<T> tileCombiner = config.produce(TileCombiner.class);

		JSONObject tileProperties = config.getPropertyValue(LayerConfiguration.FILTER_PROPS);

		TileData<T> data = TileIOUtils.tileDataForIndex(index, dataId, serializer, pyramidIO, coarseness, tileProperties);

		if (data == null) {
			return null;
		}

		TileData<T> modifiedData = tileCombiner.combine(data, index, coarseness, tileProperties);
		modifiedData = tileTransformer.transform( modifiedData );

		Boolean applyAlphaRamp = config.getPropertyValue( LayerConfiguration.ALPHA_RAMP );
		if ( data != null ) {
			return renderer.render( modifiedData, applyAlphaRamp ? data : null, config );
		}
		return null;
	}

	@Override
	public JSONObject getTileObject( String layer, TileIndex index, Iterable<TileIndex> tileSet, JSONObject query ) {
		try {
			// get layer configuration
			LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );

			// get data source id, and produce the pyramidio and serializer
			// these are all common points of failure in the config, so explicitly log these
			String dataId = config.getPropertyValue( LayerConfiguration.DATA_ID );
			if ( dataId == null ) {
				LOGGER.error( "Could not determine data id for layer:" + layer + ", please confirm that it has been configured correctly." );
				return null;
			}
			PyramidIO pyramidIO = config.produce( PyramidIO.class );
			if ( dataId == null ) {
				LOGGER.error( "Could not produce pyramidio for layer:" + layer + ", please confirm that it has been configured correctly and data is avalable." );
				return null;
			}
			@SuppressWarnings("unchecked")
			TileSerializer<?> serializer = config.produce( TileSerializer.class );
			if ( serializer == null ) {
				LOGGER.error( "Could not produce tile serializer, please confirm that it has been configured correctly." );
			}

			// prepare for rendering
			config.prepareForRendering( layer, index, tileSet );

			// pull tile data from pyramid io
			InputStream tile = pyramidIO.getTileStream( dataId, serializer, index );
			if ( null == tile ) {
				return null;
			}
			// produce transformer, return transformed de-serialized data
			TileTransformer<?> transformer = config.produce( TileTransformer.class );
			JSONObject deserializedJSON = AvroJSONConverter.convert( tile );
			return transformer.transform( deserializedJSON );

		} catch ( IOException | JSONException | ConfigurationException e ) {
			LOGGER.warn( "Exception getting tile for {}", index, e );
		} catch ( IllegalArgumentException e ) {
			LOGGER.info( "Renderer configuration not recognized." );
		}
		return null;
	}
}
