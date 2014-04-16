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
        AnnotationFeature = require('./AnnotationFeature'),
        TileAnnotationIndexer = require('./TileAnnotationIndexer'),
        defaultStyle,
        highlightStyle,
        selectStyle,
        temporaryStyle,
        annotationContext,
        AnnotationLayer;


    // this function is used to determine what the label above each annotation is
    // only displays a number if above 1 ( annotation is aggregated )
    annotationContext = {
        getLabel: function( feature ) {
            if ( feature.attributes.feature === undefined || !feature.attributes.feature.isAggregated() ) {
                return "";
            }
            return feature.attributes.feature.getAnnotationCount();
        }
    };

    defaultStyle = new OpenLayers.Style({
        externalGraphic: "./images/marker-purple.png",
        graphicWidth: 24,
        graphicHeight: 24,
        graphicYOffset: -23,
        label: "${getLabel}",
        labelYOffset: 35,
        labelOutlineColor: "#000000",
        labelOutlineWidth: 3,
        fontColor: "#FFFFFF",
        'cursor': 'pointer'
    }, { context: annotationContext });

    highlightStyle = new OpenLayers.Style({
        externalGraphic: "./images/marker-green.png",
        graphicWidth: 24,
        graphicHeight: 24,
        graphicYOffset: -23,
        'cursor': 'pointer'
    });

    selectStyle = new OpenLayers.Style({
        externalGraphic: "./images/marker-blue.png",
        graphicWidth: 24,
        graphicHeight: 24,
        graphicYOffset: -23,
        'cursor': 'pointer'
    });

    temporaryStyle = new OpenLayers.Style({
        display:"none",
        'cursor': 'pointer'
    });


    AnnotationLayer = Class.extend({


        init: function (spec) {

            this.map = spec.map;
            this.layer = spec.layer;
            this.indexer = new TileAnnotationIndexer();
            //this.filters = spec.filters;
            this.service = new AnnotationService( spec.layer );
            this.features = {};
            this.pendingFeature = null;

            this.createLayer();

            // set callbacks
            this.map.on('zoomend', $.proxy( this.onMapZoom, this ) );
            this.map.on('panend', $.proxy( this.onMapUpdate, this ) );

            // trigger callback to draw first frame
            this.onMapUpdate();
        },

    /*
        transformToMapProj: function(latLon) {
            // convert to lat / long to OpenLayers map projection
            var fromProj = new OpenLayers.Projection("EPSG:4326"),
                toProj = this.map.projection;
            return new OpenLayers.LonLat( latLon.lon, latLon.lat ).transform( fromProj, toProj );
        },
*/

        transformFromMapProj: function(latLon) {
            // convert from OpenLayers map projection to lat / long
            var fromProj = this.map.projection,
                toProj = new OpenLayers.Projection("EPSG:4326");
            return new OpenLayers.LonLat( latLon.lon, latLon.lat ).transform( fromProj, toProj );
        },


        onMapZoom: function() {

            var tilekey, binkey;

            this.addPointControl.activate();

            // un-select all features
            this.highlightControl.unselectAll();
            this.selectControl.unselectAll();

            // for each tile
            for (tilekey in this.features) {
                if (this.features.hasOwnProperty( tilekey )) {
                    // for each bin
                    for (binkey in this.features[ tilekey ]) {
                        if (this.features[ tilekey ].hasOwnProperty( binkey )) {
                            // remove and delete from layer
                            this.features[ tilekey ][ binkey ].removeFromLayerAndDestroy();
                        }
                    }
                }
            }

            // clear feature map
            this.features = {};

            this.cancelPendingFeature();
            this.onMapUpdate();
        },


        onMapUpdate: function() {

            // determine all tiles in view
            var tiles = this.map.getTilesInView(),
                i;

            function createTileKey(tile) {
                return tile.level + "," + tile.xIndex + "," + tile.yIndex;
            }

            // request each tile from server
            for (i=0; i<tiles.length; i++ ) {
                this.service.getAnnotations( createTileKey( tiles[i] ), $.proxy( this.getCallback,this ) );
            }

        },


        /**
         * @param annotationData annotation data received from server of the form:
         *  {
         *      index: {
         *                  level:
         *                  xIndex:
         *                  yIndex:
         *             }
         *      annotationsByBin: {
         *                  <binkey>: [ <annotation>, <annotation>, ... ]
         *                  <binkey>: [ <annotation>, <annotation>, ... ]
         *            }
         *  }
         */
        getCallback: function( data ) {

            function createTileKey(tile) {
                return tile.level + "," + tile.xIndex + "," + tile.yIndex;
            }

            var tilekey = createTileKey( data.index),
                annotationsByBin = data.annotationsByBin,
                binkey,
                spec,
                defunctFeatures = {};

            if ( this.features[ tilekey ] === undefined ) {
                this.features[ tilekey ] = {};
            }

            for (binkey in this.features[ tilekey ]) {
                if (this.features[ tilekey ].hasOwnProperty( binkey )) {
                    defunctFeatures[ binkey ] = true;
                }
            }

            // for each bin
            for (binkey in annotationsByBin) {
                if (annotationsByBin.hasOwnProperty(binkey)) {

                    if ( this.features[ tilekey ][ binkey ] === undefined ) {

                        // create new feature
                        spec = {
                                annotations:annotationsByBin[ binkey ],
                                tilekey:tilekey,
                                binkey:binkey,
                                map: this.map,
                                layer:this.olLayer_
                        };

                        // feature does not exist, create it
                        this.features[ tilekey ][ binkey ] = new AnnotationFeature( spec );

                    } else {

                        // modify existing feature
                        this.features[ tilekey ][ binkey ].setAnnotations( annotationsByBin[ binkey ] );
                        // remove from defunct list so we know not to delete it later
                        delete defunctFeatures[ binkey ];
                    }
                }
            }

            // remove no longer needed features from map
            for (binkey in defunctFeatures) {
                if (defunctFeatures.hasOwnProperty( binkey )) {
                    // remove old feature
                    this.removeAndDestroyFeature( tilekey, binkey );
                }
            }

        },


        writeCallback: function( data ) {

            console.log("WRITE returned from server");
        },


        modifyCallback: function( data ) {

            console.log("MODIFY returned from server");
        },


        removeCallback: function( data ) {

            console.log("REMOVE returned from server");
        },


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
            // destroy popup
            feature.removeAndDestroyPopup();
            // go back to display
            feature.createDisplayPopup( $.proxy( this.popupDisplayClose, this ),
                                        $.proxy( this.popupDisplayEdit, this ) );
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
            feature.createEditablePopup( $.proxy( this.popupEditClose, this ),
                                         $.proxy( this.popupEditSave, this ),
                                         $.proxy( this.popupEditRemove, this ) );
        },


        getFeatureInfo: function( feature ) {

            var latLon = OpenLayers.LonLat.fromString( feature.geometry.toShortString() ),
                xy = this.transformFromMapProj( latLon ),
                index = this.indexer.getIndex( {x:xy.lon, y:xy.lat}, this.map.getZoom() );

            return {
                    tilekey: index.tilekey,
                    binkey: index.binkey,
                    lat: latLon.lat,
                    lon: latLon.lon,
                    x: xy.lon,
                    y: xy.lat
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

            this.olLayer_ = new OpenLayers.Layer.Vector( "annotation-layer-"+this.layer, { 
                ratio: 2,
                styleMap: new OpenLayers.StyleMap({ 
                    "default" : defaultStyle,
                    "highlight": highlightStyle,
                    "select": selectStyle,
                    "temporary": temporaryStyle
                }),
                eventListeners: {

                    "featureselected": function(e) {

                        // cancel any pending feature creation
                        that.cancelPendingFeature();
                        // if anything is selected, prevent click-out from adding a new point
                        that.addPointControl.deactivate();
                        // show display popup
                        e.feature.attributes.feature.createDisplayPopup( $.proxy( that.popupDisplayClose, that ),
                                                                         $.proxy( that.popupDisplayEdit, that ) );
                    },

                    "featureunselected": function(e) {

                        // remove and destroy popup
                        e.feature.attributes.feature.removeAndDestroyPopup();
                        // if nothing is selected, re-enable adding points
                        that.addPointControl.activate();
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
                            priority: "_pending",   // mark as pending
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
                        that.pendingFeature.createEditablePopup( $.proxy( that.popupCreationClose, that ),
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

                    if ( that.features[ tilekey ][ binkey ].isAggregated() ) {
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
            this.addPointControl.activate();
            this.dragControl.activate();
            this.highlightControl.activate();
            this.selectControl.activate();
        }

     });

    return AnnotationLayer;
});
