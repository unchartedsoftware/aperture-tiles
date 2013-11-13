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
 * This module defines a simple client-rendered layer that displays a 
 * text score tile in a meaningful way.
 */
define(function (require) {
    "use strict";



    var MapLayer = require('./MapLayer'),
        MapServerCoordinator = require('./MapServerCoordinator'),

        ClientRenderedMapLayer;



    ClientRenderedMapLayer = MapLayer.extend({
        init: function (id, layerSpec) {
            this._super(id);
            this.setPosition('center');
            this.coordinator = new MapServerCoordinator(this.tracker,
                                                        layerSpec);
        },

        createLayer: function (nodeLayer) {
            this._labelLayer = this._nodeLayer.addLayer(aperture.LabelLayer);
            this._labelLayer.map('label-count')
                .from(function (data, index) {
                    return this.tracker.getNumValues(index);
                });
            this._labelLayer.map('text')
                .from(function (data, index, subIndex) {
                    return this.tracker.getValue(index, subIndex);
                });
            this._labelLayer.map('offset-y')
                .from(function (data, index, subIndex) {
                    return (subIndex-this.tracker.getNumValues(index)/2) * 12;
                });

            this.coordinator.setMap(this.map);
        }
    });

    return ClientRenderedMapLayer;
});
