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
import java.util.ListIterator;

import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.impl.*;
import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;


import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;

public class JSONAnnotationService implements AnnotationService {
	
	private static final String TABLE_NAME = "AnnotationTable";
	
	private AnnotationIO _io;
    private AnnotationSerializer<AnnotationTile> _tileSerializer;
    private AnnotationSerializer<AnnotationData> _dataSerializer; 
    private AnnotationIndexer<TileAndBinIndices> _indexer;
    private TilePyramid _pyramid;
    
    
	public JSONAnnotationService() {
		
		_tileSerializer = new JSONTileSerializer();
		_dataSerializer = new JSONDataSerializer();
		
		_pyramid = new WebMercatorTilePyramid();		
		_indexer = new TileAnnotationIndexer( _pyramid );
		
		try {
			_io = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		((HBaseAnnotationIO)_io).dropTable( TABLE_NAME );
	}


	private int indexOf( List<AnnotationTile> tiles, TileIndex index ) {
		int i = 0;
		for ( AnnotationTile tile : tiles ) {
			if (tile.getIndex().equals( index )) {
				return i;
			}
			i++;
		}
		return -1;
	}
	/*
	 * Write an annotation to the storage service
	 * 
	 */
	@Override
	public void writeAnnotation( AnnotationData annotation ) {
		
		try {
			
			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
			
			// get list of the indices for all levels
	    	List<TileAndBinIndices> indices = _indexer.getIndices( annotation );
	    	
			// read all these bins into memory
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationTile> tiles = _io.readTiles( TABLE_NAME, _tileSerializer, convert( indices ) );
						
			for ( TileAndBinIndices index : indices ) {
				
				 int i = indexOf( tiles, index.getTile() );
				 
				 if ( i != -1) {
					 // tile exists, add data to it
					 tiles.get(i).add( index.getBin(), annotation );
				 } else {
					 // tile does not exist, create	it
					 tiles.add( new AnnotationTile( index.getTile(), new AnnotationBin( index.getBin(), annotation ) ) );
				 }
			}
					
			// write tiles back to io
			_io.writeTiles( TABLE_NAME, _tileSerializer, tiles );

			// write data to io
			List<AnnotationData> data = new LinkedList<>();
			data.add( annotation );
			_io.writeData( TABLE_NAME, _dataSerializer, data );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	/*
	public void writeAnnotations( List<T> annotations ) {
	
		try {
			
			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
						
			// get index of data, create entry for data			
			List<AnnotationBin<T>> dataObjects = new LinkedList<AnnotationBin<T>>();
			List<T> dataReferences = new LinkedList<T>();
			
			// get all affected bin indices, no duplicates
			Set<AnnotationIndex> allIndices = new LinkedHashSet<>();
			
			// find mapping from annotation data to bin index
			Map<T, List<AnnotationIndex>> indexMap = new HashMap<>();
			
			for (T annotation : annotations) {
				// get list of the indices for all levels
				List<AnnotationIndex> indices = _indexer.getIndices( annotation );
				
				AnnotationBin<T> dataObject =  getDataBin( annotation );
				dataObjects.add( dataObject );
				T dataReference = getDataReference( dataObject.getIndex() );
				dataReferences.add( dataReference );
				
				allIndices.addAll( indices );
				indexMap.put( dataReference, indices );
			}
			
			// read all these bins into memory
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationBin<T>> bins = _io.readAnnotations( TABLE_NAME, _serializer, new ArrayList<>( allIndices ) );
						
			for (Map.Entry<T, List<AnnotationIndex>> entry : indexMap.entrySet()) {
				T dataReference = entry.getKey();
				List<AnnotationIndex> indices = entry.getValue();

				// for all bins that this data node will be written into
				for ( AnnotationIndex index : indices ) {
					
					 int j = bins.indexOf( index );
					 
					 if ( j != -1) {
						 // bin exists, add data to it
						 bins.get(j).add( dataReference );
					 } else {
						 // bin does not exist, create
						 bins.add( new AnnotationBin<T>( index, dataReference ) );
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
	*/
	
	
	private List<TileIndex> convert( List<TileAndBinIndices> tiles ) {
				
		List<TileIndex> indices = new ArrayList<>();
		for ( TileAndBinIndices tile : tiles ) {			
			indices.add( tile.getTile() );
		}
		
		return indices;
	}
	
	private List<TileIndex> addUnivariateIndices( List<TileIndex> tiles ) {		
		
		Map<String, Boolean> rangeCheck = new HashMap<>();
		
		ListIterator<TileIndex> iter = tiles.listIterator();
		
		while( iter.hasNext() ) {
			
			TileIndex tile = iter.next();
			
			if ( !rangeCheck.containsKey( "x"+tile.getX() ) ) {
				System.out.println( "x"+tile.getX());
				rangeCheck.put( "x"+tile.getX(),  true );
				iter.add( new TileIndex( tile.getLevel(), tile.getX(), -1) );
			}
			
			if ( !rangeCheck.containsKey( "y"+tile.getY() ) ) {
				System.out.println( "y"+tile.getY());
				rangeCheck.put( "y"+tile.getY(),  true );
				iter.add(  new TileIndex( tile.getLevel(), -1, tile.getY()) );
			}
		}

		return tiles;
	}
	
	/*
	 * Read annotations from the storage service
	 * 
	 */
	public List<AnnotationData> readAnnotation( TileIndex query ) {
		
		try {		
			
			List<TileIndex> indices = addUnivariateIndices( query );
			
			// read all bins in range
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationTile> tiles = _io.readTiles( TABLE_NAME, _tileSerializer, indices );
			
			// for each bin, read in all data entries
			List<AnnotationData> results = new LinkedList<>();
			
			// for each tile
			for ( AnnotationTile tile : tiles ) {
								
				// for each bin
				for (Map.Entry<BinIndex, AnnotationBin> binEntry : tile.getBins().entrySet() ) {
					
					BinIndex binKey = binEntry.getKey();
					AnnotationBin bin = binEntry.getValue();
					
					List<Long> dataReferences = new LinkedList<>();
					
					// for each reference
					for (Map.Entry<String, List<Long>> referenceEntry : bin.getReferences().entrySet() ) {
				    	
				    	String priority = referenceEntry.getKey();
				    	List<Long> references = referenceEntry.getValue();
				    	dataReferences.addAll( references );
				    	
					}
					
					results.addAll( _io.readData( TABLE_NAME, _dataSerializer, dataReferences ) );				
					
				}
				
				//System.out.println( "\t\t" + results.size() + " total data entries, read from " + tile.getBins().size() + " bins" );
			}
			
			return results;
			
		} catch (Exception e) { 
			e.printStackTrace();
		}		
		return null;
		
	}
	

	/*
	 * Remove an annotation from the storage service
	 * 
	 */
	public void removeAnnotation( AnnotationData annotation ) {
		
		try {
			// get list of the indices for all levels
	    	List<TileAndBinIndices> indices = _indexer.getIndices( annotation );	    	
	
			// maintain lists of what bins to modify and what bins to remove
			List<AnnotationTile> tilesToWrite = new LinkedList<>();
			List<AnnotationTile> tilesToRemove = new LinkedList<>();
			List<AnnotationData> dataToRemove = new LinkedList<>();
			dataToRemove.add( annotation ); // remove data entry
			
			// read existing bins
			_io.initializeForRead( TABLE_NAME );
			List<AnnotationTile> tiles = _io.readTiles( TABLE_NAME, _tileSerializer, convert( indices ) );		

			for ( AnnotationTile tile : tiles ) {
				if ( tile.remove( annotation ) ) {
					// if successfully removed data from this bin, flag to write change
					tilesToWrite.add( tile );
				}
				if ( tile.getBins().size() == 0 ) {
					// if no data left, flag bin for removal
					tilesToRemove.add( tile );
					// no longer have to write this bin back
					tilesToWrite.remove( tile );
				}
			}	
			
			// write modified bins
			_io.initializeForWrite( TABLE_NAME );
			_io.writeTiles( TABLE_NAME, _tileSerializer, tilesToWrite );
			
			// remove empty bins
			_io.initializeForRemove( TABLE_NAME );
			_io.removeTiles( TABLE_NAME, tilesToRemove );
			_io.removeData( TABLE_NAME, dataToRemove );
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}


	/*
	 * Remove an annotation from the storage service
	 * 
	 */
	/*
	public void removeAnnotations( List<T> annotations ) {
		
		try {
			// initialise for write
			_io.initializeForWrite( TABLE_NAME );
			
			// maintain lists of what bins to modify and what bins to remove
			Set<AnnotationBin<T>> toWrite = new LinkedHashSet<AnnotationBin<T>>();
			Set<AnnotationIndex> toRemove = new LinkedHashSet<AnnotationIndex>();
			
			// get all affected bin indices, no duplicates
			Set<AnnotationIndex> allIndices = new LinkedHashSet<>();
			// find mapping from annotation data to bin index
			Map<T, List<AnnotationIndex>> indexMap = new HashMap<>();
			
			for (T annotation : annotations) {
				
				// get list of the indices for all levels
				List<AnnotationIndex> indices = _indexer.getIndices( annotation );
				allIndices.addAll( indices );
				
				// get data index and reference
				AnnotationIndex dataIndex =  getDataBin( annotation ).getIndex();
				T dataReference = getDataReference( dataIndex );
				
				// flag data index for removal
				toRemove.add( dataIndex );
								
				indexMap.put( dataReference, indices );
			}

			// read all these bins into memory
			List<AnnotationBin<T>> bins = _io.readAnnotations( TABLE_NAME, _serializer, new ArrayList<>( allIndices ) );
			
			for (Map.Entry<T, List<AnnotationIndex>> entry : indexMap.entrySet()) {
				T dataReference = entry.getKey();
				List<AnnotationIndex> indices = entry.getValue();

				// for all bins that this data node will be written into
				for ( AnnotationIndex index : indices ) {
					
					int j = bins.indexOf( index );
					
					if ( j != -1) {					 
						// bin exists
						AnnotationBin<T> bin = bins.get(j);
						// remove data entry from bin
						if ( bin.remove( dataReference ) ) {
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
	*/
}
