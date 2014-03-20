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
package com.oculusinfo.annotation.rest.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.query.*;
import com.oculusinfo.annotation.index.impl.*;

import org.json.JSONException;
import org.json.JSONObject;


public class FlatAnnotationService implements AnnotationService<JSONObject> {
	
	static final String   TABLE_NAME = "AnnotationTable";
	
	AnnotationIO _io;
    AnnotationIndexer<JSONObject> _indexer;   
    AnnotationSerializer<JSONObject> _serializer;
	
	public FlatAnnotationService() {
		
		_indexer = new FlatAnnotationIndexer();
		_serializer = new TestJSONSerializer();
		
		try {
			_io = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		((HBaseAnnotationIO)_io).dropTable( TABLE_NAME );
	}

	
	/*
	 * Write an annotation to the storage service
	 * 
	 */
	@Override
	public void writeAnnotation( JSONObject annotation ) {
		
		try {
			
			// assemble annotations into bin
			List<AnnotationBin<JSONObject>> bin = new LinkedList<AnnotationBin<JSONObject>>(); 
			bin.add( new AnnotationBin<JSONObject>( _indexer.getIndex( annotation, 0 ), annotation ) );
			
			// write bins back to io
			_io.initializeForWrite( TABLE_NAME );
			_io.writeAnnotations( TABLE_NAME, _serializer, bin );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	public void writeAnnotations( List<JSONObject> annotations ) {
	
		try {
			// assemble annotations into bins
			List<AnnotationBin<JSONObject>> bins = new LinkedList<AnnotationBin<JSONObject>>(); 
			
			for (JSONObject annotation : annotations) {
				bins.add( new AnnotationBin<JSONObject>( _indexer.getIndex( annotation, 0 ), annotation ) );
			}
			
			// write bins back to io
			_io.initializeForWrite( TABLE_NAME );
			_io.writeAnnotations( TABLE_NAME, _serializer, bins );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	
	/*
	 * Read annotations from the storage service
	 * 
	 */
	public List<AnnotationBin<JSONObject>> readAnnotations( JSONObject start, JSONObject stop, int level ) {
		
		AnnotationIndex to = _indexer.getIndex( start, level );
		AnnotationIndex from = _indexer.getIndex( stop, level );
		
		try {			
			_io.initializeForRead( TABLE_NAME );
			return _io.readAnnotations( TABLE_NAME, _serializer, to, from );		
		} catch (Exception e) { 
			System.out.println( e.getMessage() );
		}		
		return null;
		
	}
	

	/*
	 * Remove an annotation from the storage service
	 * 
	 */
	public void removeAnnotation( JSONObject annotation ) {
		
		try {
			// assemble annotations into bin
			List<AnnotationIndex> indices = new LinkedList<AnnotationIndex>(); 
			indices.add( _indexer.getIndex( annotation, 0 ) );

			// remove empty bins
			_io.initializeForRemove( TABLE_NAME );
			_io.removeAnnotations( TABLE_NAME, indices );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}


	/*
	 * Remove an annotation from the storage service
	 * 
	 */
	public void removeAnnotations( List<JSONObject> annotations ) {
		
		try {

			// assemble annotations into bins
			List<AnnotationIndex> indices = new LinkedList<AnnotationIndex>(); 
			
			for (JSONObject annotation : annotations) {
				indices.add( _indexer.getIndex( annotation, 0 ) );
			}
			
			// remove empty bins
			_io.initializeForRemove( TABLE_NAME );
			_io.removeAnnotations( TABLE_NAME, indices );
			
	
		} catch (Exception e) { 
			e.printStackTrace();  
		}
	}
}
