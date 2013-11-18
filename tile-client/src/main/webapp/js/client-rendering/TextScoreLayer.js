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
        TextScoreLayer;



    TextScoreLayer = MapLayer.extend({
        ClassName: "TextScoreLayer",
        init: function (id, layerSpec) {
            this._super(id);
            this.tracker.setPosition('center');
            this.coordinator = new MapServerCoordinator(this.tracker,
                                                        layerSpec);
        },

        createLayer: function (nodeLayer) {
            // TODO:
            //   1 Reverse order
            //   2 Scale individually by tile, not across tiles
            var getYFcn, getValueFcn;

            getYFcn = function (index) {
                var n = this.bin.value.length,
                    start = (n - 1) / 22.0,
                    value = start - index / 11.0,
                    pow2 = 1 << this.level;
                return value/pow2;
            };
            getValueFcn = function (index, data) {
                var n = data.bin.value.length,
                    maxValue = 0.0,
                    i, value;
                for (i=0; i<n; ++i) {
                    value = Math.abs(data.bin.value[i].value);
                    if (value > maxValue) {
                        maxValue = value;
                    }
                }
                return data.bin.value[index].value / maxValue;
            };
                

            this.labelLayer = this._nodeLayer.addLayer(aperture.LabelLayer);
            this.labelLayer.map('label-count').from('bin.value.length');
            this.labelLayer.map('text').from(function (index) {
                return this.bin.value[index].key;
            });
            this.labelLayer.map('y').from(getYFcn);
            this.labelLayer.map('offset-x').asValue(-10);
            this.labelLayer.map('text-anchor').from(function (index) {
                var value = getValueFcn(index, this);
                if (value >= 0) {
                    return 'end';
                }
                return 'start';
            });
            this.labelLayer.map('fill').asValue('#C0FFC0');
            this.labelLayer.map('font-outline').asValue('#222');
            this.labelLayer.map('font-outline-width').asValue(3);
            this.labelLayer.map('visible').asValue(true);

            this.barLayer = this._nodeLayer.addLayer(aperture.BarLayer);
            this.barLayer.map('orientation').asValue('horizontal');
            this.barLayer.map('bar-count').from('bin.value.length');
            this.barLayer.map('y').from(getYFcn);
            this.barLayer.map('width').asValue('10');
            this.barLayer.map('x').from(function (index) {
                var value = getValueFcn(index, this);
                return 0.45 * Math.min(value, 0);
            });
            this.barLayer.map('length').from(function (index) {
                var value = getValueFcn(index, this);
                return 100.0 * Math.abs(value);
            });
            this.barLayer.map('fill').from('#80C0FF');

            this.coordinator.setMap(this.map);
        }
    });

    return TextScoreLayer;
});
