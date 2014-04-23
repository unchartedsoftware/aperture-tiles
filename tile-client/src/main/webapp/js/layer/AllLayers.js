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


/**
 * This module handles communications with the server to get the list of 
 * layers the server can serve.
 */
define(function (require) {
    "use strict";

    var AllLayers,
        visitLayers,
        leafLayerFilter,
        axisLayerFilter;

    visitLayers = function (layers, fcn) {
        var i;
        if ($.isArray(layers)) {
            for (i=0; i < layers.length; ++i) {
                visitLayers(layers[i], fcn);
            }
        } else {
            fcn(layers);
            if (layers.children) {
                visitLayers(layers.children, fcn);
            }
        }
    };

    leafLayerFilter = function (layers, filterFcn) {
        var result = [];

        visitLayers(layers, function (layer) {
            if (!layer.children && (!filterFcn || filterFcn(layer))) {
                result.push(layer);
            }
        });

        return result;
    };

    axisLayerFilter = function (layers, filterFcn) {
        var result = [];

        visitLayers(layers, function (layer) {
            if (layer.axes && (!filterFcn || filterFcn(layer))) {
                result.push(layer);
            }
        });

        return result;
    };

    AllLayers = {
	    layers: 0,
	    callbacks: [],

        /**
         * Request layers from the server, sending them to the listed callback 
         * function when they are received.
         *
         * @param callback A function taking a hierarchical layers description.
         */
        requestLayers: function (callback) {
            if (this.layers) {
                callback(this.layers);
            } else {
                this.callbacks.push(callback);
                aperture.io.rest('/layer',
                                 'POST',
                                 $.proxy(this.onLayersListRetrieved, this),
                                 {
                                     postData: {
                                         request: "list"
                                     },
                                     contentType: 'application/json'
                                 }
                                );
            }
        },

        onLayersListRetrieved: function (layers, statusInfo) {
            if (!statusInfo.success) {
                return;
            }
	        this.layers = layers;
            var callbacks = this.callbacks;
            this.callbacks = [];
            callbacks.forEach(function(callback) {
                callback(layers);
                    });
        },

        /**
         * Run through the given hierarchical layers object, retrieving only 
         * leaf nodes, and filtering those leaf nodes based on an arbitrary 
         * function.
         *
         * @param layers A hierarchical layers object, as returned to the
         *               callback from {@link #requestLayers}.
         * @param filterFcn A function that takes a leaf node and returns 
         *                  true if it is wanted, and false if it isn't.  If
         *                  the filterFcn is null, all leaves are returned.
         */
        filterLeafLayers: leafLayerFilter,

        /**
         * Run through the given hierarchical layers object, retrieving only 
         * nodes which specify axes, and filtering those leaf nodes based on 
         * an arbitrary function.
         *
         * @param layers A hierarchical layers object, as returned to the
         *               callback from {@link #requestLayers}.
         * @param filterFcn A function that takes an axis node and returns 
         *                  true if it is wanted, and false if it isn't. If
         *                  filterFcn is null, all nodes specifying axes are
         *                  returned.
         */
        filterAxisLayers: axisLayerFilter
    };

    return AllLayers;
});
