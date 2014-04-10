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
         './layer/AllLayers',
         './layer/view/server/ServerLayerFactory',
         './layer/view/client/ClientLayerFactory'
        ],

        function (FileLoader, 
        	      Map,
                  AllLayers,
                  ServerLayerFactory,
                  ClientLayerFactory) {
            "use strict";

            var mapFile = "./data/map.json";

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFile, function (jsonDataMap) {
                var allLayers = new AllLayers(),
                    // Create world map and axes from json file under mapFile
                    worldMap;

                // We need to initialize the map first, because that 
                // initializes aperture, so sets our REST parameters; until we 
                // do this, no server calls will work.
                worldMap = new Map("map", jsonDataMap[mapFile]);

                // Request all layers the server knows about.  Once we get 
                // those, we can start drawing them.
                allLayers.requestLayers(function (layers) {
                    var clientLayers = allLayers.filterLeafLayers(layers, function (layerConfig) {
                        return false;
                    }),
                        serverLayers = allLayers.filterLeafLayers(layers, function(layerConfig) {
                            return true;
                        });

                    // Create client and server layers
                    ClientLayerFactory.createLayers(clientLayers, worldMap);
                    ServerLayerFactory.createLayers(serverLayers, worldMap);

                    // Trigger the initial resize event to resize everything
                    $(window).resize();
                });
            });
        });
