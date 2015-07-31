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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;


/**
 * Implements a PyramidIO for reading tiles that are based on the filesystem (zip, resource, or directory)
 * 	This class will be injected with a PyramidSource object that will provide the necesarry tile access depending
 *  on the particular type of file system tile used.
 *
 */
public class FileBasedPyramidIO implements PyramidIO {

	private PyramidSource _source;

	public FileBasedPyramidIO(PyramidSource source){
		_source = source;
	}

	public PyramidSource getSource () {
		return _source;
	}

	@Override
	public void initializeForWrite (String basePath) throws IOException {
		_source.initializeForWrite(basePath);
	}

	@Override
	public <T> void writeTiles (String basePath, TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {

		_source.writeTiles(basePath, serializer, data);
	}

	@Override
	public void writeMetaData (String basePath, String metaData) throws IOException {
		_source.writeMetaData(basePath, metaData);
	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		// Noop
	}

	@Override
	public <T> List<TileData<T>> readTiles (String basePath,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles) throws IOException {
		return _source.readTiles(basePath, serializer, tiles);
	}

	@Override
	public <T> List<TileData<T>> readTiles (String pyramidId,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles,
											JSONObject properties ) throws IOException {
		return readTiles( pyramidId, serializer, tiles );
	}

	@Override
	public <T> InputStream getTileStream (String basePath,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
		return _source.getTileStream(basePath, serializer, tile);
	}

	@Override
	public String readMetaData (String basePath) throws IOException {
		return _source.readMetaData(basePath);
	}

	@Override
	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException {
		_source.removeTiles(id, tiles);
	}
}
