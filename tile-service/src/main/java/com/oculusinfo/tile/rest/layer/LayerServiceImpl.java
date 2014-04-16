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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.EmptyConfigurableFactory;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.binning.io.RequestParamsFactory;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.init.FactoryProvider;
import com.oculusinfo.tile.init.providers.CachingLayerConfigurationProvider;
import com.oculusinfo.tile.rendering.ImageRendererFactory;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.tile.caching.CachingPyramidIO.LayerDataChangedListener;
import com.oculusinfo.tile.util.JsonUtilities;

@Singleton
public class LayerServiceImpl implements LayerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerServiceImpl.class);



    private List<LayerInfo>                     _layers;
    private Map<String, LayerInfo>              _layersById;
    private Map<String, JSONObject>             _metaDataCache;
    private Map<UUID, JSONObject>               _configurationssByUuid;
    private FactoryProvider<LayerConfiguration> _layerConfigurationProvider;



	@Inject
    public LayerServiceImpl (@Named("com.oculusinfo.tile.layer.config") String layerConfigurationLocation,
                             FactoryProvider<LayerConfiguration> layerConfigurationProvider) {
        _layers = new ArrayList<>();
        _layersById = new HashMap<>();
        _metaDataCache = new HashMap<>();
        _configurationssByUuid = new HashMap<>();
        _layerConfigurationProvider = layerConfigurationProvider;

        if (_layerConfigurationProvider instanceof CachingLayerConfigurationProvider) {
            ((CachingLayerConfigurationProvider) _layerConfigurationProvider).addLayerListener(new LayerDataChangedListener () {
                public void onLayerDataChanged (String layerId) {
                    _metaDataCache.remove(layerId);
                }
            });
        }

        readConfigFiles(getConfigurationFiles(layerConfigurationLocation));
    }



	@Override
	public List<LayerInfo> listLayers () {
	    return _layers;
	}

    @Override
    public PyramidMetaData getMetaData (String layerId) {
        try {
            LayerConfiguration config = getLayerConfiguration(layerId);
            PyramidIO pyramidIO = config.produce(PyramidIO.class);
            return getMetaData(layerId, pyramidIO);
        } catch (ConfigurationException e) {
            LOGGER.error("Couldn't determine pyramid I/O method for {}", layerId, e);
            return null;
        }
    }
    
    private PyramidMetaData getMetaData (String layerId, PyramidIO pyramidIO) {
        try {
            JSONObject metadata = _metaDataCache.get(layerId);
            if (metadata == null){
                String s = pyramidIO.readMetaData(layerId);

                metadata = new JSONObject(s);
                _metaDataCache.put(layerId, metadata);
            }
            return new PyramidMetaData(metadata);
        } catch (JSONException e) {
            LOGGER.error("Metadata file for layer is missing or corrupt: {}", layerId, e);
        } catch (IOException e) {
            LOGGER.error("Couldn't read metadata: {}", layerId, e);
        }
        return new PyramidMetaData(new JSONObject());
    }

    /*
     * Gets the base configuration - the combination of the renderer configuration and the data configuration.
     */
	private JSONObject getBaseConfiguration (String layerId, String rendererType, boolean choiceIsError) {
        LayerInfo info = _layersById.get(layerId);
        if (null == info) {
            throw new IllegalArgumentException("Attempt to configure unknown layer "+layerId);
        }

        // Figure out which renderer config to use
        List<JSONObject> rendererConfigs = info.getRendererConfigurations();
        JSONObject rendererConfig = null;
        if (0 == rendererConfigs.size()) {
            throw new IllegalArgumentException("No configurations found for layer "+layerId);
        }
        if (1 == rendererConfigs.size()) {
            // Only one possible base configuration (the usual case, at the moment)
            rendererConfig = rendererConfigs.get(0);
        } else if (null == rendererType) {
            if (choiceIsError) {
                throw new IllegalArgumentException("No way to choose between "+rendererConfigs.size()+" configurations - no renderer given");
            }
            // Just pick the first one - we're not actually rendering, so it shouldn't matter.
            rendererConfig = rendererConfigs.get(0);
        } else {
            for (JSONObject config: rendererConfigs) {
                try {
                    ImageRendererFactory baseFactory = new ImageRendererFactory(null, null);
                    baseFactory.readConfiguration(ConfigurableFactory.getLeafNode(config,
                                                                                  LayerConfiguration.RENDERER_PATH));
                    String configType = baseFactory.getPropertyValue(ImageRendererFactory.RENDERER_TYPE);
                    if (rendererType.equals(configType)) {
                        rendererConfig =  config;
                        break;
                    }
                } catch (ConfigurationException e) {
                    LOGGER.warn("Could not determine renderer from configuration {}", config, e);
                }
            }
            throw new IllegalArgumentException("Attempt to configure unknown renderer "+rendererType);
        }

        // Combine the renderer configuration with the data configuration
        JSONObject dataConfig = info.getDataConfiguration();
        JSONObject totalConfig = JsonUtilities.deepClone(dataConfig);
        try {
            totalConfig.put("renderer", JsonUtilities.deepClone(rendererConfig));
        } catch (JSONException e) {
            LOGGER.warn("Attempt to combine renderer and data configurations failed for layer {}", layerId, e);
        }
        return totalConfig;
	}

	@Override
	public UUID configureLayer (String layerId, JSONObject overrideConfiguration) {
        // Figure out which renderer to match, if a choice is necessary
	    //
        // This is necessary because we need to return the total number of
        // images available, which at the moment, only the renderer knows. If we
        // can figure out a way to do this without going through the renderer
        // (say, using the serializer), we can get rid of this part where it has
        // to find a renderer, which really shouldn't be at this point in the
        // code.
        ImageRendererFactory overrideFactory = new ImageRendererFactory(null, null);
        String overrideRenderer = null;
        boolean rendererChoiceIsError = false;
        try {
            overrideFactory.readConfiguration(ConfigurableFactory.getLeafNode(overrideConfiguration,
                                                                              LayerConfiguration.RENDERER_PATH));
            overrideRenderer = overrideFactory.getPropertyValue(ImageRendererFactory.RENDERER_TYPE);
        } catch (ConfigurationException e) {
            // No renderer is allowed, if there is only one renderer listed; otherwise, it's an error.
            rendererChoiceIsError = true;
        }

        // Get our base configuration - a combination of renderer and data configurations.
        JSONObject configuration = getBaseConfiguration(layerId, overrideRenderer, rendererChoiceIsError);
        JsonUtilities.overlayInPlace(configuration, overrideConfiguration);
        
        UUID uuid = UUID.randomUUID();
        _configurationssByUuid.put(uuid, configuration);

        return uuid;
	}

	/**
	 * Wraps the options and query {@link JSONObject}s together into a new object.
	 */
	private JSONObject mergeQueryConfigOptions(JSONObject options, JSONObject query) {
		JSONObject ret = new JSONObject();
		try {
			if (options != null)
				ret.put("config", options);
			if (query != null)
				ret.put("request", query);
		}
		catch (Exception e) {
			LOGGER.error("Couldn't merge query options with main options.", e);
		}
		return ret;
	}
	

	@Override
	public LayerConfiguration getRenderingConfiguration (UUID uuid, TileIndex tile, JSONObject requestParams) {
	    try {
			//the root factory that does nothing
			EmptyConfigurableFactory rootFactory = new EmptyConfigurableFactory(null, null, null);
			
			//add another factory that will handle query params
			RequestParamsFactory queryParamsFactory = new RequestParamsFactory(null, rootFactory, Collections.singletonList("request"));
			rootFactory.addChildFactory(queryParamsFactory);
			
			//add the layer configuration factory under the path 'config'
	        ConfigurableFactory<LayerConfiguration> factory = _layerConfigurationProvider.createFactory(rootFactory, Collections.singletonList("config"));
	        rootFactory.addChildFactory(factory);
	        
	        JSONObject rawConfiguration = _configurationssByUuid.get(uuid);
	        if (null == rawConfiguration)
	            throw new IllegalArgumentException("Unknown configuration: "+uuid);

	        rootFactory.readConfiguration(mergeQueryConfigOptions(rawConfiguration, requestParams));
	        LayerConfiguration config = rootFactory.produce(LayerConfiguration.class);
            String layerId = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
            PyramidIO pyramidIO = config.produce(PyramidIO.class);

	        // Initialize the PyramidIO for reading
            JSONObject initJSON = config.getProducer(PyramidIO.class).getPropertyValue(PyramidIOFactory.INITIALIZATION_DATA);
            if (null != initJSON) {
                int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
                int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
                Properties initProps = JsonUtilities.jsonObjToProperties(initJSON);

                pyramidIO.initializeForRead(layerId, width, height, initProps);
            }

            // Set level-specific properties in the configuration
            if (null != tile) {
                PyramidMetaData metadata = getMetaData(layerId, pyramidIO);
                config.setLevelProperties(tile,
                                          metadata.getLevelMinimum(tile.getLevel()),
                                          metadata.getLevelMaximum(tile.getLevel()));
            }

            return config;
	    } catch (ConfigurationException e) {
	        LOGGER.warn("Error configuring rendering for {}", uuid, e);
	        return null;
	    }
	}

	/*
     * Get a layer configuration not suitable for rendering.
     * 
     * For internal use only, basically just for getting metadata, which
     * shouldn't depend on anything but the pyramidIO.
     * 
     * While there is no theoretical reason the PyramidIO should necessarily be
     * the same across all possible ways of looking at a layer, for the moment,
     * we simply stipulate that it will be so this will work. If you want to
     * look at some data both live and batched (to compare, say), configure them
     * as separate layers, with separate IDs.
     */
	private LayerConfiguration getLayerConfiguration (String layerId) {
        try {
			//the root factory that does nothing
			EmptyConfigurableFactory rootFactory = new EmptyConfigurableFactory(null, null, null);
			
			//add another factory that will handle query params
			RequestParamsFactory queryParamsFactory = new RequestParamsFactory(null, rootFactory, Collections.singletonList("request"));
			rootFactory.addChildFactory(queryParamsFactory);
			
			//add the layer configuration factory under the path 'config'
            ConfigurableFactory<LayerConfiguration> factory = _layerConfigurationProvider.createFactory(rootFactory, Collections.singletonList("config"));
            rootFactory.addChildFactory(factory);
            
            JSONObject baseConfiguration = getBaseConfiguration(layerId, null, false);
            if (null == baseConfiguration)
                throw new IllegalArgumentException("Unknown configuration: "+layerId);

            rootFactory.readConfiguration(mergeQueryConfigOptions(baseConfiguration, null));
            LayerConfiguration config = rootFactory.produce(LayerConfiguration.class);

            // Initialize the PyramidIO for reading
            JSONObject initJSON = config.getProducer(PyramidIO.class).getPropertyValue(PyramidIOFactory.INITIALIZATION_DATA);
            if (null != initJSON) {
                int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
                int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
                Properties initProps = JsonUtilities.jsonObjToProperties(initJSON);

                PyramidIO pyramidIO = config.produce(PyramidIO.class);
                pyramidIO.initializeForRead(layerId, width, height, initProps);
            }
            return config;
        } catch (ConfigurationException e) {
            LOGGER.warn("Error configuring rendering for {}", layerId, e);
            return null;
        }
	}

	@Override
	public void forgetConfiguration (UUID uuid) {
	    _configurationssByUuid.remove(uuid);
	}



	// ////////////////////////////////////////////////////////////////////////
	// Section: Configuration reading methods
	//
	private File[] getConfigurationFiles (String location) {
    	try {
	    	// Find our configuration file.
	    	URI path = null;
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

    private void readConfigFiles (File[] files) {
		for (File file: files) {
			try {
			    JSONObject contents = new JSONObject(new JSONTokener(new FileReader(file)));
			    JSONArray configurations = contents.getJSONArray("layers");
    			for (int i=0; i<configurations.length(); ++i) {
    		        LayerInfo info = new LayerInfo(configurations.getJSONObject(i));
    				addConfiguration(info);
    			}
	    	} catch (FileNotFoundException e1) {
	    		LOGGER.error("Cannot find layer configuration file {} ", file);
	    		return;
	    	} catch (JSONException e1) {
	    		LOGGER.error("Layer configuration file {} was not valid JSON.", file);
	    	}
		}
		debugConfiguration();
    }

    private void addConfiguration (LayerInfo info) {
    	_layers.add(info);
    	_layersById.put(info.getID(), info);
    	for (LayerInfo child: info.getChildren()) {
    	    addConfiguration(child);
    	}
    }

    private void debugConfiguration () {
    	System.out.println("Configuration layers for server:");
    	for (LayerInfo layer: _layers) {
    		debugLayer(layer, "  ");
    	}
    }
    private void debugLayer (LayerInfo layer, String prefix) {
    	System.out.println(prefix+layer.getID()+": "+layer.getName());
    	for (LayerInfo child: layer.getChildren()) {
    		debugLayer(child, prefix+"  ");
    	}
    }
}
