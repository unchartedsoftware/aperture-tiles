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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.binning.util.JsonUtilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.oculusinfo.annotation.config.AnnotationConfiguration;
import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.data.AnnotationManipulator;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.rest.AnnotationInfo;
import com.oculusinfo.annotation.rest.AnnotationService;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.rendering.impl.SerializationTypeChecker;


@Singleton
public class AnnotationServiceImpl implements AnnotationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationServiceImpl.class);

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

    private List<AnnotationInfo> _annotationLayers;
    private Map<String, AnnotationInfo> _annotationLayersById;
    private Map<UUID, JSONObject> _configurationssByUuid;

    private FactoryProvider<PyramidIO> _pyramidIOFactoryProvider;
    private FactoryProvider<AnnotationFilter> _filterFactoryProvider;
    private FactoryProvider<AnnotationIO>  _annotationIOFactoryProvider;
    private FactoryProvider<TileSerializer<?>> _tileSerializerFactoryProvider;
    private FactoryProvider<TilePyramid> _tilePyramidFactoryProvider;
        
    protected AnnotationSerializer _dataSerializer;
    protected AnnotationIndexer _indexer;
	
	protected final ReadWriteLock _lock = new ReentrantReadWriteLock();

	
	@Inject
    public AnnotationServiceImpl( @Named("com.oculusinfo.annotation.config") String annotationConfigurationLocation,
					    		  FactoryProvider<PyramidIO> pyramidIOFactoryProvider,
                                  FactoryProvider<AnnotationIO> annotationIOFactoryProvider,
					    	      FactoryProvider<TileSerializer<?>> tileSerializerFactoryProvider,
					    		  FactoryProvider<TilePyramid> tilePyramidFactoryProvider,
                                  FactoryProvider<AnnotationFilter> filterFactoryProvider,
					    		  AnnotationIndexer indexer,
    							  AnnotationSerializer serializer ) {

		_annotationLayers = new ArrayList<>();
		_annotationLayersById = new HashMap<>();
        _configurationssByUuid = new HashMap<>();

		_pyramidIOFactoryProvider = pyramidIOFactoryProvider;
        _annotationIOFactoryProvider = annotationIOFactoryProvider;
		_tileSerializerFactoryProvider = tileSerializerFactoryProvider;
		_tilePyramidFactoryProvider = tilePyramidFactoryProvider;
        _filterFactoryProvider = filterFactoryProvider;

		_dataSerializer = serializer;
		_indexer = indexer;
		
        readConfigFiles( getConfigurationFiles( annotationConfigurationLocation ) );
    }


	public Pair<String,Long> write( String layer,
                                    AnnotationData<?> annotation ) throws IllegalArgumentException {
		
		_lock.writeLock().lock();
    	try {

    		AnnotationConfiguration config = getConfiguration( layer );
    		TilePyramid pyramid = config.produce( TilePyramid.class );

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
            AnnotationConfiguration config = getConfiguration( layer );
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
	

	public Map<BinIndex, List<AnnotationData<?>>> read( UUID id, String layer, TileIndex query ) {

		_lock.readLock().lock();
    	try {

    		AnnotationConfiguration config = getConfiguration( layer, id );
    		TilePyramid pyramid = config.produce( TilePyramid.class );
            AnnotationFilter filter = config.produce( AnnotationFilter.class );
    		return getDataFromTiles( layer, query, filter, pyramid );
    		
    	} catch ( Exception e ) {
    		throw new IllegalArgumentException( e.getMessage() );
    	} finally { 		
    		_lock.readLock().unlock();
    	}
	}
	
		
	public void remove( String layer, Pair<String, Long> certificate ) throws IllegalArgumentException {

        _lock.writeLock().lock();
        try {

			AnnotationConfiguration config = getConfiguration( layer );
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


	@Override
	public List<AnnotationInfo> list () {
	    return _annotationLayers;
	}


    public AnnotationConfiguration getConfiguration( String layer ) {

        return getConfiguration( layer, null );
    }

	public AnnotationConfiguration getConfiguration( String layer, UUID uuid ) {
					
		try {
			AnnotationConfiguration configFactory = new AnnotationConfiguration( _pyramidIOFactoryProvider,
                                                                                 _annotationIOFactoryProvider,
																				 _tileSerializerFactoryProvider,
																				 _tilePyramidFactoryProvider,
                                                                                 _filterFactoryProvider,
																				 null, 
																				 null );
            JSONObject baseLayerConfig;
            if ( uuid != null ) {
                // copy layer info
                baseLayerConfig = JsonUtilities.deepClone( _annotationLayersById.get( layer ).getRawData() );
                // overlay configuration
                JsonUtilities.overlayInPlace( baseLayerConfig, _configurationssByUuid.get( uuid ) );
            } else {
                baseLayerConfig = _annotationLayersById.get( layer ).getRawData();
            }
			configFactory.readConfiguration( baseLayerConfig );
			return configFactory.produce( AnnotationConfiguration.class );
			
		} catch (ConfigurationException e) {
	        LOGGER.warn("Error configuring annotations for {}", layer, e);
	        return null;
	    }
		

	}


	@Override
	public UUID configureLayer (String layerId, JSONObject overrideConfiguration ) {

        UUID uuid = UUID.randomUUID();
        _configurationssByUuid.put( uuid, overrideConfiguration );
        return uuid;
	}


    @Override
    public void unconfigureLayer (String layerId, UUID uuid ) {

        _configurationssByUuid.remove( uuid );
    }


	private File[] getConfigurationFiles (String location) {
    	try {
	    	// Find our configuration file.
	    	URI path = null;
	    	if (location.startsWith("res://")) {
	    		location = location.substring(6);
	    		path = AnnotationServiceImpl.class.getResource(location).toURI();
	    	} else {
	    		path = new File(location).toURI();
	    	}

	    	File configRoot = new File(path);
	    	if (!configRoot.exists())
	    		throw new Exception(location+" doesn't exist");

	    	if (configRoot.isDirectory()) {
	    		return configRoot.listFiles();
	    	} else {
	    		return new File[] {configRoot};
	    	}
    	} catch (Exception e) {
        	LOGGER.warn("Can't find configuration file {}", location, e);
        	return new File[0];
		}
    }

    private void readConfigFiles (File[] files) {
		for (File file: files) {
			try {
			    JSONObject contents = new JSONObject(new JSONTokener(new FileReader(file)));
			    JSONArray configurations = contents.getJSONArray("layers");
    			for (int i=0; i<configurations.length(); ++i) {    	
    				
    				AnnotationInfo info = new AnnotationInfo(configurations.getJSONObject(i));
                    addInfoAndInitializeLayer(info);
    			}
	    	} catch (FileNotFoundException e) {
	    		LOGGER.error("Cannot find annotation configuration file {} ", file, e);
	    		return;
	    	} catch (JSONException e) {
	    		LOGGER.error("Annotation configuration file {} was not valid JSON.", file, e);
	    	}
		}
    }

	private void addInfoAndInitializeLayer (AnnotationInfo info) {

        _annotationLayers.add(info);
        _annotationLayersById.put(info.getID(), info);

        try {
            // ensure both the tile and data io's exist
            // tile io
            AnnotationConfiguration config = getConfiguration( info.getID() );
            PyramidIO tileIo = config.produce( PyramidIO.class );
            tileIo.initializeForWrite( info.getID() );
            // data io
            AnnotationIO dataIo = config.produce( AnnotationIO.class );
            dataIo.initializeForWrite( info.getID() );

        } catch ( Exception e ) {
            throw new IllegalArgumentException( e.getMessage() );
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
	private void addDataCertificateToTiles( List< TileData<Map<String, List<Pair<String,Long>>>>> tiles, List<TileAndBinIndices> indices, AnnotationData<?> data ) {		
		
    	for ( TileAndBinIndices index : indices ) {			
			// check all existing tiles for matching index
    		boolean found = false;
			for ( TileData<Map<String, List<Pair<String,Long>>>> tile : tiles ) {				
				if ( tile.getDefinition().equals( index.getTile() ) ) {
					// tile exists already, add data to bin
					AnnotationManipulator.addDataToTile( tile, index.getBin(), data );
					found = true;
					break;
				} 
			}
			if ( !found ) {
				// no tile exists, add tile
				TileData<Map<String, List<Pair<String,Long>>>> tile = new TileData<>( index.getTile() );				
				AnnotationManipulator.addDataToTile( tile, index.getBin(), data );
				tiles.add( tile );    	
			}
		}				
	}
	
	/*
	 * Iterate through all tiles, removing data certificate from bins, any tiles with no bin entries
	 * are added to tileToRemove, the rest are added to tilesToWrite
	 */
	private void removeDataCertificateFromTiles( List<TileData<Map<String, List<Pair<String,Long>>>>> tilesToWrite, 
											   List<TileIndex> tilesToRemove, 
											   List<TileData<Map<String, List<Pair<String,Long>>>>> tiles, 
											   AnnotationData<?> data,
											   TilePyramid pyramid ) {
		// clear supplied lists
		tilesToWrite.clear();
		tilesToRemove.clear();	

		// for each tile, remove data from bins
		for ( TileData<Map<String, List<Pair<String,Long>>>> tile : tiles ) {				
			// get bin index for the annotation in this tile
			BinIndex binIndex = _indexer.getIndex( data, tile.getDefinition().getLevel(), pyramid ).getBin();		
			// remove data from tile
			AnnotationManipulator.removeDataFromTile( tile, binIndex, data );				
		}	
		
		// determine which tiles need to be re-written and which need to be removed
		for ( TileData<Map<String, List<Pair<String,Long>>>> tile : tiles ) {
			if ( AnnotationManipulator.isTileEmpty( tile ) ) {				
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

	
	private Map<BinIndex, List<AnnotationData<?>>> getDataFromTiles( String layer, TileIndex tileIndex, AnnotationFilter filter, TilePyramid pyramid ) {
		
		// wrap index into list 
		List<TileIndex> indices = new LinkedList<>();
		indices.add( tileIndex );
			
		// get tiles
		List<TileData<Map<String,List<Pair<String,Long>>>>> tiles = readTilesFromIO( layer, indices );
				
		// for each tile, assemble list of all data certificates
		List<Pair<String,Long>> certificates = new LinkedList<>();
		for ( TileData<Map<String,List<Pair<String,Long>>>> tile : tiles ) {					
            // apply filter to the tile
            certificates.addAll( filter.filterTile( tile ) );
		}
		
		// read data from io
		List<AnnotationData<?>> annotations = readDataFromIO( layer, certificates );
        // apply filter to annotations
        List<AnnotationData<?>> filteredAnnotations =  filter.filterAnnotations( annotations );

		// assemble data by bin
		Map<BinIndex, List<AnnotationData<?>>> dataByBin =  new HashMap<>();
		for ( AnnotationData<?> annotation : filteredAnnotations ) {
			// get index 
			BinIndex binIndex = _indexer.getIndex( annotation, tileIndex.getLevel(), pyramid ).getBin();
			if (!dataByBin.containsKey( binIndex)) {
				// no data under this bin, add list to map
				dataByBin.put( binIndex, new LinkedList<AnnotationData<?>>() );
			}
			// add data to list, under bin
			dataByBin.get( binIndex ).add( annotation );
		}
		return dataByBin;
	}

	
	private void addDataToTiles( String layer, AnnotationData<?> data, TilePyramid pyramid ) {
		
		// get list of the indices for all levels
		List<TileAndBinIndices> indices = _indexer.getIndices( data, pyramid );
		// get all affected tiles
		List<TileData<Map<String, List<Pair<String,Long>>>>> tiles = readTilesFromIO( layer, convert( indices ) );
		// add new data certificate to tiles
    	addDataCertificateToTiles( tiles, indices, data );
		// write tiles back to io
		writeTilesToIO( layer, tiles );    		
		// write data to io
		writeDataToIO( layer, data );

	}
	
	
	private void removeDataFromTiles( String layer, Pair<String, Long> certificate, TilePyramid pyramid ) {

        // read the annotation data
        List<Pair<String, Long>> certificates = new ArrayList<>();
        certificates.add( certificate );
        AnnotationData<?> data = readDataFromIO( layer, certificates ).get(0);
		// get list of the indices for all levels
    	List<TileAndBinIndices> indices = _indexer.getIndices( data, pyramid );	    	
		// read existing tiles
		List<TileData<Map<String, List<Pair<String,Long>>>>> tiles = readTilesFromIO( layer, convert( indices ) );					
		// maintain lists of what bins to modify and what bins to remove
		List<TileData<Map<String, List<Pair<String,Long>>>>> tilesToWrite = new LinkedList<>(); 
		List<TileIndex> tilesToRemove = new LinkedList<>();			
		// remove data from tiles and organize into lists to write and remove
		removeDataCertificateFromTiles( tilesToWrite, tilesToRemove, tiles, data, pyramid );
		// write modified tiles
		writeTilesToIO( layer, tilesToWrite );		
		// remove empty tiles and data
		removeTilesFromIO( layer, tilesToRemove );
	}


	protected void writeTilesToIO( String layer, List<TileData<Map<String, List<Pair<String,Long>>>>> tiles ) {
		
		if ( tiles.size() == 0 ) return;
		
		try {
			AnnotationConfiguration config = getConfiguration(layer);
			PyramidIO io = config.produce(PyramidIO.class);	

            TileSerializer<Map<String, List<Pair<String, Long>>>> serializer =
                    SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
                                                           getRuntimeBinClass(),
                                                           getRuntimeTypeDescriptor());

			//io.initializeForWrite( layer );
			io.writeTiles( layer, serializer, tiles );
					
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
		
	}
	
	
	protected void writeDataToIO( String layer, AnnotationData<?> data ) {
		
		List<AnnotationData<?>> dataList = new LinkedList<>();
		dataList.add( data );

		try {
            AnnotationConfiguration config = getConfiguration(layer);
            AnnotationIO io = config.produce( AnnotationIO.class );
			//io.initializeForWrite( layer );
			io.writeData( layer, _dataSerializer, dataList );

		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
	}
	

	protected void removeTilesFromIO( String layer, List<TileIndex> tiles ) {

		if ( tiles.size() == 0 ) {
            return;
        }
		
		try {

			AnnotationConfiguration config = getConfiguration( layer );
			PyramidIO io = config.produce( PyramidIO.class );
			io.removeTiles( layer, tiles );	
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}

	}
	
	
	protected void removeDataFromIO( String layer, Pair<String, Long> data ) {
		
		List<Pair<String, Long>> dataList = new LinkedList<>();
		dataList.add( data );

		try {

            AnnotationConfiguration config = getConfiguration( layer );
			AnnotationIO io = config.produce( AnnotationIO.class );
			io.removeData( layer, dataList );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}

	}
	
	
	protected List<TileData<Map<String, List<Pair<String,Long>>>>> readTilesFromIO( String layer, List<TileIndex> indices ) {
			
		List<TileData<Map<String, List<Pair<String,Long>>>>> tiles = new LinkedList<>();
		
		if ( indices.size() == 0 ) {
            return tiles;
        }
		
		try {
			AnnotationConfiguration config = getConfiguration( layer );
			PyramidIO io = config.produce( PyramidIO.class );
            TileSerializer<Map<String, List<Pair<String, Long>>>> serializer =
                    SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
                                                           getRuntimeBinClass(),
                                                           getRuntimeTypeDescriptor());

			//io.initializeForRead( layer, 0, 0, null );
			tiles = io.readTiles( layer, serializer, indices );
					
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

            AnnotationConfiguration config = getConfiguration( layer );
            AnnotationIO io = config.produce( AnnotationIO.class );
			//io.initializeForRead( layer );
			data = io.readData( layer, _dataSerializer, certificates );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}

		return data;
	}

	
}