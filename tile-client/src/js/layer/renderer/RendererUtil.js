/*
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

/**
 * @namespace RenderUtil
 * @classdesc A utility namespace containing renderer related functionality.
 */
( function() {

    "use strict";

    function log10(val) {
        return Math.log(val) / Math.LN10;
    }

    module.exports = {

        /**
         * Returns a y offset required to vertical centre a number of entries based
         * on spacing and offset
         * @memberof RenderUtil
         *
         * @param {integer} numEntries - The number of entries to render.
         * @param {integer} spacing - The spacing between entries in pixels.
         * @param {integer} offset - The offset from the top of the tile to centre on in pixels.
         *
         * @returns {integer} The y pixel offset.
         */
        getYOffset: function( numEntries, spacing, offset ) {
            return offset - ( ( ( numEntries - 1) / 2 ) ) * spacing;
        },

        /**
         * Transforms a value into the range [0:1] based on a min and max value
         * according to a linear or log transform.
         *
         * @param {number} value - The value to transform.
         * @param {number} min -The value to transform.
         * @param {number} max - The value to transform.
         * @param {String} type - The type of transformation ('log' or 'linear').
         *
         * @returns {number} The value between 0 and 1.
         */
        transformValue: function( value, min, max, type ) {
            var clamped = Math.max( Math.min( value, max ), min );
            if ( type === "log" ) {
                var logMin = log10( min || 1 );
        		var logMax = log10( max || 1 );
        		var oneOverLogRange = 1 / ( (logMax - logMin) || 1 );
                return ( log10( clamped || 1 ) - logMin ) * oneOverLogRange;
            } else {
                var range = max - min;
                return ( clamped - min ) / range;
            }
        },

        /**
         * Returns a font size based on the percentage of tweets relative to the total count
         * @memberof RenderUtil
         *
         * @param {integer} count - The local count.
         * @param {integer} totalCount - The global count.
         * @param {Object} options - The options object to set min and max font size and type (optional).
         *
         * @returns {integer} The interpolated font size.
         */
        getFontSize: function( value, min, max, options ) {
            options = options || {};
            var MAX_FONT_SIZE = options.maxFontSize || 22,
                MIN_FONT_SIZE = options.minFontSize || 12,
                transformed = this.transformValue( value, min, max, options.type );
            return MIN_FONT_SIZE + transformed*( MAX_FONT_SIZE - MIN_FONT_SIZE );
        },

        /**
         * Traverses an object to return a nested attribute
         * @memberof RenderUtil
         *
         * @param {Object} obj - The object to traverse.
         * @param {String|Function} attribPath - Period delimited attribute path or a function that returns one.
         *
         * @returns {*} The nested value within the object.
         */
        getAttributeValue: function( obj, attribPath ) {
            var attribs,
                arraySplit,
                attrib,
                i;
            if ( typeof attribPath === "function" ) {
                attribPath = attribPath( obj );
            }
            attribs = attribPath.split('.');
            attrib = obj;
            for (i=0; i<attribs.length; i++) {
                arraySplit = attribs[i].replace(/ /g, '' ).split(/[\[\]]/);
                if ( arraySplit.length === 1 ) {
                    // normal attribute
                    if ( attribs[i].length > 0 ) {
                        attrib = attrib[ attribs[i] ];
                    }
                } else if ( arraySplit.length === 3 ) {
                    // array index expressed, use it
                    attrib = attrib[ arraySplit[0] ][ arraySplit[1] ];
                } else {
                    // unrecognized input, default to assumption of
                    // normal attribute
                    attrib = attrib[ arraySplit[0] ];
                }
            }
            return attrib;
        },

        /**
         * Converts a hex code color to its RGB counter part.
         * @memberof RenderUtil
         *
         * @param {String} hex - The hex code color.
         *
         * @returns {Object} The RGB color values.
         */
        hexToRgb: function(hex) {
            var bigint;
            // remove #
            hex = hex.replace(/#/, '');
            // if only 3 hex values are provided, expand into 6 digit hex code
            if ( hex.length === 3 ) {
                hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
            }
            bigint = parseInt(hex, 16);
            return {
                r: (bigint >> 16) & 255,
                g: (bigint >> 8) & 255,
                b: bigint & 255
            };
        },

        /**
         * Converts an RGB color to its hex code counter part.
         * @memberof RenderUtil
         *
         * @param {Object} rgb - The RGB color.
         *
         * @returns {String} The hex code color value.
         */
        rgbToHex: function( rgb ) {
            var r = rgb.r,
                g = rgb.g,
                b = rgb.b;
            function componentToHex(c) {
                var hex = c.toString(16);
                return (hex.length === 1) ? "0" + hex : hex;
            }
            return "#" + componentToHex( Math.floor(r)) +
                         componentToHex( Math.floor(g)) +
                         componentToHex( Math.floor(b));
        },

        /**
         * Increases the brightness of a hex code color by a percentage factor
         * @memberof RenderUtil
         *
         * @param {String} hex - The hex code color.
         * @param {float} factor - The percentage factor of increase.
         *
         * @returns {String} The adjusted hex code color value.
         */
        hexBrightness: function( hex, factor ) {
            var rgb = this.hexToRgb( hex );
            return this.rgbToHex( { r: Math.min( Math.max( 0, rgb.r * factor ), 255 ),
                                    g: Math.min( Math.max( 0, rgb.g * factor ), 255 ),
                                    b: Math.min( Math.max( 0, rgb.b * factor ), 255 ) } );
        },

        /**
         * Converts a hex code color to greyscale using a luminosity based model
         * @memberof RenderUtil
         *
         * @param {String} hex - The  hex code color.
         *
         * @returns {String} The greyscale hex code color value.
         */
        hexGreyscale: function( hex ) {
            var rgb = this.hexToRgb( hex ),
                avg = ( rgb.r * 0.21 + rgb.g * 0.72 + rgb.b * 0.07 );
            return this.rgbToHex({
                r: avg,
                g: avg,
                b: avg
            });
        },

        /**
         * Blends two hex code colors together, defaults to 50/50 blend
         * @memberof RenderUtil
         *
         * @param {String} hexA - The hex code color A.
         * @param {String} hexB - The hex code color B.
         * @param {number} aIntoBPercentage - The percentage to blend hexA into hexB (optional).
         *
         * @returns {String} The blended hex code color value.
         */
        hexBlend: function( hexA, hexB, aIntoBPercentage ) {
            var aPerc = ( aIntoBPercentage !== undefined ) ? Math.min( 1, aIntoBPercentage ) : 0.5,
                bPerc = 1 - aPerc,
                rgb1 = this.hexToRgb( hexA ),
                rgb2 = this.hexToRgb( hexB );
            return this.rgbToHex({
                r : ( rgb1.r*aPerc + rgb2.r*bPerc ),
                g : ( rgb1.g*aPerc + rgb2.g*bPerc ),
                b : ( rgb1.b*aPerc + rgb2.b*bPerc )
            });
        }

    };

}());
