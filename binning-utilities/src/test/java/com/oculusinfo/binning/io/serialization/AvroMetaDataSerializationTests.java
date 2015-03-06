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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.avro.file.CodecFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleJsonSerializer;



public class AvroMetaDataSerializationTests {
	private TileIndex        _index;
	private TileData<Double> _tile;

	@Before
	public void setup () {
		_index = new TileIndex(0, 0, 0, 2, 2);


		_tile = new DenseTileData<>(_index);
		_tile.setBin(0, 0, 1.0);
		_tile.setBin(0, 1, 2.0);
		_tile.setBin(1, 0, 3.0);
		_tile.setBin(1, 1, 4.0);
		_tile.setMetaData("a", "abc");
		_tile.setMetaData("b", "bcd");
	}

	@After
	public void tearDown () {
	    _tile = null;
	    _index = null;
	}

	@Test
	public void testAvroMetaDataSerialization () throws Exception {
		TileSerializer<Double> serializer = new PrimitiveAvroSerializer<>(Double.class, CodecFactory.nullCodec());

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		serializer.serialize(_tile, output);
		output.flush();
		output.close();

		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		TileData<Double> received = serializer.deserialize(_index, input);

		Assert.assertEquals(2, received.getMetaDataProperties().size());
		Assert.assertTrue(received.getMetaDataProperties().contains("a"));
		Assert.assertTrue(received.getMetaDataProperties().contains("b"));
		Assert.assertEquals("abc", received.getMetaData("a"));
		Assert.assertEquals("bcd", received.getMetaData("b"));
	}

	@Test
	public void testJavaMetaDataSerialization () throws Exception {
		TileSerializer<Double> serializer = new DoubleJsonSerializer();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		serializer.serialize(_tile, output);
		output.flush();
		output.close();

		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		TileData<Double> received = serializer.deserialize(_index, input);

		Assert.assertEquals(2, received.getMetaDataProperties().size());
		Assert.assertTrue(received.getMetaDataProperties().contains("a"));
		Assert.assertTrue(received.getMetaDataProperties().contains("b"));
		Assert.assertEquals("abc", received.getMetaData("a"));
		Assert.assertEquals("bcd", received.getMetaData("b"));
	}
}
