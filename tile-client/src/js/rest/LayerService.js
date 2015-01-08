/*
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

/**
 * @namespace LayerService
 * @classdesc A utility namespace that provides layer service REST functionality.
 */
( function() {

    "use strict";

    var Util = require('../util/Util');

    module.exports = {

        /**
         * Request all layers from the server. Upon success, will execute success
         * callback function passing the resulting object as first argument.
         * @memberof LayerService
         *
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getLayers: function( success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers'
            ).then(
                _success,
                Util.handleHTTPError
            );
        },

        /**
         * Request a specific layer from the server. Upon success, will execute success
         * callback function passing the resulting object as first argument.
         * @memberof LayerService
         *
         * @param {String} layerId - The layer identification string.
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getLayer: function( layerId, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers/' + layerId
            ).then(
                _success,
                Util.handleHTTPError
            );
        },

        /**
         * Store a configuration state on the server. Upon success, will execute success
         * callback function passing the resulting object as first argument.
         * @memberof LayerService
         *
         * @param {String} layerId - The layer identification string.
         * @param {Object} params - The parameter configuration overrides (optional).
         * @param {Function} success - The callback function executed after success received (optional).
         */
        saveLayerState: function( layerId, params, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/layers/' + layerId + '/states',
                JSON.stringify( params )
            ).then(
                _success,
                Util.handleHTTPError
            );
        },

        /**
         * Get all configuration states for a layer on the server. Upon success, will execute
         * success callback function passing the resulting object as first argument.
         * @memberof LayerService
         *
         * @param {String} layerId - The layer identification string.
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getLayerStates: function( layerId, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers/' + layerId + '/states'
            ).then(
                _success,
                Util.handleHTTPError
            );
        },

        /**
         * Get a configuration state for a layer on the server by state id. Upon success,
         * will execute success callback function passing the resulting object as first argument.
         * @memberof LayerService
         *
         * @param {String} layerId - The layer identification string.
         * @param {String} stateId - The state identification string.
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getLayerState: function( layerId, stateId, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers/' + layerId + '/states/' + stateId
            ).then(
                _success,
                Util.handleHTTPError
            );
        }
    };
}());
