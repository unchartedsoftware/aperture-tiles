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
        AnnotationFeature;



    AnnotationFeature = Class.extend({


        init: function ( spec ) {

            this.olFeature_ = spec.feature;
            this.olFeature_.attributes.key = spec.key;


            this.olFeature_.attributes.feature = this;

            // TODO: maybe just have this point to the raw Service data?
            this.annotationsByPriority = spec.annotationsByPriority;

            this.olFeature_.attributes.count = this.getAnnotationCount();

        },


        addAnnotation: function( featureX, featureY, annotation ) {

            var count = this.getAnnotationCount();

            // re calculate avg position
            this.olFeature_.geometry.x = ((this.olFeature_.geometry.x*count) + featureX) / (count+1);
            this.olFeature_.geometry.y = ((this.olFeature_.geometry.y*count) + featureY) / (count+1);
            this.olFeature_.layer.drawFeature( this.olFeature_ );

            // add annotation to data object
            if ( this.annotationsByPriority[ annotation.priority ] === undefined ) {
                this.annotationsByPriority[ annotation.priority ] = [];
            }
            this.annotationsByPriority[ annotation.priority ].push( annotation );

            this.olFeature_.attributes.count++;
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


        removeFromLayer: function( layer ) {

            layer.removeFeatures( [this.olFeature_] );
        },


        removeFromLayerAndDestroy: function( layer ) {

            layer.destroyFeatures( [this.olFeature_] );
        },



        /*
        getDisplayPopupHTML: function( id, feature ) {

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
        },


        getEditablePopupHTML: function( id, annotation ) {

            var TITLE_PLACEHOLDER = 'Enter title',
                PRIORITY_PLACEHOLDER = 'Enter priority',
                DESCRIPTION_PLACEHOLDER = 'Enter description',
                titleVal= annotation.data.title || "",
                priorityVal = annotation.priority || "",
                descriptionVal = annotation.data.comment || "";

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
        }
        */


    });

    return AnnotationFeature;

});
