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
package com.oculusinfo.binning;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TestPyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringIntPairArrayJsonSerializer;
import com.oculusinfo.factory.util.Pair;

@SuppressWarnings("deprecation")
public class SerializationTests {
	//@Test
	public void testBackwardCompatbilitySerialize() throws IOException{
		TestPyramidIO io = new TestPyramidIO();
		TileSerializer<Double> serializer
                = new com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer();

		TileIndex index = new TileIndex(0, 0, 0, 1, 1);
		TileData<Double> tile = new DenseTileData<Double>(index);
		tile.setBin(0, 0, 5.0);
		io.initializeForWrite("backwardsCompatibilityTest");
		io.writeTiles("backwardsCompatibilityTest", serializer, Collections.singleton(tile));



		List<TileData<Double>> tiles = io.readTiles("backwardsCompatibilityTest", serializer, Collections.singleton(index));
		TileData<Double> tileOut = tiles.get(0);
		double dataOut = tileOut.getBin(0, 0);

		Assert.assertEquals(5.0, dataOut);
	}

	@Test
	public void testStringIntPairArrayTileSerialization() throws IOException {
		TileSerializer<List<Pair<String, Integer>>> serializer = new StringIntPairArrayJsonSerializer();

		TileIndex index = new TileIndex(0, 0, 0, 1, 1);
		TileData<List<Pair<String, Integer>>> tile = new DenseTileData<List<Pair<String,Integer>>>(index);
		List<Pair<String, Integer>> data = new ArrayList<Pair<String,Integer>>();
		data.add(new Pair<String, Integer>("a", 1));
		data.add(new Pair<String, Integer>("b", 2));
		data.add(new Pair<String, Integer>("c", 3));
		data.add(new Pair<String, Integer>("d", 4));
		data.add(new Pair<String, Integer>("e", 5));
		tile.setBin(0, 0, data);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(tile, baos);
		baos.flush();
		baos.close();

		byte[] buffer = baos.toByteArray();

		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		TileData<List<Pair<String, Integer>>> result = serializer.deserialize(index, bais);

		Assert.assertEquals(index, result.getDefinition());
		List<Pair<String, Integer>> resultBin = result.getBin(0, 0);
		Assert.assertEquals(data.size(), resultBin.size());
		for (int i=0; i<data.size(); ++i) {
			Assert.assertEquals(data.get(i), resultBin.get(i));
		}
	}



	@Test
	public void testDoubleTileSerialization() throws IOException {
		TileIndex index = new TileIndex(2, 0, 1, 10, 20);
		TileData<Double> tile = new DenseTileData<Double>(index);
		for (int x=0; x<10; ++x) {
			for (int y=0; y<20; ++y) {
				tile.setBin(x, y, ((x+10*y)%7)/2.0);
			}
		}
		PyramidIO io = new TestPyramidIO();
		TileSerializer<Double> serializer = new PrimitiveAvroSerializer<>(Double.class, CodecFactory.nullCodec());
		io.writeTiles(".", serializer, Collections.singleton(tile));

		List<TileData<Double>> tilesOut = io.readTiles(".", serializer, Collections.singleton(index));
		Assert.assertEquals(1, tilesOut.size());
		TileData<Double> firstOut = tilesOut.get(0);
		Assert.assertEquals(tile.getDefinition(), firstOut.getDefinition());
		for (int x = 0; x < tile.getDefinition().getXBins(); ++x) {
			for (int y = 0; y < tile.getDefinition().getYBins(); ++y) {
				Assert.assertEquals(tile.getBin(x, y), firstOut.getBin(x, y), 1E-12);
			}
		}
	}

