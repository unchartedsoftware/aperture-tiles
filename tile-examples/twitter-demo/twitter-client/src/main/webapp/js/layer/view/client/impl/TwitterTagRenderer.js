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
 * This module defines a intermediary class for the twitter demo, contains
 * common functionality shared across each of the render layers
 */
define(function (require) {
    "use strict";



    var ClientRenderer = require('../ClientRenderer'),
        TwitterTagRenderer;



    TwitterTagRenderer = ClientRenderer.extend({
        ClassName: "TwitterTagRenderer",

        /**
         * Constructs a twitter tag render layer object
         * @param id the id string for the render layer
         */
        init: function(id) {
            this._super(id);
            this.Y_SPACING = 10;
            this.VALUE_COUNT = 10;
            this.TILE_SIZE = 256;
            this.X_CENTRE_OFFSET = this.TILE_SIZE / 2;  // x offest required to centre on tile
            this.Y_CENTRE_OFFSET = 0;
            this.POSITIVE_COLOUR = '#09CFFF';
            this.POSITIVE_SELECTED_COLOUR  = '#069CCC';
            this.NEGATIVE_COLOUR = '#D33CFF';
            this.NEGATIVE_SELECTED_COLOUR = '#A009CC';
            this.NEUTRAL_COLOUR = '#222222';
            this.NEUTRAL_SELECTED_COLOUR = '#000000';
            this.BLACK_COLOUR = '#000000';
            this.WHITE_COLOUR = '#FFFFFF';
            this.LIGHT_GREY_COLOUR = '#999999';
            this.YELLOW_COLOUR = '#F5F56F';
            this.HORIZONTAL_BUFFER = 14;
            this.VERTICAL_BUFFER = 24;

            this.FILTER_WORDS = [/s+h+i+t+/, /f+u+c+k+/, /n+i+g+g+/];
        },


        /**
         * Returns the number of values in the bin, capped by the VALUE_COUNT constant
         * @param data the aperturejs node data object
         */
        getCount: function(data) {
            if (data.bin.value.length === undefined ||
                data.bin.value.length === 0 ||
                isNaN(data.bin.value.length)) {
                return 0;
            }
            return (data.bin.value.length > this.VALUE_COUNT) ? this.VALUE_COUNT : data.bin.value.length;
        },


        /**
         * Returns a y offset for a given index
         * @param data the aperturejs node data object
         * @param index the layer element index
         */
        getYOffset: function(data, index) {
            return -this.Y_SPACING * (((this.getCount(data) - 1) / 2) - index);
        },


        /**
         * Returns true if the current tag in the respective tile is hovered over
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        isHovered: function (tag, tilekey) {
            var hoverTilekey = this.mouseState.hoverState.tilekey,
                hoverTag = this.mouseState.hoverState.userData.tag;

            return hoverTag === tag && hoverTilekey === tilekey;

        },


        /**
         * Returns true if the current tag in the respective tile is clicked
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        isClicked: function (tag, tilekey) {
            var clickTilekey = this.mouseState.clickState.tilekey,
                clickTag = this.mouseState.clickState.userData.tag;

            return clickTag === tag && clickTilekey === tilekey;

        },


        /**
         * Returns true if the current tag in the respective tile is hovered on or clicked
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        isHoveredOrClicked: function (tag, tilekey) {
            return this.isHovered(tag, tilekey) || this.isClicked(tag, tilekey);
        },


        /**
         * Returns true if the criteria for whether the element should be greyed out is satisfied
         * @param tag the twitter data tag string
         * @param tilekey the tilekey of the respective tile
         */
        shouldBeGreyedOut: function (tag, tilekey) {

            var hoverTilekey = this.mouseState.hoverState.tilekey,
                hoverTag = this.mouseState.hoverState.userData.tag,
                clickTilekey = this.mouseState.clickState.tilekey,
                clickTag = this.mouseState.clickState.userData.tag;

            if ( // nothing is hovered or clicked on
                 (clickTilekey === '' && hoverTilekey === '') ||
                 // current tag is hovered on
                 (hoverTag === tag && hoverTilekey === tilekey )) {
                return false
            } else if (clickTag !== undefined && clickTag !== tag) {
                return true;
            }
            return false;
        },


        /**
         * Returns true if the current tag is hovered over or clicked anywhere
         * @param tag the twitter data tag string
         */
        matchingTagIsSelected: function (tag) {
            return (this.mouseState.hoverState.userData.tag === tag ||
                    this.mouseState.clickState.userData.tag === tag)
        },


        /**
         * Filters any word in the filtered words list, replacing all inner letters with *
         * @param text the text to be filtered
         */
        filterText: function (text) {
            var splitStr = text.split(' '),
                i, j, k, index,
                replacement,
                filteredStr = '';

            String.prototype.regexIndexOf = function(regex) {
                var indexOf = this.substring(0).search(regex);
                return (indexOf >= 0) ? (indexOf + (0)) : indexOf;
            }

            function decodeHTML(s){
                var str, temp= document.createElement('p');
                temp.innerHTML= s;
                str= temp.textContent || temp.innerText;
                temp=null;
                return str;
            }

            // for each word
            for (i=0; i< splitStr.length; i++) {
                // for each filter word
                for (j=0; j<this.FILTER_WORDS.length; j++) {

                    do {
                        index = splitStr[i].toLowerCase().regexIndexOf(this.FILTER_WORDS[j]);
                        if ( index !== -1) {
                            // if it exists, replace inner letters with '*'
                            replacement = splitStr[i].substr(0, index+1);
                            for (k=index+1; k<splitStr[i].length-1; k++) {
                                replacement += '*';
                            }
                            replacement += splitStr[i].substr(index+splitStr[i].length-1, splitStr[i].length-1);
                            splitStr[i] = replacement;
                        }
                    // make sure every instance is censored
                    } while ( index !== -1);
                }
                filteredStr += splitStr[i];
                if ( i+1 < splitStr.length ) {
                    filteredStr += ' ';
                }
            }

            return decodeHTML(filteredStr);
        }

    });

    return TwitterTagRenderer;
});
