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
                tagIndex = this.mouseState.clickState.userData.index,
                count = data.bin.value[tagIndex].count;
            if (count === 0) {
                return 0;
            }
            return data.bin.value[tagIndex][attrib][index] / count;
        },


        onHover: function(event, id) {
            this.setMouseHoverState(event.data.tilekey, {
                index :  event.index[0],
                id: id
            });
            this.redrawLayers(event.data);
        },


        onHoverOff: function(event) {
            this.clearMouseHoverState();
            this.redrawLayers(event.data);
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
                BAR_CENTRE_LINE = DETAILS_OFFSET_Y + 158,
                BAR_LENGTH = 50,
                BAR_WIDTH = 9,
                HISTOGRAM_AXIS = BAR_CENTRE_LINE + BAR_LENGTH + this.VERTICAL_BUFFER,
                MOST_RECENT = this.TILE_SIZE/2 + 24,
                MOST_RECENT_SPACING = 50;

            function isVisible(data) {
                return (that.id === data.renderer) && (that.mouseState.clickState.tilekey === data.tilekey);
            }

            function getMaxPercentage(data, type) {
                var i,
                    percent,
                    maxPercent = 0,
                    tagIndex = that.mouseState.clickState.userData.index,
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

            function formatText(str, charsPerLine) {
                var CHAR_PER_LINE = charsPerLine || 35,
                    MAX_NUM_LINES = 3,
                    strArray = str.split(" "),
                    formatted = '',
                    spaceLeft = CHAR_PER_LINE,
                    i,
                    lineCount = 0;

                for (i=0; i<strArray.length; i++) {

                    while (strArray[i].length > spaceLeft) {
                        if (lineCount === MAX_NUM_LINES-1) {
                            return formatted += strArray[i].substr(0, spaceLeft-3) + "..."
                        }
                        formatted += strArray[i].substr(0, spaceLeft);
                        strArray[i] = strArray[i].substr(spaceLeft);
                        if (spaceLeft > 0) {
                            formatted += "-\n";
                        } else {
                            formatted += "\n";
                        }
                        spaceLeft = CHAR_PER_LINE;
                        lineCount++;
                    }
                    formatted += strArray[i] + ' ';
                    spaceLeft -= strArray[i].length+1;
                }
                return formatted;
            }


            function barTemplate( defaultColour, selectedColour ) {
                var bar = that.plotLayer.addLayer(aperture.BarLayer);
                bar.map('visible').from(function(){return isVisible(this)});
                bar.map('fill').from( function(index) {
                    if ( that.mouseState.hoverState.userData !== undefined &&
                        (that.mouseState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                         that.mouseState.hoverState.userData.id === 'detailsOnDemandNegative') &&
                         that.mouseState.hoverState.userData.index === index) {
                        return selectedColour;
                    }
                    return defaultColour;
                });
                bar.map('orientation').asValue('vertical');
                bar.map('bar-count').asValue(24)
                bar.map('width').asValue(BAR_WIDTH);
                bar.map('stroke').asValue("#000000");
                bar.map('stroke-width').asValue(2);
                bar.map('offset-x').from( function(index) {
                    return DETAILS_OFFSET_X + 20 + index*BAR_WIDTH;
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
            this.detailsBackground.map('fill').asValue('#111111');
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
                        var str = that.filterText(that.mouseState.clickState.userData.tag);
                        if (str.length > 15) {
                            str = str.substr(0,15) + "...";
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
                        return DETAILS_OFFSET_Y + that.VERTICAL_BUFFER;
                    case 1:
                        return DETAILS_OFFSET_Y + that.VERTICAL_BUFFER*3;
                    default:
                        return MOST_RECENT;
                }
            });
            this.titleLabels.map('offset-x').asValue(DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER);


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

            // COUNT SUMMARY LABELS
            this.summaryLabel = labelTemplate();
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('visible').from(function(){return isVisible(this)});
            this.summaryLabel.map('fill').from( function(index) {
                switch(index) {
                    case 0: return that.POSITIVE_COLOUR;
                    case 1: return '#FFFFFF';
                    default: return that.NEGATIVE_COLOUR;
                }
            });
            this.summaryLabel.map('text').from( function(index) {
                var tagIndex = that.mouseState.clickState.userData.index;
                switch(index) {
                    case 0: return "+ "+this.bin.value[tagIndex].positive;
                    case 1: return ""+this.bin.value[tagIndex].neutral;
                    default: return "- "+this.bin.value[tagIndex].negative;
                }
            });
            this.summaryLabel.map('offset-y').from(function(index) {
                return DETAILS_OFFSET_Y + (that.VERTICAL_BUFFER-4) + (14) * index;
            });
            this.summaryLabel.map('offset-x').asValue(DETAILS_OFFSET_X + that.TILE_SIZE - that.HORIZONTAL_BUFFER);
            this.summaryLabel.map('text-anchor').asValue('end');

            // POSITIVE TITLE LABEL
            this.positiveLabel = labelTemplate();
            this.positiveLabel.map('visible').from(function(){return isVisible(this)});
            this.positiveLabel.map('fill').asValue(this.POSITIVE_COLOUR);
            this.positiveLabel.map('font-size').asValue(16);
            this.positiveLabel.map('text').asValue('positive tweets');
            this.positiveLabel.map('offset-y').asValue(BAR_CENTRE_LINE - BAR_LENGTH - 12);
            this.positiveLabel.map('offset-x').asValue(DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2);

            // NEGATIVE TITLE LABEL
            this.negativeLabel = labelTemplate();
            this.negativeLabel.map('visible').from(function(){return isVisible(this)});
            this.negativeLabel.map('fill').asValue(this.NEGATIVE_COLOUR);
            this.negativeLabel.map('font-size').asValue(16);
            this.negativeLabel.map('text').asValue('negative tweets');
            this.negativeLabel.map('offset-y').asValue(BAR_CENTRE_LINE + BAR_LENGTH + 10);
            this.negativeLabel.map('offset-x').asValue(DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2);

            // AXIS CENTRE LINE
            this.line1 = lineTemplate('#FFFFFF', BAR_CENTRE_LINE);

            // NEGATIVE BAR
            this.detailsNegativeBar = barTemplate(this.NEGATIVE_COLOUR, this.NEGATIVE_SELECTED_COLOUR);
            this.detailsNegativeBar.map('offset-y').asValue(BAR_CENTRE_LINE+1);
            this.detailsNegativeBar.map('length').from(function (index) {
                var maxPercentage = getMaxPercentageBoth(this);
                if (maxPercentage === 0) { return 0; }
                return (that.getExclusiveCountPercentage(this, index, 'negative') / maxPercentage) * BAR_LENGTH;
            });
            this.detailsNegativeBar.on('mousemove', function(event) {
                that.onHover(event, 'detailsOnDemandNegative');
                that.detailsNegativeBar.all().where(event.data).redraw();
                that.countLabels.all().redraw();
            });
            this.detailsNegativeBar.on('mouseout', function(event) {
                that.onHoverOff(event);
                that.detailsNegativeBar.all().where(event.data).redraw();
                that.countLabels.all().redraw();
            });

            // POSITIVE BAR
            this.detailsPositiveBar = barTemplate(this.POSITIVE_COLOUR, this.POSITIVE_SELECTED_COLOUR);
            this.detailsPositiveBar.map('offset-y').from(function (index) {
                var maxPercentage = getMaxPercentageBoth(this);
                if (maxPercentage === 0) { return 0; }
                return BAR_CENTRE_LINE-((that.getExclusiveCountPercentage(this, index, 'positive') / maxPercentage) * BAR_LENGTH)-2;
            });
            this.detailsPositiveBar.map('length').from(function (index) {
                var maxPercentage = getMaxPercentageBoth(this);
                if (maxPercentage === 0) { return 0; }
                return (that.getExclusiveCountPercentage(this, index, 'positive') / maxPercentage) * BAR_LENGTH;
            });
            this.detailsPositiveBar.on('mousemove', function(event) {
                that.onHover(event, 'detailsOnDemandPositive');
                that.detailsPositiveBar.all().where(event.data).redraw();
                that.countLabels.all().redraw();
            });
            this.detailsPositiveBar.on('mouseout', function(event) {
                that.onHoverOff(event);
                that.detailsPositiveBar.all().where(event.data).redraw();
                that.countLabels.all().redraw();
            });

            // HOVER COUNT LABELS
            this.countLabels = that.plotLayer.addLayer(aperture.LabelLayer);
            this.countLabels.map('font-outline-width').asValue(3);
            this.countLabels.map('font-size').asValue(12);
            this.countLabels.map('visible').from(function(){
                return isVisible(this) &&
                     that.mouseState.hoverState.userData.id !== undefined &&
                    (that.mouseState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                     that.mouseState.hoverState.userData.id === 'detailsOnDemandNegative') &&
                     that.mouseState.hoverState.tilekey === this.tilekey;
            });

            this.countLabels.map('fill').asValue('#FFFFFF');
            this.countLabels.map('text').from(function(index) {

                var tagIndex, timeIndex, positive, neutral, negative;
                if (index === 0) {
                    return "positive:\n" +
                           "neutral:\n" +
                           "negative:\n" +
                           "total: "
                } else {
                    if (that.mouseState.hoverState.userData.index !== undefined) {
                        tagIndex = that.mouseState.clickState.userData.index;
                        timeIndex = that.mouseState.hoverState.userData.index;
                        if (that.mouseState.hoverState.userData.id !== undefined &&
                           (that.mouseState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                            that.mouseState.hoverState.userData.id === 'detailsOnDemandNegative')) {
                            positive =  this.bin.value[tagIndex].positiveByTime[timeIndex];
                            neutral =  this.bin.value[tagIndex].neutralByTime[timeIndex];
                            negative =  this.bin.value[tagIndex].negativeByTime[timeIndex];
                            return positive + "\n" +
                                   neutral + "\n" +
                                   negative + "\n" +
                                   (positive + neutral + negative);
                        }
                    }
                }
            });
            this.countLabels.map('label-count').asValue(2);
            this.countLabels.map('text-anchor').asValue('start');
            this.countLabels.map('font-outline').asValue('#000000');
            this.countLabels.map('font-outline-width').asValue(3);
            this.countLabels.map('offset-y').from( function() {
                if (that.mouseState.hoverState.userData.id !== undefined &&
                    that.mouseState.hoverState.userData.id === 'detailsOnDemandPositive') {
                    return BAR_CENTRE_LINE - 30;
                }
                return BAR_CENTRE_LINE + 30;
            });
            this.countLabels.map('offset-x').from( function(index) {
                if (that.mouseState.hoverState.userData !== undefined) {
                    if (index === 1) {
                        return DETAILS_OFFSET_X + that.mouseState.hoverState.userData.index*BAR_WIDTH + 94;
                    }
                    return DETAILS_OFFSET_X + that.mouseState.hoverState.userData.index*BAR_WIDTH + 40;
                }

            });

            // TIME AXIS LABEL
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
                return DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2 + 50*index;
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

            // MOST RECENT TWEETS LABELS
            this.recentTweetsLabel = labelTemplate();
            this.recentTweetsLabel.map('visible').from(function(){return isVisible(this)});
            this.recentTweetsLabel.map('label-count').from( function() {
                var length = this.bin.value[that.mouseState.clickState.userData.index].recent.length;
                if (length === undefined || length === 0 || isNaN(length)) {
                    return 0;
                }
                return (length > 4) ? 4 : length;
            });
            this.recentTweetsLabel.map('fill').from( function(index) {
                if (that.mouseState.hoverState.userData !== undefined &&
                    that.mouseState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.mouseState.hoverState.userData.index === index) {
                    return '#F5F56F';
                } else {
                    return '#FFFFFF';
                }
            });
            this.recentTweetsLabel.map('font-size').asValue(10);
            this.recentTweetsLabel.map('text').from( function(index) {
                var tagIndex = that.mouseState.clickState.userData.index,
                    filteredText = that.filterText(this.bin.value[tagIndex].recent[index].tweet);

                if (that.mouseState.hoverState.userData !== undefined &&
                    that.mouseState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.mouseState.hoverState.userData.index === index) {
                    return filteredText
                }
                return formatText(filteredText);
            });
            this.recentTweetsLabel.map('offset-y').from( function(index) {
                return MOST_RECENT + 45 + (index * MOST_RECENT_SPACING);
            });
            this.recentTweetsLabel.map('offset-x').asValue(DETAILS_OFFSET_X + that.TILE_SIZE/2);
            this.recentTweetsLabel.map('width').asValue(200);
            this.recentTweetsLabel.map('text-anchor').asValue('middle');
            this.recentTweetsLabel.on('mousemove', function(event) {
                that.onHover(event, 'detailsOnDemandRecent');
                that.recentTweetsLabel.all().where(event.data).redraw();
                return true; // swallow event, for some reason 'mousemove' on labels needs to swallow this or else it processes a mouseout
            });
            this.recentTweetsLabel.on('mouseout', function(event) {
                that.onHoverOff(event);
                that.recentTweetsLabel.all().where(event.data).redraw();
            });

            // MOST RECENT TWEETS LINES
            this.line2 = lineTemplate('#FFFFFF', MOST_RECENT + 20);
            this.line3 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING);
            this.line4 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING*2);
            this.line4 = lineTemplate('#FFFFFF', MOST_RECENT + 20 + MOST_RECENT_SPACING*3);
        }


    });

    return DetailsOnDemand;
});
