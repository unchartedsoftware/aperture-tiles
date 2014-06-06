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
        TopTextSentiment;



    TopTextSentiment = TwitterTagRenderer.extend({
        ClassName: "TopTextSentiment",

        init: function( map ) {
            this._super( map );
            this.MAX_NUM_VALUES = 5;
            this.Y_SPACING = 36;
            this.createLayer();
        },

        getTotalCount : function(data, index) {
            var i,
                sum = 0,
                n = this.getCount(data);
            for (i=0; i<n; i++) {
                sum += data.bin.value[i].count;
            }
            return sum;
        },

        getCountPercentage: function(data, index, type) {
            return (data.bin.value[index][type] / data.bin.value[index].count) || 0;
        },


        getTotalCountPercentage: function(data, index) {
            var i,
                sum = 0,
                n = this.getCount(data);
            for (i=0; i<n; i++) {
                sum += data.bin.value[i].count;
            }
            return (data.bin.value[index].count/sum) || 0;
        },


        onClick: function(event) {

            this.clientState.setClickState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].tag,
                index : event.index[0]
            });

            TwitterUtil.createDetailsOnDemand( this.map, event.data, event.index[0], DetailsOnDemand );
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
                BAR_LENGTH = 100,
                BAR_WIDTH = 6;

            function barTemplate( id, defaultColour, greyedColour, normalColour, selectedColour ) {

                var bar = that.nodeLayer.addLayer( aperture.BarLayer );

                bar.map('visible').from( function() {
                    return that.visibility;
                });

                bar.map('fill').from( function(index) {

                    /*
                    if (that.isHoveredOrClicked(this.bin.value[index].tag, this.tilekey)) {
                        if ( that.clientState.hoverState.userData.id === id &&
                             that.clientState.hoverState.userData.index === index) {
                            return selectedColour;
                        }
                        return normalColour;
                    }

                    if (that.shouldBeGreyedOut(this.bin.value[index].tag, this.tilekey)) {
                        return greyedColour;
                    }
                    */
                    return defaultColour;
                });

                bar.on('click', function(event) {
                    that.onClick(event);
                    return true; // swallow event
                });

                bar.map('orientation').asValue('horizontal');
                bar.map('bar-count').from(function() {
                    return TwitterUtil.getTagCount( this.bin.value );
                });
                bar.map('width').asValue(BAR_WIDTH);
                bar.map('stroke').asValue(that.BLACK_COLOUR);
                bar.map('stroke-width').asValue(2);
                bar.map('offset-y').from(function(index) {
                    return that.X_CENTRE_OFFSET - that.getYOffset(this, index) + 6;
                });
                bar.map('opacity').from( function() {
                    return that.opacity;
                })
                return bar;
            }

            // negative bar
            this.negativeBar = barTemplate('topTextSentimentBarsNegative', this.GREY_COLOUR, this.DARK_GREY_COLOUR, this.PURPLE_COLOUR, this.DARK_PURPLE_COLOUR);
            this.negativeBar.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2 +
                    -(that.getCountPercentage(this, index, 'negative') * BAR_LENGTH);
            });
            this.negativeBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'negative') * BAR_LENGTH;
            });

            // neutral bar
            this.neutralBar = barTemplate('topTextSentimentBarsNeutral', this.DARK_GREY_COLOUR, this.BLACK_COLOUR, this.DARK_GREY_COLOUR, this.BLACK_COLOUR );
            this.neutralBar.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.neutralBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH;
            });

            // positive bar
            this.positiveBar = barTemplate('topTextSentimentBarsPositive', this.WHITE_COLOUR, this.GREY_COLOUR, this.BLUE_COLOUR, this.DARK_BLUE_COLOUR);
            this.positiveBar.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET + (that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.positiveBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'positive') * BAR_LENGTH;
            });

        },


        createCountSummaries: function () {

            var that = this;

            this.summaryLabel = this.nodeLayer.addLayer(aperture.LabelLayer);
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.summaryLabel.map('font-outline-width').asValue(3);
            this.summaryLabel.map('visible').from(function(){
                return false; /* that.visibility; &&
                        that.clientState.hoverState.tilekey === this.tilekey &&
                        (that.clientState.hoverState.userData.id === 'topTextSentimentBarsPositive' ||
                         that.clientState.hoverState.userData.id === 'topTextSentimentBarsNeutral' ||
                         that.clientState.hoverState.userData.id === 'topTextSentimentBarsNegative' ||
                         that.clientState.hoverState.userData.id === 'topTextSentimentBarsAll'); */
            });
            this.summaryLabel.map('fill').from( function(index) {
                var id = that.clientState.hoverState.userData.id;
                switch(index) {
                    case 0:
                        if (id === 'topTextSentimentBarsPositive' || id === 'topTextSentimentBarsAll') {
                            return that.BLUE_COLOUR
                        } else {
                            return that.LIGHT_GREY_COLOUR;
                        }
                    case 1:
                        if (id === 'topTextSentimentBarsNeutral' || id === 'topTextSentimentBarsAll') {
                            return that.WHITE_COLOUR;
                        } else {
                            return that.LIGHT_GREY_COLOUR;
                        }
                    default:
                        if (id === 'topTextSentimentBarsNegative' || id === 'topTextSentimentBarsAll') {
                            return that.PURPLE_COLOUR;
                        } else {
                            return that.LIGHT_GREY_COLOUR;
                        }
                }
            });
            this.summaryLabel.map('text').from( function(index) {
                var tagIndex = 0; //that.clientState.hoverState.userData.index;
                switch(index) {
                    case 0: return "+ "+this.bin.value[tagIndex].positive;
                    case 1: return ""+this.bin.value[tagIndex].neutral;
                    default: return "- "+this.bin.value[tagIndex].negative;
                }
            });
            this.summaryLabel.map('offset-y').from(function(index) {
                return -that.TILE_SIZE/2 + (that.VERTICAL_BUFFER-4) + (14) * index;
            });
            this.summaryLabel.map('offset-x').asValue(this.TILE_SIZE - this.HORIZONTAL_BUFFER);
            this.summaryLabel.map('text-anchor').asValue('end');
            this.summaryLabel.map('opacity').from( function() {
                return that.opacity;
            });
        },


        createLabels: function () {

            var that = this,
                MAX_LABEL_CHAR_COUNT = 9;

            this.tagLabel = this.nodeLayer.addLayer(aperture.LabelLayer);

            this.tagLabel.map('visible').from(function() {
                return  that.visibility;
            });

            this.tagLabel.map('fill').from( function(index) {
                /*
                if (that.shouldBeGreyedOut(this.bin.value[index].tag, this.tilekey)) {
                    return that.GREY_COLOUR;
                }
                */
                return that.WHITE_COLOUR;
            });

            this.tagLabel.on('click', function(event) {
                that.onClick(event);
                return true; // swallow event
            });

            this.tagLabel.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabel.map('text').from(function (index) {
                var str = that.filterText(this.bin.value[index].tag);
                if (str.length > MAX_LABEL_CHAR_COUNT) {
                    str = str.substr(0, MAX_LABEL_CHAR_COUNT) + "...";
                }
                return str;
            });

            this.tagLabel.map('font-size').from(function (index) {
                var MAX_FONT_SIZE = 28,
                    MIN_FONT_SIZE = 12,
                    FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,                  
                    sum = that.getTotalCount(this, index),
                    perc = that.getTotalCountPercentage(this, index),
                    scale = Math.log(sum),
                    size = ( perc * FONT_RANGE * scale) + (MIN_FONT_SIZE * perc);
                    size = Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );

                /*
                if (that.isHoveredOrClicked(this.bin.value[index].tag, this.tilekey)) {
                    return size + 2;
                }
                */
                return size;
            });

            this.tagLabel.map('offset-y').from(function (index) {
                return that.X_CENTRE_OFFSET - that.getYOffset(this, index) + 10;
            });
            this.tagLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET);
            this.tagLabel.map('text-anchor').asValue('middle');
            this.tagLabel.map('text-anchor-y').asValue('start');
            this.tagLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabel.map('font-outline-width').asValue(3);
            this.tagLabel.map('opacity').from( function() {
                return that.opacity;
            })

        }

    });

    return TopTextSentiment;
});
