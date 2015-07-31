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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;

/** A dummy pyramid IO that doesn't actually read anything, just returns empty tiles */
public class DummyPyramidIO implements PyramidIO {
    private String _metaData;
    public DummyPyramidIO (double minX, double maxX, double minY, double maxY, int minZ, int maxZ) {
        String metaData =
                "{"+
                "  \"name\":\"dummy\",\n"+
                "  \"description\":\"dummy layer\",\n"+
                "  \"tilesize\":1,\n"+
                "  \"scheme\":\"TMS\",\n"+
                "  \"projection\":\"EPSG:4326\",\n"+
                "  \"minzoom\": "+minZ+",\n"+
                "  \"maxzoom\": "+maxZ+",\n"+
                "  \"bounds\": ["+minX+", "+minY+", "+maxX+", "+maxY+"],\n"+
                "  \"meta\": {\n"+
                "    \"levelMinimums\": {\n";
            for (int i=minZ; i<=maxZ; ++i) {
                metaData += "      \""+i+"\": 0";
                if (i == maxZ) metaData += "\n";
                else metaData += ",\n";
            }
            metaData += "    },\n";
            metaData += "    \"levelMaximums\": {\n";
            for (int i=minZ; i<=maxZ; ++i) {
                metaData += "      \""+i+"\": 0";
                if (i == maxZ) metaData += "\n";
                else metaData += ",\n";
            }
            metaData += "    }\n";
            metaData += "  }\n";
            metaData += "}\n";

            _metaData = metaData;
    }

    @Override
    public void initializeForWrite (String pyramidId) throws IOException {
    }

    @Override
    public <T> void writeTiles (String pyramidId,
                                TileSerializer<T> serializer,
                                Iterable<TileData<T>> data) throws IOException {
    }

    @Override
    public void writeMetaData (String pyramidId, String metaData) throws IOException {
    }

    @Override
    public void initializeForRead (String pyramidId, int width, int height,
                                   Properties dataDescription) {
    }

    @Override
    public <T> List<TileData<T>> readTiles (String pyramidId,
                                            TileSerializer<T> serializer,
                                            Iterable<TileIndex> tiles) throws IOException {
        List<TileData<T>> results = new ArrayList<>();
        for (TileIndex rawIndex: tiles) {
            TileIndex index = new TileIndex(rawIndex.getLevel(), rawIndex.getX(), rawIndex.getY(), 1, 1);
            TileData<T> tile = new DenseTileData<>(index);
            results.add(tile);
        }

        return results;
    }

	@Override
	public <T> List<TileData<T>> readTiles (String pyramidId,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles,
											JSONObject properties ) throws IOException {
		return readTiles( pyramidId, serializer, tiles );
	}

    @Override
    public <T> InputStream getTileStream (String pyramidId,
                                          TileSerializer<T> serializer,
                                          TileIndex rawIndex) throws IOException {
        TileIndex index = new TileIndex(rawIndex.getLevel(), rawIndex.getX(), rawIndex.getY(), 1, 1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(new DenseTileData<T>(index), baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Override
    public String readMetaData (String pyramidId) throws IOException {
        return _metaData;
    }

    @Override
    public void removeTiles (String id, Iterable<TileIndex> tiles) throws IOException {
    }
}
