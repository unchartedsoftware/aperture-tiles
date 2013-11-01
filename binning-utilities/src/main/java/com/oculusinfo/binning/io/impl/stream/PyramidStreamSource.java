package com.oculusinfo.binning.io.impl.stream;

import java.io.IOException;
import java.io.InputStream;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.TileSerializer;

public interface PyramidStreamSource {
    public InputStream getTileStream(String basePath, TileSerializer<?> serializer, TileIndex tile) throws IOException;
    public InputStream getMetaDataStream (String basePath) throws IOException;
}
