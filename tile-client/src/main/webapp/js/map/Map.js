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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



define(function (require) {
    "use strict";


    var Class = require('./class'),
		Axis =  require('./axis/Axis'),
        Config = require('./aperture-config-map'),
        Map;



    Map = Class.extend({
        ClassName: "Map",
        init: function (id, spec) {

			var that = this,
				mapSpecs,
				axisSpecs,
				axisSpec,
				i;
		
			// isolate map and axis specifications from json objects
			mapSpecs = $.grep(spec, function( element ) {
				// skip any axis config objects
				return !(element.hasOwnProperty("AxisConfig"));
			});

			axisSpecs = $.grep(spec, function( element ) {
				// skip any non-axis config objects
				return (element.hasOwnProperty("AxisConfig"));
			});

            Config.loadConfiguration(mapSpecs);

            // Set up map initialization parameters
            this.mapSpec = {
                id: id,
                options: {
                    mapExtents: [ -180.000000, -85.051129, 180.000000, 85.051129 ],
                    projection: "EPSG:900913",
                    numZoomLevels: 12,
                    units: "m",
                    restricted: false
                }
            };

            // Initialize the map
            this.map = new aperture.geo.Map(this.mapSpec);
            this.map.olMap_.baseLayer.setOpacity(1);
            this.map.all().redraw();
            // The projection the map uses
            this.projection = new OpenLayers.Projection("EPSG:900913");
			
			
			// Create axes
			this.axes = [];						
			for (i=0; i<axisSpecs.length; ++i) {
				axisSpec = axisSpecs[i].AxisConfig;
				axisSpec.parentId = this.mapSpec.id;
				axisSpec.olMap = this.map.olMap_;
				this.axes.push(new Axis(axisSpec));
			}
			
			// Set resize map callback
			$(window).resize( function() {
				var ASPECT_RATIO = 1.61803398875, // golden ratio... ooooo
					$map = $('#' + that.mapSpec.id),
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
            this.map.on(eventType, callback);
        },

        off: function(eventType, callback) {
            this.map.off(eventType, callback);
        },

        trigger: function(eventType, event) {
            this.map.trigger(eventType, event);
        }
    });

    return Map;
});
