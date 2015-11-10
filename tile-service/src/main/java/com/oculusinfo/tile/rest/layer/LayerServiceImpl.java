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
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.tile.init.providers.CachingLayerConfigurationProvider;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.config.ConfigException;
import com.oculusinfo.tile.rest.config.ConfigService;
import com.oculusinfo.tile.rest.tile.caching.CachingPyramidIO.LayerDataChangedListener;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Singleton
public class LayerServiceImpl implements LayerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LayerServiceImpl.class);

	private List< JSONObject > _layers;
	private Map< String, JSONObject > _layersById;
    private Map< String, JSONObject > _layersBySha;
	private Map< String, JSONObject > _metaDataCache;
    private FactoryProvider< LayerConfiguration > _layerConfigurationProvider;
    private final ConfigService _configService;

	@Inject
	public LayerServiceImpl( @Named("com.oculusinfo.tile.layer.config") String layerConfigurationLocation,
	                         FactoryProvider<LayerConfiguration> layerConfigProvider,
                             ConfigService configService) {
		_layers = new ArrayList<>();
		_layersById = new HashMap<>();
        _layersBySha = new HashMap<>();
		_metaDataCache = new HashMap<>();
        _layerConfigurationProvider = layerConfigProvider;
        _configService = configService;

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
            if ( dataId == null ) {
				LOGGER.error( "Couldn't determine data id for layer: "+layerId+" , please ensure the layer config file is correct." );
                return null;
            }
			PyramidIO pyramidIO = config.produce( PyramidIO.class );
			if ( pyramidIO == null ) {
				LOGGER.error( "Couldn't produce the pyramid io instance for layer: "+layerId+" , this is most likely due to either:\n" +
					"\t1) Missing or incorrectly configured 'data' node. Please confirm 'data.id' and 'pyramidio.*' are correct.\n" +
					"\t2) The layer files are unavailable. Please confirm that the database is available, or the files are in the correct directory Default='res://'." );
				return null;
			}
			return getCachedMetaData( layerId, dataId, pyramidIO );
		} catch (ConfigurationException e) {
			LOGGER.error( "Couldn't determine pyramid I/O method for {}", layerId, e );
			return null;
		}
	}

	private PyramidMetaData getCachedMetaData( String layerId, String dataId, PyramidIO pyramidIO ) {
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
            // first check if the query parameters contains a SHA-256 hash. If so
            // load the configured JSONObject. Otherwise take the server default.
            JSONObject layerConfig;
            if ( requestParams != null && requestParams.has("state") ) {
                layerConfig = _layersBySha.get( requestParams.getString("state") );
            } else {
                layerConfig = _layersById.get( layerId );
            }
			// create layer configuration factory
			ConfigurableFactory<? extends LayerConfiguration> factory = _layerConfigurationProvider.createFactory( null, new ArrayList<String>() );
			// override the server configuration with supplied query parameters, this simply overlays
            // the query parameter JSON over the server default JSON, then sets the factory upp
            // to build our layer configuration object.
            factory.readConfiguration( mergeQueryConfigOptions( layerConfig, requestParams ) );
            // produce the layer configuration
			LayerConfiguration config = factory.produce( LayerConfiguration.class );
			JSONObject initJSON = config.getProducer( PyramidIO.class ).getPropertyValue( PyramidIOFactory.INITIALIZATION_DATA );
            if ( initJSON != null ) {
			    String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
				int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
				int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
				Properties initProps = JsonUtilities.jsonObjToProperties(initJSON);
                // initialize the PyramidIO for reading
                PyramidIO pyramidIO = config.produce( PyramidIO.class );
				pyramidIO.initializeForRead( dataId, width, height, initProps);
			}
			return config;
		} catch ( Exception e ) {
			LOGGER.warn("Error configuring rendering for", e);
			return null;
		}
	}

    @Override
	public String saveLayerState( String layerId, JSONObject overrideConfiguration ) throws Exception {
        try {
            // use the layer config to produce the string rather than the config json itself,
            // this ensures that ALL configurable properties are used in sha generation, rather
            // than those only specified in the JSON
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

    @Override
    public JSONObject getLayerStates( String layerId ) {
        JSONObject states = new JSONObject();
        try {
            // add default
            states.put( "default", getLayerConfiguration( layerId, null )
                    .getExplicitConfiguration()
                    .getJSONObject("public") ); // only return public node
            // add saved
            for ( Map.Entry<String, JSONObject> entry : _layersBySha.entrySet() ) {
                String key = entry.getKey();
                JSONObject value = entry.getValue();
                states.put( key, getLayerConfiguration( layerId, value.getJSONObject("public") )
                        .getExplicitConfiguration()
                        .getJSONObject("public") ); // only return public node
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return states;
    }

    @Override
    public JSONObject getLayerState( String layerId, String stateId ) {
        try {
            JSONObject layer = _layersBySha.get( stateId );
            if ( layer == null ) {
                return null;
            }
            return getLayerConfiguration( layerId, layer.getJSONObject("public") )
                    .getExplicitConfiguration()
                    .getJSONObject("public"); // only return public node
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
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
                String fileContent = _configService.replaceProperties(file);
				JSONArray contents = new JSONArray( new JSONTokener(fileContent) );
                for ( int i=0; i<contents.length(); i++ ) {
                    if( contents.get(i) instanceof JSONObject ) {
                        JSONObject layerJSON = contents.getJSONObject(i);

						layerJSON = addLayerContext(layerJSON);
                        _layersById.put( layerJSON.getString( LayerConfiguration.LAYER_ID.getName() ), layerJSON );
                        _layers.add( layerJSON );
                    }
                }
			} catch (JSONException e) {
				LOGGER.error("Layer configuration file {} was not valid JSON.", file, e);
			} catch (ConfigException e) {
                LOGGER.error("Unable to replace properties in layer file {}.", file, e);
            }
        }
	}

	protected JSONObject addLayerContext(JSONObject layer) throws JSONException {
		JSONObject publicObj = layer.optJSONObject("public");

		if (publicObj != null) {
			switch (publicObj.optString("domain")) {
				case "kml":
					layer = addKMLLayerContext(layer);
					break;
			}
		}

		return layer;
	}

	protected JSONObject addKMLLayerContext(JSONObject kmlLayer) {
		try {
			JSONObject publicObj = kmlLayer.optJSONObject("public");
			JSONArray kmlSets = publicObj.getJSONArray("kml");
			long minTime = Long.MAX_VALUE;
			long maxTime = Long.MIN_VALUE;

			for (int i = 0; i < kmlSets.length(); i++) {
				JSONObject kmlSet = kmlSets.getJSONObject(i);
				String resourceDir = kmlSet.optString("dir", null);

				if (resourceDir != null) {
					// Scan this directory for kml files
					File dir = new File(getClass().getClassLoader().getResource(resourceDir).getFile());
					FileFilter filter = new WildcardFileFilter(kmlSet.getString("fileTemplate"));
					File[] kmlFiles = dir.listFiles(filter);
					JSONArray files = new JSONArray();

					for (int j = 0; j < kmlFiles.length; j++) {
						File kmlFile = kmlFiles[j];

						// Extract month and year from file name. For now always expect file to be named
						// MM-yyyy* Force timezon to avoid offsetting to previous month
						String dateText = kmlFile.getName().substring(0,7);
						DateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.CANADA);
						format.setTimeZone(TimeZone.getTimeZone("Canada/Eastern"));
						long dateTime = format.parse("01-" + dateText).getTime();

						JSONObject urlObject = new JSONObject();

						urlObject.put("fileName", kmlFile.getName());
						urlObject.put("date", dateTime);
						files.put(urlObject);

						if (dateTime < minTime) {
							minTime = dateTime;
						}

						if (dateTime > maxTime) {
							maxTime = dateTime;
						}
					}

					// Set URL to JSONArray of URL date pairs
					kmlSet.put("files", files);
				}
			}

			// Update the layer meta information
			JSONObject metaL1 = publicObj.optJSONObject("meta");

			if (metaL1 == null) {
				metaL1 = new JSONObject();
				publicObj.put("meta", metaL1);
			}

			JSONObject metaL2 = metaL1.optJSONObject("meta");

			if (metaL2 == null) {
				metaL2 = new JSONObject();
				metaL1.put("meta", metaL2);
			}

			metaL2.put("rangeMax", maxTime);
			metaL2.put("rangeMin", minTime);

		} catch (Exception e) {
			LOGGER.error("There was a problem reading kml files", e);
		}

		return kmlLayer;
	}
}
