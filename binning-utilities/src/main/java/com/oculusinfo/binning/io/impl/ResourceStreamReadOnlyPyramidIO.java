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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.impl.PyramidStreamSource;

public class ResourceStreamReadOnlyPyramidIO implements PyramidIO {
	private final Logger _logger = LoggerFactory.getLogger(getClass());
	
	private PyramidStreamSource _stream;
	
	public ResourceStreamReadOnlyPyramidIO(PyramidStreamSource stream){
		_stream = stream;
	}

	public PyramidStreamSource getStream () {
		return _stream;
	}

	@Override
	public void initializeForWrite (String basePath) throws IOException {
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}

	@Override
	public <T> void writeTiles (String basePath, TilePyramid tilePyramid, TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {
    	
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}

	@Override
	public void writeMetaData (String basePath, String metaData) throws IOException {
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		// Noop
	}

	@Override
	public <T> List<TileData<T>> readTiles (String basePath,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles) throws IOException {
		List<TileData<T>> results = new LinkedList<TileData<T>>();
		for (TileIndex tile: tiles) {
			InputStream stream = _stream.getTileStream(basePath, tile);
			//stream will be null if the tile cannot be found
			if(stream==null){
				_logger.info("no tile data found for " + tile.toString() );
				continue;
			}
            
			TileData<T> data = serializer.deserialize(tile, stream);
			results.add(data);
			stream.close();
		}
		return results;
	}

	@Override
	public <T> InputStream getTileStream (String basePath,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
		return _stream.getTileStream(basePath, tile);
	}

	@Override
	public String readMetaData (String basePath) throws IOException {
		InputStream stream = _stream.getMetaDataStream(basePath);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String rawMetaData = "";
		String line;
		while (null != (line = reader.readLine())) {
			rawMetaData = rawMetaData + line;
		}
		reader.close();
		return rawMetaData;
	}
	
	@Override
	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException {
		throw new UnsupportedOperationException("This is a read-only PyramidIO implementation.");
	}
}
