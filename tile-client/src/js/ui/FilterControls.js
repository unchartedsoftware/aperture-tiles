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
        Util = require('../util/Util'),
        AxisUtil = require('../map/AxisUtil'),
        PubSub = require('../util/PubSub');

    function createFilterAxis( minMax, transform ) {
        var NUM_MAJOR_MARKERS = 5;
        return $( '<div class="filter-axis"></div>' )
            .append( createAxisMarkers( NUM_MAJOR_MARKERS ) )
            .append( createAxisLabels( NUM_MAJOR_MARKERS, minMax, transform ) );
    }

    function createAxisMarkers( numMajorMarkers ) {
        var $container = $('<div class="filter-axis-ticks-container"></div>'),
            numMarkers = (numMajorMarkers-1)*2,
            margin = 100/numMarkers,
            isMajor = true,
            i;
        for( i = 0; i<numMarkers; i++ ) {
            if ( isMajor ) {
                $container.append( '<div class="filter-axis-marker filter-major-marker" style="margin-right:calc('+margin+'% - 1px);"></div>' );
            } else {
                $container.append( '<div class="filter-axis-marker filter-minor-marker" style="margin-right:calc('+margin+'% - 1px);"></div>' );
            }
            isMajor = !isMajor;
        }
        $container.append( '<div class="filter-axis-marker filter-major-marker" style="margin-left:-1px;"></div>' );
        return $container;
    }

    function createAxisLabels( numMajorMarkers, minMax, transform ){
        var $container = $('<div class="filter-axis-label-container"></div>'),
            margin = 100/(numMajorMarkers-1),
            options = {
                'stepDown' : true,
                'decimals' : 1,
                'type': 'b'
            },
            val,
            i;
        for ( i = 0; i<numMajorMarkers; i++ ) {
            val = Util.denormalizeValue( i / (numMajorMarkers-1), minMax, transform );
            $container.append(
                $( '<div class="filter-axis-label" style="position:absolute; left:'+(margin*i)+'%; width:'+margin+'%; margin-left:-'+(margin/2)+'%;">'+
                      AxisUtil.formatText( val, options ) +
                  '</div>' )
            );
        }
        return $container;
    }

    module.exports = {

        /**
         * Creates a filter jquery-ui slider and binds it to the provided layer.
         *
         * @param {Layer} layer - The layer object.
         *
         * @returns {JQuery} - The JQuery element.
         */
        create: function( layer ) {
            var $filterControls = $( '<div class="filter-controls"></div>' ),
                $filterLabel = $( '<div class="controls-label">Filter</div>' ),
                $sliderBorder = $( '<div class="slider-border">' ),
                $filterSlider = $( '<div class="filter-slider"></div>' ),
                MIN_VAL = 0,
                MAX_VAL = 100;
            $filterSlider.slider({
                range: true,
                min: MIN_VAL,
                max: MAX_VAL,
                values: [
                    layer.getRangeMinPercentage() * MAX_VAL,
                    layer.getRangeMaxPercentage() * MAX_VAL
                ],
                change: function () {
                    var result = $( this ).slider( "option", "values" );
                    // set by percentage
                    layer.setRangeMinPercentage( result[0] / MAX_VAL );
                    layer.setRangeMaxPercentage( result[1] / MAX_VAL );
                },
                slide: function( event, ui ) {
                    var handleIndex = $( ui.handle ).index() - 1,
                        values = ui.values,
                        value = Util.denormalizeValue(
                            values[ handleIndex ] / MAX_VAL,
                            layer.getLevelMinMax(),
                            layer.getValueTransformType() );
                    SliderUtil.createLabel( $( $filterSlider[0].children[ 1 + handleIndex ] ), value );
                },
                start: function( event, ui ) {
                    var handleIndex = $(ui.handle).index() - 1,
                        values = ui.values,
                        value = Util.denormalizeValue(
                            values[ handleIndex ] / MAX_VAL,
                            layer.getLevelMinMax(),
                            layer.getValueTransformType() );
                    SliderUtil.createLabel( $( $filterSlider[0].children[ 1 + handleIndex ] ), value );
                },
                stop: function( event, ui ) {
                    var handleIndex = $(ui.handle).index() - 1,
                        $handle = $( $filterSlider[0].children[ 1 + handleIndex ] );
                    if ( !$handle.is(':hover') ) {
                        // normally this label is removed on mouse out, in the case that
                        // the user has moused out while dragging, this will cause the label to
                        // be removed
                        SliderUtil.removeLabel( $handle );
                    }
                }
            });
            // bind slider label mouseover and mouseout callbacks to each handle of the slider
            $filterSlider.find( '.ui-slider-handle' ).each( function( index, elem ) {
                $( elem ).mouseover( function() {
                    var value = ( index === 0 ) ? layer.getRangeMinValue() : layer.getRangeMaxValue();
                    SliderUtil.createLabel( $( this ), value );
                });
                $( elem ).mouseout( function() {
                    SliderUtil.removeLabel( $( this ) );
                });
            });
            $filterSlider.append( createFilterAxis( layer.getLevelMinMax(), layer.getValueTransformType() ) );
            $filterSlider.find( '.ui-slider-handle' ).first().addClass( "filter-min-handle" );
            $filterSlider.find( '.ui-slider-handle' ).last().addClass( "filter-max-handle" );
            $filterControls.append( $filterLabel );
            $filterControls.append( $sliderBorder.append( $filterSlider ) );
            PubSub.subscribe( layer.getChannel(), function( message ) {
                switch ( message.field ) {

                    case 'rangeMin':

                        $filterSlider.slider( 'values', 0, layer.getRangeMinPercentage() * MAX_VAL );
                        break;

                    case 'rangeMax':

                        $filterSlider.slider( 'values', 1, layer.getRangeMaxPercentage() * MAX_VAL );
                        break;

                    case 'rampImageUrl':

                        $filterSlider.find( ".ui-slider-range" ).css({
                            "background": "url(" + message.value + ")",
                            "background-size": "contain"
                        });
                        break;

                    case 'levelMinMax':

                        $filterSlider.find( ".filter-axis" ).replaceWith(
                            createFilterAxis( layer.getLevelMinMax(), layer.getValueTransformType() )
                        );
                        break;
                }
            });
            return $filterControls;
        }
    };

}());
