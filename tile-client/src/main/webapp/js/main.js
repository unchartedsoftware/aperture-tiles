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
require(['fileloader', 'map', 'serverrenderedmaplayer', 'clientrenderedmaplayer', 'slidercontrol', 'labeledcontrolset'],
        function (FileLoader, Map, ServerLayer, ClientLayer, SliderControl, LabeledControlSet) {
            "use strict";

            var
            sLayerFileId = "./data/layers.json"
            // Uncomment for geographic data
            ,mapFileId = "./data/geomap.json"
            // Uncomment for non-geographic data
            // ,mapFileId = "./data/emptymap.json"
            ,cLayerFileId = "./data/renderLayers.json"
            ;

            // Load all our UI configuration data before trying to bring up the ui
            FileLoader.loadJSONData(mapFileId, sLayerFileId, cLayerFileId, function (jsonDataMap) {
                // We have all our data now; construct the UI.
                var worldMap,
                    slider,
                    mapLayer,
                    renderLayer,
                    renderLayerSpecs,
                    renderLayerSpec,
                    layerIds,
                    layerId,
                    i,
                    makeSlideHandler,
                    opcControlSet,
                    layerSpecsById;

                worldMap = new Map("map", jsonDataMap[mapFileId]);
                mapLayer = new ServerLayer(FileLoader.downcaseObjectKeys(jsonDataMap[sLayerFileId], 2));
                mapLayer.addToMap(worldMap);

                opcControlSet = new LabeledControlSet($('#layers-opacity-sliders'), 'opcControlSet');

                // Set up to change the base layer opacity
                layerId = 'Base Layer';
                slider = new SliderControl(layerId, 0.0, 1.0, 100);
		        slider.setValue(worldMap.getOpacity());
		        slider.setOnSlide(function (oldValue, slider) {
		            worldMap.setOpacity(slider.getValue());
		        });
                opcControlSet.addControl(layerId, 'Base Layer', slider.getElement());

                // Set up client-rendered layers
                renderLayerSpecs = jsonDataMap[cLayerFileId];
                for (i=0; i<renderLayerSpecs.length; ++i) {
                    renderLayerSpec = FileLoader.downcaseObjectKeys(renderLayerSpecs[i]);
                    renderLayer = new ClientLayer(renderLayerSpec.layer, renderLayerSpec);
                    renderLayer.addToMap(worldMap);
                }

                // Set up to change individual layer opacities
                layerIds = mapLayer.getSubLayerIds();
                layerSpecsById = mapLayer.getSubLayerSpecsById();
                makeSlideHandler = function (layerId) {
                    return function (oldValue, slider) {
                        mapLayer.setSubLayerOpacity(layerId, slider.getValue());
                    };
                };
                for (i=0; i<layerIds.length; ++i) {
                    layerId = layerIds[i];

                    slider = new SliderControl(layerId, 0.0, 1.0, 100);
                    slider.setValue(mapLayer.getSubLayerOpacity(layerId));
                    slider.setOnSlide(makeSlideHandler(layerId));

                    opcControlSet.addControl(layerId, layerSpecsById[layerId].name,
                        slider.getElement());
                }
            });
        });
