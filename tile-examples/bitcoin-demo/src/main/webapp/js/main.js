/**
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

require(['./fileloader',
         './map',
         './serverrenderedmaplayer',
		 './client-rendering/TextScoreRenderer',
		 './ui/layercontrols',
         './serverlayeruimediator',
         './axis/AxisUtil',
         './axis/Axis'],

        function (FileLoader, 
        		  Map, 
        		  ServerLayer,
                  TextScoreRenderer, 
                  LayerControls,
                  ServerLayerUiMediator,
                  AxisUtil, 
                  Axis
                  ) {
            "use strict";

            var sLayerFileId = "./data/layers.json",
            	mapFileId = "./data/map.json",
            	cLayerFileId = "./data/renderLayers.json";

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFileId, sLayerFileId, cLayerFileId, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    serverLayers,
                    axisSpecs,
                    mapSpecs,
                    axisSpec,
                    axes = [],
                    mapLayerState,
                    renderLayerSpecs,
                    renderLayerSpec,
                    tooltipFcn,
                    i,
                    layerId,
                    layerName
                ;

                // Create world map from json file under mapFileId
                mapSpecs = $.grep(jsonDataMap[mapFileId], function( element ) {
                    // skip any axis config objects

                    return !(element.hasOwnProperty("AxisConfig"));
                });

                axisSpecs = $.grep(jsonDataMap[mapFileId], function( element ) {
                    // skip any axis config objects
                    return (element.hasOwnProperty("AxisConfig"));
                });

                // create world map from json file under mapFileId
                worldMap = new Map("map", mapSpecs);

                // create axes
                for (i=0; i<axisSpecs.length; ++i) {
                    axisSpec = axisSpecs[i].AxisConfig;
                    axisSpec.parentId = worldMap.mapSpec.id;
                    axisSpec.olMap = worldMap.map.olMap_;
                    axes.push( new Axis(axisSpec));
                }

                // Set up a debug layer
                // debugLayer = new DebugLayer();
                // debugLayer.addToMap(worldMap);


                tooltipFcn = function (text) {
                    if (text) {
                        $('#hoverOutput').html(text);
                    } else {
                        $('#hoverOutput').html('');
                   }
                };

				// Set up server-rendered display layers
                serverLayers = new ServerLayer(FileLoader.downcaseObjectKeys(jsonDataMap[sLayerFileId] ));
                serverLayers.addToMap(worldMap);

                // Populate the map layer state object with server layer data, and enable
                // listeners that will push state changes into the layers.
                mapLayerState = {};
                new ServerLayerUiMediator().initialize(mapLayerState, serverLayers, worldMap);

                // Bind layer controls to the state model.
                new LayerControls().initialize(mapLayerState);
                

                /*
                setTimeout(function () {
                    console.log(Class.getProfileInfo());
                }, 10000);
                */
            });
        });
