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
package com.oculusinfo.binning.io.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
//import java.util.zip.DeflaterOutputStream;

import org.apache.avro.file.CodecFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;



/*
 * Some tests to help compare serialization speeds between various schemes
 */
//@Ignore
public class SerializationSpeedTests {
	private TileSerializer<Double> _serializer;
	private TilePyramid            _pyramid;
	private TileData<Double>       _data;

	@Before
	public void setup () {
		_serializer = new DoubleAvroSerializer(CodecFactory.deflateCodec(4));
		_pyramid = new AOITilePyramid(0.0, 0.0, 1.0, 1.0);

		// Create some data
		Random random = new Random(15485863);
		_data = new TileData<>(new TileIndex(0, 0, 0, 256, 256));
		for (int x=0; x<256; ++x) {
			for (int y=0; y<256; ++y) {
				_data.setBin(x, y, random.nextDouble());
			}
		}
	}

	@After
	public void teardown () {
		_serializer = null;
		_pyramid = null;
		_data = null;
	}


	@Test
	public void testAvroTileSerialization () throws Exception {
		int N = 100;

		// test serialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<N; ++n) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			_serializer.serialize(_data, _pyramid, baos);
			baos.close();
			baos.flush();

		}
		long endTime = System.currentTimeMillis();
		System.out.println("Avro Serialization");
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
	}


	@Test
	public void testLegacyTileSerialization () throws Exception {
		int N = 100;
		TileSerializer<Double> serializer = new BackwardCompatibilitySerializer();

		// test serialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<N; ++n) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(_data, _pyramid, baos);
			baos.close();
			baos.flush();

		}
		long endTime = System.currentTimeMillis();
		System.out.println("Legacy Serialization");
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
	}


	@Test
	public void testJavaTileSerialization () throws Exception {
		int N = 100;

		// test serialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<N; ++n) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(_data);
			oos.close();
			oos.flush();
			baos.close();
			baos.flush();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Java Serialization");
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
	}


	@Test
	public void testAvroTileDeSerialization () throws Exception {
		int N = 100;

		// Get somethign to deserialization
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		_serializer.serialize(_data, _pyramid, baos);
		baos.close();
		baos.flush();
		byte[] data = baos.toByteArray();

		// test deserialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<N; ++n) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			_serializer.deserialize(null, bais);
			bais.close();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Avro Deserialization");
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
	}


	@Test
	public void testLegacyTileDeSerialization () throws Exception {
		int N = 100;
		TileSerializer<Double> serializer = new BackwardCompatibilitySerializer();

		// Get somethign to deserialization
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(_data, _pyramid, baos);
		baos.close();
		baos.flush();
		byte[] data = baos.toByteArray();
		TileIndex index = _data.getDefinition();

		// test deserialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<N; ++n) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			serializer.deserialize(index, bais);
			bais.close();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Legacy Deserialization");
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
	}


	@Test
	public void testJavaTileDeSerialization () throws Exception {
		int N = 100;

		// Get somethign to deserialization
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(_data);
		oos.close();
		oos.flush();
		baos.close();
		baos.flush();
		byte[] data = baos.toByteArray();

		// test deserialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<N; ++n) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Assert.assertTrue(ois.readObject() instanceof TileData);
			ois.close();
			bais.close();
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Java Deserialization");
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
	}
}
