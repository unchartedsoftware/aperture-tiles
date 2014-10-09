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
        PubSub = require('../../../util/PubSub'),
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
        appendAndCenterDetails,
        AnnotationDetails;



    createRootElement = function() {
        return $('<div class="'+ANNOTATION_DETAILS_CLASS+'"></div>');
    };

    createContentElement = function() {
        return $('<div class="'+ANNOTATION_DETAILS_CONTENT_CLASS+'"></div>');
    };

    appendAndCenterDetails = function( $details, $annotations ) {
        // append now so later measurements are valid
        $annotations.append( $details );
        // make details draggable and resizable
        $details.draggable().resizable({
            minHeight: $details.find("."+ANNOTATION_DETAILS_CONTENT_CLASS).height(),
            minWidth: $details.find("."+ANNOTATION_DETAILS_CONTENT_CLASS).width(),
            stop: function( event, ui ) {
                $details.data( 'hasBeenResized', true );
            }
        });
        // center details
        $details.css('margin-left', -$details.width() / 2);
        return $details;
    };


    AnnotationDetails = Class.extend({
        ClassName: "AnnotationDetails",


        init: function( map ) {
            this.map = map;
        },

        createWriteDetailsImpl: function( annotation, $annotations ) {
            return null;
        },

        createDisplayDetailsImpl: function( annotations, $annotations ) {
            return null;
        },

        createEditDetailsImpl: function( annotation, $annotations ) {
            return null;
        },

        createWriteDetails: function( annotation, $annotations ) {
            var $details;
            this.destroyDetails();
            $details = createRootElement()
                       .append( createContentElement()
                           .append( this.createWriteDetailsImpl( annotation ) ) )
                       .append( this.createCloseButton() );

            // append and center the details panel
            return appendAndCenterDetails( $details, $annotations );
        },

        createDisplayDetails: function( annotations, $annotations ) {
            var $details;
            this.destroyDetails();
            $details = createRootElement()
                         .append( createContentElement()
                             .append( this.createDisplayDetailsImpl( annotations[0] ) ) )
                         .append( this.createCloseButton() );

            // if more than one annotation, create carousel ui
            if ( annotations.length > 1 ) {
                $details.addClass( ANNOTATION_DETAILS_AGGREGATE_CLASS );
                $details.append( this.createCarouselUI( $details, annotations ) );
            }

            // append and center the details panel
            return appendAndCenterDetails( $details, $annotations );
        },

        createEditDetails: function( annotation, $annotations ) {
            var $details;
            this.destroyDetails();
            $details = createRootElement()
                       .append( createContentElement()
                           .append( this.createEditDetailsImpl( annotation ) ) )
                       .append( this.createCloseButton() );

            // append and center the details panel
            return appendAndCenterDetails( $details, $annotations );
        },

        createCloseButton: function() {
            var that = this,
                $closeButton = $('<div class="'+ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS+'"></div>');
            $closeButton.click( function() {
                PubSub.publish( that.parent.getChannel(), { field: 'click', value: null } );
                that.destroyDetails();
                event.stopPropagation();
            });
            return $closeButton;
        },

        createCarouselUI: function( $details, annotations ) {

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
                return (index+1) +' of '+ annotations.length;
            }

            function createClickFunc( inc ) {
                return function() {
                    // change index
                    index = mod( index+inc, annotations.length );
                    // swap content
                    $( '.'+ANNOTATION_DETAILS_CONTENT_CLASS ).html( that.createDisplayDetailsImpl( annotations[index] ) );
                    // update index text
                    $indexText.text( indexText() );
                    // re-center details, but only if it hasn't been resized
                    if ( $details.data( 'hasBeenResized' ) !== true ) {
                        $details.css('margin-left', -$details.width() / 2);
                    }
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
