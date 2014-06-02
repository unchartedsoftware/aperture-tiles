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



    var ClientLayer = require('./ClientLayer'),
        CarouselLayer;



    CarouselLayer = ClientLayer.extend({

        Z_INDEX_OFFSET : 1000,

        /**
         * Construct a carousel
         */
        init: function (id, map) {

            var that = this;

            // call base class ClientLayer constructor
            this._super(id, map);

            // constants
            this.CAROUSEL_CLASS = 'carousel-ui-pane';
            this.DOT_CONTAINER_CLASS = "carousel-ui-dot-container";
            this.DOT_CLASS = 'carousel-ui-dot';
            this.DOT_CLASS_DEFAULT = 'carousel-ui-dot-default';
            this.DOT_CLASS_SELECTED = 'carousel-ui-dot-selected';
            this.DOT_ID_PREFIX = 'carousel-ui-id-';
            this.CHEVRON_CLASS = "carousel-ui-chevron";
            this.CHEVRON_CLASS_LEFT = "carousel-ui-chevron-left";
            this.CHEVRON_CLASS_RIGHT = "carousel-ui-chevron-right";

            this.Z_INDEX = this.map.getZIndex() + this.Z_INDEX_OFFSET;

            this.previousMouse = {};
            this.selectedTileInfo = {};

            // put mouse move callback on global document
            this.map.on('mousemove', function(event) {
                that.updateSelectedTile( event.xy.x, event.xy.y );
                that.previousMouse.x = event.xy.x;
                that.previousMouse.y = event.xy.y;
            });

            // update carousel on zoomend
            this.map.on('zoomend', function(event) {
                that.updateSelectedTile(  that.previousMouse.x, that.previousMouse.y );
            });

        },


        setViews: function( views ) {

            this._super(views);
            // create the carousel UI
            this.createUI();
        },


        /**
         * Construct the user interface html elements
         */
        createUI: function() {

            // tile outline layer
            this.createCarouselPanel();

            if (this.views.length > 1) {
                // only create carousel ui if there is more than 1 view
                // left and right view buttons
                this.createChevrons();
                // index dots
                this.createIndexDots();
            }
        },


        changeViewIndex: function(tilekey, prevIndex, newIndex) {

            if (prevIndex === newIndex) {
                return;
            }

            this.$dots[prevIndex].removeClass(this.DOT_CLASS_SELECTED).addClass(this.DOT_CLASS_DEFAULT);
            this.$dots[newIndex].removeClass(this.DOT_CLASS_DEFAULT).addClass(this.DOT_CLASS_SELECTED);

            this.onTileViewChange(tilekey, newIndex);
        },


        /**
         * Construct the elements for left/right view panning buttons
         */
        createChevrons: function() {

            var that = this;

            function generateCallbacks( chevron, inc ) {

                chevron.mouseout( function() { chevron.off('click'); });
                chevron.mousemove( function() { chevron.off('click'); });
                chevron.mousedown( function() {
                    chevron.click( function() {
                        var tilekey = that.selectedTileInfo.tilekey,
                            prevIndex = that.getTileViewIndex(tilekey),
                            mod = function (m, n) {
                                return ((m % n) + n) % n;
                            },
                            newIndex = mod(prevIndex + inc, that.views.length);
                      that.changeViewIndex(tilekey, prevIndex, newIndex);
                    });
                });
            }

            this.$leftChevron = $("<div class='"+this.CHEVRON_CLASS+" "+this.CHEVRON_CLASS_LEFT+"'></div>");
            generateCallbacks( this.$leftChevron, -1 );
            this.$panel.append(this.$leftChevron);

            this.$rightChevron = $("<div class='"+this.CHEVRON_CLASS+" "+this.CHEVRON_CLASS_RIGHT+"'></div>");
            generateCallbacks( this.$rightChevron, 1 );
            this.$panel.append(this.$rightChevron);

            // allow all events to propagate to map except 'click'
            this.map.enableEventToMapPropagation( this.$leftChevron );
            this.map.enableEventToMapPropagation( this.$rightChevron );
            this.map.disableEventToMapPropagation( this.$leftChevron, ['onclick'] );
            this.map.disableEventToMapPropagation( this.$rightChevron, ['onclick'] );

        },


        /**
         * Construct the elements for the index dots representing each view
         */
        createIndexDots: function() {

            var that = this,
                indexClass,
                $indexContainer,
                i;

            function generateCallbacks( dot, index ) {

                dot.mouseout( function() { dot.off('click'); });
                dot.mousemove( function() { dot.off('click'); });
                dot.mousedown( function() {
                    dot.click( function() {
                        var tilekey = that.selectedTileInfo.tilekey,
                            prevIndex = that.getTileViewIndex(tilekey);
                        that.changeViewIndex(tilekey, prevIndex, index);
                    });
                });
            }

            $indexContainer = $("<div class='"+this.DOT_CONTAINER_CLASS+"'></div>");
            this.$panel.append( $indexContainer );

            // allow all events to propagate to map except 'click'
            this.map.enableEventToMapPropagation( $indexContainer );
            this.map.disableEventToMapPropagation( $indexContainer, ['onclick'] );

            this.$dots = [];
            for (i=0; i < this.views.length; i++) {

                indexClass = (this.defaultViewIndex === i) ? this.DOT_CLASS_SELECTED : this.DOT_CLASS_DEFAULT;
                this.$dots[i] = $("<div id='" + this.DOT_ID_PREFIX +i+"' class='" + this.DOT_CLASS + " " +indexClass+"' value='"+i+"'></div>");
                generateCallbacks( this.$dots[i], i );
                $indexContainer.append( this.$dots[i] );
            }
            $indexContainer.css('left', this.map.getTileSize()/2 - $indexContainer.width()/2 );
        },


        /**
         * Construct the tile outline to indicate the current tile selected
         */
        createCarouselPanel: function() {

            this.$panel = $('<div class="' +this.CAROUSEL_CLASS +'" style="z-index:'+this.Z_INDEX+';"></div>');
            this.map.getRootElement().append(this.$panel);
        },


        /**
         * Updates selected tile nodeLayer data and redraws UI
         * @param tilekey tile identification key of the form: "level,x,y"
         */
        updateSelectedTile: function( x, y ) {

            if (this.views === undefined || this.views.length === 0) {
                return;
            }
            this.selectedTileInfo = {
                previouskey : this.selectedTileInfo.tilekey,
                tilekey : this.map.getTileKeyFromViewportPixel( x, y )
            };
            this.redrawUI();
        },


        /**
         *	Redraws the carousel specific layers
         */
        redrawUI: function() {

            var tilekey = this.selectedTileInfo.tilekey,
                parsedValues = tilekey.split(','),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10),
                topLeft = this.map.getTopLeftMapPixelForTile(xIndex, yIndex),
                prevActiveView = this.getTileViewIndex(this.selectedTileInfo.previouskey),
                activeViewForTile = this.getTileViewIndex(tilekey);

            // re-position carousel tile
            this.$panel.css({
                left: topLeft.x,
                top: this.map.getMapHeight() - topLeft.y
            });

            // if over new tile
            if ( this.selectedTileInfo.previouskey !== this.selectedTileInfo.tilekey ) {

                // if more than 1 view and different dot display is needed
                if (this.views.length > 1 && prevActiveView !== activeViewForTile ) {
                    // if active view is different we need to update the dots
                    this.$dots[prevActiveView].removeClass(this.DOT_CLASS_SELECTED).addClass(this.DOT_CLASS_DEFAULT);
                    this.$dots[activeViewForTile].removeClass(this.DOT_CLASS_DEFAULT).addClass(this.DOT_CLASS_SELECTED);
                }
            }
        }



     });

    return CarouselLayer;
});
