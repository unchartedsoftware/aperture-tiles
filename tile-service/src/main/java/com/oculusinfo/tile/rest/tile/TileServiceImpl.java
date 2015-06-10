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
import com.oculusinfo.binning.util.Statistics;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
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
import java.util.*;


@Singleton
public class TileServiceImpl implements TileService {
	private static final String FULL_COUNT = "All image requests";
	static {
		Statistics.addStatisticListener(FULL_COUNT, new Statistics.StatisticListener() {
			@Override
			public void onStatisticUpdated(Statistics.Statistic statistic) {
				if (null != statistic && 0 == (statistic.count() % 10)) {
					System.out.println();
					System.out.println();
					System.out.println();
					System.out.println("Full statistices:");
					System.out.print(Statistics.fullReport());
					System.out.println();
					System.out.println();
					System.out.println();
				}
			}
		});
	}
	private static final Logger LOGGER = LoggerFactory.getLogger( TileServiceImpl.class );
	private static final Color COLOR_BLANK = new Color( 255, 255, 255, 0 );

	private LayerService _layerService;

	@Inject
	public TileServiceImpl( LayerService layerService ) {
		_layerService = layerService;
	}


	private <T> TileData<T> tileDataForIndex( TileIndex index, String dataId, TileSerializer<T> serializer, PyramidIO pyramidIO, int coarseness ) throws IOException {
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

				tileDatas = pyramidIO.readTiles( dataId, serializer, Collections.singleton( scaleLevelIndex ) );
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
			java.util.List<TileData<T>> tileDatas = pyramidIO.readTiles( dataId, serializer, Collections.singleton( index ) );
			if ( !tileDatas.isEmpty() ) {
				data = tileDatas.get( 0 );
			}
		}

