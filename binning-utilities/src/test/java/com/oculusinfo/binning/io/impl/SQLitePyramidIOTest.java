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

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayJsonSerializer;
import com.oculusinfo.factory.util.Pair;

/**
 * test SQLite implementation
 * This will only work on a machine with SQLite already installed; since we 
 * don't want to mandate SQLite installation just to build properly, the 
 * default state of these tests is to ignore them.
 */
@Ignore
public class SQLitePyramidIOTest {

	private static final String PYRAMID_ID = "testPyramid";
	JDBCPyramidIO sqlitePyramidIO = null;


	@Before
	public void setUp() throws Exception {
		try {
			sqlitePyramidIO = new SQLitePyramidIO("test.db");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@After
	public void tearDown() throws Exception {
		try {
			sqlitePyramidIO.shutdown();
			File dbFile = new File("test.db");
			if (!dbFile.delete()) fail("Failed to delete test database.");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testInitializeForWrite() {
		try {
			sqlitePyramidIO.initializeForWrite(PYRAMID_ID);

			if (!sqlitePyramidIO.tableExists(PYRAMID_ID)) fail("Tile pyramid table wasn't created.");
			if (!sqlitePyramidIO.tableExists("metadata")) fail("Metadata table wasn't created.");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSingleTile() {
		try {
			sqlitePyramidIO.initializeForWrite(PYRAMID_ID);

			// XXX: This test is coupled to some implementations of other
			// classes--ideally, these objects should be mocked.

			TileIndex tileDef = new TileIndex(0, 0, 0, 1, 1);
			StringIntPairArrayJsonSerializer serializer = new StringIntPairArrayJsonSerializer();

			TileData<List<Pair<String, Integer>>> tileToWrite = new DenseTileData<List<Pair<String, Integer>>>(tileDef);
			List<Pair<String, Integer>> binVals = new ArrayList<Pair<String,Integer>>();
			Pair<String, Integer> binVal = new Pair<String, Integer>("name", 1);
			binVals.add(binVal);
			tileToWrite.setBin(0, 0, binVals);

			sqlitePyramidIO.writeTiles(PYRAMID_ID, serializer, Collections.singletonList(tileToWrite));

			List<TileData<List<Pair<String, Integer>>>> readResult =
				sqlitePyramidIO.readTiles(PYRAMID_ID, serializer,
				                          Collections.singletonList(tileDef));
			Assert.assertTrue(readResult.size()==1);

			TileData<List<Pair<String, Integer>>> readTileData = readResult.get(0);
			Assert.assertEquals(tileToWrite.getDefinition(), readTileData.getDefinition());
			for (int x = 0; x < tileToWrite.getDefinition().getXBins(); ++x) {
				for (int y = 0; y < tileToWrite.getDefinition().getYBins(); ++y) {
					List<Pair<String, Integer>> writeData = tileToWrite.getBin(x, y);
					List<Pair<String, Integer>> readData = readTileData.getBin(x, y);
					Assert.assertEquals(writeData.size(), readData.size());
					for (int i = 0; i < writeData.size(); ++i) {
						Assert.assertEquals(writeData.get(i), readData.get(i));
					}
				}
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testMetadata() {
		try {
			sqlitePyramidIO.initializeForWrite(PYRAMID_ID);

			String metadata = "Some metadata.";
			sqlitePyramidIO.writeMetaData(PYRAMID_ID, metadata);

			Assert.assertTrue(sqlitePyramidIO.readMetaData(PYRAMID_ID).equals(metadata));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
