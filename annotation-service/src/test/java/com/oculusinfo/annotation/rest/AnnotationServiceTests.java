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
package com.oculusinfo.annotation.rest;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.annotation.impl.JSONAnnotation;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.init.DefaultAnnotationFilterFactoryProvider;
import com.oculusinfo.annotation.init.DefaultAnnotationIOFactoryProvider;
import com.oculusinfo.annotation.init.providers.StandardAnnotationFilterFactoryProvider;
import com.oculusinfo.annotation.init.providers.StandardAnnotationIOFactoryProvider;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.util.AnnotationGenerator;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.FileBasedPyramidIO;
import com.oculusinfo.binning.io.impl.FileSystemPyramidSource;
import com.oculusinfo.binning.io.impl.HBasePyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.tile.init.DefaultPyramidIOFactoryProvider;
import com.oculusinfo.tile.init.DefaultTileSerializerFactoryProvider;
import com.oculusinfo.factory.providers.DelegateFactoryProviderTarget;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.tile.init.providers.*;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.layer.LayerService;
import com.oculusinfo.tile.rest.layer.LayerServiceImpl;
import org.json.JSONObject;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class AnnotationServiceTests {

    private static final Logger LOGGER = LoggerFactory.getLogger( AnnotationServiceTests.class );
	static final int NUM_THREADS = 8;
    static final double [] BOUNDS = { 180, 85.05, -180, -85.05};
    static final String [] GROUPS = {"Urgent", "High", "Medium", "Low"};
    static final int NUM_ENTRIES = 50;
	protected AnnotationService _service;
    protected String _layerId = "annotations-unit-test";
    protected String _dataId = "annotations-unit-test";
    protected LayerService _layerService;

	List<AnnotationWrapper> _publicAnnotations = new ArrayList<>();
	Integer _remainingAnnotations = NUM_ENTRIES * NUM_THREADS;
	Random _random = new Random( System.currentTimeMillis() );
	final Object decisionLock = new Object();

	@Before
	public void setup () { 	
    	
		try {

			String configFile = ("res:///filesystem-io-test-config.json").toString();

            Set<DelegateFactoryProviderTarget<PyramidIO>> tileIoSet = new HashSet<>();
            tileIoSet.addAll( Arrays.asList( DefaultPyramidIOFactoryProvider.values() ) );
            Set<DelegateFactoryProviderTarget<AnnotationIO>> annotationIoSet = new HashSet<>();
            annotationIoSet.addAll( Arrays.asList( DefaultAnnotationIOFactoryProvider.values() ) );
            Set<DelegateFactoryProviderTarget<TileSerializer<?>>> serializerSet = new HashSet<>();
            serializerSet.addAll( Arrays.asList( DefaultTileSerializerFactoryProvider.values() ) );
            Set<DelegateFactoryProviderTarget<AnnotationFilter>> filterIoSet = new HashSet<>();
            filterIoSet.addAll( Arrays.asList( DefaultAnnotationFilterFactoryProvider.values() ) );

            FactoryProvider<LayerConfiguration> layerConfigurationProvider = new StandardLayerConfigurationProvider(
                new StandardPyramidIOFactoryProvider( tileIoSet ),
                new StandardTilePyramidFactoryProvider(),
                new StandardTileSerializerFactoryProvider(serializerSet),
                new StandardImageRendererFactoryProvider(),
                new StandardTileTransformerFactoryProvider()
            );

            _layerService = new LayerServiceImpl( configFile, layerConfigurationProvider );

            AnnotationIndexer annotationIndexer = new AnnotationIndexerImpl();
			AnnotationSerializer annotationSerializer = new JSONAnnotationDataSerializer();

			_service = new AnnotationServiceImpl( _layerService,
              annotationSerializer,
              annotationIndexer,
              new StandardAnnotationIOFactoryProvider( annotationIoSet ),
              new StandardAnnotationFilterFactoryProvider( filterIoSet ) );

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

            AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

			// generate private local annotations
			for ( int i=0; i<NUM_ENTRIES; i++ ) {
				_annotations.add( new AnnotationWrapper( generator.generateJSONAnnotation() ) );
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
			_service.write( _layerId, annotation.clone() );
			long end = System.currentTimeMillis();
			double time = ((end-start)/1000.0);
			LOGGER.debug( "Thread " + _name + " successfully wrote " + clone.getUUID() + " in " + time + " sec" );
			addAnnotationToPublic( annotation );
		}

		private void read() {

			TileIndex tile = getRandomTile();
			long start = System.currentTimeMillis();
			List<AnnotationData<?>> scan = readTile( tile );
			long end = System.currentTimeMillis();
			double time = ((end-start)/1000.0);
			LOGGER.debug( "Thread " + _name + " read " + scan.size() + " entries from " + tile.getLevel() + ", " + tile.getX() + ", " + tile.getY() + " in " + time + " sec" );
		}

		private void modify( AnnotationWrapper annotation ) {

			AnnotationData<?> oldAnnotation = annotation.clone();
			AnnotationData<?> newAnnotation =  editAnnotation( oldAnnotation );

			try {
				long start = System.currentTimeMillis();
				_service.modify( _layerId, newAnnotation );
				long end = System.currentTimeMillis();
				double time = ((end-start)/1000.0);
				annotation.update( newAnnotation );
				LOGGER.debug( "Thread " + _name + " successfully modified " + newAnnotation.getUUID() + " in " + time + " sec" );

			} catch (Exception e) {

				LOGGER.debug( "Thread " + _name + " unsuccessfully modified " + newAnnotation.getUUID() );
			}

		}

		private void remove( AnnotationWrapper annotation ) {

			AnnotationData<?> clone = annotation.clone();
			try {
				long start = System.currentTimeMillis();
				_service.remove( _layerId, clone.getCertificate() );
				long end = System.currentTimeMillis();
				double time = ((end-start)/1000.0);
				removeAnnotationFromPublic(annotation);
				LOGGER.debug("Thread " + _name + " successfully removed " + clone.getUUID() + " in " + time + " sec");

			} catch (Exception e) {

				LOGGER.debug("Thread " + _name + " unsuccessfully removed " + clone.getUUID() );
			}
		}

		private AnnotationData<?> editAnnotation( AnnotationData<?> annotation ) {

			JSONObject json = annotation.toJSON();
            AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

			try {
				int type = (int)(Math.random() * 2);
				switch (type) {

				case 0:
					// change position
					double [] xy = generator.randomPosition();
					json.put("x", xy[0]);
					json.put("y", xy[1]);
					break;

				default:
					// change data
					JSONObject data = new JSONObject();
					data.put("comment", generator.randomComment() );
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
			List<AnnotationData<?>> scan = readAll();
			Assert.assertTrue(scan.size() == 0);

			long end = System.currentTimeMillis();
			double time = ((end - start) / 1000.0);
			LOGGER.debug("Completed in " + time + " seconds");

		} finally {

			try {

				LayerConfiguration config = _layerService.getLayerConfiguration( _layerId, null );
				PyramidIO tileIo = config.produce( PyramidIO.class );
				AnnotationIO dataIo = config.produce( AnnotationIO.class );
                LOGGER.debug("Deleting temporary file system folders");
                try {
                    File testDir = new File( ".\\" + _dataId );
                    for ( File f : testDir.listFiles() ) {
                        f.delete();
                    }
                    testDir.delete();
                } catch ( Exception e ) {
                    // swallow exception
                }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    private List<AnnotationData<?>> readTile( TileIndex tile ) {
        List<AnnotationData<?>> annotations = new ArrayList<>();
        List<List<AnnotationData<?>>> data = _service.read( _layerId, tile, null );
        if ( data != null ) {
            for ( List<AnnotationData<?>> bin : data ) {
                for ( AnnotationData<?> annotation : bin ) {
                    annotations.add( annotation );
                }
            }
            Assert.assertTrue( validateTile( annotations ) );
        }
		return annotations;
	}
	
	private List<AnnotationData<?>> readAll() {
		// scan all
		return readTile( new TileIndex( 0, 0, 0 ) );
	}

    private boolean validateTile( List<AnnotationData<?>> annotations ) {
        for ( int  i=0; i<annotations.size(); i++ ) {
            for ( int  j=i+1; j<annotations.size(); j++ ) {
                if ( annotations.get(i) == annotations.get(j) ) {
                    // duplicate found
                    LOGGER.error( "Duplicate instance of annotation found in same tile" );
                    return false;
                }
            }
        }
        return true;
    }

	private TileIndex getRandomTile() {
		final int MAX_DEPTH = 4;
		int level = (int)(Math.random() * MAX_DEPTH);
		int x = (int)(Math.random() * (level * (1 << level)) );
		int y = (int)(Math.random() * (level * (1 << level)) );
		return new TileIndex( level, x, y, AnnotationIndexer.NUM_BINS, AnnotationIndexer.NUM_BINS );
	}
}
