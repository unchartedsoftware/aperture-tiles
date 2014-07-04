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

import com.oculusinfo.annotation.config.AnnotationConfiguration;
import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.data.impl.JSONAnnotation;
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
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
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
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ConcurrentServiceTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	static final int NUM_THREADS = 8;

	protected AnnotationService _service;

    List<AnnotationWrapper> _publicAnnotations = new ArrayList<>();
    Integer _remainingAnnotations = NUM_ENTRIES * NUM_THREADS;
    Random _random = new Random( System.currentTimeMillis() );
    final Object decisionLock = new Object();

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

    public synchronized AnnotationWrapper getRandomPublicAnnotation() {
        if ( _publicAnnotations.size() == 0 ) {
            return null;
        }
        int index = _random.nextInt( _publicAnnotations.size() );
        return _publicAnnotations.get( index );
    }

    public synchronized void addAnnotationToPublic( AnnotationWrapper annotation ) {
        _publicAnnotations.add( annotation );
    }

    public synchronized void removeAnnotationFromPublic( AnnotationWrapper annotation ) {
        _publicAnnotations.remove( annotation );
        _remainingAnnotations--;
    }

    public synchronized int getRemainingAnnotations() {
        return _remainingAnnotations;
    }

    private class AnnotationWrapper {

        private AnnotationData<?> _data;

        AnnotationWrapper( AnnotationData<?> data ) {
    		_data = data;
    	}

        public synchronized AnnotationData<?> clone() {

            JSONObject json = _data.toJSON();
            return JSONAnnotation.fromJSON( json );
        }

        public synchronized void update(AnnotationData<?> newState ) {
            _data = newState;
        }
    	
    }

    
	private class Tester implements Runnable {

        private String _name;
        private List<AnnotationWrapper> _annotations = new LinkedList<>();

        Tester( String name ) {

            // set thread name
			_name = name;
            // generate private local annotations
            for ( int i=0; i<NUM_ENTRIES; i++ ) {
                _annotations.add( new AnnotationWrapper( generateJSONAnnotation() ) );
            }
		}
		
		public void run() {

            while ( true ) {

                int operation;
                AnnotationWrapper randomAnnotation;

                // decision process is atomic
                synchronized( decisionLock ) {

                    if ( getRemainingAnnotations() == 0 && _annotations.size() == 0 ) {
                        // no more annotations, we are done
                        return;
                    }

                    // get a random annotation
                    randomAnnotation = getRandomPublicAnnotation();

                    if ( randomAnnotation == null ) {

                        // no available public annotations
                        if ( _annotations.size() > 0 ) {
                            // local is available
                            operation = _random.nextInt(2) + 2;     // 2-3 read or write
                        } else {
                            // nothing available at the moment
                            operation = -1;
                        }

                    } else {
                        // public annotations available
                        if ( _annotations.size() > 0 ) {
                            // public and local both available
                            operation = _random.nextInt(4);         // 0-3 modify, remove, read or write
                        } else {
                            // only public available
                            operation = _random.nextInt(3);          // 0-2 modify, remove, or read
                        }
                    }

                }

                switch ( operation ) {

                    case 0:

                        //  modify
                        modify( randomAnnotation );
                        break;

                    case 1:

                        // remove
                        remove( randomAnnotation );
                        break;

                    case 2:

                        // read
                        read();
                        break;

                    case 3:

                        // write
                        int index = _random.nextInt( _annotations.size() );
                        AnnotationWrapper annotation = _annotations.get( index );
                        _annotations.remove( index );
                        write( annotation );
                        break;

                    default:

                        break;

                }

            }
	    	
		}

        private void write( AnnotationWrapper annotation ) {

            AnnotationData<?> clone = annotation.clone();
            long start = System.currentTimeMillis();
            _service.write( TEST_LAYER_NAME, annotation.clone() );
            long end = System.currentTimeMillis();
            double time = ((end-start)/1000.0);
            if ( VERBOSE )
                System.out.println( "Thread " + _name + " successfully wrote " + clone.getUUID() + " in " + time + " sec" );
            addAnnotationToPublic( annotation );
        }

        private void read() {

            TileIndex tile = getRandomTile();
            long start = System.currentTimeMillis();
            Map<BinIndex, List<AnnotationData<?>>> scan = readRandom( tile );
            long end = System.currentTimeMillis();
            double time = ((end-start)/1000.0);

            int annotationCount = 0;
            for (List<AnnotationData<?>> annotations : scan.values()) {
                annotationCount += annotations.size();
            }

            if ( VERBOSE )
                System.out.println( "Thread " + _name + " read " + scan.size() +" bins with " + annotationCount + " entries from " + tile.getLevel() + ", " + tile.getX() + ", " + tile.getY() + " in " + time + " sec" );
        }

        private void modify( AnnotationWrapper annotation ) {

            AnnotationData<?> oldAnnotation = annotation.clone();
            AnnotationData<?> newAnnotation =  editAnnotation( oldAnnotation );

            try {
                long start = System.currentTimeMillis();
                _service.modify( TEST_LAYER_NAME, newAnnotation );
                long end = System.currentTimeMillis();
                double time = ((end-start)/1000.0);
                annotation.update( newAnnotation );
                if ( VERBOSE )
                    System.out.println( "Thread " + _name + " successfully modified " + newAnnotation.getUUID() + " in " + time + " sec" );

            } catch (Exception e) {

                if ( VERBOSE )
                    System.out.println( "Thread " + _name + " unsuccessfully modified " + newAnnotation.getUUID() );
            }

        }

        private void remove( AnnotationWrapper annotation ) {

            AnnotationData<?> clone = annotation.clone();
            try {
                long start = System.currentTimeMillis();
                _service.remove( TEST_LAYER_NAME, clone.getCertificate() );
                long end = System.currentTimeMillis();
                double time = ((end-start)/1000.0);
                removeAnnotationFromPublic(annotation);
                if (VERBOSE)
                    System.out.println("Thread " + _name + " successfully removed " + clone.getUUID() + " in " + time + " sec");

            } catch (Exception e) {

                if (VERBOSE)
                    System.out.println("Thread " + _name + " unsuccessfully removed " + clone.getUUID() );
            }
        }

        private AnnotationData<?> editAnnotation( AnnotationData<?> annotation ) {
            JSONObject json = annotation.toJSON();
            try {
                int type = (int)(Math.random() * 2);
                switch (type) {

                    case 0:
                        // change position
                        double [] xy = randomPosition();
                        json.put("x", xy[0]);
                        json.put("y", xy[1]);
                        break;

                    default:
                        // change data
                        JSONObject data = new JSONObject();
                        data.put("comment", randomComment() );
                        json.put("data", data);
                        break;
                }

            } catch ( Exception e ) {
                e.printStackTrace();
            }

            return JSONAnnotation.fromJSON(json);
        }
	}
	
	
	@Test
	public void concurrentTest() {

        try {
            /*
                This test is designed to mimic a high user write / modify / read / remove traffic.
                All test threads begin with a list of annotations that will be written. Once an annotation
                is written, its existence becomes public and any other thread may read / modify / remove it.
            */
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

            // ensure everything was removed
            Map<BinIndex, List<AnnotationData<?>>> scan = readAll();
            printData(scan);
            Assert.assertTrue(scan.size() == 0);

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
	
	private Map<BinIndex, List<AnnotationData<?>>> readAll() {
		
		// scan all
		TileIndex tile = new TileIndex( 0, 0, 0 );
    	Map<BinIndex, List<AnnotationData<?>>> scan = _service.read( null, TEST_LAYER_NAME, tile );
    	return scan;

	}

    private TileIndex getRandomTile() {

        final int MAX_DEPTH = 4;
        int level = (int)(Math.random() * MAX_DEPTH);
        int x = (int)(Math.random() * (level * (1 << level)) );
        int y = (int)(Math.random() * (level * (1 << level)) );
        return new TileIndex( level, x, y, AnnotationIndexer.NUM_BINS, AnnotationIndexer.NUM_BINS );
    }
	
	private Map<BinIndex, List<AnnotationData<?>>> readRandom( TileIndex tile ) {

		Map<BinIndex, List<AnnotationData<?>>> scan = _service.read( null, TEST_LAYER_NAME, tile );
    	return scan;
	}

}
