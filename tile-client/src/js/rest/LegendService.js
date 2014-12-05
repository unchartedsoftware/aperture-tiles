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

    var $ = require('jquery'),
        _ = require('lodash');

    /**
     * Private: encodes parameter object into a dot notation query
     * parameter string.
     *
     * @param params {Object} parameter object
     */
    function encodeQueryParams( params ) {
        var query;
        function traverseParams( params, query ) {
            var result = "";
            _.forIn( params, function( value, key ) {
                if ( value instanceof Array ) {
                    result += query + key + '=' + value.join(',') + "&";
                } else if ( typeof value !== "object" ) {
                    result += query + key + '=' + value + "&";
                } else {
                    result += traverseParams( params[ key ], query + key + "." );
                }
            });
            return result;
        }
        query = "?" + traverseParams( params, '' );
        return query.slice( 0, query.length - 1 );
    }

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
         * Get an encoded image string representing the rendering legend.
         *
         * @param layerId   {String}   layer id
         * @param [params]  {Object}   query parameter configuration overrides (optional)
         * @param [success] {Function} function called after success received (optional)
         */
        getEncodedImage: function( layerId, params, success ) {
            var _params = ( typeof params === "object" ) ? params : null,
                _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/legend/' + layerId + encodeQueryParams( _params )
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Get a png image representing the rendering legend.
         *
         * @param layerId   {String}   layer id
         * @param [params]  {Object}   query parameter configuration overrides (optional)
         * @param [success] {Function} function called after success received (optional)
         */
        getImage: function( layerId, params, success ) {
            var _params = ( typeof params === "object" ) ? params : null,
                _success = ( typeof success === "function" ) ? success : null;
            // explicitly set output type to png image
            _params.output = 'png';
            $.get(
                'rest/v1.0/legend/' + layerId + encodeQueryParams( _params )
            ).then(
                _success,
                handleError
            );
        }
    };
}());
