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

/*global OpenLayers*/

/**
 * This module defines the carousel UI class which is used to switch between client layer renderers for each tile.
 */
define(function (require) {
    "use strict";



    var Class = require('../../class'),
        CAROUSEL_CLASS = 'carousel-ui-pane',
        DOT_CONTAINER_CLASS = "carousel-ui-dot-container",
        DOT_CLASS = 'carousel-ui-dot',
        DOT_CLASS_DEFAULT = 'carousel-ui-dot-default',
        DOT_CLASS_SELECTED = 'carousel-ui-dot-selected',
        DOT_ID_PREFIX = 'carousel-ui-id-',
        CHEVRON_CLASS = "carousel-ui-chevron",
        CHEVRON_CLASS_LEFT = "carousel-ui-chevron-left",
        CHEVRON_CLASS_RIGHT = "carousel-ui-chevron-right",
        TOOLTIP_CHEVRON_RIGHT = "Next rendering",
        TOOLTIP_CHEVRON_LEFT = "Previous rendering",
        TOOLTIP_INDEX_DOT = "Rendering by index",
        Z_INDEX = 2000,
        makeLayerStateObserver,
        createChevrons,
        createIndexDots,
        createCarousel,
        updateDotIndices,
        CarouselControls;


    /**
     * Creates an observer to handle layer state changes, and update the controls based on them.
     */
    makeLayerStateObserver = function ( map, $carousel, controlMap, layerState ) {

        return function (fieldName) {

            switch (fieldName) {

                case "tileFocus":

                    var tilekey = layerState.get('tileFocus'),
                        topLeft;

                    if ( layerState.get('carouselEnabled') ) {
                        // if carousel is enabled, update its tile position
                        topLeft = map.getTopLeftMapPixelForTile( tilekey );
                        $carousel.css({
                            left: topLeft.x,
                            top: map.getMapHeight() - topLeft.y
                        });
                        updateDotIndices( controlMap, layerState );
                    }
                    break;

                case "carouselEnabled":

                    // empty carousel
                    $carousel.empty().css('visibility', 'hidden');
                    if ( layerState.get('carouselEnabled') ) {
                        // create carousel UI
                        $carousel.css('visibility', 'visible');
                        createCarousel( $carousel, map, controlMap, layerState );
                    }
                    break;
            }
        };
    };


    updateDotIndices = function( controlMap, layerState ) {

        var tilekey = layerState.get('tileFocus'),
            count = controlMap.dots.length,
            index = layerState.get('rendererByTile', tilekey) || 0,
            i;

        for (i=0; i<count; i++) {
            controlMap.dots[i].removeClass(DOT_CLASS_SELECTED).addClass(DOT_CLASS_DEFAULT);
        }
        controlMap.dots[index].removeClass(DOT_CLASS_DEFAULT).addClass(DOT_CLASS_SELECTED);

    };


    createChevrons = function( $carousel, map, controlMap, layerState ) {

        var $leftChevron,
            $rightChevron;

        function generateCallbacks( chevron, inc, side ) {

            chevron.mouseover( function() {
                layerState.set( 'hover', {
                    target: 'carousel-'+side+'-chevron'
                });
            });
            chevron.mouseout( function() {
                chevron.off('click');
                layerState.set( 'hover', null );
            });
            chevron.mousemove( function() { chevron.off('click'); });
            chevron.mousedown( function() {
                chevron.click( function() {

                    var tilekey = layerState.get('tileFocus'),
                        prevIndex = layerState.get( 'rendererByTile', tilekey ) || 0,
                        mod = function (m, n) {
                            return ((m % n) + n) % n;
                        },
                        newIndex = mod( prevIndex + inc, layerState.get('rendererCount') );

                    layerState.set( 'rendererByTile', tilekey, newIndex );
                    updateDotIndices( controlMap, layerState );
                });
            });
        }

        $leftChevron = $("<div class='"+CHEVRON_CLASS+" "+CHEVRON_CLASS_LEFT+"' title></div>");
        $leftChevron.tooltip({
            content: TOOLTIP_CHEVRON_LEFT,
            open: function() { layerState.set('carouselChevronTooltip', 'open'); },
            close: function() { layerState.set('carouselChevronTooltip', 'close'); },
            show: { delay: 800 }
        });
        generateCallbacks( $leftChevron, -1 );
        $carousel.append( $leftChevron );

        $rightChevron = $("<div class='"+CHEVRON_CLASS+" "+CHEVRON_CLASS_RIGHT+"' title></div>");
        $rightChevron.tooltip({
            content: TOOLTIP_CHEVRON_RIGHT,
            open: function() { layerState.set('carouselChevronTooltip', 'open'); },
            close: function() { layerState.set('carouselChevronTooltip', 'close'); },
            show: { delay: 800 }
        });
        generateCallbacks( $rightChevron, 1 );
        $carousel.append( $rightChevron );

        // allow all events to propagate to map except 'click'
        map.enableEventToMapPropagation( $leftChevron );
        map.enableEventToMapPropagation( $rightChevron );
        map.disableEventToMapPropagation( $leftChevron, ['onclick', 'ondblclick'] );
        map.disableEventToMapPropagation( $rightChevron, ['onclick', 'ondblclick'] );

        controlMap.leftChevron = $leftChevron;
        controlMap.rightChevron = $rightChevron;

    };


    createIndexDots = function( $carousel, map, controlMap, layerState ) {

        var indexClass,
            $indexContainer,
            $dots = [],
            rendererCount = layerState.get('rendererCount'),
            i;

        function tooltipOpen() {
            layerState.set('carouselIndexTooltip', 'open');
        }

        function tooltipClose() {
            layerState.set('carouselIndexTooltip', 'close');
        }

        function generateCallbacks( dot, index ) {

            dot.mouseover( function() {
                layerState.set( 'hover', {
                    target: 'carousel-index-dot-'+index
                });
            });
            dot.mouseout( function() {
                dot.off('click');
                layerState.set( 'hover', null );
            });
            dot.mousemove( function() { dot.off('click'); });
            dot.mousedown( function() {
                dot.click( function() {

                    layerState.set( 'rendererByTile', layerState.get('tileFocus'), index );
                    updateDotIndices( controlMap, layerState );
                });
            });
        }

        $indexContainer = $("<div class='"+DOT_CONTAINER_CLASS+"'></div>");
        $carousel.append( $indexContainer );

        for (i=0; i < rendererCount; i++) {

            indexClass = (i === 0) ? DOT_CLASS_SELECTED : DOT_CLASS_DEFAULT;
            $dots[i] = $("<div id='" + DOT_ID_PREFIX +i+"' class='" + DOT_CLASS + " " +indexClass+"' value='"+i+"' title></div>");
            $dots[i].tooltip({
                content: TOOLTIP_INDEX_DOT,
                show: { delay: 800 },
                open: tooltipOpen,
                close: tooltipClose
            });
            generateCallbacks( $dots[i], i );
            $indexContainer.append( $dots[i] );
            // allow all events to propagate to map except 'click'
            map.enableEventToMapPropagation( $dots[i] );
            map.disableEventToMapPropagation( $dots[i], ['onclick', 'ondblclick'] );
        }

        controlMap.dots = $dots;
    };


    createCarousel = function( $carousel, map, controlMap, layerState ) {

        if ( layerState.get('rendererCount') > 1 ) {
            // only create chevrons and indices if there is more than 1 layer
            createChevrons( $carousel, map, controlMap, layerState );
            createIndexDots( $carousel, map, controlMap, layerState );
        }
        map.getRootElement().append( $carousel );
        return $carousel;
    };


    CarouselControls = Class.extend({
        ClassName: "CarouselControls",

        /**
         * Initializes the carousel controls and registers callbacks against the LayerState objects
         *
         * @param layerStates - The list of layers the layer controls reflect and modify.
         * @param map - The map for which the layers are bound to.
         */
        init: function ( layerStates, map ) {

            var i;

            this.controlMap = {};
            this.$carousel = $('<div class="' + CAROUSEL_CLASS +'" style="z-index:'+Z_INDEX+';"></div>');

            for (i=0; i<layerStates.length; i++) {
                layerStates[i].addListener( makeLayerStateObserver( map, this.$carousel, this.controlMap, layerStates[i] ) );
                if (i === 0) {
                    layerStates[i].set( 'carouselEnabled', true );
                }
            }
        },

        noop: function() {
            return true;
        }

     });

    return CarouselControls;
});
