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
package com.oculusinfo.annotation.rest;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.AnnotationTile;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.annotation.filter.impl.FilteredBinResults;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.SerializationTypeChecker;
import com.oculusinfo.binning.util.JsonUtilities;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.properties.JSONArrayProperty;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.layer.LayerService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Singleton
public class AnnotationServiceImpl implements AnnotationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationServiceImpl.class);

	public static final List<String> GROUPS_PATH = Collections.unmodifiableList( Arrays.asList( "public" ) );
	public static final JSONArrayProperty GROUPS = new JSONArrayProperty("groups",
        "The identifiers that annotations are grouped by",
        "[\"Urgent\",\"High\",\"Medium\",\"Low\"]");
	// These two functions are used to check and cast the type of the tile serializer we use.

	// Just wrapping the Map.class, which is the same as the complex class listed due to 
	// type erasure.
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Class<Map<String, List<Pair<String, Long>>>> getRuntimeBinClass () {
		return (Class) Map.class;
	}
	public static TypeDescriptor getRuntimeTypeDescriptor () {
		return new TypeDescriptor(Map.class,
		                          new TypeDescriptor(String.class),
		                          new TypeDescriptor(List.class,
		                                             new TypeDescriptor(Pair.class,
		                                                                new TypeDescriptor(String.class),
		                                                                new TypeDescriptor(Long.class))));
	}

	private LayerService _layerService;
	private AnnotationSerializer _dataSerializer;
	private AnnotationIndexer _indexer;
    private FactoryProvider<AnnotationIO> _annotationIOFactoryProvider;
    private FactoryProvider<AnnotationFilter> _annotationFilterFactoryProvider;
    private Map<String, Boolean> _initializedLayersById;
	
	protected final ReadWriteLock _lock = new ReentrantReadWriteLock();

    @Inject
	public AnnotationServiceImpl( LayerService service,
                                  AnnotationSerializer serializer,
                                  AnnotationIndexer indexer,
                                  FactoryProvider<AnnotationIO> annotationIOFactoryProvider,
                                  FactoryProvider<AnnotationFilter> annotationFilterFactoryProvider) {

        _layerService = service;
        _dataSerializer = serializer;
        _indexer = indexer;
        _annotationIOFactoryProvider = annotationIOFactoryProvider;
        _annotationFilterFactoryProvider = annotationFilterFactoryProvider;
        _initializedLayersById = new HashMap<>();
	}

    /**
	 * Wraps the options and query {@link JSONObject}s together into a new object.
	 */
	private JSONObject mergeQueryConfigOptions(JSONObject options, JSONObject query) {

        JSONObject result = JsonUtilities.deepClone( options );
        try {
            // all client configurable properties exist under an unseen 'public' node,
            // create this node before overlay query parameters onto server config
            if ( query != null ) {
                JSONObject publicNode = new JSONObject();
                publicNode.put( "public", query );
                result = JsonUtilities.overlayInPlace( result, publicNode );
            }
        } catch (Exception e) {
			LOGGER.error("Couldn't merge query options with main options.", e);
		}
		return result;
	}

    public LayerConfiguration getLayerConfiguration( String layer, JSONObject query ) {
        LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );
        config.addProperty( GROUPS, GROUPS_PATH );
		config.addChildFactory( _annotationIOFactoryProvider.createFactory(config, LayerConfiguration.PYRAMID_IO_PATH) );
        config.addChildFactory( _annotationFilterFactoryProvider.createFactory(config, LayerConfiguration.FILTER_PATH) );
        JSONObject layerConfig = _layerService.getLayerJSON( layer );
        try {
            config.readConfiguration( mergeQueryConfigOptions( layerConfig, query ) );
            return config;
        } catch ( Exception e ) {
            return null;
        }
    }

	public Pair<String,Long> write( String layer,
	                                AnnotationData<?> annotation ) throws IllegalArgumentException {
		
		_lock.writeLock().lock();
		try {

			LayerConfiguration config = getLayerConfiguration( layer, null );
			TilePyramid pyramid = config.produce( TilePyramid.class );

            /*
             * This makes the assumption that if you are writing an annotation, the table MAY
             * not exist. So in this case, for the first write, make the table if it does no exist
             * in a thread-safe manner.
             */
            if ( !_initializedLayersById.containsKey( layer  ) ) {
                String dataId = config.getPropertyValue( LayerConfiguration.DATA_ID );
                AnnotationIO aio = config.produce( AnnotationIO.class );
                aio.initializeForRead( dataId );
                PyramidIO pio = config.produce( PyramidIO.class );
                pio.initializeForRead( dataId, 0, 0, null );
                _initializedLayersById.put( layer, true );
            }

			/*
			 * check if UUID results in IO collision, if so prevent io corruption
			 * by throwing an exception, this is so statistically unlikely that
			 * any further action is unnecessary
			 */
			if ( checkForCollision( layer, annotation ) ) {
				throw new IllegalArgumentException("Unable to generate UUID without collision, WRITE operation aborted");
			}

			addDataToTiles( layer, annotation, pyramid );

			// return generated certificate
			return annotation.getCertificate();
    		
		} catch ( Exception e ) {
			e.printStackTrace();
			throw new IllegalArgumentException( e.getMessage() );
		} finally {
			_lock.writeLock().unlock();
		}

	}


	public Pair<String,Long> modify( String layer,
	                                 AnnotationData<?> annotation ) throws IllegalArgumentException {

		_lock.writeLock().lock();
		try {

			/*
			 *  ensure request is coherent with server state, if client is operating
			 *  on a previous data state, prevent io corruption by throwing an exception
			 */
			if ( isRequestOutOfDate( layer, annotation.getCertificate() ) ) {
				throw new IllegalArgumentException("Client is out of sync with Server, "
				                                   + "MODIFY operation aborted. It is recommended "
				                                   + "upon receiving this exception to refresh all client annotations");
			}
			LayerConfiguration config = getLayerConfiguration( layer, null );
			TilePyramid pyramid = config.produce( TilePyramid.class );

			/*
			 * Technically you should not have to re-tile the annotation if
			 * there is only a content change, as it will stay in the same tiles.
			 * However, we want to update the certificate time-stamp in the containing
			 * tile so that we can filter from tiles without relying on reading the
			 * individual annotations themselves
			 */
			// remove old annotation from tiles
			removeDataFromTiles( layer, annotation.getCertificate(), pyramid );
			// update certificate
			annotation.updateCertificate();
			// add new annotation to tiles
			addDataToTiles( layer, annotation, pyramid );
			// return updated certificate
			return annotation.getCertificate();

		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		} finally {
			_lock.writeLock().unlock();
		}

	}
	

	public List<List<AnnotationData<?>>> read( String layer, TileIndex index, JSONObject query ) {

		_lock.readLock().lock();
		try {

			LayerConfiguration config = getLayerConfiguration( layer, query );
			TilePyramid pyramid = config.produce( TilePyramid.class );
			AnnotationFilter filter = config.produce( AnnotationFilter.class );

			return getDataFromTiles( layer, index, filter, pyramid );
    		
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		} finally { 		
			_lock.readLock().unlock();
		}
	}
	
		
	public void remove( String layer, Pair<String, Long> certificate ) throws IllegalArgumentException {

		_lock.writeLock().lock();
		try {

			LayerConfiguration config = getLayerConfiguration( layer, null );
			TilePyramid pyramid = config.produce(TilePyramid.class);

			/*
			 *  ensure request is coherent with server state, if client is operating
			 *  on a previous data state, prevent io corruption by throwing an exception
			 */
			if ( isRequestOutOfDate( layer, certificate ) ) {
				throw new IllegalArgumentException("Client is out of sync with Server, "
				                                   + "REMOVE operation aborted. It is recommended "
				                                   + "upon receiving this exception to refresh all client annotations");
			}
			// remove the certificates from tiles
			removeDataFromTiles( layer, certificate, pyramid );
			// remove data from io
			removeDataFromIO( layer, certificate );

		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		} finally {
			_lock.writeLock().unlock();
		}
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
		
		List<Pair<String,Long>> certificate = new LinkedList<>();
		certificate.add( annotation.getCertificate() );
		return ( readDataFromIO( layer, certificate ).size() > 0 ) ;
	}
	
	/*
	 * Check data timestamp from clients source, if out of date, return true
	 */
	public boolean isRequestOutOfDate( String layer, Pair<String, Long> certificate ) {
		
		List<Pair<String, Long>> certificates = new LinkedList<>();
		certificates.add( certificate );
		List<AnnotationData<?>> annotations = readDataFromIO( layer, certificates );
		
		if ( annotations.size() == 0 ) {
			// removed since client update, abort
			return true;
		}
		
		if ( !annotations.get(0).getTimestamp().equals( certificate.getSecond() ) ) {
			// clients timestamp doesn't not match most up to date, abort
			return true;
		}
		
		// everything seems to be in order
		return false;
	}
	
	
	/*
	 * Iterate through all indices, find matching tiles and add data certificate, if tile
	 * is missing, add it
	 */
	private void addDataCertificateToTiles( List<AnnotationTile> tiles, List<TileAndBinIndices> indices, AnnotationData<?> data ) {
		
		for ( TileAndBinIndices index : indices ) {			
			// check all existing tiles for matching index
			boolean found = false;
			for ( AnnotationTile tile : tiles ) {
				if ( tile.getDefinition().equals( index.getTile() ) ) {
					// tile exists already, add data to bin
					tile.addDataToBin( index.getBin(), data );
					found = true;
					break;
				} 
			}
			if ( !found ) {
				// no tile exists, add tile
                AnnotationTile tile = new AnnotationTile( index.getTile() );
				tile.addDataToBin(index.getBin(), data);
				tiles.add( tile );    	
			}
		}				
	}
	
	/*
	 * Iterate through all tiles, removing data certificate from bins, any tiles with no bin entries
	 * are added to tileToRemove, the rest are added to tilesToWrite
	 */
	private void removeDataCertificateFromTiles( List< AnnotationTile > tilesToWrite,
	                                             List< TileIndex > tilesToRemove,
	                                             List< AnnotationTile > tiles,
	                                             AnnotationData<?> data,
	                                             TilePyramid pyramid ) {
		// clear supplied lists
		tilesToWrite.clear();
		tilesToRemove.clear();	

		// for each tile, remove data from bins
		for ( AnnotationTile tile : tiles ) {
			// get bin index for the annotation in this tile
			BinIndex binIndex = _indexer.getIndicesByLevel( data, tile.getDefinition().getLevel(), pyramid ).get(0).getBin();
			// remove data from tile
            tile.removeDataFromBin(binIndex, data);
		}	
		
		// determine which tiles need to be re-written and which need to be removed
		for ( AnnotationTile tile : tiles ) {
			if ( tile.isEmpty() ) {
				// if no data left, flag tile for removal
				tilesToRemove.add( tile.getDefinition() );
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

	
	private List< List<AnnotationData<?>> > getDataFromTiles( String layer, TileIndex tileIndex, AnnotationFilter filter, TilePyramid pyramid ) {
		
		// wrap index into list 
		List<TileIndex> indices = new LinkedList<>();
		indices.add( tileIndex );
			
		// get tiles
		List< AnnotationTile > tiles = readTilesFromIO( layer, indices );
				
		// for each tile, assemble list of all data certificates
		List<Pair<String,Long>> certificates = new LinkedList<>();
		List<FilteredBinResults> results = new LinkedList<>();
		for ( AnnotationTile tile : tiles ) {
			// for each bin
			FilteredBinResults r = filter.filterBins(tile.getData());
			certificates.addAll(r.getFilteredBins());
			results.add(r);			
		}
		
		// read data from io
		List<AnnotationData<?>> annotations = readDataFromIO( layer, certificates );

        // return null if there are no annotations
        if ( annotations.size() == 0 ) {
            return null;
        }

		// apply filter to annotations
		List<AnnotationData<?>> filteredAnnotations = filter.filterAnnotations( annotations, results );

        // fill array
		List< List<AnnotationData<?>> > dataByBin = new ArrayList<>();
        int totalBins = tileIndex.getXBins()*tileIndex.getYBins();
        for ( int i=0; i<totalBins; i++ ) {
            dataByBin.add( new ArrayList<AnnotationData<?>>() );
        }

        // assemble data by bin
		for ( AnnotationData<?> annotation : filteredAnnotations ) {
			// get index
			BinIndex binIndex = _indexer.getIndicesByLevel( annotation, tileIndex.getLevel(), pyramid ).get(0).getBin();
            int index = binIndex.getX() + ( binIndex.getY() * tileIndex.getXBins() );
			// add data to list, under bin
			dataByBin.get( index ).add( annotation );
		}
		return dataByBin;
	}

	
	private void addDataToTiles( String layer, AnnotationData<?> data, TilePyramid pyramid ) {
		
		// get list of the indices for all levels
		List< TileAndBinIndices > indices = _indexer.getIndices( data, pyramid );
		// get all affected tiles
		List< AnnotationTile > tiles = readTilesFromIO( layer, convert( indices ) );
		// add new data certificate to tiles
		addDataCertificateToTiles( tiles, indices, data );
		// write tiles back to io
		writeTilesToIO( layer, tiles );    		
		// write data to io
		writeDataToIO( layer, data );

	}
	
	
	private void removeDataFromTiles( String layer, Pair<String, Long> certificate, TilePyramid pyramid ) {

		// read the annotation data
		List< Pair<String, Long> > certificates = new ArrayList<>();
		certificates.add( certificate );
		AnnotationData<?> data = readDataFromIO( layer, certificates ).get(0);
		// get list of the indices for all levels
		List< TileAndBinIndices > indices = _indexer.getIndices( data, pyramid );
		// read existing tiles
		List< AnnotationTile > tiles = readTilesFromIO( layer, convert( indices ) );
		// maintain lists of what bins to modify and what bins to remove
		List< AnnotationTile > tilesToWrite = new LinkedList<>();
		List< TileIndex > tilesToRemove = new LinkedList<>();
		// remove data from tiles and organize into lists to write and remove
		removeDataCertificateFromTiles( tilesToWrite, tilesToRemove, tiles, data, pyramid );
		// write modified tiles
		writeTilesToIO( layer, tilesToWrite );		
		// remove empty tiles and data
		removeTilesFromIO( layer, tilesToRemove );
	}


	protected void writeTilesToIO( String layer, List< AnnotationTile > tiles ) {
		
		if ( tiles.size() == 0 ) return;
		
		try {
			LayerConfiguration config = getLayerConfiguration( layer, null );
			PyramidIO io = config.produce(PyramidIO.class);	

			TileSerializer<Map<String, List<Pair<String, Long>>>> serializer =
				SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
				                                       getRuntimeBinClass(),
				                                       getRuntimeTypeDescriptor());

			String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			io.writeTiles( dataId, serializer, AnnotationTile.convertToRaw( tiles ) );
					
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
		
	}
	
	
	protected void writeDataToIO( String layer, AnnotationData<?> data ) {
		
		List<AnnotationData<?>> dataList = new LinkedList<>();
		dataList.add( data );

		try {
			LayerConfiguration config = getLayerConfiguration( layer, null );
			AnnotationIO io = config.produce( AnnotationIO.class );
            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			io.writeData( dataId, _dataSerializer, dataList );

		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
	}
	

	protected void removeTilesFromIO( String layer, List<TileIndex> tiles ) {

		if ( tiles.size() == 0 ) {
			return;
		}
		
		try {

			LayerConfiguration config = getLayerConfiguration( layer, null );
			PyramidIO io = config.produce( PyramidIO.class );
            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			io.removeTiles( dataId, tiles );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}

	}
	
	
	protected void removeDataFromIO( String layer, Pair<String, Long> data ) {
		
		List<Pair<String, Long>> dataList = new LinkedList<>();
		dataList.add( data );

		try {

			LayerConfiguration config = getLayerConfiguration( layer, null );
			AnnotationIO io = config.produce( AnnotationIO.class );
            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			io.removeData( dataId, dataList );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}

	}
	
	
	protected List< AnnotationTile > readTilesFromIO( String layer, List<TileIndex> indices ) {

        List< AnnotationTile > tiles = new LinkedList<>();
        Set<TileIndex> readTiles = new HashSet<>();

		if ( indices.size() == 0 ) {
			return tiles;
		}
		
		try {
			LayerConfiguration config = getLayerConfiguration( layer, null );
			PyramidIO io = config.produce( PyramidIO.class );
			TileSerializer<Map<String, List<Pair<String, Long>>>> serializer =
				SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
				                                       getRuntimeBinClass(),
				                                       getRuntimeTypeDescriptor());

            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);

			for ( AnnotationTile tile : AnnotationTile.convertFromRaw( io.readTiles(dataId, serializer, indices) ) ) {
				if (!readTiles.contains(tile.getDefinition())) {
					readTiles.add(tile.getDefinition());
					tiles.add(tile);
				}
			}
					
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
		return tiles;		
	}
	
	protected List<AnnotationData<?>> readDataFromIO( String layer, List<Pair<String,Long>> certificates ) {
		
		List<AnnotationData<?>> data = new LinkedList<>();
		
		if ( certificates.size() == 0 ) {
			return data;
		}
		
		try {

			LayerConfiguration config = getLayerConfiguration( layer, null );
            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			AnnotationIO io = config.produce( AnnotationIO.class );
			data = io.readData( dataId, _dataSerializer, certificates );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}

		return data;
	}
}
