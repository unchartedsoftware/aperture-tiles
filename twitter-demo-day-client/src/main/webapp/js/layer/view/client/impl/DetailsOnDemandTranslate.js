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
            this.PARTITION_BAR_HORIZONTAL_BUFFER =  this.HORIZONTAL_BUFFER*2;
            this.PARTITION_BAR_OFFSET_X = this.DETAILS_OFFSET_X + this.PARTITION_BAR_HORIZONTAL_BUFFER;
            this.PARTITION_BAR_LENGTH = this.TILE_SIZE - this.PARTITION_BAR_HORIZONTAL_BUFFER*2;
            this.MOST_RECENT = this.DETAILS_OFFSET_Y + this.TILE_SIZE + this.VERTICAL_BUFFER;
            this.MOST_RECENT_SPACING = 50;
            this.MAX_NUM_RECENT_TWEETS = 4;
            this.BAR_LINE_SPACING = 80;
            this.MONTH_LINE_Y = this.DETAILS_OFFSET_Y + 64;
            this.WEEK_LINE_Y = this.MONTH_LINE_Y + this.BAR_LINE_SPACING ;
            this.DAY_LINE_Y = this.MONTH_LINE_Y + this.BAR_LINE_SPACING * 2;
            this.MAX_BAR_LENGTH = 20;
        },


        panMapToCenter: function(data) {

            var viewportPixel = this.map.getViewportPixelFromCoord( data.longitude, data.latitude ),
                panCoord = this.map.getCoordFromViewportPixel( viewportPixel.x + this.TILE_SIZE/2 + this.X_CENTRE_OFFSET, 
                                                               viewportPixel.y + this.TILE_SIZE/2 + this.Y_CENTRE_OFFSET );           
            this.map.map.panTo( panCoord.y, panCoord.x );
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


        areDetailsVisible: function(data) {
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
            this.detailsBackground.map('visible').from(function(){return that.areDetailsVisible(this);});
            this.detailsBackground.map('fill').asValue('#111111');
            this.detailsBackground.map('orientation').asValue('horizontal');
            this.detailsBackground.map('bar-count').asValue(1);
            this.detailsBackground.map('width').asValue(this.TILE_SIZE*2 - 2);
            this.detailsBackground.map('length').asValue(this.TILE_SIZE - 2);
            this.detailsBackground.map('offset-y').asValue(this.DETAILS_OFFSET_Y + 1);
            this.detailsBackground.map('offset-x').asValue(this.DETAILS_OFFSET_X + 1);
            this.detailsBackground.on('click', function() { return true; }); //swallow event
            this.detailsBackground.map('opacity').from( function(){return that.getOpacity();});
        },


        createTitleLabels: function() {

            var that = this;

            this.titleLabels = this.createLabel(this.WHITE_COLOUR);
            this.titleLabels.map('label-count').asValue(4);
            this.titleLabels.map('text').from(function(index) {
                switch (index) {
                    case 0:  return "Last Month";
                    case 1:  return "Last Week";
                    case 2:  return "Last 24 hours";
                    default: return "Most Recent";
                }
            });
            this.titleLabels.map('font-size').asValue(20);
            this.titleLabels.map('offset-y').from(function(index) {
                switch(index) {
                    case 0:  return that.MONTH_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    case 1:  return that.WEEK_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    case 2:  return that.DAY_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    default: return that.MOST_RECENT;
                }
            });
            this.titleLabels.map('offset-x').asValue(this.DETAILS_OFFSET_X + this.HORIZONTAL_BUFFER);
        },


        createAxis: function(type) {

            var that = this, 
                i,
                MONTH_INCS = 5,
                WEEK_INCS = 7,
                DAY_INCS = 5,
                axisSpec = {
                    x : that.PARTITION_BAR_OFFSET_X,
                    y : 0,
                    length : this.PARTITION_BAR_LENGTH,
                    isVisibleFunc : function() { return that.areDetailsVisible(this); }
                }, 
                numIncs,
                labelFunc,
                isVisibleFunc;

            

            switch (type) {

                case 'month':

                    // label function
                    axisSpec.labelFunc = function(index) {
                        var month = that.getMonth(this),
                            dayInc = that.getTotalDaysInMonth(this) / (MONTH_INCS-1);
                        return (index === 0) ? month + " 1" : month + " " + Math.round(dayInc*index);
                    };                   
                    axisSpec.numIncs = MONTH_INCS;           // set number of increments              
                    axisSpec.y = that.MONTH_LINE_Y; // set y position
                    break;

                case 'week':

                    // label function
                    axisSpec.labelFunc = function(index) {
                        return that.getLastWeekOfMonth(this)[index % WEEK_INCS];
                    };
                    axisSpec.numIncs = WEEK_INCS + 1;        // set number of increments                    
                    axisSpec.y = that.WEEK_LINE_Y;  // set y position
                    break;

                case 'day':
                default: 

                    // label function
                    axisSpec.labelFunc = function(index) {
                        switch (index) {
                            case 1: return "6am";
                            case 2: return "12pm";
                            case 3: return "6pm";
                            default: return "12am";
                        }
                    };
                    axisSpec.numIncs = DAY_INCS;             // set number of increments                   
                    axisSpec.y = that.DAY_LINE_Y;   // set y position               
                    break;
            }

            this.createBarChartAxis( axisSpec );
        },


        createBarCountLabels: function() {

            var that = this,
                X_OFFSET = 128;

            this.barTotals = this.createLabel(that.BLUE_COLOUR);
            this.barTotals.map('label-count').asValue(3);
            this.barTotals.map('text').from(function(index) {
                switch (index) {
                    case 0:  return that.getParentCount(this,'Daily') + " tweets";
                    case 1:  return that.getParentCount(this,'Per6hrs') + " tweets";
                    default: return that.getParentCount(this,'PerHour') + " tweets";
                }
            });
            this.barTotals.map('font-size').asValue(12);
            this.barTotals.map('offset-y').from(function(index) {
                switch(index) {
                    case 0:  return that.MONTH_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    case 1:  return that.WEEK_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                    default: return that.DAY_LINE_Y - that.VERTICAL_BUFFER - that.MAX_BAR_LENGTH;
                }
            });
            this.barTotals.map('offset-x').asValue(this.DETAILS_OFFSET_X + this.HORIZONTAL_BUFFER + X_OFFSET);
        },


        createBarChart: function(type) {

            var that = this,
                WEEK_BAR_WIDTH = this.PARTITION_BAR_LENGTH / 28,
                DAY_BAR_WIDTH = this.PARTITION_BAR_LENGTH / 24,
                barSpec = {
                    colourFunc : function() { return that.BLUE_COLOUR; },
                    isVisibleFunc : function() { return that.areDetailsVisible(this); },
                    length : this.PARTITION_BAR_LENGTH
                },
                lineSpec = {
                     x: this.PARTITION_BAR_OFFSET_X,  
                     y: 0,
                     length : this.PARTITION_BAR_LENGTH,
                     colour : this.GREY_COLOUR,
                     isVisibleFunc : function() { return that.areDetailsVisible(this); } 
                },
                incType;


            switch (type) {

                case 'month':

                    barSpec.countFunc = function() { return that.getTotalDaysInMonth(this); };
                    barSpec.xFunc = function(index) { 
                        return -1 + that.PARTITION_BAR_OFFSET_X + index*(that.PARTITION_BAR_LENGTH / that.getTotalDaysInMonth(this));
                    };
                    incType = 'Daily';
                    lineSpec.y = this.MONTH_LINE_Y;
                    break;

                case 'week':

                    barSpec.countFunc = function() { return 28; };
                    barSpec.xFunc = function(index) { return -1 + that.PARTITION_BAR_OFFSET_X + index*WEEK_BAR_WIDTH; };
                    incType = 'Per6hrs';
                    lineSpec.y = this.WEEK_LINE_Y;
                    break;

                case 'day':
                default: 

                    barSpec.countFunc = function() { return 24; };
                    barSpec.xFunc = function(index) { return -1 + that.PARTITION_BAR_OFFSET_X + index*DAY_BAR_WIDTH; };
                    incType = 'PerHour';
                    lineSpec.y = this.DAY_LINE_Y;
                    break;
            }

            barSpec.yFunc = function (index) {
                var maxPercentage = that.getMaxPercentage(this, incType);
                return lineSpec.y-((that.getCountPercentage(this, index, incType) / maxPercentage) * that.MAX_BAR_LENGTH)-2 || 0;
            };
            barSpec.heightFunc = function (index) {
                var maxPercentage = that.getMaxPercentage(this, incType);
                return (that.getCountPercentage(this, index, incType) / maxPercentage) * that.MAX_BAR_LENGTH || 0;
            };

            this.createBarSeries( barSpec );
            this.createPartitionLine( lineSpec );
        },


        createLabel : function(colour) {

            var that = this,
                label = that.plotLayer.addLayer(aperture.LabelLayer);

            label.on('click', function() { return true; }); //swallow event
            label.map('visible').from(function(){return that.areDetailsVisible(this)});
            label.map('fill').asValue(colour);
            label.map('label-count').asValue(1);
            label.map('text-anchor').asValue('start');
            label.map('font-outline').asValue(that.BLACK_COLOUR);
            label.map('font-outline-width').asValue(3);
            label.map('opacity').from( function() { return that.getOpacity(); });
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
            var length = data.bin.value[this.clientState.clickState.userData.index].recentTweets.length || 0;
            return (length > this.MAX_NUM_RECENT_TWEETS) ? this.MAX_NUM_RECENT_TWEETS : length;
        },


        createMostRecentTweets: function() {

            var that = this,
                lineSpec = {
                    x : this.PARTITION_BAR_OFFSET_X,  
                    y : 0,
                    length : this.PARTITION_BAR_LENGTH,
                    colour : this.GREY_COLOUR,
                    isVisibleFunc : function() { return that.areDetailsVisible(this); }
                };



            // MOST RECENT TWEETS LABELS
            this.recentTweetsLabels = this.createLabel(that.WHITE_COLOUR);
            this.recentTweetsLabels.map('visible').from(function(){return that.areDetailsVisible(this); });
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
                    tweet = this.bin.value[tagIndex].recentTweets[index].tweet,
                    filteredText = that.filterText(tweet.substring(1, tweet.length - 2) );

                if (that.clientState.hoverState.userData !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandRecent' &&
                    that.clientState.hoverState.userData.index === index) {
                    return that.separateTextIntoLines(filteredText, 70, 3);
                }
                return that.separateTextIntoLines(filteredText, 35, 3);
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
            this.recentTweetsLines = this.createPartitionLine( lineSpec );
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
            this.createBarCountLabels();
            this.createMostRecentTweets();

        }
        


    });

    return DetailsOnDemandTranslate;
});
