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

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.cache.*;
import com.oculusinfo.annotation.cache.impl.*;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.binning.*;


@Singleton
public class CachedAnnotationServiceImpl extends AnnotationServiceImpl {

	private final int MAX_CACHE_ENTRIES = 10000;
	
	private AnnotationCache<TileIndex, AnnotationTile> _tileCache;
	private AnnotationCache<Long, AnnotationData>      _dataCache;

	@Inject
	public CachedAnnotationServiceImpl( AnnotationIO io, AnnotationIndexer indexer ) {
		
		super( io, indexer );
		_tileCache = new ConcurrentLRUCache<>( MAX_CACHE_ENTRIES );
		_dataCache = new ConcurrentLRUCache<>( MAX_CACHE_ENTRIES );
	}
	
	@Override
	protected void writeTilesToIO( String layer, List<AnnotationTile> tiles ) {
		
		// put in cache
		for ( AnnotationTile tile : tiles ) {
			_tileCache.put( tile.getIndex(), tile );
		}

		super.writeTilesToIO( layer, tiles );
	}
	
	@Override
	protected void writeDataToIO( String layer, AnnotationData data ) {
		
		// put in cache
		_dataCache.put( data.getIndex(), data );
		
		super.writeDataToIO( layer, data );
	}
	
	@Override
	protected void removeTilesFromIO( String layer, List<AnnotationTile> tiles ) {

		// remove from cache
		for ( AnnotationTile tile : tiles ) {
			_tileCache.remove( tile.getIndex() );
		}
		
		super.removeTilesFromIO( layer, tiles );

	}
	
	@Override
	protected void removeDataFromIO( String layer, AnnotationData data ) {
		
		// remove from cache
		_dataCache.remove( data.getIndex() );

		super.removeDataFromIO( layer, data );

	}
	
	@Override
	protected List<AnnotationTile> readTilesFromIO( String layer, List<TileIndex> indices ) {
			
		List<AnnotationTile> tiles = new LinkedList<>();			
		List<TileIndex> toReadFromIO = new LinkedList<>();	
		
		// pull from cache
		for ( TileIndex index : indices ) {
			
			AnnotationTile tile = _tileCache.get( index );		
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
			_tileCache.put( tile.getIndex(), tile );
		}

		return tiles;		
	}
	
	
	@Override
	protected List<AnnotationData> readDataFromIO( String layer, List<Long> indices ) {
				
		List<AnnotationData> data = new LinkedList<>();	
		List<Long> dataToReadFromIO = new LinkedList<>();
		
		// for each reference, pull from cache and flag missing for read
		for ( Long index : indices ) {
			
			AnnotationData d = _dataCache.get( index );	
			if ( d != null ) {				
				// found in cache
				data.add( d );
			} else {				
				// not in cache, flag to read from io
				dataToReadFromIO.add( index );
			}
		}
		
    	// pull data from io and update cache	
		List<AnnotationData> freshData = super.readDataFromIO( layer, dataToReadFromIO );
		data.addAll( freshData );
		
		// add to cache
		for ( AnnotationData d : freshData ) {
			_dataCache.put( d.getIndex(), d );
		}

		return data;		
	}
	
	
}