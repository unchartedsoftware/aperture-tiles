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
         './layer/view/client/ClientLayerFactory',         
         './layer/controller/LayerControls',
         './layer/controller/UIMediator'
        ],

        function (FileLoader, 
        	      Map,
                  AllLayers,
                  ServerLayerFactory,
                  ClientLayerFactory,
                  LayerControls,
                  UIMediator) {
            "use strict";

            var mapFile = "./data/map.json",
                majorCitiesFile = "./data/majorCities.json",
                uiMediator,
                cloneObject;

            cloneObject = function (base) {
                var result, key;

                if ($.isArray(base)) {
                    result = [];
                } else {
                    result = {};
                }

                for (key in base) {
                    if (base.hasOwnProperty(key)) {
                        if ("object" === typeof(base[key])) {
                            result[key] = cloneObject(base[key]);
                        } else {
                            result[key] = base[key];
                        }
                    }
                }

                return result;
            };

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFile, majorCitiesFile, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var allLayers = new AllLayers(),
                worldMap,
                majorCitiesDropDown;



                // We need to initialize the map first, because that 
                // initializes aperture, so sets our REST parameters; until we 
                // do this, no server calls will work.
                worldMap = new Map("map", jsonDataMap[mapFile]);

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
                

                // Request all layers the server knows about.  Once we get 
                // those, we can start drawing them.
                allLayers.requestLayers(function (layers) {
                    // For now, just take the first node specifying axes.
                    // Eventually, we should let the user choose among them.
                    var
                    rootMapNode = allLayers.filterAxisLayers(layers)[0],
                    axes = rootMapNode.axes,
                    clientLayers = allLayers.filterLeafLayers(
                        rootMapNode,
                        function (layer) {
                            var clientLayer = false;
                            layer.renderers.forEach(function (renderer, index, renderers) {
                                if ("client" === renderer.domain) {
                                    clientLayer = true;
                                    return;
                                }
                            });
                            return clientLayer;
                        }
                    ).map(function (layer, index, layersList) {
                        // For now, just use the first client configuration we find
                        var config;

                        layer.renderers.forEach(function (renderer, index, renderers) {
                            if ("client" === renderer.domain) {
                                config = cloneObject(renderer);
                                config.layer = layer.id;
                                config.name = layer.name;
                                return;
                            }
                        });

                        return config;
                    }),
                    serverLayers =  allLayers.filterLeafLayers(
                        rootMapNode,
                        function (layer) {
                            var serverLayer = false;
                            layer.renderers.forEach(function (renderer, index, renderers) {
                                if ("server" === renderer.domain) {
                                    serverLayer = true;
                                    return;
                                }
                            });
                            return serverLayer;
                        }
                    ).map(function(layer, index, layersList) {
                        // For now, just use the first server configuration we find
                        var config;

                        layer.renderers.forEach(function (renderer, index, renderers) {
                            if ("server" === renderer.domain) {
                                config = cloneObject(renderer);
                                config.layer = layer.id;
                                config.name = layer.name;
                                return;
                            }
                        });

                        return config;
                    });

                    // Set up our map axes
                    worldMap.setAxisSpecs(axes);

                    uiMediator = new UIMediator();

                    // Create client and server layers
                    ClientLayerFactory.createLayers(clientLayers, uiMediator, worldMap);
                    ServerLayerFactory.createLayers(serverLayers, uiMediator, worldMap);

                    // Bind layer controls to the state model.
                    new LayerControls().initialize( uiMediator.getLayerStateMap() );

                    // Trigger the initial resize event to resize everything
                    $(window).resize();
                });
            });
        });
