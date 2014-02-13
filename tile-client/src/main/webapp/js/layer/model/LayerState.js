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

    var Class = require('../../class'),
        LayerState, notify;

    /**
     * @param {string} fieldName - Name of the modified field.
     * @param {Array} listeners - Array of {valueChange} listeners to execute.
     */
    notify = function (fieldName, listeners) {
        var i;
        for (i = 0; i < listeners.length; i += 1) {
            listeners[i](fieldName);
        }
    };

    LayerState = Class.extend({
        ClassName: "LayerState",

        /**
         * Initializes a LayerState object with default values.
         *
         * @param {string} id - The immutable ID of the layer.
         */
        init: function (id) {
            this.id = id;
            this.name = id;
            this.enabled = false;
            this.opacity = 1.0;
            this.filterRange = [0.0, 1.0];
            this.rampType = "ware";
            this.rampFunction = "linear";
            this.rampImageUrl = "";
            this.listeners = [];
        },


        /**
         * Listener used when a layer state value has been modified.
         * @callback valueChange
         * @param {string} - the name of the modified field.
         */

        /**
         * Registers a listener that will be executed whenever a layer state value
         * is modified.
         *
         * @param {valueChange} listener - The listener to register.
         */
        addListener: function (listener) {
            this.listeners.push(listener);
        },

        /**
         * Removes a callback if it exists.
         *
         * @param {valueChange} listener - The callback to remove.
         */
        removeListener: function (listener) {
            var index = this.listeners.indexOf(listener);
            if (index > -1) {
                this.listeners = this.listeners.splice(index, 1);
            }
        },

        /**
         * @returns {string} - The ID of this layer state object.
         */
        getId: function () {
            return this.id;
        },

        /**
         * @returns {string} - The simple name of the layer.  This can appear in user facing elements and
         * should be formatted accordingly.
         */
        getName: function () {
            return this.name;
        },

        /**
         * @param name - The simple name of the layer.  This can appear in user facing elements and should be formatted
         * accordingly.
         */
        setName: function (name) {
            if (this.name !== name) {
                this.name = name;
                notify("name", this.listeners);
            }
        },

        /**
         * @returns {boolean} - TRUE if the layer should be treated as visible, FALSE otherwise.
         */
        isEnabled: function () {
            return this.enabled;
        },

        /**
         * @param {boolean} enabled - TRUE if the layer is visible, FALSE otherwise.
         */
        setEnabled: function (enabled) {
            if (this.enabled !== enabled) {
                this.enabled = enabled;
                notify("enabled", this.listeners);
            }
        },

        /**
         * @returns {number} - A floating point value from 0.0 - 1.0 representing the opacity of the layer,
         * where 0.0 is transparent and 1.0 is opaque.
         */
        getOpacity: function () {
            return this.opacity;
        },

        /**
         * @param {number} opacity - A floating point value from 0.0 - 1.0 representing the opacity of the layer, where 0.0 is
         * transparent and 1.0 is opaque.
         */
        setOpacity: function (opacity) {
            if (this.opacity !== opacity) {
                this.opacity = opacity;
                notify("opacity", this.listeners);
            }
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
        	if (filterRange) {
        		if (this.filterRange[0] !== filterRange[0] && this.filterRange[1] !== filterRange[1]) {
        			this.filterRange = filterRange;
        			notify("filterRange", this.listeners);
        		}
            } else {
            	this.filterRange = null;
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
                notify("rampType", this.listeners);
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
                notify("rampFunction", this.listeners);
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
                notify("rampImageUrl", this.listeners);
            }
        }
    });

    /**
     * Valid ramp type strings.
     */
    LayerState.RAMP_TYPES = {
        WARE: { id: "ware", name: "Ware"},
        INV_WARE: {id: "inv-ware", name: "Inverse Ware"},
        BR: {id: "br", name: "Blue/Red"},
        INV_BR: {id: "inv-br", name: "Inverse Blue/Red"},
        GREY: {id: "grey", name: "Grey"},
        INV_GREY: {id: "inv-grey", name: "Inverse Grey"},
        FLAT: {id: "flat", name: "Flat"},
        SINGLE_GRADIENT: {id: "single-gradient", name: "Single Gradient"}
    };

    /**
     * Valid ramp function strings.
     */
    LayerState.RAMP_FUNCTIONS = {
        LINEAR: {id: "linear", name: "Linear"},
        LOG10: {id: "log10", name: "Log 10"}
    };


    return LayerState;
});
