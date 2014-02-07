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



    var TwitterTagRenderer = require('./TwitterTagRenderer'),
        DetailsOnDemand = require('./DetailsOnDemand'),
        HashTagsByTime;


    HashTagsByTime = TwitterTagRenderer.extend({
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


        redrawLayers: function(data) {
            this.tagLabels.all().where(data).redraw();
            this.bars.all().where(data).redraw();
        },


        onUnselect: function() {
            this.clearMouseClickState();
            this.plotLayer.all().redraw();
        },


        onClick: function(event, index) {
            this.setMouseClickState(event.data.tilekey, {
                tag : event.data.bin.value[index].tag,
                index :  index
            });
            this.plotLayer.all().redraw();
        },


        onHover: function(event, index) {
            this.setMouseHoverState(event.data.tilekey, {
                tag : event.data.bin.value[index].tag,
                index : index
            });
            this.redrawLayers(event.data);
        },


        onHoverOff: function(event) {
            this.clearMouseHoverState();
            this.redrawLayers(event.data);
        },


        blendSentimentColours: function(positiveCount, negativeCount) {
            var totalCount,
                negWeight, negRGB,
                posWeight, posRGB,
                finalRGB = {};

            totalCount = positiveCount + negativeCount;

            if (totalCount === 0) {
                return this.NEUTRAL_COLOUR;
            }

            negRGB = this.hexToRgb(this.NEGATIVE_COLOUR);
            posRGB = this.hexToRgb(this.POSITIVE_COLOUR);
            negWeight = negativeCount/totalCount;
            posWeight = positiveCount/totalCount;

            finalRGB.r = (negRGB.r * negWeight) + (posRGB.r * posWeight);
            finalRGB.g = (negRGB.g * negWeight) + (posRGB.g * posWeight);
            finalRGB.b = (negRGB.b * negWeight) + (posRGB.b * posWeight);
            return this.rgbToHex(finalRGB.r, finalRGB.g, finalRGB.b);
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (mapNodeLayer) {

            // TODO: everything should be put on its own PlotLayer instead of directly on the mapNodeLayer
            // TODO: currently does not render correctly if on its own PlotLayer...
            this.plotLayer = mapNodeLayer;
            this.createBars();
            this.createLabels();
            this.detailsOnDemand = new DetailsOnDemand(this.id);
            this.detailsOnDemand.attachMouseState(this.mouseState);
            this.detailsOnDemand.createLayer(this.plotLayer);
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
                return that.id === this.renderer && that.isNotBehindDoD(this.tilekey);
            });

            this.bars.map('fill').from( function(index) {
                var tagIndex = Math.floor(index/24),
                    positiveCount,
                    negativeCount;
                if (that.matchingTagIsSelected(this.bin.value[tagIndex].tag)){
                    // get counts
                    positiveCount = this.bin.value[tagIndex].positiveByTime[index % 24];
                    negativeCount = this.bin.value[tagIndex].negativeByTime[index % 24];
                    return that.blendSentimentColours(positiveCount, negativeCount);
                }
                if (that.shouldBeGreyedOut(this.bin.value[tagIndex].tag, this.tilekey)) {
                    return '#666666';
                }
                return "#FFFFFF";
            });

            this.bars.map('orientation').asValue('vertical');
            this.bars.map('bar-count').from( function() {
                return 24 * that.getCount(this);
            });
            this.bars.map('width').asValue(3);
            this.bars.map('offset-y').from(function(index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return -((that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH) +
                       that.getYOffset(this, Math.floor(index/24));
            });

            this.bars.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET - 90 + ((index % 24) * 4);
            });

            this.bars.map('length').from(function (index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return (that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH;
            });

            this.bars.on('click', function(event) {
                that.onClick(event, Math.floor(event.index[0]/24));
                return true; // swallow event
            });

            this.bars.on('mousemove', function(event) {
                that.onHover(event, Math.floor(event.index[0]/24));
            });

            this.bars.on('mouseout', function(event) {
                that.onHoverOff(event);
            });

        },

        createLabels: function () {

            var that = this;

            this.tagLabels = this.plotLayer.addLayer(aperture.LabelLayer);

            this.tagLabels.map('visible').from(function() {
                return that.id === this.renderer && that.isNotBehindDoD(this.tilekey);
            });

            this.tagLabels.map('fill').from( function(index) {

                if (that.shouldBeGreyedOut(this.bin.value[index].tag, this.tilekey)) {
                    return '#666666';
                }
                return '#FFFFFF';
            });

            this.tagLabels.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabels.map('text').from(function (index) {
                var str = this.bin.value[index].tag;
                if (str.length > 9) {
                    str = str.substr(0,9) + "...";
                }
                return str;
            });

            this.tagLabels.map('font-size').from( function(index) {
                if (that.isHoveredOrClicked(this.bin.value[index].tag, this.tilekey)) {
                    return 16;
                }
                return 12;
            });

            this.tagLabels.map('offset-y').from(function (index) {
                return that.getYOffset(this, index) - 5;
            });

            this.tagLabels.map('offset-x').asValue(that.X_CENTRE_OFFSET + 16);
            this.tagLabels.map('text-anchor').asValue('start');
            this.tagLabels.map('font-outline').asValue('#000000');
            this.tagLabels.map('font-outline-width').asValue(3);

            this.tagLabels.on('click', function(event) {
                that.onClick(event, event.index[0]);
                return true; // swallow event
            });

            this.tagLabels.on('mousemove', function(event) {
                that.onHover(event, event.index[0]);
                return true;  // swallow event, for some reason mousemove on labels needs to swallow this or else it processes a mouseout
            });

            this.tagLabels.on('mouseout', function(event) {
                that.onHoverOff(event);
            });

        }


    });

    return HashTagsByTime;
});
