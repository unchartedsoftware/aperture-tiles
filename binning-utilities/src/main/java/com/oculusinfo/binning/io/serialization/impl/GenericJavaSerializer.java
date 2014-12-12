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
package com.oculusinfo.binning.io.serialization.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;

/**
 * A very generic tile serializer that can serialize or deserialize any tile
 * using java serialization instead of avro or json.
 * 
 * Note that because this is a generic type, the type descriptor must be passed
 * in upon creation.
 * 
 * @author nkronenfeld
 * 
 * @param <T>
 *            The bin type of the tiles to be serialized/deserialized
 */
public class GenericJavaSerializer<T> implements TileSerializer<T> {
	private static final long serialVersionUID = 1L;

	private TypeDescriptor    _typeDescriptor;
	private boolean _compress = true;

	public GenericJavaSerializer (boolean compress, TypeDescriptor typeDescriptor) {
		_typeDescriptor = typeDescriptor;
		_compress = compress;
	}
	
	public GenericJavaSerializer (TypeDescriptor typeDescriptor) {
		_typeDescriptor = typeDescriptor;
	}

	@Override
	public TypeDescriptor getBinTypeDescription () {
		return _typeDescriptor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TileData<T> deserialize (TileIndex index, InputStream rawData)
			throws IOException {
		GZIPInputStream zis = null;
		ObjectInputStream ois = null;
		if (_compress) {
			zis = new GZIPInputStream(rawData);
			ois = new ObjectInputStream(zis);			
		} else {
			ois = new ObjectInputStream(rawData);
		}
		try {
			return (TileData<T>) ois.readObject();			
		} catch (ClassNotFoundException e) {
			throw new IOException("Error reading tile", e);
		} finally {
			ois.close();
		}
	}

	@Override
	public void serialize (TileData<T> data, OutputStream output)
			throws IOException {
		GZIPOutputStream zos = null;
		ObjectOutputStream oos = null;
		if (_compress) {			
			zos = new GZIPOutputStream(output);
			oos = new ObjectOutputStream(zos);
		} else {
			oos = new ObjectOutputStream(output);
		}
		try {
			oos.writeObject(data);			
		} finally {
			oos.close();
		}
	}
}
