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
         './client-rendering/TextScoreRenderer',
         './client-rendering/TextScoreRendererOther',
         './ui/SliderControl',
         './ui/CheckboxControl',
         './ui/LayerControl',
         './ui/LabeledControlSet',
         './axis/AxisUtil',
         './axis/Axis',
         './view-controller/Carousel',
         './LayerInfoLoader',
         './client-rendering/DataTracker',
         './profileclass'],

        function (FileLoader, Map, ServerLayer,
                  TextScoreRenderer, TextScoreRendererOther,
                  SliderControl,
                  CheckboxControl, LayerControl,LabeledControlSet,
                  AxisUtil, Axis, Carousel, LayerInfoLoader, DataTracker, Class ) {
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
                    slider,
                    checkbox,
                    //serverLayers,
                    renderLayerSpecs,
                    renderLayerSpec,
                    //layerIds,
                    layerId,
                    layerName,
                    layerControl,
                    //makeSlideHandler,
                    //makeCheckboxCheckedHandler,
                    //makeCheckboxUncheckedHandler,
                    i,
                    layerControlSet,
                    //layerSpecsById,
                    tooltipFcn,
                    xAxisSpec,
                    yAxisSpec,
                    xAxis,
                    yAxis,
                    carousel,
                    tileScoreRenderer,
                    tileScoreRendererOther,
                    redrawAxes,
                    dataTracker
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
                worldMap.map.on('zoomend', redrawAxes);

                layerControlSet = new LabeledControlSet($('#layers-opacity-sliders'), 'layerControlSet');

                // Set up to change the base layer opacity
                layerId = 'Base Layer';
                slider = new SliderControl(layerId, 0.0, 1.0, 100);
                slider.setValue(worldMap.getOpacity());
                slider.setOnSlide(function (oldValue, slider) {
                    worldMap.setOpacity(slider.getValue());
                });

                checkbox = new CheckboxControl(layerId, true );
                checkbox.setOnChecked( function() {
                    worldMap.setVisibility(true);
                });

                checkbox.setOnUnchecked( function() {
                    worldMap.setVisibility(false);
                });

                // create layer control for base layer
                layerControl = new LayerControl(layerId);
                // add visibility checkbox to layer controls
                layerControl.addControl(layerId + '-checkbox', checkbox.getElement() );
                // add slider to layer controls
                layerControl.addControl(layerId + '-slider', slider.getElement());
                // add layer controls to control set
                layerControlSet.addControl(layerId, 'Base Layer', layerControl.getElement());

                /*
                // Set up server-rendered display layers
                serverLayers = new ServerLayer(FileLoader.downcaseObjectKeys(jsonDataMap[sLayerFileId] ));
                serverLayers.addToMap(worldMap);

                // Set up server-rendered layer controls
                layerIds = serverLayers.getSubLayerIds();
                layerSpecsById = serverLayers.getSubLayerSpecsById();

                makeSlideHandler = function (layerId) {
                    return function (oldValue, slider) {
                        serverLayers.setSubLayerOpacity(layerId, slider.getValue());
                    };
                };

                makeCheckboxCheckedHandler = function (layerId) {
                    return function() {
                        serverLayers.setSubLayerVisibility(layerId, true);
                    };
                };

                makeCheckboxUncheckedHandler = function (layerId) {
                    return function() {
                        serverLayers.setSubLayerVisibility(layerId, false);
                    };
                };

                for (i=0; i<layerIds.length; ++i) {

                    layerId = layerIds[i];
                    layerName = layerSpecsById[layerId].name;
                    if (!layerName) {
                        layerName = layerId;
                    }

                    slider = new SliderControl(layerId, 0.0, 1.0, 100);
                    slider.setValue(1);
                    slider.setOnSlide(makeSlideHandler(layerId));

                    checkbox = new CheckboxControl(layerId, true );
                    checkbox.setOnChecked(makeCheckboxCheckedHandler(layerId));
                    checkbox.setOnUnchecked(makeCheckboxUncheckedHandler(layerId));

                    // create layer control for base layer
                    layerControl = new LayerControl(layerId);
                    // add visibility checkbox control
                    layerControl.addControl(layerId + '-checkbox', checkbox.getElement() );
                    // add slider control
                    layerControl.addControl(layerId + '-slider', slider.getElement());
                    // add layer control to control set
                    layerControlSet.addControl(layerId, layerName, layerControl.getElement());
                }
                */

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

                    tileScoreRenderer = new TextScoreRenderer();
                    tileScoreRenderer.setTooltipFcn(tooltipFcn);


                    tileScoreRendererOther = new TextScoreRendererOther();
                    tileScoreRenderer.setTooltipFcn(tooltipFcn);


                    layerName = renderLayerSpec.name;
                    if (!layerName) {
                        layerName = layerId;
                    }

                    /*
                    slider = new SliderControl(layerId, 0.0, 1.0, 100);
                    slider.setValue(1);
                    slider.setOnSlide(makeSlideHandler(layerId));

                    checkbox = new CheckboxControl(layerId, true );
                    checkbox.setOnChecked(makeCheckboxCheckedHandler(layerId));
                    checkbox.setOnUnchecked(makeCheckboxUncheckedHandler(layerId));
                     */

                    // create layer control for base layer
                    layerControl = new LayerControl(layerId);
                    // add visibility checkbox control
                    /*
                    layerControl.addControl(layerId + '.checkbox', checkbox.getElement());
                    // add slider control
                    layerControl.addControl(layerId + '.slider', slider.getElement());
                    */
                    // add layer control to control set
                    layerControlSet.addControl(layerId, layerName, layerControl.getElement());
                }

                LayerInfoLoader.getLayerInfo( renderLayerSpec, function( layerInfo ) {

                    dataTracker = new DataTracker(layerInfo);
                    carousel = new Carousel( {
                        map: worldMap.map,
                        views: [
                            {
                                id: "red",
                                dataTracker: dataTracker,
                                renderer: tileScoreRenderer
                            },
                            {
                                id: "blue",
                                dataTracker: dataTracker,
                                renderer: tileScoreRendererOther
                            }
                        ]});
                    carousel.dummy = 0; // to shut jslint up

                });


                /*
                setTimeout(function () {
                    console.log(Class.getProfileInfo());
                }, 10000);
                */
            });
        });
