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
package com.oculusinfo.binning.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;


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
	public <T> void writeTiles (String pyramidId,
	                            TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {
		for (TileData<T> tile: data) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			serializer.serialize(tile, stream);
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
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		// Noop
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
	public <T> List<TileData<T>> readTiles (String pyramidId,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles,
											JSONObject properties ) throws IOException {
		return readTiles( pyramidId, serializer, tiles );
	}

	@Override
	public <T> InputStream getTileStream (String pyramidId,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
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

	@Override
	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException {
		throw new IOException("removeTiles not currently supported for TestPyramidIO");
	}


}
