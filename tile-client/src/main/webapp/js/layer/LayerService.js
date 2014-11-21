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
        parseMetaMinMaxJson,
        parseLevelsMinMax;

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
        requestLayers: function( callback ) {

            var layerMap = {},
                i;

            $.get( 'rest/v1.0/layer' ).then(
                function( data ) {
                    for ( i=0; i<data.length; i++ ) {
                        data[i].meta.minMax = parseLevelsMinMax( data[i].meta );
                        layerMap[ data[i].id ] = data[i];
                    }
                },
                function( jqXHR, status, error ) {
                    // TODO handle error
                    return true;
                }
            ).always(function() {
                callback( layerMap );
            });
        },

        /**
         * Request a specific layer from the server, sending it to the listed callback
         * function when it is received.
         */
        requestLayer: function( layerId, callback ) {
            var layer;
            $.get( 'rest/v1.0/layer/' + layerId ).then(
                function( data ) {
                    layer = data;
                    layer.meta.minMax = parseLevelsMinMax( layer.meta );
                },
                function( jqXHR, status, error ) {
                    // TODO handle error
                    return true;
                }
            ).always(function() {
                callback( layer );
            });
        },

        /**
         * Set up a configuration object on the server.
         */
        configureLayer: function( layerId, params, callback ) {
            var response = {};
            $.post( 'rest/v1.0/layer/' + layerId, params ).then(
                function( data ) {
                    response = data;
                },
                function( jqXHR, status, error ) {
                    // TODO handle error
                    return true;
                }
            ).always(function() {
                callback( response );
            });
        }
    };

    return LayerService;
});
