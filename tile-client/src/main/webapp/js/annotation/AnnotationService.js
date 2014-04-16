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



    var Class = require('../class'),
        AnnotationService;



    AnnotationService = Class.extend({
        ClassName: "AnnotationService",

        /**
         * Construct an AnnotationService
         */
        init: function ( layer ) {

            this.layer = layer;

        },


        /**
         * send a GET request to the server to pull all annotation data for a specific tilekey
         * @param tilekey   tile identification key
         * @param callback  the callback that is called upon receiving data from server
         */
        getAnnotations: function( tilekey, callback ) {

            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            // request data from server
            aperture.io.rest(
                ('/annotation/'+
                    this.layer+'/'+
                    level+'/'+
                    xIndex+'/'+
                    yIndex+'.json'),
                'GET',
                callback
            );
        },


        /**
         * write the annotation to the server
         * @param annotation   annotation to be written
         * @param callback     the callback that is called upon receiving data from server
         */
        writeAnnotation: function( annotation, callback ) {

            // generate uuid for data
            annotation.uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random()*16|0, v = (c === 'x') ? r : (r&0x3|0x8);
                return v.toString(16);
            });

            this.postRequest( "WRITE", annotation, callback );
        },


        /**
         * modifiy an the annotation on the server
         * @param oldAnnotation   old state of the annotation to be modified
         * @param newAnnotation   new state of the annotation to be modified
         * @param callback        the callback that is called upon receiving data from server
         */
        modifyAnnotation: function( oldAnnotation, newAnnotation, callback ) {

            var annotation = {
                "old": oldAnnotation,
                "new": newAnnotation
            };

            this.postRequest( "MODIFY", annotation, callback );
        },


        /**
         * remove the annotation from the server
         * @param annotation   annotation to be removed
         * @param callback     the callback that is called upon receiving data from server
         */
        removeAnnotation: function( annotation, callback ) {

            this.postRequest( "REMOVE", annotation, callback );
        },


        /**
         * send a POST request to the server
         * @param type   type of annotation service: "WRITE", "MODIFY", or "REMOVE"
         * @param data   annotation data to send server
         * @param callback  the callback that is called upon receiving data from server
         */
        postRequest: function( type, data, callback ) {

            // Request the layer information
            aperture.io.rest('/annotation',
                             'POST',
                             callback,
                             {
                                 postData: {
                                                "layer": this.layer,
                                                "type": type.toLowerCase(),
                                                "data" : data
                                            },
                                 contentType: 'application/json'
                             });

        }

    });

    return AnnotationService;
});
