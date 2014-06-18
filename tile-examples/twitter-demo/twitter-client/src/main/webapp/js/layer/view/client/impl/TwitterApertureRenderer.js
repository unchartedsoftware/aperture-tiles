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


define(function (require) {
    "use strict";



    var ApertureRenderer = require('../ApertureRenderer'),
        TwitterUtil = require('./TwitterSentimentUtil'),
        DetailsOnDemand = require('./DetailsOnDemandHtml'),
        TwitterApertureRenderer;


    TwitterApertureRenderer = ApertureRenderer.extend({
        ClassName: "TwitterApertureRenderer",

        init: function( map) {
            this._super( map );

            this.BLACK_COLOUR = '#000000';
            this.DARK_GREY_COLOUR = '#222222';
            this.GREY_COLOUR = '#666666';
            this.LIGHT_GREY_COLOUR = '#999999';
            this.WHITE_COLOUR = '#FFFFFF';
            this.BLUE_COLOUR = '#09CFFF';
            this.DARK_BLUE_COLOUR  = '#069CCC';
            this.PURPLE_COLOUR = '#D33CFF';
            this.DARK_PURPLE_COLOUR = '#A009CC';
            this.YELLOW_COLOUR = '#F5F56F';
        },

        registerLayer: function( layerState ) {

            var that = this;

            this._super( layerState );

            this.layerState.addListener( function( fieldName ) {

                if (fieldName === "clickState") {
                    if ( !layerState.hasClickState() ) {
                        // destroy details
                        DetailsOnDemand.destroy();
                    }
                    // redraw layer
                    that.nodeLayer.all().redraw();
                }
            });

        },


        clickOn: function( data, value ) {

            this.layerState.setClickState({
                tag: value.tag,
                data: data,
                value : value
            });
            // create details here so only one is created
            this.createDetailsOnDemand();
        },


        clickOff: function() {

            this.layerState.setClickState({});
        },


        hoverOn: function( data, value ) {

            this.layerState.setHoverState({
                tag: value.tag,
                data: data,
                value : value
            });
        },


        hoverOff: function() {
            this.layerState.setHoverState({});
        },


        createCountSummaries: function () {

            var that = this;

            this.summaryLabel = this.nodeLayer.addLayer(aperture.LabelLayer);
            this.summaryLabel.map('label-count').asValue(3);
            this.summaryLabel.map('font-size').asValue(12);
            this.summaryLabel.map('font-outline').asValue(this.BLACK_COLOUR);
            this.summaryLabel.map('font-outline-width').asValue(3);
            this.summaryLabel.map('visible').from(function(){
                var layerState = that.layerState;
                return that.visibility &&
                       layerState.getTileFocus() === this.tilekey &&
                       layerState.hasHoverState();
            });
            this.summaryLabel.map('fill').from( function(index) {
                switch(index) {
                    case 0:     return that.BLUE_COLOUR;
                    case 1:     return that.WHITE_COLOUR;
                    default:    return that.PURPLE_COLOUR;
                }
            });
            this.summaryLabel.map('text').from( function(index) {
                var value = that.layerState.getHoverState().value;
                switch( index ) {
                    case 0: return "+ "+value.positive;
                    case 1: return " "+value.neutral;
                    default: return "- "+value.negative;
                }
            });

            this.summaryLabel.map('offset-y').from(function(index) {
                return 24 + (14 * index);
            });
            this.summaryLabel.map('offset-x').asValue( that.Y_CENTRE_OFFSET + 114 );
            this.summaryLabel.map('text-anchor').asValue('end');
            this.summaryLabel.map('opacity').from( function() {
                return that.opacity;
            });
        },

        createDetailsOnDemand: function() {

            var clickState = this.layerState.getClickState(),
                map = this.map,
                data = clickState.data,
                value = clickState.value,
                tilePos = map.getMapPixelFromCoord( data.longitude, data.latitude ),
                detailsPos = {
                    x: tilePos.x + 256,
                    y: map.getMapHeight() - tilePos.y
                },
                $details;

            $details = DetailsOnDemand.create( detailsPos, value, $.proxy( this.clickOff, this ) );

            map.enableEventToMapPropagation( $details, ['onmousemove', 'onmouseup'] );
            map.getRootElement().append( $details );

            TwitterUtil.centreForDetails( map, data );
        }

    });

    return TwitterApertureRenderer;

});