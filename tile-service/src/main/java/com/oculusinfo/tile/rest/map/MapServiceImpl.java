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
package com.oculusinfo.tile.rest.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class MapServiceImpl implements MapService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapServiceImpl.class);



	private Map<String, JSONObject> _mapConfigurationsById;
	private JSONArray               _mapConfigurations;



	@Inject
	public MapServiceImpl (@Named("com.oculusinfo.tile.map.config") String mapConfigurationLocation) {
	    _mapConfigurationsById = new HashMap<String, JSONObject>();
	    _mapConfigurations = new JSONArray();

	    readConfigFiles(getConfigurationFiles(mapConfigurationLocation));
	}



	@Override
	public JSONArray getMaps () {
		return _mapConfigurations;
	}

	@Override
	public JSONObject getMap (String mapId) {
		return _mapConfigurationsById.get(mapId);
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
				path = MapServiceImpl.class.getResource(location).toURI();
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
		    JSONObject mapConfiguration = null;
			try {
				mapConfiguration = new JSONObject(new JSONTokener(new FileReader(file)));
            } catch (FileNotFoundException e) {
                LOGGER.error("Cannot find map configuration file {} ", file, e);
            } catch (JSONException e) {
                LOGGER.error("Map configuration file {} was not valid JSON.", file, e);
            }
			if (null != mapConfiguration) {
			    try {
				_mapConfigurationsById.put(mapConfiguration.getString("id"), mapConfiguration);
	            } catch (JSONException e) {
	                LOGGER.warn("Map configuration file {} had no ID - configuration won't be accessible by id.", file, e);
	            }
			    _mapConfigurations.put(mapConfiguration);
			}
		}
		debugConfiguration();
	}

	private void debugConfiguration () {
		System.out.println("Map configurations for server:");
		for (String mapId: _mapConfigurationsById.keySet()) {
			System.out.println("Map "+mapId+":");
			System.out.println(_mapConfigurationsById.get(mapId));
		}
	}
}
