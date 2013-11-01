package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.BackwardCompatibilitySerializer;

public class LegacyDoublesImageRenderer extends DoublesImageRenderer {

	public LegacyDoublesImageRenderer(PyramidIO pyramidIo) {
		super(pyramidIo);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected TileSerializer<Double> createSerializer() {
		return new BackwardCompatibilitySerializer();
	}
}
