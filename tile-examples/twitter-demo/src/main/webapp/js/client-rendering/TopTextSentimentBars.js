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
        DETAILS_POSITION = 128,
        TopTextSentimentBars;



    TopTextSentimentBars = ClientRenderer.extend({
        ClassName: "TopTextSentimentBars",

        init: function(id) {

            this._super(id);
            this.valueCount = 5;
            this.ySpacing = 30;
            this.hoverInfo = {
                tag : '',
                tilekey : '',
                index : -1
            };

            this.clickInfo = {
                tag : '',
                tilekey : '',
                index : -1
            };
        },

        onUnselect: function() {
            this.clickInfo.tag = '';
            this.clickInfo.tilekey = '';
            this.clickInfo.index = -1;
            this.plotLayer.all().redraw();
        },


        getExclusiveCountPercentage: function(data, index, type) {

            var attrib = type + 'ByTime';
            if (data.bin.value[this.clickInfo.index][type] === 0) {
                return 0;
            }
            return data.bin.value[this.clickInfo.index][attrib][index] / data.bin.value[this.clickInfo.index][type];
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


        onClick: function(event) {
            this.clickInfo.tag = event.data.bin.value[event.index[0]].tag;
            this.clickInfo.tilekey = event.data.tilekey;
            this.clickInfo.index = event.index[0];
            this.plotLayer.all().redraw();
            return true;
        },


        onHover: function(event) {
            this.hoverInfo.tag = event.data.bin.value[event.index[0]].tag;
            this.hoverInfo.tilekey = event.data.tilekey;
            this.hoverInfo.index = event.index[0];
            this.negativeBar.all().where(event.data).redraw();
            this.positiveBar.all().where(event.data).redraw();
            this.tagLabel.all().where(event.data).redraw();
            return true;
        },


        onHoverOff: function(event) {
            this.hoverInfo.tag = '';
            this.hoverInfo.tilekey = '';
            this.hoverInfo.index = -1;
            this.negativeBar.all().where(event.data).redraw();
            this.positiveBar.all().where(event.data).redraw();
            this.tagLabel.all().where(event.data).redraw();
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
            this.createDetailsOnDemand();
        },


        createBars: function() {

            var that = this,
                BAR_LENGTH = 100;

            function barTemplate( defaultColour, selectedColour ) {

                var bar = that.plotLayer.addLayer(aperture.BarLayer);

                bar.map('visible').from( function() {
                    return (that.id === this.renderer) &&
                        (that.clickInfo.tag === '' || that.clickInfo.tilekey === this.tilekey);
                });

                bar.map('fill').from( function(index) {
                    if ((that.hoverInfo.tag === this.bin.value[index].tag && that.hoverInfo.tilekey === this.tilekey) ||
                        (that.clickInfo.tag === this.bin.value[index].tag && that.clickInfo.tilekey === this.tilekey)) {
                        return selectedColour;
                    }
                    return defaultColour;
                });

                bar.on('click', function(event) {
                    return that.onClick(event);
                });

                bar.on('mousemove', function(event) {
                    return that.onHover(event);
                });

                bar.on('mouseout', function(event) {
                    that.onHoverOff(event);
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
                return bar;
            }

            // negative bar
            this.negativeBar = barTemplate('#777777', this.NEGATIVE_COLOUR);
            this.negativeBar.map('offset-x').from(function (index) {
                return -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2 +
                    -(that.getCountPercentage(this, index, 'negative') * BAR_LENGTH);
            });
            this.negativeBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'negative') * BAR_LENGTH;
            });

            // neutral bar
            this.neutralBar = barTemplate('#222222', this.NEUTRAL_COLOUR );
            this.neutralBar.map('offset-x').from(function (index) {
                return -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.neutralBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH;
            });

            // positive bar
            this.positiveBar = barTemplate('#FFFFFF', this.POSITIVE_COLOUR);
            this.positiveBar.map('offset-x').from(function (index) {
                return (that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.positiveBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'positive') * BAR_LENGTH;
            });
        },


        createLabels: function () {

            var that = this;

            this.tagLabel = this.plotLayer.addLayer(aperture.LabelLayer);

            this.tagLabel.map('visible').from(function() {
                var clickedKey = that.clickInfo.tilekey.split(','),
                    clickedX = parseInt(clickedKey[1]),
                    clickedY = parseInt(clickedKey[2]),
                    thisKey = this.tilekey.split(','),
                    thisKeyX = parseInt(thisKey[1]),
                    thisKeyY = parseInt(thisKey[2]);

                return that.id === this.renderer &&
                    // hack to prevent other labels from rendering on detailsOnDemand
                    (that.clickInfo.tag === '' || thisKeyX !== clickedX+1 ||
                    (thisKeyY !== clickedY && thisKeyY !== clickedY-1));

            });

            this.tagLabel.map('fill').from( function(index) {
                if (that.clickInfo.tag === '' && that.hoverInfo.tag === '') {
                    return '#FFFFFF';
                } else if (that.hoverInfo.tag !== '' &&
                    that.hoverInfo.tag === this.bin.value[index].tag &&
                    that.hoverInfo.tilekey === this.tilekey ) {
                    return '#FFFFFF';
                } else if (that.clickInfo.tag !== '' &&
                           that.clickInfo.tag !== this.bin.value[index].tag) {
                    return '#666666';
                }
                return '#FFFFFF';
            });

            this.tagLabel.on('click', function(event) {
                return that.onClick(event);
            });

            this.tagLabel.on('mousemove', function(event) {
                return that.onHover(event);
            });

            this.tagLabel.on('mouseout', function(event) {
                that.onHoverOff(event);
            });

            this.tagLabel.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabel.map('text').from(function (index) {
                var str = "#" + this.bin.value[index].tag;
                if (str.length > 12) {
                    str = str.substr(0,12) + "...";
                }
                return str;
            });

            this.tagLabel.map('font-size').from(function (index) {
                var size = (that.getTotalCountPercentage(this, index) * 60) + 10;
                return size > 30 ? 30 : size;
            });
            this.tagLabel.map('offset-y').from(function (index) {
                return that.getYOffset(this, index) + 5;
            });
            this.tagLabel.map('text-anchor').asValue('middle');
            this.tagLabel.map('font-outline').asValue('#000000');
            this.tagLabel.map('font-outline-width').asValue(3);
        },


        createDetailsOnDemand: function() {

            var that = this,
                V_SPACING = 10,
                H_SPACING = 14,
                BAR_CENTRE_LINE = 30,
                BAR_LENGTH = 50,
                HISTOGRAM_AXIS = BAR_CENTRE_LINE + BAR_LENGTH + V_SPACING + 12,
                MOST_RECENT = HISTOGRAM_AXIS + 36,
                MOST_RECENT_SPACING = 55;

            function isVisible(data) {
                return (that.id === data.renderer) && (that.clickInfo.tilekey === data.tilekey);
            }

            function getMaxPercentage(data, type) {
                var i,
                    percent,
                    maxPercent = 0,
                    count = data.bin.value[that.clickInfo.index][type];
                if (count === 0) {
                    return 0;
                }
                for(i=0; i<24; i++) {
                    // get maximum percent
                    percent = data.bin.value[that.clickInfo.index][type + 'ByTime'][i] / count;
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

                for(i=0; i<strArray.length; i++) {

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
                    return DETAILS_POSITION + 20 + index*9;
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
                line.map('offset-x').asValue(DETAILS_POSITION*2);
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
            this.detailsBackground.map('fill').asValue('#222222');
            this.detailsBackground.map('orientation').asValue('horizontal');
            this.detailsBackground.map('bar-count').asValue(1);
            this.detailsBackground.map('width').asValue('450');
            this.detailsBackground.map('length').asValue('256');
            this.detailsBackground.map('stroke').asValue("#000000");
            this.detailsBackground.map('stroke-width').asValue(2);
            this.detailsBackground.map('offset-y').asValue(-128);
            this.detailsBackground.map('offset-x').asValue(DETAILS_POSITION);

            // TITLE LABELS
            this.titleLabels = labelTemplate();
            this.titleLabels.map('label-count').asValue(3);
            this.titleLabels.map('text').from(function(index) {
                switch (index) {
                    case 0:
                        var str = "#" + that.clickInfo.tag;
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
                        return -105;
                    case 1:
                        return -60;
                    default:
                        return MOST_RECENT;
                }
            });
            this.titleLabels.map('offset-x').asValue(DETAILS_POSITION + H_SPACING);


            // TRANSLATE LABEL
            // TODO: IMPLEMENT FUNCTIONALITY WITH GOOGLE TRANSLATE API
            this.translateLabel = labelTemplate();
            this.translateLabel.map('visible').from(function(){return isVisible(this)});
            this.translateLabel.map('fill').asValue('#999999');
            this.translateLabel.map('font-size').asValue(16);
            this.translateLabel.map('text').asValue('translate');
            this.translateLabel.map('offset-y').asValue(-85);
            this.translateLabel.map('offset-x').asValue(DETAILS_POSITION + 28);


            this.positiveLabel = labelTemplate();
            this.positiveLabel.map('visible').from(function(){return isVisible(this)});
            this.positiveLabel.map('fill').asValue('#09CFFF');
            this.positiveLabel.map('font-size').asValue(16);
            this.positiveLabel.map('text').asValue('positive tweets');
            this.positiveLabel.map('offset-y').asValue(BAR_CENTRE_LINE - BAR_LENGTH - V_SPACING - 2);
            this.positiveLabel.map('offset-x').asValue(DETAILS_POSITION + H_SPACING*2);

            this.negativeLabel = labelTemplate();
            this.negativeLabel.map('visible').from(function(){return isVisible(this)});
            this.negativeLabel.map('fill').asValue('#D33CFF');
            this.negativeLabel.map('font-size').asValue(16);
            this.negativeLabel.map('text').asValue('negative tweets');
            this.negativeLabel.map('offset-y').asValue(BAR_CENTRE_LINE + BAR_LENGTH + V_SPACING);
            this.negativeLabel.map('offset-x').asValue(DETAILS_POSITION + H_SPACING*2);


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
                return DETAILS_POSITION + H_SPACING*2 + 50*index;
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
                return DETAILS_POSITION + 24 + 51.5*index;
            });


            this.recentTweetsLabel = labelTemplate();
            this.recentTweetsLabel.map('visible').from(function(){return isVisible(this)});
            this.recentTweetsLabel.map('label-count').from( function() {
                var length = this.bin.value[that.clickInfo.index].recent.length;
                if (length === undefined ||
                    length === 0 ||
                    isNaN(length)) {
                    return 0;
                }
                return (length > 3) ? 3 : length;
            });
            this.recentTweetsLabel.map('font-size').asValue(10);
            this.recentTweetsLabel.map('text').from( function(index) {
                return formatText(this.bin.value[that.clickInfo.index].recent[index].tweet);
            });
            this.recentTweetsLabel.map('offset-y').from( function(index) {
                return MOST_RECENT + 45 + (index * MOST_RECENT_SPACING);
            });
            this.recentTweetsLabel.map('offset-x').asValue(DETAILS_POSITION + H_SPACING*2);
            this.recentTweetsLabel.map('width').asValue(200);

            this.line1 = lineTemplate('#000000', BAR_CENTRE_LINE);
            this.line2 = lineTemplate('#FFFFFF', MOST_RECENT + 20);
            this.line3 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING);
            this.line4 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING*2);







        }


    });

    return TopTextSentimentBars;
});
