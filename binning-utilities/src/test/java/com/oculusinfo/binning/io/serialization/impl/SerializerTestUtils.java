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
package com.oculusinfo.binning.io.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;

public class SerializerTestUtils {
	/**
	 * Check if two (single-value-per-bin) tiles are equal
	 */
	public static <T> void assertTilesEqual (TileData<T> expected, TileData<T> actual) {
		Assert.assertEquals(expected.getDefinition(), actual.getDefinition());
		int xN = expected.getDefinition().getXBins();
		int yN = expected.getDefinition().getYBins();
		for (int x=0; x<xN; ++x) {
			for (int y=0; y<yN; ++y) {
				Assert.assertEquals(expected.getBin(x, y), actual.getBin(x, y));
			}
		}
	}


	/**
	 * Check if two (list-of-values-per-bin) tiles are equal
	 */
	public static <T> void assertListTilesEqual (TileData<List<T>> expected, TileData<List<T>> actual) {
		Assert.assertEquals(expected.getDefinition(), actual.getDefinition());
		int xN = expected.getDefinition().getXBins();
		int yN = expected.getDefinition().getYBins();
		for (int x=0; x<xN; ++x) {
			for (int y=0; y<yN; ++y) {
				List<T> expectedBin = expected.getBin(x, y);
				List<T> actualBin = actual.getBin(x, y);
				int zN = expectedBin.size();

				Assert.assertEquals(zN, actualBin.size());
				for (int z=0; z<zN; ++z)
					Assert.assertEquals(expectedBin.get(z), actualBin.get(z));
			}
		}
	}

	/**
	 * Send a tile through a round trip serialization, serializing through the writer, and deserializing through the reader.
	 * @throws IOException 
	 */
	public static <T> TileData<T> roundTrip (TileData<T> input, TileSerializer<T> writer, TileSerializer<T> reader) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer.serialize(input, baos);
		baos.flush();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return reader.deserialize(input.getDefinition(), bais);
	}

	/**
	 * Test if two serializers are equal, by creating a tile data, and making sure each reads the 
	 * output of the other.
	 */
	public static <T> void assertSerializersEqual (TileSerializer<T> expected,
	                                               TileSerializer<T> actual,
	                                               DataSource<T> random) throws Exception {
		int nX = 8;
		int nY = 8;
		TileIndex index = new TileIndex(4, 3, 2, nX, nY);
		TileData<T> data = new DenseTileData<>(index);
		for (int x=0; x<nX; ++x) {
			for (int y=0; y<nY; ++y) {
				data.setBin(x, y, random.create());
			}
		}

		assertTilesEqual(data, roundTrip(data, expected, actual));
		assertTilesEqual(data, roundTrip(data, actual, expected));
	}

	/** null-aware equality test */
	public static boolean objectsEqual (Object a, Object b) {
		if (null == a) return null == b;
		else return a.equals(b);
	}


	/**
	 * A source of data to hand to a test, so the test can construct real tiles.
	 */
	public static interface DataSource<T> {
		T create ();
		void reset ();
	}

	/**
	 * A source for integer data
	 */
	public static class IntegerSource implements DataSource<Integer> {
		@Override
		public void reset () {}

		@Override
		public Integer create () {
			return (int) Math.floor(Math.random()*1000);
		}
	}

	/**
	 * A source for double data
	 */
	public static class DoubleSource implements DataSource<Double> {
		@Override
		public void reset () {}

		@Override
		public Double create () {
			return Math.random()*1000;
		}
	}
}
