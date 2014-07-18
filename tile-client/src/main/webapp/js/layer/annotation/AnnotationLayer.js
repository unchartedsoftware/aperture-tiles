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


define(function (require) {
    "use strict";



    var Layer = require('../Layer'),
        Util = require('../../util/Util'),
        AnnotationService = require('./AnnotationService'),
        DETAILS_VERTICAL_OFFSET = 26,
        AnnotationLayer;



    AnnotationLayer = Layer.extend({

        init: function( spec, renderer, details, map ) {

            this._super( spec, map );
            this.renderer = renderer;
            this.details = details;
            this.pendingTiles = {};
            this.tiles = [];
            this.map.on('moveend', $.proxy( this.update, this ) );
        },


        setOpacity: function( opacity ) {

            this.renderer.setOpacity( opacity );
        },


        setVisibility: function( visible ) {

            this.renderer.setVisibility( visible );
        },


        createDetails: function( clickState ) {

            var $details = this.details.createDisplayDetails( clickState.bin, clickState.$bin );
            // position details over click
            $details.css({
                left: clickState.position.x - ( $details.outerWidth() / 2 ),
                top: clickState.position.y - ( $details.outerHeight() + DETAILS_VERTICAL_OFFSET )
            });
        },


        destroyDetails: function( clickState ) {

            this.details.destroyDetails();
        },


        createAnnotation: function( position ) {

            var that = this,
                coord,
                tilekey;

            // temp for debug writing
            function DEBUG_ANNOTATION( coord ) {
                return {
                    x: coord.x,
                    y: coord.y,
                    group: that.layerSpec.groups[ Math.floor( that.layerSpec.groups.length*Math.random() ) ],
                    range: {
                        min: 0,
                        max: that.map.getZoom()
                    },
                    level: that.map.getZoom(),
                    data: {
                        user: "debug"
                    }
                };
            }

            if ( !this.layerSpec.accessibility.write ) {
                return;
            }

            // get position and tilekey for annotation
            coord = this.map.getCoordFromViewportPixel( position.x, position.y );
            tilekey = this.map.getTileKeyFromViewportPixel( position.x, position.y );
            // write annotation
            AnnotationService.writeAnnotation( this.layerInfo,
                                               DEBUG_ANNOTATION( coord ),
                                               this.updateCallback(tilekey) );
        },


        modifyAnnotation: function( annotation ) {

            AnnotationService.modifyAnnotation( this.layerInfo, annotation, function() {
                // TODO: request old and new tile locations in case of failure
                return true;
            });

        },


        removeAnnotation: function( annotation ) {
            var  pixel = this.map.getViewportPixelFromCoord( annotation.x, annotation.y ),
                 tilekey = this.map.getTileKeyFromViewportPixel( pixel.x, pixel.y );
            AnnotationService.removeAnnotation( this.layerInfo, annotation.certificate, this.updateCallback(tilekey) );
        },


        createTileKey : function ( tile ) {
            return tile.level + "," + tile.xIndex + "," + tile.yIndex;
        },


        update: function() {

            var visibleTiles = this.map.getTilesInView(),  // determine all tiles in view
                currentTiles = this.tiles,
                pendingTiles = this.pendingTiles,
                neededTiles = [],
                defunctTiles = {},
                i, tile, tilekey;

            if ( !this.layerInfo || !this.layerSpec.accessibility.read ) {
                return;
            }

            // track the tiles we have
            for ( tilekey in currentTiles ) {
                if ( currentTiles.hasOwnProperty(tilekey) ) {
                    defunctTiles[ tilekey ] = true;
                }
            }
            // and the tiles we are waiting on
            for ( tilekey in pendingTiles ) {
                if ( pendingTiles.hasOwnProperty(tilekey) ) {
                    defunctTiles[ tilekey ] = true;
                }
            }

            // Go through, seeing what we need.
            for (i=0; i<visibleTiles.length; ++i) {
                tile = visibleTiles[i];
                tilekey = this.createTileKey(tile);

                if ( defunctTiles[tilekey] ) {

                    // Already have the data, remove from defunct list
                    delete defunctTiles[tilekey];

                } else {

                    // we do not have it, and we are not waiting on it, flag it for retrieval
                    pendingTiles[tilekey] = true;
                    neededTiles.push(tilekey);
                }

            }

            // Remove all old defunct tiles references
            for (tilekey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tilekey)) {
                    // remove from memory and pending list
                    delete currentTiles[tilekey];
                    delete pendingTiles[tilekey];
                }
            }

            // Request needed tiles from service
            AnnotationService.getAnnotations( this.layerInfo, neededTiles, this.getCallback() );
        },


        updateCallback : function( tilekey ) {

            var that = this;

            return function( data ) {

                if ( !that.layerInfo ) {
                    return;
                }
                // set as pending
                that.pendingTiles[tilekey] = true;
                // set force update flag to ensure this tile overrides any other pending requests
                AnnotationService.getAnnotations( this.layerInfo, [tilekey], that.getCallback( true ) );
            };
        },


        transformTileToBins: function (tileData, tilekey) {

            var tileRect = this.map.getPyramid().getTileBounds( tileData.tile );

            return {
                bins : tileData.annotations,
                uuid : Util.generateUuid(),
                tilekey : tilekey,
                longitude: tileRect.minX,
                latitude: tileRect.maxY
            };
        },


        /**
         * @param annotationData annotation data received from server of the form:
         *  {
         *      tile: {
         *                  level:
         *                  xIndex:
         *                  yIndex:
         *             }
         *      annotations: {
         *                  <binkey>: [ <annotation>, <annotation>, ... ]
         *                  <binkey>: [ <annotation>, <annotation>, ... ]
         *            }
         *  }
         */

        getCallback: function( forceUpdate ) {

            var that = this;

            return function( data ) {

                var tilekey = that.createTileKey( data.tile ),
                    currentTiles = that.tiles,
                    key,
                    tileArray = [];

                if ( !that.pendingTiles[tilekey] && !forceUpdate ) {
                    // receiving data from old request, ignore it
                    return;
                }

                // add to data cache
                currentTiles[tilekey] = that.transformTileToBins( data, tilekey );

                // convert all tiles from object to array and redraw
                for (key in currentTiles) {
                    if ( currentTiles.hasOwnProperty( key )) {
                        tileArray.push( currentTiles[key] );
                    }
                }

                that.redraw( tileArray );
            };

        },


        configure: function( callback ) {

            var that = this;

            AnnotationService.configureLayer( this.layerSpec, function( layerInfo, statusInfo ) {
                if (statusInfo.success) {
                    if ( that.layerInfo ) {
                        // if a previous configuration exists, release it
                        AnnotationService.unconfigureLayer( that.layerInfo, function() {
                            return true;
                        });
                    }
                    // set layer info
                    that.layerInfo = layerInfo;
                }

                callback( layerInfo, statusInfo );
            });
        },


        redraw: function( data ) {
            this.renderer.redraw( data );
        }


     });

    return AnnotationLayer;
});
