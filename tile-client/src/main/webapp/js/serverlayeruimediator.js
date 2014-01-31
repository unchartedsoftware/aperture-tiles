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

/* JSLint global declarations: these objects don't need to be declared. */
/*global define, console*/

/**
 * Populates the LayerState model based on the contents of a ServerRenderedMapLayer, and makes the appropriate
 * modifications to it as the LayerState model changes.
 *
 * TODO:
 * 1) Read filter range from server
 * 2) Write filter, ramp function and ramp type back to server in response to layer state model changes
 * 3) Generate filter ramp image URL using the /legend rest request (see ImageTileLegendService.java in tile-service)
 *
 */
define(['class', 'layerstate'], function (Class, LayerState) {
    "use strict";

    var ServerLayerUiMediator;

    ServerLayerUiMediator = Class.extend({
        ClassName: "ServerLayerUiMediator",

        /**
         * Populates the layerStateMap with LayerState objects based on the current set of map layers,
         * and registers listeners to update the map layer in response to state changes.
         *
         * @param layerStateMap - The map of LayerState objects to populate and register listeners against.
         *
         * @param mapLayer - The ServerRenderedMapLayer that the layerStateMap will be populated from, and
         * subsequently be modified by.
         *
         * @param worldMap - The base world map layer.
         */
        initialize: function (layerStateMap, mapLayer, worldMap) {
            var layerState, layerIds, layerSpecsById, layerSpec, makeLayerStateCallback, layerId, layerName, i;

            this.layerStateMap = layerStateMap;
            this.mapLayer = mapLayer;

            layerIds = mapLayer.getSubLayerIds();
            layerSpecsById = mapLayer.getSubLayerSpecsById();

            // A callback to modify map / visual state in response to layer changes.
            makeLayerStateCallback = function (mapLayer, layerState) {
                return function (fieldName) {
                    if (fieldName === "opacity") {
                        mapLayer.setSubLayerOpacity(layerState.getId(), layerState.getOpacity());
                    } else if (fieldName === "enabled") {
                        mapLayer.setSubLayerEnabled(layerState.getId(), layerState.isEnabled());
                    } // TODO: Add handlers for other changes (filter value, ramp type, ramp function)
                };
            };

            // Create layer state objects from the layer specs provided by the server rendered map layer.
            for (i = 0; i < layerIds.length; i += 1) {
                // Get the layer spec using the layer ID
                layerId = layerIds[i];
                layerSpec = layerSpecsById[layerId];
                layerName = layerSpec.name;
                if (!layerName) {
                    layerName = layerId;
                }

                // Create a layer state object.
                layerState = new LayerState(layerId);
                layerState.setName(layerName);
                layerState.setEnabled(true);
                layerState.setOpacity(layerSpec.opacity);
                layerState.setRampFunction(layerSpec.transform);
                layerState.setRampType(layerSpec.ramp);
                // TODO: Retrieve filter range from map layer

                // Register a callback to handle layer state change events.
                layerState.addListener(makeLayerStateCallback(mapLayer, layerState));

                // Add the layer to the layer statemap.
                this.layerStateMap[layerState.getId()] = layerState;
            }

            // Create a layer state object for the base map.
            layerState = new LayerState("Base Layer");
            layerState.setName("Base Layer");
            layerState.setEnabled(worldMap.isEnabled());
            layerState.setOpacity(worldMap.getOpacity());
            layerState.setRampFunction(null);
            layerState.setRampType(null);

            // Register a callback to handle layer state change events.
            layerState.addListener(function (fieldName) {
                if (fieldName === "opacity") {
                    worldMap.setOpacity(layerState.getOpacity());
                } else if (fieldName === "enabled") {
                    worldMap.setEnabled(layerState.isEnabled());
                }
            });

            // Add the layer to the layer statemap.
            this.layerStateMap[layerState.getId()] = layerState;
        }
    });
    return ServerLayerUiMediator;
});
