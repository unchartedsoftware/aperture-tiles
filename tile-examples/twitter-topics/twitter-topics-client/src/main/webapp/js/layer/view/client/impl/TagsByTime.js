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
        NUM_TAGS_DISPLAYED = 10,
        TagsByTime;



    TagsByTime = TwitterApertureRenderer.extend({
        ClassName: "TagsByTime",

        init: function( map ) {
            this._super( map );
            this.createLayer();
        },


        getYOffset: function( values, index ) {
            return 18 * ((( TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED ) - 1) / 2) - index);
        },


        getTotalCountPercentage: function(data, index) {
            var numDays = this.getTotalDaysInMonth(data),
                tagIndex = Math.floor(index / numDays),
                dailycount = data.bin.value[tagIndex].countDaily[numDays - 1 - (index % numDays)];

            return (dailycount / data.bin.value[tagIndex].countMonthly) || 0;
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

            this.bars = this.nodeLayer.addLayer(aperture.BarLayer);
            this.bars.map('orientation').asValue('vertical');
            this.bars.map('width').asValue(3);
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
                    tag = value.topic;

                if ( (hasClickState && selectedTag === tag) ||
                     (hasHoverState && hoveredTag === tag) ) {
                    return that.BLUE_COLOUR;
                }
                if ( hasClickState && selectedTag !== tag  ) {
                    return that.GREY_COLOUR;
                }
                return that.WHITE_COLOUR;
            });

            this.bars.map('bar-count').from( function() {
                return NUM_HOURS_IN_DAY * TwitterUtil.getTagCount( this.bin.value, NUM_TAGS_DISPLAYED );
            });

            this.bars.map('offset-y').from( function(index) {
                var tagIndex = Math.floor( index/NUM_HOURS_IN_DAY ),
                    hour = index % NUM_HOURS_IN_DAY,
                    values = this.bin.value,
                    value = values[tagIndex],
                    maxPercentage = TwitterUtil.getMaxPercentageByType( value, 'PerHour' ),
                    relativePercent = (TwitterUtil.getPercentageByType( value, hour, 'PerHour' ) / maxPercentage) || 0 ;

                return that.Y_CENTRE_OFFSET - ( relativePercent * BAR_LENGTH ) - that.getYOffset( values, tagIndex );
            });
            this.bars.map('offset-x').from( function(index) {
                var hour = index % NUM_HOURS_IN_DAY;
                return that.X_CENTRE_OFFSET - 100 + ( hour * 4);
            });
            this.bars.map('length').from(function (index) {
                var tagIndex = Math.floor( index/NUM_HOURS_IN_DAY ),
                    hour = index % NUM_HOURS_IN_DAY,
                    value = this.bin.value[tagIndex],
                    maxPercentage = TwitterUtil.getMaxPercentageByType( value, 'PerHour' ),
                    relativePercent = (TwitterUtil.getPercentageByType( value, hour, 'PerHour' ) / maxPercentage) || 0 ;

                return relativePercent * BAR_LENGTH;
            });
            this.bars.map('opacity').from( function() {
                    return that.opacity;
            });

            this.bars.on('click', function(event) {
                var data = event.data,
                    value = data.bin.value[ Math.floor(event.index[0]/NUM_HOURS_IN_DAY)  ];
                that.clickOn( data, value );
                return true; // swallow event
            });
            this.bars.on('mouseover', function(event) {
                var data = event.data,
                    value = data.bin.value[ Math.floor(event.index[0]/NUM_HOURS_IN_DAY) ];
                that.hoverOn( data, value );
                that.nodeLayer.all().where(data).redraw( new aperture.Transition( 100 ) );
            });
            this.bars.on('mouseout', function(event) {

                that.hoverOff();
                that.nodeLayer.all().where(event.data).redraw( new aperture.Transition( 100 ) );
            });

        },


        createLabels: function () {

            var that = this;

            this.tagLabels = this.nodeLayer.addLayer(aperture.LabelLayer);
            this.tagLabels.map('offset-x').asValue(that.X_CENTRE_OFFSET + 38);
            this.tagLabels.map('text-anchor').asValue('start');
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
                    tag = this.bin.value[index].topic;

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
                var str = that.getTopic( this.bin.value[index], this.tilekey );
                if (str.length > 9) {
                    str = str.substr(0,9) + "...";
                }
                return str;
            });

            this.tagLabels.map('font-size').from( function(index) {

                var layerState = that.layerState,
                    hoverState = layerState.getHoverState(),
                    hasHoverState = layerState.hasHoverState(),
                    tileFocus = layerState.getTileFocus(),
                    hoveredTag = hoverState.tag,
                    tag = this.bin.value[index].topic,
                    fontSize = 12;

                if ( hasHoverState && hoveredTag === tag && this.tilekey === tileFocus ) {
                    fontSize += 4;
                }
                return fontSize;
            });

            this.tagLabels.map('offset-y').from(function (index) {
                return that.Y_CENTRE_OFFSET - that.getYOffset( this.bin.value, index ) - 5;
            });

            this.tagLabels.map('opacity').from( function() {
                    return that.opacity;
            });

            this.tagLabels.on('click', function(event) {
                var data = event.data,
                    value = data.bin.value[ event.index[0] ];
                that.clickOn( data, value );
                return true; // swallow event
            });
            this.tagLabels.on('mouseover', function(event) {
                var data = event.data,
                    value = data.bin.value[ event.index[0] ];
                that.hoverOn( data, value );
                that.nodeLayer.all().where(data).redraw( new aperture.Transition( 100 ) );
            });
            this.tagLabels.on('mouseout', function(event) {

                that.hoverOff();
                that.nodeLayer.all().where(event.data).redraw( new aperture.Transition( 100 ) );
            });

        }


    });

    return TagsByTime;
});
