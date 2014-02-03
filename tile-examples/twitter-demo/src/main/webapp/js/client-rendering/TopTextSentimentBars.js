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
            this.hoverInfo = {
                tag : '',
                binkey : '',
                index : -1
            };

        },


        getCount: function(data) {
            if (data.bin.value.length === undefined) {
                return 0;
            }
            return (data.bin.value.length > 5) ? 5 : data.bin.value.length;
        },


        getExclusiveCountPercentage: function(data, index, type) {

            var attrib = type + 'ByTime';
            if (data.bin.value[this.hoverInfo.index][type] === 0) {
                return 0;
            }
            return data.bin.value[this.hoverInfo.index][attrib][index] / data.bin.value[this.hoverInfo.index][type];
        },


        getCountPercentage: function(data, index, type) {
            if (data.bin.value[index].count === 0) {
                return 0;
            }
            return data.bin.value[index][type] / data.bin.value[index].count;
        },


        getTotalCountPercentage: function(data, index) {
            var i,
                sum = 0,
                n = this.getCount(data);
            for (i=0; i<n; i++) {
                sum += data.bin.value[i].count;
            }
            if (sum === 0) {
                return 0;
            }
            return data.bin.value[index].count/sum;
        },


        getYOffset: function(data, index) {
            return -30 * (((this.getCount(data) - 1) / 2) - index);
        },


        hoverOnEvent: function(event) {
            this.hoverInfo.tag = event.data.bin.value[event.index[0]].tag;
            this.hoverInfo.binkey = event.data.binkey;
            this.hoverInfo.index = event.index[0];
            this.plotLayer.all().redraw();
            return true;
        },


        hoverOffEvent: function() {
            this.hoverInfo.tag = '';
            this.hoverInfo.binkey = '';
            this.hoverInfo.index = -1;
            this.plotLayer.all().redraw();
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (nodeLayer) {

            var that = this;

            this.plotLayer = nodeLayer.addLayer(aperture.PlotLayer);
            this.plotLayer.map('visible').from(function() {
                return that.id === this.renderer;
            });

            this.createBars();
            this.createLabels();
            this.createDetailsOnDemand();

        },


        createBars: function() {

            var that = this,
                BAR_LENGTH = 100;

            function barTemplate( defaultColour, selectedColour ) {

                var bar = that.plotLayer.addLayer(aperture.BarLayer);

                bar.map('visible').from( function() {
                    return (that.id === this.renderer) &&
                        (that.hoverInfo.tag === '' || that.hoverInfo.binkey === this.binkey);
                });

                // this doesnt work
                /*
                bar.map('bar-visible').from( function(index) {
                    return (that.id === this.renderer) &&
                           (hoverInfo.tag === '' || hoverInfo.index === index);
                });
                */

                bar.map('fill').from( function(index) {
                    if (that.hoverInfo.tag === this.bin.value[index].tag) {
                        return selectedColour;
                    }
                    return defaultColour;
                });

                bar.on('mousemove', function(event) {
                    return that.hoverOnEvent(event);
                });

                bar.on('mouseout', function() {
                    that.hoverOffEvent();
                });

                bar.map('orientation').asValue('horizontal');
                bar.map('bar-count').from(function() {
                    return that.getCount(this);
                });
                bar.map('width').asValue(10);
                bar.map('stroke').asValue("#000000");
                bar.map('stroke-width').asValue(2);
                bar.map('offset-y').from(function(index) {
                    return that.getYOffset(this, index) + 15;
                });
                bar.toFront();
                return bar;
            }

            // negative bar
            this.negativeBar = barTemplate('#777777', '#D33CFF');
            this.negativeBar.map('offset-x').from(function (index) {
                return -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2 +
                    -(that.getCountPercentage(this, index, 'negative') * BAR_LENGTH);
            });
            this.negativeBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'negative') * BAR_LENGTH;
            });

            // neutral bar
            this.neutralBar = barTemplate('#222222', '#222222' );
            this.neutralBar.map('offset-x').from(function (index) {
                return -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.neutralBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH;
            });

            // positive bar
            this.positiveBar = barTemplate('#FFFFFF', '#09CFFF');
            this.positiveBar.map('offset-x').from(function (index) {
                return (that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.positiveBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'positive') * BAR_LENGTH;
            });
        },


        createLabels: function () {

            var that = this;

            this.labelLayer = this.plotLayer.addLayer(aperture.LabelLayer);

            this.labelLayer.map('visible').from(function() {
                return that.id === this.renderer;
            });

            this.labelLayer.map('fill').from( function(index) {
                if (that.hoverInfo.tag === '') {
                    return '#FFFFFF';
                } else if (that.hoverInfo.tag !== this.bin.value[index].tag) {
                    return '#666666';
                }
                return '#FFFFFF';
            });

            this.labelLayer.on('mousemove', function(event) {
                return that.hoverOnEvent(event);
            });

            this.labelLayer.on('mouseout', function() {
                that.hoverOffEvent();
            });


            this.labelLayer.on('click', function(event) {
                console.log("click");
            });


            this.labelLayer.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.labelLayer.map('text').from(function (index) {
                var str = "#" + this.bin.value[index].tag;
                if (str.length > 12) {
                    str = str.substr(0,12) + "...";
                }
                return str;
            });

            this.labelLayer.map('font-size').from(function (index) {
                var size = (that.getTotalCountPercentage(this, index) * 60) + 10;
                return size > 30 ? 30 : size;
            });
            this.labelLayer.map('offset-y').from(function (index) {
                return that.getYOffset(this, index) + 5;
            });
            this.labelLayer.map('text-anchor').asValue('middle');
            this.labelLayer.map('font-outline').asValue('#000000');
            this.labelLayer.map('font-outline-width').asValue(3);
        },


        createDetailsOnDemand: function() {

            var that = this,
                BAR_LENGTH = 80,
                background = that.plotLayer.addLayer(aperture.BarLayer);

            background.map('visible').from( function() {
                return (that.id === this.renderer) && (that.hoverInfo.binkey === this.binkey);
            });

            background.map('fill').asValue('#222222');
            background.map('orientation').asValue('horizontal');
            background.map('bar-count').asValue(1);
            background.map('width').asValue('512');
            background.map('length').asValue('256');
            background.map('stroke').asValue("#000000");
            background.map('stroke-width').asValue(2);
            background.map('offset-y').asValue(-128);
            background.map('offset-x').asValue(-384);

            function barTemplate( colour ) {

                var bar = that.plotLayer.addLayer(aperture.BarLayer);

                bar.map('visible').from( function() {
                    return (that.id === this.renderer) && (that.hoverInfo.binkey === this.binkey);
                });

                bar.map('fill').asValue(colour);
                bar.map('orientation').asValue('vertical');
                bar.map('bar-count').asValue(24);
                bar.map('width').asValue(10);
                bar.map('stroke').asValue("#000000");
                bar.map('stroke-width').asValue(2);

                bar.map('offset-x').from( function(index) {
                    return 0; //-384 + 16 + index*10;
                });

                return bar;
            }

            // negative bar
            this.detailsNegativeBar = barTemplate('#D33CFF');
            this.detailsNegativeBar.map('offset-y').asValue(-128);
            this.detailsNegativeBar.map('length').from(function (index) {
                return that.getExclusiveCountPercentage(this, index, 'negative') * BAR_LENGTH;
            });

            // positive bar
            this.detailsPositiveBar = barTemplate('#09CFFF');
            this.detailsPositiveBar.map('offset-y').from(function (index) {
                return -128 -(that.getExclusiveCountPercentage(this, index, 'positive') * BAR_LENGTH);
            });
            this.detailsPositiveBar.map('length').from(function (index) {
                return that.getExclusiveCountPercentage(this, index, 'positive') * BAR_LENGTH;
            });

        }


    });

    return TopTextSentimentBars;
});
