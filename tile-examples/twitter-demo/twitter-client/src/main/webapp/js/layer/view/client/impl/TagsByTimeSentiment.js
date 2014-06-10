/*
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
        DetailsOnDemand = require('./DetailsOnDemandHtml'),
        TwitterUtil = require('./TwitterUtil'),
        NUM_HOURS_IN_DAY = 24,
        TagsByTimeSentiment;



    TagsByTimeSentiment = TwitterTagRenderer.extend({
        ClassName: "TagsByTimeSentiment",

        init: function( map ) {
            this._super( map );
            this.MAX_NUM_VALUES = 10;
            this.Y_SPACING = 18;
            this.createLayer();
        },


        getTotalCountPercentage: function(data, index) {
            var tagIndex = Math.floor(index / NUM_HOURS_IN_DAY),
                countByTime = data.bin.value[tagIndex].countByTime[index % NUM_HOURS_IN_DAY];
            return (countByTime / data.bin.value[tagIndex].count) || 0;
        },


        getMaxPercentage: function(data, index) {
            var i,
                percent,
                tagIndex = Math.floor(index/NUM_HOURS_IN_DAY),
                maxPercent = 0,
                count = data.bin.value[tagIndex].count;

            for (i=0; i<NUM_HOURS_IN_DAY; i++) {
                percent = (data.bin.value[tagIndex].countByTime[i] / count) || 0;
                if (percent > maxPercent) {
                    maxPercent = percent;
                }
            }
            return maxPercent;
        },


        onClick: function(event, index) {

            TwitterUtil.createDetailsOnDemand( this.map, event.data, index, DetailsOnDemand );
        },


        blendSentimentColours: function(positiveCount, negativeCount) {
            var totalCount,
                negWeight, negRGB,
                posWeight, posRGB,
                finalRGB = {};

            totalCount = positiveCount + negativeCount;

            if (totalCount === 0) {
                return this.DARK_GREY_COLOUR;
            }

            negRGB = this.hexToRgb(this.PURPLE_COLOUR);
            posRGB = this.hexToRgb(this.BLUE_COLOUR);
            negWeight = negativeCount/totalCount;
            posWeight = positiveCount/totalCount;

            finalRGB.r = (negRGB.r * negWeight) + (posRGB.r * posWeight);
            finalRGB.g = (negRGB.g * negWeight) + (posRGB.g * posWeight);
            finalRGB.b = (negRGB.b * negWeight) + (posRGB.b * posWeight);
            return this.rgbToHex(finalRGB.r, finalRGB.g, finalRGB.b);
        },


        createLayer: function() {

            this.nodeLayer = this.map.addApertureLayer( aperture.geo.MapNodeLayer );
            this.nodeLayer.map('latitude').from('latitude');
            this.nodeLayer.map('longitude').from('longitude');
            this.createBars();
            this.createLabels();
            this.createCountSummaries();
        },


        createBars: function() {

            var that = this,
                BAR_WIDTH = 3,
                BAR_LENGTH = 10;
       
            this.bars = this.nodeLayer.addLayer(aperture.BarLayer);
            this.bars.map('orientation').asValue('vertical');
            this.bars.map('width').asValue(BAR_WIDTH);
            this.bars.map('visible').from( function() {
                return that.visibility;
            });
            this.bars.map('fill').from( function(index) {
                /*
                // get counts
                positiveCount = this.bin.value[tagIndex].positiveByTime[index % NUM_HOURS_IN_DAY];
                negativeCount = this.bin.value[tagIndex].negativeByTime[index % NUM_HOURS_IN_DAY];
                return that.blendSentimentColours(positiveCount, negativeCount);
                */
                return that.WHITE_COLOUR;
            });

            this.bars.map('bar-count').from( function() {
                return NUM_HOURS_IN_DAY * that.getCount(this);
            });
            this.bars.map('offset-y').from(function(index) {
                var maxPercentage = that.getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return that.Y_CENTRE_OFFSET -((that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH) +
                       that.getYOffset(this, Math.floor(index/NUM_HOURS_IN_DAY));
            });
            this.bars.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET - 90 + ((index % NUM_HOURS_IN_DAY) * (BAR_WIDTH+1));
            });
            this.bars.map('length').from(function (index) {
                var maxPercentage = that.getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return (that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH;
            });

            this.bars.on('click', function(event) {
                that.onClick(event, Math.floor(event.index[0]/NUM_HOURS_IN_DAY));
                return true; // swallow event
            });

            this.bars.map('opacity').from( function() {
                return that.opacity;
            })

        },


        createCountSummaries: function () {

            var that = this;

            this.summaryLabel = this.nodeLayer.addLayer(aperture.LabelLayer);
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.summaryLabel.map('font-outline-width').asValue(3);
            this.summaryLabel.map('visible').from( function() {
                return false; //that.visible;
            });
            this.summaryLabel.map('fill').from( function(index) {
                switch(index) {
                    case 0: return that.BLUE_COLOUR;
                    case 1: return that.WHITE_COLOUR;
                    default: return that.PURPLE_COLOUR;
                }
            });
            this.summaryLabel.map('text').from( function(index) {
                /*
                var tagIndex = that.clientState.hoverState.userData.index;
                switch(index) {
                    case 0: return "+ "+this.bin.value[tagIndex].positive;
                    case 1: return ""+this.bin.value[tagIndex].neutral;
                    default: return "- "+this.bin.value[tagIndex].negative;
                }
                */
                return "";
            });
            this.summaryLabel.map('offset-y').from(function(index) {
                return (-that.TILE_SIZE/2) + (that.VERTICAL_BUFFER-4) + (14 * index);
            });
            this.summaryLabel.map('offset-x').asValue(this.TILE_SIZE - this.HORIZONTAL_BUFFER);
            this.summaryLabel.map('text-anchor').asValue('end');
            this.summaryLabel.map('opacity').from( function() {
                return that.opacity;
            });
        },


        createLabels: function () {

            var that = this,
                MAX_TITLE_CHAR_COUNT = 9;

            this.tagLabels = this.nodeLayer.addLayer(aperture.LabelLayer);

            this.tagLabels.map('visible').from(function() {
                return that.visibility;
            });

            this.tagLabels.map('fill').asValue( that.WHITE_COLOUR );

            this.tagLabels.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabels.map('text').from(function (index) {
                var str = that.filterText(this.bin.value[index].tag);
                if (str.length > MAX_TITLE_CHAR_COUNT) {
                    str = str.substr(0, MAX_TITLE_CHAR_COUNT) + "...";
                }
                return str;
            });

            this.tagLabels.map('font-size').asValue( 12 );

            this.tagLabels.map('offset-y').from(function (index) {
                return that.Y_CENTRE_OFFSET - that.getYOffset(this, index) - 5;
            });

            this.tagLabels.map('offset-x').asValue(that.X_CENTRE_OFFSET + 16);
            this.tagLabels.map('text-anchor').asValue('start');
            this.tagLabels.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabels.map('font-outline-width').asValue(3);

            this.tagLabels.on('click', function(event) {
                that.onClick( event, event.index[0] );
                return true; // swallow event
            });

            this.tagLabels.map('opacity').from( function() {
                return that.opacity;
            });

        }


    });

    return TagsByTimeSentiment;
});
