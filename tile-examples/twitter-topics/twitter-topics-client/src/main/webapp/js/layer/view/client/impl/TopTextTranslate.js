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

        getCountPercentage: function(data, index, type) {
            return (data.bin.value[index][type] / data.bin.value[index].countMonthly) || 0;
        },


        getTotalCountPercentage: function(data, index) {
            var i,
                sum = 0,
                n = this.getCount(data);
            for (i=0; i<n; i++) {
                sum += data.bin.value[i].countMonthly;
            }
            return (data.bin.value[index].countMonthly/sum) || 0;
        },

        onClick: function(event) {
            this.clientState.setClickState(event.data.tilekey, {
                tag : event.data.bin.value[event.index[0]].topic,
                index : event.index[0]
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
                tag :  event.data.bin.value[event.index[0]].topic,
                index :  event.index[0],
                id : id
            });
            this.plotLayer.all().where(event.data).redraw();
        },


        onHoverOff: function(event) {
            this.clientState.clearHoverState();
            this.plotLayer.all().where(event.data).redraw();
        },


        /**
         * Create our layer visuals, and attach them to our node layer.
         */
        createLayer: function (mapNodeLayer) {

            // TODO: everything should be put on its own PlotLayer instead of directly on the mapNodeLayer
            // TODO: currently does not render correctly if on its own PlotLayer...
            this.plotLayer = mapNodeLayer;
            this.createLabels();
            this.createTranslateLabel();
            this.detailsOnDemand = new DetailsOnDemand(this.id, this.map);
            this.detailsOnDemand.attachClientState(this.clientState);
            this.detailsOnDemand.createLayer(this.plotLayer);
        },


        createLabels: function () {


            var that = this,
                MAX_LABEL_CHAR_COUNT = 12;


            this.wordCloudLabel = this.plotLayer.addLayer(aperture.WordCloudLayer);

            this.wordCloudLabel.map('visible').from(function() {
                return that.isSelectedView(this) && that.isVisible(this);
            });

            this.wordCloudLabel.map('fill').from(function(index) {

                if (that.matchingTagIsSelected(this.bin.value[index].topic, this.tilekey)){
                    return that.BLUE_COLOUR;
                }
                if (that.shouldBeGreyedOut(this.bin.value[index].topic, this.tilekey)) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });


            this.wordCloudLabel.on('click', function(event) {
                that.onClick(event);
                return true; // swallow event
            });

            this.wordCloudLabel.on('mousemove', function(event) {
                that.onHover(event, 'topTextSentimentBarsAll');
                return true; // swallow event
            });

            this.wordCloudLabel.on('mouseout', function(event) {
                that.onHoverOff(event);
            });

            this.wordCloudLabel.map('offset-x').asValue(this.X_CENTRE_OFFSET);
            this.wordCloudLabel.map('offset-y').asValue(this.Y_CENTRE_OFFSET);
            this.wordCloudLabel.map('cursor').asValue('pointer');
            this.wordCloudLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.wordCloudLabel.map('font-outline-width').asValue(3);
            this.wordCloudLabel.map('width').asValue(this.TILE_SIZE - this.HORIZONTAL_BUFFER*2);
            this.wordCloudLabel.map('height').asValue(this.TILE_SIZE - this.VERTICAL_BUFFER*2);

            this.wordCloudLabel.map('words').from(function() {
                var numWords = this.bin.value.length,
                    wordList = [],
                    word,
                    i;
                for (i=0; i<numWords; i++) {
                    word = that.getTopic(this, i);
                    word = (word.length > MAX_LABEL_CHAR_COUNT) ? word.substr(0, MAX_LABEL_CHAR_COUNT) + "..." : word;
                    wordList.push( word );
                }
                return wordList;
            });

            this.wordCloudLabel.map('frequencies').from(function() {
                var numWords = this.bin.value.length,
                    frequencies = [],
                    i;
                for (i=0; i<numWords; i++) {      
                    frequencies.push( this.bin.value[i].countMonthly );
                }
                return frequencies;
            });

            this.wordCloudLabel.map('opacity').from( function() {
                return that.getOpacity();
            });

            this.wordCloudLabel.map('min-font-size').asValue(9);
            this.wordCloudLabel.map('max-font-size').asValue(32);

        }

    });

    return TopTextTranslate;
});
