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
        arraysEqual,
        ServerLayerState;



    /**
     * Compares arrays for equality.
     *
     * @param {Array} a - First array under comparison
     * @param {Array} b - Second array under comparison
     * @returns {boolean} true if they are equal, false otherwise.
     */
    arraysEqual = function (a, b) {
        var i;
        if (a === b) {
            return true;
        }
        if (a === null || b === null) {
            return false;
        }
        if (a.length !== b.length) {
            return false;
        }
        for (i = 0; i < a.length; ++i) {
            if (a[i] !== b[i]) {
                return false;
            }
        }
        return true;
    };

    ServerLayerState = LayerState.extend({
        ClassName: "ServerLayerState",

        /**
         * Valid ramp type strings.
         */
        RAMP_TYPES : [
            {id: "ware", name: "Ware"},
            {id: "inv-ware", name: "Inverse Ware"},
            {id: "br", name: "Blue/Red"},
            {id: "inv-br", name: "Inverse Blue/Red"},
            {id: "grey", name: "Grey"},
            {id: "inv-grey", name: "Inverse Grey"},
            {id: "flat", name: "Flat"},
            {id: "single-gradient", name: "Single Gradient"}
        ],


        /**
         * Valid ramp function strings.
         */
        RAMP_FUNCTIONS : [
            {id: "linear", name: "Linear"},
            {id: "log10", name: "Log 10"}
        ],

        /**
         * Initializes a LayerState object with default values.
         *
         * @param {string} id - The immutable ID of the layer.
         */
        init: function ( id ) {
            this._super( id );
            this.domain = 'server';
            this.filterRange = [0.0, 1.0];
            this.rampType = "ware";
            this.rampFunction = "linear";
            this.rampImageUrl = "";
            this.rampLevel = 0;
            this.rampMinMax = [0,10];
        },

        /**
         * @returns {Array} - A 2 element array containing the min and max filter values.  These values range
         * from [0.0 - 1.0], representing a fraction of the total data range.
         */
        getFilterRange: function () {
            return this.filterRange;
        },

        /**
         * @param {Array} filterRange - A 2 element array containing the min and max filter values.  These values range
         * from [0.0 - 1.0], representing a fraction of the total data range.
         */
        setFilterRange: function (filterRange) {
            if (!arraysEqual(this.filterRange, filterRange)) {
                this.filterRange = filterRange;
                this.notify("filterRange", this.listeners);
            }
        },

        /**
         * @returns {string} - The type of the colour ramp that is applied to the layer when displayed.  See
         * RAMP_TYPES for valid ramp types.
         */
        getRampType: function () {
            return this.rampType;
        },

        /**
         * @param {string} rampType - The type of the colour ramp that is applied to the layer when displayed.  See
         * RAMP_TYPES for valid ramp types.
         */
        setRampType: function (rampType) {
            if (this.rampType !== rampType) {
                this.rampType = rampType;
                this.notify("rampType", this.listeners);
            }
        },

        /**
         * @returns {string} - The transformation to apply to the ramp.  See RAMP_FUNCTIONS for valid values.
         */
        getRampFunction: function () {
            return this.rampFunction;
        },

        /**
         * @param {string} rampFunction - See RAMP_FUNCTIONS for valid values.
         */
        setRampFunction: function (rampFunction) {
            if (this.rampFunction !== rampFunction) {
                this.rampFunction = rampFunction;
                this.notify("rampFunction", this.listeners);
            }
        },

        /**
         * @returns {string} - The URL of an image representing the colour ramp defined by the ramp type and function.
         */
        getRampImageUrl: function () {
            return this.rampImageUrl;
        },

        /**
         *
         * @param {string} url - The URL of an image representing the colour ramp defined by the ramp type and function.
         */
        setRampImageUrl: function (url) {
            if (this.rampImageUrl !== url) {
                this.rampImageUrl = url;
                this.notify("rampImageUrl", this.listeners);
            }
        },

        setRampMinMax: function(minMax) {
            if (this.rampMinMax !== minMax ) {
                this.rampMinMax = minMax;
                this.notify("rampMinMax", this.listeners);
            }
        },

        getRampMinMax: function () {
            return this.rampMinMax;
        }
    });




    return ServerLayerState;
});
