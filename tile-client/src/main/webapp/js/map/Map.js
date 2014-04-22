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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



define(function (require) {
    "use strict";


	
    var Class = require('../class'),
        //AoITilePyramid = require('../binning/AoITilePyramid'),
        WebPyramid = require('../binning/WebTilePyramid'),
        TileIterator = require('../binning/TileIterator'),
		Axis =  require('./Axis'),
        TILESIZE = 256,
        Map;



    Map = Class.extend({
        ClassName: "Map",
		
        init: function (id, spec) {

			var that = this,
				mapSpecs;
		
			mapSpecs = spec.MapConfig;

	        aperture.config.provide({
		        // Set the map configuration
		        'aperture.map' : {
			        'defaultMapConfig' : mapSpecs
		        }
	        });
		        
			
            // Map div id
			this.id = id;

            // Initialize the map
            this.map = new aperture.geo.Map({ 
				id: this.id
            });
            this.map.olMap_.baseLayer.setOpacity(1);
            this.map.all().redraw();

			// Create axes
			this.axes = [];

			this.projection = this.map.olMap_.projection;

            this.pyramid = new WebPyramid();

			// Set resize map callback
			$(window).resize( function() {
				var $map = $('#' + that.id),
					$mapContainer = $map.parent(),
					offset = $map.offset(),
					leftOffset = offset.left || 0,
					topOffset = offset.top || 0,
					vertical_buffer = parseInt($mapContainer.css("marginBottom"), 10) + topOffset + 24,
					horizontal_buffer = parseInt($mapContainer.css("marginRight"), 10) + leftOffset + 24,			
					width = $(window).width(),
					height = $(window).height(),				
					newHeight,
					newWidth;

				newWidth = (width - horizontal_buffer);
				newHeight = (height - vertical_buffer);
					
				$map.width(newWidth);
				$map.height(newHeight);
				that.map.olMap_.updateSize();
			});
												
			// Trigger the initial resize event to resize everything
            $(window).resize();			
        },

    /*
        addAxis: function(axisSpec) {

            axisSpec.parentId = this.id;
            axisSpec.olMap = this.map.olMap_;
            this.axes.push( new Axis(axisSpec) );
            $(window).resize();
        },
    */

        setAxisSpecs: function (axes) {

            var i, spec;

            for (i=0; i< axes.length; i++) {
                spec = axes[i];
                spec.parentId = this.id;
                spec.olMap = this.map.olMap_;
                this.axes.push(new Axis(spec));
            }
        },


        getPyramid: function() {

            return this.pyramid;
        },


        getTileIterator: function() {
            var level = this.map.getZoom(),
                bounds = this.map.olMap_.getExtent();

            // determine all tiles in view
            return new TileIterator( this.getPyramid(), level,
                                     bounds.left, bounds.bottom,
                                     bounds.right, bounds.top);
        },


        getTilesInView: function() {

            return this.getTileIterator().getRest();
        },


        getTileSetBoundsInView: function() {

            return {'params': this.getTileIterator().toTileBounds()};
        },


        getPixelUnderMouse: function(mx, my) {

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

            return pixel;
        },


        getTileAndBinUnderMouse: function(mx, my, xBinCount, yBinCount) {

            var zoom = this.map.olMap_.getZoom(),
                pixel = this.getPixelUnderMouse(mx, my),
                tileIndexX = Math.floor(pixel.x / TILESIZE),
                tileIndexY = Math.floor(pixel.y / TILESIZE),
                tilePixelX = pixel.x % TILESIZE,
                tilePixelY = pixel.y % TILESIZE;

            return {
                tile: {
                    level : zoom,
                    xIndex : tileIndexX,
                    yIndex : tileIndexY,
                    xBinCount : xBinCount,
                    yBinCount : yBinCount
                },
                bin: {
                    x : Math.floor( tilePixelX / (TILESIZE / xBinCount) ),
                    y : (yBinCount - 1) - Math.floor( tilePixelY / (TILESIZE / yBinCount) ) // bin [0,0] is top left
                }
            };

        },


        getTileKeyUnderMouse: function(mx, my) {

            var tileAndBin = this.getTileAndBinUnderMouse( mx, my, 1, 1);

            return tileAndBin.tile.level + "," + tileAndBin.tile.xIndex + "," + tileAndBin.tile.xIndex;
        },


        getBinKeyUnderMouse: function(mx, my, xBinCount, yBinCount) {

            var tileAndBin = this.getTileAndBinUnderMouse( mx, my, xBinCount, yBinCount );

            return + tileAndBin.bin.x + "," + tileAndBin.bin.x;
        },


        getCoordUnderMouse: function(mx, my) {

            var tileAndBin = this.getTileAndBinUnderMouse( mx, my, TILESIZE, TILESIZE),
                bounds = this.pyramid.getBinBounds( tileAndBin.tile, tileAndBin.bin );

            return {
                x: bounds.centerX,
                y: bounds.centerY
            };
        },

        getOLMap: function() {
            return this.map.olMap_;
        },

        getApertureMap: function() {
            return this.map;
        },

        addApertureLayer: function(layer, mappings, spec) {
            return this.map.addLayer(layer, mappings, spec);
        },

        addOLLayer: function(layer) {
            return this.map.olMap_.addLayer(layer);
        },

        addOLControl: function(control) {
            return this.map.olMap_.addControl(control);
        },

        getUid: function() {
            return this.map.uid;
        },

        setLayerIndex: function(layer, zIndex) {
            this.map.olMap_.setLayerIndex(layer, zIndex);
        },

        getLayerIndex: function(layer) {
            return this.map.olMap_.getLayerIndex(layer);
        },

        setOpacity: function (newOpacity) {
            this.map.olMap_.baseLayer.setOpacity(newOpacity);
        },

        getOpacity: function () {
            return this.map.olMap_.baseLayer.opacity;
        },

        setVisibility: function (visibility) {
            this.map.olMap_.baseLayer.setVisibility(visibility);
        },

        getExtent: function () {
            return this.map.olMap_.getExtent();
        },

        getZoom: function () {
            return this.map.olMap_.getZoom();
        },

        isEnabled: function () {
            return this.map.olMap_.baseLayer.getVisibility();
        },

        setEnabled: function (enabled) {
            this.map.olMap_.baseLayer.setVisibility(enabled);
        },

        zoomToExtent: function (extent, findClosestZoomLvl) {
            this.map.olMap_.zoomToExtent(extent, findClosestZoomLvl);
        },

        on: function (eventType, callback) {

            switch (eventType) {

                case 'click':
                case 'zoomend':
                case 'mousemove':

                    this.map.olMap_.events.register(eventType, this.map.olMap_, callback);
                    break;

                default:

                    this.map.on(eventType, callback);
                    break;
            }

        },

        off: function(eventType, callback) {

            switch (eventType) {

                case 'click':
                case 'zoomend':
                case 'mousemove':

                    this.map.olMap_.events.unregister(eventType, this.map.olMap_, callback);
                    break;

                default:

                    this.map.off(eventType, callback);
                    break;
            }
        },

        trigger: function(eventType, event) {

            switch (eventType) {

                case 'click':
                case 'zoomend':
                case 'mousemove':

                    this.map.olMap_.events.triggerEvent(eventType, event);
                    break;

                default:

                    this.map.trigger(eventType, event);
                    break;
            }
        }

    });

    return Map;
});
