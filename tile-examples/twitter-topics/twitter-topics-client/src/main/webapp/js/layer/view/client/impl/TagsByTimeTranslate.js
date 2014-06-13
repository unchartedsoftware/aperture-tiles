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
        DetailsOnDemand = require('./DetailsOnDemandTranslate'),
        TagsByTimeTranslate;



    TagsByTimeTranslate = TwitterTagRenderer.extend({
        ClassName: "TagsByTimeTranslate",

        init: function( map ) {
            this._super( map );
            this.MAX_NUM_VALUES = 10;
            this.Y_SPACING = 18;
            this.createLayer();
        },


        getTotalCountPercentage: function(data, index) {
            var numDays = this.getTotalDaysInMonth(data),
                tagIndex = Math.floor(index / numDays),
                dailycount = data.bin.value[tagIndex].countDaily[numDays - 1 - (index % numDays)];

            return (dailycount / data.bin.value[tagIndex].countMonthly) || 0;
        },


        onClick: function(event, index) {
            this.clientState.clickState = {
                tilekey : event.data.tilekey,
                tag : event.data.bin.value[index].topic,
                index : index
            };
            // redraw all nodes
            this.nodeLayer.all().redraw();
        },


        onHover: function(event, index) {
            this.clientState.hoverState = {
                tilekey : event.data.tilekey,
                tag : event.data.bin.value[index].topic,
                index : index
            };
            this.nodeLayer.all().where(event.data).redraw();
        },


        onHoverOff: function(event) {
            this.clientState.hoverState = {};
            this.nodeLayer.all().where(event.data).redraw();
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function() {

            this.nodeLayer = this.map.addApertureLayer( aperture.geo.MapNodeLayer );
            this.nodeLayer.map('latitude').from('latitude');
            this.nodeLayer.map('longitude').from('longitude');
            this.createBars();
            this.createLabels();
            this.createTranslateLabel();
        },


        createBars: function() {

            var that = this,
                BAR_LENGTH = 10;

            function getMaxPercentage(data, index) {
                var i,
                    percent,
                    numDays = that.getTotalDaysInMonth(data),
                    tagIndex = Math.floor(index/numDays),                 
                    maxPercent = 0,
                    count = data.bin.value[tagIndex].countMonthly;

                for (i=0; i<numDays; i++) {
                    // get maximum percent
                    percent = (data.bin.value[tagIndex].countDaily[i] / count) || 0;
                    if (percent > maxPercent) {
                        maxPercent = percent;
                    }
                }
                return maxPercent;
            }

            this.bars = this.nodeLayer.addLayer(aperture.BarLayer);
            this.bars.map('orientation').asValue('vertical');
            this.bars.map('width').asValue(3);
            this.bars.map('visible').from( function() {
                return that.visibility;
            });
            this.bars.map('fill').from( function(index) {

                var numDays = that.getTotalDaysInMonth(this),
                    tagIndex = Math.floor(index/numDays),
                    topic = this.bin.value[tagIndex].topic;

                if (that.matchingTagIsSelected(topic, this.tilekey)){
                    return that.BLUE_COLOUR;
                }
                if (that.shouldBeGreyedOut(topic, this.tilekey)) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });
            this.bars.map('cursor').asValue('pointer');

            this.bars.map('bar-count').from( function() {
                return that.getTotalDaysInMonth(this) * that.getCount(this);
            });
            this.bars.map('offset-y').from(function(index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return that.Y_CENTRE_OFFSET -((that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH) +
                       that.getYOffset(this, Math.floor(index/that.getTotalDaysInMonth(this)));
            });
            this.bars.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET - 100 + ((index % that.getTotalDaysInMonth(this)) * 4);
            });
            this.bars.map('length').from(function (index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return (that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH;
            });

            this.bars.on('click', function(event) {
                that.onClick(event, Math.floor(event.index[0]/that.getTotalDaysInMonth(event.data)));
                return true; // swallow event
            });

            this.bars.on('mousemove', function(event) {
                that.onHover(event, Math.floor(event.index[0]/that.getTotalDaysInMonth(event.data)));
                return true; // swallow event
            });

            this.bars.on('mouseout', function(event) {
                that.onHoverOff(event);
            });
            this.bars.map('opacity').from( function() {
                    return that.opacity;
                })

        },


        createLabels: function () {

            var that = this;

            this.tagLabels = this.nodeLayer.addLayer(aperture.LabelLayer);

            this.tagLabels.map('visible').from(function() {
                return that.visibility;
            });

            this.tagLabels.map('fill').from( function(index) {
                if (that.shouldBeGreyedOut(this.bin.value[index].topic, this.tilekey)) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });

            this.tagLabels.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabels.map('text').from(function (index) {
                var str = that.getTopic(this, index);
                if (str.length > 9) {
                    str = str.substr(0,9) + "...";
                }
                return str;
            });

            this.tagLabels.map('cursor').asValue('pointer');

            this.tagLabels.map('font-size').from( function(index) {
                if (that.isHoveredOrClicked(this.bin.value[index].topic, this.tilekey)) {
                    return 14;
                }
                return 12;
            });

            this.tagLabels.map('offset-y').from(function (index) {
                return that.Y_CENTRE_OFFSET + that.getYOffset(this, index) - 5;
            });

            this.tagLabels.map('offset-x').asValue(that.X_CENTRE_OFFSET + 38);
            this.tagLabels.map('text-anchor').asValue('start');
            this.tagLabels.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabels.map('font-outline-width').asValue(3);

            this.tagLabels.on('click', function(event) {
                that.onClick(event, event.index[0]);
                return true; // swallow event
            });

            this.tagLabels.on('mousemove', function(event) {
                that.onHover(event, event.index[0]);
                return true;  // swallow event
            });

            this.tagLabels.on('mouseout', function(event) {
                that.onHoverOff(event);
            });
            this.tagLabels.map('opacity').from( function() {
                    return that.opacity;
                })

        }


    });

    return TagsByTimeTranslate;
});
