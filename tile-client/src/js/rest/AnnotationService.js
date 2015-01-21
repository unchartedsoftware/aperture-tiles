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
 * @namespace AnnotationService
 * @classdesc A utility namespace that provides annotation service REST functionality.
 */
( function() {

    "use strict";

    var Util = require('../util/Util');

    module.exports = {

        /**
         * Get a tiles worth of annotations from the server. Upon success, will execute success
         * callback function passing the resulting object as first argument.
         * @memberof AnnotationService
         *
         * @param {String} layerId - The annotation layer identification string.
         * @param {integer} level - The zoom level.
         * @param {integer} x - The tile x index.
         * @param {integer} y - The tile y index.
         * @param {Object} params - The query parameter configuration overrides (optional).
         * @param {Function} success - The callback function executed after success received (optional).
         */
        getTileJSON: function( layerId, level, x, y, params, success ) {
            var _params = ( typeof params === "object" ) ? params : null,
                _success = ( typeof success === "function" )
                    ? success
                    : ( typeof params === "function" )
                        ? params
                        : null;
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
         * Write an annotation to the server. Upon success, will execute success
         * callback function passing the resulting certificate as first argument.
         * @memberof AnnotationService
         *
         * @param {String} layerId - The annotation layer identification string.
         * @param {Object} annotation - The annotation to be written.
         * @param {Function} success - The callback function executed after success received (optional).
         */
        writeAnnotation: function( layerId, annotation, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/annotation',
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
         * Modify an annotation on the server. Upon success, will execute success
         * callback function passing the resulting certificate as first argument.
         * @memberof AnnotationService
         *
         * @param {String} layerId - The annotation layer identification string.
         * @param {Object} annotation - The annotation to be modified.
         * @param {Function} success - The callback function executed after success received (optional).
         */
        modifyAnnotation: function( layerId, annotation, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/annotation',
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
         * Remove an annotation from the server.
         * @memberof AnnotationService
         *
         * @param {String} layerId - The annotation layer identification string.
         * @param {Object} certificate - The certificate of the annotation to be removed.
         * @param {Function} success - The callback function executed after success received (optional).
         */
        removeAnnotation: function( layerId, certificate, success ) {
            var _success = ( typeof success === "function" ) ? success : null;
            $.post(
                'rest/v1.0/annotation',
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
