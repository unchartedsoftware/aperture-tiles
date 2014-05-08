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
        DetailsOnDemandTranslate;



    DetailsOnDemandTranslate = TwitterTagRenderer.extend({
        ClassName: "DetailsOnDemandTranslate",

        init: function(id, map) {
            this._super(id, map, true);
            this.DETAILS_OFFSET_X = this.X_CENTRE_OFFSET + this.TILE_SIZE/2;
            this.DETAILS_OFFSET_Y = -this.TILE_SIZE/2;
            this.MOST_RECENT = this.DETAILS_OFFSET_Y + this.TILE_SIZE + this.VERTICAL_BUFFER,
            this.MOST_RECENT_SPACING = 50;
            this.MAX_NUM_RECENT_TWEETS = 4;
            this.BAR_LINE_SPACING = 80;
            this.MONTH_LINE_Y = this.DETAILS_OFFSET_Y + 64;
            this.WEEK_LINE_Y = this.MONTH_LINE_Y + this.BAR_LINE_SPACING ;
            this.DAY_LINE_Y = this.MONTH_LINE_Y + this.BAR_LINE_SPACING * 2;
            this.MAX_BAR_LENGTH = 20;
        },


        getParentCount: function(data, type) {

            var tagIndex = this.clientState.clickState.userData.index,
                i, length, count = 0;

            switch(type) {

                case 'Daily':

                    count = data.bin.value[tagIndex].countMonthly;
                    break;

                case 'Per6hrs':

                    length = this.getTotalDaysInMonth(data);
                    for (i = 0; i<7; i++) {
                        count += data.bin.value[tagIndex].countDaily[length - 1 - i];
                    }                  
                    break;

                case 'PerHour':
                default:

                    length = this.getTotalDaysInMonth(data);
                    count = data.bin.value[tagIndex].countDaily[length - 1]; 
                    break;
            } 

            if (count === 0 && type !== 'PerHour') {
                console.log(count);
            }

            return count;
        },


        getCountPercentage: function(data, index, type) {

            var attrib = 'count' + type,
                tagIndex = this.clientState.clickState.userData.index,
                count = this.getParentCount(data, type);
            if (count === 0) {
                return 0;
            }
            return data.bin.value[tagIndex][attrib][index] / count;
        },


         getMaxPercentage: function(data, type) {
            var i,
                percent,
                maxPercent = 0,
                tagIndex = this.clientState.clickState.userData.index,
                count = this.getParentCount(data, type);
            if (count === 0) {
                return 0;
            }

            for (i=0; i<data.bin.value[tagIndex]['count' + type].length; i++) {
                // get maximum percent
                percent = data.bin.value[tagIndex]['count' + type][i] / count;
                if (percent > maxPercent) {
                    maxPercent = percent;
                }
            }
            
            return maxPercent;
        },

        onHover: function(event, id) {
            this.clientState.setHoverState(event.data.tilekey, {
                index :  event.index[0],
                id: id
            });
        },


        onHoverOff: function(event) {
            this.clientState.clearHoverState();
        },


        isDoDVisible: function(data) {
            var that = this;
            return that.isSelectedView(data) && 
                   that.isVisible(data) && 
                   (that.clientState.clickState.tilekey === data.tilekey);
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


        createBackgroundPanel: function() {

            var that = this;
            this.detailsBackground = this.plotLayer.addLayer(aperture.BarLayer);
            this.detailsBackground.map('visible').from(function(){return that.isDoDVisible(this)});
            this.detailsBackground.map('fill').asValue('#111111');
            this.detailsBackground.map('orientation').asValue('horizontal');
            this.detailsBackground.map('bar-count').asValue(1);
            this.detailsBackground.map('width').asValue(this.TILE_SIZE*2 - 2);
            this.detailsBackground.map('length').asValue(this.TILE_SIZE - 2);
            this.detailsBackground.map('offset-y').asValue(this.DETAILS_OFFSET_Y + 1);
            this.detailsBackground.map('offset-x').asValue(this.DETAILS_OFFSET_X + 1);
            this.detailsBackground.on('click', function() { return true; }); //swallow event
            this.detailsBackground.map('opacity').from( function() {
                    return that.clientState.opacity;
                })
        },


        createTitleLabels: function() {

            var that = this;

            this.titleLabels = this.createLabel(this.WHITE_COLOUR);
            this.titleLabels.map('label-count').asValue(4);
            this.titleLabels.map('text').from(function(index) {
                switch (index) {
                    case 0:
                        return "Last Month";
                    case 1:
                        return "Last Week";
                    case 2:
                        return "Last 24 hours";
                    default:
                        return "Most Recent";
                }
            });
            this.titleLabels.map('font-size').asValue(20);
            this.titleLabels.map('offset-y').from(function(index) {
                switch(index) {
                    case 0:
                        return that.MONTH_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    case 1:
                        return that.WEEK_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    case 2:
                        return that.DAY_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    default:
                        return that.MOST_RECENT;
                }
            });
            this.titleLabels.map('offset-x').asValue(this.DETAILS_OFFSET_X + this.HORIZONTAL_BUFFER);
        },


        createPartitionBar:  function(colour, yOffset) {

            var that = this,
                bar = that.plotLayer.addLayer(aperture.BarLayer);

            bar.on('click', function() { return true; }); //swallow event
            bar.map('visible').from(function(){ return that.isDoDVisible(this); });
            bar.map('fill').asValue(colour);
            bar.map('orientation').asValue('horizontal');
            bar.map('bar-count').asValue(1)
            bar.map('length').asValue(that.TILE_SIZE - that.HORIZONTAL_BUFFER*2);
            bar.map('width').asValue(1);
            bar.map('offset-x').asValue(that.DETAILS_OFFSET_X+that.HORIZONTAL_BUFFER);
            bar.map('offset-y').asValue(yOffset-1);
            bar.map('opacity').from( function() { return that.clientState.opacity; });
            return bar;
        },


        createAxis: function(type) {

            var that = this,
                AXIS_OFFSET = 2,
                MONTH_X_SPACING = 50,
                WEEK_X_SPACING = 35,
                DAY_X_SPACING = 50,
                TICK_LENGTH = 6,
                TICK_WIDTH = 3,
                LABEL_TICK_OFFSET = 10,
                labels, ticks;

            // TIME AXIS LABELS
            labels = that.plotLayer.addLayer(aperture.LabelLayer);
            labels.on('click', function() { return true; }); //swallow event
            labels.map('visible').from(function(){return that.isDoDVisible(this); });
            labels.map('fill').asValue(this.WHITE_COLOUR);
            labels.map('text-anchor').asValue('middle');
            labels.map('font-outline').asValue(this.BLACK_COLOUR);
            labels.map('font-outline-width').asValue(3);
            labels.map('opacity').from( function() { return that.clientState.opacity; });

            // TIME AXIS TICKS
            ticks = that.plotLayer.addLayer(aperture.BarLayer);
            ticks.map('visible').from(function(){return that.isDoDVisible(this); });
            ticks.map('orientation').asValue('vertical');
            ticks.map('fill').asValue(this.WHITE_COLOUR);
            ticks.map('length').asValue(TICK_LENGTH);
            ticks.map('width').asValue(TICK_WIDTH);           
            ticks.map('stroke').asValue(this.BLACK_COLOUR);
            ticks.map('stroke-width').asValue(1);
            ticks.map('opacity').from( function() { return that.clientState.opacity; });

            switch (type) {

                case 'month':

                    labels.map('offset-y').asValue(that.MONTH_LINE_Y + AXIS_OFFSET + LABEL_TICK_OFFSET);
                    labels.map('offset-x').from(function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2 + MONTH_X_SPACING*index;
                    });
                    labels.map('label-count').asValue(5);
                    labels.map('text').from(function(index) {
                        switch (index) {
                            case 1: return "Apr 01";
                            case 2: return "Apr 07";
                            case 3: return "Apr 14";
                            case 4: return "Apr 21";
                            default: return "Apr 28";
                        }
                    });
                    this.monthAxisLabels = labels; 

                    ticks.map('bar-count').asValue(5);
                    ticks.map('offset-y').asValue(that.MONTH_LINE_Y + AXIS_OFFSET);
                    ticks.map('offset-x').from( function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2 + MONTH_X_SPACING*index + 1.5;
                    });
                    this.monthAxisTicks = ticks;
                    break;

                case 'week':

                    labels.map('offset-y').asValue(that.WEEK_LINE_Y + AXIS_OFFSET + LABEL_TICK_OFFSET);
                    labels.map('offset-x').from(function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER * 1.5 + WEEK_X_SPACING*index;
                    });
                    labels.map('label-count').asValue(7);
                    labels.map('text').from(function(index) {
                        switch (index) {
                            case 1: return "Mo";
                            case 2: return "Tu";
                            case 3: return "We";
                            case 4: return "Th";
                            case 5: return "Fr";
                            case 6: return "Sa";
                            case 7: return "Su";
                            default: return "12am";
                        }
                    });
                    this.weekAxisLabels = labels; 

                    ticks.map('bar-count').asValue(7);
                    ticks.map('offset-y').asValue(that.WEEK_LINE_Y + AXIS_OFFSET);
                    ticks.map('offset-x').from( function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER * 1.5 + WEEK_X_SPACING*index + 1.5;
                    });
                    this.weekAxisTicks = ticks;
                    break;

                case 'day':
                default: 

                    labels.map('offset-y').asValue(that.DAY_LINE_Y + AXIS_OFFSET + LABEL_TICK_OFFSET);
                    labels.map('offset-x').from(function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2 + DAY_X_SPACING*index;
                    });
                    labels.map('label-count').asValue(5);
                    labels.map('text').from(function(index) {
                        switch (index) {
                            case 1: return "6am";
                            case 2: return "12pm";
                            case 3: return "6pm";
                            default: return "12am";
                        }
                    });
                    this.dayAxisLabels = labels; 

                    ticks.map('bar-count').asValue(5);
                    ticks.map('offset-y').asValue(that.DAY_LINE_Y + AXIS_OFFSET);
                    ticks.map('offset-x').from( function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2 + DAY_X_SPACING*index + 1.5;
                    });
                    this.dayAxisTicks = ticks;
                    break;
            }


        },


        createGraphBars: function( defaultColour, selectedColour ) {

            var that = this,
                bar = that.plotLayer.addLayer(aperture.BarLayer);

            bar.on('click', function() { return true; }); //swallow event
            bar.map('visible').from(function(){return that.isDoDVisible(this); });
            bar.map('fill').from( function(index) {
                /*if ( that.clientState.hoverState.userData !== undefined &&
                     that.clientState.hoverState.userData.index === index) {
                    return selectedColour;
                }*/
                return defaultColour;
            });
            bar.map('orientation').asValue('vertical');                   
            bar.map('stroke').asValue(that.BLACK_COLOUR);
            bar.map('stroke-width').asValue(2);            
            bar.map('opacity').from( function() { return that.clientState.opacity; })
            return bar;
        },


        createBarChart: function(type) {

            var that = this,
                MONTH_LINE_Y = that.DETAILS_OFFSET_Y + 64,
                WEEK_LINE_Y = that.DETAILS_OFFSET_Y + 128,
                DAY_LINE_Y = that.DETAILS_OFFSET_Y + 192,
                bars;

            bars = this.createGraphBars(this.POSITIVE_COLOUR, this.POSITIVE_SELECTED_COLOUR);

            switch (type) {

                case 'month':

                    bars.map('bar-count').from(function() {
                        return that.getTotalDaysInMonth(this)
                    });
                    bars.map('offset-y').from(function (index) {
                        var maxPercentage = that.getMaxPercentage(this, 'Daily');
                        if (maxPercentage === 0) { return 0; }
                        return that.MONTH_LINE_Y-((that.getCountPercentage(this, index, 'Daily') / maxPercentage) * that.MAX_BAR_LENGTH)-2;
                    });
                    bars.map('length').from(function (index) {
                        var maxPercentage = that.getMaxPercentage(this, 'Daily');
                        if (maxPercentage === 0) { return 0; }
                        return (that.getCountPercentage(this, index, 'Daily') / maxPercentage) * that.MAX_BAR_LENGTH;
                    });
                    bars.map('offset-x').from( function(index) {
                        return that.DETAILS_OFFSET_X + 18 + index* ((that.TILE_SIZE - 20) / that.getTotalDaysInMonth(this));
                    });
                    bars.map('width').from(function() {
                        return (that.TILE_SIZE - 20) / that.getTotalDaysInMonth(this);
                    });
                    this.monthBars = bars;
                    this.monthBarLine = this.createPartitionBar(this.GREY_COLOUR, this.MONTH_LINE_Y);
                    break;

                case 'week':

                    bars.map('bar-count').asValue(28);
                    bars.map('offset-y').from(function (index) {
                        var maxPercentage = that.getMaxPercentage(this, 'Per6hrs');
                        if (maxPercentage === 0) { return 0; }
                        return that.WEEK_LINE_Y-((that.getCountPercentage(this, index, 'Per6hrs') / maxPercentage) * that.MAX_BAR_LENGTH)-2;
                    });
                    bars.map('length').from(function (index) {
                        var maxPercentage = that.getMaxPercentage(this, 'Per6hrs');
                        if (maxPercentage === 0) { return 0; }
                        return (that.getCountPercentage(this, index, 'Per6hrs') / maxPercentage) * that.MAX_BAR_LENGTH;
                    });
                    bars.map('offset-x').from( function(index) {
                        return that.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER + 2 + index*8;
                    });
                    bars.map('width').asValue(8);
                    this.weekBars = bars;
                    this.weekBarLine = this.createPartitionBar(this.GREY_COLOUR, this.WEEK_LINE_Y);
                    break;

                case 'day':
                default: 

                    bars.map('bar-count').asValue(24);
                    bars.map('offset-y').from(function (index) {
                        var maxPercentage = that.getMaxPercentage(this, 'PerHour');
                        if (maxPercentage === 0) { return 0; }
                        return that.DAY_LINE_Y-((that.getCountPercentage(this, index, 'PerHour') / maxPercentage) * that.MAX_BAR_LENGTH)-2;
                    });
                    bars.map('length').from(function (index) {
                        var maxPercentage = that.getMaxPercentage(this, 'PerHour');
                        if (maxPercentage === 0) { return 0; }
                        return (that.getCountPercentage(this, index, 'PerHour') / maxPercentage) * that.MAX_BAR_LENGTH;
                    });
                     bars.map('offset-x').from( function(index) {
                        return that.DETAILS_OFFSET_X + 20 + index*9;
                    });
                    bars.map('width').asValue(9);
                    this.dayBars = bars;
                    this.dayBarLine = this.createPartitionBar(this.GREY_COLOUR, this.DAY_LINE_Y);
                    break;

            }
        },


        createLabel : function(colour) {

            var that = this,
                label = that.plotLayer.addLayer(aperture.LabelLayer);

            label.on('click', function() { return true; }); //swallow event
            label.map('visible').from(function(){return that.isDoDVisible(this)});
            label.map('fill').asValue(colour);
            label.map('label-count').asValue(1);
            label.map('text-anchor').asValue('start');
            label.map('font-outline').asValue(that.BLACK_COLOUR);
            label.map('font-outline-width').asValue(3);
            label.map('opacity').from( function() { return that.clientState.opacity; });
            return label;
        },


        formatRecentTweetText: function(str, charPerLine) {
            var CHAR_PER_LINE = charPerLine || 35,
                MAX_NUM_LINES = 3,
                strArray = str.substring(1, str.length - 2).split(" "),
                formatted = '',
                spaceLeft = CHAR_PER_LINE,
                i,
                lineCount = 0;

            for (i=0; i<strArray.length; i++) {

                while (strArray[i].length > spaceLeft) {

                    // if past maximum amount of lines, truncate
                    if (lineCount === MAX_NUM_LINES-1) {
                        // strip space if is als character of string
                        if (formatted[formatted.length-1] === ' ') {
                            formatted = formatted.substring(0, formatted.length - 1);
                        }
                        return formatted += strArray[i].substr(0, spaceLeft-3) + "..."
                    }

                    if (strArray[i].length < CHAR_PER_LINE) {
                        // can fit in next line, put new line
                        formatted += "\n";
                    } else {
                        // cannot fit in next line, hyphenate word
                        formatted += strArray[i].substr(0, spaceLeft);
                        strArray[i] = strArray[i].substr(spaceLeft);
                        if (spaceLeft > 0) {
                            formatted += "-\n";
                        }
                    }
                    spaceLeft = CHAR_PER_LINE;
                    lineCount++;
                }
                formatted += strArray[i] + ' ';
                spaceLeft -= strArray[i].length+1;
            }
            return formatted;
        },


        getRecentTweetsCount: function(data) {
            var length = data.bin.value[this.clientState.clickState.userData.index].recentTweets.length;
            if (length === undefined || isNaN(length)) {
                return 0;
            }
            return (length > this.MAX_NUM_RECENT_TWEETS) ? this.MAX_NUM_RECENT_TWEETS : length;
        },


        createMostRecentTweets: function() {

            var that= this;

            // MOST RECENT TWEETS LABELS
            this.recentTweetsLabels = this.createLabel(that.WHITE_COLOUR);
            this.recentTweetsLabels.map('visible').from(function(){return that.isDoDVisible(this); });
            this.recentTweetsLabels.map('label-count').from( function() {
                return that.getRecentTweetsCount(this);
            });
            this.recentTweetsLabels.map('fill').from( function(index) {
                if (that.clientState.hoverState.userData !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.clientState.hoverState.userData.index === index) {
                    return that.YELLOW_COLOUR;
                } else {
                    return that.WHITE_COLOUR;
                }
            });
            this.recentTweetsLabels.map('font-size').asValue(10);
            this.recentTweetsLabels.map('text').from( function(index) {
                var tagIndex = that.clientState.clickState.userData.index,
                    filteredText = that.filterText(this.bin.value[tagIndex].recentTweets[index].tweet);

                if (that.clientState.hoverState.userData !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.clientState.hoverState.userData.index === index) {
                    return that.formatRecentTweetText(filteredText, 70);
                }
                return that.formatRecentTweetText(filteredText);
            });
            this.recentTweetsLabels.map('offset-y').from( function(index) {
                return that.MOST_RECENT + 45 + (index * that.MOST_RECENT_SPACING);
            });
            this.recentTweetsLabels.map('offset-x').asValue(this.DETAILS_OFFSET_X + this.TILE_SIZE/2);
            this.recentTweetsLabels.map('width').asValue(200);
            this.recentTweetsLabels.map('text-anchor').asValue('middle');
            this.recentTweetsLabels.on('mousemove', function(event) {
                that.onHover(event, 'detailsOnDemandRecent');
                that.recentTweetsLabels.all().where(event.data).redraw();
                return true; // swallow event, for some reason 'mousemove' on labels needs to swallow this or else it processes a mouseout
            });
            this.recentTweetsLabels.on('mouseout', function(event) {
                that.onHoverOff(event);
                that.recentTweetsLabels.all().where(event.data).redraw();
            });

            // MOST RECENT TWEETS LINES
            this.recentTweetsLines = this.createPartitionBar(this.GREY_COLOUR, 0);
            this.recentTweetsLines.map('bar-count').from( function() {
                return that.getRecentTweetsCount(this);
            });
            this.recentTweetsLines.map('offset-y').from( function(index) {
                return that.MOST_RECENT + 20 + that.MOST_RECENT_SPACING*index;
            });
        },

        createDetailsOnDemand: function() {

            this.createBackgroundPanel();
            this.createBarChart('month');
            this.createBarChart('week');
            this.createBarChart('day');
            this.createAxis('month');
            this.createAxis('week');
            this.createAxis('day');
            this.createTitleLabels();
            this.createMostRecentTweets();

/*
            var that = this,
                BAR_CENTRE_LINE = DETAILS_OFFSET_Y + this.TILE_SIZE/2 + 30,
                BAR_LENGTH = 50,
                BAR_WIDTH = 9,
                HISTOGRAM_AXIS = BAR_CENTRE_LINE + BAR_LENGTH + this.VERTICAL_BUFFER,
                MOST_RECENT = DETAILS_OFFSET_Y + this.TILE_SIZE + this.VERTICAL_BUFFER,
                MOST_RECENT_SPACING = 50;

            function isVisible(data) {
                return that.isSelectedView(data) && that.isVisible(data) && (that.clientState.clickState.tilekey === data.tilekey);
            }

            function getMaxPercentage(data, type) {
                var i,
                    percent,
                    maxPercent = 0,
                    tagIndex = that.clientState.clickState.userData.index,
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

            function formatText(str, charPerLine) {
                var CHAR_PER_LINE = charPerLine || 35,
                    MAX_NUM_LINES = 3,
                    strArray = str.split(" "),
                    formatted = '',
                    spaceLeft = CHAR_PER_LINE,
                    i,
                    lineCount = 0;

                for (i=0; i<strArray.length; i++) {

                    while (strArray[i].length > spaceLeft) {

                        // if past maximum amount of lines, truncate
                        if (lineCount === MAX_NUM_LINES-1) {
                            // strip space if is als character of string
                            if (formatted[formatted.length-1] === ' ') {
                                formatted = formatted.substring(0, formatted.length - 1);
                            }
                            return formatted += strArray[i].substr(0, spaceLeft-3) + "..."
                        }

                        if (strArray[i].length < CHAR_PER_LINE) {
                            // can fit in next line, put new line
                            formatted += "\n";
                        } else {
                            // cannot fit in next line, hyphenate word
                            formatted += strArray[i].substr(0, spaceLeft);
                            strArray[i] = strArray[i].substr(spaceLeft);
                            if (spaceLeft > 0) {
                                formatted += "-\n";
                            }
                        }
                        spaceLeft = CHAR_PER_LINE;
                        lineCount++;
                    }
                    formatted += strArray[i] + ' ';
                    spaceLeft -= strArray[i].length+1;
                }
                return formatted;
            }

            function getRecentTweetsCount(data) {
                var length = data.bin.value[that.clientState.clickState.userData.index].recent.length;
                if (length === undefined || length === 0 || isNaN(length)) {
                    return 0;
                }
                return (length > 4) ? 4 : length;
            }


            function barTemplate( defaultColour, selectedColour ) {
                var bar = that.plotLayer.addLayer(aperture.BarLayer);
                bar.on('click', function() { return true; }); //swallow event
                bar.map('visible').from(function(){return isVisible(this)});
                bar.map('fill').from( function(index) {
                    if ( that.clientState.hoverState.userData !== undefined &&
                        (that.clientState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                         that.clientState.hoverState.userData.id === 'detailsOnDemandNegative') &&
                         that.clientState.hoverState.userData.index === index) {
                        return selectedColour;
                    }
                    return defaultColour;
                });
                bar.map('orientation').asValue('vertical');
                bar.map('bar-count').asValue(24)
                bar.map('width').asValue(BAR_WIDTH);
                bar.map('stroke').asValue(that.BLACK_COLOUR);
                bar.map('stroke-width').asValue(2);
                bar.map('offset-x').from( function(index) {
                    return DETAILS_OFFSET_X + 20 + index*BAR_WIDTH;
                });
                bar.map('opacity').from( function() {
                    return that.clientState.opacity;
                })
                return bar;
            }


            function lineTemplate( colour, yOffset ) {

                var bar = that.plotLayer.addLayer(aperture.BarLayer);
                bar.on('click', function() { return true; }); //swallow event
                bar.map('visible').from(function(){return isVisible(this)});
                bar.map('fill').asValue(that.GREY_COLOUR);
                bar.map('orientation').asValue('horizontal');
                bar.map('bar-count').asValue(1)
                bar.map('length').asValue(that.TILE_SIZE - that.HORIZONTAL_BUFFER*2);
                bar.map('width').asValue(1);
                bar.map('offset-x').asValue(DETAILS_OFFSET_X+that.HORIZONTAL_BUFFER);
                bar.map('offset-y').asValue(yOffset-1);
                bar.map('opacity').from( function() {
                    return that.clientState.opacity;
                })
                return bar;
            }


            function labelTemplate() {
                var label = that.plotLayer.addLayer(aperture.LabelLayer);
                label.on('click', function() { return true; }); //swallow event
                label.map('visible').from(function(){return isVisible(this)});
                label.map('fill').asValue(that.WHITE_COLOUR);
                label.map('label-count').asValue(1);
                label.map('text-anchor').asValue('start');
                label.map('font-outline').asValue(that.BLACK_COLOUR);
                label.map('font-outline-width').asValue(3);
                label.map('opacity').from( function() {
                    return that.clientState.opacity;
                })
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
            this.detailsBackground.on('click', function() { return true; }); //swallow event
            this.detailsBackground.map('opacity').from( function() {
                    return that.clientState.opacity;
                })

            // TITLE LABELS
            this.titleLabels = labelTemplate();
            this.titleLabels.map('label-count').asValue(3);
            this.titleLabels.map('text').from(function(index) {
                switch (index) {
                    case 0:
                        var str = that.filterText(that.clientState.clickState.userData.tag);
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

            // COUNT SUMMARY LABELS
            this.summaryLabel = labelTemplate();
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('visible').from(function(){return isVisible(this)});
            this.summaryLabel.map('fill').from( function(index) {
                switch(index) {
                    case 0: return that.POSITIVE_COLOUR;
                    case 1: return that.WHITE_COLOUR;
                    default: return that.NEGATIVE_COLOUR;
                }
            });
            this.summaryLabel.map('text').from( function(index) {
                var tagIndex = that.clientState.clickState.userData.index;
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
            this.summaryLabel.map('opacity').from( function() {
                    return that.clientState.opacity;
                });

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
            this.line1 = lineTemplate(this.WHITE_COLOUR, BAR_CENTRE_LINE);

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
            this.countLabels.on('click', function() { return true; }); //swallow event
            this.countLabels.map('font-outline-width').asValue(3);
            this.countLabels.map('font-size').asValue(12);
            this.countLabels.map('visible').from(function(){
                return isVisible(this) &&
                     that.clientState.hoverState.userData.id !== undefined &&
                    (that.clientState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                     that.clientState.hoverState.userData.id === 'detailsOnDemandNegative') &&
                     that.clientState.hoverState.tilekey === this.tilekey;
            });

            this.countLabels.map('fill').asValue(this.WHITE_COLOUR);
            this.countLabels.map('text').from(function(index) {

                var tagIndex, timeIndex, positive, neutral, negative;
                if (index === 0) {
                    return "positive:\n" +
                           "neutral:\n" +
                           "negative:\n" +
                           "total: "
                } else {
                    if (that.clientState.hoverState.userData.index !== undefined) {
                        tagIndex = that.clientState.clickState.userData.index;
                        timeIndex = that.clientState.hoverState.userData.index;
                        if (that.clientState.hoverState.userData.id !== undefined &&
                           (that.clientState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                            that.clientState.hoverState.userData.id === 'detailsOnDemandNegative')) {
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
            this.countLabels.map('font-outline').asValue(this.BLACK_COLOUR);
            this.countLabels.map('font-outline-width').asValue(3);
            this.countLabels.map('offset-y').from( function() {
                if (that.clientState.hoverState.userData.id !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandPositive') {
                    return BAR_CENTRE_LINE - 30;
                }
                return BAR_CENTRE_LINE + 30;
            });
            this.countLabels.map('offset-x').from( function(index) {
                if (that.clientState.hoverState.userData !== undefined) {
                    if (index === 1) {
                        return DETAILS_OFFSET_X + that.clientState.hoverState.userData.index*BAR_WIDTH + 94;
                    }
                    return DETAILS_OFFSET_X + that.clientState.hoverState.userData.index*BAR_WIDTH + 40;
                }

            });
            this.countLabels.map('opacity').from( function() {
                    return that.clientState.opacity;
                });

            // TIME AXIS LABEL
            this.timeAxisLabel = that.plotLayer.addLayer(aperture.LabelLayer);
            this.timeAxisLabel.on('click', function() { return true; }); //swallow event
            this.timeAxisLabel.map('visible').from(function(){return isVisible(this)});
            this.timeAxisLabel.map('fill').asValue(this.WHITE_COLOUR);
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
            this.timeAxisLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.timeAxisLabel.map('font-outline-width').asValue(3);
            this.timeAxisLabel.map('offset-y').asValue(HISTOGRAM_AXIS + 10);
            this.timeAxisLabel.map('offset-x').from(function(index) {
                return DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER*2 + 50*index;
            });
            this.timeAxisTicks = that.plotLayer.addLayer(aperture.BarLayer);
            this.timeAxisTicks.map('visible').from(function(){return isVisible(this)});
            this.timeAxisTicks.map('orientation').asValue('vertical');
            this.timeAxisTicks.map('fill').asValue(this.WHITE_COLOUR);
            this.timeAxisTicks.map('length').asValue(6);
            this.timeAxisTicks.map('width').asValue(3);
            this.timeAxisTicks.map('bar-count').asValue(5);
            this.timeAxisTicks.map('stroke').asValue(this.BLACK_COLOUR);
            this.timeAxisTicks.map('stroke-width').asValue(1);
            this.timeAxisTicks.map('offset-y').asValue(HISTOGRAM_AXIS);
            this.timeAxisTicks.map('offset-x').from( function(index) {
                return DETAILS_OFFSET_X + 24 + 51.5*index;
            });
            this.timeAxisTicks.map('opacity').from( function() {
                    return that.clientState.opacity;
                });

            // MOST RECENT TWEETS LABELS
            this.recentTweetsLabel = labelTemplate();
            this.recentTweetsLabel.map('visible').from(function(){return isVisible(this)});
            this.recentTweetsLabel.map('label-count').from( function() {
                return getRecentTweetsCount(this);
            });
            this.recentTweetsLabel.map('fill').from( function(index) {
                if (that.clientState.hoverState.userData !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.clientState.hoverState.userData.index === index) {
                    return that.YELLOW_COLOUR;
                } else {
                    return that.WHITE_COLOUR;
                }
            });
            this.recentTweetsLabel.map('font-size').asValue(10);
            this.recentTweetsLabel.map('text').from( function(index) {
                var tagIndex = that.clientState.clickState.userData.index,
                    filteredText = that.filterText(this.bin.value[tagIndex].recent[index].tweet);

                if (that.clientState.hoverState.userData !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.clientState.hoverState.userData.index === index) {
                    return formatText(filteredText, 70);
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
            this.recentTweetsLines = lineTemplate(this.WHITE_COLOUR, 0);
            this.recentTweetsLines.map('bar-count').from( function() {
                return getRecentTweetsCount(this);
            });
            this.recentTweetsLines.map('offset-y').from( function(index) {
                return MOST_RECENT + 20 + MOST_RECENT_SPACING*index;
            });*/
        }
        


    });

    return DetailsOnDemandTranslate;
});
