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
        ANNOTATION_POPUP_ID = "annotation-popup-id",
        ANNOTATION_CANCEL_BUTTON_ID = ANNOTATION_POPUP_ID + "-cancel",
        ANNOTATION_SAVE_BUTTON_ID = ANNOTATION_POPUP_ID + "-save",
        ANNOTATION_EDIT_BUTTON_ID = ANNOTATION_POPUP_ID + "-edit",
        ANNOTATION_POPUP_TITLE_ID = "annotation-popup-title-id",
        ANNOTATION_POPUP_PRIORITY_ID = "annotation-popup-priority-id",
        ANNOTATION_POPUP_DESCRIPTION_ID = "annotation-popup-description-id",
        AnnotationFeature;



    AnnotationFeature = Class.extend({


        init: function ( spec ) {

            this.olFeature_ = spec.feature;
            this.olFeature_.attributes.key = spec.key;
            this.olFeature_.attributes.feature = this;

            this.popup = null;

            // TODO: maybe just have this point to the raw Service data?
            this.annotationsByPriority = spec.annotationsByPriority;

            //this.olFeature_.attributes.count = this.getAnnotationCount();

        },


        redraw: function() {
            this.olFeature_.layer.drawFeature( this.olFeature_ );
        },


        addAnnotation: function( featureX, featureY, annotation ) {

            var count = this.getAnnotationCount();

            // re calculate avg position
            this.olFeature_.geometry.x = ((this.olFeature_.geometry.x*count) + featureX) / (count+1);
            this.olFeature_.geometry.y = ((this.olFeature_.geometry.y*count) + featureY) / (count+1);


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


        removeAndDestroyPopup: function() {

            // destroy popup
            if ( this.popup !== null && this.popup !== undefined ) {
                this.olFeature_.layer.map.removePopup( this.popup );
                this.popup.destroy();
                this.popup = null;
            }
        },


        removeFromLayerAndDestroy: function() {

            this.removeAndDestroyPopup();
            this.olFeature_.layer.destroyFeatures( [this.olFeature_] );
        },


        createEditablePopup: function( closeFunc, saveFunc ) {

            // edit popup is only possible on single features, so take only data entry
            var that = this,
                html = this.getEditablePopupHTML( this.getDataArray()[0] );

            // create popup with close callback
            this.createPopup( html, function() {
                closeFunc( that );
            });

            // set save callback
            $( "#"+ANNOTATION_SAVE_BUTTON_ID ).click( function() {
                saveFunc( that );
            });

            // set cancel callback
            $( "#"+ANNOTATION_CANCEL_BUTTON_ID ).click( function() {
                closeFunc( that );
            });

        },


        createDisplayPopup: function( closeFunc, editFunc ) {

            var that = this,
                html;

            if ( this.isAggregated() ) {
                // aggregate
                // MAGICAL FILTER OF MAGIC
                html = this.getAggregateDisplayPopupHTML();

            } else {
                // single
                html = this.getSingleDisplayPopupHTML( this.getDataArray()[0] );

            }

            // create popup with close callback
            this.createPopup( html, function() {
                closeFunc( that );
            });


            if ( !this.isAggregated() ) {

                // set save callback
                $( "#"+ANNOTATION_EDIT_BUTTON_ID ).click( function() {
                    editFunc( that );
                });
            }


            // set cancel callback
            $( "#"+ANNOTATION_CANCEL_BUTTON_ID ).click( function() {
                closeFunc( that );
            });

        },


        createPopup: function( html, closeFunc ) {

            var that = this,
                latlon = OpenLayers.LonLat.fromString( this.olFeature_.geometry.toShortString() );

            this.popup = new OpenLayers.Popup("annotation-popup",
                                              latlon,
                                              null,
                                              html,
                                              true,
                                              closeFunc );

            this.popup.autoSize = true;
            this.olFeature_.popup = this.popup;
            this.olFeature_.layer.map.addPopup( this.popup, false );
            this.centrePopup( latlon );

            $( "#"+ANNOTATION_POPUP_ID ).resizable({
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


        centrePopup: function( latlon ) {

            var px,
                size;

            if (this.popup !== null && this.popup !== null ) {
                px = this.olFeature_.layer.map.getLayerPxFromViewPortPx( this.olFeature_.layer.map.getPixelFromLonLat(latlon) );
                size = this.popup.size;
                px.x -= size.w / 2;
                px.y -= size.h + 35;
                this.popup.moveTo( px );
                //this.popup.panIntoView();
            }

        },


        checkAndSetData: function() {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $priority = $('#'+ANNOTATION_POPUP_PRIORITY_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID),
                annotation = this.getDataArray()[0];

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

        getSingleDisplayPopupHTML: function( annotation ) {

            return  "<input id='"+ ANNOTATION_EDIT_BUTTON_ID + "' type='image' src='./images/edit-icon.png' width='17' height='17' style='position:absolute; right:0px; outline-width:0'>"+
                    "<div style='overflow:hidden' id='" + ANNOTATION_POPUP_ID + "' class='ui-widget-content'>"+

                        "<div style='padding-top:5px; padding-left:15px;'>"+
                            "<div style='width:256px; font-weight:bold; padding-bottom:10px; padding-right:20px'>"+
                                annotation.data.title +
                            "</div>"+
                            "<div style='padding-bottom:10px'>"+
                                "Priority: "+ annotation.priority +
                            "</div>"+
                            "<div style='width:256px; height:100px; overflow:auto;  padding-right:15px;'>"+
                                annotation.data.comment +
                            "</div>"+
                        "</div>"+
                    "</div>";
        },


        getAggregateDisplayPopupHTML: function() {

            return "<div style='overflow:hidden' id='" + ANNOTATION_POPUP_ID + "' class='ui-widget-content'>"+
                        "Example aggregate"+
                    "</div>";
        },


        getEditablePopupHTML: function( annotation ) {

            var TITLE_PLACEHOLDER = 'Enter title',
                PRIORITY_PLACEHOLDER = 'Enter priority',
                DESCRIPTION_PLACEHOLDER = 'Enter description',
                titleVal= annotation.data.title || "",
                priorityVal = annotation.priority || "",
                descriptionVal = annotation.data.comment || "";

            return  "<div style='overflow:hidden'>"+

                        "<div style='overflow:hidden' id='" + ANNOTATION_POPUP_ID + "' class='ui-widget-content'>"+

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

                        "<button id='" + ANNOTATION_CANCEL_BUTTON_ID + "' style='margin-top:10px; border-radius:5px; float:right; '>Cancel</button>"+
                        "<button id='" + ANNOTATION_SAVE_BUTTON_ID + "' style='margin-top:10px; border-radius:5px; float:right; '>Save</button>"+
                    "</div>";
        }



    });

    return AnnotationFeature;

});
