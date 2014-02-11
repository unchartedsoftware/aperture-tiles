/**
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
 * This module defines a simple client-rendered layer that displays a 
 * text score tile in a meaningful way.
 */
define(function (require) {
    "use strict";



    var ClientRenderer = require('./ClientRenderer'),
        TwitterTagRenderer;



    TwitterTagRenderer = ClientRenderer.extend({
        ClassName: "TwitterTagRenderer",

        init: function(id) {
            this._super(id);
            this.POSITIVE_COLOUR = '#09CFFF';
            this.POSITIVE_SELECTED_COLOUR  = '#069CCC';
            this.NEGATIVE_COLOUR = '#D33CFF';
            this.NEGATIVE_SELECTED_COLOUR = '#A009CC';
            this.NEUTRAL_COLOUR = '#222222';
            this.NEUTRAL_SELECTED_COLOUR = '#000000';
            this.HORIZONTAL_BUFFER = 14;
            this.VERTICAL_BUFFER = 24;
        },


        isNotBehindDoD: function (tilekey) {

            var parsedKey = tilekey.split(','),
                thisKeyX = parseInt(parsedKey[1], 10),
                thisKeyY = parseInt(parsedKey[2], 10);

            return (this.mouseState.clickState.tilekey === '' || // nothing clicked, or
                // not under details on demand window
                    this.mouseState.clickState.xIndex+1 !== thisKeyX ||
                   (this.mouseState.clickState.yIndex !== thisKeyY &&
                    this.mouseState.clickState.yIndex-1 !==  thisKeyY));
        },


        isHovered: function (tag, tilekey) {
            var hoverTilekey = this.mouseState.hoverState.tilekey,
                hoverTag = this.mouseState.hoverState.userData.tag;

            return hoverTag !== undefined && hoverTag === tag && hoverTilekey === tilekey;

        },


        isClicked: function (tag, tilekey) {
            var clickTilekey = this.mouseState.clickState.tilekey,
                clickTag = this.mouseState.clickState.userData.tag;

            return clickTag !== undefined && clickTag === tag && clickTilekey === tilekey;

        },

        isHoveredOrClicked: function (tag, tilekey) {
            return this.isHovered(tag, tilekey) || this.isClicked(tag, tilekey);
        },


        shouldBeGreyedOut: function (tag, tilekey) {
            if ( // nothing is hovered or clicked on
                 (this.mouseState.clickState.tilekey === '' && this.mouseState.hoverState.tilekey === '') ||
                 // current tag is hovered on
                 (this.mouseState.hoverState.userData.tag !== undefined &&
                  this.mouseState.hoverState.userData.tag === tag &&
                  this.mouseState.hoverState.tilekey === tilekey )) {
                return false
            } else if (this.mouseState.clickState.userData.tag !== undefined &&
                this.mouseState.clickState.userData.tag !== tag) {
                return true;
            }
            return false;
        },


        matchingTagIsSelected: function (tag) {
            return (this.mouseState.hoverState.userData.tag !== undefined &&
                    this.mouseState.hoverState.userData.tag === tag ||
                    this.mouseState.clickState.userData.tag !== undefined &&
                    this.mouseState.clickState.userData.tag === tag)
        },


        filterText: function (text) {
            var splitStr = text.split(' '),
                i, j, k, index,
                filterWords = ['shit', 'fuck', 'nigg'],
                replacement,
                filteredStr = '';

            // for each word
            for (i=0; i< splitStr.length; i++) {
                // for each filter word
                for (j=0; j<filterWords.length; j++) {

                    do {
                        index = splitStr[i].toLowerCase().indexOf(filterWords[j]);
                        if ( index !== -1) {
                            // if it exists, replace inner letters with '*'
                            replacement = splitStr[i].substr(0, index+1);
                            for (k=index+1; k<filterWords[j].length-1; k++) {
                                replacement += '*';
                            }
                            replacement += splitStr[i].substr(index+filterWords[j].length-1, splitStr[i].length-1);
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
            return filteredStr;
        }

    });

    return TwitterTagRenderer;
});
