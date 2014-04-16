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
/*global define, console, $, aperture*/

/**
 * Populates the LayerState model based on the contents of a ServerRenderedMapLayer, and makes the appropriate
 * modifications to it as the LayerState model changes.
 */
define(function (require) {
    "use strict";

    var Class = require('../../class'),
        LayerState = require('../model/LayerState'),
        ServerLayerUiMediator;

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
         * @param worldMapLayer - The base world map layer.
         *
         */
        initialize: function (layerStateMap, mapLayer, worldMapLayer) {
            var layerState, layerIds, layerSpecsById, layerSpec, makeLayerStateCallback,
                layerId, layerName, i, makeMapZoomCallback;

            this.layerStateMap = layerStateMap;
            this.mapLayer = mapLayer;

            // Store and listen to configuration changes, so our requests can 
            // match the map's configuration
            this.layerInfos = {};
            this.mapLayer.addLayerInfoListener($.proxy(function (dataListener, layerInfo) {
                var layerState, layer;

                // Record for posterity...
                layer = layerInfo.layer;
                this.layerInfos[layer] = layerInfo;

                // And try to update our image.
                layerState = this.layerStateMap[layer];
                if (layerState) {
                    this.setupRampImage(layerState, worldMapLayer.map.getZoom());
                }
            }, this));
            layerIds = mapLayer.getSubLayerIds();

            layerSpecsById = mapLayer.getSubLayerSpecsById();

            // A callback to modify map / visual state in response to layer changes.
            makeLayerStateCallback = function (mapLayer, layerState, self) {
                // Create layer state objects from the layer specs provided by the server rendered map layer.
                return function (fieldName) {
                    if (fieldName === "opacity") {
                        mapLayer.setSubLayerOpacity(layerState.getId(), layerState.getOpacity());
                    } else if (fieldName === "enabled") {
                        mapLayer.setSubLayerEnabled(layerState.getId(), layerState.isEnabled());
                    } else if (fieldName === "rampType") {
                        mapLayer.setSubLayerRampType(layerState.getId(), layerState.getRampType());
                        self.setupRampImage(layerState, worldMapLayer.map.getZoom());
                    } else if (fieldName === "rampFunction") {
                        mapLayer.setSubLayerRampFunction(layerState.getId(), layerState.getRampFunction());
                        self.setupRampImage(layerState, worldMapLayer.map.getZoom());
                    } else if (fieldName === "filterRange") {
                        mapLayer.setSubLayerFilterRange(layerState.getId(), layerState.getFilterRange(), 0);
                    } else if (fieldName === "zIndex") {
                        mapLayer.setSubLayerZIndex(layerState.getId(), layerState.getZIndex());
                    }
                };
            };

            // Make a callback to regen the ramp image on map zoom changes
            makeMapZoomCallback = function (layerState, map, self) {
                return function () {
                    self.setupRampImage(layerState, worldMapLayer.map.getZoom());
                };
            };

            // Iterate over the list in reverse - the underlying map system will order the layers
            //
            for (i = 0; i < layerIds.length; i++) {
                // Get the layer spec using the layer ID
                layerId = layerIds[i];
                layerSpec = layerSpecsById[layerId];
                layerName = layerSpec.name;
                if (!layerName) {
                    layerName = layerId;
                }

                // Create a layer state object.  Values are initialized to those provided
                // by the layer specs, which are defined in the layers.json file, or are
                // defaulted to appropriate starting values.
                layerState = new LayerState(layerId);
                layerState.setName(layerName);
                layerState.setEnabled(true);
                layerState.setOpacity(layerSpec.renderer.opacity);
                layerState.setRampFunction(mapLayer.getSubLayerRampFunction(layerId));
                layerState.setRampType(mapLayer.getSubLayerRampType(layerId));
                layerState.setFilterRange([0.0, 1.0]);
                layerState.setZIndex(i);

                // Register a callback to handle layer state change events.
                layerState.addListener(makeLayerStateCallback(mapLayer, layerState, this));

                // Initiate ramp image computation.  This is done asynchronously by the
                // server.
                this.setupRampImage(layerState, 0);

                // Add the layer to the layer statemap.
                this.layerStateMap[layerState.getId()] = layerState;

                // Handle map zoom events - can require a re-gen of the filter image.
                worldMapLayer.map.on("zoom", makeMapZoomCallback(layerState, worldMapLayer.map, this));
            }

            // Create a layer state object for the base map.
            layerState = new LayerState("Base Layer");
            layerState.setName("Base Layer");
            layerState.setEnabled(worldMapLayer.isEnabled());
            layerState.setOpacity(worldMapLayer.getOpacity());
            layerState.setRampFunction(null);
            layerState.setRampType(null);
            layerState.setFilterRange(null);
            layerState.setZIndex(-1);

            // Register a callback to handle layer state change events.
            layerState.addListener(function (fieldName) {
                if (fieldName === "opacity") {
                    worldMapLayer.setOpacity(layerState.getOpacity());
                } else if (fieldName === "enabled") {
                    worldMapLayer.setEnabled(layerState.isEnabled());
                }
            });

            // Add the layer to the layer statemap.
            this.layerStateMap[layerState.getId()] = layerState;
        },

        /**
         * Asynchronously updates colour ramp image.
         *
         * @param {Object} layerState - The layer state object that contains
         * the parameters to use when generating the image.
         */
        setupRampImage: function (layerState, level) {
            var layer, layerUuid, legendData;

            // Make sure we have a configuration ID for this layer.
            layer = layerState.getId();
            if (this.layerInfos[layer]) {
                layerUuid = this.layerInfos[layer].id;

                legendData = {
                    transform: layerState.getRampFunction(),
                    layer: layer,
                    id: layerUuid,
                    level: level,
                    width: 128,
                    height: 1,
                    orientation: "horizontal",
                    ramp: layerState.getRampType()
                };
                aperture.io.rest('/legend', 'POST', function (legendString, status) {
                    layerState.setRampImageUrl(legendString);
                }, {postData: legendData, contentType: 'application/json'});
            }
        }
    });
    return ServerLayerUiMediator;
});
