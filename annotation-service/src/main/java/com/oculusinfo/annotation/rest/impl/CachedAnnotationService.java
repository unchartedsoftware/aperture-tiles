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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ListIterator;
import java.util.concurrent.locks.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;










//import com.google.inject.Singleton;
import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.impl.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.rest.AnnotationService;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;


//@Singleton
public class CachedAnnotationService implements AnnotationService {
	
	static final int MAX_MEMORY_USAGE_MB = 500;
	
	protected static final String TABLE_NAME = "AnnotationTable";
	
	protected AnnotationIO _io;
	protected AnnotationSerializer<AnnotationTile> _tileSerializer;
	protected AnnotationSerializer<AnnotationData> _dataSerializer; 
	protected AnnotationIndexer<TileAndBinIndices> _indexer;
	protected TilePyramid _pyramid;
		
	protected Map<TileIndex, AnnotationTile> _tileCache = new HashMap <>();
	protected Map<Long, AnnotationData>      _dataCache = new HashMap <>();
	
	private final ReadWriteLock _cacheLock = new ReentrantReadWriteLock();
	private final ReadWriteLock _hbaseLock = new ReentrantReadWriteLock();
		
	
	public CachedAnnotationService() {
		
		_tileSerializer = new JSONTileSerializer();
		_dataSerializer = new JSONDataSerializer();
		
		_pyramid = new WebMercatorTilePyramid();		
		_indexer = new TileAnnotationIndexer( _pyramid );
		
		try {
			_io = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
			System.out.println( "Dropping previous table");
			((HBaseAnnotationIO)_io).dropTable(TABLE_NAME);
			_io.initializeForWrite( TABLE_NAME );
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
		
	public void writeAnnotation( AnnotationData data ) {
		
		// get list of the indices for all levels
    	List<TileAndBinIndices> indices = _indexer.getIndices( data );
    	   	
    	// get all affected tiles
    	// start sync
    	List<AnnotationTile> tiles = getTiles( convert( indices ) );	/* synchronized */
    	
    	addDataReferenceToTiles( tiles, indices, data );				/* synchronized */
    	// end sync
    	
		// write tiles back to io
		writeTiles( tiles );	/* synchronized */
		
		// write data to io
		writeData( data );		/* synchronized */
    	
	}
	

	public List<AnnotationData> readAnnotation( TileIndex query ) {
	
		return getData( addUnivariateIndices( query ) ); /* synchronized */

	}
	
	
	
	public void removeAnnotation( AnnotationData annotation ) {

		try {
			// get list of the indices for all levels
	    	List<TileAndBinIndices> indices = _indexer.getIndices( annotation );	    	

			// maintain lists of what bins to modify and what bins to remove
			List<AnnotationTile> tilesToWrite = new LinkedList<>();
			List<AnnotationTile> tilesToRemove = new LinkedList<>();
			
			// read existing tiles
			List<AnnotationTile> tiles = getTiles( convert( indices ) ); 	/* synchronized */
			
			/////////
			_cacheLock.writeLock().lock();
			try {
				
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
			
			} finally {
				_cacheLock.writeLock().unlock();
			}
			/////////
			
			// write modified tiles
			writeTiles( tilesToWrite );		/* synchronized */
						
			// remove empty tiles and data
			removeTiles( tilesToRemove );	/* synchronized */
			removeData( annotation );		/* synchronized */
			
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
		
	}

	
	/*
	 * 
	 * Helper methods
	 * 
	 */
	private void addDataReferenceToTiles( List<AnnotationTile> tiles, List<TileAndBinIndices> indices, AnnotationData data ) {		
		
		_cacheLock.writeLock().lock();
		try {

	    	for ( TileAndBinIndices index : indices ) {			
				// check all existing tiles for matching index
				for ( AnnotationTile tile : tiles ) {
					
					if ( tile.getIndex().equals( index.getTile() ) ) {
						// tile exists already, add data to bin
						tile.add( index.getBin(), data );
						return;				
					} 
				}
				// no tile exists, add tile
				tiles.add( new AnnotationTile( index.getTile(), new AnnotationBin( index.getBin(), data ) ) );									
	    	}				
						
		} finally {
			_cacheLock.writeLock().unlock();
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
	
	

	/*
	 * 
	 * Synchronized Cache methods
	 * 
	 */
	
	
	/*
	 * Get
	 */
	private AnnotationData getDataFromCache( Long index ) {
		
		_cacheLock.readLock().lock();
		try {
			return _dataCache.get( index );
		} finally {
			_cacheLock.readLock().unlock();
		}
	}
	private AnnotationTile getTileFromCache( TileIndex index ) {
		
		_cacheLock.readLock().lock();
		try {
			return _tileCache.get( index );
		} finally {
			_cacheLock.readLock().unlock();
		}
	}
	
	/*
	 * Put
	 */
	private void putDataInCache( List<AnnotationData> data ) {
		
		_cacheLock.writeLock().lock();
		try {
			for ( AnnotationData d : data ) {
				_dataCache.put( d.getIndex(), d );
			}
			System.out.println("Data cache size: " + _dataCache.size() );
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	private void putDataInCache( AnnotationData data ) {
		
		_cacheLock.writeLock().lock();
		try {
			_dataCache.put( data.getIndex(), data );
			System.out.println("Data cache size: " + _dataCache.size() );
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	
	private void putTilesInCache( List<AnnotationTile> tiles ) {
		
		_cacheLock.writeLock().lock();
		try {
			
			for ( AnnotationTile tile : tiles ) {
				if ( _tileCache.containsKey( tile.getIndex() ) ) {
					// update existing tile, don't replace as other threads
					// may have a reference
					_tileCache.get( tile.getIndex() ).copy( tile );
				} else {
					_tileCache.put( tile.getIndex(), tile );
				}
				System.out.println("Tile cache size: " + _tileCache.size() );
			}
			
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	private void putTileInCache( AnnotationTile tile ) {
		
		_cacheLock.writeLock().lock();
		try {
			_tileCache.put( tile.getIndex(), tile );
			System.out.println("Tile cache size: " + _tileCache.size() );
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	
	/*
	 * Remove
	 */
	private void removeDataFromCache( List<AnnotationData> data ) {
		
		_cacheLock.writeLock().lock();
		try {
			for ( AnnotationData d : data ) {
				_dataCache.remove( d.getIndex() );
			}
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	private void removeDataFromCache( AnnotationData data ) {
		
		_cacheLock.writeLock().lock();
		try {
			_dataCache.remove( data.getIndex() );
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	private void removeTilesFromCache( List<AnnotationTile> tiles ) {
		
		_cacheLock.writeLock().lock();
		try {
			for ( AnnotationTile tile : tiles ) {
				_tileCache.remove( tile.getIndex() );
			}
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	private void removeTileFromCache( AnnotationTile tile ) {
		
		_cacheLock.writeLock().lock();
		try {
			_tileCache.remove( tile.getIndex() );	
		} finally {
			_cacheLock.writeLock().unlock();
		}
	}
	
	
	
	private List<AnnotationTile> getTiles( List<TileIndex> indices ) {
		
		List<AnnotationTile> tiles = new LinkedList<>();			
		List<TileIndex> toReadFromIO = new LinkedList<>();	
		
		// pull from cache
		for ( TileIndex index : indices ) {
			
			AnnotationTile tile = getTileFromCache( index ); /* synchronized */			
			if ( tile != null ) {
				// found in cache
				tiles.add( tile );
			} else {
				// not in cache, flag to read from io
				toReadFromIO.add( index );
			}
		}
		
		// pull tiles from io while updating cache
		tiles.addAll( pullTilesFromIO( toReadFromIO ) );
		return tiles;		
	}
	
	
	private List<AnnotationData> getDataFromIndex( List<Long> indices ) {
		
		List<AnnotationData> data = new LinkedList<>();	
		List<Long> dataToReadFromIO = new LinkedList<>();
		
		// pull data from cache
		for ( Long index : indices ) {
			
			AnnotationData d = getDataFromCache( index ); /* synchronized */	
			if ( d != null ) {				
				// found in cache
				data.add( d );
			} else {				
				// not in cache, flag to read from io
				dataToReadFromIO.add( index );
			}
		}
		
		// pull data from io and update cache	
		data.addAll( pullDataFromIO( dataToReadFromIO ) );
		return data;
		
	}
	
	private List<AnnotationData> getData( List<TileIndex> indices ) {
			
		List<AnnotationTile> tiles = getTiles( indices );  /* synchronized */

		List<Long> references = new LinkedList<>();
		_cacheLock.readLock().lock();
		try {
			// get filtered references from tiles		
			for ( AnnotationTile tile : tiles ) {
				references.addAll( tile.getAllReferences() );
			}
		} finally {
			_cacheLock.readLock().unlock();
		}

		return getDataFromIndex( references );

	}
	
	
	private List<AnnotationData> getData( List<TileIndex> indices, Map<String, Integer> filter ) {
					
		// get all required tiles
		List<AnnotationTile> tiles = getTiles( indices ); /* synchronized */
		
		// get filtered references from tiles
		List<Long> references = new LinkedList<>();			
		for ( AnnotationTile tile : tiles ) {
			references.addAll( tile.getFilteredReferences( filter ) );
		}
		return getDataFromIndex( references );
	}

	
	private void writeTiles( List<AnnotationTile> tiles ) {
		
		putTilesInCache( tiles ); /* synchronized */

		try {
			
			_hbaseLock.writeLock().lock();
			try {
				
				_io.initializeForWrite( TABLE_NAME );
				_io.writeTiles( TABLE_NAME, _tileSerializer, tiles );
				
			} finally {
				_hbaseLock.writeLock().unlock();
			}	
						
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
	}
	
	
	private void writeData( AnnotationData data ) {
		
		putDataInCache( data ); /* synchronized */
		
		List<AnnotationData> dataList = new LinkedList<>();
		dataList.add( data );
		
		try {
			
			_hbaseLock.writeLock().lock();
			try {
				
				_io.initializeForWrite( TABLE_NAME );		
				_io.writeData( TABLE_NAME, _dataSerializer, dataList );
				
			} finally {
				_hbaseLock.writeLock().unlock();
			}
			
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	

	private void removeTiles( List<AnnotationTile> tiles ) {

		removeTilesFromCache( tiles ); /* synchronized */

		try {
			
			_hbaseLock.writeLock().lock();
			try {
				
				_io.initializeForRemove( TABLE_NAME );		
				_io.removeTiles( TABLE_NAME, tiles );
				
			} finally {
				_hbaseLock.writeLock().unlock();
			}			
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}

	}
	
	
	private void removeData( AnnotationData data ) {
		
		removeDataFromCache( data ); /* synchronized */

		List<AnnotationData> dataList = new LinkedList<>();
		dataList.add( data );
		
		try {
			
			_hbaseLock.writeLock().lock();
			try {
				
				_io.initializeForRemove( TABLE_NAME );	
				_io.removeData( TABLE_NAME, dataList );	
				
			} finally {
				_hbaseLock.writeLock().unlock();
			}						
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}

	}
	
	
	private List<AnnotationTile> pullTilesFromIO( List<TileIndex> indices ) {
			
		List<AnnotationTile> tiles = new LinkedList<>();
		try {
			
			_hbaseLock.readLock().lock();
			try {
				
				_io.initializeForRead( TABLE_NAME );		
				tiles = _io.readTiles( TABLE_NAME, _tileSerializer, indices );						
				
			} finally {
				_hbaseLock.readLock().unlock();
			}
					
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		// add to cache
		putTilesInCache( tiles ); /* synchronized */

		return tiles;		
	}
	
	
	private List<AnnotationData> pullDataFromIO( List<Long> indices ) {
		
		List<AnnotationData> data = new LinkedList<>();
		try {
			
			_hbaseLock.readLock().lock();
			try {
				
				_io.initializeForRead( TABLE_NAME );	
				data = _io.readData( TABLE_NAME, _dataSerializer, indices );
				
			} finally {
				_hbaseLock.readLock().unlock();
			}			
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		// add to cache
		putDataInCache( data ); /* synchronized */		
		
		return data;		
	}
	
	
}