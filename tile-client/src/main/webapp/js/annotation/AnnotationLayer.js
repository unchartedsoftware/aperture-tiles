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
        uniquePopupId = 0,
        defaultStyle,
        hoverStyle,
        selectStyle,
        temporaryStyle,
        AnnotationLayer,
        generatePopup,
        generateEditablePopup;

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

        var titleFill = "placeholder='Enter title'",
            priorityFill = "placeholder='Enter priority'",
            commentFill = "placeholder='Enter description'";

        if ( feature.attributes.annotation.data.title !== undefined ) {
            titleFill = "value='" + feature.attributes.annotation.data.title + "'";
        }

        if ( feature.attributes.annotation.priority !== undefined ) {
            priorityFill = "value='" + feature.attributes.annotation.priority + "'";
        }

        if ( feature.attributes.annotation.data.comment !== undefined ) {
            commentFill = "value='" + feature.attributes.annotation.data.comment + "'";
        }

        return  "<div style='overflow:hidden'>"+

                    "<div style='overflow:hidden' id='" + id + "' class='ui-widget-content'>"+

                        "<div style='padding-top:15px; padding-left:15px; '>"+
                            "<div style='font-weight:bold; padding-bottom:10px;'>" +

                                "<label style='width:70px; float:left;'>Title: </label>" +
                                "<input style='width: calc(100% - 90px)' type='text' "+titleFill+">" +

                            "</div>"+
                            "<div style='font-weight:bold; padding-bottom:10px;'>" +

                                "<label style='width:70px; float:left;'>Priority: </label>" +
                                "<input style='width: calc(100% - 90px)' type='text' "+priorityFill+">" +

                            "</div>"+
                            "<div style='font-weight:bold;  padding-bottom:10px;'> Description: </div>" +
                            "<div>"+
                                  "<textarea style='width: calc(100% - 25px); resize:none;' >"+ commentFill+"</textarea> "+
                            "</div>"+
                        "</div>"+

                    "</div>"+
                "<button style='margin-top:10px; border-radius:5px; float:right; '>Cancel</button>"+
                "<button style='margin-top:10px; border-radius:5px; float:right; '>Save</button>"+
                "</div>";
    };




    defaultStyle = new OpenLayers.Style({
        externalGraphic: "./images/marker-purple.png",
        graphicWidth: 24,
        graphicHeight: 24,
        graphicYOffset: -23,
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

                that.unselect();
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

            var fromProj = new OpenLayers.Projection("EPSG:4326"),
                toProj = this.map.projection;

            return new OpenLayers.LonLat( latLon.lon, latLon.lat ).transform( fromProj, toProj );
        },


        transformFromMapProj: function(latLon) {

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


        createPopup: function( e ) {

            this.selectedFeature = e.feature.attributes.annotation;

            var that = this,
                latlon = OpenLayers.LonLat.fromString( e.feature.geometry.toShortString() ),
                popupId = "annotation-popup-" + uniquePopupId++;

            this.popup = new OpenLayers.Popup("annotation-popup",
                                               latlon,
                                               null,
                                               generatePopup( popupId, e.feature ),
                                               true,
                                               function() {
                                                   that.destroyPopup();
                                                   that.unselect();
                                               });

            this.popup.autoSize = true;
            e.feature.popup = this.popup;
            this.map.map.olMap_.addPopup( this.popup, false );

            this.centrePopup( latlon );

            $( "#"+popupId ).resizable({
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


            $( "#"+popupId+"-edit" ).click( function() {

                that.popup.setContentHTML( generateEditablePopup( popupId, e.feature ) );
                // TODO: will need to rebind jQuery callbacks to this new html
            });
        },


        destroyPopup: function() {

            if ( this.popup !== null && this.popup !== undefined ) {
                this.map.map.olMap_.removePopup( this.popup );
                this.popup.destroy();
                this.popup = null;
                this.selectedFeature = null;
                //this.selectControl.clickoutFeature();
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

                        var rawLatLon = OpenLayers.LonLat.fromString(e.feature.geometry.toShortString()),
                            latlon = that.transformFromMapProj( rawLatLon ),
                            annotation,
                            spec,
                            tempData = { x: latlon.lon, y: latlon.lat },
                            index = that.tracker.dataService.indexer.getIndex( tempData, that.map.getZoom() ),
                            key = index.tilekey + "," + index.binkey,
                            annotationsByPriority = {};


                        function randString( num ) {
                            var text = "";
                            var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                            for( var i=0; i < num; i++ ) {
                                text += possible.charAt(Math.floor(Math.random() * possible.length));
                            }
                            return text;
                        }

                        /*
                         TODO:  create feature with empty data, open edit popup. On 'save', write to server.
                         */
                        ////
                        annotation = {
                            x: latlon.lon,
                            y: latlon.lat,
                            priority : "P0",
                            data: {
                                title: randString( 24 ),
                                comment: randString( 256 )
                            }
                        };
                        /////


                        //// temp
                        if ( that.features[key] !== undefined ) {

                            // already exists, aggregate
                            that.features[key].addAnnotation( rawLatLon.lon, rawLatLon.lat, annotation );

                            // manually remove feature, since it is being aggregated
                            that.olLayer_.removeFeatures( [e.feature] );
                            e.feature.destroy();

                        } else {

                            // create new feature
                            annotationsByPriority[ annotation.priority ] = [ annotation ];

                            spec = {
                                feature : e.feature,
                                annotationsByPriority : annotationsByPriority,
                                key : key
                            };

                            that.features[key] = new AnnotationFeature( spec );
                            // no need to attach, its already attached to layer
                        }

                        // send POST request to write annotation to server
                        that.tracker.dataService.postRequest( "WRITE", annotation );

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

                    // deep copy annotation data
                    newAnno = JSON.parse( JSON.stringify( oldAnno ) );
                    newAnno.x = latlon.lon;
                    newAnno.y = latlon.lat;

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
                        that.features[ newkey ].addAnnotation( rawLatLon.lon, rawLatLon.lat, newAnno );

                        // completely remove old feature
                        that.features[ oldkey ].removeFromLayerAndDestroy( that.olLayer_ );
                        delete that.features[ oldkey ];

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

                    // set key attribute to new key
                    feature.attributes.key = newkey;

                    // send POST request to write annotation to server
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
