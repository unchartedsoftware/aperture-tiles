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

define( function() {
    "use strict";

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

    return {

        /**
         * Get a tiles worth of annotations from the server.
         *
         * @param layerId   {String}   annotation layer id
         * @param level     {int}      tile level
         * @param x         {int}      tile x index
         * @param y         {int}      tile y index
         * @param [params]  {Object}   query parameter configuration overrides (optional)
         * @param [success] {Function} function called after success received (optional)
         */
        getTileJSON: function( layerId, level, x, y, params, success ) {
            var _params = ( typeof params === "object" ) ? params : null,
                _success = ( typeof success === "function" ) ? success : null;
            $.get(
                'rest/v1.0/annotation/'
                + layerId + "/"
                + level + "/"
                + x + "/"
                + y + ".json" + encodeQueryParams( _params )
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Write the annotation to the server.
         *
         * @param layerId    {String}   annotation layer id
         * @param annotation {Object}   annotation to be written
         * @param [success]  {Function} function called after success received (optional)
         */
        writeAnnotation: function( layerId, annotation, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/annotation/',
                JSON.stringify({
                    type: "write",
                    annotation: annotation,
                    layer: layerId
                })
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Modify an the annotation on the server.
         *
         * @param layerId    {String}   annotation layer id
         * @param annotation {Object}   annotation to be modified
         * @param [success]  {Function} function called after success received (optional)
         */
        modifyAnnotation: function( layerId, annotation, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/annotation/',
                JSON.stringify({
                    type: "modify",
                    annotation: annotation,
                    layer: layerId
                })
            ).then(
                _success,
                handleError
            );
        },

        /**
         * Remove the annotation from the server.
         *
         * @param layerId     {String}   annotation layer id
         * @param certificate {Object}   certificate of annotation to be removed
         * @param [success]   {Function} function called after success received (optional)
         */
        removeAnnotation: function( layerId, certificate, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/annotation/',
                JSON.stringify({
                    type: "remove",
                    certificate: certificate,
                    layer: layerId
                })
            ).then(
                _success,
                handleError
            );
        }

    };
});
