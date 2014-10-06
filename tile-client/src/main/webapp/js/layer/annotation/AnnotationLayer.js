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
        PubSub = require('../../util/PubSub'),
        Util = require('../../util/Util'),
        AnnotationService = require('./AnnotationService'),
        DETAILS_VERTICAL_OFFSET = 26,
        addDataToMap,
        removeDataFromMap,
        AnnotationLayer;


    /**
     * Adds a given data entry to the data map
     */
    addDataToMap = function( dataMap, entry ) {

        var key = entry.id,
            annotations = entry.annotations,
            dataEntry;

        // store key in data map, and update uuid for redraw
        dataMap[key] = dataMap[key] || {
            id: key,
            annotations: []
        };
        dataEntry = dataMap[key];
        // update uuid whenever data changes to flag a redraw
        dataEntry.uuid = Util.generateUuid();
        // add annotations
        dataEntry.annotations = dataEntry.annotations.concat( annotations );
    };


    /**
     * Removes all tile data entries from the data map
     */
    removeDataFromMap = function( dataMap, tile ) {

        var i, j, index, id, entry,
            annotations, annotation,
            dataEntry;

        for (i=0; i<tile.length; i++) {

            entry = tile[i];
            id = entry.id;
            annotations = entry.annotations;
            dataEntry = dataMap[id];

            for (j=0; j<annotations.length; j++) {

                annotation = annotations[j];

                // remove annotation from data entry
                index = dataEntry.annotations.indexOf( annotation );
                dataEntry.annotations.splice( index, 1 );
                // if no more annotations in entry, remove entry
                if (dataEntry.annotations.length === 0) {
                    delete dataMap[id];
                }
            }
        }
    };


    AnnotationLayer = Layer.extend({

        init: function( spec, renderer, details, map ) {

            // set reasonable defaults
            spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
            spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;

            var that = this;

            this._super( spec, map );
            this.renderer = renderer;
            this.details = details;

            // tiles that have been requested from server, removing a tile from
            // this list will result in the client ignoring it upon receiving it
            this.pendingTiles = {};

            // tile map, stores tile data under each tilekey. Each tile entry is
            // an array organized such that a single entry is a single node in the
            // renderer's node layer
            this.tiles = {};

            // if idKey is specified, group annotations by its value, else group
            // annotations by bin
            this.idKey = spec.idKey || null;

            // if an idKey is specified, data will most likely span across tiles
            // and as such this attribute will hold the organized data
            this.dataMap = {};

            this.map.on('moveend', $.proxy( this.update, this ) );

            // clear click state if map is clicked
            this.map.on( 'click', function() {
                if ( that.isClicked() ) {
                    // un-select
                     that.setClick( null );
                } else {
                    // add new annotation
                    that.createAnnotation( event.xy );
                }
            });

            this.setZIndex( spec.zIndex );
            this.setOpacity( spec.opacity );
            this.setVisibility( spec.enabled );
        },


        setClick: function( value ) {
            this.click = value;
            if ( value !== null ) {
                this.createDetails( value );
            } else {
                this.destroyDetails();
            }
            PubSub.publish( this.getChannel(), { field: 'click', value: value });
        },


        getClick: function() {
            return this.click;
        },


        isClicked: function() {
            return this.click !== null && this.click !== undefined;
        },


        setOpacity: function( opacity ) {
            this.opacity = opacity;
            this.renderer.setOpacity( opacity );
            PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
        },


        getOpacity: function() {
            return this.opacity;
        },


        setVisibility: function( visible ) {
            this.visibility = visible;
            this.renderer.setVisibility( visible );
            PubSub.publish( this.getChannel(), { field: 'enabled', value: visible });
        },


        getVisibility: function() {
            return this.visibility;
        },


        setZIndex: function( zIndex ) {
            this.zIndex = zIndex;
            this.renderer.setZIndex( zIndex );
            PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
        },


        getZIndex: function() {
            return this.zIndex;
        },


        createDetails: function( clickState ) {

            var $details = this.details.createDisplayDetails( clickState.annotations, this.map.getRootElement() ); //clickState.$annotations );
            // position details over click
            $details.css({
                left: clickState.position.x, // - ( $details.outerWidth() / 2 ),
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

                    if ( this.idKey && !pendingTiles[tilekey] ) {
                        // if idKey is used, and the tile isn't just pending, remove entries from data map
                        removeDataFromMap( this.dataMap, currentTiles[tilekey] );
                    }

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

            function organizeDataByKey( data, currentTile, currentData ) {

                // organize all data by key for this tile
                var bins = data.annotations,
                    bin, binkey,
                    key, annotation,
                    tileEntry, i,
                    tileMap = {};

                // assemble by data by key
                for ( binkey in bins ) {
                    if ( bins.hasOwnProperty( binkey )) {

                        bin = bins[binkey];

                        for (i=0; i<bin.length; i++) {

                            annotation = bin[i];
                            // get key value
                            key = annotation.data[that.idKey];
                            // create entry under key value in tile
                            tileMap[key] = tileMap[key] || {
                                id: key,
                                annotations: []
                            };
                            tileMap[key].annotations.push( annotation );
                        }
                    }
                }

                // convert map into an array of the keyed values
                // and store in tile
                for (key in tileMap) {
                    if ( tileMap.hasOwnProperty( key )) {
                        // add entry to tile array
                        tileEntry = tileMap[key];
                        currentTile.push( tileEntry );

                        addDataToMap( currentData, tileEntry );
                    }
                }
                return currentData;
            }

            function organizeDataByBin( data, currentTile, currentTiles ) {

                var bins = data.annotations,
                    binkey;

                // assemble annotation data by bin
                for ( binkey in bins ) {
                    if ( bins.hasOwnProperty( binkey )) {

                        currentTile.push({
                            uuid: Util.generateUuid(),
                            annotations: bins[binkey]
                        });

                    }
                }
                return currentTiles;
            }

            return function( data ) {

                var tilekey = that.createTileKey( data.tile ),
                    pendingTiles = that.pendingTiles,
                    currentTiles = that.tiles,
                    currentData = that.dataMap,
                    key, dataSource,
                    dataArray = [];

                if ( !that.pendingTiles[tilekey] && !forceUpdate ) {
                    // receiving data from old request, ignore it
                    return;
                }

                // remove from pending list
                delete pendingTiles[tilekey];

                // add to data cache
                currentTiles[tilekey] = [];

                if ( that.idKey ) {
                    // organize data by key
                    dataSource = organizeDataByKey( data, currentTiles[tilekey], currentData );
                } else {
                    // organize data by bin
                    dataSource = organizeDataByBin( data, currentTiles[tilekey], currentTiles );
                }

                // assemble array of data and redraw
                for (key in dataSource) {
                    if ( dataSource.hasOwnProperty( key )) {
                        dataArray = dataArray.concat( dataSource[key] );
                    }
                }

                that.redraw( dataArray );
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
                    // pass parent layer (this) along with meta data to the renderer / details
                    that.renderer.parent = that;
                    that.renderer.meta = layerInfo.meta;
                    that.details.parent = that;
                    that.details.meta = layerInfo.meta;
                    // subscribe renderer to pubsub AFTER it has its parent reference
                    that.renderer.subscribeRenderer();
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
