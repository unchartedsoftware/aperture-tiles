/**
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
 * This module defines a Carousel class which inherits from a ViewController and provides a user
 * interface and event handler for switching between views for individual tiles
 */
define(function (require) {
    "use strict";



    var ViewController = require('./ViewController'),
        Carousel;



    Carousel = ViewController.extend({

        /**
         * Construct a carousel
         */
        init: function (spec) {

            var that = this;

            // call base class ViewController constructor
            this._super(spec);
            this.previousMouse = {};
            this.selectedTileInfo = {};

            // add mouse move and zoom callbacks
            this.map.olMap_.events.register('mousemove', this.map.olMap_, function(event) {
                var tilekey = that.mapMouseToTileKey(event.xy.x, event.xy.y);
                that.updateSelectedTile(tilekey);
                that.previousMouse.x = event.xy.x;
                that.previousMouse.y = event.xy.y;
            });

            this.map.olMap_.events.register('zoomend', this.map.olMap_, function() {
                var tilekey = that.mapMouseToTileKey(that.previousMouse.x, that.previousMouse.y);
                that.updateSelectedTile(tilekey);
            });

            // create the carousel UI
            this.createUI();
        },


        /**
         * Construct the user interface aperture.iconlayers
         */
        createUI: function() {

            var that = this,
                positionFactor = (this.views.length - 1) / 2,
                i, j;

            this.plotLayer = this.mapNodeLayer; //.addLayer(aperture.PlotLayer);

            /*
            this.plotLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });
*/
            // tile outline layer
            //this.outline = this.createTileOutlineLayer();
            // left and right view buttons
            this.leftButton = this.createViewSelectionLayer('left');
            this.rightButton = this.createViewSelectionLayer('right');
            // index dots
            this.indexButtons = [];
            for (i=0, j=-positionFactor; j<=positionFactor; i++, j++) {
                this.indexButtons.push( this.createViewIndexLayer(i, j));
            }
        },


        /**
         * Construct aperture.iconlayers for the left/right view panning buttons
         */
        createViewSelectionLayer: function(position) {

            var that = this,
                icon = (position === 'left') ? "./images/chevron_L.png" : "./images/chevron_R.png",
                x = (position === 'left') ? -0.22 : 0.22,
                y = 0,
                hover = new aperture.Set('tilekey'), // separate tiles by tile key for hovering
                viewSelectionLayer;

            viewSelectionLayer = this.plotLayer.addLayer(aperture.IconLayer);

            viewSelectionLayer.map('width').asValue(15).filter(hover.scale(1.2));
            viewSelectionLayer.map('height').asValue(42).filter(hover.scale(1.5));
            viewSelectionLayer.map('anchor-x').asValue(0.5);
            viewSelectionLayer.map('anchor-y').asValue(0.5);
            viewSelectionLayer.map('url').asValue(icon);

            viewSelectionLayer.map('x').from(function(){
                return x/Math.pow(2, that.map.getZoom()-1);
            });
            viewSelectionLayer.map('y').from(function(){
                return y/Math.pow(2, that.map.getZoom()-1);
            });

            viewSelectionLayer.on('mouseover', function(event) {
                hover.add(event.data.tilekey);
            });

            viewSelectionLayer.on('mouseout', function() {
                hover.clear();
            });

            viewSelectionLayer.on('mouseup', function(event) {

                var tilekey = event.data.tilekey,
                    mod = function (m, n) {
                        return ((m % n) + n) % n;
                    },
                    inc = (position === 'left') ? -1 : 1,
                    oldIndex = that.getTileViewIndex(tilekey),
                    newIndex = mod(oldIndex + inc, that.views.length);

                if (event.source.button !== 0) {
                    // not left click, abort
                    return;
                }

                that.onTileViewChange(tilekey, newIndex);
            });

            viewSelectionLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });

            return viewSelectionLayer;
        },


        /**
         * Construct aperture.iconlayers for the index dots representing each view
         */
        createViewIndexLayer: function(index, spacingFactor) {

            var that = this,
                selectIcon = "./images/no_select.png",
                noSelectIcon = "./images/select.png",
                spacing = 0.04,
                hover = new aperture.Set('tilekey'), // separate tiles by bin key for hovering
                viewIndexLayer;

            viewIndexLayer = this.plotLayer.addLayer(aperture.IconLayer);

            viewIndexLayer.map('width').asValue(12).filter(hover.scale(1.4));
            viewIndexLayer.map('height').asValue(12).filter(hover.scale(1.4));
            viewIndexLayer.map('anchor-x').asValue(0.5);
            viewIndexLayer.map('anchor-y').asValue(0.5);

            viewIndexLayer.map('x').from(function() {
                return spacingFactor*spacing/Math.pow(2, that.map.getZoom()-1);
            });
            viewIndexLayer.map('y').from(function(){
                return 0.2/Math.pow(2, that.map.getZoom()-1);
            });

            viewIndexLayer.map('url').from(function() {
                var id = that.getTileViewIndex( this.tilekey),
                    url;
                if ( id !== index ) {
                    url = selectIcon;
                } else {
                    url = noSelectIcon;
                }
                return url;
            });

            viewIndexLayer.on('mouseover', function(event) {
                hover.add(event.data.tilekey);
            });

            viewIndexLayer.on('mouseout', function() {
                hover.clear();
            });

            viewIndexLayer.on('mouseup', function(event) {
                if (event.source.button !== 0) {
                    // not left click, abort
                    return;
                }
                that.onTileViewChange(event.data.tilekey, index);
            });

            viewIndexLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });

            return viewIndexLayer;
        },


        /**
         * Construct aperture.iconlayers for the tile outline during mouse hover
         */
        createTileOutlineLayer: function() {

            var that = this,
                icon = "./images/tileoutline.png",
                outlineLayer;

            outlineLayer = this.plotLayer.addLayer(aperture.IconLayer);

            outlineLayer.map('width').asValue(256);
            outlineLayer.map('height').asValue(256);
            outlineLayer.map('anchor-x').asValue(0.5);
            outlineLayer.map('anchor-y').asValue(0.5);

            outlineLayer.map('x').asValue(0);
            outlineLayer.map('y').asValue(0);

            outlineLayer.map('url').asValue(icon);

            outlineLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });

            return outlineLayer;
        },


        /**
         * Maps a mouse position in the mouse viewport to a tile identification key
         * @param mx mouse x position in the map viewport
         * @param my mouse y position in the map viewport
        */
        mapMouseToTileKey: function(mx, my) {

            var TILESIZE = 256,
                zoom,
                maxPx = {},
                minPx = {},
                totalTilespan,
                totalPixelSpan = {},
                pixelMax = {},
                pixelMin = {},
                pixel = {};

                zoom = this.map.olMap_.getZoom();
                maxPx.x = this.map.olMap_.maxPx.x;
                maxPx.y = this.map.olMap_.maxPx.y;
                minPx.x = this.map.olMap_.minPx.x;
                minPx.y = this.map.olMap_.minPx.y;
                totalTilespan = Math.pow(2, zoom);
                totalPixelSpan.x = TILESIZE * totalTilespan;
                totalPixelSpan.y = this.map.olMap_.viewPortDiv.clientHeight;
                pixelMax.x = totalPixelSpan.x - minPx.x;
                pixelMax.y = totalPixelSpan.y - minPx.y;
                pixelMin.x = totalPixelSpan.x - maxPx.x;
                pixelMin.y = totalPixelSpan.x - maxPx.y;
                pixel.x = mx + pixelMin.x;
                pixel.y = (this.map.olMap_.size.h - my - pixelMax.y + totalPixelSpan.x );

            return zoom + "," + Math.floor(pixel.x / TILESIZE) + "," + Math.floor(pixel.y / TILESIZE);
        },


        /**
         * Updates selected tile nodeLayer data and redraws UI
         * @param tilekey tile identification key
         */
        updateSelectedTile: function(tilekey) {

           var i;

           this.selectedTileInfo = {
                previouskey : this.selectedTileInfo.tilekey,
                tilekey : tilekey
            };

            //console.log(this.selectedTileInfo.tilekey);


            //this.outline.all().redraw();
            this.leftButton.all().redraw();
            this.rightButton.all().redraw();
            for (i=0; i<this.indexButtons.length; i++) {
                this.indexButtons[i].all().redraw();
            }
            //this.plotLayer.all().redraw();

            //if (this.selectedTileInfo.previouskey !== this.selectedTileInfo.tilekey) {
                // only redraw if a new tile is highlighted
                //this.plotLayer.all().redraw();
                /*
                this.outline.all().redraw();
                this.leftButton.all().redraw();
                this.rightButton.all().redraw();
                for (i=0; i<this.indexButtons.length; i++) {
                    this.indexButtons[i].all().redraw();
                }
                */
            //}

        },


        /**
         * Maps a tilekey to its current view index. If none is specified, use default
         * @param tilekey tile identification key
         */
        getTileViewIndex: function(tilekey) {
            // given a tile key "level + "," + xIndex + "," + yIndex"
            // return the view index
            var viewIndex;
            if ( this.tileViewMap[tilekey] === undefined ) {
                viewIndex = this.defaultViewIndex ;
            } else {
                viewIndex = this.tileViewMap[tilekey];
            }
            return viewIndex;
        }

     });

    return Carousel;
});
