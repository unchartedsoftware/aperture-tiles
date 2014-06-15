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
 * This module defines a CarouselLayer class which inherits from a ClientLayer and provides a user
 * interface and event handler for switching between views for individual tiles
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
        Z_INDEX = 2000,
        makeLayerStateObserver,
        updateTileFocus,
        createChevrons,
        createIndexDots,
        createCarousel,
        CarouselControls;


    /**
     * Creates an observer to handle layer state changes, and update the controls based on them.
     */
    makeLayerStateObserver = function ( map, $carousel, layerState ) {

        return function (fieldName) {

            if (fieldName === "tileFocus") {

                var tilekey = layerState.getTileFocus(),
                    topLeft;

                topLeft = map.getTopLeftMapPixelForTile( tilekey );

                $carousel.css({
                    left: topLeft.x,
                    top: map.getMapHeight() - topLeft.y
                });
            }
        };
    };


    updateTileFocus = function ( x, y, map, layerStates ) {

        var tilekey = map.getTileKeyFromViewportPixel( x, y ), // get tilekey under mouse
            i;

        for (i=0; i<layerStates.length; i++) {
            layerStates[i].setTileFocus( tilekey );
        }
    };


    createChevrons = function( $carousel, map, controlMapping ) {

        var $leftChevron,
            $rightChevron;

        function generateCallbacks( chevron, inc ) {

            chevron.mouseout( function() { chevron.off('click'); });
            chevron.mousemove( function() { chevron.off('click'); });
            chevron.mousedown( function() {
                chevron.click( function() {
                    return true;
                    /*
                    var tilekey = that.selectedTileInfo.tilekey,
                        prevIndex = that.clientState.getTileViewIndex(tilekey),
                        mod = function (m, n) {
                            return ((m % n) + n) % n;
                        },
                        newIndex = mod(prevIndex + inc, that.views.length);

                    that.clientState.setTileViewIndex( tilekey, newIndex );
                  */

                });
            });
        }

        $leftChevron = $("<div class='"+CHEVRON_CLASS+" "+CHEVRON_CLASS_LEFT+"'></div>");
        generateCallbacks( $leftChevron, -1 );
        $carousel.append( $leftChevron );

        $rightChevron = $("<div class='"+CHEVRON_CLASS+" "+CHEVRON_CLASS_RIGHT+"'></div>");
        generateCallbacks( $rightChevron, 1 );
        $carousel.append( $rightChevron );

        // allow all events to propagate to map except 'click'
        map.enableEventToMapPropagation( $leftChevron );
        map.enableEventToMapPropagation( $rightChevron );
        map.disableEventToMapPropagation( $leftChevron, ['onclick', 'ondblclick'] );
        map.disableEventToMapPropagation( $rightChevron, ['onclick', 'ondblclick'] );

        controlMapping.leftChevron = $leftChevron;
        controlMapping.rightChevron = $rightChevron;

    };


    createIndexDots = function( $carousel, map, controlMapping, layerStates ) {

        var indexClass,
            $indexContainer,
            $dots = [],
            i;

        function generateCallbacks( dot, index ) {

            dot.mouseout( function() { dot.off('click'); });
            dot.mousemove( function() { dot.off('click'); });
            dot.mousedown( function() {
                dot.click( function() {
                    return true;
                    /*
                    var tilekey = that.selectedTileInfo.tilekey;
                    that.clientState.setTileViewIndex( tilekey, index );
                    */
                });
            });
        }

        $indexContainer = $("<div class='"+DOT_CONTAINER_CLASS+"'></div>");
        $carousel.append( $indexContainer );

        // allow all events to propagate to map except 'click'
        map.enableEventToMapPropagation( $indexContainer );
        map.disableEventToMapPropagation( $indexContainer, ['onclick', 'ondblclick'] );

        for (i=0; i < layerStates.length; i++) {

            indexClass = (i === 0) ? DOT_CLASS_SELECTED : DOT_CLASS_DEFAULT;
            $dots[i] = $("<div id='" + DOT_ID_PREFIX +i+"' class='" + DOT_CLASS + " " +indexClass+"' value='"+i+"'></div>");
            generateCallbacks( $dots[i], i );
            $indexContainer.append( $dots[i] );
        }

        controlMapping.dots = $dots;
    };


    createCarousel = function( map, controlMapping, layerStates ) {

        var $carousel = $('<div class="' +CAROUSEL_CLASS +'" style="z-index:'+Z_INDEX+';"></div>');

        if ( layerStates.length > 1 ) {
            // only create chevrons and indices if there is more than 1 layer
            createChevrons( $carousel, map, controlMapping );
            createIndexDots( $carousel, map, controlMapping, layerStates );
        }
        map.getRootElement().append( $carousel );
        return $carousel;
    };


    CarouselControls = Class.extend({


        init: function ( layerStates, map ) {

            var previousMouse = {},
                i;

            this.controlMap = {};
            this.$carousel = createCarousel( map, this.controlMap, layerStates );

            for (i=0; i<layerStates.length; i++) {
                layerStates[i].addListener( makeLayerStateObserver( map, this.$carousel, layerStates[i] ));
            }

            // set tile focus callbacks
            map.on('mousemove', function(event) {
                updateTileFocus( event.xy.x, event.xy.y, map, layerStates );
                previousMouse.x = event.xy.x;
                previousMouse.y = event.xy.y;
            });
            map.on('zoomend', function(event) {
                updateTileFocus( previousMouse.x, previousMouse.y, map, layerStates );
            });

        },

        noop: function() {
            return true;
        }


        /*
        changeViewIndex: function(tilekey, prevIndex, newIndex) {

            if (prevIndex === newIndex) {
                return;
            }

            this.$dots[prevIndex].removeClass(this.DOT_CLASS_SELECTED).addClass(this.DOT_CLASS_DEFAULT);
            this.$dots[newIndex].removeClass(this.DOT_CLASS_DEFAULT).addClass(this.DOT_CLASS_SELECTED);

            this.onTileViewChange(tilekey, newIndex);
        },



        updateSelectedTile: function( x, y ) {

            // get tilekey under mouse
            var tilekey = this.map.getTileKeyFromViewportPixel( x, y );
            // if only one view, or no views, abort
            if (this.views === undefined || this.views.length === 0) {
                return;
            }
            // set selected info
            this.selectedTileInfo = {
                previouskey : this.selectedTileInfo.tilekey,
                tilekey : tilekey
            };
            //this.clientState.activeCarouselTile = tilekey
            // update ui
            this.redrawUI();
        },


        redrawUI: function() {

            var tilekey = this.selectedTileInfo.tilekey,
                previousTilekey = this.selectedTileInfo.previouskey,
                topLeft,
                prevActiveView,
                activeViewForTile;

            // abort if not over new tile
            if ( previousTilekey === tilekey ) {
                return;
            }

            topLeft = this.map.getTopLeftMapPixelForTile( tilekey );
            prevActiveView = this.getTileViewIndex( previousTilekey );
            activeViewForTile = this.getTileViewIndex( tilekey );

            // re-position carousel tile
            this.$panel.css({
                left: topLeft.x,
                top: this.map.getMapHeight() - topLeft.y
            });

            // if more than 1 view update dots
            if ( this.views.length > 1 ) {
                // if active view is different we need to update the dots
                this.$dots[prevActiveView].removeClass(this.DOT_CLASS_SELECTED).addClass(this.DOT_CLASS_DEFAULT);
                this.$dots[activeViewForTile].removeClass(this.DOT_CLASS_DEFAULT).addClass(this.DOT_CLASS_SELECTED);
            }
        }
        */

     });

    return CarouselControls;
});
