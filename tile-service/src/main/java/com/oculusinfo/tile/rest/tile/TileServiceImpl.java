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
import com.oculusinfo.binning.io.impl.HBaseSlicedPyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.AvroJSONConverter;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.transformations.tile.AvgDivSliceTileTransformer;
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
import java.lang.SuppressWarnings;
import java.util.List;
import java.util.Collections;


@Singleton
public class TileServiceImpl implements TileService {
	private static final Logger LOGGER = LoggerFactory.getLogger( TileServiceImpl.class );
	private static final Color COLOR_BLANK = new Color( 255, 255, 255, 0 );

	private LayerService _layerService;

	@Inject
	public TileServiceImpl( LayerService layerService ) {
		_layerService = layerService;
	}


	private <T> TileData<T> tileDataForIndex( TileIndex index, String dataId, String layer, TileSerializer<T> serializer,
	                                          PyramidIO pyramidIO, TileTransformer<T> tileTransformer, int coarseness,
	                                          JSONObject query, JSONObject tileProperties) throws IOException {
		TileData<T> data = null;
		if ( coarseness > 1 ) {
			int coarsenessFactor = ( int ) Math.pow( 2, coarseness - 1 );

			// Coarseness support:
			// Find the appropriate tile data for the given level and coarseness
			java.util.List<TileData<T>> tileDatas = null;
			TileIndex scaleLevelIndex = null;

			// need to get the tile data for the level of the base level minus the coarseness
			for ( int coarsenessLevel = coarseness - 1; coarsenessLevel >= 0; --coarsenessLevel ) {
				scaleLevelIndex = new TileIndex( index.getLevel() - coarsenessLevel,
					( int ) Math.floor( index.getX() / coarsenessFactor ),
					( int ) Math.floor( index.getY() / coarsenessFactor ) );
				tileDatas = getTileDatas(layer, dataId, serializer, pyramidIO, tileTransformer, query, scaleLevelIndex, tileProperties);
				if ( tileDatas.size() >= 1 ) {
					//we got data for this level so use it
					break;
				}
			}

			// Missing tiles are commonplace and we didn't find any data up the tree either.  We don't want a big long error for that.
			if ( tileDatas.size() < 1 ) {
				LOGGER.info( "Missing tile " + index + " for layer data id " + dataId );
				return null;
			}

			// We're using a scaled tile so wrap in a view class that will make the source data look like original tile we're looking for
			data = SubTileDataView.fromSourceAbsolute( tileDatas.get( 0 ), index );
		} else {
			// No coarseness - use requested tile
			java.util.List<TileData<T>> tileDatas = getTileDatas(layer, dataId, serializer, pyramidIO, tileTransformer, query, index, tileProperties);
			if ( !tileDatas.isEmpty() ) {
				data = tileDatas.get( 0 );
			}
		}

		return data;
	}

	/**
	 * TEMPORARY - checks the pyramid IO and modifies the tile name to include the slice range if SliceHBasePyramidIO
	 * is used.  This needs to be replaced with a more generic request pre-process method can instantiated through
	 * config.
	 */
	private <T> List<TileData<T>> getTileDatas(String layer, String dataId, TileSerializer<T> serializer, PyramidIO pyramidIO,
	                                           TileTransformer<T> tileTransformer, JSONObject query,
	                                           TileIndex scaleLevelIndex, JSONObject tileProperties) throws IOException {
		List<TileData<T>> tileDatas;

		if (pyramidIO instanceof HBaseSlicedPyramidIO) {
			PyramidMetaData metadata = _layerService.getMetaData( layer );
			Integer numBuckets = Integer.parseInt(metadata.getCustomMetaData("bucketCount"));

			Integer start = 0;
			Integer end = numBuckets - 1;
			try {
				JSONObject renderer = query.getJSONObject("tileTransform");
				if (renderer.has("data")) {
					start = renderer.getJSONObject("data").getInt("startBucket");
					end = renderer.getJSONObject("data").getInt("endBucket");
				}
			} catch (Exception e){
				LOGGER.error("Exception processing range from query", e);
			}

			if (tileTransformer instanceof AvgDivSliceTileTransformer) {
				((AvgDivSliceTileTransformer) tileTransformer).setMaxBuckets(numBuckets);
				int range = ((AvgDivSliceTileTransformer) tileTransformer).getAverageRange();
				int middle = (end - start) / 2 + start;
				start = Math.max(0, middle - (range / 2));
				end = Math.min(numBuckets - 1, middle + (range / 2 + range % 2 ));
			}

			String bracket = "[0]";
			if (start != null && end != null) {
				if (start == 0 && end == numBuckets - 1) {
					// If this is uncommented, it will allow us to redirect to a table created as a single bucket,
					// giving us a big boost in fetch times.
					// bracket = "__all__";
					bracket = "";
				} else if (end > start) {
					bracket = "[" + start + "-" + end + "]";
				} else {
					bracket = "[" + start + "]";
				}
			}
			tileDatas = pyramidIO.readTiles(dataId + bracket, serializer, Collections.singleton(scaleLevelIndex), tileProperties);
		} else {
			tileDatas = pyramidIO.readTiles(dataId, serializer, Collections.singleton( scaleLevelIndex ), tileProperties);
		}
		return tileDatas;
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
			bi = renderTileImage( config, layer, index, tileSet, tileRenderer, query );

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
			int outputWidth = config.getPropertyValue( LayerConfiguration.OUTPUT_WIDTH );
			int outputHeight = config.getPropertyValue( LayerConfiguration.OUTPUT_HEIGHT );
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
											   TileDataImageRenderer<T> renderer, JSONObject query)
			throws ConfigurationException, IOException, Exception {
		// prepare for rendering
		config.prepareForRendering( layer, index, tileSet );

		// get data source id, and produce the pyramidio and serializer
		// these are all common points of failure in the config, so explicitly log these
		String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
		if ( dataId == null ) {
			LOGGER.error( "Could not determine data id for layer:" + layer + ", please confirm that it has been configured correctly." );
			return null;
		}
		PyramidIO pyramidIO = config.produce(PyramidIO.class);
		if ( dataId == null ) {
			LOGGER.error( "Could not produce pyramidio for layer:" + layer + ", please confirm that it has been configured correctly and data is avalable." );
			return null;
		}
		@SuppressWarnings("unchecked")
		TileSerializer<T> serializer = config.produce(TileSerializer.class);
		if ( serializer == null ) {
			LOGGER.error( "Could not produce tile serializer, please confirm that it has been configured correctly." );
		}

		int coarseness = config.getPropertyValue( LayerConfiguration.COARSENESS );
		JSONObject tileProperties = config.getPropertyValue(LayerConfiguration.FILTER_PROPS);

		@SuppressWarnings("unchecked")
		TileTransformer<T> tileTransformer = config.produce(TileTransformer.class);
		TileData<T> data = tileDataForIndex(index, dataId, layer, serializer, pyramidIO, tileTransformer, coarseness, query, tileProperties);
		if (data == null) {
			return null;
		}

		data = tileTransformer.transform( data );
		if ( data != null ) {
			return renderer.render( data, config );
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
