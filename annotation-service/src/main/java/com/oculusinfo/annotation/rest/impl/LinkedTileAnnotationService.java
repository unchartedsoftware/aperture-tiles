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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class LinkedTileAnnotationService implements AnnotationService<JSONObject> {
	
	static final String   TABLE_NAME = "AnnotationTable";
	
	AnnotationIO _io;
    AnnotationIndexer<JSONObject> _indexer;
    AnnotationIndexer<JSONObject> _dataIndexer; 
    AnnotationSerializer<JSONObject> _serializer;

    
	public LinkedTileAnnotationService() {
		
		_indexer = new TiledAnnotationIndexer();
		_dataIndexer = new FlatAnnotationIndexer();
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

	
	private AnnotationBin<JSONObject> getDataBin( JSONObject data ){
		AnnotationIndex dataIndex = new AnnotationIndex( -_dataIndexer.getIndex( data, 0 ).getValue() -1 );
		return new AnnotationBin<JSONObject>( dataIndex, data );	
	}
	
	private JSONObject getDataReference( AnnotationIndex index ) {
		
		try {
			JSONObject dataReference = new JSONObject();
			dataReference.put("key", index.getValue() );
			return dataReference;
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
		return null;
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
		
			// get index of data, create entry for data			
			AnnotationBin<JSONObject> dataObject = getDataBin( annotation );
			
			// create reference object
			JSONObject dataReference = getDataReference( dataObject.getIndex() );

			_io.initializeForRead( TABLE_NAME );
			
			// read all these bins into memory
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, indices );
						
			for ( AnnotationIndex index : indices ) {
				 int i = bins.indexOf( index );
				 
				 if ( i != -1) {
					 // bin exists, add data to it
					 bins.get(i).add( dataReference );

				 } else {
					 // bin does not exist, create					 					 					 
					 bins.add( new AnnotationBin<JSONObject>( index, dataReference ) );
				 }
			}
			
			// write bins back to io
			_io.writeAnnotations( TABLE_NAME, _serializer, bins );
			
			List<AnnotationBin<JSONObject>> dataEntry = new LinkedList<AnnotationBin<JSONObject>>();
			dataEntry.add( dataObject );
			
			// write data to io
			_io.writeAnnotations( TABLE_NAME, _serializer, dataEntry );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	public void writeAnnotations( List<JSONObject> annotations ) {
	
		try {
			
			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
						
			// get index of data, create entry for data			
			List<AnnotationBin<JSONObject>> dataObjects = new LinkedList<AnnotationBin<JSONObject>>();
			List<JSONObject> dataReferences = new LinkedList<JSONObject>();
			
			// get all affected bin indices, no duplicates
			Set<AnnotationIndex> allIndices = new LinkedHashSet<>();
			
			// find mapping from annotation data to bin index
			Map<JSONObject, List<AnnotationIndex>> indexMap = new HashMap<>();
			
			for (JSONObject annotation : annotations) {
				// get list of the indices for all levels
				List<AnnotationIndex> indices = _indexer.getIndices( annotation );
				
				AnnotationBin<JSONObject> dataObject =  getDataBin( annotation );
				dataObjects.add( dataObject );
				JSONObject dataReference = getDataReference( dataObject.getIndex() );
				dataReferences.add( dataReference );
				
				allIndices.addAll( indices );
				indexMap.put( dataReference, indices );
			}
			
			// read all these bins into memory
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, new ArrayList<>( allIndices ) );
						
			for (Map.Entry<JSONObject, List<AnnotationIndex>> entry : indexMap.entrySet()) {
				JSONObject dataReference = entry.getKey();
				List<AnnotationIndex> indices = entry.getValue();

				// for all bins that this data node will be written into
				for ( AnnotationIndex index : indices ) {
					
					 int j = bins.indexOf( index );
					 
					 if ( j != -1) {
						 // bin exists, add data to it
						 bins.get(j).add( dataReference );
					 } else {
						 // bin does not exist, create
						 bins.add( new AnnotationBin<JSONObject>( index, dataReference ) );
					 }
				}
			}
						
			// write bins back to io
			_io.writeAnnotations( TABLE_NAME, _serializer, bins );
			
			// write data back to io
			_io.writeAnnotations( TABLE_NAME, _serializer, dataObjects );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	
	/*
	 * Read annotations from the storage service
	 * 
	 */
	public List<AnnotationBin<JSONObject>> readAnnotations( JSONObject start, JSONObject stop, int level ) {
		
		int MAX_ENTRIES_PER_BIN = 10;
		
		AnnotationIndex to = _indexer.getIndex( start, level );
		AnnotationIndex from = _indexer.getIndex( stop, level );
		
		try {		
			
			// read all bins in range
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationBin<JSONObject>> bins = _io.readAnnotations( TABLE_NAME, _serializer, to, from );
			
			// for each bin, read in all data entries
			List<AnnotationBin<JSONObject>> results = new LinkedList<AnnotationBin<JSONObject>>();			
			for (AnnotationBin<JSONObject> bin : bins) {
				
				List<AnnotationIndex> indices = new LinkedList<AnnotationIndex>();
				for (int i=0; i<MAX_ENTRIES_PER_BIN && i<bin.getData().size(); i++) {
					indices.add( new AnnotationIndex( bin.getData().get(i).getLong("key") ) );
				}
				results.addAll( _io.readAnnotations( TABLE_NAME, _serializer, indices ) );
				
			}
			System.out.println( results.size() + " total data entries read from " + bins.size() + " bins" );
			
			return results;
			
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
			
			// get index of data, create entry for data
			AnnotationIndex dataIndex = getDataBin( annotation ).getIndex(); 
			
			// create reference object
			JSONObject dataReference = getDataReference( dataIndex );
						
			// maintain lists of what bins to modify and what bins to remove
			List<AnnotationBin<JSONObject>> toWrite = new LinkedList<AnnotationBin<JSONObject>>();
			List<AnnotationIndex> toRemove = new LinkedList<AnnotationIndex>();
			toRemove.add( dataIndex ); // remove data entry
			
			// read existing bins
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationBin<JSONObject>> annotations = _io.readAnnotations( TABLE_NAME, _serializer, indices );		

			for (AnnotationBin<JSONObject> existing : annotations) {
				if ( existing.remove( dataReference ) ) {
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
