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

( function() {

    "use strict";

    var $ = require('jquery');

    /**
     * Private: encodes parameter object into query parameter string.
     *
     * @param xhr {XmlHttpRequest} XmlHttpRequest object
     */
    function handleError( xhr ) {
        console.error( xhr.responseText );
        console.error( xhr );
    }

    module.exports = {

        /**
         * Request all layers from the server.
         *
         * @param [success] {Function} function called after success received (optional)
         */
        getLayers: function( success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers'
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Request a specific layer from the server, sending it to the listed callback
         * function when it is received.
         *
         * @param layerId   {String}   layer id
         * @param [success] {Function} function called after success received (optional)
         */
        getLayer: function( layerId, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers/' + layerId
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Store a configuration state on the server.
         *
         * @param layerId   {String}   layer id
         * @param params    {Object}   layer configuration parameters
         * @param [success] {Function} function called after success received (optional)
         */
        saveLayerState: function( layerId, params, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/layers/' + layerId + '/states',
                JSON.stringify( params )
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Get all configuration states for a layer on the server.
         *
         * @param layerId   {String}   layer id
         * @param [success] {Function} function called after success received (optional)
         */
        getLayerStates: function( layerId, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/layers/' + layerId + '/states'
            ).then(
                _success,
                handleError
            );
        }
    };
}());
