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
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;

import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class FileSystemPyramidSourceTest {
	
	private static String SOURCE_DIR = "./src/test/file_pyramid/";
	private static String SOURCE_EXT = "avro";



	@Test 
	public void writeReadAvroRoundTripTest () {
		FileBasedPyramidIO io = new FileBasedPyramidIO(new FileSystemPyramidSource(SOURCE_DIR, SOURCE_EXT));
		TileSerializer<Integer> serializer = new PrimitiveAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

		ArrayList<TileData<Integer>> writeTiles = new ArrayList<TileData<Integer>>();

		TileIndex index = new TileIndex(4, 3, 2);
		TileData<Integer> tile = new TileData<Integer>(index);
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
			List<Integer> writeTileData = writeTile.getData();

			TileData<Integer> readTile = readTiles.get(i);
			TileIndex readTileDef = readTile.getDefinition();
			List<Integer> readTileData = readTile.getData();

			Assert.assertEquals(readTileData.size(), writeTileData.size());

			for (int j=0; j<readTileData.size(); j++){
				Assert.assertEquals(readTileData.get(j), writeTileData.get(j));
			}

			Assert.assertEquals(writeTileDef, readTileDef);
		}
	}

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

}
