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

define( function( require ) {
    "use strict";

    return {

        /**
         * Returns a y offset required to vertical centre a number of entries based
         * on spacing and offset
         *
         * @param numEntries {int} number of entries to render
         * @param spacing    {int} the spacing between entries in pixels
         * @param offset     {int} the offset from the top of the tile to centre on in pixels
         */
        getYOffset: function( numEntries, spacing, offset ) {
            return offset - ( ( ( numEntries - 1) / 2 ) ) * spacing;
        },

        /**
         * Returns a font size based on the percentage of tweets relative to the total count
         *
         * @param count      {int} local count
         * @param totalCount {int} global count
         * @param [options]    {Object} options object to set min and max font size and bias (optional)
         */
        getFontSize: function( count, totalCount, options ) {
            options = options || {};
            var MAX_FONT_SIZE = options.maxFontSize || 22,
                MIN_FONT_SIZE = options.minFontSize || 12,
                BIAS = options.bias || 0,
                FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                percentage = ( count / totalCount ) || 0,
                size = ( percentage * FONT_RANGE ) + MIN_FONT_SIZE;
            size = Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
            return Math.min( Math.max( size+BIAS, MIN_FONT_SIZE), MAX_FONT_SIZE );
        },

        /**
         * Traverses an object to return a nested attribute
         *
         * @param obj        {Object} the object to traverse
         * @param attribPath {String} period delimited attribute path
         */
        getAttributeValue: function( obj, attribPath ) {
            var attribs = attribPath.split('.'),
                attrib,
                i;
            attrib = obj;
            for (i=0; i<attribs.length; i++) {
                attrib = attrib[ attribs[i] ];
            }
            return attrib;
        },

        /**
         * Converts a hexcode color to its RGB counter part.
         *
         * @param hex {String} hex code color
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
         * Converts an RGB color to its hexcode counter part.
         *
         * @param rgb {Object} RGB color
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
         * Increases the brightness of a hexcode color by a percentage factor
         *
         * @param hex    {String} hex code color
         * @param factor {float}  percentage factor of increase
         */
        hexBrightness: function( hex, factor ) {
            var rgb = this.hexToRgb( hex );
            return this.rgbToHex( { r: Math.min( Math.max( 0, rgb.r * factor ), 255 ),
                                    g: Math.min( Math.max( 0, rgb.g * factor ), 255 ),
                                    b: Math.min( Math.max( 0, rgb.b * factor ), 255 ) } );
        },

        /**
         * Converts a hexcode color to greyscale using aluminosity based model
         *
         * @param hex {String} hex code color
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
         * Blends two hexcode colors together, defaults to 50/50 blend
         *
         * @param hexA {String} hex code color A
         * @param hexB {String} hex code color B
         * @param [aIntoBPercentage] {float} percentage to blend hexA into hexB (optional)
         */
        hexBlend: function( hexA, hexB, aIntoBPercentage ) {
            var aPerc = Math.min( 1, aIntoBPercentage ) || 0.5,
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

});