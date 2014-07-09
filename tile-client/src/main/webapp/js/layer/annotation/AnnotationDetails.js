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



    var ANNOTATION_DETAILS_CLASS = 'annotation-details',
        ANNOTATION_DETAILS_CONTENT_CLASS = "annotation-details-content",
        ANNOTATION_DETAILS_HEAD_CLASS = "annotation-details-head",
        ANNOTATION_DETAILS_BODY_CLASS = "annotation-details-body",
        ANNOTATION_DETAILS_AGGREGATE_CLASS = "annotation-details-aggregate",
        ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS = "annotation-details-close-button",
        ANNOTATION_CAROUSEL_CLASS = "annotation-carousel",
        ANNOTATION_CHEVRON_CLASS = "annotation-carousel-ui-chevron",
        ANNOTATION_CHEVRON_LEFT_CLASS = "annotation-carousel-ui-chevron-left",
        ANNOTATION_CHEVRON_RIGHT_CLASS = "annotation-carousel-ui-chevron-right",
        ANNOTATION_INDEX_CLASS = "annotation-carousel-ui-text-index",
        createCloseButton,
        createCarouselUI,
        createDetailsContent,
        createDetailsElement;



    createCloseButton = function () {

        var $closeButton = $('<div class="'+ANNOTATION_DETAILS_CLOSE_BUTTON_CLASS+'"></div>');
        $closeButton.click( function() {
            $('.point-annotation-aggregate').removeClass('clicked-aggregate');
            $('.point-annotation-front').removeClass('clicked-annotation');
            $( "."+ANNOTATION_DETAILS_CLASS ).remove();
            event.stopPropagation();
        });
        return $closeButton;
    };


    createCarouselUI = function ( bin, headerContentFunc, bodyContentFunc ) {

        var $carousel,
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
                // swap content
                $( '.'+ANNOTATION_DETAILS_CONTENT_CLASS ).replaceWith( createDetailsContent( headerContentFunc( bin[0] ), bodyContentFunc( bin[0] ) ) );
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
    };


    createDetailsContent = function ( $headerContent, $bodyContent ) {

        var $detailsHeader,
            $detailsBody,
            $detailsContent;

        $detailsContent = $('<div class="'+ANNOTATION_DETAILS_CONTENT_CLASS+'"></div>');

        $detailsHeader = $('<div class="'+ANNOTATION_DETAILS_HEAD_CLASS+'"></div>');
        $detailsHeader.append( $headerContent );

        $detailsBody = $('<div class="'+ANNOTATION_DETAILS_BODY_CLASS+'"></div>');
        $detailsBody.append( $bodyContent );

        $detailsContent.append( $detailsHeader );
        $detailsContent.append( $detailsBody );

        return $detailsContent;
    };


    createDetailsElement = function ( bin, $bin, headerContentFunc, bodyContentFunc ) {

        var $details;

        // remove any previous details
        $( "."+ANNOTATION_DETAILS_CLASS ).remove();

        // create details div
        $details = $('<div class="'+ANNOTATION_DETAILS_CLASS+'"></div>');

        // create display for first annotation
        $details.append( createDetailsContent( headerContentFunc( bin[0] ), bodyContentFunc( bin[0] ) ) );
        $details.append( createCloseButton() );

        // if more than one annotation, create carousel ui
        if ( bin.length > 1 ) {
            $details.addClass( ANNOTATION_DETAILS_AGGREGATE_CLASS );
            $details.append( createCarouselUI( bin, headerContentFunc, bodyContentFunc ) );
        }

        $bin.append( $details );

        // make details draggable and resizable
        $details.draggable().resizable({
            minHeight: $details.find("."+ANNOTATION_DETAILS_CONTENT_CLASS).height(),
            minWidth: $details.find("."+ANNOTATION_DETAILS_CONTENT_CLASS).width()
        });

        return $details;
    };


    return {

        destroyDetails : function () {

            $( "."+ANNOTATION_DETAILS_CLASS ).remove();
        },


        createDetails : function ( bin, $bin, headerContentFunc, bodyContentFunc ) {

            return createDetailsElement( bin, $bin, headerContentFunc, bodyContentFunc );
        }


     };

});
