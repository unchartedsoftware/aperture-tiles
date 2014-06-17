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

 /*global OpenLayers */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        AnnotationService = require('./AnnotationService'),
        ClientNodeLayer = require('../layer/view/client/ClientNodeLayer'),
        HtmlLayer = require('../layer/view/client/HtmlLayer'),
        NUM_BINS_PER_DIM = 8,
        ANNOTATION_DETAILS_CLASS = 'annotation-details',
        ANNOTATION_DETAILS_CONTENT_CLASS = "annotation-details-content",
        ANNOTATION_DETAILS_HEAD_CLASS = "annotation-details-head",
        ANNOTATION_DETAILS_BODY_CLASS = "annotation-details-body",
        ANNOTATION_DETAILS_LABEL_CLASS = "annotation-details-label",
        ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS = "annotation-details-close-button",
        ANNOTATION_CAROUSEL_CLASS = "annotation-carousel",
        ANNOTATION_CHEVRON_CLASS = "carousel-ui-chevron",
        ANNOTATION_CHEVRON_LEFT_CLASS = "carousel-ui-chevron-left",
        ANNOTATION_CHEVRON_RIGHT_CLASS = "carousel-ui-chevron-right",
        ANNOTATION_INDEX_CLASS = "carousel-ui-text-index",
        AnnotationLayer;



    AnnotationLayer = Class.extend({

        init: function (spec) {

            var that = this;

            this.map = spec.map;
            this.service = new AnnotationService( spec.id );
            this.groups = spec.groups;
            this.accessibility = spec.accessibility;
            this.filter = spec.filter;
            this.pendingTileRequests = {};
            this.tiles = [];

            // set callbacks
            this.map.on('moveend', $.proxy( this.updateTiles, this ) );
            this.map.on('zoom', function() {
                that.nodeLayer.clear();
                that.updateTiles();
            });

            this.createLayer();
            this.updateTiles();
        },


        writeCallback: function( data, statusInfo ) {

            if ( statusInfo.success ) {
                console.log("WRITE SUCCESS");
            }
            // writes can only fail if server is dead or UUID collision
            this.updateTiles();

        },


        modifyCallback: function( data, statusInfo ) {

            if ( statusInfo.success ) {
                console.log("MODIFY SUCCESS");
            }
            this.updateTiles();
        },


        removeCallback: function( data, statusInfo ) {

            if ( statusInfo.success ) {
                console.log("REMOVE SUCCESS");
            }
            this.updateTiles();
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
                        username: "DebugTweetUser",
                        tweet: "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed nec purus in ante pretium blandit. Aliquam erat volutpat. Nulla libero lectus."
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
                     +         '<div class="'+ANNOTATION_DETAILS_LABEL_CLASS+'"> x: '+annotation.x+', y: '+annotation.y+'</div>'
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

                $details.addClass('annotation-details-aggregate');

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
                $details = $('<div class="'+ANNOTATION_DETAILS_CLASS+'" style="left:'+pos.x+'px; top:'+ pos.y +'px;"></div>');
                // create close button
                $closeButton = $('<div class="'+ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS+'"></div>');
                $closeButton.click( destroyDetails );
                // create display for first annotation
                createDetailsContent( $details, bin[0] );
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
                that.service.writeAnnotation( DEBUG_ANNOTATION( pos ), $.proxy( that.writeCallback, that) );
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

                        if (bin.length === 1 && that.accessibility.draggable) {

                            $aggregate.draggable({

                                stop: function( event ) {
                                    var $element = $(this).find('.point-annotation-back'),
                                        offset = $element.offset(),
                                        dim = $element.width()/2,
                                        pos = that.map.getCoordFromViewportPixel( offset.left+dim, offset.top+dim ),
                                        newAnno = JSON.parse( JSON.stringify( bin[0] ) );

                                    newAnno.x = pos.x;
                                    newAnno.y = pos.y;

                                    that.service.modifyAnnotation( bin[0], newAnno, $.proxy( that.modifyCallback, that ) );
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

            that.map.on('click', createAnnotation );

        },


        updateTiles: function() {

            var visibleTiles = this.map.getTilesInView(),  // determine all tiles in view
                activeTiles = [],
                defunctTiles = {},
                key,
                i, tile, tilekey;

            if ( !this.accessibility.read ) {
                return;
            }

            function createTileKey ( tile ) {
                return tile.level + "," + tile.xIndex + "," + tile.yIndex;
            }

            // keep track of current tiles to ensure we know
            // which ones no longer exist
            for (key in this.tiles) {
                if ( this.tiles.hasOwnProperty(key)) {
                    defunctTiles[key] = true;
                }
            }

            this.pendingTileRequests = {};

            // Go through, seeing what we need.
            for (i=0; i<visibleTiles.length; ++i) {
                tile = visibleTiles[i];
                tilekey = createTileKey(tile);

                if ( defunctTiles[tilekey] ) {
                    // Already have the data, remove from defunct list
                    delete defunctTiles[tilekey];
                }
                // And mark tile it as meaningful
                this.pendingTileRequests[tilekey] = true;
                activeTiles.push(tilekey);
            }

            // Remove all old defunct tiles references
            for (tilekey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tilekey)) {
                    delete this.tiles[tilekey];
                }
            }

            // Request needed tiles from dataService
            this.service.getAnnotations( activeTiles, $.proxy( this.getCallback, this ) );
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

            function createTileKey( tile ) {
                return tile.level + "," + tile.xIndex + "," + tile.yIndex;
            }

            var tilekey = createTileKey( data.tile ),
                key,
                tileArray = [];

            if ( !this.pendingTileRequests[tilekey] ) {
                // receiving data from old request, ignore it
                return;
            }

            // clear data and visual representation
            this.nodeLayer.remove( tilekey );
            this.tiles[tilekey] = this.transformTileToBins( data, tilekey );

            // convert all tiles from object to array and redraw
            for (key in this.tiles) {
                if ( this.tiles.hasOwnProperty( key )) {
                    tileArray.push( this.tiles[key] );
                }
            }

            console.log("read anno " + tileArray.length);

            this.redraw( tileArray );
        },


        redraw: function( data ) {
            this.nodeLayer.all( data ).redraw();
        }


     });

    return AnnotationLayer;
});
