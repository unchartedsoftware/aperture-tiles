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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract class that is used by certain pyramidIO to access the tile data encapsulated within.
 * 	Contains a basic implementation for a read-only Pyramid tile with child classes providing the
 * 	stream data to the tile source and/or provide its own implementation for read/write of tile data.
 *
 */
public abstract class PyramidSourceStream implements PyramidSource {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	protected abstract InputStream getSourceTileStream(String basePath, TileIndex tile) throws IOException;
	protected abstract InputStream getSourceMetaDataStream (String basePath) throws IOException;

	public void initializeForWrite (String basePath) throws IOException {
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}

	public <T> void writeTiles (String basePath, TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {

		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}

	public void writeMetaData (String basePath, String metaData) throws IOException {
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}

	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		// Noop
	}

	public <T> List<TileData<T>> readTiles (String basePath,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles) throws IOException {
		List<TileData<T>> results = new LinkedList<TileData<T>>();
		for (TileIndex tile: tiles) {
			InputStream stream = getSourceTileStream(basePath, tile);
			//stream will be null if the tile cannot be found
			if(stream==null){
				LOGGER.info("no tile data found for " + tile.toString() );
				continue;
			}

			TileData<T> data = serializer.deserialize(tile, stream);
			results.add(data);
			stream.close();
		}
		return results;
	}

	public <T> InputStream getTileStream (String basePath,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
		return getSourceTileStream(basePath, tile);
	}

	public String readMetaData (String basePath) throws IOException {
		InputStream stream = getSourceMetaDataStream(basePath);
		if (stream == null)  {
			return null;
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String rawMetaData = "";
		String line;
		while (null != (line = reader.readLine())) {
			rawMetaData = rawMetaData + line;
		}
		reader.close();
		return rawMetaData;
	}

	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException {
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}
}
