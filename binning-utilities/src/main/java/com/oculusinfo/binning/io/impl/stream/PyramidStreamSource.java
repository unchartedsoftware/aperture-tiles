package com.oculusinfo.binning.io.impl.stream;

import java.io.IOException;
import java.io.InputStream;

import com.oculusinfo.binning.TileIndex;

public interface PyramidStreamSource {
    public InputStream getTileStream(String basePath, TileIndex tile) throws IOException;
    public InputStream getMetaDataStream (String basePath) throws IOException;
}
