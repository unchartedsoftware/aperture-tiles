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



    var Class = require('../class'),
        AnnotationService = require('./AnnotationService'),
        ClientNodeLayer = require('../layer/view/client/ClientNodeLayer'),
        HtmlLayer = require('../layer/view/client/HtmlLayer'),
        //NUM_BINS_PER_DIM = 8,
        ANNOTATION_DETAILS_CLASS = 'annotation-details',
        ANNOTATION_DETAILS_CONTENT_CLASS = "annotation-details-content",
        ANNOTATION_DETAILS_HEAD_CLASS = "annotation-details-head",
        ANNOTATION_DETAILS_BODY_CLASS = "annotation-details-body",
        ANNOTATION_DETAILS_AGGREGATE_CLASS = "annotation-details-aggregate",
        ANNOTATION_DETAILS_LABEL_CLASS = "annotation-details-label",
        ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS = "annotation-details-close-button",
        ANNOTATION_CAROUSEL_CLASS = "annotation-carousel",
        ANNOTATION_CHEVRON_CLASS = "annotation-carousel-ui-chevron",
        ANNOTATION_CHEVRON_LEFT_CLASS = "annotation-carousel-ui-chevron-left",
        ANNOTATION_CHEVRON_RIGHT_CLASS = "annotation-carousel-ui-chevron-right",
        ANNOTATION_INDEX_CLASS = "annotation-carousel-ui-text-index",
        AnnotationLayer;



    AnnotationLayer = Class.extend({

        init: function (spec) {

            var that = this;

            this.map = spec.map;
            this.service = new AnnotationService( spec.id );
            this.groups = spec.groups;
            this.accessibility = spec.accessibility;
            this.filter = spec.filter;
            this.pendingTiles = {};
            this.tiles = [];

            // set callbacks
            this.map.on('moveend', $.proxy( this.update, this ) );
            this.map.on('zoom', function() {
                that.nodeLayer.clear();
                that.update();
            });

            this.createLayer();
            this.update();
        },


        createLayer : function() {

            var that = this,
                detailsIsOpen = false;

            function DEBUG_ANNOTATION( pos ) {
                return {
                    x: pos.x,
                    y: pos.y,
                    group: that.groups[ Math.floor( that.groups.length*Math.random() ) ],
                    range: {
                        min: 0,
                        max: that.map.getZoom()
                    },
                    level: that.map.getZoom(),
                    data: {
                        user: "DebugTweetUser"
                    }
                };
            }


            function createDetailsContent( $details, annotation ) {

                var html = "";

                // remove any previous view
                $( "."+ANNOTATION_DETAILS_CONTENT_CLASS ).remove();

                html = '<div class="'+ANNOTATION_DETAILS_CONTENT_CLASS+'">'
                     +     '<div class="'+ANNOTATION_DETAILS_HEAD_CLASS+'">'
                     +         '<div class="'+ANNOTATION_DETAILS_LABEL_CLASS+'">'+annotation.data.username+'</div>'
                     +     '</div>'
                     +     '<div class="'+ANNOTATION_DETAILS_BODY_CLASS+'">'
                     +         '<div class="'+ANNOTATION_DETAILS_LABEL_CLASS+'"> x: '+annotation.x+'</div>'
                     +         '<div class="'+ANNOTATION_DETAILS_LABEL_CLASS+'"> y: '+annotation.y+'</div>'
                     +         '<div class="'+ANNOTATION_DETAILS_LABEL_CLASS+'"> group: '+annotation.group+'</div>'
                     +         '<div class="'+ANNOTATION_DETAILS_LABEL_CLASS+'"> tweet: '+annotation.data.tweet+'</div>'
                     +    '</div>'
                     + '</div>';

                $details.append( html );
            }

            function createDetailsCarouselUI( $details, bin ) {

                var $carousel,
                    $leftChevron,
                    $rightChevron,
                    $indexText,
                    index = 0;

                function mod( m, n ) {
                    return ((m % n) + n) % n;
                }

                function indexText() {
                    return (index+1) +' of '+ bin.length;
                }

                // remove any previous details
                $( "."+ANNOTATION_CAROUSEL_CLASS ).remove();

                $details.addClass( ANNOTATION_DETAILS_AGGREGATE_CLASS );

                $leftChevron = $("<div class='"+ANNOTATION_CHEVRON_CLASS+" "+ANNOTATION_CHEVRON_LEFT_CLASS+"'></div>");
                $rightChevron = $("<div class='"+ANNOTATION_CHEVRON_CLASS+" "+ANNOTATION_CHEVRON_RIGHT_CLASS+"'></div>");

                $leftChevron.click( function() {
                    index = mod( index-1, bin.length );
                    createDetailsContent( $details, bin[index] );
                    $indexText.text( indexText() );
                });
                $rightChevron.click( function() {
                    index = mod( index+1, bin.length );
                    createDetailsContent( $details, bin[index] );
                    $indexText.text( indexText() );
                });

                $indexText = $('<div class="'+ANNOTATION_INDEX_CLASS+'">'+ indexText() +'</div>');
                $carousel = $('<div class="'+ANNOTATION_CAROUSEL_CLASS+'"></div>');

                $carousel.append( $leftChevron );
                $carousel.append( $rightChevron );
                $carousel.append( $indexText );
                $details.append( $carousel );

            }


            function destroyDetails() {

                $( "."+ANNOTATION_DETAILS_CLASS ).remove();
                detailsIsOpen = false;
            }

            function createDetailsElement( bin, pos ) {

                var $details, $closeButton;

                // remove any previous details
                $( "."+ANNOTATION_DETAILS_CLASS ).remove();
                // create details div
                $details = $('<div class="'+ANNOTATION_DETAILS_CLASS+'"></div>');
                // create close button
                $closeButton = $('<div class="'+ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS+'"></div>');
                $closeButton.click( destroyDetails );
                // create display for first annotation
                createDetailsContent( $details, bin[0], bin.length > 1 );
                // if more than one annotation, create carousel ui
                if ( bin.length > 1 ) {
                    createDetailsCarouselUI( $details, bin );
                }
                // add close button last to overlap
                $details.append( $closeButton );
                // make details draggable and resizable
                $details.draggable().resizable({
                    minHeight: 128,
                    minWidth: 257
                });
                that.map.getRootElement().append( $details );

                $details.css({
                    left: pos.x -( $details.outerWidth()/2 ),
                    top: pos.y - ( $details.outerHeight() + 26 )
                });
            }

            function createDetails( bin, pos ) {

                createDetailsElement( bin, pos );
                detailsIsOpen = true;
            }

            function createAnnotation() {

                if (detailsIsOpen) {
                    destroyDetails();
                    return;
                }

                if ( !that.accessibility.write ) {
                    return;
                }

                var pos = that.map.getCoordFromViewportPixel( event.xy.x, event.xy.y );
                that.service.writeAnnotation( DEBUG_ANNOTATION( pos ), $.proxy( that.update, that) );
            }


            this.nodeLayer = new ClientNodeLayer({
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

                    function createAggregateHtml( bin ) {

                        var html = '',
                            avgPos = { x:0, y:0 },
                            annoPos, annoMapPos,
                            offset,
                            $aggregate,
                            i;

                        html += '<div class="point-annotation-aggregate" style="position:absolute;">';

                        // for each annotation
                        for (i=0; i<bin.length; i++) {

                            annoPos = that.map.getViewportPixelFromCoord( bin[i].x, bin[i].y );
                            annoMapPos = that.map.getMapPixelFromViewportPixel( annoPos.x, annoPos.y );

                            avgPos.x += annoMapPos.x;
                            avgPos.y += annoMapPos.y;

                            offset = {
                                x : annoPos.x - tilePos.x,
                                y : annoPos.y - tilePos.y
                            };

                            html += '<div class="point-annotation point-annotation-front" style="left:'+offset.x+'px; top:'+offset.y+'px;"></div>' +
                                    '<div class="point-annotation point-annotation-back" style="left:'+offset.x+'px; top:'+offset.y+'px;"></div>';

                        }

                        html += '</div>';

                        $aggregate = $(html);

                        if (bin.length === 1 && that.accessibility.modify) {

                            $aggregate.draggable({

                                stop: function( event ) {
                                    var $element = $(this).find('.point-annotation-back'),
                                        annotation = bin[0],
                                        offset = $element.offset(),
                                        dim = $element.width()/2,
                                        pos = that.map.getCoordFromViewportPixel( offset.left+dim, offset.top+dim );

                                    annotation.x = pos.x;
                                    annotation.y = pos.y;

                                    that.service.modifyAnnotation( annotation, $.proxy( that.update, that ) );
                                }
                            });
                        }

                        avgPos.x /= bin.length;
                        avgPos.y = that.map.getMapHeight() - ( avgPos.y / bin.length );

                        $aggregate.click( function() {

                            createDetails( bin, avgPos );
                        });

                        return $aggregate;
                    }


                    // for each bin
                    for (key in data.bins) {
                        if (data.bins.hasOwnProperty(key)) {
                            $tile = $tile.add( createAggregateHtml( data.bins[key] ) );
                        }
                    }

                    return $tile;
                }

            }));

            /* debug bin visualizing
            this.nodeLayer.addLayer( new HtmlLayer({
                html : function() {

                    var data = this,
                        html = '',
                        key;

                    function createBinHtml( binkey ) {
                        var BIN_SIZE = 256/NUM_BINS_PER_DIM,
                            parsedValues = binkey.split(/[,\[\] ]/),
                            bX = parseInt(parsedValues[1], 10),
                            bY = parseInt(parsedValues[3], 10),
                            left = BIN_SIZE*bX,
                            top = BIN_SIZE*bY;
                        return '<div class="annotation-bin"' +
                               'style="position:absolute;' +
                               'z-index:-1;'+
                               'left:'+left+'px; top:'+top+'px;'+
                               'width:'+BIN_SIZE+'px; height:'+BIN_SIZE+'px;'+
                               'pointer-events:none;' +
                               'border: 1px solid #FF0000;' +
                               '-webkit-box-sizing: border-box; -moz-box-sizing: border-box; box-sizing: border-box;">'+
                               '</div>';
                    }

                    // for each bin
                    for (key in data.bins) {
                        if (data.bins.hasOwnProperty(key)) {
                            html += createBinHtml( key );
                        }
                    }
                    return html;

                }
            }));
            */

            that.map.on('click', createAnnotation );
            that.map.on('zoom', destroyDetails );
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

            if ( !this.accessibility.read ) {
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
            this.service.getAnnotations( neededTiles, $.proxy( this.getCallback, this ) );
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
        getCallback: function( data ) {

            var tilekey = this.createTileKey( data.tile ),
                currentTiles = this.tiles,
                key,
                tileArray = [];

            if ( !this.pendingTiles[tilekey] ) {
                // receiving data from old request, ignore it
                return;
            }

            // clear visual representation
            this.nodeLayer.remove( tilekey );
            // add to data cache
            currentTiles[tilekey] = this.transformTileToBins( data, tilekey );

            // convert all tiles from object to array and redraw
            for (key in currentTiles) {
                if ( currentTiles.hasOwnProperty( key )) {
                    tileArray.push( currentTiles[key] );
                }
            }

            this.redraw( tileArray );

        },


        redraw: function( data ) {
            this.nodeLayer.all( data ).redraw();
        }


     });

    return AnnotationLayer;
});
