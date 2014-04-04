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
        AoITilePyramid = require('../binning/AoITilePyramid'),
        TileIterator = require('../binning/TileIterator'),
		Axis =  require('./Axis'),
        Map;



    Map = Class.extend({
        ClassName: "Map",
		
        init: function (id, spec) {

			var that = this,
				apertureConfig,
				mapSpecs;//,
				//axisSpecs,
				//axisSpec;
		
			apertureConfig = spec.ApertureConfig;
			mapSpecs = spec.MapConfig;
			//axisSpecs = spec.AxisConfig;
			
			// configure aperture
            aperture.config.provide({
            	/*
                 * Set aperture log configuration
                 */
                'aperture.log' : apertureConfig['aperture.log'],
                
                /*
                 * The endpoint locations for Aperture services accessed through the io interface
                 */
                'aperture.io' : apertureConfig['aperture.io'],

                /*
                 * Set the map configuration
                 */
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

            /*
			// create x axis
			axisSpec = axisSpecs.XAxisConfig;
			axisSpec.parentId = this.id;
			axisSpec.olMap = this.map.olMap_;
            axisSpec.projection = this.map.olMap_.projection.projCode;
			this.axes.push(new Axis(axisSpec));
			
			// create y axis
			axisSpec = axisSpecs.YAxisConfig;
			axisSpec.parentId = this.id;
			axisSpec.olMap = this.map.olMap_;
            axisSpec.projection = this.map.olMap_.projection.projCode;
			this.axes.push(new Axis(axisSpec));
			*/

			// Set resize map callback
			$(window).resize( function() {
				var ASPECT_RATIO = 1.61803398875, // golden ratio
					$map = $('#' + that.id),
					$mapContainer = $map.parent(),
					offset = $map.offset(),
					leftOffset = offset.left || 0,
					topOffset = offset.top || 0,
					vertical_buffer = parseInt($mapContainer.css("marginBottom"), 10) + topOffset + 24,
					horizontal_buffer = parseInt($mapContainer.css("marginRight"), 10) + leftOffset,			
					width = $(window).width(),
					height = $(window).height(),				
					newHeight,
					newWidth;

				if ((width-horizontal_buffer / ASPECT_RATIO) < height) {
					// window height supports width
					newWidth = width - horizontal_buffer;
					newHeight = (width - horizontal_buffer) / ASPECT_RATIO;
				} else {
					// windows height does not support width
					newWidth = (height - vertical_buffer) * ASPECT_RATIO;
					newHeight = height - vertical_buffer;
				}
					
				$map.width(newWidth);
				$map.height(newHeight);
				that.map.olMap_.updateSize();
			});
												
			// Trigger the initial resize event to resize everything
            $(window).resize();			
        },

        addAxis: function(axisSpec) {

            axisSpec.parentId = this.id;
            axisSpec.olMap = this.map.olMap_;
            this.axes.push( new Axis(axisSpec) );
            $(window).resize();
        },

        getTilesInView: function() {

            var level = this.map.getZoom(),
                bounds = this.map.olMap_.getExtent(),
                mapExtents = this.map.olMap_.getMaxExtent(),
                mapPyramid = new AoITilePyramid(mapExtents.left, mapExtents.bottom,
                                            mapExtents.right, mapExtents.top);

            // determine all tiles in view
            return new TileIterator(mapPyramid, level,
                                    bounds.left, bounds.bottom,
                                    bounds.right, bounds.top).getRest();
        },

        /**
         * Maps a mouse position in the mouse viewport to a tile identification key
         * @param mx mouse x position in the map viewport
         * @param my mouse y position in the map viewport
         * @return string tile identification key under the specified mouse position
         */
        getTileKeyUnderMouse: function(mx, my) {

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
