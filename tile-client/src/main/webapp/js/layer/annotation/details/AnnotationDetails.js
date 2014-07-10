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


define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        ANNOTATION_DETAILS_CLASS = 'annotation-details',
        ANNOTATION_DETAILS_CONTENT_CLASS = "annotation-details-content",
        ANNOTATION_DETAILS_AGGREGATE_CLASS = "annotation-details-aggregate",
        ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS = "annotation-details-close-button",
        ANNOTATION_CAROUSEL_CLASS = "annotation-carousel",
        ANNOTATION_CHEVRON_CLASS = "annotation-carousel-ui-chevron",
        ANNOTATION_CHEVRON_LEFT_CLASS = "annotation-carousel-ui-chevron-left",
        ANNOTATION_CHEVRON_RIGHT_CLASS = "annotation-carousel-ui-chevron-right",
        ANNOTATION_INDEX_CLASS = "annotation-carousel-ui-text-index",
        createRootElement,
        createContentElement,
        AnnotationDetails;


    createRootElement = function() {
        return $('<div class="'+ANNOTATION_DETAILS_CLASS+'"></div>');
    };

    createContentElement = function() {
        return $('<div class="'+ANNOTATION_DETAILS_CONTENT_CLASS+'"></div>');
    };


    AnnotationDetails = Class.extend({
        ClassName: "AnnotationDetails",


        init: function( map ) {
            this.map = map;
        },

        createWriteDetailsImpl: function( annotation, $bin ) {
            return null;
        },

        createDisplayDetailsImpl: function( bin, $bin ) {
            return null;
        },

        createEditDetailsImpl: function( annotation, $bin ) {
            return null;
        },

        registerLayer: function( layerState ) {
            this.layerState = layerState;
        },

        createWriteDetails: function( annotation, $bin ) {
            this.destroyDetails();
            return createRootElement()
                       .append( createContentElement()
                           .append( this.createWriteDetailsImpl( annotation ) ) )
                       .append( this.createCloseButton() );
        },

        createDisplayDetails: function( bin, $bin ) {
            var $details;
            this.destroyDetails();
            $details = createRootElement()
                         .append( createContentElement()
                             .append( this.createDisplayDetailsImpl( bin[0] ) ) )
                         .append( this.createCloseButton() );

            // if more than one annotation, create carousel ui
            if ( bin.length > 1 ) {
                $details.addClass( ANNOTATION_DETAILS_AGGREGATE_CLASS );
                $details.append( this.createCarouselUI( bin ) );
            }

            // append now so later measurements are valid
            $bin.append( $details );

            // make details draggable and resizable
            $details.draggable().resizable({
                minHeight: $details.find("."+ANNOTATION_DETAILS_CONTENT_CLASS).height(),
                minWidth: $details.find("."+ANNOTATION_DETAILS_CONTENT_CLASS).width()
            });

            return $details;
        },

        createEditDetails: function( annotation ) {
            this.destroyDetails();
            return createRootElement()
                       .append( createContentElement()
                           .append( this.createEditDetailsImpl( annotation ) ) )
                       .append( this.createCloseButton() );
        },

        createCloseButton: function() {

            var that = this,
                $closeButton = $('<div class="'+ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS+'"></div>');
            $closeButton.click( function() {
                that.layerState.set('click', null );
                that.destroyDetails();
                event.stopPropagation();
            });
            return $closeButton;
        },

        createCarouselUI: function( bin ) {

            var that = this,
                $carousel,
                $leftChevron,
                $rightChevron,
                $indexText,
                index = 0;

            function mod( m, n ) {
                return ((m % n) + n) % n;
            }

            function indexText() {
                return (index+1) +' of '+ bin.length;
            }

            function createClickFunc( inc ) {
                return function() {
                    // change index
                    index = mod( index+inc, bin.length );
                    console.log(index);
                    // swap content
                    $( '.'+ANNOTATION_DETAILS_CONTENT_CLASS ).html( that.createDisplayDetailsImpl( bin[index] ) );
                    // update index text
                    $indexText.text( indexText() );
                    // prevent event from propagating
                    event.stopPropagation();
                };
            }

            // chevrons
            $leftChevron = $("<div class='"+ANNOTATION_CHEVRON_CLASS+" "+ANNOTATION_CHEVRON_LEFT_CLASS+"'></div>");
            $rightChevron = $("<div class='"+ANNOTATION_CHEVRON_CLASS+" "+ANNOTATION_CHEVRON_RIGHT_CLASS+"'></div>");
            $leftChevron.click( createClickFunc(-1) );
            $rightChevron.click( createClickFunc(1) );
            // index text
            $indexText = $('<div class="'+ANNOTATION_INDEX_CLASS+'">'+ indexText() +'</div>');
            // carousel
            $carousel = $('<div class="'+ANNOTATION_CAROUSEL_CLASS+'"></div>');
            $carousel.append( $leftChevron );
            $carousel.append( $rightChevron );
            $carousel.append( $indexText );
            return $carousel;
        },

        destroyDetails: function() {

            $( "."+ANNOTATION_DETAILS_CLASS ).remove();
        }

     });

     return AnnotationDetails;

});
