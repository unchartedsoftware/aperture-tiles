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
        ANNOTATION_CLASS = "annotation",
        ANNOTATION_ATTRIBUTE_CLASS = ANNOTATION_CLASS + " annotation-attribute",
        ANNOTATION_INPUT_CLASS = ANNOTATION_CLASS + " annotation-input",
        ANNOTATION_LABEL_CLASS = ANNOTATION_CLASS + " annotation-label",
        ANNOTATION_TEXTAREA_CLASS = ANNOTATION_CLASS + " annotation-textarea",
        ANNOTATION_TEXT_DIV_CLASS = ANNOTATION_CLASS + " annotation-text-div",
        ANNOTATION_CONTENT_CLASS = ANNOTATION_CLASS + " annotation-content-container",
        ANNOTATION_BUTTON_CLASS = ANNOTATION_CLASS + " annotation-button",
        ANNOTATION_LEFT_BUTTON_CLASS = ANNOTATION_BUTTON_CLASS + " annotation-button-left",
        ANNOTATION_RIGHT_BUTTON_CLASS = ANNOTATION_BUTTON_CLASS + " annotation-button-right",
        ANNOTATION_POPUP_OL_CONTAINER_ID = "annotation-ol-container",
        ANNOTATION_POPUP_ID = "annotation-popup-id",
        ANNOTATION_CANCEL_BUTTON_ID = "annotation-popup-cancel",
        ANNOTATION_SAVE_BUTTON_ID = "annotation-popup-save",
        ANNOTATION_EDIT_BUTTON_ID = "annotation-popup-edit",
        ANNOTATION_REMOVE_BUTTON_ID = "annotation-popup-remove",
        ANNOTATION_POPUP_TITLE_ID = "annotation-popup-title-id",
        ANNOTATION_POPUP_PRIORITY_ID = "annotation-popup-priority-id",
        ANNOTATION_POPUP_DESCRIPTION_ID = "annotation-popup-description-id",
        ANNOTATION_POPUP_RESIZE_ID = 'annotation-popup-resize-icon',
        ANNOTATION_ACCORDION_ID = "annotation-accordion-id",
        AnnotationFeature;



    AnnotationFeature = Class.extend({


        init: function ( spec  ) {

            /* There are two ways to instantiate a AnnotationFeature:
             *
             *  a)  creation by the user, in this case, the OpenLayers.Feature.Vector is created in the
             *      AnnotationLayer and already attached to the OpenLayers.Layer.Vector
             *
             *  b)  using data from the server, in this case the OpenLayers.Feature.Vector must be created
             *      internally and manually attached to the OpenLayers.Layer.Vector
             */

            // case a)
            if ( spec.feature !== undefined ) {
                // feature already exists ( created by user )
                this.olFeature_ = spec.feature;
                this.olLayer_ = spec.feature.layer;
            }

            // case b)
            if ( spec.layer !== undefined ) {
                // feature will need to be created as it is using data from server
                this.olLayer_ = spec.layer;
            }

            this.map = spec.map;

            // sets annotations, if feature has not been provided, creates it at average annotation location
            this.setAnnotations( spec.annotations );

            // append tilekey, binkey and a reference to the OpenLayers.Feature.attributes
            this.olFeature_.attributes.tilekey = spec.tilekey;
            this.olFeature_.attributes.binkey = spec.binkey;
            this.olFeature_.attributes.feature = this;
            this.olPopup_ = null;

            this.redraw();
        },


        redraw: function() {
            this.olFeature_.layer.drawFeature( this.olFeature_ );
        },


        setAnnotation: function( annotation ) {

            this.annotationsByPriority = {}; // clear old annotation
            this.annotationsByPriority[ annotation.priority ] = [ annotation ]; // set new annotation
        },


        setAnnotations: function( annotations ) {

            var i,
                xSum = 0,
                ySum = 0,
                sumCount = 0,
                annotationsByPriority = {},
                viewportPixel,
                latlon,
                priority;

            // for each annotation
            for (i=0; i<annotations.length; i++) {

                // organize by priority
                priority = annotations[i].priority;

                if ( annotationsByPriority[ priority ] === undefined ) {
                    annotationsByPriority[ priority ] = [];
                }

                xSum += annotations[i].x;
                ySum += annotations[i].y;
                sumCount++;
                annotationsByPriority[ priority ].push( annotations[i] );
            }

            // average display lat lon
            viewportPixel = this.map.getViewportPixelFromCoord( xSum/sumCount, ySum/sumCount );
            latlon = this.map.getOLMap().getLonLatFromViewPortPx( viewportPixel );
            this.annotationsByPriority = annotationsByPriority;

            if ( this.olFeature_ === undefined ) {
                // if no feature exists yet, create it
                this.olFeature_ = new OpenLayers.Feature.Vector( new OpenLayers.Geometry.Point( latlon.lon, latlon.lat) );
                this.addToLayer( this.olLayer_ );
            } else {
                // otherwise change its location to new location
                this.olFeature_.geometry.x = latlon.lon;
                this.olFeature_.geometry.y = latlon.lat;
                this.redraw();
            }
        },


        addAnnotation: function( annotation ) {

            var count = this.getAnnotationCount(),
                viewportPixel = this.map.getViewportPixelFromCoord( annotation.x, annotation.y),
                latlon = this.map.getOLMap().getLonLatFromViewPortPx( viewportPixel );

            // re calculate avg position
            this.olFeature_.geometry.x = ((this.olFeature_.geometry.x*count) + latlon.lon) / (count+1);
            this.olFeature_.geometry.y = ((this.olFeature_.geometry.y*count) + latlon.lat) / (count+1);

            // add annotation to data object
            if ( this.annotationsByPriority[ annotation.priority ] === undefined ) {
                this.annotationsByPriority[ annotation.priority ] = [];
            }
            this.annotationsByPriority[ annotation.priority ].push( annotation );
            this.redraw();

        },


        getDataArray: function() {

            var key, i,
                data = [];

            for (key in this.annotationsByPriority) {
                if (this.annotationsByPriority.hasOwnProperty(key)) {
                    for(i=0; i<this.annotationsByPriority[key].length; i++) {
                        data.push( this.annotationsByPriority[key][i] );
                    }
                }
            }
            return data;
        },


        isAggregated: function() {

            var key, i, count = 0;
            for (key in this.annotationsByPriority) {
                if (this.annotationsByPriority.hasOwnProperty(key)) {

                    for(i=0; i<this.annotationsByPriority[key].length; i++) {
                        count++;
                        if (count > 1) {
                            return true;
                        }
                    }

                }
            }
            return false;
        },


        getAnnotationCount: function() {

            var key, i, count = 0;
            for (key in this.annotationsByPriority) {
                if (this.annotationsByPriority.hasOwnProperty(key)) {
                    for(i=0; i<this.annotationsByPriority[key].length; i++) {
                        count++;
                    }

                }
            }
            return count;
        },


        addToLayer: function( layer ) {

            layer.addFeatures( [this.olFeature_], {silent:true} );
        },


        removeFromLayerAndDestroy: function() {

            this.removeAndDestroyPopup();
            this.olFeature_.layer.destroyFeatures( [this.olFeature_] );
        },


        removeAndDestroyPopup: function() {

            // destroy popup
            if ( this.olPopup_ !== null ) {
                this.olFeature_.layer.map.removePopup( this.olPopup_ );
                this.olPopup_.destroy();
                this.olPopup_ = null;
            }
        },


        createEditablePopup: function( priorities, closeFunc, saveFunc, removeFunc ) {

            // edit popup is only possible on single features, so take only data entry
            var that = this,
                hasRemoveFunc = ( removeFunc !== undefined ),
                html = this.getEditablePopupHTML( this.getDataArray()[0], priorities, hasRemoveFunc );

            // create popup with close callback
            this.createPopup( html, function() {
                closeFunc( that );
                return false;   // stop event propagation
            });

            // set save callback
            $( "#"+ANNOTATION_SAVE_BUTTON_ID ).click( function() {
                saveFunc( that );
                return false;   // stop event propagation
            });

            // set cancel callback
            $( "#"+ANNOTATION_CANCEL_BUTTON_ID ).click( function() {
                closeFunc( that );
                return false;   // stop event propagation
            });

            if ( hasRemoveFunc ) {
                // set remove callback
                $( "#"+ANNOTATION_REMOVE_BUTTON_ID ).click( function() {
                    removeFunc( that );
                    return false;   // stop event propagation
                });
            }
        },


        createDisplayPopup: function( closeFunc, editFunc ) {

            var that = this,
                html;

            if ( this.isAggregated() ) {
                // aggregate
                html = this.getAggregateDisplayPopupHTML();

            } else {
                // single
                html = this.getSingleDisplayPopupHTML( this.getDataArray()[0] );

            }

            // create popup with close callback
            this.createPopup( html, function() {
                closeFunc( that );
                return false;   // stop event propagation
            });


            if ( this.isAggregated() ) {

                // set accordion
                $( "#"+ANNOTATION_ACCORDION_ID ).accordion({
                    active: false,
                    collapsible: true,
                    heightStyle: "content"
                });

            } else {

                // set save callback
                $( "#"+ANNOTATION_EDIT_BUTTON_ID ).click( function() {
                    editFunc( that );
                    return false;   // stop event propagation
                });
            }

            // set cancel callback
            $( "#"+ANNOTATION_CANCEL_BUTTON_ID ).click( function() {
                closeFunc( that );
                return false;   // stop event propagation
            });

        },


        createPopup: function( html, closeFunc ) {

            var that = this,
                latlon = OpenLayers.LonLat.fromString( this.olFeature_.geometry.toShortString() );

            this.olPopup_ = new OpenLayers.Popup(ANNOTATION_POPUP_OL_CONTAINER_ID,
                                              latlon,
                                              null,
                                              html,
                                              true,
                                              closeFunc );

            this.olPopup_.autoSize = true;
            this.olFeature_.popup = this.olPopup_;
            this.olFeature_.layer.map.addPopup( this.olPopup_, false );
            this.centrePopup( latlon );

            $( "#"+ANNOTATION_POPUP_ID ).resizable({
                resize: function() {
                    that.olPopup_.updateSize();
                },
                stop: function() {
                    that.centrePopup( OpenLayers.LonLat.fromString( that.olFeature_.geometry.toShortString() ) );
                },
                maxWidth: 384,
                maxHeight: 256,
                minHeight: 150,
                minWidth: 200,
                handles: 'se'
            });

            // manually inject id for resize icon for clean css
            $( "#"+ANNOTATION_POPUP_ID).find(".ui-resizable-handle", ".ui-resizable-se").attr('id', ANNOTATION_POPUP_RESIZE_ID);

        },


        centrePopup: function( latlon ) {

            var px,
                size;

            if ( this.olPopup_ !== null ) {
                px = this.olFeature_.layer.map.getLayerPxFromViewPortPx( this.olFeature_.layer.map.getPixelFromLonLat(latlon) );
                size = this.olPopup_.size;
                px.x -= size.w / 2;
                px.y -= size.h + 32;
                this.olPopup_.moveTo( px );
                //this.olPopup_.panIntoView();
            }

        },


        checkData: function() {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $priority = $('#'+ANNOTATION_POPUP_PRIORITY_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID),
                INVALID_COLOR = {color: '#7e0004'},
                INVALID_COLOR_EFFECT_LENGTH_MS = 3000;

            // if entry is invalid, flash
            if ( $title.val() === "" ) {
                $title.effect("highlight", INVALID_COLOR, INVALID_COLOR_EFFECT_LENGTH_MS);
            }
            if ( $priority.val() === "" ) {
                $priority.effect("highlight", INVALID_COLOR, INVALID_COLOR_EFFECT_LENGTH_MS);
            }
            if ( $description.val() === "" ) {
                $description.effect("highlight", INVALID_COLOR, INVALID_COLOR_EFFECT_LENGTH_MS);
            }

            // check input values
            return ( $title.val() !== "" &&
                     $priority.val() !== "" &&
                     $description.val() !== "" );
        },


        setDataFromPopup: function( annotation ) {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $priority = $('#'+ANNOTATION_POPUP_PRIORITY_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID);

            annotation.priority = $priority.val();
            annotation.data.title = $title.val();
            annotation.data.comment = $description.val();
        },


        checkAndSetData: function() {

            var annotation = this.getDataArray()[0];

            if ( this.checkData() ) {
                this.setDataFromPopup( annotation );
                return true;
            }
            return false;
        },


        getSingleDisplayPopupHTML: function( annotation ) {

            return  "<div style='overflow:hidden'>"+
                        "<input id='"+ ANNOTATION_EDIT_BUTTON_ID + "' type='image' src='./images/edit-icon.png' width='17' height='17'>"+
                        "<div id='" +ANNOTATION_POPUP_ID+ "' class='ui-widget-content'>"+
                            "<div class='"+ANNOTATION_CONTENT_CLASS+"'>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +
                                    annotation.data.title +
                                "</div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +
                                    "Priority: "+ annotation.priority +
                                "</div>"+
                                "<div class='"+ANNOTATION_TEXT_DIV_CLASS+"'>"+
                                    annotation.data.comment +
                                "</div>"+
                            "</div>"+
                        "</div>"+
                    "</div>";
        },


        getAggregateDisplayPopupHTML: function() {

            var annotations = this.getDataArray(),
                i,
                html = "<div style='overflow:hidden'>"+
                            "<div id='" +ANNOTATION_POPUP_ID+ "' class='ui-widget-content'>"+
                                "<div class='"+ANNOTATION_CONTENT_CLASS+"'>"+
                                    "<div id='"+ANNOTATION_ACCORDION_ID+"'>";

            for (i=0; i<annotations.length; i++) {
                html += "<h3>" + annotations[i].data.title + "</h3>"+
                            "<div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +
                                    "Priority: "+ annotations[i].priority +
                                "</div>"+
                                "<div class='"+ANNOTATION_TEXT_DIV_CLASS+"'>"+
                                    annotations[i].data.comment +
                                "</div>"+
                            "</div>";
            }

            return html + "</div></div></div></div>";
        },


        getEditablePopupHTML: function( annotation, priorities, includeRemoveButton ) {

            var TITLE_PLACEHOLDER = ' Enter title',
                PRIORITY_PLACEHOLDER = 'Enter priority',
                DESCRIPTION_PLACEHOLDER = ' Enter description',
                titleVal= annotation.data.title || "",
                descriptionVal = annotation.data.comment || "",
                removeButtonHTML = includeRemoveButton ? "<button class='"+ANNOTATION_LEFT_BUTTON_CLASS+"' id='" + ANNOTATION_REMOVE_BUTTON_ID + "'>Remove</button>" : "";

                function getSelectHTML() {

                    var html = "<select class='"+ANNOTATION_INPUT_CLASS+"' id='"+ ANNOTATION_POPUP_PRIORITY_ID+"'>",
                        i;

                    for (i=0; i<priorities.length; i++) {
                        if ( priorities[i] === annotation.priority ) {
                            html += "<option selected='true' value='" +priorities[i] +"'>"+priorities[i]+"</option>";
                        } else {
                            html += "<option value='" +priorities[i] +"'>"+priorities[i]+"</option>";
                        }
                    }

                    if ( annotation.priority === undefined ) {
                        html += "<option selected='true' style='display:none;' value=''>"+PRIORITY_PLACEHOLDER+"</option>";
                    }

                    return html + "</select>";
                }

                return  "<div style='overflow:hidden'>"+
                            "<div id='" +ANNOTATION_POPUP_ID+ "' class='ui-widget-content'>"+
                            "<div class='"+ANNOTATION_CONTENT_CLASS+"'>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +

                                    "<label class='"+ANNOTATION_LABEL_CLASS+"'>Title: </label>" +
                                    "<input class='"+ANNOTATION_INPUT_CLASS+"' id='"+ ANNOTATION_POPUP_TITLE_ID +"' type='text' placeholder='"+TITLE_PLACEHOLDER+"' value='"+ titleVal +"'>" +

                                "</div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +

                                    "<label class='"+ANNOTATION_LABEL_CLASS+"'>Priority: </label>" +
                                    getSelectHTML()+

                                "</div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'> Description: </div>" +
                                "<textarea class='"+ANNOTATION_TEXTAREA_CLASS+"' id='"+ ANNOTATION_POPUP_DESCRIPTION_ID +"' placeholder='"+DESCRIPTION_PLACEHOLDER+"'>"+ descriptionVal +"</textarea> "+
                            "</div>"+

                        "</div>"+
                        removeButtonHTML +
                        "<button class='"+ANNOTATION_RIGHT_BUTTON_CLASS+"' id='" + ANNOTATION_CANCEL_BUTTON_ID + "'>Cancel</button>"+
                        "<button class='"+ANNOTATION_RIGHT_BUTTON_CLASS+"' id='" + ANNOTATION_SAVE_BUTTON_ID + "'>Save</button>"+
                    "</div>";
        }



    });

    return AnnotationFeature;

});
