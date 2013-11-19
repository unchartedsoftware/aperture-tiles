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
            // Set our base position within each tile
            this.tracker.setPosition('center');
            // Set hover output to be ignored by default
            this.tooltipFcn = null;
            // Start up our server data listener
            this.coordinator = new MapServerCoordinator(this.tracker,
                                                        layerSpec);
        },

        /**
         * Set a function to call whenever a tooltip can be provided.  The 
         * function should expect one argument, the text of the tooltip (as 
         * html).  It will be called with null to clear the tooltip.
         */
        setTooltipFcn: function (tooltipFcn) {
            this.tooltipFcn = tooltipFcn;
        },

        /**
         * A method to get the text of a tooltip from a mouse event
         */
        getTooltip: function (event) {
            var index = event.index,
                data = event.data,
                n = data.bin.value.length,
                maxValue = null,
                minValue = null,
                i, key, value;

            for (i=0; i<n; ++i) {
                value = data.bin.value[i].value;
                if (!maxValue || value > maxValue) {
                    maxValue = value;
                }
                if (!minValue || value < minValue) {
                    minValue = value;
                }
            }
            key = data.bin.value[index].key;
            value = data.bin.value[index].value;

            return "Text: "+key+", score: "+value+" from range ["+minValue+", "+maxValue+"]";
        },

        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (nodeLayer) {
            var that = this,
                getYOffsetFcn, getValueFcn;

            getYOffsetFcn = function (index) {
                return 16 * ((this.bin.value.length - 1.0) / 2.0 - index);
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
            this.labelLayer.map('offset-x').from(function (index) {
                var value = getValueFcn(index, this);
                if (value >= 0) {
                    return -10;
                }
                return 10;
            });
            this.labelLayer.map('offset-y').from(getYOffsetFcn);
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
            this.labelLayer.on('mouseover', function (event) {
                if (that.tooltipFcn) {
                    that.tooltipFcn(that.getTooltip(event));
                }
            });
            this.labelLayer.on('mouseout', function (event) {
                if (that.tooltipFcn) {
                    that.tooltipFcn(null);
                }
            });

            this.barLayer = this._nodeLayer.addLayer(aperture.BarLayer);
            this.barLayer.map('orientation').asValue('horizontal');
            this.barLayer.map('bar-count').from('bin.value.length');
            this.barLayer.map('width').asValue('10');
            this.barLayer.map('offset-x').from(function (index) {
                var value = getValueFcn(index, this);
                return 100*Math.min(value, 0);
            });
            this.barLayer.map('offset-y').from(getYOffsetFcn);
            this.barLayer.map('length').from(function (index) {
                var value = getValueFcn(index, this);
                return 100.0 * Math.abs(value);
            });
            this.barLayer.map('fill').from('#80C0FF');
            this.barLayer.on('mouseover', function (event) {
                if (that.tooltipFcn) {
                    that.tooltipFcn(that.getTooltip(event));
                }
            });
            this.barLayer.on('mouseout', function (event) {
                if (that.tooltipFcn) {
                    that.tooltipFcn(null);
                }
            });


            this.coordinator.setMap(this.map);
        }
    });

    return TextScoreLayer;
});
