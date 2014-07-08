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
package com.oculusinfo.annotation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oculusinfo.annotation.config.AnnotationConfiguration;
import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.init.DefaultAnnotationIOFactoryProvider;
import com.oculusinfo.annotation.init.providers.StandardAnnotationIOFactoryProvider;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.impl.HBaseAnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.impl.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.rest.AnnotationService;
import com.oculusinfo.annotation.rest.impl.AnnotationServiceImpl;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.HBasePyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.tile.init.DefaultPyramidIOFactoryProvider;
import com.oculusinfo.tile.init.DelegateFactoryProviderTarget;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.init.providers.StandardPyramidIOFactoryProvider;
import com.oculusinfo.tile.init.providers.StandardTilePyramidFactoryProvider;
import com.oculusinfo.tile.init.providers.StandardTileSerializationFactoryProvider;


public class ConcurrentWriteTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	static final int NUM_THREADS = 8;

	protected AnnotationService _service;

    Random _random = new Random( System.currentTimeMillis() );

    @Before
    public void setup () { 	
    	
    	try {

            String configFile = ".\\annotation-service\\src\\test\\config\\hbase-test-config.json";
            //String configFile = ".\\annotation-service\\src\\test\\config\\filesystem-io-test-config.json";

            Set<DelegateFactoryProviderTarget<PyramidIO>> tileIoSet = new HashSet<>();
            tileIoSet.add( DefaultPyramidIOFactoryProvider.HBASE.create() );
            tileIoSet.add( DefaultPyramidIOFactoryProvider.FILE_SYSTEM.create() );
            FactoryProvider<PyramidIO> tileIoFactoryProvider = new StandardPyramidIOFactoryProvider( tileIoSet );

            Set<DelegateFactoryProviderTarget<AnnotationIO>> annotationIoSet = new HashSet<>();
            annotationIoSet.add( DefaultAnnotationIOFactoryProvider.HBASE.create() );
            annotationIoSet.add( DefaultAnnotationIOFactoryProvider.FILE_SYSTEM.create() );
            FactoryProvider<AnnotationIO> annotationIoFactoryProvider = new StandardAnnotationIOFactoryProvider( annotationIoSet );

            FactoryProvider<TileSerializer<?>> serializerFactoryProvider = new StandardTileSerializationFactoryProvider();
            FactoryProvider<TilePyramid> pyramidFactoryProvider = new StandardTilePyramidFactoryProvider();
            AnnotationIndexer annotationIndexer = new AnnotationIndexerImpl();
            AnnotationSerializer annotationSerializer = new JSONAnnotationDataSerializer();

            _service = new AnnotationServiceImpl( configFile,
                                                  tileIoFactoryProvider,
                                                  annotationIoFactoryProvider,
                                                  serializerFactoryProvider,
                                                  pyramidFactoryProvider,
                                                  annotationIndexer,
                                                  annotationSerializer );

    	} catch (Exception e) {
            throw e;
		}
    	
    }

    @After
    public void teardown () {
    	_service = null;
    }

    
	private class Tester implements Runnable {

        private String _name;
        private List<AnnotationData<?>> _annotations = new LinkedList<>();

        Tester( String name ) {

            // set thread name
			_name = name;
            // generate private local annotations
            for ( int i=0; i<NUM_ENTRIES; i++ ) {
                _annotations.add( generateJSONAnnotation() );
            }
		}
		
		public void run() {

            while (  _annotations.size() > 0 ) {

                // write
                int index = _random.nextInt( _annotations.size() );
                AnnotationData<?> annotation = _annotations.get( index );
                _annotations.remove( index );
                write( annotation );
            }
	    	
		}

        private void write( AnnotationData<?> annotation ) {

            long start = System.currentTimeMillis();
            _service.write( TEST_LAYER_NAME, annotation );
            long end = System.currentTimeMillis();
            double time = ((end-start)/1000.0);
            if ( VERBOSE )
                System.out.println( "Thread " + _name + " successfully wrote " + annotation.getUUID() + " in " + time + " sec" );
        }
	}
	
	
	@Test
	public void concurrentTest() {

        try {

            long start = System.currentTimeMillis();

            List<Thread> threads = new LinkedList<>();

            // write / read
            for (int i = 0; i < NUM_THREADS; i++) {

                Thread t = new Thread(new Tester("" + i));
                threads.add(t);
                t.start();
            }

            for (Thread t : threads) {
                try {
                    t.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            long end = System.currentTimeMillis();
            double time = ((end - start) / 1000.0);
            System.out.println("Completed in " + time + " seconds");

        } finally {

            try {

                AnnotationConfiguration config = _service.getConfiguration( TEST_LAYER_NAME );
                PyramidIO tileIo = config.produce( PyramidIO.class );
                AnnotationIO dataIo = config.produce( AnnotationIO.class );
                if ( tileIo instanceof HBasePyramidIO ) {
                    System.out.println("Dropping tile HBase table");
                    ((HBasePyramidIO)tileIo).dropTable( TEST_LAYER_NAME );
                }
                if ( dataIo instanceof HBaseAnnotationIO ) {
                    System.out.println("Dropping data HBase table");
                    ((HBaseAnnotationIO)dataIo).dropTable( TEST_LAYER_NAME );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
		
	}

}
