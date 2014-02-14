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
		idIncrement = 0,
        ClientRenderer;



    ClientRenderer = Class.extend({
        ClassName: "ClientRenderer",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function(id, avoidIncrement) {
			// ensure each render layer has a unique id for view controller to maintain visibility correctly
			id = id || "renderer-id";
			if (avoidIncrement) {
				this.id = id;	// don't increment id, used for nested renderers
			} else {
				this.id = id + "-" + idIncrement++;
			}
            
            this.mouseState = null;
        },


        /**
         * Returns true if the layer is selected for the respective tile by the view controller,
         * the data.renderer attribute is assigned by the TileTracker
         * @param data aperturejs node data object
         */
        isSelectedView: function(data) {
            return data.renderer[this.id] === true;
        },


        /**
         * Attaches a mouse state object to be shared by the render layer. A ViewController has
         * primary ownership of this object. It is too be shared with each render layer so that
         * mouse events can be handled across each layer
         * @param mouseState the mouse state object
         */
        attachMouseState: function(mouseState) {
            this.mouseState = mouseState;
        },


        /**
         * Empty function to create the layer, must be overloaded
         * @param nodeLayer the aperturejs.geo.mapnodelayer
         */
        createLayer: function (nodeLayer) {
			console.log('Warning createLayer function in client renderer has not been overloaded');
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
        }

    });

    return ClientRenderer;
});
