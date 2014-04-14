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



    var Class        = require('../class'),
        AnnotationService = require('./AnnotationService'),
        AnnotationFeature = require('./AnnotationFeature'),
        TileTracker = require('../layer/TileTracker'),
        defaultStyle,
        hoverStyle,
        selectStyle,
        temporaryStyle,
        annotationContext,
        AnnotationLayer;



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

    hoverStyle = new OpenLayers.Style({
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

            var that = this;

            this.map = spec.map;
            this.layer = spec.layer;
            this.filters = spec.filters;
            this.tracker = new TileTracker( new AnnotationService( spec.layer ) );
            this.features = {};
            this.pendingFeature = null;

            this.createLayer();

            // set callbacks
            this.map.on('zoomend', function() {

                that.unselect();

                while( that.map.map.olMap_.popups.length ) {
                    that.map.map.olMap_.removePopup( that.map.map.olMap_.popups[0] );
                }

                that.cancelPendingFeature();
                that.onMapUpdate();
            });

            this.map.on('panend', function() {

                that.onMapUpdate();
            });

            // trigger callback to draw first frame
            this.onMapUpdate();
        },


        onMapUpdate: function() {
            // determine all tiles in view
            var tiles = this.map.getTilesInView();
            // request annotation data for all tiles in view
            this.tracker.filterAndRequestTiles( tiles, $.proxy( this.updateAnnotations, this ) );
            // remove and redraw annotations
            this.updateAnnotations();
        },


        transformToMapProj: function(latLon) {
            // convert to lat / long to OpenLayers map projection
            var fromProj = new OpenLayers.Projection("EPSG:4326"),
                toProj = this.map.projection;
            return new OpenLayers.LonLat( latLon.lon, latLon.lat ).transform( fromProj, toProj );
        },


        transformFromMapProj: function(latLon) {
            // convert from OpenLayers map projection to lat / long
            var fromProj = this.map.projection,
                toProj = new OpenLayers.Projection("EPSG:4326");
            return new OpenLayers.LonLat( latLon.lon, latLon.lat ).transform( fromProj, toProj );
        },


        select: function() {
            this.addPointControl.deactivate();
        },


        unselect: function() {
            this.addPointControl.activate();
        },


        filterAnnotationBin: function( annotations, key ) {

            var i, //j,
                xSum = 0,
                ySum = 0,
                sumCount = 0,
                annotationsByPriority = {},
                latlon,
                priority,
                spec;


            for (i=0; i<annotations.length; i++) {
                //for (j=0; j<this.filters.length; j++) {

                    priority = annotations[i].priority;

                    //if ( priority === this.filters[j].priority ) {

                        if ( annotationsByPriority[ priority ] === undefined ) {
                            annotationsByPriority[ priority ] = [];
                        }

                        // if within count, add
                        //if ( annotationsByPriority[ priority ].length < this.filters[j].count ) {
                            xSum += annotations[i].x;
                            ySum += annotations[i].y;
                            sumCount++;
                            annotationsByPriority[ priority ].push( annotations[i] );
                        //}
                    //}
                //}
            }

            // average display lat lon
            latlon = this.transformToMapProj( new OpenLayers.LonLat( xSum/sumCount, ySum/sumCount ) );

            spec = {
                feature : new OpenLayers.Feature.Vector( new OpenLayers.Geometry.Point( latlon.lon, latlon.lat) ),
                annotationsByPriority : annotationsByPriority,
                key : key
            };

            return new AnnotationFeature( spec );

        },


        updateAnnotations: function() {

            var annotations = this.tracker.getDataObject(),
                key,
                tilekey,
                binkey,
                defunctFeatures = {};

            // Copy out all keys from the current data.  As we go through
            // making new requests, we won't bother with tiles we've
            // already received, but we'll remove them from the defunct
            // list.  When we're done requesting data, we'll then know
            // which tiles are defunct, and can be removed from the data
            // set.
            for (key in this.features) {
                if (this.features.hasOwnProperty( key )) {
                    defunctFeatures[ key ] = true;
                }
            }

            // for each tile
            for (tilekey in annotations) {
                if (annotations.hasOwnProperty(tilekey)) {

                    // for each bin
                    for (binkey in annotations[tilekey]) {
                        if (annotations[tilekey].hasOwnProperty(binkey)) {

                            key = tilekey + "," + binkey;

                            if ( defunctFeatures[ key ] ) {
                                // already have feature, remove from defunct list
                                delete defunctFeatures[ key ];
                            } else {
                                // do not have feature, create it
                                this.features[ key ] = this.filterAnnotationBin( annotations[tilekey][binkey], key );
                                this.features[ key ].addToLayer( this.olLayer_ );
                            }

                        }
                    }
                }
            }

            // remove no longer needed features from map
            for (key in defunctFeatures) {
                if (defunctFeatures.hasOwnProperty( key )) {

                    this.removeAndDestroyFeature( key );
                    //this.features[ key ].removeFromLayerAndDestroy();
                    //delete this.features[ key ];
                }
            }

        },


        removeAndDestroyFeature: function( key ) {

            var olFeature = this.features[ key ].olFeature_;

            this.highlightControl.unselect( olFeature );
            this.selectControl.unselect( olFeature );

            // completely remove old feature
            this.features[ key ].removeFromLayerAndDestroy();
            delete this.features[ key ];
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
                key = feature.olFeature_.attributes.key,
                rawLatLon = OpenLayers.LonLat.fromString( feature.olFeature_.geometry.toShortString() );

            if ( feature.checkAndSetData() ) {

                // null out pending feature as it is no longer pending
                this.pendingFeature = null;

                if ( this.features[key] !== undefined ) {

                    // already exists, aggregate
                    this.features[key].addAnnotation( rawLatLon.lon, rawLatLon.lat, annotation );
                    // destroy feature
                    feature.removeFromLayerAndDestroy();
                    // set feature to correct feature
                    feature = this.features[key];

                } else {

                    this.features[key] = feature;
                }

                // send POST request to write annotation to server
                this.tracker.dataService.postRequest( "WRITE", annotation );

                // get rid of popup
                feature.removeAndDestroyPopup();

                // select newly added annotation up. This will open display popup
                this.selectControl.select( feature.olFeature_ );
            }
        },


        popupEditRemove : function( feature ) {

            var key = feature.olFeature_.attributes.key;

            // send POST request to remove annotation from server
            this.tracker.dataService.postRequest( "REMOVE", feature.getDataArray()[0] );

            // remove and destroy feature from layer
            this.removeAndDestroyFeature( key );
        },


        popupEditClose : function( feature ) {
            // destroy popup
            feature.removeAndDestroyPopup();
            // go back to display
            feature.createDisplayPopup( $.proxy( this.popupDisplayClose, this ),
                                        $.proxy( this.popupDisplayEdit, this ) );
        },


        popupEditSave : function( feature ) {

            // get a deep copy of the old anootation data
            var oldAnno = JSON.parse( JSON.stringify( feature.getDataArray()[0] ) ),
                newAnno;

            if ( feature.checkAndSetData() ) {

                // get new data
                newAnno = feature.getDataArray()[0];

                // send POST request to modify annotation on server
                /*
                    Normally on a modify request all old instances held by the AnnotationService are
                    replaced. In this case, as the checkAndSetData() function sets the annotation
                    values, the service will not find the 'old' annotation, as its content will have been
                    replaced already.
                 */
                this.tracker.dataService.postRequest( "MODIFY", { "old" : oldAnno, "new" : newAnno } );

                // then remove edit popup, and add display popup
                feature.removeAndDestroyPopup();
                feature.createDisplayPopup( $.proxy( this.popupDisplayClose, this ),
                                            $.proxy( this.popupDisplayEdit, this ) );
            }
        },


        popupDisplayClose : function( feature ) {
            // destroy popup
            feature.removeAndDestroyPopup();
            this.selectControl.clickoutFeature(feature.olFeature_);
        },


        popupDisplayEdit : function( feature ) {
            // destroy popup
            feature.removeAndDestroyPopup();
            feature.createEditablePopup( $.proxy( this.popupEditClose, this ),
                                         $.proxy( this.popupEditSave, this ) );
        },


        getFeatureInfo: function( feature ) {

            var mapLatLon = OpenLayers.LonLat.fromString( feature.geometry.toShortString() ),
                latlon = this.transformFromMapProj( mapLatLon ),
                index = this.tracker.dataService.indexer.getIndex( {x:latlon.lon, y:latlon.lat}, this.map.getZoom() );

            return {
                    key: index.tilekey + "," + index.binkey,
                    lat: mapLatLon.lat,
                    lon: mapLatLon.lon,
                    x: latlon.lon,
                    y: latlon.lat
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
                    "hover": hoverStyle,
                    "select": selectStyle,
                    "temporary": temporaryStyle
                }),
                eventListeners: {

                    "featureselected": function(e) {

                        console.log("featureselected");

                        // cancel any pending feature creation
                        that.cancelPendingFeature();

                        that.select();

                        e.feature.attributes.feature.createDisplayPopup( $.proxy( that.popupDisplayClose, that ),
                                                                         $.proxy( that.popupDisplayEdit, that ) );

                    },


                    "featureunselected": function(e) {

                        console.log("featureunselected");

                        e.feature.attributes.feature.removeAndDestroyPopup();

                        that.unselect();
                    },


                    "featureadded": function(e) {

                        console.log("featureadded");

                        // cancel any previous pending feature creation
                        that.cancelPendingFeature();

                        var info = that.getFeatureInfo( e.feature ),
                            annotation;

                        // create base annotation
                        annotation = {
                            x: info.x,
                            y: info.y,
                            data: {}
                        };

                        // create temporary feature
                        that.pendingFeature = new AnnotationFeature({
                            feature : e.feature,
                            annotationsByPriority : { _pending : [annotation] },
                            key : info.key
                        });

                        // create edit popup to enter annotation info
                        that.pendingFeature.createEditablePopup( $.proxy( that.popupCreationClose, that ),
                                                                 $.proxy( that.popupCreationSave, that ));

                    }

                }
            });


            this.addPointControl = new OpenLayers.Control.DrawFeature( this.olLayer_, OpenLayers.Handler.Point );

            this.selectControl = new OpenLayers.Control.SelectFeature( this.olLayer_, {
                clickout: true
            });

            this.highlightControl = new OpenLayers.Control.SelectFeature( this.olLayer_, {
                hover: true,
                highlightOnly: true,
                renderIntent: "hover"

            });

            this.dragControl = new OpenLayers.Control.DragFeature( this.olLayer_, {

                onStart: function(feature, pixel) {

                    // get feature key
                    var key = feature.attributes.key;

                    if ( that.pendingFeature !== null &&
                         that.pendingFeature.olFeature_ === feature ) {
                        // cannot select pending feature
                        return;
                    }

                    if ( that.features[ key ].isAggregated() ) {
                        // disable dragging of aggregated points!
                        that.dragControl.handlers.drag.deactivate();
                    }

                    that.selectControl.clickFeature(feature);
                },


                onComplete: function(feature, pixel) {

                    var info = that.getFeatureInfo( feature ),
                        oldkey = feature.attributes.key,
                        newkey = info.key,
                        oldAnno,
                        newAnno;

                    // set key attribute to new key
                    feature.attributes.key = newkey;

                    // if dragging pending feature, update location and return
                    if (that.pendingFeature !== null &&
                        that.pendingFeature.olFeature_ === feature ) {

                        oldAnno = that.pendingFeature.getDataArray()[0];
                        oldAnno.x = info.x;
                        oldAnno.y = info.y;
                        return;
                    }

                    // this feature only contains 1 annotation as you cannot drag aggregates
                    oldAnno = that.features[ oldkey ].getDataArray()[0];

                    // deep copy annotation data
                    newAnno = JSON.parse( JSON.stringify( oldAnno ) );
                    newAnno.x = info.x;
                    newAnno.y = info.y;

                    if ( oldkey === newkey ) {

                        // remaining in same bin
                        // keep feature, change data

                        // change internal data
                        that.features[ newkey ].annotationsByPriority = {};
                        that.features[ newkey ].annotationsByPriority[ newAnno.priority ] = [ newAnno ];

                    } else if ( that.features[ newkey ] !== undefined ) {

                        // occupied bin
                        // add data, remove old

                        // feature already exists in new location, aggregate!
                        that.features[ newkey ].addAnnotation( info.lon, info.lat, newAnno );

                        // remove and destroy feature
                        that.removeAndDestroyFeature( oldkey );
                        /*
                        // un-select before destroying
                        that.highlightControl.unselect( feature );
                        that.selectControl.unselect( feature );

                        // completely remove old feature
                        that.features[ oldkey ].removeFromLayerAndDestroy();
                        delete that.features[ oldkey ];
                        */

                    } else {

                        // un-occupied bin
                        // swap new and old, change data

                        // move feature to new bin
                        that.features[ newkey ] = that.features[ oldkey ];

                        // delete old bin
                        delete that.features[ oldkey ];

                        // change internal data
                        that.features[ newkey ].annotationsByPriority = {};
                        that.features[ newkey ].annotationsByPriority[ newAnno.priority ] = [ newAnno ];
                    }

                    // send POST request to modify annotation on server
                    that.tracker.dataService.postRequest( "MODIFY", { "old" : oldAnno, "new" : newAnno } );

                },

                onDrag: function(feature, pixel) {

                    feature.attributes.feature.centrePopup( OpenLayers.LonLat.fromString( feature.geometry.toShortString() ) );
                }


            });

            this.map.addOLLayer( this.olLayer_ );
            this.map.addOLControl(this.addPointControl);
            this.map.addOLControl(this.dragControl);
            this.map.addOLControl(this.highlightControl);
            this.map.addOLControl(this.selectControl);

            this.addPointControl.activate();
            this.dragControl.activate();
            this.highlightControl.activate();
            this.selectControl.activate();
        }

     });

    return AnnotationLayer;
});
