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

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.rest.impl.*;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;


public class ConcurrentServiceTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	static final int NUM_THREADS = 20;
	protected AnnotationService    _service;	
	protected List<AnnotationData> _annotations;
	
    @Before
    public void setup () {
    	
    	_service = new AnnotationServiceImpl();

    }

    @After
    public void teardown () {
    	_service = null;
    }


    static ConcurrentMap<String, List<AnnotationData>> _dataRecord = new ConcurrentHashMap<>();
    static ConcurrentMap<String, AnnotationTile> _tileRecord = new ConcurrentHashMap<>();
    
    public void read( String name ) {
		
		final int NUM_READS = 20;
		for ( int i=0; i<NUM_READS; i++ ) {
			// read
	    	System.out.println( "Thread " + name + " begins reading entries" );
	    	List<AnnotationData> scan = readRandom();	
	    	System.out.println( "Thread " + name + " read " + scan.size() +" entries" );
		}
    	
	}
    
    public void remove( String name, boolean firstHalf ) {

		List<AnnotationData> subset;
		if ( firstHalf ) {
			subset = _dataRecord.get( name ).subList(0, NUM_ENTRIES/2);
		} else {
			subset = _dataRecord.get( name ).subList(NUM_ENTRIES/2, NUM_ENTRIES);    		
		}
		// remove
    	System.out.println( "Thread " + name + " begins removing all" );
    	
		for (AnnotationData annotation : subset ) {
    		_service.removeAnnotation( annotation );
    		System.out.println( "Thread " + name + " removed "+ annotation.getIndex() );
    	}
		System.out.println( "Thread " + name + " finished removing all" );
		
	}
    
    public void write( String name, boolean firstHalf ) {
		
		List<AnnotationData> subset;
		if ( firstHalf ) {
			subset = _dataRecord.get( name ).subList(0, NUM_ENTRIES/2);
		} else {
			subset = _dataRecord.get( name ).subList(NUM_ENTRIES/2, NUM_ENTRIES);    		
		}
		
		// write
		System.out.println( "Thread " + name + " begins writing first half" );    	
    	for (AnnotationData annotation : subset ) {    	    		
    		_service.writeAnnotation( annotation );
    		System.out.println( "Thread " + name + " wrote "+ annotation.getIndex() );
    	}
    	
	}
	
    
	private class RemoveReader implements Runnable {
		
		String _name;

		RemoveReader( String name ) {
			_name = name;
		}
		
		public void run() {
					
			remove( _name, true );		
			read( _name );			
			remove( _name, false );
	    	
		}		
	}
	
	private class WriteReader implements Runnable {
		
		String _name;

		WriteReader( String name ) {
			_name = name;
		}
		
		public void run() {
			
			// generate annotations
			List<AnnotationData> annotations = generateJSONAnnotations( NUM_ENTRIES );				
			_dataRecord.put( _name, annotations );
						
			write( _name, true );		
			read( _name );			
			write( _name, false );
	    	
		}		
	}
	
	@Test
	public void concurrentTest() {
			
		List<Thread> threads = new LinkedList<>();
		
		// write / read
		for (int i=0; i<NUM_THREADS; i++) {
						
			Thread t = new Thread( new WriteReader( ""+i ) );
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
		
		// check everything
		List<AnnotationData> scan = readAll();		
		System.out.println( "Scan resulted in " + scan.size() + " entries read" );
		System.out.println( "Checking to see if all values from record have been written" );
		
		for ( List<AnnotationData> record: _dataRecord.values() ) {			
			for ( AnnotationData data : record ) {
				Assert.assertTrue( scan.contains( data ) );
			}			
		}
		
		
		// remove / read
		for (int i=0; i<NUM_THREADS; i++) {
			
			Thread t = new Thread( new RemoveReader( ""+i ) );
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
    	scan = readAll();
    	printData( scan );
    	Assert.assertTrue( scan.size() == 0 );
	}
	
	private List<AnnotationData> readAll() {
		
		// scan all
		TileIndex tile = new TileIndex( 0, 0, 0 );
    	List<AnnotationData> scan = _service.readAnnotation( tile );   	
    	return scan;

	}
	
	private List<AnnotationData> readRandom() {
		
		/*
		int level = (int)(Math.random() * 4);
		int x = (int)(Math.random() * (level * (1 << level)) );
		int y = (int)(Math.random() * (level * (1 << level)) );
		// scan all
		TileIndex tile = new TileIndex( level, x, y, AnnotationTile.NUM_BINS, AnnotationTile.NUM_BINS );
		*/
		TileIndex tile = new TileIndex( 0, 0, 0 );
    	
		List<AnnotationData> scan = _service.readAnnotation( tile );   	
    	return scan;

	}

}