	@Test
	public void testDoubleArrayTileSerialization() throws IOException {
		TileIndex index = new TileIndex(2, 0, 1, 10, 20);
		TileData<List<Double>> tile = new DenseTileData<List<Double>>(index);
		for (int x=0; x<10; ++x) {
			for (int y=0; y<20; ++y) {
				tile.setBin(x, y, Arrays.asList(1.0*x, 2.0*y));
			}
		}
		PyramidIO io = new TestPyramidIO();
		TileSerializer<List<Double>> serializer = new PrimitiveArrayAvroSerializer<>(Double.class, CodecFactory.nullCodec());
		io.writeTiles(".", serializer, Collections.singleton(tile));

		List<TileData<List<Double>>> tilesOut = io.readTiles(".", serializer, Collections.singleton(index));
		Assert.assertEquals(1, tilesOut.size());
		TileData<List<Double>> firstOut = tilesOut.get(0);
		Assert.assertEquals(tile.getDefinition(), firstOut.getDefinition());
		for (int x = 0; x < tile.getDefinition().getXBins(); ++x) {
			for (int y = 0; y < tile.getDefinition().getYBins(); ++y) {
				List<Double> inData = tile.getBin(x, y);
				List<Double> outData = firstOut.getBin(x, y);
				Assert.assertEquals(inData.size(), outData.size());
				for (int j=0; j<inData.size(); ++j) {
					Assert.assertEquals(inData.get(j), outData.get(j), 1E-12);
				}
			}
		}
	}

	@Test
	public void testStringArrayTileSerialization() throws IOException {
		TileIndex index = new TileIndex(2, 0, 1, 10, 20);
		TileData<List<String>> tile = new DenseTileData<List<String>>(index);
		for (int x=0; x<10; ++x) {
			for (int y=0; y<20; ++y) {
				tile.setBin(x, y, Arrays.asList(String.format("bin [%d, %d]", x, y), "second", "third"));
			}
		}
		PyramidIO io = new TestPyramidIO();
		TileSerializer<List<String>> serializer = new PrimitiveArrayAvroSerializer<>(String.class, CodecFactory.nullCodec());
		io.writeTiles(".", serializer, Collections.singleton(tile));

		List<TileData<List<String>>> tilesOut = io.readTiles(".", serializer, Collections.singleton(index));
		Assert.assertEquals(1, tilesOut.size());
		TileData<List<String>> firstOut = tilesOut.get(0);
		Assert.assertEquals(tile.getDefinition(), firstOut.getDefinition());
		for (int x = 0; x < tile.getDefinition().getXBins(); ++x) {
			for (int y = 0; y < tile.getDefinition().getYBins(); ++y) {
				List<String> inData = tile.getBin(x, y);
				List<String> outData = firstOut.getBin(x, y);
				Assert.assertEquals(inData.size(), outData.size());
				for (int j=0; j<inData.size(); ++j) {
					Assert.assertEquals(inData.get(j), outData.get(j));
				}
			}
		}
	}

	@Test
	public void testUnicodeStringIntPairTileSerialization() throws IOException {
		TileSerializer<List<Pair<String, Integer>>> serializer = new StringIntPairArrayJsonSerializer();

		TileIndex index = new TileIndex(0, 0, 0, 1, 1);
		TileData<List<Pair<String, Integer>>> tile = new DenseTileData<List<Pair<String,Integer>>>(index);
		List<Pair<String, Integer>> data = new ArrayList<Pair<String,Integer>>();

		String[] unicode_examples = {
			"a",  		// Basic latin code block
			"\u00C0",	// Latin-1 Supplement code block
			"\u0108",	// Latin Extended-A code block
			"\u0194",	// Latin Extended-B code block
			"\u0255",	// IPA Extensions code block
			"\u02B7",	// Spacing Modifier Letters code block
			"\u0310",	// Combining Diacritical Marks code block
			"\u0398",	// Greek code block
			"\u0409",	// Cyrillic code block
			"\u0570",	// Armenian code block
			"\u05D0",	// Hebrew code block
			"\u060F",	// Arabic code block
			"\u21D0",	// Arrows code block
			"\u2602", 	// Misc symbols code block
			"\u2728",	// Dingbats code block
			"\u1F302", 	// Emjoi Extension code block
		};

		for (int i=0; i < unicode_examples.length; i++) {
			data.add(new Pair<String, Integer>(unicode_examples[i], 1));
		}

		tile.setBin(0, 0, data);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(tile, baos);
		baos.flush();
		baos.close();

		byte[] buffer = baos.toByteArray();

		ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
		TileData<List<Pair<String, Integer>>> result = serializer.deserialize(index, bais);

		Assert.assertEquals(index, result.getDefinition());
		List<Pair<String, Integer>> resultBin = result.getBin(0, 0);
		Assert.assertEquals(data.size(), resultBin.size());
		for (int i=0; i < data.size(); ++i) {
			Assert.assertEquals(data.get(i), resultBin.get(i));
		}
	}
}
