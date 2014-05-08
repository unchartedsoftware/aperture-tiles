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
        TopTextTranslate;



    TopTextTranslate = TwitterTagRenderer.extend({
        ClassName: "TopTextTranslate",

        init: function(map) {
            this._super("top-text-translate", map);
            this.MAX_NUM_VALUES = 5;
            this.Y_SPACING = 36;
            this.translatedTiles = {};
        },


        isTileTranslated: function( tilekey ) {
            if (this.translatedTiles[tilekey] === undefined) {
                return false;
            } else {
                return true;
            }
        },


        toggleTileTranslation: function( tilekey ) {
            if (this.translatedTiles[tilekey] === undefined) {
                this.translatedTiles[tilekey] = true;
            } else {
                delete this.translatedTiles[tilekey];
            }
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
            this.translateLabel.all().where(data).redraw();
            //this.summaryLabel.all().where(data).redraw();
        },


        onClick: function(event) {
            this.clientState.setClickState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].topic,
                index :  event.index[0]
            });
            // pan map to center
            this.detailsOnDemand.panMapToCenter(event.data);
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
            this.createTranslateLabel();
            //this.createCountSummaries();
            this.detailsOnDemand = new DetailsOnDemand(this.id, this.map);
            this.detailsOnDemand.attachClientState(this.clientState);
            this.detailsOnDemand.createLayer(this.plotLayer);
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
                var topic = (that.isTileTranslated(this.tilekey)) ? this.bin.value[index].topicEnglish : this.bin.value[index].topic,
                    str = that.filterText(topic);
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
                return that.getOpacity();
            })
        },


        createTranslateLabel: function () {

            var that = this,
                isHoveredOn = false;

            this.translateLabel = this.plotLayer.addLayer(aperture.LabelLayer);

            this.translateLabel.map('visible').from(function() {
                return that.isSelectedView(this) && 
                       that.isVisible(this) && 
                       that.clientState.getSharedState('activeCarouselTile') === this.tilekey;
            });

            this.translateLabel.map('fill').from( function() {

                if (that.isTileTranslated(this.tilekey)) {
                    return that.WHITE_COLOUR;
                }
                return that.LIGHT_GREY_COLOUR;
            });

            this.translateLabel.on('click', function(event) {
                that.toggleTileTranslation(event.data.tilekey)
                that.translateLabel.all().where(event.data).redraw();
                that.tagLabel.all().where(event.data).redraw();
                return true; // swallow event
            });

            this.translateLabel.on('mousemove', function(event) {
                isHoveredOn = true;
                that.translateLabel.all().where(event.data).redraw();
                return true; // swallow event, for some reason 'mousemove' on labels needs to swallow this or else it processes a mouseout
            });

            this.translateLabel.on('mouseout', function(event) {
                isHoveredOn = false;
                that.translateLabel.all().where(event.data).redraw();
            });

            this.translateLabel.map('label-count').asValue(1);

            this.translateLabel.map('text').asValue('translate');

            this.translateLabel.map('font-size').from(function () {
                var FONT_SIZE = 16;
                if (isHoveredOn) {
                    return FONT_SIZE + 2;
                }
                return FONT_SIZE;

            });
            this.translateLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET + this.TILE_SIZE / 3);
            this.translateLabel.map('offset-y').asValue(this.Y_CENTRE_OFFSET - 100);          
            this.translateLabel.map('text-anchor').asValue('left');
            this.translateLabel.map('text-anchor-y').asValue('start');
            this.translateLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.translateLabel.map('font-outline-width').asValue(3);
            this.translateLabel.map('opacity').from( function() {
                return that.getOpacity();
            })
        }

    });

    return TopTextTranslate;
});
