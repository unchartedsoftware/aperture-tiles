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
        ANNOTATION_POPUP_TITLE_ID = "annotation-popup-title-id",
        ANNOTATION_POPUP_PRIORITY_ID = "annotation-popup-priority-id",
        ANNOTATION_POPUP_DESCRIPTION_ID = "annotation-popup-description-id",
        uniquePopupId = 0,
        defaultStyle,
        hoverStyle,
        selectStyle,
        temporaryStyle,
        AnnotationLayer,
        generatePopup,
        generateEditablePopup,
        annotationContext;



    generatePopup = function( id, feature ) {

        return  "<input id='"+ id +"-edit' type='image' src='./images/edit-icon.png' width='17' height='17' style='position:absolute; right:0px; outline-width:0'>"+
                "<div style='overflow:hidden' id='" + id + "' class='ui-widget-content'>"+

                    "<div style='padding-top:5px; padding-left:15px;'>"+
                        "<div style='width:256px; font-weight:bold; padding-bottom:10px; padding-right:20px'>"+
                            feature.attributes.annotation.data.title +
                        "</div>"+
                        "<div style='padding-bottom:10px'>"+
                            "Priority: "+ feature.attributes.annotation.priority +
                        "</div>"+
                        "<div style='width:256px; height:100px; overflow:auto;  padding-right:15px;'>"+
                            feature.attributes.annotation.data.comment +
                        "</div>"+
                    "</div>"+
                "</div>";
    };


    generateEditablePopup = function( id, feature ) {

        var TITLE_PLACEHOLDER = 'Enter title',
            PRIORITY_PLACEHOLDER = 'Enter priority',
            DESCRIPTION_PLACEHOLDER = 'Enter description',
            titleVal= feature.attributes.annotation.data.title || "",
            priorityVal = feature.attributes.annotation.priority || "",
            descriptionVal = feature.attributes.annotation.data.comment || "";

        return  "<div style='overflow:hidden'>"+

                    "<div style='overflow:hidden' id='" + id + "' class='ui-widget-content'>"+

                        "<div style='padding-top:15px; padding-left:15px; '>"+
                            "<div style='font-weight:bold; padding-bottom:10px;'>" +

                                "<label style='width:70px; float:left;'>Title: </label>" +
                                "<input id='"+ ANNOTATION_POPUP_TITLE_ID +"' style='width: calc(100% - 90px)' type='text' placeholder='"+TITLE_PLACEHOLDER+"' value='"+ titleVal +"'>" +

                            "</div>"+
                            "<div style='font-weight:bold; padding-bottom:10px;'>" +

                                "<label style='width:70px; float:left;'>Priority: </label>" +
                                "<input id='"+ ANNOTATION_POPUP_PRIORITY_ID +"' style='width: calc(100% - 90px)' type='text' placeholder='"+PRIORITY_PLACEHOLDER+"' value='"+ priorityVal +"'>" +

                            "</div>"+
                            "<div style='font-weight:bold;  padding-bottom:10px;'> Description: </div>" +
                            "<div>"+
                                  "<textarea id='"+ ANNOTATION_POPUP_DESCRIPTION_ID +"' style='width: calc(100% - 25px); resize:none;' placeholder='"+DESCRIPTION_PLACEHOLDER+"'>"+ descriptionVal +"</textarea> "+
                            "</div>"+
                        "</div>"+

                    "</div>"+
                "<button id='" + id + "-cancel' style='margin-top:10px; border-radius:5px; float:right; '>Cancel</button>"+
                "<button id='" + id + "-save' style='margin-top:10px; border-radius:5px; float:right; '>Save</button>"+
                "</div>";
    };


    annotationContext = {
        getLabel: function( feature ) {
            return ( feature.attributes.key )
        }
    };


    defaultStyle = new OpenLayers.Style({
        externalGraphic: "./images/marker-purple.png",
        graphicWidth: 24,
        graphicHeight: 24,
        graphicYOffset: -23,

        context: annotationContext,

        label: "${count}",
        labelYOffset: 35,
        labelOutlineColor: "#000000",
        labelOutlineWidth: 3,
        fontColor: "#FFFFFF",

        'cursor': 'pointer'
    });

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
            this.createLayer();

            // set callbacks
            this.map.on('zoomend', function() {

                that.unselect();
                that.destroyPopup();
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
            this.tracker.filterAndRequestTiles( tiles, $.proxy( this.redrawAnnotations, this ) );
            // remove and redraw annotations
            this.redrawAnnotations();
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

            var i, j,
                xSum = 0,
                ySum = 0,
                sumCount = 0,
                annotationsByPriority = {},
                latlon,
                priority,
                spec;

            for (i=0; i<annotations.length; i++) {
                for (j=0; j<this.filters.length; j++) {

                    priority = annotations[i].priority;

                    if ( priority === this.filters[j].priority ) {

                        if ( annotationsByPriority[ priority ] === undefined ) {
                            annotationsByPriority[ priority ] = [];
                        }

                        // if within count, add
                        if ( annotationsByPriority[ priority ].length < this.filters[j].count ) {
                            xSum += annotations[i].x;
                            ySum += annotations[i].y;
                            sumCount++;
                            annotationsByPriority[ priority ].push( annotations[i] );
                        }
                    }
                }
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


        redrawAnnotations: function() {

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

                    this.features[ key ].removeFromLayerAndDestroy( this.olLayer_ );
                    delete this.features[ key ];
                }
            }

        },


        createEditablePopup: function( feature, closeFunc, saveFunc ) {

            var that = this,
                id = "annotation-popup-" + uniquePopupId++;

            function onClosePopup() {

                that.destroyPopup();
                that.unselect();
                closeFunc( feature );
            }

            this.createPopup( feature, id, generateEditablePopup, onClosePopup );

            $( "#"+id+"-save" ).click( function() {
                saveFunc( feature );
            });

            $( "#"+id+"-cancel" ).click( onClosePopup );

        },


        createDisplayPopup: function( feature, closeFunc, editFunc ) {

            var that = this,
                id = "annotation-popup-" + uniquePopupId++;

            function onClosePopup() {

                that.destroyPopup();
                that.unselect();
                closeFunc( feature );
            }

            this.createPopup( feature, id, generatePopup, onClosePopup );

            $( "#"+id+"-edit" ).click( function() {
                editFunc( feature );
            });

        },

        createPopup: function( feature, id, popupFunc, closeFunc ) {

            var that = this,
                latlon = OpenLayers.LonLat.fromString( feature.geometry.toShortString() );

            this.popup = new OpenLayers.Popup("annotation-popup",
                                               latlon,
                                               null,
                                               popupFunc( id, feature ),
                                               true,
                                               closeFunc );

            this.popup.autoSize = true;

            feature.popup = this.popup;

            this.map.map.olMap_.addPopup( this.popup, false );

            this.centrePopup( latlon );

            $( "#"+id ).resizable({
                resize: function() {
                    that.popup.updateSize();
                },
                stop: function() {
                    that.centrePopup( that.popup.lonlat );
                },
                maxWidth: 414,
                maxHeight: 256,
                minHeight: 80,
                minWidth: 129,
                handles: 'se'
            });

        },


        destroyPopup: function() {

            if ( this.popup !== null && this.popup !== undefined ) {
                this.map.map.olMap_.removePopup( this.popup );
                this.popup.destroy();
                this.popup = null;
            }
        },


        centrePopup: function( latlon ) {
            var px = this.map.map.olMap_.getLayerPxFromViewPortPx( this.map.map.olMap_.getPixelFromLonLat(latlon) ),
                size = this.popup.size;
            px.x -= size.w / 2;
            px.y -= size.h + 35;
            this.popup.moveTo( px );
            //this.popup.panIntoView();
            this.popup.lonlat = latlon;
        },


        checkAndSetData: function( feature ) {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $priority = $('#'+ANNOTATION_POPUP_PRIORITY_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID),
                annotation = feature.attributes.annotation;

            // check input values
            if ( $title.val() !== "" &&
                $priority.val() !== "" &&
                $description.val() !== "" ) {

                annotation.priority = $priority.val();
                annotation.data.title = $title.val();
                annotation.data.comment = $description.val();

                return true;
            }

            return false;
        },


        popupCreationClose : function( feature ) {

            // manually remove feature
            this.olLayer_.removeFeatures( [feature] );
            feature.destroy();
        },


        popupCreationSave : function( feature ) {

            var annotation = feature.attributes.annotation,
                key = feature.attributes.key,
                rawLatLon = OpenLayers.LonLat.fromString( feature.geometry.toShortString() ),
                annotationsByPriority = {},
                spec;

            if ( this.checkAndSetData( feature ) ) {

                if ( this.features[key] !== undefined ) {

                    // already exists, aggregate
                    this.features[key].addAnnotation( rawLatLon.lon, rawLatLon.lat, annotation );

                    // manually remove feature, since it is being aggregated
                    this.olLayer_.removeFeatures( [e.feature] );
                    e.feature.destroy();

                } else {

                    // create new feature
                    annotationsByPriority[ annotation.priority ] = [ annotation ];

                    spec = {
                        feature : feature,
                        annotationsByPriority : annotationsByPriority,
                        key : key
                    };

                    this.features[key] = new AnnotationFeature( spec );
                    // no need to attach, its already attached to layer
                }

                // send POST request to write annotation to server
                this.tracker.dataService.postRequest( "WRITE", annotation );

                this.destroyPopup();
                this.createDisplayPopup( feature, $.proxy( this.popupDisplayClose, this ),
                                                  $.proxy( this.popupDisplayEdit, this ) );
            }



        },


        popupEditClose : function( feature ) {
            // empty
        },


        popupEditSave : function( feature ) {


            if ( this.checkAndSetData( feature ) ) {

                // save stuff

                // then
                this.destroyPopup();
                this.createDisplayPopup( feature, $.proxy( this.popupDisplayClose, this ),
                                                  $.proxy( this.popupDisplayEdit, this ) );
            }


        },


        popupDisplayClose : function( feature ) {
            // empty
        },


        popupDisplayEdit : function( feature ) {
            //
            this.destroyPopup();
            console.log("here");
            this.createEditablePopup( feature, $.proxy( this.popupEditClose, this ),
                                               $.proxy( this.popupEditSave, this ) );

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
                        that.select();
                        /*
                        if ( that.selectedFeature === e.feature.attributes.annotation ) {
                            return;
                        }

                        that.destroyPopup();
                        that.createPopup( e );
                        */
                    },


                    "featureunselected": function(e) {

                        console.log("featureunselected");
                        that.unselect();
                    },


                    "featureadded": function(e) {

                        console.log("featureadded");

                        var rawLatLon = OpenLayers.LonLat.fromString( e.feature.geometry.toShortString() ),
                            latlon = that.transformFromMapProj( rawLatLon ),
                            annotation,
                            tempData = { x: latlon.lon, y: latlon.lat },
                            index = that.tracker.dataService.indexer.getIndex( tempData, that.map.getZoom() ),
                            key = index.tilekey + "," + index.binkey;

                        // create base annotation
                        annotation = {
                            x: latlon.lon,
                            y: latlon.lat,
                            data: {}
                        };

                        // add key to feature
                        e.feature.attributes.key = key;
                        e.feature.attributes.annotation = annotation;

                        that.createEditablePopup( e.feature, $.proxy( that.popupCreationClose, that ),
                                                             $.proxy( that.popupCreationSave, that ) );

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

                    if ( that.features[ key ].isAggregated() ) {
                        // disable dragging of aggregated points!
                        that.dragControl.handlers.drag.deactivate();
                    }

                    that.selectControl.clickFeature(feature);
                },


                onComplete: function(feature, pixel) {

                    var rawLatLon = OpenLayers.LonLat.fromString( feature.geometry.toShortString()),
                        latlon = that.transformFromMapProj( rawLatLon ),
                        tempData = { x: latlon.lon, y: latlon.lat },
                        oldkey = feature.attributes.key,
                        index = that.tracker.dataService.indexer.getIndex( tempData, that.map.getZoom() ),
                        newkey = index.tilekey + "," + index.binkey,
                        oldAnno,
                        newAnno;

                    // this feature only contains 1 annotation as you cannot drag aggregates
                    oldAnno = that.features[ oldkey ].getDataArray()[0];

                    // set key attribute to new key
                    feature.attributes.key = newkey;

                    // deep copy annotation data
                    newAnno = JSON.parse( JSON.stringify( oldAnno ) );
                    newAnno.x = latlon.lon;
                    newAnno.y = latlon.lat;

                    if ( oldkey === newkey ) {

                        console.log("A");
                        // remaining in same bin
                        // keep feature, change data

                        // change internal data
                        that.features[ newkey ].annotationsByPriority = {};
                        that.features[ newkey ].annotationsByPriority[ newAnno.priority ] = [ newAnno ];

                    } else if ( that.features[ newkey ] !== undefined ) {

                        console.log("B");
                        // occupied bin
                        // add data, remove old

                        // feature already exists in new location, aggregate!
                        that.features[ newkey ].addAnnotation( rawLatLon.lon, rawLatLon.lat, newAnno );

                        // un-select before destroying
                        that.highlightControl.unselect( feature );
                        that.selectControl.unselect( feature );

                        // completely remove old feature
                        that.features[ oldkey ].removeFromLayerAndDestroy( that.olLayer_ );
                        delete that.features[ oldkey ];

                    } else {

                        console.log("C");
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

                    //that.centrePopup( OpenLayers.LonLat.fromString(feature.geometry.toShortString()) );
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
