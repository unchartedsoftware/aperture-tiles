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
            this.NEGATIVE_COLOUR = '#D33CFF';
            this.NEUTRAL_COLOUR = '#222222';
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

        isHoveredOrClicked: function (tag, tilekey) {
            var clickTilekey = this.mouseState.clickState.tilekey,
                clickTag = this.mouseState.clickState.binData.tag,
                hoverTilekey = this.mouseState.hoverState.tilekey,
                hoverTag = this.mouseState.hoverState.binData.tag;

            return ((hoverTag !== undefined && hoverTag === tag && hoverTilekey === tilekey) ||
                (clickTag !== undefined && clickTag === tag && clickTilekey === tilekey));

        },


        shouldBeGreyedOut: function (tag, tilekey) {
            if ( // nothing is hovered or clicked on
                 (this.mouseState.clickState.tilekey === '' && this.mouseState.hoverState.tilekey === '') ||
                 // current tag is hovered on
                 (this.mouseState.hoverState.binData.tag !== undefined &&
                  this.mouseState.hoverState.binData.tag === tag &&
                  this.mouseState.hoverState.tilekey === tilekey )) {
                return false
            } else if (this.mouseState.clickState.binData.tag !== undefined &&
                this.mouseState.clickState.binData.tag !== tag) {
                return true;
            }
            return false;
        },


        matchingTagIsSelected: function (tag) {
            return (this.mouseState.hoverState.binData.tag !== undefined &&
                    this.mouseState.hoverState.binData.tag === tag ||
                    this.mouseState.clickState.binData.tag !== undefined &&
                    this.mouseState.clickState.binData.tag === tag)
        }

    });

    return TwitterTagRenderer;
});
