package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.binning.io.PyramidIO;

public class ImageRendererFactory {
	public static TileDataImageRenderer getRenderer (JSONObject parameterObject, PyramidIO pyramidIo) {
		String rendererType = "default";

		try {
			rendererType = parameterObject.getString("renderer");
		} catch (JSONException e) {
		}

		rendererType = rendererType.toLowerCase();
		if ("textscore".equals(rendererType)) {
			return new TextScoreImageRenderer(pyramidIo);
		} else if ("legacy".equals(rendererType)) {
			return new LegacyDoublesImageRenderer(pyramidIo);
		} else if ("doubleseries".equals(rendererType)) {
			return new DoublesSeriesImageRenderer(pyramidIo);
		} else {
			return new DoublesImageRenderer(pyramidIo);
		}
	}
}
