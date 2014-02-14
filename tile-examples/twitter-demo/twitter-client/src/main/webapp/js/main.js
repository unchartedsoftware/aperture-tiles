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
         './layer/view/server/ServerLayer',
         './layer/view/client/impl/TopTextSentimentBars',
         './layer/view/client/impl/HashTagsByTime',
         './layer/controller/LayerControls',
         './layer/controller/UIMediator',
         './layer/view/client/CarouselLayer',
         './layer/view/client/data/DataTrackerCache',
        ],

        function (FileLoader, 
        	  Map, 
        	  ServerLayer,
                  TopTextSentimentBars,
                  HashTagsByTime,
                  LayerControls,
                  ServerLayerUiMediator,
                  Carousel, 
                  DataTrackerCache) {
            "use strict";

            var serverLayerFile = "./data/layers.json",
            	mapFile = "./data/map.json",
            	clientLayerFile = "./data/renderLayers.json",
                majorCitiesFile = "./data/majorCities.json";

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFile, serverLayerFile, clientLayerFile, majorCitiesFile, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    serverLayers,
                    mapLayerState,
                    majorCitiesDropDown,
                    topTextSentimentBars,
                    hashTagsByTime;

                // Create world map and axes from json file under mapFile
                worldMap = new Map("map", jsonDataMap[mapFile]);

                // Set up a debug layer
                // debugLayer = new DebugLayer();
                // debugLayer.addToMap(worldMap);

                // Set up client-renderers
                topTextSentimentBars = new TopTextSentimentBars();
                hashTagsByTime = new HashTagsByTime();

                // Get required data trackers, upon receiving, create carousel view controller
                DataTrackerCache.get(jsonDataMap[clientLayerFile], function(dataTrackers) {
                    new Carousel( {
                        map: worldMap.map,
                        views: [
                            {
                                dataTracker: dataTrackers[0],
                                renderer: topTextSentimentBars
                            },
                            {
                                dataTracker: dataTrackers[0],
                                renderer: hashTagsByTime
                            }
                        ]});
                });

                // Set up server-rendered display layers
                serverLayers = new ServerLayer(FileLoader.downcaseObjectKeys(jsonDataMap[serverLayerFile] ));
                serverLayers.addToMap(worldMap);

                // Populate the map layer state object with server layer data, and enable
                // listeners that will push state changes into the layers.
                mapLayerState = {};
                new ServerLayerUiMediator().initialize(mapLayerState, serverLayers, worldMap);

                // Bind layer controls to the state model.
                new LayerControls().initialize(mapLayerState);

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
                worldMap.map.zoomTo( 40, -95, 4 );
                
                // Trigger the initial resize event to resize everything
                $(window).resize();
            });
        });
