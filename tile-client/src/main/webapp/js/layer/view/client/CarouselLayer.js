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

        /**
         * Construct a carousel
         */
        init: function (id, map) {

            var that = this;

            // constants
            this.OUTLINE_CLASS = 'carousel-ui-outline';
            this.DOT_CONTAINER_CLASS = "carousel-ui-dot-container";
            this.DOT_CLASS = 'carousel-ui-dot';
            this.DOT_CLASS_DEFAULT = 'carousel-ui-dot-default';
            this.DOT_CLASS_SELECTED = 'carousel-ui-dot-selected';
            this.DOT_ID_PREFIX = 'carousel-ui-id-';
            this.CHEVRON_CLASS = "carousel-ui-chevron";
            this.CHEVRON_CLASS_LEFT = "carousel-ui-chevron-left";
            this.CHEVRON_CLASS_RIGHT = "carousel-ui-chevron-right";

            // call base class ClientLayer constructor
            this._super(id, map);
            this.previousMouse = {};
            this.selectedTileInfo = {};

            // add mouse move and zoom callbacks
            this.map.on('mousemove', function(event) {
                var tilekey = that.map.getTileKeyFromViewportPixel(event.xy.x, event.xy.y);
                that.updateSelectedTile(tilekey);
                that.previousMouse.x = event.xy.x;
                that.previousMouse.y = event.xy.y;
            });

            // update carousel if map is moving and mouse isn't
            this.map.on('move', function(event) {
                var tilekey = that.map.getTileKeyFromViewportPixel( that.previousMouse.x, that.previousMouse.y );
                that.updateSelectedTile(tilekey);
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
            this.createTileOutline();

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

            var that = this,
                incIndex = function(inc) {
                    var tilekey = that.selectedTileInfo.tilekey,
                        prevIndex = that.getTileViewIndex(tilekey),
                        mod = function (m, n) {
                            return ((m % n) + n) % n;
                        },
                        newIndex = mod(prevIndex + inc, that.views.length);
                    that.changeViewIndex(tilekey, prevIndex, newIndex);
                };

            this.$leftChevron = $("<div class='"+this.CHEVRON_CLASS+" "+this.CHEVRON_CLASS_LEFT+"'></div>");
            this.$leftChevron.click( function() { incIndex(-1); return true; });
            this.$outline.append(this.$leftChevron);

            this.$rightChevron = $("<div class='"+this.CHEVRON_CLASS+" "+this.CHEVRON_CLASS_RIGHT+"'></div>");
            this.$rightChevron.click( function() { incIndex(1); return true; });
            this.$outline.append(this.$rightChevron);
        },


        /**
         * Construct the elements for the index dots representing each view
         */
        createIndexDots: function() {

            var that = this,
                indexClass,
                $indexContainer,
                i;

            function generateDotCallback(index) {
                return function(){
                    var tilekey = that.selectedTileInfo.tilekey,
                        prevIndex = that.getTileViewIndex(tilekey);
                    that.changeViewIndex(tilekey, prevIndex, index);
                    return true;
                };
            }

            $indexContainer = $("<div class='"+this.DOT_CONTAINER_CLASS+"'></div>");
            this.$outline.append($indexContainer);

            this.$dots = [];

            for (i=0; i < this.views.length; i++) {

                indexClass = (this.defaultViewIndex === i) ? this.DOT_CLASS_SELECTED : this.DOT_CLASS_DEFAULT;

                this.$dots[i] = $("<div id='" + this.DOT_ID_PREFIX +i+"' class='" + this.DOT_CLASS + " " +indexClass+"' value='"+i+"'></div>");
                this.$dots[i].click( generateDotCallback(i) );
                $indexContainer.append(this.$dots[i] );
            }
            $indexContainer.css('left', this.map.getTileSize()/2 - $indexContainer.width()/2 );
        },


        /**
         * Construct the tile outline to indicate the current tile selected
         */
        createTileOutline: function() {

            this.$outline = $("<div class='" +this.OUTLINE_CLASS +"'></div>");
            this.map.getElement().append(this.$outline);
        },


        /**
         * Updates selected tile nodeLayer data and redraws UI
         * @param tilekey tile identification key of the form: "level,x,y"
         */
        updateSelectedTile: function(tilekey) {

            if (this.views === undefined || this.views.length === 0) {
                return;
            }
            this.selectedTileInfo = {
                previouskey : this.selectedTileInfo.tilekey,
                tilekey : tilekey
            };
            this.clientState.setSharedState('activeCarouselTile', tilekey);
            this.redrawUI();
        },


        /**
         *	Redraws the carousel specific layers
         */
        redrawUI: function() {

            var that = this,
                tilekey = this.selectedTileInfo.tilekey,
                parsedValues = tilekey.split(','),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10),
                topLeft = this.map.getTopLeftViewportPixelForTile(xIndex, yIndex),
                prevActiveView = this.getTileViewIndex(this.selectedTileInfo.previouskey),
                activeViewForTile = this.getTileViewIndex(tilekey);

            function currentOrPreviousTilekey() {
                // only redraw previous tile, and current tile
                return this.tilekey === that.selectedTileInfo.previouskey ||
                       this.tilekey === that.selectedTileInfo.tilekey;
            }

            // move carousel tile
            this.$outline.css('left', topLeft.x);
            this.$outline.css('top', topLeft.y);

            if (this.clientState.areDetailsOverTile(xIndex, yIndex)) {
                this.$outline.css('visibility', 'hidden');
            } else {
                this.$outline.css('visibility', 'visible');
            }

            // if over new tile
            if ( this.selectedTileInfo.previouskey !== this.selectedTileInfo.tilekey ) {

                // if more than 1 view and different dot display is needed
                if (this.views.length > 1 && prevActiveView !== activeViewForTile ) {
                    // if active view is different we need to update the dots
                    this.$dots[prevActiveView].removeClass(this.DOT_CLASS_SELECTED).addClass(this.DOT_CLASS_DEFAULT);
                    this.$dots[activeViewForTile].removeClass(this.DOT_CLASS_DEFAULT).addClass(this.DOT_CLASS_SELECTED);
                }

                // only redraw if hovering over a new tile
                this.mapNodeLayer.all().where(currentOrPreviousTilekey).redraw();
            }
        }



     });

    return CarouselLayer;
});
