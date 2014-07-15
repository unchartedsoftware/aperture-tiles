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
 * This module defines a AnnotationService class which manages all REST calls to the server for annotations
 */
define(function (require) {
    "use strict";



    var AnnotationService;


    AnnotationService = {

        /**
         * send a GET request to the server to pull all annotation data for an array of tilekeys
         * @param layerInfo    annotation layer info
         * @param tilekeys     array of tile identification keys
         * @param callback     the callback that is called upon receiving data from server
         */
        getAnnotations: function( layerInfo, tilekeys, callback) {

            var i;
            if ( !$.isArray( tilekeys ) ) {
                tilekeys = [ tilekeys ];
            }
            for (i=0; i<tilekeys.length; ++i) {
                this.getAnnotation( layerInfo, tilekeys[i], callback );
            }
        },

         /**
         * send a GET request to the server to pull all annotation data for a specific tilekey
         * @param layerInfo    annotation layer info
         * @param tilekey      tile identification key
         * @param callback     the callback that is called upon receiving data from server
         */
        getAnnotation: function( layerInfo, tilekey, callback ) {

            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            // request data from server
            aperture.io.rest(
                '/annotation/'+
                 layerInfo.layer+'/'+
                 layerInfo.uuid+'/'+
                 level+'/'+
                 xIndex+'/'+
                 yIndex+'.json',
                'GET',
                callback
            );
        },


        /**
         * write the annotation to the server
         * @param layerInfo    annotation layer info
         * @param annotation   annotation to be written
         * @param callback     the callback that is called upon receiving data from server
         */
        writeAnnotation: function( layerInfo, annotation, callback ) {

            var request = {
                    type: "WRITE",
                    annotation: annotation
                };

            this.postRequest( layerInfo, request, function( result, statusInfo ) {
                if (statusInfo.success) {
                    // update certificate on success
                    annotation.certificate = result;
                }
                callback( annotation, statusInfo );
            });
        },


        /**
         * modify an the annotation on the server
         * @param layerInfo     annotation layer info
         * @param certificate   annotation to be modified
         * @param callback      the callback that is called upon receiving data from server
         */
        modifyAnnotation: function( layerInfo, annotation, callback ) {

            var request = {
                    type: "MODIFY",
                    annotation: annotation
                };

            this.postRequest( layerInfo, request, function( result, statusInfo ) {
                if (statusInfo.success) {
                    // update certificate on success
                    annotation.certificate = result;
                }
                callback( annotation, statusInfo );
            });

        },


        /**
         * remove the annotation from the server
         * @param layerInfo     annotation layer info
         * @param certificate   certificate of annotation to be removed
         * @param callback      the callback that is called upon receiving data from server
         */
        removeAnnotation: function( layerInfo, certificate, callback ) {

            var request = {
                    type: "REMOVE",
                    certificate: certificate
                };

            this.postRequest( layerInfo, request, callback );
        },


        /**
         * Set new server side annotation configuration
         * @param layerSpec    configuration spec to be passed to server
         * @param callback     the callback that is called upon receiving data from server
         */
        configureLayer: function( layerSpec, callback ) {

            var request = {
                    type : "CONFIGURE",
                    configuration: layerSpec
                };

            this.postRequest( layerSpec, request, callback );
        },


        /**
         * Release server side annotation filters
         * @param layerInfo the annotation layer info to be unconfigured
         * @param callback  the callback that is called upon receiving data from server
         */
        unconfigureLayer: function( layerInfo, callback ) {

            var request = {
                    type : "UNCONFIGURE",
                    uuid: layerInfo.uuid
                };

            this.postRequest( layerInfo, request, callback );
        },


        /**
         * send a POST request to the server
         * @param layerInfo annotation layer info
         * @param request   request json to POST to server
         * @param callback  the callback that is called upon receiving data from server
         */
        postRequest: function( layerInfo, request, callback ) {

            // append layer id to request
            request.layer = layerInfo.layer;

            // Request the layer information
            aperture.io.rest('/annotation',
                             'POST',
                             callback,
                             {
                                 postData: request,
                                 contentType: 'application/json'
                             });
        },


        /**
         * Receive all annotation layers from server
         * @param callback     the callback that is called upon receiving data from server
         */
        requestLayers : function() {

            var annotationDeferred = $.Deferred();

            // Request the layer information
            aperture.io.rest('/annotation',
                             'POST',
                             function (layers) {
                                 annotationDeferred.resolve(layers);
                             },
                             {
                                 postData: {
                                     "type": "list"
                                 },
                                 contentType: 'application/json'
                             });

            return annotationDeferred;
        }

    };



    return AnnotationService;
});
