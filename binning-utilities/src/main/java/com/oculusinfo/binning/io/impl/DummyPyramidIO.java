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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;

/** A dummy pyramid IO that doesn't actually read anything, just returns empty tiles */
public class DummyPyramidIO implements PyramidIO {
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
        for (TileIndex index: tiles) {
            TileData<T> tile = new TileData<>(index);
            results.add(tile);
        }

        return results;
    }

    @Override
    public <T> InputStream getTileStream (String pyramidId,
                                          TileSerializer<T> serializer,
                                          TileIndex tile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(new TileData<T>(tile), baos);

        return null;
    }

    @Override
    public String readMetaData (String pyramidId) throws IOException {
        return "";
    }

    @Override
    public void removeTiles (String id, Iterable<TileIndex> tiles) throws IOException {
    }
}
