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

        init: function(map) {
            this._super("tags-by-time-translate", map);
            this.MAX_NUM_VALUES = 10;
            this.Y_SPACING = 18;
        },




        getTotalCountPercentage: function(data, index) {
            var numDays = this.getTotalDaysInMonth(data),
                tagIndex = Math.floor(index / numDays);
                
            if (data.bin.value[tagIndex].countMonthly === 0) {
                return 0;
            }
            return data.bin.value[tagIndex].countDaily[index % numDays] / data.bin.value[tagIndex].countMonthly;
        },


        redrawLayers: function(data) {
            this.tagLabels.all().where(data).redraw();
            this.bars.all().where(data).redraw();
            //this.summaryLabel.all().where(data).redraw();
        },


        onClick: function(event, index) {
            this.clientState.setClickState(event.data.tilekey, {
                tag : event.data.bin.value[index].topic,
                index : index
            });
            // pan map to center
            this.detailsOnDemand.panMapToCenter(event.data);
            // send this node to the front
            this.plotLayer.all().where(event.data).toFront();
            // redraw all nodes
            this.plotLayer.all().redraw();
        },


        onHover: function(event, index, id) {
            this.clientState.setHoverState(event.data.tilekey, {
                tag : event.data.bin.value[index].topic,
                index : index,
                id : id
            });
            this.redrawLayers(event.data);
        },


        onHoverOff: function(event) {
            this.clientState.clearHoverState();
            this.redrawLayers(event.data);
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
            //this.createCountSummaries();
            this.detailsOnDemand = new DetailsOnDemand(this.id, map);
            this.detailsOnDemand.attachClientState(this.clientState);
            this.detailsOnDemand.createLayer(this.plotLayer);
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

                if (count === 0) {
                    return 0;
                }
                for (i=0; i<numDays; i++) {
                    // get maximum percent
                    percent = data.bin.value[tagIndex].countDaily[i] / count;
                    if (percent > maxPercent) {
                        maxPercent = percent;
                    }
                }
                return maxPercent;
            }

            this.bars = this.plotLayer.addLayer(aperture.BarLayer);
            this.bars.map('orientation').asValue('vertical');
            this.bars.map('width').asValue(3);
            this.bars.map('visible').from( function() {
                return that.isSelectedView(this) && that.isVisible(this);
            });
            this.bars.map('fill').from( function(index) {

                var numDays = that.getTotalDaysInMonth(this),
                    tagIndex = Math.floor(index/numDays);

                if (that.matchingTagIsSelected(this.bin.value[tagIndex].topic)){
                    return that.POSITIVE_COLOUR;
                }
                if (that.shouldBeGreyedOut(this.bin.value[tagIndex].topic, this.tilekey)) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });
            this.bars.map('bar-count').from( function() {
                return that.getTotalDaysInMonth(this) * that.getCount(this);
            });
            this.bars.map('offset-y').from(function(index) {
                var maxPercentage = getMaxPercentage(this, index);
                if (maxPercentage === 0) {
                    return 0;
                }
                return -((that.getTotalCountPercentage(this, index) / maxPercentage) * BAR_LENGTH) +
                       that.getYOffset(this, Math.floor(index/that.getTotalDaysInMonth(this)));
            });
            this.bars.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET - 90 + ((index % that.getTotalDaysInMonth(this)) * 4);
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
                that.onHover(event, Math.floor(event.index[0]/that.getTotalDaysInMonth(event.data)), 'tagsByTimeTranslateCountSummary');
            });

            this.bars.on('mouseout', function(event) {
                that.onHoverOff(event);
            });
            this.bars.map('opacity').from( function() {
                    return that.clientState.opacity;
                })

        },


        /*
        createCountSummaries: function () {

            var that = this;

            this.summaryLabel = this.plotLayer.addLayer(aperture.LabelLayer);
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.summaryLabel.map('font-outline-width').asValue(3);
            this.summaryLabel.map('visible').from(function(){
                return that.isSelectedView(this) &&
                    that.clientState.hoverState.tilekey === this.tilekey &&
                    that.clientState.hoverState.userData.id === 'tagsByTimeTranslateCountSummary';
            });
            this.summaryLabel.map('fill').from( function(index) {
                switch(index) {
                    case 0: return that.POSITIVE_COLOUR;
                    case 1: return that.WHITE_COLOUR;
                    default: return that.NEGATIVE_COLOUR;
                }
            });
            this.summaryLabel.map('text').from( function(index) {
                var tagIndex = that.clientState.hoverState.userData.index;
                switch(index) {
                    case 0: return "+ "+this.bin.value[tagIndex].positive;
                    case 1: return ""+this.bin.value[tagIndex].neutral;
                    default: return "- "+this.bin.value[tagIndex].negative;
                }
            });
            this.summaryLabel.map('offset-y').from(function(index) {
                return (-that.TILE_SIZE/2) + (that.VERTICAL_BUFFER-4) + (14 * index);
            });
            this.summaryLabel.map('offset-x').asValue(this.TILE_SIZE - this.HORIZONTAL_BUFFER);
            this.summaryLabel.map('text-anchor').asValue('end');
            this.summaryLabel.map('opacity').from( function() {
                    return that.clientState.opacity;
                })
        },
        */


        createLabels: function () {

            var that = this;

            this.tagLabels = this.plotLayer.addLayer(aperture.LabelLayer);

            this.tagLabels.map('visible').from(function() {
                return that.isSelectedView(this) && that.isVisible(this);
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
                var str = that.filterText(this.bin.value[index].topic);
                if (str.length > 9) {
                    str = str.substr(0,9) + "...";
                }
                return str;
            });

            this.tagLabels.map('font-size').from( function(index) {
                if (that.isHoveredOrClicked(this.bin.value[index].topic, this.tilekey)) {
                    return 14;
                }
                return 12;
            });

            this.tagLabels.map('offset-y').from(function (index) {
                return that.getYOffset(this, index) - 5;
            });

            this.tagLabels.map('offset-x').asValue(that.X_CENTRE_OFFSET + 48);
            this.tagLabels.map('text-anchor').asValue('start');
            this.tagLabels.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabels.map('font-outline-width').asValue(3);

            this.tagLabels.on('click', function(event) {
                that.onClick(event, event.index[0]);
                return true; // swallow event
            });

            this.tagLabels.on('mousemove', function(event) {
                that.onHover(event, event.index[0], 'tagsByTimeTranslateCountSummary');
                return true;  // swallow event, for some reason mousemove on labels needs to swallow this or else it processes a mouseout
            });

            this.tagLabels.on('mouseout', function(event) {
                that.onHoverOff(event);
            });
            this.tagLabels.map('opacity').from( function() {
                    return that.clientState.opacity;
                })

        }


    });

    return TagsByTimeTranslate;
});
