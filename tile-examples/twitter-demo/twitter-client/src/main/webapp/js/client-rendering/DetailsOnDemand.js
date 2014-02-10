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
        DetailsOnDemand;



    DetailsOnDemand = TwitterTagRenderer.extend({
        ClassName: "DetailsOnDemand",

        init: function(id) {
            this._super(id);
        },


        getExclusiveCountPercentage: function(data, index, type) {

            var attrib = type + 'ByTime',
                tagIndex = this.mouseState.clickState.binData.index,
                count = data.bin.value[tagIndex].count;
            if (count === 0) {
                return 0;
            }
            return data.bin.value[tagIndex][attrib][index] / count;
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (nodeLayer) {

            // TODO: everything should be put on its own PlotLayer instead of directly on the mapNodeLayer
            // TODO: currently doesnt not render correctly if on its on PlotLayer...
            this.plotLayer = nodeLayer;
            this.createDetailsOnDemand();
        },


        createDetailsOnDemand: function() {

            var that = this,
                DETAILS_OFFSET_X = that.X_CENTRE_OFFSET + this.TILE_SIZE/ 2,
                DETAILS_OFFSET_Y = -this.TILE_SIZE/2,
                V_SPACING = 10,
                H_SPACING = 14,
                BAR_CENTRE_LINE = DETAILS_OFFSET_Y + 158, //30,
                BAR_LENGTH = 50,
                HISTOGRAM_AXIS = BAR_CENTRE_LINE + BAR_LENGTH + V_SPACING + 12,
                MOST_RECENT = this.TILE_SIZE/2 + 24, //HISTOGRAM_AXIS + 50, //36,
                MOST_RECENT_SPACING = 50;

            function isVisible(data) {
                return (that.id === data.renderer) && (that.mouseState.clickState.tilekey === data.tilekey);
            }

            function getMaxPercentage(data, type) {
                var i,
                    percent,
                    maxPercent = 0,
                    tagIndex = that.mouseState.clickState.binData.index,
                    count = data.bin.value[tagIndex].count;
                if (count === 0) {
                    return 0;
                }
                for (i=0; i<24; i++) {
                    // get maximum percent
                    percent = data.bin.value[tagIndex][type + 'ByTime'][i] / count;
                    if (percent > maxPercent) {
                        maxPercent = percent;
                    }
                }
                return maxPercent;
            }

            function getMaxPercentageBoth(data) {
                var maxPositive = getMaxPercentage(data, 'positive'),
                    maxNegative = getMaxPercentage(data, 'negative');
                return (maxPositive > maxNegative) ? maxPositive : maxNegative;
            }

            function formatText(str) {
                var CHAR_PER_LINE = 35,
                    MAX_NUM_LINES = 3,
                    strArray = str.split(" "),
                    formatted = '',
                    spaceLeft = CHAR_PER_LINE,
                    i,
                    lineCount = 0;

                for (i=0; i<strArray.length; i++) {

                    while (strArray[i].length+1 > spaceLeft) {
                        if (lineCount === MAX_NUM_LINES-1) {
                            return formatted += strArray[i].substr(0, spaceLeft-3) + "..."
                        }
                        formatted += strArray[i].substr(0, spaceLeft-1) + "-\n";
                        strArray[i] = strArray[i].substr(spaceLeft-1, strArray[i].length-1);
                        spaceLeft = CHAR_PER_LINE;
                        lineCount++;
                    }
                    formatted += strArray[i] + ' ';
                    spaceLeft -= strArray[i].length+1;
                }
                return formatted;
            }


            function barTemplate( colour ) {

                var bar = that.plotLayer.addLayer(aperture.BarLayer);
                bar.map('visible').from(function(){return isVisible(this)});
                bar.map('fill').asValue(colour);
                bar.map('orientation').asValue('vertical');
                bar.map('bar-count').asValue(24)
                bar.map('width').asValue(9);
                bar.map('stroke').asValue("#000000");
                bar.map('stroke-width').asValue(2);
                bar.map('offset-x').from( function(index) {
                    return DETAILS_OFFSET_X + 20 + index*9;
                });
                return bar;
            }


            function lineTemplate( colour, yOffset ) {

                var line = that.plotLayer.addLayer(aperture.LabelLayer);
                line.map('visible').from(function(){return isVisible(this)});
                line.map('fill').asValue(colour);
                line.map('label-count').asValue(1);
                line.map('text-anchor').asValue('middle');
                line.map('text-anchor-y').asValue('middle');
                line.map('font-outline-width').asValue(0);
                line.map('font-size').asValue(12);
                line.map('text').asValue('..........................................................................');
                line.map('offset-y').asValue(yOffset-5);
                line.map('offset-x').asValue(DETAILS_OFFSET_X+that.TILE_SIZE/2);
                return line;
            }


            function labelTemplate() {
                var label = that.plotLayer.addLayer(aperture.LabelLayer);
                label.map('visible').from(function(){return isVisible(this)});
                label.map('fill').asValue('#FFFFFF');
                label.map('label-count').asValue(1);
                label.map('text-anchor').asValue('start');
                label.map('font-outline').asValue('#000000');
                label.map('font-outline-width').asValue(3);
                return label;
            }

            // BACKGROUND FOR DETAILS
            this.detailsBackground = this.plotLayer.addLayer(aperture.BarLayer);
            this.detailsBackground.map('visible').from(function(){return isVisible(this)});
            //this.detailsBackground.map('opacity').asValue(0.4);
            this.detailsBackground.map('fill').asValue('#000000');
            this.detailsBackground.map('orientation').asValue('horizontal');
            this.detailsBackground.map('bar-count').asValue(1);
            this.detailsBackground.map('width').asValue(this.TILE_SIZE*2 - 2);
            this.detailsBackground.map('length').asValue(this.TILE_SIZE - 2);
            this.detailsBackground.map('offset-y').asValue(DETAILS_OFFSET_Y + 1);
            this.detailsBackground.map('offset-x').asValue(DETAILS_OFFSET_X + 1);

            // TITLE LABELS
            this.titleLabels = labelTemplate();
            this.titleLabels.map('label-count').asValue(3);
            this.titleLabels.map('text').from(function(index) {
                switch (index) {
                    case 0:
                        var str = that.mouseState.clickState.binData.tag;
                        if (str.length > 16) {
                            str = str.substr(0,16) + "...";
                        }
                        return str;
                    case 1:
                        return "Last 24 hours";
                    default:
                        return "Most Recent";
                }
            });
            this.titleLabels.map('font-size').asValue(24);
            this.titleLabels.map('offset-y').from(function(index) {
                switch(index) {
                    case 0:
                        return DETAILS_OFFSET_Y + 24;
                    case 1:
                        return DETAILS_OFFSET_Y + 72;
                    default:
                        return MOST_RECENT;
                }
            });
            this.titleLabels.map('offset-x').asValue(DETAILS_OFFSET_X + H_SPACING);


            // TRANSLATE LABEL
            // TODO: IMPLEMENT FUNCTIONALITY WITH GOOGLE TRANSLATE API
            /*
            this.translateLabel = labelTemplate();
            this.translateLabel.map('visible').from(function(){return isVisible(this)});
            this.translateLabel.map('fill').asValue('#999999');
            this.translateLabel.map('font-size').asValue(16);
            this.translateLabel.map('text').asValue('translate');
            this.translateLabel.map('offset-y').asValue(DETAILS_OFFSET_Y + 48);
            this.translateLabel.map('offset-x').asValue(DETAILS_OFFSET_X + 28);
            */

            this.positiveLabel = labelTemplate();
            this.positiveLabel.map('visible').from(function(){return isVisible(this)});
            this.positiveLabel.map('fill').asValue('#09CFFF');
            this.positiveLabel.map('font-size').asValue(16);
            this.positiveLabel.map('text').asValue('positive tweets');
            this.positiveLabel.map('offset-y').asValue(BAR_CENTRE_LINE - BAR_LENGTH - V_SPACING - 2);
            this.positiveLabel.map('offset-x').asValue(DETAILS_OFFSET_X + H_SPACING*2);

            this.negativeLabel = labelTemplate();
            this.negativeLabel.map('visible').from(function(){return isVisible(this)});
            this.negativeLabel.map('fill').asValue('#D33CFF');
            this.negativeLabel.map('font-size').asValue(16);
            this.negativeLabel.map('text').asValue('negative tweets');
            this.negativeLabel.map('offset-y').asValue(BAR_CENTRE_LINE + BAR_LENGTH + V_SPACING);
            this.negativeLabel.map('offset-x').asValue(DETAILS_OFFSET_X + H_SPACING*2);

            this.line1 = lineTemplate('#FFFFFF', BAR_CENTRE_LINE);

            // negative bar
            this.detailsNegativeBar = barTemplate('#D33CFF');
            this.detailsNegativeBar.map('offset-y').asValue(BAR_CENTRE_LINE);
            this.detailsNegativeBar.map('length').from(function (index) {
                var maxPercentage = getMaxPercentageBoth(this);
                if (maxPercentage === 0) { return 0; }
                return (that.getExclusiveCountPercentage(this, index, 'negative') / maxPercentage) * BAR_LENGTH;
            });

            // positive bar
            this.detailsPositiveBar = barTemplate('#09CFFF');
            this.detailsPositiveBar.map('offset-y').from(function (index) {
                var maxPercentage = getMaxPercentageBoth(this);
                if (maxPercentage === 0) { return 0; }
                return BAR_CENTRE_LINE-((that.getExclusiveCountPercentage(this, index, 'positive') / maxPercentage) * BAR_LENGTH);
            });
            this.detailsPositiveBar.map('length').from(function (index) {
                var maxPercentage = getMaxPercentageBoth(this);
                if (maxPercentage === 0) { return 0; }
                return (that.getExclusiveCountPercentage(this, index, 'positive') / maxPercentage) * BAR_LENGTH;
            });


            this.timeAxisLabel = that.plotLayer.addLayer(aperture.LabelLayer);
            this.timeAxisLabel.map('visible').from(function(){return isVisible(this)});
            this.timeAxisLabel.map('fill').asValue('#FFFFFF');
            this.timeAxisLabel.map('text').from(function(index) {
                switch (index) {
                    case 1: return "6am";
                    case 2: return "12pm";
                    case 3: return "6pm";
                    default: return "12am";
                }
            });
            this.timeAxisLabel.map('label-count').asValue(5);
            this.timeAxisLabel.map('text-anchor').asValue('middle');
            this.timeAxisLabel.map('font-outline').asValue('#000000');
            this.timeAxisLabel.map('font-outline-width').asValue(3);
            this.timeAxisLabel.map('offset-y').asValue(HISTOGRAM_AXIS + 10);
            this.timeAxisLabel.map('offset-x').from(function(index) {
                return DETAILS_OFFSET_X + H_SPACING*2 + 50*index;
            });


            this.timeAxisTicks = that.plotLayer.addLayer(aperture.BarLayer);
            this.timeAxisTicks.map('visible').from(function(){return isVisible(this)});
            this.timeAxisTicks.map('orientation').asValue('vertical');
            this.timeAxisTicks.map('fill').asValue('#FFFFFF');
            this.timeAxisTicks.map('length').asValue(6);
            this.timeAxisTicks.map('width').asValue(3);
            this.timeAxisTicks.map('bar-count').asValue(5);
            this.timeAxisTicks.map('stroke').asValue("#000000");
            this.timeAxisTicks.map('stroke-width').asValue(1);
            this.timeAxisTicks.map('offset-y').asValue(HISTOGRAM_AXIS);
            this.timeAxisTicks.map('offset-x').from( function(index) {
                return DETAILS_OFFSET_X + 24 + 51.5*index;
            });


            this.recentTweetsLabel = labelTemplate();
            this.recentTweetsLabel.map('visible').from(function(){return isVisible(this)});
            this.recentTweetsLabel.map('label-count').from( function() {
                var length = this.bin.value[that.mouseState.clickState.binData.index].recent.length;
                if (length === undefined ||
                    length === 0 ||
                    isNaN(length)) {
                    return 0;
                }
                return (length > 4) ? 4 : length;
            });
            this.recentTweetsLabel.map('font-size').asValue(10);
            this.recentTweetsLabel.map('text').from( function(index) {
                return formatText(this.bin.value[that.mouseState.clickState.binData.index].recent[index].tweet);
            });
            this.recentTweetsLabel.map('offset-y').from( function(index) {
                return MOST_RECENT + 45 + (index * MOST_RECENT_SPACING);
            });
            this.recentTweetsLabel.map('offset-x').asValue(DETAILS_OFFSET_X + H_SPACING*2);
            this.recentTweetsLabel.map('width').asValue(200);

            this.line2 = lineTemplate('#FFFFFF', MOST_RECENT + 20);
            this.line3 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING);
            this.line4 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING*2);
            this.line4 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING*3);
        }


    });

    return DetailsOnDemand;
});
