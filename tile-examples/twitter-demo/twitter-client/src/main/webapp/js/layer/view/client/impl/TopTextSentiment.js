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



    var TwitterApertureRenderer = require('./TwitterApertureRenderer'),
        TwitterUtil = require('./TwitterUtil'),
        NUM_TAGS_DISPLAYED = 5,
        TopTextSentiment;



    TopTextSentiment = TwitterApertureRenderer.extend({
        ClassName: "TopTextSentiment",

        init: function( map ) {
            this._super( map );
            this.createLayer();
        },


        createLayer: function() {

            this.nodeLayer = this.map.addApertureLayer( aperture.geo.MapNodeLayer );
            this.nodeLayer.map('latitude').from('latitude');
            this.nodeLayer.map('longitude').from('longitude');
            this.createBars();
            this.createLabels();
            this.createCountSummaries();
        },

        getYOffset: function( values, index ) {
            return 36 * ((( TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED ) - 1) / 2) - index);
        },

        createBars: function() {

            var that = this,
                BAR_LENGTH = 100,
                BAR_WIDTH = 6;

            function barTemplate( defaultColour, greyedColour, normalColour ) {

                var bar = that.nodeLayer.addLayer( aperture.BarLayer );
                bar.map('width').asValue(BAR_WIDTH);
                bar.map('stroke').asValue(that.BLACK_COLOUR);
                bar.map('stroke-width').asValue(2);
                bar.map('orientation').asValue('horizontal');
                bar.map('visible').from( function() {
                    return that.visibility;
                });
                bar.map('fill').from( function(index) {

                    var layerState = that.layerState,
                        clickState = layerState.getClickState(),
                        hoverState = layerState.getHoverState(),
                        hasClickState = layerState.hasClickState(),
                        hasHoverState = layerState.hasHoverState(),
                        selectedTag = clickState.tag,
                        hoveredTag = hoverState.tag,
                        tag = this.bin.value[index].tag;

                    if ( (hasClickState && selectedTag === tag) ||
                         (hasHoverState && hoveredTag === tag) ) {
                        return normalColour;
                    }
                    if ( hasClickState && selectedTag !== tag  ) {
                        return greyedColour;
                    }
                    return defaultColour;
                });
                bar.map('bar-count').from(function() {
                    return TwitterUtil.getTagCount( this.bin.value );
                });
                bar.map('offset-y').from(function(index) {
                    return that.X_CENTRE_OFFSET - that.getYOffset( this.bin.value, index ) + 6;
                });
                bar.map('opacity').from( function() {
                    return that.opacity;
                });

                bar.on('click', function(event) {
                    var data = event.data,
                        value = data.bin.value[ event.index[0] ],
                        tag = value.tag;
                    that.clickOn( tag, data, value );
                    return true; // swallow event
                });
                bar.on('mouseover', function(event) {
                    var data = event.data,
                        value = data.bin.value[ event.index[0] ],
                        tag = value.tag;
                    that.hoverOn( tag, data, value );
                    that.nodeLayer.all().where(data).redraw( new aperture.Transition( 100 ) );
                });
                bar.on('mouseout', function(event) {

                    that.hoverOff();
                    that.nodeLayer.all().where(event.data).redraw( new aperture.Transition( 100 ) );
                });
                return bar;
            }

            // negative bar
            this.negativeBar = barTemplate( this.GREY_COLOUR, this.DARK_GREY_COLOUR, this.PURPLE_COLOUR );
            this.negativeBar.map('offset-x').from(function (index) {

                var value = this.bin.value[index],
                    neutralPerc = TwitterUtil.getSentimentPercentage(value, 'neutral'),
                    negativePerc = TwitterUtil.getSentimentPercentage(value, 'negative');

                return that.X_CENTRE_OFFSET - (( neutralPerc * BAR_LENGTH )/2) - ( negativePerc * BAR_LENGTH );
            });
            this.negativeBar.map('length').from(function (index) {
                var value = this.bin.value[index],
                    negativePerc = TwitterUtil.getSentimentPercentage(value, 'negative');
                return negativePerc * BAR_LENGTH;
            });

            // neutral bar
            this.neutralBar = barTemplate( this.DARK_GREY_COLOUR, this.BLACK_COLOUR, this.DARK_GREY_COLOUR );
            this.neutralBar.map('offset-x').from(function (index) {
                var value = this.bin.value[index],
                    neutralPerc = TwitterUtil.getSentimentPercentage(value, 'neutral');
                return that.X_CENTRE_OFFSET - ( neutralPerc * BAR_LENGTH )/2;
            });
            this.neutralBar.map('length').from(function (index) {
                var value = this.bin.value[index],
                    neutralPerc = TwitterUtil.getSentimentPercentage(value, 'neutral');
                return neutralPerc * BAR_LENGTH;
            });

            // positive bar
            this.positiveBar = barTemplate( this.WHITE_COLOUR, this.GREY_COLOUR, this.BLUE_COLOUR );
            this.positiveBar.map('offset-x').from(function (index) {
                var value = this.bin.value[index],
                    neutralPerc = TwitterUtil.getSentimentPercentage(value, 'neutral');
                return that.X_CENTRE_OFFSET + ( neutralPerc * BAR_LENGTH )/2;
            });
            this.positiveBar.map('length').from(function (index) {
                var value = this.bin.value[index],
                    positivePerc = TwitterUtil.getSentimentPercentage(value, 'positive');
                return positivePerc * BAR_LENGTH;
            });

        },


        createLabels: function () {

            var that = this,
                MAX_LABEL_CHAR_COUNT = 9;

            this.tagLabels = this.nodeLayer.addLayer(aperture.LabelLayer);
            this.tagLabels.map('offset-x').asValue(this.X_CENTRE_OFFSET);
            this.tagLabels.map('text-anchor').asValue('middle');
            this.tagLabels.map('text-anchor-y').asValue('start');
            this.tagLabels.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabels.map('font-outline-width').asValue(3);
            this.tagLabels.map('visible').from(function() {
                return that.visibility;
            });
            this.tagLabels.map('fill').from( function(index) {

                var layerState = that.layerState,
                    clickState = layerState.getClickState(),
                    hoverState = layerState.getHoverState(),
                    hasClickState = layerState.hasClickState(),
                    hasHoverState = layerState.hasHoverState(),
                    selectedTag = clickState.tag,
                    hoveredTag = hoverState.tag,
                    tag = this.bin.value[index].tag;

                if ( (hasClickState && selectedTag === tag) ||
                     (hasHoverState && hoveredTag === tag) ) {
                    return that.WHITE_COLOUR;
                }

                if ( hasClickState && selectedTag !== tag  ) {
                    return that.GREY_COLOUR;
                }

                return that.WHITE_COLOUR;
            });
            this.tagLabels.map('label-count').from(function() {
                return TwitterUtil.getTagCount( this.bin.value, NUM_TAGS_DISPLAYED );
            });
            this.tagLabels.map('text').from(function (index) {
                var str = this.bin.value[index].tag;
                if (str.length > MAX_LABEL_CHAR_COUNT) {
                    str = str.substr(0, MAX_LABEL_CHAR_COUNT) + "...";
                }
                return str;
            });
            this.tagLabels.map('font-size').from( function (index) {
                var MAX_FONT_SIZE = 28,
                    MIN_FONT_SIZE = 12,
                    values = this.bin.value,
                    fontSize = TwitterUtil.getFontSize( values, index, MIN_FONT_SIZE, MAX_FONT_SIZE ),
                    layerState = that.layerState,
                    hoverState = layerState.getHoverState(),
                    hasHoverState = layerState.hasHoverState(),
                    tileFocus = layerState.getTileFocus(),
                    hoveredTag = hoverState.tag,
                    tag = values[index].tag;

                if ( hasHoverState && hoveredTag === tag && this.tilekey === tileFocus ) {
                    fontSize += 8;
                }
                return fontSize;
            });
            this.tagLabels.map('offset-y').from( function(index) {
                return that.Y_CENTRE_OFFSET - that.getYOffset( this.bin.value, index ) + 10;
            });
            this.tagLabels.map('opacity').from( function() {
                return that.opacity;
            });

            this.tagLabels.on('click', function(event) {
                var data = event.data,
                    value = data.bin.value[ event.index[0] ],
                    tag = value.tag;
                that.clickOn( tag, data, value );
                return true; // swallow event
            });
            this.tagLabels.on('mouseover', function(event) {
                var data = event.data,
                    value = data.bin.value[ event.index[0] ],
                    tag = value.tag;
                that.hoverOn( tag, data, value );
                that.nodeLayer.all().where(data).redraw( new aperture.Transition( 100 ) );
            });
            this.tagLabels.on('mouseout', function(event) {

                that.hoverOff();
                that.nodeLayer.all().where(event.data).redraw( new aperture.Transition( 100 ) );
            });
        }

    });

    return TopTextSentiment;
});
