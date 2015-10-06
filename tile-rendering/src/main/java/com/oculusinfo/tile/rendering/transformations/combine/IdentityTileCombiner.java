package com.oculusinfo.tile.rendering.transformations.combine;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONObject;

/**
 * Created by wmayo on 2015-10-02.
 */
public class IdentityTileCombiner<T> implements TileCombiner<T> {

	public TileData<T> combine(TileData<T> data, TileIndex index, TileSerializer<T> serializer, int coarseness, JSONObject tileProperties) throws Exception {
		return data; // Nothing to combine with
	}
}
