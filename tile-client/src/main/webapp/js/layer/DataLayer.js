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
 * This module defines a basic data layer class that handles communications 
 * with the server about a layer or layers of data.
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        DataLayer;



    DataLayer = Class.extend({
        ClassName: "DataLayer",
        init: function (layerSpecs) {
            var i, layer;

            // Input information
            this.layerSpecs = {};
            // Server information
            this.layerInfos = {};

            // Populate both indexed by layer name
            for (i=0; i<layerSpecs.length; ++i) {
                layer = layerSpecs[i].layer;
                this.layerSpecs[layer] = layerSpecs[i];
                this.layerInfos[layer] = null;
            }

            // Event information
            this.onInfoRequested = null;
            this.onInfoRetrieved = [];
        },



        /**
         * Set the callback function to be called when data is requested from
         * the server
         */
        setRequestCallback: function (callback) {
            this.onInfoRequested = callback;
        },

        /**
         * Set the callback function to be called when data is retrieved from 
         * the server
         */
        addRetrievedCallback: function (callback) {
            var layer;
            // Send it a notification of what we know right now
            for (layer in this.layerInfos) {
                if (this.layerInfos.hasOwnProperty(layer) && this.layerInfos[layer]) {
                    callback(this, this.layerInfos[layer]);
                }
            }
            // Add the callback to our list of things requiring notification.
            this.onInfoRetrieved.push(callback);
        },



        /**
         * Get all currently known layer infos
         */
        getLayersInformation: function () {
            return this.layerInfos;
        },

        /**
         * Get the specification for the given layer
         */
        getLayerSpecification: function (layer) {
            return this.layerSpecs[layer];
        },


        onUnconfigRetrieved: function (layerInfo, statusInfo) {

            if (!statusInfo.success) {
                return;
            }
        },


        /**
         * Called when the server returns layer data to us
         */
        onLayerInfoRetrieved: function (layerInfo, statusInfo) {
            if (!statusInfo.success) {
                return;
            }

	        // Check to see if we have any previous info on this layer.
	        // If we do, this is a change of configuration; we need to tell
	        // the server we're done with the old configuration.
	        if (this.layerInfos[layerInfo.layer] &&
	            this.layerInfos[layerInfo.layer].id) {
		        aperture.io.rest('/layer',
		                         'POST',
		                         $.proxy(this.onUnconfigRetrieved, this),
		                         {
			                         postData: {
				                         request: "unconfigure",
                                         configuration: this.layerInfos[layerInfo.layer].id
                                     },
                                     contentType: 'application/json'
                                 }
                                );
	        }

            this.layerInfos[layerInfo.layer] = layerInfo;
            // Notify our user that we have new layer information
            if (this.onInfoRetrieved && this.onInfoRetrieved.length > 0) {
                this.onInfoRetrieved.forEach($.proxy(function (callback, index, array) {
                    callback(this, layerInfo);
                }, this));
            }
        },

        /**
         * Get basic information about this layer from the server
         *
         * @param layerSet The layers to retrieve.  Leave off to retrieve all 
         *                 layers.
         */
        retrieveLayerInfo: function (layerSet) {
            var i, layer, layerSpec;
            if (!layerSet) {
                layerSet = [];
                i = 0;
                for (layer in this.layerSpecs) {
                    if (this.layerSpecs.hasOwnProperty(layer)) {
                        layerSet[i] = layer;
                        i++;
                    }
                }
            }


            // Make sure to send out all requesting notifications before we
            // actually request any data.
            if (this.onInfoRequested) {
                for (i=0; i<layerSet.length; ++i) {
                    layer = layerSet[i];
                    layerSpec = this.layerSpecs[layer];
                    // Notify our user that we are requesting layer information
                    this.onInfoRequested(this, layerSpec);
                }
            }
            // Actually request the information
            for (i=0; i<layerSet.length; ++i) {
                layer = layerSet[i];
                layerSpec = this.layerSpecs[layer];

                // Request the layer information
                aperture.io.rest('/layer',
                                 'POST',
                                 $.proxy(this.onLayerInfoRetrieved, this),
                                 {
                                     postData: {
                                         request: "configure",
                                         layer: layer,
                                         configuration: layerSpec
                                     },
                                     contentType: 'application/json'
                                 }
                                );
            }
        }
    });


    return DataLayer;
});
