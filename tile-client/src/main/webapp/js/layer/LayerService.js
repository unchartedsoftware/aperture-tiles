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

    var Util = require('../util/Util'),
        LayerService,
        visitLayers,
        parseMetaMinMaxJson,
        parseLevelsMinMax,
        layersDeferred;

    visitLayers = function( layers, fcn ) {
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


    /**
     * Parse a given meta data layers min and max json strings,
     * they are currently stored as malformed json strings, so
     * they require some massaging.
     */
    parseMetaMinMaxJson = function( meta ) {
        var min = {}, max = {};
        if ( meta && ( ( meta.max && meta.max.maximum ) || meta.maximum ) ) {
            max = Util.parseMalformedJson( meta.maximum || meta.max.maximum );
        }
        if ( meta && ( ( meta.min && meta.min.minimum ) || meta.minimum ) ) {
            min = Util.parseMalformedJson( meta.minimum || meta.min.minimum );
        }
        return {
            min: $.isArray( min ) ? min[0] : min,
            max: $.isArray( max ) ? max[0] : max
        };
    };

    /**
     * Meta data minimum and maximums are stored as malformed json
     * strings, but are usually access at a high frequency ( multiple times per tile ).
     * this parses them all and stores them as actual objects.
     */
    parseLevelsMinMax = function( layerInfo ) {
        var meta = layerInfo.meta, key;
        for ( key in meta ) {
            if ( meta.hasOwnProperty( key ) ) {
                meta[key].minMax = parseMetaMinMaxJson( meta[key] );
            }
        }
        return meta;
    };

    LayerService = {

        /**
         * Request layers from the server, sending them to the listed callback 
         * function when they are received.
         */
        requestLayers: function () {
	        if ( !layersDeferred ) {
		        layersDeferred = $.Deferred();
                aperture.io.rest('/layer',
                                 'POST',
                                 function (layers, status) {
	                                 if (status.success) {
		                                 layersDeferred.resolve(layers);
	                                 } else {
		                                 layersDeferred.fail(status);
	                                 }
                                 },
                                 {
                                     postData: {
                                         request: "list"
                                     },
                                     contentType: 'application/json'
                                 }
                                );
            }
	        return layersDeferred;
        },


        /**
         * Set up a configuration object on the server.
         *
         * @param layerSpec The layer specification object used to configure the server.
         * @param callback  The callback function executed upon receiving the server result.
         */
        configureLayer: function( layerSpec, callback ) {

            aperture.io.rest('/layer',
                             'POST',
                             function( layerInfo, statusInfo ) {
                                // parse min/max meta data json string
                                layerInfo.meta.minMax = parseLevelsMinMax( layerInfo );
                                callback( layerInfo, statusInfo );
                             },
                             {
                                 postData: {
                                     request: "configure",
                                     layer: layerSpec.layer,
                                     configuration: layerSpec
                                 },
                                 contentType: 'application/json'
                             });
        },

        /**
         * Release a previous configuration from the server.
         *
         * @param layerInfo The layer information object containing the previous configurations uuid.
         * @param callback  The callback function executed upon receiving the server result.
         */
        unconfigureLayer: function( layerInfo, callback ) {

            aperture.io.rest('/layer',
                             'POST',
                             callback,
                             {
                                 postData: {
                                     request: "unconfigure",
                                     configuration: layerInfo.id
                                 },
                                 contentType: 'application/json'
                             }
                            );
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
        filterLeafLayers: function (layers, filterFcn) {

            var result = [];
            visitLayers(layers, function (layer) {
                if (!layer.children && (!filterFcn || filterFcn(layer))) {
                    result.push(layer);
                }
            });

            return result;
        }

    };

    return LayerService;
});
