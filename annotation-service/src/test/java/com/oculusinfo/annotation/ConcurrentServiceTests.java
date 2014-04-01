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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Collections;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.google.inject.Inject;

import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.rest.impl.*;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;


public class ConcurrentServiceTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	static final int NUM_THREADS = 20;

	protected AnnotationService    _service;	
	
    @Before
    public void setup () { 	
    	//_service = new CachedAnnotationServiceImpl();
    }

    @After
    public void teardown () {
    	_service = null;
    }

    
    private class WriteReadRemove {
    	
    	AnnotationData _data;
    	int _status = 0;
    	
    	WriteReadRemove( AnnotationData data ) {
    		_data = data;
    	}
    	
    	AnnotationData getData() {
    		return _data;
    	}
    	
    	boolean process( String name ) {
    		
    		switch ( _status ) { 		
    			case 0:
    			{
    				//System.out.println( "Thread " + name + " writing " + _data.getIndex() );   		    	
    				_service.writeAnnotation( _data );	
    				_status++;
    				break;
    			}
    			case 1:
    			{  		    	
    				long start = System.currentTimeMillis();
    				List<AnnotationData> scan = readRandom();	   				
    				long end = System.currentTimeMillis();
    		    	double time = ((end-start)/1000.0);
    		    	_readTimesPerEntry.get( name ).add( time );
    		    	//System.out.println( "Thread " + name + " read " + scan.size() +" entries in " + time );	
    				_status++;
    				break;
    			}	
    			case 2:
    			{
    				//System.out.println( "Thread " + name + " removing " + _data.getIndex() );   		    	
    				_service.removeAnnotation( _data );		
    				_status++;
    				break;
    			}  
    			case 3: 
    			{
    				System.out.println( "Thread " + name + " completed write, read, remove for index " + _data.getIndex() );
        			return true;
    			}
    		}

    		return false;
    	}
    	
    }
    

    static ConcurrentMap<String, List<WriteReadRemove>> _dataRecord = new ConcurrentHashMap<>();   
    static ConcurrentMap<String, List<Double>> _readTimesPerEntry = new ConcurrentHashMap<>();
    
    //static ConcurrentMap<String, AnnotationTile> _tileRecord = new ConcurrentHashMap<>();
    
   
    
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

		/*
		// check everything
		List<AnnotationData> scan = readAll();		
		System.out.println( "Scan resulted in " + scan.size() + " entries read" );
		System.out.println( "Checking to see if all values from record have been written" );
		
		for ( List<WriteReadRemove> records: _dataRecord.values() ) {			
			for ( WriteReadRemove record : records ) {
				Assert.assertTrue( scan.contains( record.getData() ) );
			}			
		}
		*/
		
		// ensure everything was removed
		List<AnnotationData> scan = readAll();
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
	
	private List<AnnotationData> readAll() {
		
		// scan all
		TileIndex tile = new TileIndex( 0, 0, 0 );
    	List<AnnotationData> scan = _service.readAnnotations( tile );   	
    	return scan;

	}
	
	private List<AnnotationData> readRandom() {
		
		final int MAX_DEPTH = 4;
		int level = (int)(Math.random() * MAX_DEPTH);
		int x = (int)(Math.random() * (level * (1 << level)) );
		int y = (int)(Math.random() * (level * (1 << level)) );
		// scan all
		TileIndex tile = new TileIndex( level, x, y, AnnotationTile.NUM_BINS, AnnotationTile.NUM_BINS );
		
		//TileIndex tile = new TileIndex( 0, 0, 0 );
    	
		List<AnnotationData> scan = _service.readAnnotations( tile );   	
    	return scan;

	}

}
