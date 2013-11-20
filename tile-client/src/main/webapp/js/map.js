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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



define(function (require) {
    "use strict";



    var Class = require('./class'),
        Config = require('./aperture-config-map'),
        Map;



    Map = Class.extend({
        init: function (id, baseLayerSpec) {
            var mapSpec;

            Config.loadConfiguration(baseLayerSpec);

            // Set up map initialization parameters
            mapSpec = {
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
            this.map = new aperture.geo.Map(mapSpec);
            this.map.olMap_.baseLayer.setOpacity(1);
            this.map.all().redraw();


            // The projection the map uses
            this.projection = new OpenLayers.Projection("EPSG:900913");
        },

        setOpacity: function (newOpacity) {
            this.map.olMap_.baseLayer.setOpacity(newOpacity);
        },

        getOpacity: function () {
            return this.map.olMap_.baseLayer.opacity;
        },

        getExtent: function () {
            return this.map.olMap_.getExtent();
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
