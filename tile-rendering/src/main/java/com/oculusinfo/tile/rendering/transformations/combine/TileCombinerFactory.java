package com.oculusinfo.tile.rendering.transformations.combine;

import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.JSONProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.tile.rendering.transformations.tile.*;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by wmayo on 2015-10-02.
 */
public class TileCombinerFactory extends ConfigurableFactory<TileCombiner<?>> {

	public static final List<String> DATA_PATH = Collections.unmodifiableList(Arrays.asList("data"));
	public static final List<String> PYRAMID_IO_PATH = Collections.unmodifiableList( Arrays.asList( "data","pyramidio" ) );
	public static final List<String> SERIALIZER_PATH = Collections.unmodifiableList( Arrays.asList( "data","serializer" ) );

	public static StringProperty TILE_COMBINER_TYPE = new StringProperty("type",
		"The type of Transformer desired.",
		"identity");

	public static final StringProperty DATA_ID = new StringProperty("id",
		"The ID of the data source of the layer; exact format depends on how the layer is stored.",
		null);

	// There is no way to produce a Class<TileTransformer<?>> directly; the best one can do is fake it through erasure.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Class<TileCombiner<?>> getFactoryClass () {
		return (Class) TileCombiner.class;
	}

	public TileCombinerFactory(FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
							   FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
							   ConfigurableFactory<?> parent,
							   List<String> path) {
		this(null, pyramidIOFactoryProvider, serializationFactoryProvider, parent, path);
	}

	public TileCombinerFactory(String name,
							   FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
							   FactoryProvider<TileSerializer<?>> serializationFactoryProvider,
							   ConfigurableFactory<?> parent,
							   List<String> path) {
		super(name, getFactoryClass(), parent, path);
		addProperty(TILE_COMBINER_TYPE);
		addProperty(DATA_ID, DATA_PATH);

		addChildFactory( pyramidIOFactoryProvider.createFactory(this, PYRAMID_IO_PATH) );
		addChildFactory( serializationFactoryProvider.createFactory(this, SERIALIZER_PATH) );
	}

	@Override
	protected TileCombiner<?> create () {

		String transformerTypes = getPropertyValue(TILE_COMBINER_TYPE);

		if ("normalize".equals(transformerTypes)) {
			try {
				PyramidIO pyramidIO = produce( PyramidIO.class );
				TileSerializer<?> serializer = produce( TileSerializer.class );
				String dataId = getPropertyValue(DATA_ID);
				return new NormalizeTileCombiner(pyramidIO, serializer, dataId);
			} catch (Exception e) {
				return null;
			}
		} else {  // 'identity' or none passed in will give the default transformer
			return new IdentityTileCombiner();
		}
	}
}
