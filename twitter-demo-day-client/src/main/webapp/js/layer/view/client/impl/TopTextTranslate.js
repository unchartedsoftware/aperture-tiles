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
        //DetailsOnDemand = require('./DetailsOnDemand'),
        TopTextTranslate;



    TopTextTranslate = TwitterTagRenderer.extend({
        ClassName: "TopTextTranslate",

        init: function() {
            this._super("top-text-translate");
            this.MAX_NUM_VALUES = 5;
            this.Y_SPACING = 36;
        },


        getCountPercentage: function(data, index, type) {
            if (data.bin.value[index].countMonthly === 0) {
                return 0;
            }
            return data.bin.value[index][type] / data.bin.value[index].countMonthly;
        },


        getTotalCountPercentage: function(data, index) {
            var i,
                sum = 0,
                n = this.getCount(data);
            for (i=0; i<n; i++) {
                sum += data.bin.value[i].countMonthly;
            }
            if (sum === 0) {
                return 0;
            }
            return data.bin.value[index].countMonthly/sum;
        },


        redrawLayers: function(data) {
            //this.negativeBar.all().where(data).redraw();
            //this.positiveBar.all().where(data).redraw();
            this.tagLabel.all().where(data).redraw();
            //this.summaryLabel.all().where(data).redraw();
        },


        onClick: function(event) {
            this.clientState.setClickState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].topic,
                index :  event.index[0]
            });
            // send this node to the front
            this.plotLayer.all().where(event.data).toFront();
            // redraw all nodes
            this.plotLayer.all().redraw();
        },


        onHover: function(event, id) {
            this.clientState.setHoverState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].topic,
                index :  event.index[0],
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
            //this.createBars();
            this.createLabels();
            //this.createCountSummaries();
            //this.detailsOnDemand = new DetailsOnDemand(this.id);
            //this.detailsOnDemand.attachClientState(this.clientState);
            //this.detailsOnDemand.createLayer(this.plotLayer);
        },


        createLabels: function () {

            var that = this;

            this.tagLabel = this.plotLayer.addLayer(aperture.LabelLayer);

            this.tagLabel.map('visible').from(function() {
                return that.isSelectedView(this) && that.isVisible(this);
            });

            this.tagLabel.map('fill').from( function(index) {

                if (that.shouldBeGreyedOut(this.bin.value[index].topic, this.tilekey)) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });

            this.tagLabel.on('click', function(event) {
                that.onClick(event);
                return true; // swallow event
            });

            this.tagLabel.on('mousemove', function(event) {
                that.onHover(event, 'topTextSentimentBarsAll');
                return true; // swallow event, for some reason 'mousemove' on labels needs to swallow this or else it processes a mouseout
            });

            this.tagLabel.on('mouseout', function(event) {
                that.onHoverOff(event);
            });

            this.tagLabel.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabel.map('text').from(function (index) {
                var str = that.filterText(this.bin.value[index].topic);
                if (str.length > 12) {
                    str = str.substr(0,12) + "...";
                }
                return str;
            });

            this.tagLabel.map('font-size').from(function (index) {
                var MAX_FONT_SIZE = 28,
                    FONT_SCALE_FACTOR = 60,
                    size = (that.getTotalCountPercentage(this, index) * FONT_SCALE_FACTOR) + 10;
                    size = (size > MAX_FONT_SIZE) ? MAX_FONT_SIZE : size;
                if (that.isHoveredOrClicked(this.bin.value[index].topic, this.tilekey)) {
                    return size + 2;
                }
                return size;

            });
            this.tagLabel.map('offset-y').from(function (index) {
                return that.getYOffset(this, index) + 10;
            });
            this.tagLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET);
            this.tagLabel.map('text-anchor').asValue('middle');
            this.tagLabel.map('text-anchor-y').asValue('start');
            this.tagLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabel.map('font-outline-width').asValue(3);
            this.tagLabel.map('opacity').from( function() {
                    return that.clientState.opacity;
                })
        }

    });

    return TopTextTranslate;
});
