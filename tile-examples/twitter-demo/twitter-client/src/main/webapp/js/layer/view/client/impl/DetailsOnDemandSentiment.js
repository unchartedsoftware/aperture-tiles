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
        DetailsOnDemandSentiment;



    DetailsOnDemandSentiment = TwitterTagRenderer.extend({
        ClassName: "DetailsOnDemandSentiment",

        init: function(id, map) {
            this._super(id, map, true);
            this.DETAILS_OFFSET_X = this.X_CENTRE_OFFSET + this.TILE_SIZE/2;
            this.DETAILS_OFFSET_Y = -this.TILE_SIZE/2;
            this.PARTITION_BAR_HORIZONTAL_BUFFER =  this.HORIZONTAL_BUFFER*2;
            this.PARTITION_BAR_OFFSET_X = this.DETAILS_OFFSET_X + this.PARTITION_BAR_HORIZONTAL_BUFFER;
            this.PARTITION_BAR_OFFSET_Y = this.DETAILS_OFFSET_Y + this.TILE_SIZE/2 + 30,
            this.PARTITION_BAR_LENGTH = this.TILE_SIZE - this.PARTITION_BAR_HORIZONTAL_BUFFER*2;

            this.GRAPH_BAR_WIDTH = this.PARTITION_BAR_LENGTH / 24;
            this.MAX_BAR_LENGTH = 50,
            this.AXIS_OFFSET_Y = this.PARTITION_BAR_OFFSET_Y + this.MAX_BAR_LENGTH + this.VERTICAL_BUFFER,

            this.MOST_RECENT = this.DETAILS_OFFSET_Y + this.TILE_SIZE + this.VERTICAL_BUFFER;
            this.MOST_RECENT_SPACING = 50;
            this.MAX_NUM_RECENT_TWEETS = 4;
        },


        panMapToCenter: function(data) {

            var viewportPixel = this.map.getViewportPixelFromCoord( data.longitude, data.latitude ),
                panCoord = this.map.getCoordFromViewportPixel( viewportPixel.x + this.TILE_SIZE/2 + this.X_CENTRE_OFFSET, 
                                                           viewportPixel.y + this.TILE_SIZE/2 + this.Y_CENTRE_OFFSET );           
            this.map.map.panTo( panCoord.y, panCoord.x );
        },


        areDetailsVisible: function(data) {
            var that = this;
            return that.isSelectedView(data) && 
                   that.isVisible(data) && 
                   (that.clientState.clickState.tilekey === data.tilekey);
        },


        getMaxPercentage: function(data, type) {
            var i,
                percent,
                maxPercent = 0,
                tagIndex = this.clientState.clickState.userData.index,
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
        },


         getMaxPercentageBoth: function(data) {
            var maxPositive = this.getMaxPercentage(data, 'positive'),
                maxNegative = this.getMaxPercentage(data, 'negative');
            return (maxPositive > maxNegative) ? maxPositive : maxNegative;
        },


        getExclusiveCountPercentage: function(data, index, type) {

            var attrib = type + 'ByTime',
                tagIndex = this.clientState.clickState.userData.index,
                count = data.bin.value[tagIndex].count;
            if (count === 0) {
                return 0;
            }
            return data.bin.value[tagIndex][attrib][index] / count;
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


        createLayer: function (nodeLayer) {

            // TODO: everything should be put on its own PlotLayer instead of directly on the mapNodeLayer
            // TODO: currently doesnt not render correctly if on its on PlotLayer...
            this.plotLayer = nodeLayer;
            this.createDetailsOnDemand();
        },


        getRecentTweetsCount: function(data) {
            var length = data.bin.value[this.clientState.clickState.userData.index].recent.length || 0;
            return (length > this.MAX_NUM_RECENT_TWEETS) ? this.MAX_NUM_RECENT_TWEETS : length;
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


        createTitleLabels: function() {

            var that = this;

            this.titleLabels = this.createLabel(that.WHITE_COLOUR);
            this.titleLabels.map('label-count').asValue(3);
            this.titleLabels.map('text').from(function(index) {
                switch (index) {
                    case 0:
                        var str = that.filterText(that.clientState.clickState.userData.tag);
                        if (str.length > 15) {
                            str = str.substr(0, 15) + "...";
                        }
                        return str;
                    case 1:  return "Last 24 hours";
                    default: return "Most Recent";
                }
            });
            this.titleLabels.map('font-size').asValue(24);
            this.titleLabels.map('offset-y').from(function(index) {
                switch(index) {
                    case 0: return that.DETAILS_OFFSET_Y + that.VERTICAL_BUFFER;
                    case 1: return that.DETAILS_OFFSET_Y + that.VERTICAL_BUFFER*3;
                    default: return that.MOST_RECENT;
                }
            });
            this.titleLabels.map('offset-x').asValue(this.DETAILS_OFFSET_X + that.HORIZONTAL_BUFFER);
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
                    filteredText = that.filterText(this.bin.value[tagIndex].recent[index].tweet);

                if (that.clientState.hoverState.userData !== undefined &
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


        createBarGraph: function() {

            var that = this;

            function getBarHoverColour( defaultColour, selectedColour, index ) {
                if ( that.clientState.hoverState.userData !== undefined &&
                    (that.clientState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                     that.clientState.hoverState.userData.id === 'detailsOnDemandNegative') &&
                     that.clientState.hoverState.userData.index === index) {
                    return selectedColour;
                }
                return defaultColour;
            }

            // POSITIVE TITLE LABEL
            this.positiveLabel = this.createLabel(this.BLUE_COLOUR);
            this.positiveLabel.map('font-size').asValue(16);
            this.positiveLabel.map('text').asValue('positive tweets');
            this.positiveLabel.map('offset-y').asValue(this.PARTITION_BAR_OFFSET_Y - this.MAX_BAR_LENGTH - this.HORIZONTAL_BUFFER);
            this.positiveLabel.map('offset-x').asValue(this.PARTITION_BAR_OFFSET_X);

            // NEGATIVE TITLE LABEL
            this.negativeLabel = this.createLabel(this.PURPLE_COLOUR);
            this.negativeLabel.map('font-size').asValue(16);
            this.negativeLabel.map('text').asValue('negative tweets');
            this.negativeLabel.map('offset-y').asValue(this.PARTITION_BAR_OFFSET_Y + this.MAX_BAR_LENGTH + this.HORIZONTAL_BUFFER);
            this.negativeLabel.map('offset-x').asValue(this.PARTITION_BAR_OFFSET_X);

            // AXIS CENTRE LINE
            this.axisLine = this.createPartitionLine( {
                colour : this.GREY_COLOUR,
                length : this.PARTITION_BAR_LENGTH,
                x : this.PARTITION_BAR_OFFSET_X,
                y : this.PARTITION_BAR_OFFSET_Y,
                isVisibleFunc : function() { return that.areDetailsVisible(this); },
            });

            // TIME AXIS LABEL
            this.timeAxisLabel = this.createBarChartAxis({
                numIncs : 5,
                x : this.PARTITION_BAR_OFFSET_X,
                y : this.PARTITION_BAR_OFFSET_Y + this.MAX_BAR_LENGTH + this.VERTICAL_BUFFER + 10,
                length : this.PARTITION_BAR_LENGTH,
                isVisibleFunc : function() { return that.areDetailsVisible(this); },
                labelFunc : function(index) {
                    switch (index) {
                        case 1: return "6am";
                        case 2: return "12pm";
                        case 3: return "6pm";
                        default: return "12am";
                    }
                }
            });
           

            // POSITIVE BAR
            this.detailsPositiveBar = this.createBarSeries( {
                length : this.PARTITION_BAR_LENGTH,
                isVisibleFunc : function() { return that.areDetailsVisible(this); },
                xFunc : function(index) {
                    return -1 + that.PARTITION_BAR_OFFSET_X + index*that.GRAPH_BAR_WIDTH;
                },
                yFunc : function (index) {
                    var maxPercentage = that.getMaxPercentageBoth(this);
                    return that.PARTITION_BAR_OFFSET_Y-((that.getExclusiveCountPercentage(this, index, 'positive') / maxPercentage) * that.MAX_BAR_LENGTH)-2 || 0;
                },
                heightFunc : function (index) {
                    var maxPercentage = that.getMaxPercentageBoth(this);
                    return (that.getExclusiveCountPercentage(this, index, 'positive') / maxPercentage) * that.MAX_BAR_LENGTH || 0;
                },
                colourFunc : function(index) {
                    return getBarHoverColour( that.BLUE_COLOUR, that.DARK_BLUE_COLOUR, index);
                },
                countFunc : function() { return 24; },
                mousemove : function(event) {
                    that.onHover(event, 'detailsOnDemandPositive');
                    that.detailsPositiveBar.all().where(event.data).redraw();
                    that.countLabels.all().where(event.data).redraw();
                },
                mouseout : function(event) {
                    that.onHoverOff(event);
                    that.detailsPositiveBar.all().where(event.data).redraw();
                    that.countLabels.all().where(event.data).redraw();
                }
            });

            // NEGATIVE BAR
            this.detailsNegativeBar = this.createBarSeries( {
                length : this.PARTITION_BAR_LENGTH,
                isVisibleFunc : function() { return that.areDetailsVisible(this); },
                xFunc : function(index) {
                    return -1 + that.PARTITION_BAR_OFFSET_X + index*that.GRAPH_BAR_WIDTH;
                },
                yFunc : function (index) { return that.PARTITION_BAR_OFFSET_Y+1; },
                heightFunc : function (index) {
                    var maxPercentage = that.getMaxPercentageBoth(this);
                return (that.getExclusiveCountPercentage(this, index, 'negative') / maxPercentage) * that.MAX_BAR_LENGTH || 0;
                },
                colourFunc : function(index) {
                    return getBarHoverColour( that.PURPLE_COLOUR, that.DARK_PURPLE_COLOUR, index);
                },
                countFunc : function() { return 24; },
                mousemove : function(event) {
                    that.onHover(event, 'detailsOnDemandNegative');
                    that.detailsNegativeBar.all().where(event.data).redraw();
                    that.countLabels.all().where(event.data).redraw();
                },
                mouseout :  function(event) {
                    that.onHoverOff(event);
                    that.detailsNegativeBar.all().where(event.data).redraw();
                    that.countLabels.all().where(event.data).redraw();
                }
            });
        },


        createCountSummaryLabels: function() {

            var that = this;

            // COUNT SUMMARY LABELS
            this.summaryLabel = this.createLabel();
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('fill').from( function(index) {
                switch(index) {
                    case 0: return that.BLUE_COLOUR;
                    case 1: return that.WHITE_COLOUR;
                    default: return that.PURPLE_COLOUR;
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
                return that.DETAILS_OFFSET_Y + (that.VERTICAL_BUFFER-4) + (14) * index;
            });
            this.summaryLabel.map('offset-x').asValue(this.DETAILS_OFFSET_X + this.TILE_SIZE - this.HORIZONTAL_BUFFER);
            this.summaryLabel.map('text-anchor').asValue('end');
        },


        createHoverCountLabels: function() {

            var that = this;

            // HOVER COUNT LABELS
            this.countLabels = this.createLabel(this.WHITE_COLOUR);          
            this.countLabels.map('font-outline-width').asValue(3);
            this.countLabels.map('font-size').asValue(12);
            this.countLabels.map('label-count').asValue(2);
            this.countLabels.map('text-anchor').asValue('start');
            this.countLabels.map('visible').from(function(){
                return that.areDetailsVisible(this) &&
                     that.clientState.hoverState.userData.id !== undefined &&
                    (that.clientState.hoverState.userData.id === 'detailsOnDemandPositive' ||
                     that.clientState.hoverState.userData.id === 'detailsOnDemandNegative') &&
                     that.clientState.hoverState.tilekey === this.tilekey;
            });
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
            
            this.countLabels.map('offset-y').from( function() {
                if (that.clientState.hoverState.userData.id !== undefined &&
                    that.clientState.hoverState.userData.id === 'detailsOnDemandPositive') {
                    return that.PARTITION_BAR_OFFSET_Y - 30;
                }
                return that.PARTITION_BAR_OFFSET_Y + 30;
            });
            this.countLabels.map('offset-x').from( function(index) {
                if (that.clientState.hoverState.userData !== undefined) {
                    if (index === 1) {
                        return that.DETAILS_OFFSET_X + that.clientState.hoverState.userData.index*that.GRAPH_BAR_WIDTH + 94;
                    }
                    return that.DETAILS_OFFSET_X + that.clientState.hoverState.userData.index*that.GRAPH_BAR_WIDTH + 40;
                }
            });

        },


        createDetailsOnDemand: function() {

            var that = this;

            this.createBackgroundPanel();
            this.createTitleLabels();
            this.createBarGraph();
            this.createCountSummaryLabels();
            this.createMostRecentTweets();
            this.createHoverCountLabels();
        }



    });

    return DetailsOnDemandSentiment;
});
