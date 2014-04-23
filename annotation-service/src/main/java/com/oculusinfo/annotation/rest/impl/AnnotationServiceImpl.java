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


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.binning.*;


@Singleton
public class AnnotationServiceImpl implements AnnotationService {

	protected AnnotationIO _io;
	protected AnnotationSerializer<AnnotationTile> _tileSerializer;
	protected AnnotationSerializer<AnnotationData<?>> _dataSerializer; 
	protected AnnotationIndexer _indexer;
	protected ConcurrentHashMap< UUID, Map<String, Integer> > _uuidFilterMap;
	
	protected final ReadWriteLock _lock = new ReentrantReadWriteLock();

	@Inject
	public AnnotationServiceImpl( AnnotationIO io, AnnotationIndexer indexer ) {
		
		_tileSerializer = new JSONTileSerializer();
		_dataSerializer = new JSONDataSerializer();
		_uuidFilterMap =  new ConcurrentHashMap<>();
		_indexer = indexer;
		_io = io;
	}
	

	public void writeAnnotation( String layer, AnnotationData<?> annotation ) throws IllegalArgumentException {
		
    	_lock.writeLock().lock();
    	try {
    		
    		// check in case client generated UUID results in IO collision
    		if ( checkForCollision( layer, annotation ) ) {
    			throw new IllegalArgumentException("UUID for data results in collision, WRITE operation aborted");
    		}
    		
    		addDataToTiles( layer, annotation );
    		
    	} finally {
    		_lock.writeLock().unlock();
    	}
	}

	
	public void modifyAnnotation( String layer, 
								  AnnotationData<?> oldAnnotation, 
								  AnnotationData<?> newAnnotation ) throws IllegalArgumentException {
		
		// temporary naive modification, remove old, write new		
		_lock.writeLock().lock();
    	try {
		
    		// ensure request is coherent with server state
    		if ( isRequestOutOfDate( layer, oldAnnotation ) ) {
    			throw new IllegalArgumentException("Client is out of sync with Server, "
    											 + "MODIFY operation aborted. It is recommended "
    											 + "upon receiving this exception to refresh all client annotations");        		
    		}
    		
			/* Technically, you should not have to re-tile the annotation if
			 * there is only a content change, as it will stay in the same tiles,
			 * however, since we want to update the reference time-stamp on a content
			 * change, it is re-tile so that we can filter from tiles without reading
			 * the annotations themselves
			 */
			// remove from old tiles
			removeDataFromTiles( layer, oldAnnotation );
			// add it to new tiles
			addDataToTiles( layer, newAnnotation );
			
    	} finally {
    		_lock.writeLock().unlock();
    	}
	}
	
	

	public Map<BinIndex, List<AnnotationData<?>>> readAnnotations( UUID id, String layer, TileIndex query ) {
		
		Map<String, Integer> filters = null;
		
		if ( id != null ) {
			filters = _uuidFilterMap.get( id );
		}
		
		_lock.readLock().lock();
    	try {  		
    		return getDataFromTiles( layer, query, filters );    		
    	} finally { 		
    		_lock.readLock().unlock();
    	}
	}
	
		
	public void removeAnnotation( String layer, AnnotationData<?> annotation ) throws IllegalArgumentException {

		_lock.writeLock().lock();		
		try {
			
			// ensure request is coherent with server state
    		if ( isRequestOutOfDate( layer, annotation ) ) {
    			throw new IllegalArgumentException("Client is out of sync with Server, "
												 + "REMOVE operation aborted. It is recommended "
												 + "upon receiving this exception to refresh all client annotations");       		
    		}
			
			removeDataFromTiles( layer, annotation );
			removeDataFromIO( layer, annotation );
			
		} finally {
			_lock.writeLock().unlock();
		}
		
	}

	
	public void setFilter( UUID id, String layer, Map<String, Integer> filter ) {
		_uuidFilterMap.put( id, filter );
	}
	
	
	/*
	 * 
	 * Helper methods
	 * 
	 */	
	
