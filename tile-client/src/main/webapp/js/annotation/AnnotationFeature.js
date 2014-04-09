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

            // TODO: maybe just have this point to the raw Service data?
            this.annotationsByPriority = spec.annotationsByPriority;

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
        }


     });

    return AnnotationFeature;
});
