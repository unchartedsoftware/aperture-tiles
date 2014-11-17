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
        Util = require('../../util/Util'),
        PubSub = require('../../util/PubSub'),
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
        resetCarousel,
        makeClientLayersSubscriber,
        makeLayerSubscriber,
        repositionCarousel,
        createChevrons,
        createIndexDots,
        createCarousel,
        updateDotIndices,
        CarouselControls;

    repositionCarousel = function( map, $carousel, tilekey ) {

        var parsedValues = tilekey.split(','),
            level = parseInt( parsedValues[0], 10 ),
            xIndex = parseInt( parsedValues[1], 10 ),
            yIndex = parseInt( parsedValues[2], 10 ),
            topLeft,
            css = {};

        if ( xIndex < 0 || xIndex > ( 1 << level )-1 ||
            yIndex < 0  || yIndex > ( 1 << level )-1 ) {
            // prevent carousel from rendering outside of map bounds
            css.visibility = 'hidden';
        } else {
            // if carousel is enabled, update its tile position
            topLeft = map.getTopLeftMapPixelForTile( tilekey );
            css.left = topLeft.x;
            css.top = map.getMapHeight() - topLeft.y;
            css.visibility = 'visible';
        }
        $carousel.css( css );
    };


    resetCarousel = function( $carousel, layers ) {

        var layerIndex,
            maxZ = 0,
            i;

        // if a layer is disabled, ensure carousel appears
        // on top most carouselEnabled client layer
        for ( i=0; i<layers.length; i++ ) {
            if ( layers[i].getVisibility() &&
                 layers[i].getNumViews() > 1 &&
                 layers[i].getZIndex() > maxZ ) {
                layerIndex = i;
                maxZ = layers[i].getZIndex();
            }
            // set all false
            layers[i].setCarouselEnabled( false );
        }

        if ( layerIndex !== undefined ) {
            layers[layerIndex].setCarouselEnabled( true );
        } else {
            $carousel.empty().css('visibility', 'hidden');
        }
    };


    makeClientLayersSubscriber = function ( map, $carousel, controlMap, layers ) {
        return function ( message, path ) {

            var field = message.field;

            switch ( field ) {

                case "enabled":
                    resetCarousel( $carousel, layers );
                    break;

                case "zIndex":
                    resetCarousel( $carousel, layers );
                    break;
            }
        };
    };

    /**
     * Creates a subscriber to handle published carousel related layer state changes,
     * and update the controls based on them.
     */
    makeLayerSubscriber = function ( map, $carousel, controlMap, layer ) {

        return function ( message, path ) {

            var field = message.field,
                value = message.value;

            switch ( field ) {

                case "tileFocus":

                    if ( layer.isCarouselEnabled() ) {
                        repositionCarousel( map, $carousel, value );
                        updateDotIndices( controlMap, layer );
                    }
                    break;

                case "carouselEnabled":

                    if ( value === true ) {
                        // create carousel UI
                        $carousel.css('visibility', 'visible');
                        createCarousel( $carousel, map, controlMap, layer );
                    }
                    break;
            }
        };
    };


    updateDotIndices = function( controlMap, layer ) {

        var tilekey = layer.map.getTileFocus(),
            count = controlMap.dots.length,
            index = layer.getTileViewIndex( tilekey ) || 0,
            i;

        for (i=0; i<count; i++) {
            controlMap.dots[i].removeClass(DOT_CLASS_SELECTED).addClass(DOT_CLASS_DEFAULT);
        }
        controlMap.dots[index].removeClass(DOT_CLASS_DEFAULT).addClass(DOT_CLASS_SELECTED);

    };


    createChevrons = function( $carousel, map, controlMap, layer ) {

        var $leftChevron,
            $rightChevron;

        function generateCallbacks( chevron, inc ) {

            Util.dragSensitiveClick(chevron, function() {
                var tilekey = layer.map.getTileFocus(),
                    prevIndex = layer.getTileViewIndex( tilekey ) || 0,
                    mod = function (m, n) {
                        return ((m % n) + n) % n;
                    },
                    newIndex = mod( prevIndex + inc, layer.getNumViews() );

                layer.setTileViewIndex( tilekey, newIndex );
                updateDotIndices( controlMap, layer );
            });
        }

        $leftChevron = $("<div class='"+CHEVRON_CLASS+" "+CHEVRON_CLASS_LEFT+"'></div>");
        // set tooltip
        Util.enableTooltip( $leftChevron,
                         TOOLTIP_CHEVRON_LEFT );

        generateCallbacks( $leftChevron, -1 );
        $carousel.append( $leftChevron );

        $rightChevron = $("<div class='"+CHEVRON_CLASS+" "+CHEVRON_CLASS_RIGHT+"'></div>");
        // set tooltip
        Util.enableTooltip( $rightChevron,
                         TOOLTIP_CHEVRON_RIGHT );

        generateCallbacks( $rightChevron, 1 );
        $carousel.append( $rightChevron );

        // allow all events to propagate to map except 'click'
        Util.enableEventPropagation( $leftChevron );
        Util.enableEventPropagation( $rightChevron );
        Util.disableEventPropagation( $leftChevron, ['onclick', 'ondblclick'] );
        Util.disableEventPropagation( $rightChevron, ['onclick', 'ondblclick'] );

        controlMap.leftChevron = $leftChevron;
        controlMap.rightChevron = $rightChevron;

    };


    createIndexDots = function( $carousel, map, controlMap, layer ) {

        var indexClass,
            $indexContainer,
            $dots = [],
            rendererCount = layer.getNumViews(),
            i;

        function generateCallbacks( dot, index ) {
            Util.dragSensitiveClick(dot, function() {
                layer.setTileViewIndex( layer.map.getTileFocus(), index );
                updateDotIndices( controlMap, layer );
            });
        }

        $indexContainer = $("<div class='"+DOT_CONTAINER_CLASS+"'></div>");
        $carousel.append( $indexContainer );

        for (i=0; i < rendererCount; i++) {

            indexClass = (i === 0) ? DOT_CLASS_SELECTED : DOT_CLASS_DEFAULT;
            $dots[i] = $("<div id='" + DOT_ID_PREFIX +i+"' class='" + DOT_CLASS + " " +indexClass+"' value='"+i+"'></div>");
            // set tooltip
            Util.enableTooltip( $dots[i], TOOLTIP_INDEX_DOT );

            generateCallbacks( $dots[i], i );
            $indexContainer.append( $dots[i] );
            // allow all events to propagate to map except 'click'
            Util.enableEventPropagation( $dots[i] );
            Util.disableEventPropagation( $dots[i], ['onclick', 'ondblclick'] );
        }

        controlMap.dots = $dots;
    };


    createCarousel = function( $carousel, map, controlMap, layer ) {

        if ( layer.getNumViews() > 1 ) {
            // only create chevrons and indices if there is more than 1 layer
            createChevrons( $carousel, map, controlMap, layer );
            createIndexDots( $carousel, map, controlMap, layer );
        }
        map.getRootElement().append( $carousel );
        return $carousel;
    };


    CarouselControls = Class.extend({
        ClassName: "CarouselControls",

        /**
         * Initializes the carousel controls and registers callbacks against the layer objects
         *
         * @param layers - The list of layers the layer controls reflect and modify.
         * @param map - The map for which the layers are bound to.
         */
        init: function ( layers, map ) {

            var i;

            if ( layers.length < 1 ) {
                return;
            }

            this.controlMap = {};
            this.$carousel = $('<div class="' + CAROUSEL_CLASS +'"></div>');

            for ( i=0; i<layers.length; i++ ) {
                if ( layers[i].getNumViews() > 1 ) {
                    PubSub.subscribe( layers[i].getChannel(), makeLayerSubscriber( map, this.$carousel, this.controlMap, layers[i] ) );
                }
            }
            PubSub.subscribe( 'layer.client', makeClientLayersSubscriber( map, this.$carousel, this.controlMap, layers ) );

            resetCarousel( this.$carousel, layers );
        },

        noop: function() {
            return true;
        }

     });

    return CarouselControls;
});