	/*
	 * Check data UUID in IO, if already exists, return true
	 */
	private boolean checkForCollision( String layer, AnnotationData<?> annotation ) {
		
		List<AnnotationReference> reference = new LinkedList<>();
		reference.add( annotation.getReference() );
		return ( readDataFromIO( layer, reference ).size() > 0 ) ;

	}
	
	/*
	 * Check data timestamp from clients source, if out of date, return true
	 */
	public boolean isRequestOutOfDate( String layer, AnnotationData<?> annotation ) {
		
		List<AnnotationReference> reference = new LinkedList<>();
		reference.add( annotation.getReference() );
		List<AnnotationData<?>> annotations = readDataFromIO( layer, reference );
		
		if ( annotations.size() == 0 ) {
			// removed since client update, abort
			return true;
		}
		
		if ( !annotations.get(0).getTimeStamp().equals( annotation.getTimeStamp() ) ) {
			// clients timestamp doesn't not match most up to date, abort
			return true;
		}
		
		// everything seems to be in order
		return false;
	}
	
	
	/*
	 * Iterate through all indices, find matching tiles and add data reference, if tile
	 * is missing, add it
	 */
	private void addDataReferenceToTiles( List<AnnotationTile> tiles, List<TileAndBinIndices> indices, AnnotationData<?> data ) {		
		
    	for ( TileAndBinIndices index : indices ) {			
			// check all existing tiles for matching index
    		boolean found = false;
			for ( AnnotationTile tile : tiles ) {				
				if ( tile.getIndex().equals( index.getTile() ) ) {
					// tile exists already, add data to bin
					tile.add( index.getBin(), data );
					found = true;
					break;
				} 
			}
			if ( !found ) {
				// no tile exists, add tile
				tiles.add( new AnnotationTile( index.getTile(), new AnnotationBin( index.getBin(), data ) ) );	    	
			}
		}				
	}
	
	/*
	 * Iterate through all tiles, removing data reference from bins, any tiles with no bin entries
	 * are added to tileToRemove, the rest are added to tilesToWrite
	 */
	private void removeDataReferenceFromTiles( List<AnnotationTile> tilesToWrite, List<AnnotationTile> tilesToRemove, List<AnnotationTile> tiles, AnnotationData<?> data ) {		
		
		// clear supplied lists
		tilesToWrite.clear();
		tilesToRemove.clear();	
		
		// for each tile, remove data from bins
		for ( AnnotationTile tile : tiles ) {				
			// get bin index for the annotation in this tile
			BinIndex binIndex = _indexer.getIndex( data, tile.getIndex().getLevel() ).getBin();		
			// remove data from tile
			tile.remove( binIndex, data );				
		}	
		
		// determine which tiles need to be re-written and which need to be removed
		for ( AnnotationTile tile : tiles ) {			
			if ( tile.size() == 0 ) {				
				// if no data left, flag tile for removal
				tilesToRemove.add( tile );
			} else {
				// flag tile to be written
				tilesToWrite.add( tile );
			}
		}
	}
	
