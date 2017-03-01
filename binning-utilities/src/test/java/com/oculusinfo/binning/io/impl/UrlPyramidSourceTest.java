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
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;

import org.apache.avro.file.CodecFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class UrlPyramidSourceTest {

	private static String SOURCE_DIR = "./src/test/file_pyramid/";
	private static String SOURCE_EXT = "avro";
	private static String SOURCE_LAYER = "test";
	private static int LEVEL = 4;
	private static int X_INDEX = 3;
	private static int Y_INDEX = 2;


	@Test
	public void writeReadAvroRoundTripTest () {
		FileBasedPyramidIO io = new FileBasedPyramidIO(new FileSystemPyramidSource(SOURCE_DIR, SOURCE_EXT));
		TileSerializer<Integer> serializer = new PrimitiveAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

		ArrayList<TileData<Integer>> writeTiles = new ArrayList<>();

		TileIndex index = new TileIndex( LEVEL, X_INDEX, Y_INDEX );
		TileData<Integer> tile = new DenseTileData<>(index);
		for (int x=0; x<256; ++x) {
			for (int y=0; y<256; ++y) {
				tile.setBin(x, y, x+256*y);
			}
		}
		writeTiles.add(tile);
		writeAvroTiles(io, serializer, SOURCE_LAYER, writeTiles);

		List<TileData<Integer>> readTiles = readAvroTiles(io, serializer, SOURCE_LAYER);

		for (int i=0; i<writeTiles.size(); i++){
			TileData<Integer> writeTile = writeTiles.get(i);
			TileIndex writeTileDef = writeTile.getDefinition();

			TileData<Integer> readTile = readTiles.get(i);
			TileIndex readTileDef = readTile.getDefinition();

			Assert.assertEquals(writeTileDef, readTileDef);

			for (int x = 0; x < writeTile.getDefinition().getXBins(); ++x) {
				for (int y = 0; y < writeTile.getDefinition().getYBins(); ++y) {
					Assert.assertEquals(writeTile.getBin(x, y), readTile.getBin(x, y));
				}
			}
		}
	}

	@After
	public void removeWrittenFile() {
		try {
            FileUtils.deleteDirectory(  new File( SOURCE_DIR ) );
        } catch ( Exception e ) {
            // swallow exception
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
