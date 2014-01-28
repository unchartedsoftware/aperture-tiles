/**
 * Copyright (c) 2013 Oculus Info Inc.
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
         './client-rendering/TextScoreLayer',
         './client-rendering/DebugLayer',
         './axis/AxisUtil',
         './axis/Axis',
         './profileclass',
         './ui/layercontrols',
         './serverlayeruimediator'],

        function (FileLoader, Map, ServerLayer,
                  TextScoreLayer, DebugLayer, AxisUtil, Axis, Class,
                  LayerControls, ServerLayerUiMediator) {
            "use strict";

            var sLayerFileId = "./data/layers.json"
                // Uncomment for geographic data
                ,mapFileId = "./data/geomap.json"
                // Uncomment for non-geographic data
                //,mapFileId = "./data/emptymap.json"
                ,cLayerFileId = "./data/renderLayers.json";

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFileId, sLayerFileId, cLayerFileId, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    serverLayers,
                    renderLayer,
                    renderLayerSpecs,
                    renderLayerSpec,
                    layerId,
                    layerName,
                    layerControl,
                    i,
                    layerControlSet,
                    tooltipFcn,
                    xAxisSpec,
                    yAxisSpec,
                    xAxis,
                    yAxis,
                    redrawAxes,
                    mapLayerState
                ;

                // create world map from json file under mapFileId
                worldMap = new Map("map", jsonDataMap[mapFileId]);


                xAxisSpec = {

                    title: "Longitude",
                    parentId: worldMap.mapSpec.id,
                    id: "map-x-axis",
                    olMap: worldMap.map.olMap_,
                    min: worldMap.mapSpec.options.mapExtents[0],
                    max: worldMap.mapSpec.options.mapExtents[2],
                    intervalSpec: {
                        type: "fixed",
                        value: 60,
                        pivot: 0,
                        allowScaleByZoom: true,
                        isMercatorProjected: true
                    },
                    unitSpec: {
                        type: 'degrees',
                        divisor: undefined,
                        decimals: 2,
                        allowStepDown: false
                    },
                    position: 'bottom',
                    repeat: true

                };

                yAxisSpec = {

                    title: "Latitude",
                    parentId: worldMap.mapSpec.id,
                    id: "map-y-axis",
                    olMap: worldMap.map.olMap_,
                    min: worldMap.mapSpec.options.mapExtents[1],
                    max: worldMap.mapSpec.options.mapExtents[3],
                    intervalSpec: {
                        type: "fixed",
                        value: 30,
                        pivot: 0,
                        allowScaleByZoom: true,
                        isMercatorProjected: true
                    },
                    unitSpec: {
                        type: 'degrees',
                        divisor: undefined,
                        decimals: 2,
                        allowStepDown: false
                    },
                    position: 'left',
                    repeat: false
                };

                xAxis = new Axis(xAxisSpec);
                yAxis = new Axis(yAxisSpec);

                redrawAxes = function() {
                    xAxis.redraw();
                    yAxis.redraw();
                };


                worldMap.map.olMap_.events.register('mousemove', worldMap.map.olMap_, function(e) {

                    var xVal = xAxis.getAxisValueForPixel(e.xy.x),
                        yVal = yAxis.getAxisValueForPixel(e.xy.y);

                    // set map "title" to display mouse coordinates
                    $('#' + worldMap.mapSpec.id).prop('title', 'x: ' + xVal + ', y: ' + yVal);

                    redrawAxes();

                    return true;
                });

                worldMap.map.on('panend', redrawAxes);
                worldMap.map.on('zoom',   redrawAxes);

                // Set up server-rendered display layers
                serverLayers = new ServerLayer(FileLoader.downcaseObjectKeys(jsonDataMap[sLayerFileId] ));
                serverLayers.addToMap(worldMap);

                // Set up a debug layer
                // debugLayer = new DebugLayer();
                // debugLayer.addToMap(worldMap);

                // Set up client-rendered layers
                renderLayerSpecs = jsonDataMap[cLayerFileId];
                tooltipFcn = function (text) {
                    if (text) {
                        $('#hoverOutput').html(text);
                    } else {
                        $('#hoverOutput').html('');
                    }
                };

                for (i=0; i<renderLayerSpecs.length; ++i) {

                    renderLayerSpec = FileLoader.downcaseObjectKeys(renderLayerSpecs[i]);
                    layerId = renderLayerSpec.layer;

                    renderLayer = new TextScoreLayer(layerId, renderLayerSpec);
                    renderLayer.setTooltipFcn(tooltipFcn);
                    renderLayer.addToMap(worldMap);

                    layerName = renderLayerSpec.name;
                    if (!layerName) {
                        layerName = layerId;
                    }
                }

                /*
                setTimeout(function () {
                    console.log(Class.getProfileInfo());
                }, 10000);
                */

                // Populate the map layer state object with server layer data, and enable
                // listeners that will push state changes into the layers.
                mapLayerState = {};
                new ServerLayerUiMediator(mapLayerState, serverLayers, worldMap);

                // Bind layer controls to the state model.
                new LayerControls(mapLayerState);
            });
        });
