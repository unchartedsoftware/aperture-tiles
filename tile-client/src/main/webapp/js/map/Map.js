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
		Axis =  require('./Axis'),
        Map;



    Map = Class.extend({
        ClassName: "Map",
		
        init: function (id, spec) {

			var that = this,
				apertureConfig,
				mapSpecs;
		
			apertureConfig = spec.ApertureConfig;
			mapSpecs = spec.MapConfig;
			
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

        setAxisSpecs: function (axes) {
            var xAxisSpec, yAxisSpec;

            xAxisSpec = axes.xAxisConfig;
            xAxisSpec.parentId = this.id;
            xAxisSpec.olMap = this.map.olMap_;
            this.axes.push(new Axis(xAxisSpec));

            yAxisSpec = axes.yAxisConfig;
            yAxisSpec.parentId = this.id;
            yAxisSpec.olMap = this.map.olMap_;
            this.axes.push(new Axis(yAxisSpec));
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
