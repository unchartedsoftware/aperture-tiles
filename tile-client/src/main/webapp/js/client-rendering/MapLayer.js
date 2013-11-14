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



/**
 * This modules defines a basic layer class that can be added to maps.
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        DataTracker = require('./TileLayerDataTracker'),
        MapLayer;



    MapLayer = Class.extend({
        ClassName: "MapLayer",
        init: function (id) {
            this.id = id;
            this.map = null;
            this.tracker = new DataTracker();
            this.position = {x: 'centerX', y: 'centerY'};
        },

        /**
         * Called when the visuals for this layer are to be created. 
         * This method <em>must</em> be overridden by implementations.
         * Without an implementation of this method, any MapLayer would,
         * of course be useless anyway.
         *
         * @param nodeLayer The node layer to which to attach visuals.
         */
        createLayer: function (nodeLayer) {
            throw ("Undefined createLayer method in map layer;  " +
                   "createLayer must be overridden in leaf class.");
        },

        /**
         * Called when this layer is removed from the map, and visuals should
         * be destroyed.  It is not essential that implementations implement
         * this method, but it exists as a convenience to allow implementations
         * to do any cleanup they need at this point.
         *
         * @param nodeLayer The node layer to which visuals were attached.
         */
        destroyLayer: function (nodeLayer) {
            return undefined;
        },





        /**
         * Add this layer to the given map
         */
        addToMap: function (map) {
            this.map = map;


            // Add our layers
            this._nodeLayer = this.map.map.addLayer(aperture.geo.MapNodeLayer);
            this.tracker.setNodeLayer(this._nodeLayer);
            this._nodeLayer.map('longitude').from('longitude');
            this._nodeLayer.map('latitude').from('latitude');
            this._nodeLayer.map('visible').asValue('visible');

            this.createLayer(this._nodeLayer);
        },

        /**
         * Remove this layer from its current map
         */
        removeFromMap: function () {
            if (this.map) {
                this.destroyLayer(this._nodeLayer);
                this.map.map.remove(this._nodeLayer);

                this._nodeLayer = null;
                this.map = null;
            }
        }
    });

    return MapLayer;
});
