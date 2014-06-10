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
                    group: "Urgent",
                    range: {
                        min: 0,
                        max: that.map.getZoom()
                    },
                    level: that.map.getZoom(),
                    data: {
                        title: "example annotation title " + Math.random(),
                       comment: "example annotation id " + Math.random()
                    }
                };
            }

            function createDetails( pos ) {

                var html = '<div class="annotation-details" style="left:'+pos.x+'px; top:'+ (that.map.getMapHeight()-pos.y) +'px;"></div>',
                    $details = $(html);
                $(".annotation-details").remove(); // remove any previous details
                that.map.getRootElement().append( $details );
                $details.draggable().resizable();
                detailsIsOpen = true;
            }

            function destroyDetails() {

                $(".annotation-details").remove();
                detailsIsOpen = false;
            }

            function createAnnotation() {

                if (detailsIsOpen) {
                    destroyDetails();
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

                        if (bin.length === 1) {

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
                        avgPos.y /= bin.length;


                        $aggregate.click( function() {
                            createDetails( avgPos );
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


            if ( !that.accessibility.write ) {
                that.map.on('click', createAnnotation );
            }
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

            if ( this.pendingTileRequests[tilekey] === undefined ) {
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

            this.redraw( tileArray );
        },


        redraw: function( data ) {
            this.nodeLayer.all( data ).redraw();
        }




        /*,


        removeAndDestroyFeature: function( tilekey, binkey ) {

            var olFeature = this.features[ tilekey ][ binkey ].olFeature_;

            this.highlightControl.unselect( olFeature );
            this.selectControl.unselect( olFeature );

            // completely remove old feature
            this.features[ tilekey ][ binkey ].removeFromLayerAndDestroy();
            delete this.features[ tilekey ][ binkey ];
        },


        popupCreationClose : function( feature ) {
            // destroy popup
            feature.removeAndDestroyPopup();
            // destroy feature
            feature.removeFromLayerAndDestroy();
            this.pendingFeature = null;
        },


        popupCreationSave : function( feature ) {

            var annotation = feature.getDataArray()[0],
                tilekey = feature.olFeature_.attributes.tilekey,
                binkey = feature.olFeature_.attributes.binkey;

            // make sure input is valid, if so, put it in annotation
            if ( feature.checkAndSetData() ) {

                // null out pending feature as it is no longer pending
                this.pendingFeature = null;

                if ( this.features[ tilekey ][ binkey ] !== undefined ) {

                    // already exists, aggregate
                    this.features[ tilekey ][ binkey ].addAnnotation( annotation );
                    // destroy feature
                    feature.removeFromLayerAndDestroy();
                    // set feature to correct feature
                    feature = this.features[ tilekey ][ binkey ];

                } else {

                    // does not exist, add to correct feature bin
                    this.features[ tilekey ][ binkey ] = feature;
                }

                // send POST request to write annotation to server
                this.service.writeAnnotation( annotation, $.proxy( this.writeCallback, this) );

                // get rid of popup
                feature.removeAndDestroyPopup();

                // select newly added annotation up. This will open the display popup
                this.selectControl.select( feature.olFeature_ );
            }
        },


        popupEditRemove : function( feature ) {

            var tilekey = feature.olFeature_.attributes.tilekey,
                binkey = feature.olFeature_.attributes.binkey;

            // send POST request to remove annotation from server
            this.service.removeAnnotation( feature.getDataArray()[0], $.proxy( this.removeCallback, this) );

            // remove and destroy feature from layer
            this.removeAndDestroyFeature( tilekey, binkey );
        },


        popupEditClose : function( feature ) {

            var editFunc = (this.accessibility.modify) ? $.proxy( this.popupDisplayEdit, this ) : null;
            // destroy popup
            feature.removeAndDestroyPopup();
            // go back to display
            feature.createDisplayPopup( $.proxy( this.popupDisplayClose, this ),
                                        editFunc );
        },


        popupEditSave : function( feature ) {

            // get a deep copy of the old annotation data
            var oldAnno,
                newAnno;

            // only check if input is correct
            if ( feature.checkData() ) {

                // get old data
                oldAnno = feature.getDataArray()[0];
                // deep copy annotation data
                newAnno = JSON.parse( JSON.stringify( oldAnno ) );
                // set new annotation data from popup input values
                feature.setDataFromPopup( newAnno );
                // send POST request to modify annotation on server
                this.service.modifyAnnotation( oldAnno, newAnno, $.proxy( this.modifyCallback, this) );
                // swap old data for new
                feature.setAnnotation( newAnno );
                // then remove edit popup, and add display popup
                feature.removeAndDestroyPopup();
                feature.createDisplayPopup( $.proxy( this.popupDisplayClose, this ),
                                            $.proxy( this.popupDisplayEdit, this ) );
            }
        },


        popupDisplayClose : function( feature ) {
            // destroy popup
            feature.removeAndDestroyPopup();
            this.selectControl.clickoutFeature( feature.olFeature_ );
        },


        popupDisplayEdit : function( feature ) {
            // destroy popup
            feature.removeAndDestroyPopup();
            feature.createEditablePopup( this.groups,
                                         $.proxy( this.popupEditClose, this ),
                                         $.proxy( this.popupEditSave, this ),
                                         $.proxy( this.popupEditRemove, this ) );
        },


        getFeatureInfo: function( feature ) {

            var latLon = OpenLayers.LonLat.fromString( feature.geometry.toShortString() ),
                viewportPixel = this.map.getOLMap().getViewPortPxFromLonLat( latLon ),
                xy = this.map.getCoordFromViewportPixel( viewportPixel.x, viewportPixel.y ),
                index = this.indexer.getIndex( xy, this.map.getZoom() );

            return {
                    tilekey: index.tilekey,
                    binkey: index.binkey,
                    lat: latLon.lat,
                    lon: latLon.lon,
                    x: xy.x,
                    y: xy.y
                };
        },


        cancelPendingFeature: function() {

            if ( this.pendingFeature !== null ) {
                this.pendingFeature.removeFromLayerAndDestroy();
                this.pendingFeature = null;
            }
        },


        createLayer: function() {

            var that = this;

            this.olLayer_ = new OpenLayers.Layer.Vector( "annotation-layer-"+this.id, { 
                ratio: 2,
                styleMap: new OpenLayers.StyleMap({ 
                    "default" : defaultStyle,
                    "highlight": highlightStyle,
                    "select": selectStyle,
                    "temporary": temporaryStyle
                }),
                eventListeners: {

                    "featureselected": function(e) {

                        var editFunc = (that.accessibility.modify) ? $.proxy( that.popupDisplayEdit, that ) : null;
                        // cancel any pending feature creation
                        that.cancelPendingFeature();
                        // if anything is selected, prevent click-out from adding a new point
                        that.addPointControl.deactivate();
                        // show display popup
                        e.feature.attributes.feature.createDisplayPopup( $.proxy( that.popupDisplayClose, that ),
                                                                         editFunc );
                    },

                    "featureunselected": function(e) {

                        // remove and destroy popup
                        e.feature.attributes.feature.removeAndDestroyPopup();
                        // if nothing is selected, re-enable adding points
                        if ( that.accessibility.write ) {
                            that.addPointControl.activate();
                        }
                    },

                    "featureadded": function(e) {

                        // cancel any previous pending feature creation
                        that.cancelPendingFeature();

                        var info = that.getFeatureInfo( e.feature ),
                            annotation;

                        // create base annotation
                        annotation = {
                            x: info.x,
                            y: info.y,
                            group: "_pending",   // mark as pending
                            range: {
                                min: 0,
                                max: that.map.getZoom()
                            },
                            level: that.map.getZoom(),
                            data: {}
                        };

                        // create temporary feature
                        that.pendingFeature = new AnnotationFeature({
                            map: that.map,
                            feature : e.feature,
                            annotations: [ annotation ],
                            tilekey : info.tilekey,
                            binkey: info.binkey
                        });

                        // create edit popup to enter annotation info
                        that.pendingFeature.createEditablePopup( that.groups,
                                                                 $.proxy( that.popupCreationClose, that ),
                                                                 $.proxy( that.popupCreationSave, that ));

                    }

                }
            });

            this.addPointControl = new OpenLayers.Control.DrawFeature( this.olLayer_, OpenLayers.Handler.Point );

            this.selectControl = new OpenLayers.Control.SelectFeature( this.olLayer_ );

            this.highlightControl = new OpenLayers.Control.SelectFeature( this.olLayer_, {
                hover: true,                // mouse-over == select and mouse-off == un-select
                highlightOnly: true,        // instead of selecting, only highlight
                renderIntent: "highlight"   // when highlighted, use this style
            });

            this.dragControl = new OpenLayers.Control.DragFeature( this.olLayer_, {

                onStart: function(feature, pixel) {

                    // get feature key
                    var tilekey = feature.attributes.tilekey,
                        binkey = feature.attributes.binkey;

                    if ( that.pendingFeature !== null &&
                         that.pendingFeature.olFeature_ === feature ) {
                        // cannot select pending feature
                        return;
                    }

                    if ( that.features[ tilekey ][ binkey ].isAggregated() || !that.accessibility.modify ) {
                        // disable dragging of aggregated points!
                        that.dragControl.handlers.drag.deactivate();
                    }

                    // dragging has prescedence over selection, so manually select the feature
                    that.selectControl.clickFeature( feature );
                },

                onComplete: function(feature, pixel) {

                    var info = that.getFeatureInfo( feature ),
                        oldTilekey = feature.attributes.tilekey,
                        oldBinkey =  feature.attributes.binkey,
                        newTilekey = info.tilekey,
                        newBinkey = info.binkey,
                        oldAnno,
                        newAnno;

                    // set key attribute to new key
                    feature.attributes.tilekey = newTilekey;
                    feature.attributes.binkey = newBinkey;

                    // if dragging pending feature, update location and return
                    if (that.pendingFeature !== null &&
                        that.pendingFeature.olFeature_ === feature ) {

                        oldAnno = that.pendingFeature.getDataArray()[0];
                        oldAnno.x = info.x;
                        oldAnno.y = info.y;
                        return;
                    }

                    // this feature only contains 1 annotation as you cannot drag aggregates
                    oldAnno = that.features[ oldTilekey ][ oldBinkey ].getDataArray()[0];

                    // deep copy annotation data
                    newAnno = JSON.parse( JSON.stringify( oldAnno ) );
                    newAnno.x = info.x;
                    newAnno.y = info.y;

                    if ( oldTilekey === newTilekey &&
                         oldBinkey === newBinkey ) {

                        // remaining in same bin
                        // change internal data from old to new anno
                        that.features[ newTilekey ][ newBinkey ].setAnnotation( newAnno );

                    } else if ( that.features[ newTilekey ][ newBinkey ] !== undefined ) {

                        // feature already exists in new location, aggregate!
                        // add annotation to aggregate
                        that.features[ newTilekey ][ newBinkey ].addAnnotation( newAnno );
                        // remove and destroy feature
                        that.removeAndDestroyFeature( oldTilekey, oldBinkey );

                    } else {

                        // un-occupied bin
                        // move feature to new bin
                        that.features[ newTilekey ][ newBinkey ] = that.features[ oldTilekey ][ oldBinkey ];
                        // delete old bin
                        delete that.features[ oldTilekey ][ oldBinkey ];
                        // change internal data from old to new anno
                        that.features[ newTilekey ][ newBinkey ].setAnnotation( newAnno );
                    }

                    // send POST request to modify annotation on server
                    that.service.modifyAnnotation( oldAnno, newAnno, $.proxy( that.modifyCallback, that) );

                },

                onDrag: function(feature, pixel) {

                    // centre popup on feature when dragging
                    feature.attributes.feature.centrePopup( OpenLayers.LonLat.fromString( feature.geometry.toShortString() ) );
                }

            });

            // attach layer and controls to map
            this.map.addOLLayer( this.olLayer_ );
            this.map.addOLControl(this.addPointControl);
            this.map.addOLControl(this.dragControl);
            this.map.addOLControl(this.highlightControl);
            this.map.addOLControl(this.selectControl);

            // activate controls
            if (this.accessibility.write) {
                this.addPointControl.activate();
            }
            this.dragControl.activate();
            this.highlightControl.activate();
            this.selectControl.activate();
        }
        */
     });

    return AnnotationLayer;
});
