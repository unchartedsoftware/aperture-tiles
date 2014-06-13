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
        LayerState;

    


    LayerState = Class.extend({
        ClassName: "LayerState",

        /**
         * Initializes a LayerState object with default values.
         *
         * @param {string} id - The immutable ID of the layer.
         */
        init: function (id) {
            this.domain = null;
            this.id = id;
            this.name = id; // for now just set name to to he id
            this.zIndex = 0;
            this.enabled = false;
            this.opacity = 1.0;
            this.listeners = [];
        },


        /**
         * @param {string} fieldName - Name of the modified field.
         * @param {Array} listeners - Array of {valueChange} listeners to execute.
         */
        notify : function (fieldName, listeners) {
            var i;
            for (i = 0; i < listeners.length; i += 1) {
                listeners[i](fieldName);
            }
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
                this.listeners.splice(index, 1);
            }
        },

        /**
         * @returns {string} - The ID of this layer state object.
         */
        getId: function () {
            return this.id;
        },

        /**
         * @returns {number} - The Z index of the layer.  Layers are drawn starting at 0, going from lowest
         * to highest.
         */
        getZIndex: function () {
            return this.zIndex;
        },


        /**
         * @param {number} - The Z index of the layer.  Layers are drawn starting at 0, going from lowest
         * to highest.
         */
        setZIndex: function (zIndex) {
            if (this.zIndex !== zIndex) {
                this.zIndex = zIndex;
                this.notify("zIndex", this.listeners);
            }
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
                this.notify("name", this.listeners);
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
                this.notify("enabled", this.listeners);
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
                this.notify("opacity", this.listeners);
            }
        }

    });


    return LayerState;
});
