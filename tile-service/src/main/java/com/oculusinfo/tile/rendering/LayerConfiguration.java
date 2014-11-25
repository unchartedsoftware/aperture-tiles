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


import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.tile.rendering.transformations.tile.TileTransformer;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformerFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.factory.properties.TileIndexProperty;
import com.oculusinfo.tile.init.FactoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;



/**
 * The root node of the ConfigurableFactory tree that represents a layer's configured
 * state. Each layer can be represented as a JSON object.
 *
 * Nodes that contain values are represented by ConfigurableProperty objects.
 * Nodes that contain class enumerations and are represented by ConfigurableFactory objects.
 * 
 * @author nkronenfeld, kbirk
 */
public class LayerConfiguration extends ConfigurableFactory<LayerConfiguration> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LayerConfiguration.class);

    /**
     * public configuration paths, properties under these paths are accessible to the client.
     */
	public static final List<String> TILE_TRANSFORM_PATH = Collections.unmodifiableList( Arrays.asList( "public","tileTransform" ) );
    public static final List<String> VALUE_TRANSFORM_PATH = Collections.unmodifiableList( Arrays.asList( "public","valueTransform" ) );
    public static final List<String> FILTER_PATH = Collections.unmodifiableList( Arrays.asList( "public", "filter" ) );
    public static final List<String> TILE_PYRAMID_PATH = Collections.unmodifiableList( Arrays.asList( "public", "pyramid" ) );
	public static final List<String> RENDERER_PATH = Collections.unmodifiableList( Arrays.asList( "public", "renderer" ) );

    /**
     * private configuration paths, properties under public nodes are not accessible to the client
     */
    public static final List<String> DATA_PATH = Collections.unmodifiableList( Arrays.asList( "private", "data" ) );
    public static final List<String> PYRAMID_IO_PATH = Collections.unmodifiableList( Arrays.asList( "private", "data","pyramidio" ) );
	public static final List<String> SERIALIZER_PATH = Collections.unmodifiableList( Arrays.asList( "private", "data","serializer" ) );
    public static final List<String> REST_ENDPOINT_PATH = Collections.unmodifiableList( Arrays.asList( "private" ) );

    public static final String DEFAULT_VERSION = "v1.0";

    public static final StringProperty LAYER_ID = new StringProperty("id",
        "The ID of the layer",
        null);
    public static final StringProperty DATA_ID = new StringProperty("id",
        "The ID of the data source of the layer; exact format depends on how the layer is stored.",
        null);
    public static final StringProperty REST_ENDPOINT = new StringProperty("restEndpoint",
	    "The REST endpoint used for the layer, defaults to 'tile'",
	    "tile");
	public static final IntegerProperty COARSENESS = new IntegerProperty("coarseness",
	    "Used by the standard heatmap renderer to allow the client to specify getting coarser tiles than needed, for efficiency (if needed)",
	    1);
	public static final IntegerProperty OUTPUT_WIDTH = new IntegerProperty("outputWidth",
	    "The output image width, defaults to the standard 256",
	    256);
	public static final IntegerProperty OUTPUT_HEIGHT = new IntegerProperty("outputHeight",
	    "The output image height, defaults to the standard 256",
	    256);
	public static final IntegerProperty RANGE_MIN = new IntegerProperty("rangeMin",
	    "The maximum value set to the lower bound of the color ramp spectrum",
	    0);
	public static final IntegerProperty RANGE_MAX = new IntegerProperty("rangeMax",
	    "The maximum value set to the upper bound of the color ramp spectrum",
	    100);
	public static final TileIndexProperty TILE_COORDINATE = new TileIndexProperty("tileCoordinate",
        "For server use only, on a tile-by-tile basis",
        null);
	public static final StringProperty LEVEL_MINIMUMS = new StringProperty("levelMinimums",
        "For server use only, on a tile-by-tile basis",
        null);
	public static final StringProperty LEVEL_MAXIMUMS = new StringProperty("levelMaximums",
        "For server use only, on a tile-by-tile basis",
        null);

	private static Set<ConfigurationProperty<?>> LOCAL_PROPERTIES =
		Collections.unmodifiableSet(new HashSet<ConfigurationProperty<?>>(Arrays.asList(
            TILE_COORDINATE,
            LEVEL_MAXIMUMS,
            LEVEL_MINIMUMS
        )));

	private ValueTransformerFactory _transformFactory;
	private TileIndex _tileCoordinate;
	private String _levelMinimum;
	private String _levelMaximum;

	public LayerConfiguration( FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                               FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
	                           FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
	                           FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
	                           FactoryProvider<TileTransformer> tileTransformerFactoryProvider,
	                           ConfigurableFactory<?> parent,
	                           List<String> path) {
		this( pyramidIOFactoryProvider, tilePyramidFactoryProvider, serializationFactoryProvider,
		      rendererFactoryProvider, tileTransformerFactoryProvider,
              null, parent, path);
	}


	public LayerConfiguration( FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                               FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
	                           FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
	                           FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
	                           FactoryProvider<TileTransformer> tileTransformerFactoryProvider,
	                           String name,
                               ConfigurableFactory<?> parent,
	                           List<String> path) {
		super( name, LayerConfiguration.class, parent, path );

		addProperty(LAYER_ID);
        addProperty(REST_ENDPOINT, REST_ENDPOINT_PATH);
        addProperty(OUTPUT_WIDTH);
		addProperty(OUTPUT_HEIGHT);
        addProperty(DATA_ID, DATA_PATH);
		addProperty(COARSENESS, RENDERER_PATH);
		addProperty(RANGE_MIN, RENDERER_PATH);
		addProperty(RANGE_MAX, RENDERER_PATH);
		addProperty(TILE_COORDINATE);
		addProperty(LEVEL_MINIMUMS);
		addProperty(LEVEL_MAXIMUMS);

		_transformFactory = new ValueTransformerFactory( this, VALUE_TRANSFORM_PATH );
		addChildFactory( _transformFactory );

        addChildFactory( rendererFactoryProvider.createFactory(this, RENDERER_PATH) );
        addChildFactory( pyramidIOFactoryProvider.createFactory(this, PYRAMID_IO_PATH) );
        addChildFactory( serializationFactoryProvider.createFactory(this, SERIALIZER_PATH) );
        addChildFactory( tileTransformerFactoryProvider.createFactory(this, TILE_TRANSFORM_PATH) );
		addChildFactory( tilePyramidFactoryProvider.createFactory(this, TILE_PYRAMID_PATH) );
	}

	@Override
	protected LayerConfiguration create () {
		return this;
	}

	@Override
	public <PT> PT getPropertyValue (ConfigurationProperty<PT> property) {
		if (LOCAL_PROPERTIES.contains(property)) {
            if (TILE_COORDINATE.equals(property)) {
				return property.getType().cast(_tileCoordinate);
			} else if (LEVEL_MAXIMUMS.equals(property)) {
				return property.getType().cast(_levelMaximum);
			} else if (LEVEL_MINIMUMS.equals(property)) {
				return property.getType().cast(_levelMinimum);
			}
		}
		return super.getPropertyValue(property);
	}

    /**
     * Set the tile index, and level minimum and maximum for the impending read
     * @param tileIndex The index of the tile to be rendererd.
     * @param levelMinimum The level minimum.
     * @param levelMaximum The level maximum.
     */
	public void setLevelProperties (TileIndex tileIndex,
	                                String levelMinimum,
	                                String levelMaximum) {
		_tileCoordinate = tileIndex;
		_levelMaximum = levelMaximum;
		_levelMinimum = levelMinimum;
		try {
			TileDataImageRenderer renderer = produce(TileDataImageRenderer.class);
			if (null != renderer) {
				Pair<Double, Double> extrema = renderer.getLevelExtrema(this);
				_transformFactory.setExtrema(extrema.getFirst(), extrema.getSecond());
			}
		} catch (ConfigurationException e) {
			LOGGER.warn("Error determining layer-specific extrema for "+getPropertyValue(LAYER_ID));
		}
	}


	/**
	 * This is a placeholder for the caching configuration to override; it does
	 * nothing in this version.
	 * 
	 * Theoretically, it allows for a hook point for extending classes to make
	 * last-minute preparations before actually rendering a tile, whether to
	 * JSON or an image.
	 * 
	 * @param layer The layer to be rendered.
	 * @param tile The tile to be rendered
	 * @param tileSet Any other tiles that will need to be rendered along with
	 *            this one.
	 */
	public void prepareForRendering (String layer,
	                                 TileIndex tile,
	                                 Iterable<TileIndex> tileSet) {
		// NOOP
	}
}
