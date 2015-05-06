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
 * @namespace LayerUtil
 * @classdesc A utility namespace containing layer related functionality.
 */
( function() {

    "use strict";

    /**
     * Parses a malformed JSON string into a usable object.
     * @private
     *
     * @param jsonString {String} the malformed JSON string.
     */
    function parseMalformedJson( jsonString ) {
        // replace ( and ) with [ and ]
        var squared = jsonString.replace( /\(/g, '[' ).replace( /\)/g, ']'),
            // ensure all attributes are quoted in ""
            quoted = squared.replace(/([a-zA-Z0-9]+)(:)/g,'"$1"$2');
        return JSON.parse( quoted );
    }

    /**
     * Parse a given layers meta data min and max json strings,
     * they are currently stored as malformed json, so they require some
     * massaging.
     * @private
     *
     * @param meta {Object} the layers meta data object.
     */
    function parseMetaMinMaxJson( meta ) {
        var minMax,
            min,
            max,
            key;
        if ( typeof meta === 'string' ) {
            // new meta data is valid json, hurray!
            return JSON.parse( meta );
        }
        // old meta data was crafted in the fiery pits of hades and manifests
        // as malformed json sometimes wrapped in an array, in one of six
        // potential formats
        if ( meta &&
            ( meta.max !== undefined || meta.maximum !== undefined ) &&
            ( meta.min !== undefined || meta.minimum !== undefined ) ) {
            // single bucket entries, in one of three attributes
			max = meta.maximum !== undefined ? meta.maximum : meta.max.maxmium || meta.max || 0;
			min = meta.minimum !== undefined ? meta.minimum : meta.min.minimum || meta.min || 0;
            // sometimes the meta data is wraped in an array
            max = ( max instanceof Array ) ? max[0] : max;
            min = ( min instanceof Array ) ? min[0] : min;
            // sometimes its a string
            if ( typeof max === 'string' ) {
                max = parseMalformedJson( max );
            }
            if ( typeof min === 'string' ) {
                min = parseMalformedJson( min );
            }
            // sometimes the parsed value is also wrapped in an array
            return {
                maximum: ( max instanceof Array ) ? max[0] : max,
                minimum: ( min instanceof Array ) ? min[0] : min,
                bins: meta.bins
            };
        }
        // some multi-bucket entries come as arrays
        minMax = [];
        for ( key in meta ) {
            if ( meta.hasOwnProperty( key ) ) {
                minMax.push( parseMetaMinMaxJson( meta[key] ) );
            }
        }
        return minMax;
    }

    /**
     * Meta data minimum and maximums are stored as malformed json
     * strings, but are usually accessed at a high frequency ( multiple
     * times per tile render ). This parses them all and stores them
     * as actual objects.
     * @private
     *
     * @param layerMeta {Object} the .meta node of the data returned for a layer
     *                           service call
     */
    function parseLevelsMinMax( layerMeta ) {
        var meta = layerMeta.meta,
            key;
        for ( key in meta ) {
            if ( meta.hasOwnProperty( key ) ) {
                if ( key !== "bucketCount" &&
                    key !== "rangeMin" &&
                    key !== "rangeMax" &&
                    key !== "topicType" &&
                    key !== "translatedTopics" ) {
                    meta[ key ] = parseMetaMinMaxJson( meta[key] );
                }
            }
        }
        return meta;
    }

    var LayerUtil = {

        /**
         * Parses a layer or an array of layer data objects, formats meta data
         * min and max and returns either the single layer, or a map of layers
         * keyed by layerId.
         * @memberof LayerUtil
         *
         * @param {Object|Array} layerData - layer data object or array of layer data objects.
         */
        parse: function( layerData ) {
            var layerMap,
                i;
            if ( !(layerData instanceof Array) ) {
                if ( layerData.meta ) {
                    parseLevelsMinMax( layerData.meta );
                }
                return layerData;
            }
            // if given an array, convert it into a map keyed by layerId
            layerMap = {};
            for ( i=0; i<layerData.length; i++ ) {
                if ( layerData[i].meta ) {
                    parseLevelsMinMax( layerData[i].meta );
                }
                layerMap[ layerData[i].id ] = layerData[i];
            }
            return layerMap;
        },

        /**
         * Given an OpenLayers.Layer class and a bounds object, return the x,
         * y, and y components of the tile.
         *
         * @param {OpenLayers.Layer) olLayer - The OpenLayers Layer object.
         * @param {OpenLayers.Bounds} bounds - The OpenLayers Bounds object.
         *
         * @returns {{x: (number), y: (number), z: integer}} The tile index.
         */
        getTileIndex: function( olLayer, bounds ) {
            var res = olLayer.map.getResolution(),
                maxBounds = olLayer.maxExtent,
                tileSize = olLayer.tileSize;
            return {
                xIndex: Math.round( (bounds.left - maxBounds.left) / (res * tileSize.w) ),
                yIndex: Math.round( (bounds.bottom - maxBounds.bottom) / (res * tileSize.h) ),
                level: olLayer.map.getZoom()
            };
        },

        /**
         * Given an OpenLayers.Layer class and a OpenLayers.Bounds object, return the
         * tilekey.
         *
         * @param {OpenLayers.Layer) olLayer - The OpenLayers Layer object.
         * @param {OpenLayers.Bounds} bounds - The OpenLayers Bounds object.
         *
         * @returns {String} The tilekey from the bounds.
         */
        getTilekey: function( olLayer, bounds ) {
            var tileIndex = LayerUtil.getTileIndex( olLayer, bounds ),
                x = tileIndex.xIndex,
                y = tileIndex.yIndex,
                z = tileIndex.level;
            return z + "," + x + "," + y;
        },

        /**
         * The getURL function passed to a OpenLayers TMS / Grid Layer to generate the
         * tile urls. Can be passed as is, or appended by using 'call'. The 'this'
         * context is set to the context of the OpenLayers layer.
         * @memberof LayerUtil
         *
         * @param {Object} bounds - The bounds object for the current tile.
         */
        getURL: function( bounds ) {
            var tileIndex = LayerUtil.getTileIndex( this, bounds ),
                x = tileIndex.xIndex,
                y = tileIndex.yIndex,
                z = tileIndex.level;
            if ( x >= 0 && y >= 0 ) {
                return this.url + this.layername + "/" + z + "/" + x + "/" + y + "." + this.type;
            }
        }
    };

    module.exports = LayerUtil;
}());
