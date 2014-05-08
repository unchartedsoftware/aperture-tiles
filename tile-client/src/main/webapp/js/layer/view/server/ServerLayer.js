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
/*global OpenLayers */



/**
 * This modules defines a basic layer class that can be added to maps.
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        DataLayer = require('../../DataLayer'),

        ServerRenderedMapLayer,
        minRect,
        computeAggregateInfo,
        forEachLayer,
        Y_TILE_FUNC_PASSTHROUGH,
        Y_TILE_FUNC_ZERO_CLAMP;



    /*
     * Private function to calculate the minimum of two rectangles, defined 
     * with left, right, top, bottom, xCenter, and yCenter properties
     */
    minRect = function (a, b) {
        var result;
        if (!a) {
            result = b;
        } else if (!b) {
            result = a;
        } else {
            result = {
                top:    Math.max(   a.top,    b.top),
                left:   Math.min(  a.left,   b.left),
                right:  Math.max( a.right,  b.right),
                bottom: Math.min(a.bottom, b.bottom)
            };
        }
        result.xCenter = (result.left + result.right)/2;
        result.yCenter = (result.top + result.bottom)/2;
        return result;
    };

    /*
     * Private function to execute another function on each layer.
     */
    forEachLayer = function (mapLayer, fcn) {
        var layerInfos, layer;

        if (!mapLayer) { return; }
        if (!mapLayer.dataListener) { return; }

        layerInfos = mapLayer.dataListener.getLayersInformation();
        if (!layerInfos) { return; }
        for (layer in layerInfos) {
            if (layerInfos.hasOwnProperty(layer)) {
                $.proxy(fcn, mapLayer)(layer, layerInfos[layer]);
            }
        }
    };

    /*
     * Private function to recompute properties derived from the set of all 
     * layer information.
     */
    computeAggregateInfo = function (mapLayer) {
        var mapBounds, dataBounds, maxZoom;

        if (!mapLayer.collectedMapBounds) {
            mapBounds = null;
            dataBounds = null;
            maxZoom = null;

            forEachLayer(mapLayer, function (layer, layerInfo) {
                mapBounds = minRect(mapBounds, layerInfo.bounds);
                dataBounds = minRect(dataBounds, layerInfo.dataBounds);
                if (maxZoom) {
                    maxZoom = Math.max(maxZoom, layerInfo.maxzoom);
                } else {
                    maxZoom = layerInfo.maxZoom;
                }
            });

            mapLayer.collectedMapBounds = mapBounds;
            mapLayer.collectedDataBounds = dataBounds;
            mapLayer.maxZoom = maxZoom;
        }
    };

    /*
     * Y transformation function for non-density strips
     */
    Y_TILE_FUNC_PASSTHROUGH = function (yInput) {
        return yInput;
    };

    /*
     * Y transformation function for density strips
     */
    Y_TILE_FUNC_ZERO_CLAMP = function (yInput) {
        return 0;
    };


    ServerRenderedMapLayer = Class.extend({
        ClassName: "ServerRenderedMapLayer",
        init: function (layerSpec, map) {
            this.unfulfilledRequests = [];
            // The collected map bounds of all our map layers
            this.collectedMapBounds = null;
            // The collected data bounds of all our map layers
            this.collectedDataBounds = null;
            // The maximum zoom level of all our layers
            this.maxZoom = null;

            // The openlayer layers we put on the map, indexed by layer name.
            this.mapLayer = {};

            // The map to which we render
            this.map = map; //null;

            this.dataListener = new DataLayer(layerSpec);
            this.dataListener.setRequestCallback($.proxy(this.requestLayerInfo,
                                                         this));
            this.dataListener.addRetrievedCallback($.proxy(this.useLayerInfo,
                                                           this));

            this.dataListener.retrieveLayerInfo();
        },

        /**
         * Let someone else listen to the layer info to which we listen
         */
        addLayerInfoListener: function (listener) {
            this.dataListener.addRetrievedCallback(listener);
        },

        /*
         * Called when data is requested from the server.
         */
        requestLayerInfo: function (dataListener, layerSpec) {
            // Record the layer being requested
            this.unfulfilledRequests[this.unfulfilledRequests.length] =
                layerSpec.layer;
        },

        /*
         * Called when data the basic information about the layer is received
         * from the server.
         */
        useLayerInfo: function (dataListener, layerInfo) {
            var layer, ufrIndex;

            layer = layerInfo.layer;

            // Wait until all requests have been fulfilled
            ufrIndex = this.unfulfilledRequests.indexOf(layer);
            if (ufrIndex > -1) {
                this.unfulfilledRequests.splice(ufrIndex, 1);
            }
            if (this.unfulfilledRequests.length > 0) {
                return;
            }

            // We've got everything - create our map layers.
            this.updateLayers();
        },



        /**
         * Get the minimal data rectangle containing all layers
         */
        getBoundingMapRectangle: function () {
            computeAggregateInfo();
            return this.collectedMapBounds();
        },

        /**
         * Get the minimal data rectangle containing all layers
         */
        getBoundingDataRectangle: function () {
            computeAggregateInfo();
            return this.collectedDataBounds();
        },

        /**
         * Get the maximum zoom level of all layers
         */
        getMaximumZoom: function () {
            computeAggregateInfo();
            return this.maxZoom;
        },


        /**
         * Add this layer to a map.
         */
        addToMap: function (map) {
            this.map = map;

            this.updateLayers();
        },

        /**
         * Get a list of server rendered sub-layers we control.
         */
        getSubLayerIds: function () {
            var ids = [];

            forEachLayer(this, function (layer, layerInfo) {
                ids[ids.length] = layer;
            });

            return ids;
        },

        getSubLayerSpecsById: function () {
            var layerInfoById = {};

            forEachLayer(this, function (layer, layerInfo) {
                layerInfoById[layer] = this.dataListener.getLayerSpecification(layer);
            });

            return layerInfoById;
        },

        /**
         * Get the opacity of a given sub-layer
         */
        getSubLayerOpacity: function (subLayerId) {
            var layer = this.mapLayer[subLayerId],
                opacity = NaN;

            if (layer) {
                opacity = layer.opacity;
            }

            return opacity;
        },

        /**
         * Set the opacity of a given sub-layer
         */
        setSubLayerOpacity: function (subLayerId, opacity) {
            var layer = this.mapLayer[subLayerId];
            if (layer) {
                layer.olLayer_.setOpacity(opacity);
            }
        },


        /**
         * Set the visibility of a given sub-layer
         */
        setSubLayerVisibility: function (subLayerId, visibility) {
            var layer = this.mapLayer[subLayerId];
            if (layer) {
                layer.olLayer_.setVisibility(visibility);
            }
        },


        /**
         * Sets the visiblity of a sub layer.
         *
         * @param {string} subLayerId - The ID of the sublayer to modify.
         * @param {boolean} enabled - TRUE if the sub layer should be enabled, false otherwise.
         */
        setSubLayerEnabled: function (subLayerId, enabled) {
            var layer = this.mapLayer[subLayerId];
            if (layer) {
                layer.olLayer_.setVisibility(enabled);
            }
        },

        /**
         * @param {string} subLayerId - The sub layer ID.
         * @returns {boolean} - TRUE if the sublayer is enabled, false otherwise.
         */
        getSubLayerEnabled: function (subLayerId) {
            var layer = this.mapLayer[subLayerId], enabled;
            enabled = false;

            if (layer) {
                enabled = layer.olLayer_.setVisibility(enabled);
            }
            return enabled;
        },

        /**
         * Updates the ramp type associated with the layer.  Results in a POST
         * to the server.
         *
         * @param {string} subLayerId - The ID of layer
         * @param {string} rampType - The new ramp type for the layer.
         */
        setSubLayerRampType: function (subLayerId, rampType) {
            var layerSpec = this.dataListener.getLayerSpecification(subLayerId);
            if (!layerSpec) {
                return;
            }

            if (!layerSpec.renderer) {
                layerSpec.renderer = {ramp: rampType};
            } else {
                layerSpec.renderer.ramp = rampType;
            }
            this.dataListener.retrieveLayerInfo();
        },

        /**
         * @param {string} subLayerId - The ID of the sub layer to query.
         * @returns {string} ramp - The type of the ramp applied to the layer.
         */
        getSubLayerRampType: function (subLayerId) {
            var layerSpec = this.dataListener.getLayerSpecification(subLayerId);
            if (!layerSpec || !layerSpec.renderer || !layerSpec.renderer.ramp) {
                return "ware";
            }
            return layerSpec.renderer.ramp;
        },

        /**
         * Updates the ramp function associated with the layer.  Results in a POST
         * to the server.
         *
         * @param {string} subLayerId - The ID of the sub layer to update.
         * @param {string} rampFunction - The new new ramp function.
         */
        setSubLayerRampFunction: function (subLayerId, rampFunction) {
            var layerSpec = this.dataListener.getLayerSpecification(subLayerId);
            if (!layerSpec) {
                return;
            }

            if (!layerSpec.transform) {
                layerSpec.transform = {name: rampFunction};
            } else {
                layerSpec.transform.name = rampFunction;
            }
            this.dataListener.retrieveLayerInfo();
        },

        /**
         * @param {string} subLayerId - The ID of the sub layer to query.
         * @returns {string}  The ramp function for the layer.
         */
        getSubLayerRampFunction: function (subLayerId) {
            var layerSpec = this.dataListener.getLayerSpecification(subLayerId);
            if (!layerSpec || !layerSpec.transform || !layerSpec.transform.name) {
                return "linear";
            }
            return layerSpec.transform.name;
        },

        /**
         * Updates the filter range for the layer.  Results in a POST to the server.
         *
         * @param {string} subLayerId - The ID of the sub layer to update.
         * @param {Array} filterRange - A two element array with values in the range [0.0, 1.0],
         * where the first element is the min range, and the second is the max range.
         */
        setSubLayerFilterRange: function (subLayerId, filterRange) {
            var layerSpec;
            layerSpec = this.dataListener.getLayerSpecification(subLayerId);
            layerSpec.legendrange = [filterRange[0] * 100, filterRange[1] * 100];
            this.dataListener.retrieveLayerInfo();
        },

        /**
         * @param {string} subLayerId - The ID of the sub layer to update.
         * @returns {Array} filterRange - A two element array with values in the range [0.0, 1.0],
         * where the first element is the min range, and the second is the max range.
         */
        getSubLayerFilterRange: function (subLayerId) {
            var layerSpec = this.dataListener.getLayerSpecification(subLayerId);
            return layerSpec.legendrange;
        },

        /**
         * @param {string} subLayerId - The ID of the sublayer to update.
         * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
         */
        setSubLayerZIndex: function (subLayerId, zIndex) {
            var olLayer = this.mapLayer[subLayerId].olLayer_;
            this.map.setLayerIndex(olLayer, zIndex);
        },

        /**
         * @param {string} subLayerId - The ID of the sublayer to query.
         * @returns {number} - The z-order value of the layer, where 0 is front.
         */
        getSubLayerZIndex: function (subLayerId) {
            var olLayer = this.mapLayer[subLayerId].olLayer_;
            return this.map.getLayerIndex(olLayer);
        },

        /**
         * Update all our openlayers layers on our map.
         */
        updateLayers: function () {

            var that = this;

            if (!this.map) {
                return;
            }

            forEachLayer(this, function (layer, layerInfo) {
                var layerSpec = this.dataListener.getLayerSpecification(layer),
                    olBounds,
                    yFunction;
                if (!layerInfo) {
                    // No info; remove layer for now
                    if (this.mapLayer[layer]) {
                        this.map.map.remove(this.mapLayer[layer]);
                        this.mapLayer[layer] = null;
                    }
                } else {
                    // The bounds of the full OpenLayers map, used to determine 
                    // tile coordinates
                    // Note: these values are not set to full 20037508.342789244 as 
                    // it produces extra tiles around intended border. This rounded 
                    // down value results in the intended tile bounds ending at the
                    // border of the map
                    olBounds = new OpenLayers.Bounds(-20037500, -20037500,
                                                      20037500, 20037500);

                    // Adjust y function if we're displaying a density strip
                    if (layerSpec.isDensityStrip) {
                        yFunction = Y_TILE_FUNC_ZERO_CLAMP;
                    } else {
                        yFunction = Y_TILE_FUNC_PASSTHROUGH;
                    }

                    // Remove any old version of this layer
                    if (this.mapLayer[layer]) {
                        //this.map.map.removeLayer(this.mapLayer[layer]);
                        this.mapLayer[layer].remove();
                        this.mapLayer[layer] = null;
                    }

                    // Add the new layer
                    this.mapLayer[layer] = this.map.addApertureLayer(
                        aperture.geo.MapTileLayer.TMS, {},
                        {
                            'name': 'Aperture Tile Layers',
                            'url': layerInfo.tms,
                            'options': {
                                'layername': layerInfo.layer,
                                'type': 'png',
                                'version': '1.0.0',
                                'maxExtent': olBounds,
                                transparent: true,
                                getURL: function (bounds) {
                                    var res, x, y, z, maxBounds, tileSize, fullUrl, viewBounds;

                                    res = this.map.getResolution();
                                    tileSize = this.tileSize;
                                    maxBounds = this.maxExtent;

                                    x = Math.round((bounds.left - maxBounds.left) /
                                                   (res*tileSize.w));
                                    y = Math.round((bounds.bottom - maxBounds.bottom) /
                                                   (res*tileSize.h));
                                    y = yFunction(y);
                                    z = this.map.getZoom();

                                    if (x >= 0 && y >= 0) {

                                        viewBounds = that.map.getTileSetBoundsInView().params;
                                        
                                        fullUrl = (this.url + this.version + "/" +
                                                   this.layername + "/" + 
                                                   z + "/" + x + "/" + y + "." + this.type);

                                        if (viewBounds) {
                                            fullUrl = (fullUrl
                                                       + "?minX=" + viewBounds.minX
                                                       + "&maxX=" + viewBounds.maxX
                                                       + "&minY=" + viewBounds.minY
                                                       + "&maxY=" + viewBounds.maxY
                                                       + "&minZ=" + viewBounds.minZ
                                                       + "&maxZ=" + viewBounds.maxZ);
                                        }

                                        return fullUrl;
                                    }
                                }
                            }
                        }
                    );

					// manually set z index of this layer to 0, so it is behind client layers
                    // Note: this does not seem to actually affect the zindex value in the DOM
                    // but does prevent them from overlapping client layers. In the future it 
                    // may be worth implementing a more sophisticated system to allow proper 
                    // client-server inclusive ordering
					this.map.setLayerIndex( this.mapLayer[layer].olLayer_, 0 );

                    // Apparently we can't set opacity through options, so we 
                    // hand-set it now
                    if (layerSpec && layerSpec.Opacity) {
                        this.mapLayer[layer].olLayer_.setOpacity(layerSpec.Opacity);
                    }
                }
            });
        }
    });

    return ServerRenderedMapLayer;
});
