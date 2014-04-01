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
import java.util.Map;
import java.util.concurrent.locks.*;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;


@Singleton
public class AnnotationServiceImpl implements AnnotationService {

	//protected static final String TABLE_NAME = "AnnotationTable";	
	protected AnnotationIO _io;
	protected AnnotationSerializer<AnnotationTile> _tileSerializer;
	protected AnnotationSerializer<AnnotationData> _dataSerializer; 
	protected AnnotationIndexer<TileAndBinIndices> _indexer;

	protected final ReadWriteLock _lock = new ReentrantReadWriteLock();

	@Inject
	public AnnotationServiceImpl( AnnotationIO io, AnnotationIndexer indexer ) {
		
		_tileSerializer = new JSONTileSerializer();
		_dataSerializer = new JSONDataSerializer();	
		_indexer = indexer;
		_io = io;
		/*
		try {

			System.out.println( "Dropping previous table");
			((HBaseAnnotationIO)_io).dropTable(TABLE_NAME);
			_io.initializeForWrite( TABLE_NAME );
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
	}
	
		
	public void writeAnnotation( String layer, AnnotationData data ) {
		
		// get list of the indices for all levels
    	List<TileAndBinIndices> indices = _indexer.getIndices( data );
    	   	
    	_lock.writeLock().lock();
    	try {
    		
    		// get all affected tiles
    		List<AnnotationTile> tiles = getTiles( layer, convert( indices ) );
    		// add new data reference to tiles
        	addDataReferenceToTiles( tiles, indices, data );
    		// write tiles back to io
    		writeTilesToIO( layer, tiles );    		
    		// write data to io
    		writeDataToIO( layer, data );
    		
    	} finally {
    		_lock.writeLock().unlock();
    	}
	}
	

	public List<AnnotationData> readAnnotations( String layer, TileIndex query ) {
		
		_lock.readLock().lock();
    	try {
    		
    		return getData( layer, addUnivariateIndices( query ) );
    		
    	} finally { 		
    		_lock.readLock().unlock();
    	}
	}
	
		
	public void removeAnnotation( String layer, AnnotationData annotation ) {

		_lock.writeLock().lock();		
		try {
			
			// get list of the indices for all levels
	    	List<TileAndBinIndices> indices = _indexer.getIndices( annotation );	    	
			
	    	// maintain lists of what bins to modify and what bins to remove
			List<AnnotationTile> tilesToWrite = new LinkedList<>();
			List<AnnotationTile> tilesToRemove = new LinkedList<>();		
			
			// read existing tiles
			List<AnnotationTile> tiles = getTiles( layer, convert( indices ) );
							
			for ( AnnotationTile tile : tiles ) {	
				
				// get bin index for the annotation in this tile
				BinIndex binIndex = _indexer.getIndex( annotation, tile.getIndex().getLevel() ).getBin();
				
				if ( tile.remove( binIndex, annotation ) ) {					
					// if successfully removed data from this bin, flag to write change
					tilesToWrite.add( tile );
				}
				if ( tile.size() == 0 ) {				
					// if no data left, flag bin for removal
					tilesToRemove.add( tile );
					// no longer have to write this bin back
					tilesToWrite.remove( tile );
				}
			}	

			// write modified tiles
			writeTilesToIO( layer, tilesToWrite );	
			
			// remove empty tiles and data
			removeTilesFromIO( layer, tilesToRemove );
			removeDataFromIO( layer, annotation );
			
		} finally {
			_lock.writeLock().unlock();
		}
		
	}

	
	/*
	 * 
	 * Helper methods
	 * 
	 */
	private void addDataReferenceToTiles( List<AnnotationTile> tiles, List<TileAndBinIndices> indices, AnnotationData data ) {		
		
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
	
	private List<TileIndex> convert( List<TileAndBinIndices> tiles ) {
		
		List<TileIndex> indices = new ArrayList<>();
		for ( TileAndBinIndices tile : tiles ) {			
			indices.add( tile.getTile() );
		}
		
		return indices;
	}
	
	
	private List<TileIndex> addUnivariateIndices( TileIndex tile ) {		
		
		List<TileIndex> tiles = new LinkedList<>();
		tiles.add( tile );
		tiles.add( new TileIndex( tile.getLevel(), tile.getX(), -1 ) );
		tiles.add( new TileIndex( tile.getLevel(), -1, tile.getY() ) );
		return tiles;
	}

	
	private List<AnnotationTile> getTiles( String layer, List<TileIndex> indices ) {
		
		return readTilesFromIO( layer, indices );	
	}
	
	
	private List<AnnotationData> getData( String layer, List<TileIndex> indices ) {
		
		List<AnnotationTile> tiles = getTiles( layer, indices );
		List<Long> references = new LinkedList<>();
		
		// for each tile, assemble list of all data references
		for ( AnnotationTile tile : tiles ) {
			references.addAll( tile.getAllReferences() );
		}
		return readDataFromIO( layer, references );

	}
		
	private List<AnnotationData> getData( String layer, List<TileIndex> indices, Map<String, Integer> filter ) {
					
		// get all required tiles
		List<AnnotationTile> tiles = getTiles( layer, indices );
		
		// get filtered references from tiles
		List<Long> references = new LinkedList<>();			
		for ( AnnotationTile tile : tiles ) {
			references.addAll( tile.getFilteredReferences( filter ) );
		}
		return readDataFromIO( layer, references );
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
	
	
	protected void writeDataToIO( String layer, AnnotationData data ) {
		
		List<AnnotationData> dataList = new LinkedList<>();
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
	
	
	protected void removeDataFromIO( String layer, AnnotationData data ) {
		
		List<AnnotationData> dataList = new LinkedList<>();
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
	
	
	protected List<AnnotationData> readDataFromIO( String layer, List<Long> indices ) {
		
		List<AnnotationData> data = new LinkedList<>();
		
		if ( indices.size() == 0 ) return data;
		
		try {
			
			_io.initializeForRead( layer );	
			data = _io.readData( layer, _dataSerializer, indices );			
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		return data;		
	}
	
	
}