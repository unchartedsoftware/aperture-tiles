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
        TwitterUtil = require('./TwitterUtil'),
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

        getTopic: function( value ) {
            return value.topic;
        },

        clickOn: function( tag, data, value ) {

            this.layerState.setClickState({
                tag: tag,
                data: data,
                value : value
            });
            // create details here so only one is created
            this.createDetailsOnDemand();
        },


        clickOff: function() {

            this.layerState.setClickState({});
        },


        hoverOn: function( tag, data, value ) {

            this.layerState.setHoverState({
                tag: tag,
                data: data,
                value : value
            });
        },


        hoverOff: function() {
            this.layerState.setHoverState({});
        },


        createTranslateLabel: function () {

            var that = this,
                isHoveredOn = false;

            this.translateLabel = this.nodeLayer.addLayer(aperture.LabelLayer);

            this.translateLabel.map('visible').from(function() {
                return that.visibility &&
                       that.clientState.activeCarouselTile === this.tilekey;
            });

            this.translateLabel.map('fill').from( function() {

                if (that.isTileTranslated(this.tilekey)) {
                    return that.WHITE_COLOUR;
                }
                return that.LIGHT_GREY_COLOUR;
            });

            this.translateLabel.map('cursor').asValue('pointer');

            this.translateLabel.on('click', function(event) {
                that.toggleTileTranslation(event.data.tilekey);
                that.nodeLayer.all().where(event.data).redraw();
                return true; // swallow event
            });

            this.translateLabel.on('mousemove', function(event) {
                isHoveredOn = true;
                that.nodeLayer.all().where(event.data).redraw();
                return true; // swallow event
            });

            this.translateLabel.on('mouseout', function(event) {
                isHoveredOn = false;
                that.nodeLayer.all().where(event.data).redraw();
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