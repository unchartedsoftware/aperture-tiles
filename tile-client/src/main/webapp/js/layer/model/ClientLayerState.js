/**
 * Copyright (c) 2014 Oculus Info Inc.
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

/*global $, define*/

/**
 * @class LayerState
 *
 * Captures the visual state of a layer in the system, and provides a notification
 * mechanism to allow external code to react to changes to it.
 */
define(function (require) {
    "use strict";



    var LayerState = require('./LayerState'),
        objectsEqual,
        ClientLayerState;


    /**
     * Compares objects for equality.
     *
     * @param {Object} a - First object under comparison
     * @param {Object} b - Second object under comparison
     * @returns {boolean} true if they are equal, false otherwise.
     */
    objectsEqual = function (a, b) {

        var keyA, keyB, found;
        for ( keyA in a ) {         // iterate through a
            if ( a.hasOwnProperty(keyA) ) {
                found = false;
                for ( keyB in b ) { // iterate through by
                    if ( b.hasOwnProperty(keyB) ) {
                        if ( a[keyA] === b[keyB] ) {
                            found = true; // found and equal
                        }
                    }
                }
                if ( !found ) {
                    return false;
                }
            }
        }
        return true;
    };


    ClientLayerState = LayerState.extend({
        ClassName: "ClientLayerState",

        /**
         * Initializes a LayerState object with default values.
         *
         * @param {string} id - The immutable ID of the layer.
         */
        init: function ( id ) {

            this._super( id );
            this.domain = 'client';
            this.tileFocus = "";

            this.carouselEnabled = false;

            this.clickState = {};
            this.hoverState = {};

            this.rendererCount = 0;
            this.rendererIndicesByTile = {};
            this.defaultRendererIndex = 0;
        },

        setRendererCount: function(count) {
            if (this.rendererCount !== count) {
                this.rendererCount = count;
                this.notify("rendererCount", this.listeners);
            }
        },


        getRendererCount: function() {
            return this.rendererCount;
        },


        setDefaultRendererIndex: function( defaultRendererIndex ) {
            if ( this.defaultRendererIndex !== defaultRendererIndex ) {
                this.defaultRendererIndex = defaultRendererIndex;
                this.notify("defaultRendererIndex", this.listeners);
            }
        },


        getDefaultRendererIndex: function() {
            return this.defaultRendererIndex;
        },


        setRendererByTile: function( tilekey, index ) {

            var renderersByTile = this.rendererIndicesByTile;

            if ( renderersByTile[tilekey] !== index ) {
                if ( index === this.defaultRendererIndex ) {
                    delete renderersByTile[tilekey];
                } else {
                    renderersByTile[tilekey] = index;
                }
                this.notify("tileRendererIndex", this.listeners);
            }
        },

        getRendererByTile: function( tilekey ) {
            return this.rendererIndicesByTile[tilekey] || this.defaultRendererIndex;
        },

        getRenderersByTile: function() {
            return this.rendererIndicesByTile;
        },

        setCarouselEnabled: function( enabled ) {
            if (this.carouselEnabled !== enabled) {
                this.carouselEnabled = enabled;
                this.notify("carouselEnabled", this.listeners);
            }
        },

        getCarouselEnabled: function( enabled ) {
            return this.carouselEnabled;
        },


        setTileFocus: function( tilekey ) {
            if (this.tileFocus !== tilekey) {
                this.tileFocus = tilekey;
                this.notify("tileFocus", this.listeners);
            }
        },

        getTileFocus: function() {
            return this.tileFocus;
        },

        setClickState: function( clickState ) {
            if ( !objectsEqual( this.clickState, clickState ) ) {
                this.clickState = clickState;
                this.notify("clickState", this.listeners);
            }
        },

        getClickState: function() {
            return this.clickState;
        },

        setHoverState: function( hoverState ) {
            if ( !objectsEqual( this.hoverState, hoverState ) ) {
                this.hoverState = hoverState;
                this.notify("hoverState", this.listeners);
            }
        },

        getHoverState: function() {
            return this.hoverState;
        }

    });

    return ClientLayerState;
});
