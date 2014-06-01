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
        },


        setOpacity: function( opacity ) {
            this.nodeLayer.$root_.css( 'opacity', opacity );
        },

        setVisibility: function( visible ) {
            var visibility = visible ? 'visible' : 'hidden';
            this.nodeLayer.$root_.css( 'visibility', visibility );
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

        redraw: function() {
            return true;
        }

    });

    return ClientRenderer;
});
