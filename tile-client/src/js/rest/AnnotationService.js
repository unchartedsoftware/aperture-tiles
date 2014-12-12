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
 * A namespace that provides annotation service functionality. Functionality
 * includes:
 *
 *      - Retrieving a tiles worth of annotation data via GET request
 *      - Writing an annotation via POST request
 *      - Modifying an annotation via PPOST request
 *      - Removing an annotation via POST request
 */
( function() {

    "use strict";

    var Util = require('../util/Util');

    module.exports = {

        /**
         * Get a tiles worth of annotations from the server. Upon success, will execute success
         * callback function passing the resulting object as first argument.
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
                + y + ".json" + Util.encodeQueryParams( _params )
            ).then(
                _success,
                Util.handleHTTPError
            );
        },

        /**
         * Write the annotation to the server. Upon success, will execute success
         * callback function passing the resulting certificate as first argument.
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
                Util.handleHTTPError
            );
        },

        /**
         * Modify an the annotation on the server. Upon success, will execute success
         * callback function passing the resulting certificate as first argument.
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
                Util.handleHTTPError
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
                Util.handleHTTPError
            );
        }

    };
}());
