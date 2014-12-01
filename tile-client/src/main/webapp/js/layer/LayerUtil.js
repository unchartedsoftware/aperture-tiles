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

define( function () {
    "use strict";

    /**
     * Private: Parses a malformed JSON string into a usable object.
     *
     * @param jsonString {String} the malformed JSON string
     */
     function parseMalformedJson( jsonString ) {
        // replace ( and ) with [ and ]
        var squared = jsonString.replace( /\(/g, '[' ).replace( /\)/g, ']'),
            // ensure all attributes are quoted in ""
            quoted = squared.replace(/([a-zA-Z0-9]+)(:)/g,'"$1"$2');
        return JSON.parse( quoted );
     }

    /**
     * Private: Parse a given layers meta data min and max json strings,
     * they are currently stored as malformed json, so they require some
     * massaging.
     *
     * @param meta {Object} the layers meta data object
     */
    function parseMetaMinMaxJson( meta ) {
        var min = {}, max = {};
        if ( meta && ( ( meta.max && meta.max.maximum ) || meta.maximum ) ) {
            max = parseMalformedJson( meta.maximum || meta.max.maximum );
        }
        if ( meta && ( ( meta.min && meta.min.minimum ) || meta.minimum ) ) {
            min = parseMalformedJson( meta.minimum || meta.min.minimum );
        }
        return {
            min: $.isArray( min ) ? min[0] : min,
            max: $.isArray( max ) ? max[0] : max
        };
    }

    /**
     * Private: Meta data minimum and maximums are stored as malformed json
     * strings, but are usually accessed at a high frequency ( multiple
     * times per tile render ). This parses them all and stores them
     * as actual objects.
     *
     * @param layerMeta {Object} the .meta node of the data returned for a layer
     *                           service call
     */
    function parseLevelsMinMax( layerMeta ) {
        var meta = layerMeta.meta,
            key;
        for ( key in meta ) {
            if ( meta.hasOwnProperty( key ) ) {
                meta[ key ].minMax = parseMetaMinMaxJson( meta[key] );
            }
        }
        return meta;
    }

    return {

        /**
         * Parses a layer or an array of layer data objects, formats meta data
         * min and max and returns either the single layer, or a map of layers
         * keyed by layerId.
         *
         * @param layerData {Object|Array} layer data object or array of layer data objects
         */
        parse: function( layerData ) {
            var layers = layerData.layers || layerData.layer,
                layerMap,
                i;
            if ( !(layers instanceof Array) ) {
                layers.meta.minMax = parseLevelsMinMax( layers.meta );
                return layerData;
            }
            // if given an array, convert it into a map keyed by layerId
            layerMap = {};
            for ( i=0; i<layers.length; i++ ) {
                layers[i].meta.minMax = parseLevelsMinMax( layers[i].meta );
                layerMap[ layers[i].id ] = layers[i];
            }
            return layerMap;
        },

        /**
         * The getURL function passed to a OpenLayers TMS / Grid Layer to generate the
         * tile urls. Can be passed as is, or appended by using 'call'. The 'this'
         * context is set to the context of the OpenLayers layer.
         *
         * @param bounds {Object} the bounds object for the current tile
         */
        getURL: function( bounds ) {
            var res = this.map.getResolution(),
                maxBounds = this.maxExtent,
                tileSize = this.tileSize,
                x = Math.round( (bounds.left-maxBounds.left) / (res*tileSize.w) ),
                y = Math.round( (bounds.bottom-maxBounds.bottom) / (res*tileSize.h) ),
                z = this.map.getZoom();
            if ( x >= 0 && y >= 0 ) {
                return this.url + this.layername
                    + "/" + z + "/" + x + "/" + y + "."
                    + this.type;
            }
        }
    };
});
