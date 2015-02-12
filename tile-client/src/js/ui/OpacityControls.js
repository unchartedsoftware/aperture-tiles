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
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

( function() {

    "use strict";

    var SliderUtil = require('./SliderUtil'),
        PubSub = require('../util/PubSub');

    module.exports = {

        /**
         * Creates an opacity jquery-ui slider and binds it to the provided layer.
         *
         * @param {Layer} layer - The layer object.
         *
         * @returns {JQuery} - The JQuery element.
         */
        create: function( layer ) {
            var $opacityControls = $( '<div class="opacity-controls"></div>' ),
                $opacityLabel = $( '<div class="controls-label">Opacity</div>' ),
                $sliderBorder = $( '<div class="slider-border">' ),
                $opacitySlider = $( '<div class="opacity-slider"></div>' ),
                MIN_VAL = 0,
                MAX_VAL = 100;
            $opacitySlider.slider({
                range: "min",
                min: MIN_VAL,
                max: MAX_VAL,
                value: layer.getOpacity() * MAX_VAL,
                slide: function( event, ui ) {
                    $( '.olTileImage' ).addClass( 'no-transition' );
                    $( '.olTileHtml' ).addClass( 'no-transition' );
                    layer.setOpacity( ui.value / MAX_VAL );
                    SliderUtil.createLabel( $( this ).find(".ui-slider-handle"), ui.value / MAX_VAL );
                },
                start: function( event, ui ) {
                    SliderUtil.createLabel( $( this ).find(".ui-slider-handle"), ui.value / MAX_VAL );
                },
                stop: function() {
                    var $handle = $( this ).find(".ui-slider-handle");
                    if ( !$handle.is(':hover') ) {
                        // normally this label is removed on mouse out, in the case that
                        // the user has moused out while dragging, this will cause the label to
                        // be removed
                        SliderUtil.removeLabel( $handle );
                    }                       
                    $( '.olTileImage' ).removeClass( 'no-transition' );
                    $( '.olTileHtml' ).removeClass( 'no-transition' );
                }
            });   
            // bind slider label mouseover and mouseout callbacks
            $opacitySlider.find( ".ui-slider-handle" ).mouseover( function() {
                SliderUtil.createLabel( $( this ), layer.getOpacity() );
            });
            $opacitySlider.find( ".ui-slider-handle" ).mouseout( function() {
                SliderUtil.removeLabel( $( this ) );
            });
            PubSub.subscribe( layer.getChannel(), function( message ) {
                if ( message.field === "opacity" ) {
                    $opacitySlider.slider('value', layer.getOpacity()*MAX_VAL );
                }
            });
            $opacityControls.append( $opacityLabel );
            $opacityControls.append( $sliderBorder.append( $opacitySlider ) );
            return $opacityControls;
        }
    };

}());