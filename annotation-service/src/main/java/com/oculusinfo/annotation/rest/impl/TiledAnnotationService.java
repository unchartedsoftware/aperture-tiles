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
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;

import org.json.JSONException;
import org.json.JSONObject;


public class TiledAnnotationService implements AnnotationService<JSONObject> {
	
	static final String   TABLE_NAME = "AnnotationTable";
	
    AnnotationIndexer    _indexer;
    AnnotationIO 	     _io;
    AnnotationSerializer<JSONObject> _serializer;
	
	public TiledAnnotationService() {
		
		_indexer = new TiledAnnotationIndexer();
		_serializer = new TestJSONSerializer();
		
		try {
			_io = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//((HBaseAnnotationIO)_io).dropTable( TABLE_NAME );
	}

	
	/*
	 * Write an annotation to the storage service
	 * 
	 */
	@Override
	public void writeAnnotation( JSONObject annotation ) {
		
		try {
			
			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
			
			// get list of the indices for all levels
			List<AnnotationIndex> indices = _indexer.getIndices( annotation.getDouble("x"),
													             annotation.getDouble("y") );

			
			_io.initializeForRead( TABLE_NAME );
			
			// read all these bins into memory
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, indices );
			
			// add data to existing bins
			for ( AnnotationBin<JSONObject> bin : bins ) {
				bin.add( annotation );
			}
			
			// if bin is missing, create it
			for ( AnnotationIndex index : indices ) {
				boolean exists = false;
				for ( AnnotationBin<JSONObject> bin : bins ) {
					if ( bin.getIndex().equals( index ) ) {
						// exists
						exists = true;
						break;
					}
				}
				// bin does not exist for this level, create one
				if (!exists) {
					bins.add( new AnnotationBin<JSONObject>(index, annotation) );
				}
			}
			
			// write bins back to io
			_io.writeAnnotations( TABLE_NAME, _serializer, bins );
			
		} catch (Exception e) { 
			System.out.println( e.getMessage() ); 
		}
	}
	
	
	/*
	 * Read annotations from the storage service
	 * 
	 */
	public List<AnnotationBin<JSONObject>> readAnnotations( double xMin, double yMin,
															double xMax, double yMax, int level ) {
		
		AnnotationIndex start = _indexer.getIndex( xMin, yMin, level );
		AnnotationIndex stop = _indexer.getIndex( xMax, yMax, level );
		
		try {			
			_io.initializeForRead( TABLE_NAME );
			return _io.readAnnotations( TABLE_NAME, _serializer, start, stop );		
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
			// get list of the indices for all levels
			List<AnnotationIndex> indices = _indexer.getIndices( annotation.getDouble("x"),
													             annotation.getDouble("y") );
			
			// maintain lists of what bins to modify and what bins to remove
			List<AnnotationBin<JSONObject>> toWrite = new LinkedList<AnnotationBin<JSONObject>>();
			List<AnnotationIndex> toRemove = new LinkedList<AnnotationIndex>();
			
			// read existing bins
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationBin<JSONObject>> annotations = _io.readAnnotations( TABLE_NAME, _serializer, indices );		

			for (AnnotationBin<JSONObject> existing : annotations) {
				
				for (JSONObject data : existing.getData() ) {
					// check if data is held in bin
					if ( data.toString().equals( annotation.toString() ) ) {
						// remove data from bin
						existing.remove( annotation );
						toWrite.add( existing );
						break;
					}
				}
				if (existing.getData().size() == 0) {
					// not data left, remove entire bin
					toRemove.add( existing.getIndex() );
				}
			}	
			
			// write modified bins
			_io.initializeForWrite( TABLE_NAME );
			_io.writeAnnotations( TABLE_NAME, _serializer, toWrite );
			
			// remove empty bins
			_io.initializeForRemove( TABLE_NAME );
			_io.removeAnnotations( TABLE_NAME, toRemove );
			
		} catch (Exception e) { 
			System.out.println( e.getMessage() ); 
		}
	}


}
