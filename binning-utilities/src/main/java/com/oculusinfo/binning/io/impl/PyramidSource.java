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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;

public interface PyramidSource {

	public void initializeForWrite (String basePath) throws IOException;

	public <T> void writeTiles (String basePath, TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException;
	
	public void writeMetaData (String basePath, String metaData) throws IOException;

	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription);

	public <T> List<TileData<T>> readTiles (String basePath,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles) throws IOException;
	
	public <T> InputStream getTileStream (String basePath,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException;

	public String readMetaData (String basePath) throws IOException;
	
	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException;
}
