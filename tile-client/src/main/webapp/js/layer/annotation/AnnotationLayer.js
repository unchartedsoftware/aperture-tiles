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
        AnnotationService = require('./AnnotationService'),
        AnnotationDetails = require('./AnnotationDetails'),
        HtmlNodeLayer = require('../HtmlNodeLayer'),
        HtmlLayer = require('../HtmlLayer'),
        //NUM_BINS_PER_DIM = 8,
        AnnotationLayer;



    AnnotationLayer = Layer.extend({

        init: function( spec, map ) {

            this._super( spec, map );
            this.service = new AnnotationService( spec.layer );
            this.pendingTiles = {};
            this.tiles = [];

            // set callbacks
            this.map.on('moveend', $.proxy( this.update, this ) );

            this.createLayer();
            this.update();
        },


        setOpacity: function( opacity ) {
            this.nodeLayer.getRootElement().css( 'opacity', opacity );
        },


        setVisibility: function( visible ) {
            var visibility = visible ? 'visible' : 'hidden';
            this.nodeLayer.getRootElement().css( 'visibility', visibility );
        },


        getAnnotationHtml : function ( bin, tilePosition ) {

            var ANNOTATION_POINT_CLASS = "point-annotation",
                ANNOTATION_AGGREGATE_POINT_CLASS = "point-annotation-aggregate",
                ANNOTATION_POINT_FILL_CLASS = "point-annotation-front",
                ANNOTATION_POINT_BORDER_CLASS = "point-annotation-back",
                CLICKED_ANNOTATION_CLASS = "clicked-annotation",
                CLICKED_AGGREGATE_CLASS = "clicked-aggregate",
                html = '',
                pos,
                offset,
                $annotation,
                i;

            // aggregation div
            html += '<div class="'+ANNOTATION_AGGREGATE_POINT_CLASS+'">';

            // for each annotation in the bin
            for (i=0; i<bin.length; i++) {

                // get annotations position in viewport space
                pos = this.map.getViewportPixelFromCoord( bin[i].x, bin[i].y );
                // get relative position from tile top left
                offset = {
                    x : pos.x - tilePosition.x,
                    y : pos.y - tilePosition.y
                };

                html += '<div class="'+ANNOTATION_POINT_CLASS+' '+ANNOTATION_POINT_FILL_CLASS+'" style="left:'+offset.x+'px; top:'+offset.y+'px;"></div>' +
                        '<div class="'+ANNOTATION_POINT_CLASS+' '+ANNOTATION_POINT_BORDER_CLASS+'" style="left:'+offset.x+'px; top:'+offset.y+'px;"></div>';
            }

            html += '</div>';

            // create element
            $annotation = $( html );
            // attach click event handler
            $annotation.click( function() {
                // on click highlight annotation
                $( '.'+ANNOTATION_AGGREGATE_POINT_CLASS ).removeClass( CLICKED_AGGREGATE_CLASS );
                $( '.'+ANNOTATION_POINT_FILL_CLASS ).removeClass( CLICKED_ANNOTATION_CLASS );
                $annotation.addClass( CLICKED_AGGREGATE_CLASS );
                $annotation.find( '.'+ANNOTATION_POINT_FILL_CLASS ).addClass( CLICKED_ANNOTATION_CLASS );
            });

            return $annotation;
        },


        getDetailsHeaderHtml : function( annotation ) {

            return $('<div class="twitter-details-header">'+annotation.data.user+'</div>');
        },


        getDetailsBodyHtml : function( annotation ) {

            var captureSettings = {
                    format : 'PNG',
                    captureWidth : 384,
                    cache : false
                },
                $temporaryContent = $('<div class="twitter-details-loader">'
                                     +    '<div class="twitter-loader-gif"></div>'
                                     +'</div>'),
                captureURL = aperture.capture.inline( 'https://mobile.twitter.com/justinbieber',
                                                      captureSettings,
                                                      null ),
                $img = $('<img class="twitter-capture" src="'+captureURL+'">');

            $img.load( function() {

                var $content = $('<div class="twitter-details-img" style="opacity:0"></div>').append( $img );

                $content.mouseenter( function() {

                    var $externalLink = $('<div class="twitter-external-link"></div>');
                    $externalLink.click( function() {
                        window.open('https://www.twitter.com/justinbieber');
                    });
                    $content.append( $externalLink );

                });

                $content.mouseleave( function() {
                    var $externalLink = $('.twitter-external-link');
                    $externalLink.remove();
                });

                // swap content
                $temporaryContent.replaceWith( $content );
                // fade content in
                $content.animate({opacity:1});
            });

            return $temporaryContent;
        },


        createLayer : function() {

            var that = this,
                detailsIsOpen = false;

            function createAnnotation() {

                var pos,
                    tilekey;

                function DEBUG_ANNOTATION( pos ) {
                    return {
                        x: pos.x,
                        y: pos.y,
                        group: that.layerSpec.groups[ Math.floor( that.layerSpec.groups.length*Math.random() ) ],
                        range: {
                            min: 0,
                            max: that.map.getZoom()
                        },
                        level: that.map.getZoom(),
                        data: {
                            user: "justinbieber"
                        }
                    };
                }

                if ( detailsIsOpen ) {
                    AnnotationDetails.destroyDetails();
                    detailsIsOpen = false;
                    return;
                }

                if ( !that.layerSpec.accessibility.write ) {
                    return;
                }

                // get position and tilekey for annotation
                pos = that.map.getCoordFromViewportPixel( event.xy.x, event.xy.y );
                tilekey = that.map.getTileKeyFromViewportPixel( event.xy.x, event.xy.y );
                // write annotation
                that.service.writeAnnotation( DEBUG_ANNOTATION( pos ), that.updateCallback(tilekey) );
            }


            this.nodeLayer = new HtmlNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey',
                propagate: false
            });


            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var data = this,
                        $tile = $(''),
                        key,
                        tilePos;

                    // get tile position
                    tilePos = that.map.getViewportPixelFromCoord( data.longitude, data.latitude );

                    function createAnnotationBin( bin ) {

                        var ANNOTATION_BIN_CLASS = "annotation-bin",
                            $bin = $('<div class="'+ANNOTATION_BIN_CLASS+'"></div>'),
                            $annotation = that.getAnnotationHtml( bin, tilePos );

                        // add details click event
                        $annotation.click( function( event ) {

                            var $details = AnnotationDetails.createDetails( bin, $bin,
                                                                            $.proxy( that.getDetailsHeaderHtml, that ),
                                                                            $.proxy( that.getDetailsBodyHtml, that ) ),
                                offset = $bin.offset(),
                                relX = event.pageX - offset.left,
                                relY = event.pageY - offset.top;

                            $details.css({
                                left: relX -( $details.outerWidth()/2 ),
                                top: relY - ( $details.outerHeight() + 26 )
                            });

                            detailsIsOpen = true;
                        });

                        // add drag modify event
                        if ( bin.length === 1 && that.layerSpec.accessibility.modify ) {

                            $bin.draggable({

                                stop: function( event ) {

                                    var annotation = bin[0],
                                        offset = that.map.getElement().offset(),
                                        pos = that.map.getCoordFromViewportPixel( event.clientX - offset.left,
                                                                                  event.clientY - offset.top );

                                    annotation.x = pos.x;
                                    annotation.y = pos.y;

                                    that.service.modifyAnnotation( annotation, function() {
                                        // TODO: request old and new tile locations in case of failure
                                        return true;
                                    });

                                    // prevent click from firing
                                    $( event.toElement ).one('click', function(e) {
                                        e.stopImmediatePropagation();
                                    });
                                }
                            });
                        }

                        $bin.append( $annotation );
                        return $bin;
                    }

                    // for each bin
                    for (key in data.bins) {
                        if (data.bins.hasOwnProperty(key)) {
                            $tile = $tile.add( createAnnotationBin( data.bins[key] ) );
                        }
                    }

                    return $tile;
                }

            }));

            that.map.on('click', function() {
                $('.point-annotation-aggregate').removeClass('clicked-aggregate');
                $('.point-annotation-front').removeClass('clicked-annotation');
                createAnnotation();
            });
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

            if ( !this.layerSpec.accessibility.read ) {
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

            // Request needed tiles from dataService
            this.service.getAnnotations( neededTiles, this.getCallback() );
        },


        updateCallback : function( tilekey ) {

            var that = this;

            return function( data ) {
                // set as pending
                that.pendingTiles[tilekey] = true;
                // set force update flag to ensure this tile overrides any other pending requests
                that.service.getAnnotations( [tilekey], that.getCallback( true ) );
            };
        },

        transformTileToBins: function (tileData, tilekey) {

            var tileRect = this.map.getPyramid().getTileBounds( tileData.tile );

            return {
                bins : tileData.annotations,
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

                // clear visual representation
                that.nodeLayer.remove( tilekey );
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


        redraw: function( data ) {
            this.nodeLayer.all( data ).redraw();
        }


     });

    return AnnotationLayer;
});
