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



    var ClientRenderer = require('./ClientRenderer'),
        HashTagsByTime;


    HashTagsByTime = ClientRenderer.extend({
        ClassName: "HashTagsByTime",

        init: function(id) {

            this._super(id);
            this.valueCount = 10;
            this.ySpacing = 18;
        },


        getTotalCountPercentage: function(data, index) {
            var tagIndex = Math.floor(index / 24);

            if (data.bin.value[tagIndex].count === 0) {
                return 0;
            }

            return data.bin.value[tagIndex].countByTime[index % 24] / data.bin.value[tagIndex].count;
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (nodeLayer) {

            var that = this;

            this.plotLayer = nodeLayer; //.addLayer(aperture.PlotLayer);
            /*
            this.plotLayer.map('visible').from(function() {
                return that.id === this.renderer;
            });
*/
            this.createBars();
            this.createLabels();

        },


        createBars: function() {

            var that = this,
                BAR_LENGTH = 10;

            function getMaxPercentage(data, index) {
                var i,
                    percent,
                    tagIndex = Math.floor(index/24),
                    maxPercent = 0,
                    count = data.bin.value[tagIndex].count;
                if (count === 0) {
                    return 0;
                }
                for(i=0; i<24; i++) {
                    // get maximum percent
                    percent = data.bin.value[tagIndex].countByTime[i] / count;
                    if (percent > maxPercent) {
                        maxPercent = percent;
                    }
                }
                return maxPercent;
            }

            this.bars = this.plotLayer.addLayer(aperture.BarLayer);

            this.bars.map('visible').from( function() {
                return that.id === this.renderer;
            });

            this.bars.map('fill').asValue('#FFFFFF');
            this.bars.map('orientation').asValue('vertical');
            this.bars.map('bar-count').from( function() {
                return 24 * that.getCount(this);
            });
            this.bars.map('width').asValue(4);
            this.bars.map('offset-y').from(function(index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return -((that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH) +
                    that.getYOffset(this, Math.floor(index/24));
            });

            this.bars.map('offset-x').from(function (index) {
                return -90 + ((index % 24) * 5);
            });

            this.bars.map('length').from(function (index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return (that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH;
            });


        },

        createLabels: function () {

            var that = this;

            this.labelLayer = this.plotLayer.addLayer(aperture.LabelLayer);

            this.labelLayer.map('visible').from(function() {
                return that.id === this.renderer;
            });

            this.labelLayer.map('fill').asValue('#FFFFFF');

            this.labelLayer.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.labelLayer.map('text').from(function (index) {
                var str = "#" + this.bin.value[index].tag;
                if (str.length > 7) {
                    str = str.substr(0,7) + "...";
                }
                return str;
            });

            this.labelLayer.map('font-size').asValue(12);

            this.labelLayer.map('offset-y').from(function (index) {
                return that.getYOffset(this, index) - 5;
            });

            this.labelLayer.map('offset-x').asValue(40);
            this.labelLayer.map('text-anchor').asValue('start');
            this.labelLayer.map('font-outline').asValue('#000000');
            this.labelLayer.map('font-outline-width').asValue(3);
        }




    });

    return HashTagsByTime;
});