		return data;
	}


	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getTile(int, double, double)
	 */
	@Override
	public BufferedImage getTileImage( String layer, TileIndex index, Iterable<TileIndex> tileSet, JSONObject query ) {
		synchronized (TILE_REQUEST_SYNC_OBJ) {
			Statistics.addStatistic(FULL_COUNT, 1L);
//			Statistics.addStatistic(layer + ": image requests", 1L);
			BufferedImage bi = null;

			try {
				Statistics.checkpointTimeNano(CKPT_START);
				// get layer configuration
				LayerConfiguration config = _layerService.getLayerConfiguration(layer, query);
				Statistics.checkpointTimeNano(CKPT_CONFIG);

				try {
					// set level extrema
					PyramidMetaData metadata = _layerService.getMetaData(layer);
					Statistics.checkpointTimeNano(CKPT_METADATA);
					String minimum = metadata.getCustomMetaData("" + index.getLevel(), "minimum");
					String maximum = metadata.getCustomMetaData("" + index.getLevel(), "maximum");
					config.setLevelProperties(index, minimum, maximum);
					// produce the tile renderer from the configuration
					TileDataImageRenderer<?> tileRenderer = config.produce(TileDataImageRenderer.class);
					Statistics.checkpointTimeNano(CKPT_PRERENDER);
					bi = renderTileImage(config, layer, index, tileSet, tileRenderer);
					Statistics.checkpointTimeNano(CKPT_POSTRENDER);

				} catch (ConfigurationException e) {
					LOGGER.warn("No renderer specified for tile request. " + e.getMessage());
				} catch (IllegalArgumentException e) {
					LOGGER.info("Renderer configuration not recognized.");
				} catch (Exception e) {
					LOGGER.warn("Tile is corrupt: " + layer + ":" + index);
					LOGGER.warn("Tile error: ", e);
				}

				// always return a blank tile if there is no data
				if (bi == null) {
					int outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
					int outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
					bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = bi.createGraphics();
					g.setColor(COLOR_BLANK);
					g.fillRect(0, 0, 256, 256);
					g.dispose();
					Statistics.checkpointTimeNano(CKPT_COPY);
				}
//				Statistics.addCheckpointDifference(layer + ": getTileImage[A]: configuration", CKPT_START, CKPT_CONFIG);
//				Statistics.addCheckpointDifference(layer + ": getTileImage[B]: metadata retreival", CKPT_CONFIG, CKPT_METADATA);
//				Statistics.addCheckpointDifference(layer + ": getTileImage[C]: preprocessing", CKPT_METADATA, CKPT_PRERENDER);
//				Statistics.addCheckpointDifference(layer + ": getTileImage[D]: rendering", CKPT_PRERENDER, CKPT_POSTRENDER);
				if (bi == null)
					Statistics.addCheckpointDifference(layer + ": getTileImage[E]: copying image", CKPT_POSTRENDER, CKPT_COPY);
			} finally {
				Statistics.clearCheckpoints(CKPT_START, CKPT_CONFIG, CKPT_METADATA, CKPT_PRERENDER, CKPT_POSTRENDER, CKPT_COPY);
			}

			return bi;
		}
	}
	private static Object TILE_REQUEST_SYNC_OBJ = new Object();
	private static final String CKPT_START      = "getTileImage: start";
	private static final String CKPT_CONFIG     = "getTileImage: config";
	private static final String CKPT_METADATA   = "getTileImage: metadata";
	private static final String CKPT_PRERENDER  = "getTileImage: preRender";
	private static final String CKPT_POSTRENDER = "getTileImage: postRender";
	private static final String CKPT_COPY       = "getTileImage: copyImage";

	private <T> BufferedImage renderTileImage( LayerConfiguration config, String layer,
											   TileIndex index, Iterable<TileIndex> tileSet,
											   TileDataImageRenderer<T> renderer ) throws ConfigurationException, IOException, Exception {
		try {
			Statistics.checkpointTimeNano(CKPT_RTI_START);
			// prepare for rendering
			config.prepareForRendering(layer, index, tileSet);

			// get data source id, and produce the pyramidio and serializer
			// these are all common points of failure in the config, so explicitly log these
			String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			if (dataId == null) {
				LOGGER.error("Could not determine data id for layer:" + layer + ", please confirm that it has been configured correctly.");
				return null;
			}
			Statistics.checkpointTimeNano(CKPT_RENDERPREP);
			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			if (dataId == null) {
				LOGGER.error("Could not produce pyramidio for layer:" + layer + ", please confirm that it has been configured correctly and data is avalable.");
				return null;
			}
			Statistics.checkpointTimeNano(CKPT_PYRAMID);
			@SuppressWarnings("unchecked")
			TileSerializer<T> serializer = config.produce(TileSerializer.class);
			if (serializer == null) {
				LOGGER.error("Could not produce tile serializer, please confirm that it has been configured correctly.");
			}
			Statistics.checkpointTimeNano(CKPT_SERIALIZER);

			int coarseness = config.getPropertyValue(LayerConfiguration.COARSENESS);

			@SuppressWarnings("unchecked")
			TileTransformer<T> tileTransformer = config.produce(TileTransformer.class);

			Statistics.checkpointTimeNano(CKPT_PREPDONE);
			TileData<T> data = tileDataForIndex(index, dataId, serializer, pyramidIO, coarseness);
			if (data == null) {
				return null;
			}

			Statistics.checkpointTimeNano(CKPT_DATA);
			data = tileTransformer.transform(data);
			Statistics.checkpointTimeNano(CKPT_TRANSFORM);
			BufferedImage result = null;
			if (data != null) {
				result = renderer.render(data, config);
				Statistics.checkpointTimeNano(CKPT_RENDER);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[A]: prepareForRendering", CKPT_RTI_START, CKPT_RENDERPREP);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[B]: create pyramid", CKPT_RENDERPREP, CKPT_PYRAMID);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[C]: create serializer", CKPT_PYRAMID, CKPT_SERIALIZER);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[D]: total prep time", CKPT_RTI_START, CKPT_PREPDONE);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[E]: retrieve tile", CKPT_PREPDONE, CKPT_DATA);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[F]: transform tile", CKPT_DATA, CKPT_TRANSFORM);
//				Statistics.addCheckpointDifference(layer + ": renderTileImage[G]: render tile", CKPT_TRANSFORM, CKPT_RENDER);
			}
			return result;
		} finally {
			Statistics.clearCheckpoints(CKPT_RTI_START, CKPT_RENDERPREP, CKPT_PYRAMID, CKPT_SERIALIZER, CKPT_PREPDONE,
			                            CKPT_DATA, CKPT_TRANSFORM, CKPT_RENDER);
		}
	}
	private static final String CKPT_RTI_START  = "renderTileImage: start";
	private static final String CKPT_RENDERPREP = "renderTileImage: render prep";
	private static final String CKPT_PYRAMID    = "renderTileImage: pyramid";
	private static final String CKPT_SERIALIZER = "renderTileImage: serializer";
	private static final String CKPT_PREPDONE   = "renderTileImage: prepdone";
	private static final String CKPT_DATA       = "renderTileImage: data retrieval";
	private static final String CKPT_TRANSFORM  = "renderTileImage: transform";
	private static final String CKPT_RENDER     = "renderTileImage: render";

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
