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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.util.JsonUtilities;

public class LayerServiceImpl implements LayerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerServiceImpl.class);



    private List<LayerInfo>         _layers;
    private Map<String, LayerInfo>  _layersById;
    private Map<String, JSONObject> _metaDataCache;
    private Map<UUID, JSONObject>   _configurationssByUuid;



	@Inject
    public LayerServiceImpl (@Named("com.oculusinfo.tile.layer.config") String layerConfigurationLocation) {
        _layers = new ArrayList<>();
        _layersById = new HashMap<>();
        _metaDataCache = new HashMap<>();
        _configurationssByUuid = new HashMap<>();
        readConfigFiles(getConfigurationFiles(layerConfigurationLocation));
    }



	@Override
	public List<LayerInfo> listLayers () {
	    return _layers;
	}

	@Override
	public PyramidMetaData getMetaData (String layerId) {
        try {
            JSONObject metadata = _metaDataCache.get(layerId);
            if (metadata == null){
                LayerConfiguration config = getRenderingConfiguration(layerId, -1);
                PyramidIO pyramidIO = config.produce(PyramidIO.class);
                String s = pyramidIO.readMetaData(layerId);

                metadata = new JSONObject(s);
                _metaDataCache.put(layerId, metadata);
            }
            return new PyramidMetaData(metadata);
        } catch (ConfigurationException e) {
            LOGGER.error("Couldn't figure out how to read metadata for {}", layerId, e);
        } catch (JSONException e) {
            LOGGER.error("Metadata file for layer is missing or corrupt: {}", layerId, e);
        } catch (IOException e) {
            LOGGER.error("Couldn't read metadata: {}", layerId, e);
        }
        return new PyramidMetaData(new JSONObject());
	}

	@Override
	public UUID configureLayer (JSONObject overrideConfiguration) {
        try {
            String layer = overrideConfiguration.getString(LayerConfiguration.LAYER_NAME.getName());

            LayerInfo info = _layersById.get(layer);
            if (null == info) {
                return null;
            }
            // TODO: Merge input configuration with the one we know about from our own configuration
            // Merge the passed-in configuration with the stored one from our general configuration.
            JSONObject baseConfiguration = null;
            for (JSONObject possibleBase: info.getRendererConfigurations()) {
                // TODO: Figure out which one it came from.
            }
            JSONObject configuration = new JSONObject(metadata.getRawData(), names);
            
            UUID uuid = UUID.randomUUID();
            _configurationssByUuid.put(uuid, configuration);
            _latestIDMap.put(layer, id);

            // Determine the pyramidIO, so we can get the metaData
            LayerConfiguration config = getLayerConfiguration();
            config.readConfiguration(options);
            PyramidIO pyramidIO = config.produce(PyramidIO.class);

            // Initialize the pyramid for reading
            JSONObject initJSON = config.getProducer(PyramidIO.class).getPropertyValue(PyramidIOFactory.INITIALIZATION_DATA);
            if (null != initJSON) {
                int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
                int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
                Properties initProps = JsonUtilities.jsonObjToProperties(initJSON);
                pyramidIO.initializeForRead(layer, width, height, initProps);
            }

            PyramidMetaData metadata = getMetadata(layer, pyramidIO);

            // Construct our return object
            String[] names = JSONObject.getNames(metadata.getRawData());
            JSONObject result = new JSONObject(metadata.getRawData(), names);

            result.put("layer", layer);
            result.put("id", id);
            result.put("tms", hostUrl + "tile/" + id.toString() + "/");
            result.put("apertureservice", "/tile/" + id.toString() + "/");

            TileDataImageRenderer renderer = config.produce(TileDataImageRenderer.class);
            result.put("imagesPerTile", renderer.getNumberOfImagesPerTile(metadata));

            System.out.println("UUID Count after "+layer+": " + _uuidToOptionsMap.size());
            return result;
        } catch (ConfigurationException e) {
            _logger.warn("Configuration exception trying to apply layer parameters to json object.", e);
            return new JSONObject();
        } catch (JSONException e) {
            _logger.warn("Failed to apply layer parameters to json object.", e);
            return new JSONObject();
        } 
	}

	@Override
	public LayerConfiguration getRenderingConfiguration (String layerId, int level) {
	    // TODO Auto-generated method stub
	    return null;
	}

	@Override
	public void forgetConfiguration (UUID uuid) {
	    // TODO Auto-generated method stub
	    
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
    			JSONArray configArray = JsonUtilities.readJSONArray(new FileReader(file));
    			for (int i=0; i<configArray.length(); ++i) {
    				addConfiguration(configArray.getJSONObject(i));
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

    private void addConfiguration (JSONObject configJSON) {
        LayerInfo info = new LayerInfo(configJSON);
    	_layers.add(info);
    	_layersById.put(info.getID(), info);
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
