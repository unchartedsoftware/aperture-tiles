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
import java.util.Map;


import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.cache.*;
import com.oculusinfo.annotation.cache.impl.*;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.*;
import com.oculusinfo.tile.init.FactoryProvider;

@Singleton
public class CachedAnnotationServiceImpl extends AnnotationServiceImpl {

	private final int MAX_CACHE_ENTRIES = 10000;
	
	private ConcurrentHashMap< String, AnnotationCache<TileIndex, TileData<Map<String, List<Pair<String,Long>>>>> > _tileCache;
	private ConcurrentHashMap< String, AnnotationCache<Pair<String,Long>, AnnotationData<?>> > _dataCache;

	
	@Inject
    public CachedAnnotationServiceImpl( @Named("com.oculusinfo.annotation.config") String annotationConfigurationLocation,
							    		FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
							  	        FactoryProvider<TileSerializer<?>> tileSerializerFactoryProvider,
							  		    FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
							  		    AnnotationIndexer indexer,
									    AnnotationSerializer serializer ) {
		super( annotationConfigurationLocation, 
			   pyramidIOFactoryProvider, 
			   tileSerializerFactoryProvider, 
			   tilePyramidFactoryProvider, 
			   indexer, serializer );
		_tileCache = new ConcurrentHashMap<>();
		_dataCache = new ConcurrentHashMap<>();
	}
	
	protected AnnotationCache<TileIndex, TileData<Map<String, List<Pair<String,Long>>>>> getLayerTileCache( String layer ) {
		if ( !_tileCache.containsKey( layer ) ) {
			_tileCache.put( layer, new ConcurrentLRUCache< TileIndex, TileData<Map<String, List<Pair<String,Long>>>> >( MAX_CACHE_ENTRIES ) );
		}
		return _tileCache.get( layer );
	}
	
	protected AnnotationCache<Pair<String,Long>, AnnotationData<?>> getLayerDataCache( String layer ) {
		if ( !_dataCache.containsKey( layer ) ) {
			_dataCache.put( layer, new ConcurrentLRUCache< Pair<String,Long>, AnnotationData<?> >( MAX_CACHE_ENTRIES ) );	
		}
		return _dataCache.get( layer );
	}
	
	@Override
	protected void writeTilesToIO( String layer, List<TileData<Map<String, List<Pair<String,Long>>>>> tiles ) {
		
		AnnotationCache<TileIndex, TileData<Map<String, List<Pair<String,Long>>>>> tileCache = getLayerTileCache( layer );
		
		// put in cache
		for ( TileData<Map<String, List<Pair<String,Long>>>> tile : tiles ) {

			tileCache.put( tile.getDefinition(), tile );
		}

		super.writeTilesToIO( layer, tiles );
	}
	
	@Override
	protected void writeDataToIO( String layer, AnnotationData<?> data ) {
		
		AnnotationCache<Pair<String,Long>, AnnotationData<?>> dataCache = getLayerDataCache( layer );
		
		// put in cache
		dataCache.put( data.getReference(), data );
		
		super.writeDataToIO( layer, data );
	}
	
	@Override
	protected void removeTilesFromIO( String layer, List<TileIndex> tiles ) {

		AnnotationCache<TileIndex, TileData<Map<String, List<Pair<String,Long>>>>> tileCache = getLayerTileCache( layer );
		
		// remove from cache
		for ( TileIndex tile : tiles ) {
			tileCache.remove( tile );
		}
		
		super.removeTilesFromIO( layer, tiles );

	}
	
	@Override
	protected void removeDataFromIO( String layer, Pair<String,Long> reference ) {
		
		AnnotationCache<Pair<String,Long>, AnnotationData<?>> dataCache = getLayerDataCache( layer );
		
		// remove from cache
		dataCache.remove( reference );

		super.removeDataFromIO( layer, reference );

	}
	
	@Override
	protected List<TileData<Map<String, List<Pair<String,Long>>>>> readTilesFromIO( String layer, List<TileIndex> indices ) {
			
		AnnotationCache<TileIndex, TileData<Map<String, List<Pair<String,Long>>>>> tileCache = getLayerTileCache( layer );		
		
		List<TileData<Map<String, List<Pair<String,Long>>>>> tiles = new LinkedList<>();			
		List<TileIndex> toReadFromIO = new LinkedList<>();	
		
		// pull from cache
		for ( TileIndex index : indices ) {
			
			TileData<Map<String, List<Pair<String,Long>>>> tile = tileCache.get( index );		
			if ( tile != null ) {
				// found in cache
				tiles.add( tile );
			} else {
				// not in cache, flag to read from io
				toReadFromIO.add( index );
			}
		}
		
		// pull tiles from io while updating cache
		List<TileData<Map<String, List<Pair<String,Long>>>>> freshTiles = super.readTilesFromIO( layer, toReadFromIO );
    	tiles.addAll( freshTiles );
		
		// add to cache
		for ( TileData<Map<String, List<Pair<String,Long>>>> tile : freshTiles ) {
			tileCache.put( tile.getDefinition(), tile );
		}

		return tiles;		
	}
	
	
	@Override
	protected List<AnnotationData<?>> readDataFromIO( String layer, List<Pair<String,Long>> references ) {
		
		AnnotationCache<Pair<String,Long>, AnnotationData<?>> dataCache = getLayerDataCache( layer );		
		
		List<AnnotationData<?>> data = new LinkedList<>();	
		List<Pair<String,Long>> dataToReadFromIO = new LinkedList<>();
		
		// for each reference, pull from cache and flag missing for read
		for ( Pair<String,Long> reference : references ) {
			
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