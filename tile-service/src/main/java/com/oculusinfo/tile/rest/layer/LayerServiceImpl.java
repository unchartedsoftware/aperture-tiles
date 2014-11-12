/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rest.layer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.JsonUtilities;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.EmptyConfigurableFactory;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.init.providers.CachingLayerConfigurationProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.RequestParamsFactory;
import com.oculusinfo.tile.rest.tile.caching.CachingPyramidIO.LayerDataChangedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;

@Singleton
public class LayerServiceImpl implements LayerService {
	private static final Logger LOGGER = LoggerFactory.getLogger(LayerServiceImpl.class);
    private static final String ERROR_SHA = "ERROR-SHA-256";

	private List< JSONObject > _layers;
	private Map< String, JSONObject > _layersById;
    private Map< String, JSONObject > _layersBySha;
	private Map< String, JSONObject > _metaDataCache;
    private FactoryProvider< LayerConfiguration > _layerConfigurationProvider;

	@Inject
	public LayerServiceImpl( @Named("com.oculusinfo.tile.layer.config") String layerConfigurationLocation,
	                         FactoryProvider<LayerConfiguration> layerConfigProvider ) {
		_layers = new ArrayList<>();
		_layersById = new HashMap<>();
        _layersBySha = new HashMap<>();
		_metaDataCache = new HashMap<>();
        _layerConfigurationProvider = layerConfigProvider;

		if (layerConfigProvider instanceof CachingLayerConfigurationProvider) {
            CachingLayerConfigurationProvider caching = (CachingLayerConfigurationProvider)layerConfigProvider;
			caching.addLayerListener( new LayerDataChangedListener() {
                public void onLayerDataChanged( String layerId ) {
                    _metaDataCache.remove( layerId );
                }
            } );
		}
		readConfigFiles( getConfigurationFiles( layerConfigurationLocation ) );
	}

	@Override
	public List< JSONObject > getLayerJSONs() {
		return _layers;
	}

    @Override
	public JSONObject getLayerJSON( String layerId ) {
		return _layersById.get( layerId );
	}

    @Override
	public List< String > getLayerIds() {
        List< String > layers = new ArrayList<>();
        try {
            for ( JSONObject layerConfig : _layers ) {
                layers.add( layerConfig.getString( LayerConfiguration.LAYER_ID.getName() ) );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
		return layers;
	}

	@Override
	public PyramidMetaData getMetaData( String layerId ) {
		try {
			LayerConfiguration config = getLayerConfiguration( layerId, null );
            String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
			PyramidIO pyramidIO = config.produce( PyramidIO.class );
			return getMetaData( layerId, dataId, pyramidIO );
		} catch (ConfigurationException e) {
			LOGGER.error( "Couldn't determine pyramid I/O method for {}", layerId, e );
			return null;
		}
	}
    
	private PyramidMetaData getMetaData( String layerId, String dataId, PyramidIO pyramidIO ) {
		try {
			JSONObject metadata = _metaDataCache.get( layerId );
			if ( metadata == null ) {
				String s = pyramidIO.readMetaData( dataId );
				if ( s == null ) {
                    metadata = new JSONObject();
				} else {
                    metadata = new JSONObject( s );
                }
				_metaDataCache.put( layerId, metadata );
			}
			return new PyramidMetaData( metadata );
		} catch (JSONException e) {
			LOGGER.error("Metadata file for layer is missing or corrupt: {}", layerId, e);
		} catch (IOException e) {
			LOGGER.error("Couldn't read metadata: {}", layerId, e);
		}
		return null;
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

    @Override
	public LayerConfiguration getLayerConfiguration( String layerId, JSONObject requestParams ) {
		try {
            JSONObject layerConfig;
            if ( requestParams != null && requestParams.has("sha") ) {
                layerConfig =_layersBySha.get( requestParams.getString("sha") );
            } else {
                layerConfig = _layersById.get( layerId );
            }
			//the root factory that does nothing
			EmptyConfigurableFactory rootFactory = new EmptyConfigurableFactory( null, null, null );
			//add another factory that will handle query params
			RequestParamsFactory queryParamsFactory = new RequestParamsFactory( null, rootFactory, new ArrayList<String>() );
			rootFactory.addChildFactory(queryParamsFactory);
			//add the layer configuration factory
			ConfigurableFactory<LayerConfiguration> factory = _layerConfigurationProvider.createFactory( rootFactory, new ArrayList<String>() );
			rootFactory.addChildFactory(factory);
			rootFactory.readConfiguration( mergeQueryConfigOptions( layerConfig, requestParams ) );
			LayerConfiguration config = rootFactory.produce( LayerConfiguration.class );
			// Initialize the PyramidIO for reading
			String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
            PyramidIO pyramidIO = config.produce( PyramidIO.class );
			JSONObject initJSON = config.getProducer( PyramidIO.class ).getPropertyValue( PyramidIOFactory.INITIALIZATION_DATA );
            if ( initJSON != null ) {
				int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
				int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
				Properties initProps = JsonUtilities.jsonObjToProperties(initJSON);
				pyramidIO.initializeForRead( dataId, width, height, initProps);
			}
			return config;
		} catch ( Exception e ) {
			LOGGER.warn("Error configuring rendering for", e);
			return null;
		}
	}

    @Override
	public String configureLayer( String layerId, JSONObject overrideConfiguration ) throws Exception {
        try {
            // build layer config to produce string, this ensures that ALL configurable
            // properties are used in sha generation, rather than only those in the JSON
            LayerConfiguration config = getLayerConfiguration( layerId, overrideConfiguration );

            // get SHA-256 hash of state
            String shaHex = config.generateSHA256();

            // store the config under the SHA-256
            _layersBySha.put( shaHex, mergeQueryConfigOptions( _layersById.get( layerId ), overrideConfiguration ) );
            return shaHex;
        } catch ( Exception e ) {
			LOGGER.warn("Error registering configuration to SHA");
            throw e;
		}
	}

	private File[] getConfigurationFiles (String location) {
		try {
			// Find our configuration file.
			URI path;
			if (location.startsWith("res://")) {
				location = location.substring(6);
				path = LayerServiceImpl.class.getResource(location).toURI();
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

	private void readConfigFiles( File[] files ) {
		for (File file: files) {
			try {
				JSONArray contents = new JSONArray( new JSONTokener(new FileReader(file)) );
                for ( int i=0; i<contents.length(); i++ ) {
                    if( contents.get(i) instanceof JSONObject ) {
                        JSONObject layerJSON = contents.getJSONObject(i);
                        _layersById.put( layerJSON.getString( LayerConfiguration.LAYER_ID.getName() ), layerJSON );
                        _layers.add( layerJSON );
                        System.out.println( layerJSON.toString( 4 ) );
                    }
                }

			} catch (FileNotFoundException e) {
				LOGGER.error("Cannot find layer configuration file {} ", file, e);
				return;
			} catch (JSONException e) {
				LOGGER.error("Layer configuration file {} was not valid JSON.", file, e);
			}
		}
	}
}
