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
        ANNOTAITON_BUTTON_CLASS = ANNOTATION_CLASS + " annotation-button",
        ANNOTATION_POPUP_OL_CONTAINER_ID = "annotation-ol-container",
        ANNOTATION_POPUP_ID = "annotation-popup-id",
        ANNOTATION_CANCEL_BUTTON_ID = "annotation-popup-cancel",
        ANNOTATION_SAVE_BUTTON_ID = "annotation-popup-save",
        ANNOTATION_EDIT_BUTTON_ID = "annotation-popup-edit",
        //ANNOTATION_REMOVE_BUTTON_ID = "annotation-popup-remove",
        ANNOTATION_POPUP_TITLE_ID = "annotation-popup-title-id",
        ANNOTATION_POPUP_PRIORITY_ID = "annotation-popup-priority-id",
        ANNOTATION_POPUP_DESCRIPTION_ID = "annotation-popup-description-id",
        ANNOTATION_ACCORDION_ID = "annotation-accordian-id",
        AnnotationFeature;



    AnnotationFeature = Class.extend({


        init: function ( spec ) {

            this.olFeature_ = spec.feature;
            this.olFeature_.attributes.key = spec.key;
            this.olFeature_.attributes.feature = this;

            this.popup = null;

            // TODO: maybe just have this point to the raw Service data?
            // filter on popoup?
            this.annotationsByPriority = spec.annotationsByPriority;

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


        removeFromLayerAndDestroy: function() {

            this.removeAndDestroyPopup();
            this.olFeature_.layer.destroyFeatures( [this.olFeature_] );
        },


        removeAndDestroyPopup: function() {

            // destroy popup
            if ( this.popup !== null && this.popup !== undefined ) {
                this.olFeature_.layer.map.removePopup( this.popup );
                this.popup.destroy();
                this.popup = null;
            }
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


            if ( this.isAggregated() ) {

                // set accordian
                $( "#"+ANNOTATION_ACCORDION_ID ).accordion({
                    active: false,
                    collapsible: true,
                    heightStyle: "content"
                });

            } else {

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

            this.popup = new OpenLayers.Popup(ANNOTATION_POPUP_OL_CONTAINER_ID,
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
                    that.centrePopup( OpenLayers.LonLat.fromString( that.olFeature_.geometry.toShortString() ) );
                },
                maxWidth: 384,
                maxHeight: 256,
                minHeight: 150,
                minWidth: 200,
                handles: 'se'
            });

        },


        centrePopup: function( latlon ) {

            var px,
                size;

            if ( this.popup !== null ) {
                px = this.olFeature_.layer.map.getLayerPxFromViewPortPx( this.olFeature_.layer.map.getPixelFromLonLat(latlon) );
                size = this.popup.size;
                px.x -= size.w / 2;
                px.y -= size.h + 32;
                this.popup.moveTo( px );
                //this.popup.panIntoView();
            }

        },


        checkAndSetData: function() {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $priority = $('#'+ANNOTATION_POPUP_PRIORITY_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID),
                INVALID_COLOR = '#7e0004',
                INVALID_COLOR_EFFECT_LENGTH_MS = 3000,
                annotation = this.getDataArray()[0];

            // if entry is invalid, flash
            if ( $title.val() === "" ) {
                $title.effect("highlight", {color: INVALID_COLOR}, INVALID_COLOR_EFFECT_LENGTH_MS);
            }

            if ( $priority.val() === "" ) {
                $priority.effect("highlight", {color: INVALID_COLOR}, INVALID_COLOR_EFFECT_LENGTH_MS);
            }

            if ( $description.val() === "" ) {
                $description.effect("highlight", {color: INVALID_COLOR}, INVALID_COLOR_EFFECT_LENGTH_MS);
            }

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


        getEditablePopupHTML: function( annotation ) {

            var TITLE_PLACEHOLDER = ' Enter title',
                PRIORITY_PLACEHOLDER = 'Enter priority',
                DESCRIPTION_PLACEHOLDER = ' Enter description',
                titleVal= annotation.data.title || "",
                descriptionVal = annotation.data.comment || "";

                function getSelectHTML() {

                    var html = "<select class='"+ANNOTATION_INPUT_CLASS+"' id='"+ ANNOTATION_POPUP_PRIORITY_ID+"'>",
                        PRIORITIES = [ 'Urgent', 'High', 'Medium', 'Low'],
                        i;

                    for (i=0; i<PRIORITIES.length; i++) {
                        if ( PRIORITIES[i] === annotation.priority ) {
                            html += "<option selected='true' value='" +PRIORITIES[i] +"'>"+PRIORITIES[i]+"</option>";
                        } else {
                            html += "<option value='" +PRIORITIES[i] +"'>"+PRIORITIES[i]+"</option>";
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

                        "<button class='"+ANNOTAITON_BUTTON_CLASS+"' id='" + ANNOTATION_CANCEL_BUTTON_ID + "'>Cancel</button>"+
                        "<button class='"+ANNOTAITON_BUTTON_CLASS+"' id='" + ANNOTATION_SAVE_BUTTON_ID + "'>Save</button>"+
                    "</div>";
        }



    });

    return AnnotationFeature;

});