	/*
	 * convert a List<TileAndBinIndices> to List<TileIndex>
	 */
	private List<TileIndex> convert( List<TileAndBinIndices> tiles ) {
		
		List<TileIndex> indices = new ArrayList<>();
		for ( TileAndBinIndices tile : tiles ) {			
			indices.add( tile.getTile() );
		}
		
		return indices;
	}

	
	private Map<BinIndex, List<AnnotationData<?>>> getDataFromTiles( String layer, TileIndex tileIndex, Map<String, Integer> filter ) {
		
		// wrap index into list 
		List<TileIndex> indices = new LinkedList<>();
		indices.add( tileIndex );
			
		// get tiles
		List<AnnotationTile> tiles = readTilesFromIO( layer, indices );
				
		// for each tile, assemble list of all data references
		List<AnnotationReference> references = new LinkedList<>();
		for ( AnnotationTile tile : tiles ) {					
			if ( filter != null ) {
				// filter provided
				references.addAll( tile.getFilteredReferences( filter ) );
			} else {
				// no filter provided
				references.addAll( tile.getAllReferences() );
			}
		}
		
		// read data from io in bulk
		List<AnnotationData<?>> data = readDataFromIO( layer, references );

		// assemble data by bin
		Map<BinIndex, List<AnnotationData<?>>> dataByBin =  new HashMap<>();
		for ( AnnotationData<?> d : data ) {
			// get index 
			BinIndex binIndex = _indexer.getIndex( d, tileIndex.getLevel() ).getBin();
			if (!dataByBin.containsKey( binIndex)) {
				// no data under this bin, add list to map
				dataByBin.put( binIndex, new LinkedList<AnnotationData<?>>() );
			}
			// add data to list, under bin
			dataByBin.get( binIndex ).add( d );
		}
		return dataByBin;
	}

	
	private void addDataToTiles( String layer, AnnotationData<?> data ) {
		
		// get list of the indices for all levels
		List<TileAndBinIndices> indices = _indexer.getIndices( data );
		// get all affected tiles
		List<AnnotationTile> tiles = readTilesFromIO( layer, convert( indices ) );
		// add new data reference to tiles
    	addDataReferenceToTiles( tiles, indices, data );
		// write tiles back to io
		writeTilesToIO( layer, tiles );    		
		// write data to io
		writeDataToIO( layer, data );

	}
	
	
	private void removeDataFromTiles( String layer, AnnotationData<?> data ) {
		
		// get list of the indices for all levels
    	List<TileAndBinIndices> indices = _indexer.getIndices( data );	    	
		// read existing tiles
		List<AnnotationTile> tiles = readTilesFromIO( layer, convert( indices ) );					
		// maintain lists of what bins to modify and what bins to remove
		List<AnnotationTile> tilesToWrite = new LinkedList<>(); 
		List<AnnotationTile> tilesToRemove = new LinkedList<>();			
		// remove data from tiles and organize into lists to write and remove
		removeDataReferenceFromTiles( tilesToWrite, tilesToRemove, tiles, data );
		// write modified tiles
		writeTilesToIO( layer, tilesToWrite );		
		// remove empty tiles and data
		removeTilesFromIO( layer, tilesToRemove );
	}
	
	
	protected void writeTilesToIO( String layer, List<AnnotationTile> tiles ) {
		
		if ( tiles.size() == 0 ) return;
		
		try {

			_io.initializeForWrite( layer );
			_io.writeTiles( layer, _tileSerializer, tiles );
					
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
	}
	
	
	protected void writeDataToIO( String layer, AnnotationData<?> data ) {
		
		List<AnnotationData<?>> dataList = new LinkedList<>();
		dataList.add( data );
		
		try {
			
			_io.initializeForWrite( layer );		
			_io.writeData( layer, _dataSerializer, dataList );

		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	

	protected void removeTilesFromIO( String layer, List<AnnotationTile> tiles ) {

		if ( tiles.size() == 0 ) return;
		
		try {		

			_io.initializeForRemove( layer );		
			_io.removeTiles( layer, tiles );	
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}

	}
	
	
	protected void removeDataFromIO( String layer, AnnotationData<?> data ) {
		
		List<AnnotationData<?>> dataList = new LinkedList<>();
		dataList.add( data );
		
		try {
			
			_io.initializeForRemove( layer );	
			_io.removeData( layer, dataList );											
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}

	}
	
	
	protected List<AnnotationTile> readTilesFromIO( String layer, List<TileIndex> indices ) {
			
		List<AnnotationTile> tiles = new LinkedList<>();
		
		if ( indices.size() == 0 ) return tiles;
		
		try {
			
			_io.initializeForRead( layer );		
			tiles = _io.readTiles( layer, _tileSerializer, indices );						
					
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		return tiles;		
	}
	
	protected List<AnnotationData<?>> readDataFromIO( String layer, List<AnnotationReference> references ) {
		
		List<AnnotationData<?>> data = new LinkedList<>();
		
		if ( references.size() == 0 ) return data;
		
		try {
			
			_io.initializeForRead( layer );	
			data = _io.readData( layer, _dataSerializer, references );			
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		return data;
	}
	
}