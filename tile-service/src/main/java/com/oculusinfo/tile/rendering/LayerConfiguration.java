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
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.ListProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.factory.properties.TileIndexProperty;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.rendering.transformations.ValueTransformerFactory;



/**
 * A tile-service specific, local-only factory that acts as a container to
 * random parameters used for rendering. This is wierd as a factory - it really
 * is a catch-all for various other things, and doesn't produce a good, per se,
 * but rather just properties. As such, the good type it produces is really just
 * itself (though it does have sub-factories).
 * 
 * @author nkronenfeld
 */
public class LayerConfiguration extends ConfigurableFactory<LayerConfiguration> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LayerConfiguration.class);

    public static final List<String>          RENDERER_PATH   = Collections.singletonList("renderer");
    public static final List<String>          PYRAMID_IO_PATH = Collections.singletonList("pyramidio");
    public static final List<String>          SERIALIZER_PATH = Collections.singletonList("serializer");

    public static final StringProperty        LAYER_NAME      = new StringProperty("layer",
		         "The ID of the layer; exact format depends on how the layer is stored.",
		         null);
	public static final StringProperty        SHORT_NAME      = new StringProperty("name",
		         "A shortened, human-readable version of the layer name.  Defaults to the same value as LAYER_NAME",
		         null);
	public static final StringProperty        TRANSFORM       = new StringProperty("transform",
		         "The transformation to apply to the data before display",
		         "linear",
		         new String[] {"linear", "log10"});
	public static final ListProperty<Integer> LEGEND_RANGE    = new ListProperty<>(new IntegerProperty("", "", -1),
		         "legendrange",
		         "The value bounds to use for coloration");
	public static final IntegerProperty       CURRENT_IMAGE   = new IntegerProperty("currentImage",
		          "used to determine which of a series of potential images should be displayed.  Ordinarily, this parameter is for programatic use only.",
		          -1);
	public static final IntegerProperty       COARSENESS      = new IntegerProperty("coarseness",
		          "Used by the standard heatmap renderer to allow the client to specify getting coarser tiles than needed, for efficiency (if needed)",
		          1);

	public static final IntegerProperty       LINE_NUMBER     = new IntegerProperty("lineNumber",
		          "For use by the server only",
		          0);
	public static final IntegerProperty       OUTPUT_WIDTH    = new IntegerProperty("outputWidth",
		          "For use by the server only",
		          256);
	public static final IntegerProperty       OUTPUT_HEIGHT   = new IntegerProperty("outputHeight",
		          "For use by the server only",
		          256);
	public static final IntegerProperty       RANGE_MIN       = new IntegerProperty("rangeMin",
		          "For server use only - derived property",
		          0);
	public static final IntegerProperty       RANGE_MAX       = new IntegerProperty("rangeMax",
		          "For server use only - derived property",
		          100);

	// Per-tile properties
	public static final TileIndexProperty     TILE_COORDINATE = new TileIndexProperty("tileCoordinate",
		            "For server use only, on a tile-by-tile basis",
		            null);
	public static final StringProperty        LEVEL_MINIMUMS  = new StringProperty("levelMinimums",
		         "For server use only, on a tile-by-tile basis",
		         null);
	public static final StringProperty        LEVEL_MAXIMUMS  = new StringProperty("levelMaximums",
		         "For server use only, on a tile-by-tile basis",
		         null);



	private ValueTransformerFactory _transformFactory;

	public LayerConfiguration (FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
	                           FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
	                           FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
	                           ConfigurableFactory<?> parent,
	                           List<String> path) {
		this(pyramidIOFactoryProvider, serializationFactoryProvider,
		     rendererFactoryProvider, null, parent, path);
	}


	public LayerConfiguration (FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
	                           FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
	                           FactoryProvider<TileDataImageRenderer> rendererFactoryProvider,
	                           String name, ConfigurableFactory<?> parent,
	                           List<String> path) {
		super(name, LayerConfiguration.class, parent, path);

		addProperty(LAYER_NAME);
		addProperty(SHORT_NAME);
		addProperty(TRANSFORM);
		addProperty(LEGEND_RANGE);
		addProperty(CURRENT_IMAGE);
		addProperty(OUTPUT_WIDTH);
		addProperty(OUTPUT_HEIGHT);
		addProperty(LINE_NUMBER);
		addProperty(COARSENESS);
		addProperty(RANGE_MIN);
		addProperty(RANGE_MAX);

		addProperty(TILE_COORDINATE);
		addProperty(LEVEL_MINIMUMS);
		addProperty(LEVEL_MAXIMUMS);

		_transformFactory = new ValueTransformerFactory(this, Collections.singletonList("transform"));
		addChildFactory(_transformFactory);
		addChildFactory(rendererFactoryProvider.createFactory(this, RENDERER_PATH));
		addChildFactory(pyramidIOFactoryProvider.createFactory(this, PYRAMID_IO_PATH));
		addChildFactory(serializationFactoryProvider.createFactory(this, SERIALIZER_PATH));
	}

	@Override
	protected LayerConfiguration create () {
		return this;
	}

	@Override
	public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
		super.readConfiguration(rootNode);

		calculateDerivedProperties();
	}

	@Override
	public void readConfiguration (Properties properties) throws ConfigurationException {
		super.readConfiguration(properties);

		calculateDerivedProperties();
	}

	private void calculateDerivedProperties () {
		List<Integer> legendRange = getPropertyValue(LayerConfiguration.LEGEND_RANGE);
		if (null != legendRange && !legendRange.isEmpty()) {
			setPropertyValue(RANGE_MIN,  legendRange.get(0));
			setPropertyValue(RANGE_MAX,  legendRange.get(1));
		}

		if (!hasPropertyValue(SHORT_NAME)) {
			setPropertyValue(SHORT_NAME, getPropertyValue(LAYER_NAME));
		}
	}

	public void setLevelProperties (TileIndex tileIndex,
	                                String levelMinimum,
	                                String levelMaximum) {
		setPropertyValue(TILE_COORDINATE, tileIndex);
		setPropertyValue(LEVEL_MAXIMUMS, levelMaximum);
		setPropertyValue(LEVEL_MINIMUMS, levelMinimum);

		try {
			TileDataImageRenderer renderer = produce(TileDataImageRenderer.class);
			Pair<Double, Double> extrema = renderer.getLevelExtrema(this);
			_transformFactory.setExtrema(extrema.getFirst(), extrema.getSecond());
		} catch (ConfigurationException e) {
			LOGGER.warn("Error determining layer-specific extrema for "+getPropertyValue(SHORT_NAME));
		}
	}

    /*
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
