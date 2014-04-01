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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines a DataTracker class which manages all tile data from a
 * single dataset.
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        AnnotationTracker;


    AnnotationTracker = Class.extend({
        ClassName: "AnnotationTracker",

        /**
         * Construct a DataTracker
         */
        init: function ( layer ) {

            this.layer = layer;
            this.annotations = {};
            this.annotationStatus = {};
            this.getCallbacks = {};
            this.annotatonStatus = {};
            /*
            // set tile pyramid type
            if (this.layerInfo.projection === "EPSG:900913") {
                // mercator projection
                this.tilePyramid = new WebPyramid();
            } else {
                // linear projection, pass bounds of data
                this.tilePyramid = new AoITilePyramid(this.layerInfo.bounds[0],
                                                      this.layerInfo.bounds[1],
                                                      this.layerInfo.bounds[2],
                                                      this.layerInfo.bounds[3]);
            }
            */
            
        },


        createTileKey: function (tile) {
            return tile.level + "," + tile.xIndex + "," + tile.yIndex;
        },


        getAnnotations: function(requestedTiles, callback) {
            var i;
            // send request to respective coordinator
            for (i=0; i<requestedTiles.length; ++i) {
                this.getAnnotation( this.createTileKey( requestedTiles[i] ), callback );
            }
        },


        getAnnotation: function(tilekey, callback) {

            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);


            if (this.annotationStatus[tilekey] === undefined) {

                // flag tile as loading, add callback to list
                this.annotationStatus[tilekey] = "loading";
                this.getCallbacks[tilekey] = [];
                this.getCallbacks[tilekey].push(callback);

                // request data from server
                aperture.io.rest(
                    ('/annotation/'+
                     this.layer+'/'+
                     level+'/'+
                     xIndex+'/'+
                     yIndex+'.json'),
                     'GET',
                    $.proxy( this.getCallback, this )
                );

            } else {

                if (this.annotationStatus[tilekey] === "loaded") {
                    callback(this.annotations[tilekey]);
                    return;
                }
                // waiting on tile from server, add to callback list
                this.getCallbacks[tilekey].push(callback);
            }

        },


        getCallback: function( annotationData ) {

            // create tile key: "level, xIndex, yIndex"
            var tilekey = this.createTileKey( annotationData.index ),
                i;

            console.log("After get: "+ tilekey);

            this.annotations[tilekey] = annotationData.annotations;
            this.annotationStatus[tilekey] = "loaded"; // flag as loaded

            if (this.annotations[tilekey].length > 0) {
                if (this.getCallbacks[tilekey] === undefined) {
                    console.log('ERROR: Received annotation data out of sync from server... ');
                    return;
                }
                for (i =0; i <this.getCallbacks[tilekey].length; i++ ) {
                    this.getCallbacks[tilekey][i]( this.annotations[tilekey] );
                }
            }

            delete this.getCallbacks[tilekey];
        },


        postAnnotation: function(annotation) {

            // Request the layer information
            aperture.io.rest('/annotation',
                             'POST',
                             $.proxy(this.postCallback, this),
                             {
                                 postData: {    "layer": this.layer,
                                                "annotation" : annotation
                                            },
                                 contentType: 'application/json'
                             });
        },


        postCallback: function( postResult ) {

            console.log("DEBUG: POST complete: "+ postResult );

        }


        

    });

    return AnnotationTracker;
});
