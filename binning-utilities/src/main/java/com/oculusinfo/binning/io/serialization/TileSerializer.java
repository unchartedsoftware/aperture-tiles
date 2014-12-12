/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.serialization;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.TypeDescriptor;



/**
 * A TileSerializer, as its name indicates, determines how to read and write a
 * tile from some source. Sources are expected to be able to provide or use
 * input and output streams.
 * 
 * Serializers are specific to the type of data being read and written. The
 * majority of our serializers read and write using Apache AVRO, but other
 * formats are possible.
 * 
 * @author nkronenfeld
 * 
 * @param <T> The type of data stored in the bins of the tiles the serializer
 *            knows how to handle.
 */
public interface TileSerializer<T> extends Serializable {

	/**
	 * Get a description of the type of data read from and written to bins in
	 * tiles by this serializer, in a format comparable, even accounting for
	 * generics, with other sources.
	 * 
	 * @return A fully exploded version of the bin type associated with this
	 *         serializer, accounting for all generics.
	 */
	public TypeDescriptor getBinTypeDescription ();

	/**
	 * Read a tile
	 * 
	 * @param index The tile to read
	 * @param rawData A stream containing the tile data
	 * @return The tile
	 * @throws IOException
	 */
	public TileData<T> deserialize (TileIndex index, InputStream rawData) throws IOException;

	/**
	 * Write a tile
	 * 
	 * @param data The tile to write
	 * @param output A raw output stream to which to write the tile
	 * @throws IOException
	 */
	public void serialize (TileData<T> data, OutputStream output) throws IOException;
}
