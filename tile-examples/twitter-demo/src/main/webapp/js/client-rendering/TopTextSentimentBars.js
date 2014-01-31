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



    var Class = require('../class'),
        TopTextSentimentBars;



    TopTextSentimentBars = Class.extend({
        ClassName: "TopTextSentimentBars",

        init: function(colour, id) {

            this.colour = colour;
            this.tooltipFcn = null;
            this.id = id;

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


        createBars: function(nodeLayer) {

            var that = this,
                BAR_LENGTH = 100;

            function getNegativeCountPercentage(data, index) {
                return data.bin.value[index].negative / data.bin.value[index].count;
            }

            function getPositiveCountPercentage(data, index) {
                return data.bin.value[index].positive / data.bin.value[index].count;
            }

            function getNeutralCountPercentage(data, index) {
                return data.bin.value[index].neutral / data.bin.value[index].count;
            }

            function barTemplate() {
                var bar = nodeLayer.addLayer(aperture.BarLayer);
                bar.map('visible').from(function() {
                    return that.id === this.renderer;
                });
                bar.map('orientation').asValue('horizontal');
                bar.map('bar-count').from(getCount);
                bar.map('width').asValue('10');
                bar.map('stroke').asValue("#000000");
                bar.map('stroke-width').asValue(2);
                bar.map('offset-y').from(getYOffset);
                bar.toFront();
                return bar;
            }

            function getCount() {
                return (this.bin.value.length > 5) ? 5 : this.bin.value.length;
            };

            function getYOffset(index) {
                return -30 * (($.proxy(getCount, this)() - 1.0) / 2.0 - index) + 15;
            };

            // negative bar
            this.negativeBar = barTemplate();
            this.negativeBar.map('offset-x').from(function (index) {
                return -(getNeutralCountPercentage(this,index) * BAR_LENGTH)/2 +
                    -(getNegativeCountPercentage(this,index) * BAR_LENGTH);
            });
            this.negativeBar.map('length').from(function (index) {
                return getNegativeCountPercentage(this,index) * BAR_LENGTH;
            });
            this.negativeBar.map('fill').asValue('#333333');

            // neutral bar
            this.neutralBar = barTemplate();
            this.neutralBar.map('offset-x').from(function (index) {
                return -(getNeutralCountPercentage(this,index) * BAR_LENGTH)/2;
            });
            this.neutralBar.map('length').from(function (index) {
                return getNeutralCountPercentage(this,index) * BAR_LENGTH;
            });
            this.neutralBar.map('fill').asValue('#666666');

            // positive bar
            this.positiveBar = barTemplate();
            this.positiveBar.map('offset-x').from(function (index) {
                return (getNeutralCountPercentage(this,index) * BAR_LENGTH)/2;
            });
            this.positiveBar.map('length').from(function (index) {
                return getPositiveCountPercentage(this,index) * BAR_LENGTH;
            });
            this.positiveBar.map('fill').asValue('#FFFFFF');
        },

        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (nodeLayer) {

            var that = this,
                hover = new aperture.Set(); //'bin.value[].tag');

            console.log("creating layer " + this.id);

            this.createBars(nodeLayer);

            function getYOffset(index) {
                return -30 * (($.proxy(getCount, this)() - 1.0) / 2.0 - index) + 5;
            };

            function getCountPercentage(data, index) {
                var i,
                    sum = 0,
                    n = $.proxy(getCount, data)();
                for (i=0; i<n; i++) {
                    sum += data.bin.value[i].count;
                }

                return data.bin.value[index].count/sum;
            }

            function getCount() {
                return (this.bin.value.length > 5) ? 5 : this.bin.value.length;
            };

            this.labelLayer = nodeLayer.addLayer(aperture.LabelLayer);

            this.labelLayer.map('visible').from(function() {
                return that.id === this.renderer;
            });

            this.labelLayer.map('fill').asValue(this.colour); //filter( hover.constant( aperture.palette.color('selected') ) );

            this.labelLayer.on('click', function(event) {
                console.log("click");
                hover.add(event.data.bin.value[event.index].tag);
            });


            this.labelLayer.map('label-count').from(getCount);

            this.labelLayer.map('text').from(function (index) {

                var str = "#" + this.bin.value[index].tag;
                if (str.length > 12) {
                    str = str.substr(0,12) + "...";
                }
                return str;
                //var long = parseFloat( (Math.round(this.longitude * 100) / 100).toFixed(2)),
                //    lat = parseFloat( (Math.round(this.latitude * 100) / 100).toFixed(2));
                //return long+ " " + lat;
            });

            this.labelLayer.map('font-size').from(function (index) {
                var size = (getCountPercentage(this,index) * 60) + 10;
                return size > 30 ? 30 : size;
            });
            this.labelLayer.map('offset-y').from(getYOffset);
            this.labelLayer.map('text-anchor').asValue('middle');
            //this.labelLayer.map('fill').asValue('#FFFFFF');
            this.labelLayer.map('font-outline').asValue('#000000');
            this.labelLayer.map('font-outline-width').asValue(3);

            this.labelLayer.toFront();

        }
    });

    return TopTextSentimentBars;
});
