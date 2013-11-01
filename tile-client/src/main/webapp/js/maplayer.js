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
define(['class'], function (Class) {
    "use strict";
    var MapLayer;



    MapLayer = Class.extend({
        init: function (id) {
            this.id = id;
            this.map = null;
            this.onMapUpdate = null;
            this.adding = false;
        },

        createLayer: function (nodeLayer) {
            throw "Undefined createLayer method in map layer;  createLayer must be overridden in leaf class.";
        },

        destroyLayer: function (nodeLayer) {
            // destroy doesn't have to be rededfined, but we give layers the opportunity.
            return undefined;
        },

        startMapUpdate: function () {
            if (!this.adding && this.map && this.onMapUpdate) {
                this.map.map.on('zoom',   this.onMapUpdate);
                this.map.map.on('panend', this.onMapUpdate);
                this.onMapUpdate(null);
            }
        },

        stopMapUpdate: function () {
            if (!this.adding && this.map && this.onMapUpdate) {
                this.map.map.off('zoom',   this.onMapUpdate);
                this.map.map.off('panend', this.onMapUpdate);
            }
        },

        setUpdateFcn: function (onUpdate) {
            this.stopMapUpdate();

            this.onMapUpdate = onUpdate;

            this.startMapUpdate();
        },
                
                
        // Add this layer to the given map
        addToMap: function (map) {
            this.adding = true;
            this.map = map;

            // Add our layers
            this._nodeLayer = this.map.map.addLayer(aperture.geo.MapNodeLayer);
            this._nodeLayer.map('latitude').from('latitude');
            this._nodeLayer.map('longitude').from('longitude');
            this._nodeLayer.map('visible').asValue(true);

            this.createLayer(this._nodeLayer);
            this.adding = false;

            this.startMapUpdate();
        },

        // Remove this layer from its current map
        removeFromMap: function () {
            if (this.map) {
                this.stopMapUpdate();

                this.destroyLayer(this._nodeLayer);
                this.map.map.remove(this._nodeLayer);

                this._nodeLayer = null;
                this.map = null;
            }
        }
    });


    return MapLayer;
});
