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

import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.data.impl.JSONAnnotation;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.impl.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.rest.AnnotationService;
import com.oculusinfo.annotation.rest.impl.CachedAnnotationServiceImpl;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
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
	static final int NUM_THREADS = 4;

	protected AnnotationService _service;	

    @Before
    public void setup () { 	
    	
    	try {

            String configFile = ".\\annotation-service\\src\\test\\config\\hbase-test-config.json";
            //String configFile = ".\\annotation-service\\src\\test\\config\\filesystem-io-test-config.json";
            Set<DelegateFactoryProviderTarget<PyramidIO>> ioSet = new HashSet<>();
            ioSet.add( DefaultPyramidIOFactoryProvider.HBASE.create() );
            ioSet.add( DefaultPyramidIOFactoryProvider.FILE_SYSTEM.create() );

            FactoryProvider<PyramidIO> ioFactoryProvider = new StandardPyramidIOFactoryProvider( ioSet );
            FactoryProvider<TileSerializer<?>> serializerFactoryProvider = new StandardTileSerializationFactoryProvider();
            FactoryProvider<TilePyramid> pyramidFactoryProvider = new StandardTilePyramidFactoryProvider();
            AnnotationIndexer annotationIndexer = new AnnotationIndexerImpl();
            AnnotationSerializer annotationSerializer = new JSONAnnotationDataSerializer();

            _service = new CachedAnnotationServiceImpl( configFile,
                                                        ioFactoryProvider,
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

    
    private class WriteReadRemove {
    	
    	AnnotationData<?> _data;
    	int _status = 0;
    	
    	WriteReadRemove( AnnotationData<?> data ) {
    		_data = data;
    	}
    	
    	private void write( String name ) {

            if ( VERBOSE )
    		    System.out.println( "Thread " + name + " writing " + _data.getUUID() );

			_service.write( TEST_LAYER_NAME, _data );
			_status++;
    	}
    	
    	private void read( String name ) {
    		long start = System.currentTimeMillis();
            TileIndex tile = getRandomTile();
			Map<BinIndex, List<AnnotationData<?>>> scan = readRandom( tile );

            int annotationCount = 0;
            for (List<AnnotationData<?>> annotations : scan.values()) {
                annotationCount += annotations.size();
            }

			long end = System.currentTimeMillis();
	    	double time = ((end-start)/1000.0);
	    	_readTimesPerEntry.get( name ).add( time );
            if ( VERBOSE )
	    	    System.out.println( "Thread " + name + " read " + scan.size() +" bins with " + annotationCount + " entries from " + tile.getLevel() + ", " + tile.getX() + ", " + tile.getY() + " in " + time );
			_status++;
    	}
    	
    	private void modify( String name ) {

            if ( VERBOSE )
    		    System.out.println( "Thread " + name + " writing " + _data.getUUID() );
    		JSONObject json = _data.toJSON();    		
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
    		
    		AnnotationData<?> newData = JSONAnnotation.fromJSON(json);
    		
			_service.modify( TEST_LAYER_NAME, _data, newData );
			_data = newData;
			_status++;
    	}
    	
    	private void remove( String name ) {

            if ( VERBOSE )
    		    System.out.println( "Thread " + name + " removing " + _data.getUUID() );

			_service.remove( TEST_LAYER_NAME, _data );
			_status++;
    	}
    	
    	public boolean process( String name ) {
    		
    		switch ( _status ) { 		
    			case 0:
    			{
    				write( name );
    				break;
    			}
    			case 1:
    			{  		   				
    				read( name );
    				break;
    			}
    			case 2:
    			{
    				modify( name );
    				break;
    			} 
    			case 3:
    			{
    				read( name );
    				break;
    			}  
    			case 4: 
    			{
    				remove( name );
    				System.out.println( "Thread " + name + " completed write, read, remove for index " + _data.getUUID().toString() );
        			return true;
    			}
    		}

    		return false;
    	}
    	
    }
    

    static ConcurrentMap<String, List<WriteReadRemove>> _dataRecord = new ConcurrentHashMap<>();   
    static ConcurrentMap<String, List<Double>> _readTimesPerEntry = new ConcurrentHashMap<>();
   
    
	private class Tester implements Runnable {
		
		String _name;

		Tester( String name ) {
			_name = name;
		}
		
		public void run() {
				
			// generate annotations
			List<WriteReadRemove> annotations = new LinkedList<>();
			for ( int i=0; i<NUM_ENTRIES; i++ ) {
				annotations.add( new WriteReadRemove( generateJSONAnnotation() ) );
			}	
			_dataRecord.put( _name, annotations );
			_readTimesPerEntry.put( _name, new LinkedList<Double>() );
						
			List<Integer> indices = new ArrayList<>();
			for (int i=0; i<NUM_ENTRIES; i++) {
				indices.add(i);
			}			
			Collections.shuffle( indices );
			
	    	while ( indices.size() > 0 ) {
	    		
	    		int i = (int)( Math.random() * indices.size() );
	    		if ( annotations.get( indices.get(i) ).process( _name ) ) {
	    			indices.remove(i);
	    		}
	    		
	    	}
	    	
		}		
	}
	
	
	@Test
	public void concurrentTest() {

        long start = System.currentTimeMillis();

        List<Thread> threads = new LinkedList<>();

        // write / read
        for (int i=0; i<NUM_THREADS; i++) {

            Thread t = new Thread( new Tester( ""+i ) );
            threads.add( t );
            t.start();
        }

        for( Thread t : threads ) {
            try {
                t.join();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        // ensure everything was removed
        Map<BinIndex, List<AnnotationData<?>>> scan = readAll();
        printData( scan );
        Assert.assertTrue( scan.size() == 0 );

        long end = System.currentTimeMillis();
        double time = ((end-start)/1000.0);
        System.out.println( "Completed in " + time + " seconds");

        double sum = 0;
        int count = 0;
        for ( List<Double> t : _readTimesPerEntry.values() ) {
            for ( Double d : t ) {
                sum += d;
                count++;
            }
        }
        System.out.println( "Average read times of " + ( sum / count ) + " seconds per scan");

		
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
