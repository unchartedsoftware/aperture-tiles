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
         * @param spec Carousel specification object:
         */


        init: function () {
            this._super();
            this.leftButton = null;
            this.rightButton = null;
            this.indexButtons = [];
        },

        attachToMap: function( map ){

            var that = this;

            if (this.map) {
                return; // can only attach a single map to carousel
            }

            // call base function
            this._super(map);

            // add ui elements
            this.map.olMap_.events.register('mousemove', this.map.olMap_, function(event) {
                var tilekey = that.mapMouseToTileKey(event.xy.x, event.xy.y);
                that.updateSelectedTile(tilekey);
            });

            this.nodeLayer = this.map.addLayer(aperture.geo.MapNodeLayer);
            this.nodeLayer.map('longitude').from('longitude');
            this.nodeLayer.map('latitude').from('latitude');

            // if views already added, re-create ui for new map node layer
            if (this.views.length > 0) {
                this.createUI();
            }
        },

        removeUI: function() {

            if (this.map) {
                var i;
                if (this.leftButton) {
                    this.leftButton.remove();
                    this.leftButton = null;
                }
                if (this.rightButton) {
                    this.rightButton.remove();
                    this.rightButton = null;
                }
                for (i = 0; i < this.indexButtons.length; i++) {
                    this.indexButtons[i].remove();
                }
                this.indexButtons = [];
            }
        },

        addView: function( id, layerSpec, clientRenderer ) {
            this._super( id, layerSpec, clientRenderer );

            // if map is attached, recreate the ui layers
            if (this.map) {
                this.removeUI();
                this.createUI();
            }
        },

        createUI: function() {

            var positionFactor = (this.views.length - 1) / 2,
                i, j;

            // tile outline layer
            this.createTileOutlineLayer();
            // left and right view buttons
            this.leftButton = this.createViewSelectionLayer('left');
            this.rightButton = this.createViewSelectionLayer('right');
            // index dots
            this.indexButtons = [];
            for (i=0, j=-positionFactor; j<=positionFactor; i++, j++) {
                this.indexButtons.push( this.createViewIndexLayer(i, j) );
            }
        },

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

            viewSelectionLayer.on('click', function(event) {

                var tilekey = event.data.tilekey,
                    mod = function (m, n) {
                        return ((m % n) + n) % n;
                    },
                    inc = (position === 'left') ? -1 : 1,
                    oldIndex = that.getTileViewIndex(tilekey),
                    newIndex = mod(oldIndex + inc, that.views.length);

                that.onTileViewChange(tilekey, newIndex);

                that.indexButtons[oldIndex].all().redraw();
                that.indexButtons[newIndex].all().redraw();
            });

            return viewSelectionLayer;
        },

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
                    oldIndex = that.getTileViewIndex(tilekey);

                that.onTileViewChange(tilekey, index);
                that.indexButtons[oldIndex].all().redraw();
                that.indexButtons[index].all().redraw();
            });

            return viewIndexLayer;
        },


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
        },


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
                longitude: position.centerX,
                latitude: position.centerY,
                visible: true,
                tilekey : tilekey
            };
            this.nodeLayer.all(this.selectedTileInfo).redraw();
        },

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
