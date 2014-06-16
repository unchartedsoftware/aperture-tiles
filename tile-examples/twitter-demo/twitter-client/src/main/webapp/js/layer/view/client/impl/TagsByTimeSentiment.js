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
        NUM_HOURS_IN_DAY = 24,
        NUM_TAGS_DISPLAYED = 5,
        TagsByTimeSentiment;



    TagsByTimeSentiment = TwitterApertureRenderer.extend({
        ClassName: "TagsByTimeSentiment",

        init: function( map ) {
            this._super( map );

            this.createLayer();
        },


        getYOffset: function( values, index ) {
            return 18 * ((( TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED ) - 1) / 2) - index);
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
                BAR_WIDTH = 3,
                BAR_LENGTH = 10;
       
            this.bars = this.nodeLayer.addLayer( aperture.BarLayer );
            this.bars.map('orientation').asValue('vertical');
            this.bars.map('width').asValue(BAR_WIDTH);
            this.bars.map('visible').from( function() {
                return that.visibility;
            });
            this.bars.map('fill').from( function(index) {

                var layerState = that.layerState,
                    clickState = layerState.getClickState(),
                    hoverState = layerState.getHoverState(),
                    hasClickState = layerState.hasClickState(),
                    hasHoverState = layerState.hasHoverState(),
                    selectedTag = clickState.tag,
                    hoveredTag = hoverState.tag,
                    tagIndex = Math.floor( index/NUM_HOURS_IN_DAY ),
                    hour = index % NUM_HOURS_IN_DAY,
                    value = this.bin.value[tagIndex],
                    tag = value.tag;

                function getBlendedColor() {
                    var percentages = TwitterUtil.getSentimentPercentageByTime( value, hour );
                    return TwitterUtil.blendSentimentColours( percentages.positive,
                                                              percentages.neutral,
                                                              percentages.negative );
                }

                if ( (hasClickState && selectedTag === tag) ||
                     (hasHoverState && hoveredTag === tag) ) {
                    return getBlendedColor();
                }

                if ( hasClickState && selectedTag !== tag  ) {
                    return that.GREY_COLOUR;
                }

                return that.WHITE_COLOUR;
            });

            this.bars.map('bar-count').from( function() {
                return NUM_HOURS_IN_DAY * TwitterUtil.getTagCount( this.bin.value, NUM_TAGS_DISPLAYED );
            });
            this.bars.map('offset-y').from(function(index) {
                var values = this.bin.value,
                    tagIndex = Math.floor( index/NUM_HOURS_IN_DAY ),
                    hour = index % NUM_HOURS_IN_DAY,
                    value = values[tagIndex],
                    maxPercentage = TwitterUtil.getMaxCountByTimePercentage( value ),
                    countByTimePerc= TwitterUtil.getCountByTimePercentage( value, hour ),
                    relativePercentage = countByTimePerc / maxPercentage || 0;
                return that.Y_CENTRE_OFFSET -( relativePercentage * BAR_LENGTH ) - that.getYOffset( values, tagIndex );
            });
            this.bars.map('offset-x').from(function (index) {
                return that.X_CENTRE_OFFSET - 90 + ((index % NUM_HOURS_IN_DAY) * (BAR_WIDTH+1));
            });
            this.bars.map('length').from(function (index) {
                var values = this.bin.value,
                    tagIndex = Math.floor( index/NUM_HOURS_IN_DAY ),
                    hour = index % NUM_HOURS_IN_DAY,
                    value = values[tagIndex],
                    maxPercentage = TwitterUtil.getMaxCountByTimePercentage( value ),
                    countByTimePerc = TwitterUtil.getCountByTimePercentage( value, hour ),
                    relativePercentage = countByTimePerc / maxPercentage || 0;

                return relativePercentage * BAR_LENGTH;
            });
            this.bars.map('opacity').from( function() {
                return that.opacity;
            });

            this.bars.on('click', function(event) {
                var data = event.data,
                    value = data.bin.value[ Math.floor(event.index[0]/NUM_HOURS_IN_DAY)  ],
                    tag = value.tag;
                that.clickOn( tag, data, value );
                return true; // swallow event
            });
            this.bars.on('mouseover', function(event) {
                var data = event.data,
                    value = data.bin.value[ Math.floor(event.index[0]/NUM_HOURS_IN_DAY) ],
                    tag = value.tag;
                that.hoverOn( tag, data, value );
                that.nodeLayer.all().where(data).redraw( new aperture.Transition( 100 ) );
            });
            this.bars.on('mouseout', function(event) {

                that.hoverOff();
                that.nodeLayer.all().where(event.data).redraw( new aperture.Transition( 100 ) );
            });

        },


        createLabels: function () {

            var that = this,
                MAX_TITLE_CHAR_COUNT = 9;

            this.tagLabels = this.nodeLayer.addLayer(aperture.LabelLayer);
            this.tagLabels.map('offset-x').asValue(that.X_CENTRE_OFFSET + 16);
            this.tagLabels.map('text-anchor').asValue('start');
            this.tagLabels.map('font-outline').asValue(this.BLACK_COLOUR);
            this.tagLabels.map('font-outline-width').asValue(3);
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
            this.tagLabels.map('visible').from(function() {
                return that.visibility;
            });
            this.tagLabels.map('font-size').from( function(index) {
                var layerState = that.layerState,
                    hoverState = layerState.getHoverState(),
                    hasHoverState = layerState.hasHoverState(),
                    tileFocus = layerState.getTileFocus(),
                    hoveredTag = hoverState.tag,
                    tag = this.bin.value[index].tag,
                    fontSize = 12;

                if ( hasHoverState && hoveredTag === tag && this.tilekey === tileFocus ) {
                    fontSize += 4;
                }
                return fontSize;
            });
            this.tagLabels.map('label-count').from(function() {
                return TwitterUtil.getTagCount( this.bin.value, NUM_TAGS_DISPLAYED );
            });
            this.tagLabels.map('text').from(function (index) {
                var str = this.bin.value[index].tag;
                if (str.length > MAX_TITLE_CHAR_COUNT) {
                    str = str.substr(0, MAX_TITLE_CHAR_COUNT) + "...";
                }
                return str;
            });
            this.tagLabels.map('offset-y').from(function (index) {
                return that.Y_CENTRE_OFFSET - that.getYOffset(this.bin.value, index) - 5;
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

    return TagsByTimeSentiment;
});
