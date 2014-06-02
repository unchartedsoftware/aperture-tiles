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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines the base class for a client render layer. Must be
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";

    return {

        /*
            Return the count of node entries, clamped at MAX_COUNT
        */
        getCount : function( value ) {
            var MAX_COUNT = 5;
            return Math.min( value.length, MAX_COUNT );
        },


        /*
            Return the relative percentages of positive, neutral, and negative tweets
        */
        getSentimentPercentages : function( value, index ) {
            return {
                positive : ( value[index].positive / value[index].count )*100 || 0,
                neutral : ( value[index].neutral / value[index].count )*100 || 0,
                negative : ( value[index].negative / value[index].count )*100 || 0
            };
        },

        /*
            Returns the total count of all tweets in a node
        */
        getTotalCount : function( value, index ) {
            var i,
                sum = 0,
                n = this.getCount( value );
            for (i=0; i<n; i++) {
                sum += value[i].count;
            }
            return sum;
        },

        /*
            Returns the percentage of tweets in a node for the respective tag
        */
        getTotalCountPercentage : function( value, index ) {
            return ( value[index].count / this.getTotalCount( value, index ) ) || 0;
        },

        /*
            Returns a font size based on the percentage of tweets relative to the total count
        */
        getFontSize : function( value, index ) {
            var DOWNSCALE_OFFSET = 1.5,
                MAX_FONT_SIZE = 28 * DOWNSCALE_OFFSET,
                MIN_FONT_SIZE = 12 * DOWNSCALE_OFFSET,
                FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                sum = this.getTotalCount( value, index ),
                percentage = this.getTotalCountPercentage( value, index ),
                scale = Math.log( sum ),
                size = ( percentage * FONT_RANGE * scale ) + ( MIN_FONT_SIZE * percentage );
            return Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
        },

        /*
            Returns a y offset to position tag entry relative to centre of tile
        */
        getYOffset : function( value, index ) {
            return 98 - ( (( this.getCount( value ) - 1) / 2 ) - index ) * 36;
        },

        /*
            Returns a trimmed string based on character limit
        */
        trimLabelText : function( str ) {
            var MAX_LABEL_CHAR_COUNT = 9;
            if (str.length > MAX_LABEL_CHAR_COUNT) {
                str = str.substr( 0, MAX_LABEL_CHAR_COUNT ) + "...";
            }
            return str;
        },

    };

});