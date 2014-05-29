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



    var Class = require('../../../class'),
        ClientRenderer;



    ClientRenderer = Class.extend({
        ClassName: "ClientRenderer",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function(map) {

            this.map = map;
            this.clientState = null;

            this.TILE_SIZE = 256;

            this.BLACK_COLOUR = '#000000';
            this.DARK_GREY_COLOUR = '#222222';
            this.GREY_COLOUR = '#666666';
            this.LIGHT_GREY_COLOUR = '#999999';
            this.WHITE_COLOUR = '#FFFFFF';
            this.BLUE_COLOUR = '#09CFFF';
            this.DARK_BLUE_COLOUR  = '#069CCC';
            this.PURPLE_COLOUR = '#D33CFF';
            this.DARK_PURPLE_COLOUR = '#A009CC';
            this.YELLOW_COLOUR = '#F5F56F';
        },


        /**
         * Returns the opacity of the layer set in the layer controls
         */
        getOpacity: function() {
            return this.clientState.getSharedState('opacity');
        },


        /**
         * Returns true if the layer is enabled in the layer controls
         */
        isVisible: function() {
            return this.clientState.getSharedState('isVisible');
        },

        /**
         * Attaches a mouse state object to be shared by the render layer. A ViewController has
         * primary ownership of this object. It is too be shared with each render layer so that
         * mouse events can be handled across each layer
         * @param clientState the mouse state object
         */
        attachClientState: function(clientState) {
            this.clientState = clientState;
        },


        /**
         * Helper function for converting hex code colour to greyscale
         * @param hex the hex code colour
         */
        hexToGreyscale: function(hex) {
            var col, rgb;
            if (hex[0] === '#') {
                hex = hex.substr(1,6);
            }
            col = parseInt(hex, 16);
            rgb = (((((((col >> 16) & 0xff)*76) + (((col >> 8) & 0xff)*150) +
                   ((col & 0xff)*29)) >> 8)) << 16) |

                   (((((((col >> 16) & 0xff)*76) + (((col >> 8) & 0xff)*150) +
                   ((col & 0xff)*29)) >> 8)) << 8) |

                   ((((((col >> 16) & 0xff)*76) + (((col >> 8) & 0xff)*150) +
                   ((col & 0xff)*29)) >> 8));

            return this.rgbToHex( (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
        },


        /**
         * Helper function for converting rgb components to hex code colour
         * @param r red component, should be in range 0-255
         * @param g green component, should be in range 0-255
         * @param b blue component, should be in range 0-255
         */
         rgbToHex : function(r, g, b) {
             function componentToHex(c) {
                 var hex = c.toString(16);
                 return (hex.length === 1) ? "0" + hex : hex;
             }
             return "#" + componentToHex( Math.floor(r)) +
                          componentToHex( Math.floor(g)) +
                          componentToHex( Math.floor(b));
        },


        /**
         * Helper function for converting hex code colour to rgb components
         * @param hex the hex code colour
         */
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


        redraw: function() {
            return true;
        }

    });

    return ClientRenderer;
});
