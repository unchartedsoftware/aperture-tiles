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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.Number;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.query.*;

public class AnnotationHBaseIOTests extends AnnotationTestsBase {
	
	private static final String  TABLE_NAME = "AnnotationTable";
	private static final boolean VERBOSE = true;

	private AnnotationIO _io;
	private AnnotationIndexer<JSONObject> _indexer;
	private AnnotationSerializer<JSONObject> _serializer;
    
	private List<AnnotationBin<JSONObject>> _annotations;
	private AnnotationIndex _start;
	private AnnotationIndex _stop;
	
    @Before
    public void setup () {
    	try {
    		_io = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
		}	
    	
    	_indexer = new TiledAnnotationIndexer();
    	_serializer = new TestJSONSerializer(); 
    	
    	_annotations = generateJSONAnnotations( NUM_ENTRIES, _indexer );
    	
        _start = generateJSONAnnotation( _indexer ).getIndex();
    	_stop = generateJSONAnnotation( _indexer ).getIndex();
    	
    	if ( _stop.getValue() < _start.getValue() ) {
    		AnnotationIndex temp = _start;
    		_start = _stop;
    		_stop = temp;
    	}
	
    }

    @After
    public void teardown () {
    	_io = null;
    }
	
	
    @Test
    public void testHBaseIO() {
    	
    	try {
    		
	        /*
	    	 *  Create Table
	    	 */
			System.out.println("Creating table");
	    	_io.initializeForWrite( TABLE_NAME );
	    	
	        /*
	    	 *  Write annotations
	    	 */ 	
	    	System.out.println("Writing "+NUM_ENTRIES+" to table");	
	    	_io.writeAnnotations(TABLE_NAME, _serializer, _annotations );
	
	        /*
	    	 *  Scan range annotations
	    	 */
	    	System.out.println( "Scanning table from " + _start.getValue() + " to " + _stop.getValue() );	
	    	List<AnnotationBin<JSONObject>> scannedEntries = _io.readAnnotations( TABLE_NAME, _serializer, _start, _stop );			
	        if (VERBOSE) print( scannedEntries );
	        
	    	/*
	    	 *  Scan and check all annotations
	    	 */
	    	System.out.println( "Scanning all annotations" );
	    	List<AnnotationIndex> indices = convertToIndices( _annotations );
	    	List<AnnotationBin<JSONObject>> allEntries = _io.readAnnotations( TABLE_NAME, _serializer, indices );

	    	if (VERBOSE) print( allEntries );
	    	for ( AnnotationBin<JSONObject> annotation : _annotations ) {
	    		Assert.assertTrue( allEntries.contains( annotation ) );
	    		allEntries.remove( annotation );
	    	}
	
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
			/*
	    	 * Drop table
	    	 */
	    	System.out.println("Disabling and dropping table");
	    	((HBaseAnnotationIO)_io).dropTable(TABLE_NAME);
		}
    }


	
}
