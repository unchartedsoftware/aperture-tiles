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
import java.util.ArrayList;
import java.util.Random;
//import java.util.zip.DeflaterOutputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;



/*
 * Some tests to help compare serialization speeds between various schemes
 */
@Ignore
public class SerializationSpeedTests {
    private TileSerializer<Double> _serializer;
    private TilePyramid            _pyramid;
    private TileData<Double>       _data;

    @Before
    public void setup () {
        _serializer = new DoubleAvroSerializer();
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
    public void testKryoTileSerialization () throws Exception {
    	int N = 100;

    	Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);
    	
        kryo.register(TileData.class);
        kryo.register(TileIndex.class);
        kryo.register(ArrayList.class);
        
        // test serialization time
        long startTime = System.currentTimeMillis();
        for (int n=0; n<N; ++n) {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	Output output = new Output( baos );
        	kryo.writeObject(output, _data);
            output.close();
//            System.out.println("Tile size: "+baos.toByteArray().length);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Kryo Serialization");
        System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
        System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
    }

    @Test
    public void testKryoTileDeSerialization () throws Exception {
        int N = 100;

        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);        
        Output output = new Output( new ByteArrayOutputStream() );      
        kryo.register(TileData.class);
        kryo.register(TileIndex.class);
        kryo.register(ArrayList.class);
        kryo.writeObject(output, _data);
        output.close();
        
        byte[] data = ((ByteArrayOutputStream)output.getOutputStream()).toByteArray();
        
        
        // test deserialization time
        long startTime = System.currentTimeMillis();
        for (int n=0; n<N; ++n) {
        	Input input = new Input( data );
        	TileData<Double> newTile = kryo.readObject(input, TileData.class);
        	input.close();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Kryo Deserialization");
        System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
        System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
    }

    @Test
    public void testAvroTileSerialization () throws Exception {
        int N = 100;

        // test serialization time
        long startTime = System.currentTimeMillis();
        int length = 0;
        for (int n=0; n<N; ++n) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            _serializer.serialize(_data, _pyramid, baos);
            baos.close();
            baos.flush();
            length += baos.toByteArray().length;
        }
        long endTime = System.currentTimeMillis();
        System.out.println();
        System.out.println("Avro Serialization");
        System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
        System.out.println("Average time: "+(((endTime-startTime)/1000.0)/N)+" seconds");
        System.out.println("Average size: "+(length/N));
        System.out.println();
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
