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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

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


public class TiledAnnotationService implements AnnotationService<JSONObject> {
	
	static final String   TABLE_NAME = "AnnotationTable";
	
	private AnnotationIO _io;
	private AnnotationIndexer<JSONObject> _indexer;   
    private AnnotationSerializer<JSONObject> _serializer;
	
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
		
		((HBaseAnnotationIO)_io).dropTable( TABLE_NAME );
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
			List<AnnotationIndex> indices = _indexer.getIndices( annotation );
		
			_io.initializeForRead( TABLE_NAME );
			
			// read all these bins into memory
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, indices );
						
			for ( AnnotationIndex index : indices ) {
				 int i = bins.indexOf( index );
				 
				 if ( i != -1) {
					 // bin exists, add data to it
					 bins.get(i).add( annotation );
				 } else {
					 // bin does not exist, create
					 bins.add( new AnnotationBin<JSONObject>( index, annotation ) );
				 }
			}
			
			// write bins back to io
			_io.writeAnnotations( TABLE_NAME, _serializer, bins );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	public void writeAnnotations( List<JSONObject> annotations ) {
	
		try {
			
			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
			
			// get all affected bin indices, no duplicates
			Set<AnnotationIndex> allIndices = new LinkedHashSet<>();
			// find mapping from annotation data to bin index
			Map<JSONObject, List<AnnotationIndex>> indexMap = new HashMap<>();
			
			for (JSONObject annotation : annotations) {
				// get list of the indices for all levels
				List<AnnotationIndex> indices = _indexer.getIndices( annotation );
				allIndices.addAll( indices );
				indexMap.put( annotation, indices );
			}
			
			// read all these bins into memory
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, new ArrayList<>( allIndices ) );
						
			for (Map.Entry<JSONObject, List<AnnotationIndex>> entry : indexMap.entrySet()) {
				JSONObject data = entry.getKey();
				List<AnnotationIndex> indices = entry.getValue();

				// for all bins that this data node will be written into
				for ( AnnotationIndex index : indices ) {
					
					 int j = bins.indexOf( index );
					 
					 if ( j != -1) {
						 // bin exists, add data to it
						 bins.get(j).add( data );
					 } else {
						 // bin does not exist, create
						 bins.add( new AnnotationBin<JSONObject>(index, data) );
					 }
				}
			}
						
			// write bins back to io
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
			// get list of the indices for all levels
			List<AnnotationIndex> indices = _indexer.getIndices( annotation );
			
			// maintain lists of what bins to modify and what bins to remove
			List<AnnotationBin<JSONObject>> toWrite = new LinkedList<AnnotationBin<JSONObject>>();
			List<AnnotationIndex> toRemove = new LinkedList<AnnotationIndex>();
			
			// read existing bins
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationBin<JSONObject>> annotations = _io.readAnnotations( TABLE_NAME, _serializer, indices );		

			for (AnnotationBin<JSONObject> existing : annotations) {
				if ( existing.remove( annotation ) ) {
					// if successfully removed data from this bin, flag to write change
					toWrite.add( existing );
				}
				if ( existing.getData().size() == 0 ) {
					// if no data left, flag bin for removal
					toRemove.add( existing.getIndex() );
					// no longer have to write this bin back
					toWrite.remove( existing );
				}
			}	
			
			// write modified bins
			_io.initializeForWrite( TABLE_NAME );
			_io.writeAnnotations( TABLE_NAME, _serializer, toWrite );
			
			// remove empty bins
			_io.initializeForRemove( TABLE_NAME );
			_io.removeAnnotations( TABLE_NAME, toRemove );
			
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

			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
			
			// get all affected bin indices, no duplicates
			Set<AnnotationIndex> allIndices = new LinkedHashSet<>();
			// find mapping from annotation data to bin index
			Map<JSONObject, List<AnnotationIndex>> indexMap = new HashMap<>();
			
			for (JSONObject annotation : annotations) {
				// get list of the indices for all levels
				List<AnnotationIndex> indices = _indexer.getIndices( annotation );
				allIndices.addAll( indices );
				indexMap.put( annotation, indices );
			}

			// read all these bins into memory
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, new ArrayList<>( allIndices ) );
			
			// maintain lists of what bins to modify and what bins to remove
			Set<AnnotationBin<JSONObject>> toWrite = new LinkedHashSet<AnnotationBin<JSONObject>>();
			Set<AnnotationIndex> toRemove = new LinkedHashSet<AnnotationIndex>();
			
			for (Map.Entry<JSONObject, List<AnnotationIndex>> entry : indexMap.entrySet()) {
				JSONObject data = entry.getKey();
				List<AnnotationIndex> indices = entry.getValue();

				// for all bins that this data node will be written into
				for ( AnnotationIndex index : indices ) {
					
					int j = bins.indexOf( index );
					
					if ( j != -1) {					 
						// bin exists
						AnnotationBin<JSONObject> bin = bins.get(j);
						// remove data entry from bin
						if ( bin.remove( data ) ) {
							// if successfully removed data from this bin, flag to write change
							toWrite.add( bin );
						}
						if ( bin.getData().size() == 0 ) {
							// no data left, flag bin for removal
							toRemove.add( bin.getIndex() );
							// no longer have to write this bin back
							toWrite.remove( bin );
						}
					}
				}
	
			}
			
			// write modified bins
			_io.initializeForWrite( TABLE_NAME );
			_io.writeAnnotations( TABLE_NAME, _serializer, new ArrayList<>( toWrite ) );
			
			// remove empty bins
			_io.initializeForRemove( TABLE_NAME );
			_io.removeAnnotations( TABLE_NAME, new ArrayList<>( toRemove ) );
	
		} catch (Exception e) { 
			e.printStackTrace();  
		}
	}
}
