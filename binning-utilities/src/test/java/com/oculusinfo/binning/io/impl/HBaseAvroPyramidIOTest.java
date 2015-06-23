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
import java.util.ArrayList;
import java.util.List;


import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TestPyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;

//@Ignore
public class HBaseAvroPyramidIOTest {

	private <T> void writeAvroTiles (PyramidIO pio, TileSerializer<T> serializer,
	                                 String pyramidId, ArrayList<TileData<T>> tiles) {
		try {
			pio.initializeForWrite(pyramidId);
			pio.writeTiles(pyramidId, serializer, tiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private <T> List<TileData<T>> readAvroTiles (PyramidIO pio, TileSerializer<T> serializer, String pyramidId) {
		ArrayList<TileIndex> tiles = new ArrayList<TileIndex>();
		TileIndex index = new TileIndex(4, 3, 2);

		tiles.add(index);

		try {
			return pio.readTiles(pyramidId, serializer, tiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void writeReadAvroRoundTripTest () {
		PyramidIO io = new TestPyramidIO();
		TileSerializer<Integer> serializer = new PrimitiveAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

		ArrayList<TileData<Integer>> writeTiles = new ArrayList<TileData<Integer>>();

		TileIndex index = new TileIndex(4, 3, 2);
		TileData<Integer> tile = new DenseTileData<Integer>(index);
		for (int x=0; x<256; ++x) {
			for (int y=0; y<256; ++y) {
				tile.setBin(x, y, x+256*y);
			}
		}
		writeTiles.add(tile);
		writeAvroTiles(io, serializer, "test", writeTiles);

		List<TileData<Integer>> readTiles = readAvroTiles(io, serializer, "test");

		for (int i=0; i<writeTiles.size(); i++){
			TileData<Integer> writeTile = writeTiles.get(i);
			TileIndex writeTileDef = writeTile.getDefinition();

			TileData<Integer> readTile = readTiles.get(i);
			TileIndex readTileDef = readTile.getDefinition();

			Assert.assertEquals(writeTileDef, readTileDef);
			for (int x = 0; x < writeTileDef.getXBins(); ++x) {
				for (int y = 0; y < writeTileDef.getYBins(); ++y) {
					Assert.assertEquals(writeTile.getBin(x, y), readTile.getBin(x, y));
				}
			}
		}
	}
}
