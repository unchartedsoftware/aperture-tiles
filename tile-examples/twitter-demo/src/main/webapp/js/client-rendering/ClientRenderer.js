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



    var Class = require('../class'),
        ClientRenderer;



    ClientRenderer = Class.extend({
        ClassName: "ClientRenderer",

        init: function(id) {

            this.id = id;
            this.mouseState = {};
            this.POSITIVE_COLOUR = '#09CFFF';
            this.NEGATIVE_COLOUR = '#D33CFF';
            this.NEUTRAL_COLOUR = '#222222';
            this.TILE_SIZE = 254;
            this.X_CENTRE_OFFSET = this.TILE_SIZE / 2;
            this.Y_CENTRE_OFFSET = 0;
        },


         rgbToHex : function(r, g, b) {
             function componentToHex(c) {
                 var hex = c.toString(16);
                 return hex.length == 1 ? "0" + hex : hex;
             }
             return "#" + componentToHex( Math.floor(r)) + componentToHex( Math.floor(g)) + componentToHex( Math.floor(b));
        },


         hexToRgb: function(hex) {
             var bigint;
             if (hex[0] === '#') {
                 hex = hex.substr(1,6);
             }
             bigint = parseInt(hex, 16);
             return {
                 r: (bigint >> 16) & 255,
                 g: (bigint >> 8) & 255,
                 b: bigint & 255
             };
        },


        getCount: function(data) {
            if (data.bin.value.length === undefined ||
                data.bin.value.length === 0 ||
                isNaN(data.bin.value.length)) {
                return 0;
            }
            return (data.bin.value.length > this.valueCount) ? this.valueCount : data.bin.value.length;
        },


        getYOffset: function(data, index) {
            return -this.ySpacing * (((this.getCount(data) - 1) / 2) - index);
        },


        attachMouseState: function(mouseState) {
            this.mouseState = mouseState;
        },


        setMouseHoverState: function(tilekey, binData) {
            var splitKey = tilekey.split(',');
            this.mouseState.hoverState.binData = binData;
            this.mouseState.hoverState.tilekey = tilekey;
            this.mouseState.hoverState.level = parseInt(splitKey[0]);
            this.mouseState.hoverState.xIndex = parseInt(splitKey[1]);
            this.mouseState.hoverState.yIndex = parseInt(splitKey[2]);
        },

        setMouseClickState: function(tilekey, binData) {
            var splitKey = tilekey.split(',');
            this.mouseState.clickState.binData = binData;
            this.mouseState.clickState.tilekey = tilekey;
            this.mouseState.clickState.level = parseInt(splitKey[0]);
            this.mouseState.clickState.xIndex = parseInt(splitKey[1]);
            this.mouseState.clickState.yIndex = parseInt(splitKey[2]);
        },

        clearMouseState: function() {
            this.clearMouseClickState();
            this.clearMouseHoverState();
        },


        clearMouseClickState: function() {
            this.mouseState.clickState = {
                binData : {},
                tilekey : '',
                level : -1,
                xIndex : -1,
                yIndex : -1
            };
        },


        clearMouseHoverState: function() {
            this.mouseState.hoverState = {
                binData : {},
                tilekey : '',
                level : -1,
                xIndex : -1,
                yIndex : -1
            };
        },


        isNotBehindDoD: function (tilekey) {

            var parsedKey = tilekey.split(','),
                thisKeyX = parseInt(parsedKey[1]),
                thisKeyY = parseInt(parsedKey[2]);

            return (this.mouseState.clickState.tilekey === '' || // nothing clicked, or
                // not under details on demand window
                    this.mouseState.clickState.xIndex+1 !== thisKeyX ||
                   (this.mouseState.clickState.yIndex !== thisKeyY &&
                    this.mouseState.clickState.yIndex-1 !==  thisKeyY));
        },

        onUnselect: function() {
        },


        createLayer: function (nodeLayer) {
        },


        redrawLayers: function() {
        }

    });

    return ClientRenderer;
});
