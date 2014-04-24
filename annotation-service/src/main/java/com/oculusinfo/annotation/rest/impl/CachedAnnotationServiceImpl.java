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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;



import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.cache.*;
import com.oculusinfo.annotation.cache.impl.*;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.binning.*;


@Singleton
public class CachedAnnotationServiceImpl extends AnnotationServiceImpl {

	private final int MAX_CACHE_ENTRIES = 10000;
	
	private ConcurrentHashMap< String, AnnotationCache<TileIndex, AnnotationTile> > _tileCache;
	private ConcurrentHashMap< String, AnnotationCache<AnnotationReference, AnnotationData<?>> > _dataCache;

	/*
	@Inject
    public CachedAnnotationServiceImpl( @Named("com.oculusinfo.tile.annotation.config") String annotationConfigurationLocation ) {
		super( annotationConfigurationLocation );
		_tileCache = new ConcurrentHashMap<>();
		_dataCache = new ConcurrentHashMap<>();
	}
	*/
		
	public CachedAnnotationServiceImpl( AnnotationIO io, AnnotationIndexer indexer ) {
		
		super( io, indexer );
		_tileCache = new ConcurrentHashMap<>();
		_dataCache = new ConcurrentHashMap<>();
	}
	
	protected AnnotationCache<TileIndex, AnnotationTile> getLayerTileCache( String layer ) {
		if ( !_tileCache.containsKey( layer ) ) {
			_tileCache.put( layer, new ConcurrentLRUCache< TileIndex, AnnotationTile >( MAX_CACHE_ENTRIES ) );
		}
		return _tileCache.get( layer );
	}
	
	protected AnnotationCache<AnnotationReference, AnnotationData<?>> getLayerDataCache( String layer ) {
		if ( !_dataCache.containsKey( layer ) ) {
			_dataCache.put( layer, new ConcurrentLRUCache< AnnotationReference, AnnotationData<?> >( MAX_CACHE_ENTRIES ) );	
		}
		return _dataCache.get( layer );
	}
	
	@Override
	protected void writeTilesToIO( String layer, List<AnnotationTile> tiles ) {
		
		AnnotationCache<TileIndex, AnnotationTile> tileCache = getLayerTileCache( layer );
		
		// put in cache
		for ( AnnotationTile tile : tiles ) {

			tileCache.put( tile.getIndex(), tile );
		}

		super.writeTilesToIO( layer, tiles );
	}
	
	@Override
	protected void writeDataToIO( String layer, AnnotationData<?> data ) {
		
		AnnotationCache<AnnotationReference, AnnotationData<?>> dataCache = getLayerDataCache( layer );
		
		// put in cache
		dataCache.put( data.getReference(), data );
		
		super.writeDataToIO( layer, data );
	}
	
	@Override
	protected void removeTilesFromIO( String layer, List<AnnotationTile> tiles ) {

		AnnotationCache<TileIndex, AnnotationTile> tileCache = getLayerTileCache( layer );
		
		// remove from cache
		for ( AnnotationTile tile : tiles ) {
			tileCache.remove( tile.getIndex() );
		}
		
		super.removeTilesFromIO( layer, tiles );

	}
	
	@Override
	protected void removeDataFromIO( String layer, AnnotationData<?> data ) {
		
		AnnotationCache<AnnotationReference, AnnotationData<?>> dataCache = getLayerDataCache( layer );
		
		// remove from cache
		dataCache.remove( data.getReference() );

		super.removeDataFromIO( layer, data );

	}
	
	@Override
	protected List<AnnotationTile> readTilesFromIO( String layer, List<TileIndex> indices ) {
			
		AnnotationCache<TileIndex, AnnotationTile> tileCache = getLayerTileCache( layer );		
		
		List<AnnotationTile> tiles = new LinkedList<>();			
		List<TileIndex> toReadFromIO = new LinkedList<>();	
		
		// pull from cache
		for ( TileIndex index : indices ) {
			
			AnnotationTile tile = tileCache.get( index );		
			if ( tile != null ) {
				// found in cache
				tiles.add( tile );
			} else {
				// not in cache, flag to read from io
				toReadFromIO.add( index );
			}
		}
		
		// pull tiles from io while updating cache
		List<AnnotationTile> freshTiles = super.readTilesFromIO( layer, toReadFromIO );
    	tiles.addAll( freshTiles );
		
		// add to cache
		for ( AnnotationTile tile : freshTiles ) {
			tileCache.put( tile.getIndex(), tile );
		}

		return tiles;		
	}
	
	
	@Override
	protected List<AnnotationData<?>> readDataFromIO( String layer, List<AnnotationReference> references ) {
		
		AnnotationCache<AnnotationReference, AnnotationData<?>> dataCache = getLayerDataCache( layer );		
		
		List<AnnotationData<?>> data = new LinkedList<>();	
		List<AnnotationReference> dataToReadFromIO = new LinkedList<>();
		
		// for each reference, pull from cache and flag missing for read
		for ( AnnotationReference reference : references ) {
			
			AnnotationData<?> d = dataCache.get( reference );	
			if ( d != null ) {				
				// found in cache
				data.add( d );
			} else {				
				// not in cache, flag to read from io
				dataToReadFromIO.add( reference );
			}
		}
		
    	// pull data from io and update cache	
		List<AnnotationData<?>> freshData = super.readDataFromIO( layer, dataToReadFromIO );
		data.addAll( freshData );
		
		// add to cache
		for ( AnnotationData<?> d : freshData ) {
			dataCache.put( d.getReference(), d );
		}

		return data;		
	}
	
	
}