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
        DetailsOnDemand = require('./DetailsOnDemand'),
        TopTextSentimentBars;



    TopTextSentimentBars = TwitterTagRenderer.extend({
        ClassName: "TopTextSentimentBars",

        init: function(id) {

            this._super(id);
            this.valueCount = 5;
            this.ySpacing = 30;
        },


        onUnselect: function() {
            this.clearMouseClickState();
            this.plotLayer.all().redraw();
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


        redrawLayers: function(data) {
            this.negativeBar.all().where(data).redraw();
            this.positiveBar.all().where(data).redraw();
            this.tagLabel.all().where(data).redraw();
        },


        onClick: function(event) {
            this.setMouseClickState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].tag,
                index :  event.index[0]
            });
            this.plotLayer.all().redraw();
        },


        onHover: function(event) {
            this.setMouseHoverState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].tag,
                index :  event.index[0]
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
        createLayer: function (mapNodeLayer) {

            // TODO: everything should be put on its own PlotLayer instead of directly on the mapNodeLayer
            // TODO: currently does not render correctly if on its own PlotLayer...
            this.plotLayer = mapNodeLayer;
            this.createBars();
            this.createLabels();
            this.detailsOnDemand = new DetailsOnDemand(this.id);
            this.detailsOnDemand.attachMouseState(this.mouseState);
            this.detailsOnDemand.createLayer(this.plotLayer);
        },


        createBars: function() {

            var that = this,
                BAR_LENGTH = 100;

            function barTemplate( defaultColour, greyedColour, selectedColour ) {

                var bar = that.plotLayer.addLayer(aperture.BarLayer);

                bar.map('visible').from( function() {
                    return (that.id === this.renderer) && that.isNotBehindDoD(this.tilekey);
                });

                bar.map('fill').from( function(index) {

                    if (that.isHoveredOrClicked(this.bin.value[index].tag, this.tilekey)) {
                        return selectedColour;
                    }

                    if (that.shouldBeGreyedOut(this.bin.value[index].tag, this.tilekey)) {
                        return greyedColour;
                    }

                    return defaultColour;
                });

                bar.on('click', function(event) {
                    that.onClick(event);
                    return true; // swallow event
                });

                bar.on('mousemove', function(event) {
                    that.onHover(event);
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
            this.negativeBar = barTemplate('#777777', '#222222', this.NEGATIVE_COLOUR);
            this.negativeBar.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2 +
                    -(that.getCountPercentage(this, index, 'negative') * BAR_LENGTH);
            });
            this.negativeBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'negative') * BAR_LENGTH;
            });

            // neutral bar
            this.neutralBar = barTemplate('#222222', '#000000', this.NEUTRAL_COLOUR );
            this.neutralBar.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET -(that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.neutralBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH;
            });

            // positive bar
            this.positiveBar = barTemplate('#FFFFFF', '#666666', this.POSITIVE_COLOUR);
            this.positiveBar.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET + (that.getCountPercentage(this, index, 'neutral') * BAR_LENGTH)/2;
            });
            this.positiveBar.map('length').from(function (index) {
                return that.getCountPercentage(this, index, 'positive') * BAR_LENGTH;
            });
        },


        createLabels: function () {

            var that = this;

            this.tagLabel = this.plotLayer.addLayer(aperture.LabelLayer);

            this.tagLabel.map('visible').from(function() {
                return that.id === this.renderer && that.isNotBehindDoD(this.tilekey);
            });

            this.tagLabel.map('fill').from( function(index) {

                if (that.shouldBeGreyedOut(this.bin.value[index].tag, this.tilekey)) {
                    return '#666666';
                }
                return '#FFFFFF';
            });

            this.tagLabel.on('click', function(event) {
                that.onClick(event);
                return true; // swallow event
            });

            this.tagLabel.on('mousemove', function(event) {
                that.onHover(event);
                return true; // swallow event, for some reason mousemove on labels needs to swallow this or else it processes a mouseout
            });

            this.tagLabel.on('mouseout', function(event) {
                that.onHoverOff(event);
            });

            this.tagLabel.map('label-count').from(function() {
                return that.getCount(this);
            });

            this.tagLabel.map('text').from(function (index) {
                var str = this.bin.value[index].tag;
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
            this.tagLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET);
            this.tagLabel.map('text-anchor').asValue('middle');
            this.tagLabel.map('font-outline').asValue('#000000');
            this.tagLabel.map('font-outline-width').asValue(3);
        }

    });

    return TopTextSentimentBars;
});
