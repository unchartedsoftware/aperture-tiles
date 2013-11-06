package com.oculusinfo.binning.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;


/**
 * An in-memory storage of pyramids, for use in testing.
 * 
 * @author nkronenfeld
 */
public class TestPyramidIO implements PyramidIO {
    private Map<String, byte[]> _data;

    public TestPyramidIO () {
        _data = new HashMap<String, byte[]>();
    }

    private String getMetaDataKey (String basePath) {
        return basePath+".metaData";
    }
    private String getTileKey (String basePath, TileIndex index) {
        return basePath+"."+index.toString();
    }


    @Override
    public void initializeForWrite (String pyramidId) throws IOException {
    }

    @Override
    public <T> void writeTiles (String pyramidId, TilePyramid tilePyramid,
                                TileSerializer<T> serializer,
                                Iterable<TileData<T>> data) throws IOException {
        for (TileData<T> tile: data) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            serializer.serialize(tile, tilePyramid, stream);
            stream.flush();
            stream.close();

            String key = getTileKey(pyramidId, tile.getDefinition());
            _data.put(key, stream.toByteArray());
        }
    }

    @Override
    public void writeMetaData (String pyramidId, String metaData) throws IOException {
        String key = getMetaDataKey(pyramidId);
        _data.put(key, metaData.getBytes());
    }

    @Override
    public <T> List<TileData<T>> readTiles (String pyramidId,
                                            TileSerializer<T> serializer,
                                            Iterable<TileIndex> tiles) throws IOException {
        List<TileData<T>> results = new ArrayList<TileData<T>>();
        for (TileIndex index: tiles) {
            String key = getTileKey(pyramidId, index);
            if (_data.containsKey(key)) {
                byte[] data = _data.get(key);
                ByteArrayInputStream stream = new ByteArrayInputStream(data);
                results.add(serializer.deserialize(index, stream));
            } else {
                results.add(null);
            }
        }
        return results;
    }

    @Override
    public InputStream getTileStream (String pyramidId, TileIndex tile) throws IOException {
        String key = getTileKey(pyramidId, tile);
        if (_data.containsKey(key)) {
            byte[] data = _data.get(key);
            return new ByteArrayInputStream(data);
        } else {
            return null;
        }
    }

    @Override
    public String readMetaData (String pyramidId) throws IOException {
        String key = getMetaDataKey(pyramidId);
        if (_data.containsKey(key)) {
            byte[] data = _data.get(key);
            return new String(data);
        } else {
            return null;
        }
    }

}
