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
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

require(['./FileLoader',
         './map/Map',
         './layer/view/server/ServerLayerFactory',
         './layer/view/client/ClientLayerFactory'
        ],

        function (FileLoader, 
        	      Map,
                  ServerLayerFactory,
                  ClientLayerFactory) {
            "use strict";

            var mapFile = "./data/map.json",
                majorCitiesFile = "./data/majorCities.json",
                layersFile = "./data/layers.json";

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFile, layersFile, majorCitiesFile, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    majorCitiesDropDown;

                // Create world map and axes from json file under mapFile
                worldMap = new Map("map", jsonDataMap[mapFile]);

                // Create client and server layers
                ClientLayerFactory.createLayers(jsonDataMap[layersFile].ClientLayers, worldMap);
                ServerLayerFactory.createLayers(jsonDataMap[layersFile].ServerLayers, worldMap);

                // Add major cities entries to zoom select box
                majorCitiesDropDown = $("#select-city-zoom");
                $.each(jsonDataMap[majorCitiesFile], function(key) {
                    majorCitiesDropDown.append(
                        $('<option></option>').val(key).html(this.text)
                    );
                });
                // Set city zoom callback function
                majorCitiesDropDown.change( function() {
                    var majorCitiesMap = jsonDataMap[majorCitiesFile];
                    worldMap.map.zoomTo( majorCitiesMap[this.value].lat,
                                         majorCitiesMap[this.value].long, 8);
                });

                // Zoom to the area of the world with the data.
                worldMap.map.zoomTo( -15, -60, 3 );
                
                // Trigger the initial resize event to resize everything
                $(window).resize();
            });
        });
