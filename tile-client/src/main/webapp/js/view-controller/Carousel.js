/**
 * Copyright (c) 2013 Oculus Info Inc.
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



    var WebPyramid = require('../client-rendering/WebTilePyramid'),
        ViewController = require('./ViewController'),
        webPyramid,
        Carousel;



    webPyramid = new WebPyramid();



    Carousel = ViewController.extend({

        /**
         * Construct a carousel
         */
        init: function (spec) {

            var that = this;

            // call base class ViewController constructor
            this._super(spec);
            this.previousMouse = {};

            // add mouse move and zoom callbacks
            this.map.olMap_.events.register('mousemove', this.map.olMap_, function(event) {
                var tilekey = that.mapMouseToTileKey(event.xy.x, event.xy.y);
                that.updateSelectedTile(tilekey);
                that.previousMouse.x = event.xy.x;
                that.previousMouse.y = event.xy.y;
            });

            this.map.olMap_.events.register('zoomend', this.map.olMap_, function(event) {
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

            var positionFactor = (this.views.length - 1) / 2,
                i, j;

            this.nodeLayer = this.map.addLayer(aperture.geo.MapNodeLayer);
            this.nodeLayer.map('longitude').from('longitude');
            this.nodeLayer.map('latitude').from('latitude');

            // tile outline layer
            this.createTileOutlineLayer();
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
                icon = (position === 'left') ? "./img/chevron_L.png" : "./img/chevron_R.png",
                x = (position === 'left') ? -0.22 : 0.22,
                y = 0,
                hover = new aperture.Set('tilekey'), // separate tiles by tile key for hovering
                viewSelectionLayer;

            viewSelectionLayer = this.nodeLayer.addLayer(aperture.IconLayer);

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

            viewSelectionLayer.on('mouseout', function(event) {
                hover.clear();
            });

            viewSelectionLayer.on('mouseup', function(event) {

                var tilekey = event.data.tilekey,
                    button = event.source.button,
                    mod = function (m, n) {
                        return ((m % n) + n) % n;
                    },
                    inc = (position === 'left') ? -1 : 1,
                    oldIndex = that.getTileViewIndex(tilekey),
                    newIndex = mod(oldIndex + inc, that.views.length);

                if (button !== 0) {
                    // not left click, abort
                    return;
                }

                that.onTileViewChange(tilekey, newIndex);

                that.indexButtons[oldIndex].all().redraw();
                that.indexButtons[newIndex].all().redraw();
            });

            return viewSelectionLayer;
        },


        /**
         * Construct aperture.iconlayers for the index dots representing each view
         */
        createViewIndexLayer: function(index, spacingFactor) {

            var that = this,
                selectIcon = "./img/no_select.png",
                noSelectIcon = "./img/select.png",
                spacing = 0.04,
                hover = new aperture.Set('tilekey'), // separate tiles by bin key for hovering
                viewIndexLayer;

            viewIndexLayer = this.nodeLayer.addLayer(aperture.IconLayer);

            viewIndexLayer.map('width').asValue(12).filter(hover.scale(1.4));
            viewIndexLayer.map('height').asValue(12).filter(hover.scale(1.4));
            viewIndexLayer.map('anchor-x').asValue(0.5);
            viewIndexLayer.map('anchor-y').asValue(0.5);

            viewIndexLayer.map('x').from(function() {
                return spacingFactor*spacing/Math.pow(2, that.map.getZoom()-1);
            });
            viewIndexLayer.map('y').from(function(){
                return 0.20/Math.pow(2, that.map.getZoom()-1);
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

            viewIndexLayer.on('mouseout', function(event) {
                hover.clear();
            });

            viewIndexLayer.on('click', function(event) {

                var tilekey = event.data.tilekey,
                    button = event.source.button,
                    oldIndex = that.getTileViewIndex(tilekey);

                if (button !== 0) {
                    // not left click, abort
                    return;
                }

                that.onTileViewChange(tilekey, index);
                that.indexButtons[oldIndex].all().redraw();
                that.indexButtons[index].all().redraw();
            });

            return viewIndexLayer;
        },


        /**
         * Construct aperture.iconlayers for the tile outline during mouse hover
         */
        createTileOutlineLayer: function() {

            var icon = "./img/tileoutline.png",
                outlineLayer;

            outlineLayer = this.nodeLayer.addLayer(aperture.IconLayer);

            outlineLayer.map('width').asValue(256);
            outlineLayer.map('height').asValue(256);
            outlineLayer.map('anchor-x').asValue(0.5);
            outlineLayer.map('anchor-y').asValue(0.5);

            outlineLayer.map('x').asValue(0);
            outlineLayer.map('y').asValue(0);

            outlineLayer.map('url').asValue(icon);

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

           var parsedValues = tilekey.split(','),
               tile = {
                    level:  parseInt(parsedValues[0], 10),
                    xIndex: parseInt(parsedValues[1], 10),
                    yIndex: parseInt(parsedValues[2], 10),
                    xBinCount: 1,
                    yBinCount: 1
                },
                position = webPyramid.getTileBounds(tile);

           this.selectedTileInfo = {
                type: 'carousel-interface',
                longitude: position.centerX,
                latitude: position.centerY,
                visible: true,
                tilekey : tilekey
            };
            this.nodeLayer.all(this.selectedTileInfo).where('type','carousel-interface').redraw();
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
