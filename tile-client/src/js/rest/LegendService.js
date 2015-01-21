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
 * @namespace LegendService
 * @classdesc A utility namespace that provides legend service REST functionality.
 */
( function() {

    "use strict";

    var Util = require('../util/Util');

    module.exports = {

        /**
         * Get an encoded image string representing the rendering legend. Upon success,
         * will execute success callback function passing the resulting string as first
         * argument.
         * @memberof LegendService
         *
         * @param {String} layerId - The layer identification string.
         * @param {Object} params - The query parameter configuration overrides (optional).
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getEncodedImage: function( layerId, params, success ) {
            var _params = ( typeof params === "object" ) ? params : null,
                _success = ( typeof success === "function" )
                    ? success
                    : ( typeof params === "function" )
                        ? params
                        : null;
            $.get(
                'rest/v1.0/legend/' + layerId + Util.encodeQueryParams( _params )
            ).then(
                _success,
                Util.handleHTTPError
            );
        },

        /**
         * Get a png image representing the rendering legend.
         * @memberof LegendService
         *
         * @param {String} layerId - The layer identification string.
         * @param {Object} params - The query parameter configuration overrides (optional).
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getImage: function( layerId, params, success ) {
            var _params = ( typeof params === "object" ) ? params : {},
                _success = ( typeof success === "function" )
                    ? success
                    : ( typeof params === "function" )
                        ? params
                        : null;
            // explicitly set output type to png image
            _params.output = 'png';
            $.get(
                'rest/v1.0/legend/' + layerId + Util.encodeQueryParams( _params )
            ).then(
                _success,
                Util.handleHTTPError
            );
        }
    };
}());
