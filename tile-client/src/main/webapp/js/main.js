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
require(['fileloader', 'map', 'serverrenderedmaplayer', 'slidercontrol'],
        function (FileLoader, Map, ServerLayer, SliderControl) {
            "use strict";

            var
            layerFileId = "./data/layers.json",
            // Uncomment for geographic data
            // mapFileId = "./data/geomap.json",
            // Uncomment for non-geographic data
            mapFileId = "./data/emptymap.json"
            ;

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(layerFileId, mapFileId, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    slider,
                    mapLayer,
                    layerIds,
                    layerId,
                    i,
                    base,
                    layerSlider,
                    makeSlideHandler;

                worldMap = new Map("map", jsonDataMap[mapFileId]);
                mapLayer = new ServerLayer(FileLoader.downcaseObjectKeys(jsonDataMap[layerFileId], 2));
                mapLayer.addToMap(worldMap);

                // Set up to change the base layer opacity
                slider = new SliderControl($("#mapcontrol"), "mapcontrol",
                                           "Base&nbsp;Layer&nbsp;Opacity", 0.0, 1.0, 100);
		slider.setValue(worldMap.getOpacity());
		slider.setOnSlide(function (oldValue, slider) {
		    worldMap.setOpacity(slider.getValue());
		});

                // Set up to change individual layer opacities
                layerIds = mapLayer.getSubLayerIds();
                base = $('#layerControls');
                makeSlideHandler = function (layerId) {
                    return function (oldValue, slider) {
                        mapLayer.setSubLayerOpacity(layerId, slider.getValue());
                    };
                };
                for (i=0; i<layerIds.length; ++i) {
                    layerId = layerIds[i];
                    layerSlider = $('<div id="layercontrol.'+layerId+'"></div>');
                    layerSlider.addClass("slider-table");
                    base.append(layerSlider);
                    slider = new SliderControl(layerSlider, "layercontrol."+layerId, layerId, 0.0, 1.0, 100);
                    slider.setValue(mapLayer.getSubLayerOpacity(layerId));
                    slider.setOnSlide(makeSlideHandler(layerId));
                }
            });
        });
