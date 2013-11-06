/**
 * Copyright (c) 2013 Oculus Info Inc. 
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.common.primitives.Doubles;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.TileSerializer;

public class BackwardCompatibilitySerializer implements TileSerializer<Double>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public TileData<Double> deserialize(TileIndex index, InputStream rawData) throws IOException {

		ZipInputStream stream = new ZipInputStream(rawData);
		stream.getNextEntry();

		ObjectInputStream ois = new ObjectInputStream(stream);
		try {
			double[] data = (double[]) ois.readObject();
			List<Double> d = Doubles.asList(data);
			TileData<Double> tileData = new TileData<Double>(index, d);
			return tileData;			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void serialize(TileData<Double> data, TilePyramid tilePyramid,
			OutputStream output) throws IOException {

		ZipOutputStream zip = new ZipOutputStream(output);
		zip.putNextEntry(new ZipEntry("tile.data"));

		ObjectOutputStream oos = new ObjectOutputStream(zip);
		double[] array = Doubles.toArray(data.getData());
		oos.writeObject(array);
		oos.flush();

		zip.closeEntry();
		zip.close();
	}

}
