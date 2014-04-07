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
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.name.Named;

public class LayerServiceImpl implements LayerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerServiceImpl.class);



    private List<LayerInfo> _layers;



    public LayerServiceImpl (@Named("com.oculusinfo.tile.layer.config") String layerConfigurationLocation) {
        _layers = new ArrayList<>();
        readLayerConfiguration(layerConfigurationLocation);
    }

    private void readLayerConfiguration (String layerConfigurationLocation) {
        File location = new File(layerConfigurationLocation);
        if (location.exists()) {
            if (location.isDirectory()) {
                for (File contents: location.listFiles())
                    readConfigFile(contents);
            } else {
                readConfigFile(location);
            }
        }
    }

    private void readConfigFile (File contents) {
        JSONTokener tokener;
        try {
            tokener = new JSONTokener(new FileReader(contents));
        } catch (FileNotFoundException e1) {
            LOGGER.error("Cannot find layer configuration file {} ", contents);
            return;
        }

        try {
            JSONObject rawConfig = new JSONObject(tokener);
        } catch (JSONException e) {
            try {
                JSONArray rawArray = new JSONArray(tokener);
            } catch (JSONException e1) {
                LOGGER.error("Layer configuration file {} was not valid JSON.", contents);
            }
        }
    }
}
