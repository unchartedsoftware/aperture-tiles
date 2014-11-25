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

        /*
            Utility function for positioning the labels
        */
        getYOffset: function( numEntries, spacing, offset ) {
            return offset - ( ( ( numEntries - 1) / 2 ) ) * spacing;
        },


        /*
            Returns a font size based on the percentage of tweets relative to the total count
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


        getAttributeValue: function( value, attribString ) {
            var attribs = attribString.split('.'),
                attrib,
                i;
            attrib = value;
            for (i=0; i<attribs.length; i++) {
                attrib = attrib[ attribs[i] ];
            }
            return attrib;
        }

    };

});