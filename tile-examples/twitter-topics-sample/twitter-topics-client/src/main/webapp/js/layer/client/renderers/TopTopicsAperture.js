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



    var ApertureRenderer = require('./ApertureRenderer'),
        TopTopicsAperture;



    TopTopicsAperture = ApertureRenderer.extend({
        ClassName: "TopTopicsAperture",

        init: function( map ) {

            this._super( map );
            this.createNodeLayer();
            this.createLayer();
        },

        registerLayer: function( layerState ) {

            var that = this;

            this._super( layerState );

            this.layerState.addListener( function(fieldName) {

                var layerState = that.layerState;

                switch (fieldName) {

                    case "clickState":

                        that.nodeLayer.all().redraw( new aperture.Transition( 100 ) );
                        break;

                    case "hoverState":
                        that.nodeLayer.all().where('tilekey', layerState.getHoverState().tilekey ).redraw( new aperture.Transition( 100 ) );
                        break;
                }
            });

        },


        createNodeLayer: function() {

            this.nodeLayer = this.map.addApertureLayer( aperture.geo.MapNodeLayer );
            this.nodeLayer.map('latitude').from('latitude');
            this.nodeLayer.map('longitude').from('longitude');
        },


        createLayer: function() {

            var that = this,
                MAX_TOPICS = 5;

            function getYOffset( numTopics, index ) {
                var SPACING =  36;
                return 118 - ( (( numTopics - 1) / 2 ) - index ) * SPACING;
            }

            this.tagLabels = this.nodeLayer.addLayer( aperture.LabelLayer );
            this.tagLabels.map('offset-x').asValue( this.Y_CENTRE_OFFSET );
            this.tagLabels.map('text-anchor').asValue( 'middle' );
            this.tagLabels.map('font-outline').asValue( '#000000' );
            this.tagLabels.map('font-outline-width').asValue( 3 );
            this.tagLabels.map('fill').from( function( index ) {
                var clickState;
                if ( that.layerState.hasClickState() ) {
                    clickState = that.layerState.getClickState();
                    if ( this.tilekey === clickState.tilekey && index === clickState.index ) {
                        return "blue";
                    }
                }
                return "white";
            });
            this.tagLabels.map('font-size').from( function( index ) {
                var hoverState;
                if ( that.layerState.hasHoverState() ) {
                    hoverState = that.layerState.getHoverState();
                    if ( this.tilekey === hoverState.tilekey && index === hoverState.index ) {
                        return 28;
                    }
                }
                return 21;
            });

            this.tagLabels.map('visible').from( function() {
                return that.visibility;
            });
            this.tagLabels.map('opacity').from( function() {
                return that.opacity;
            });

            this.tagLabels.map('label-count').from( function() {
                return Math.min( this.bin.value.length, MAX_TOPICS );
            });
            this.tagLabels.map('text').from( function( index ) {
                return this.bin.value[index].topic;
            });

            this.tagLabels.map('offset-y').from( function( index ) {
                return getYOffset( Math.min( this.bin.value.length, MAX_TOPICS ), index );
            });

            this.tagLabels.on('click', function(event) {
                that.layerState.setClickState({
                    tilekey: event.data.tilekey,
                    index: event.index[0],
                    type: "aperture"
                });
                return true; // swallow event
            });
            this.tagLabels.on('mouseover', function(event) {
                that.layerState.setHoverState({
                    tilekey: event.data.tilekey,
                    index: event.index[0],
                    type: "aperture"
                });
            });
            this.tagLabels.on('mouseout', function(event) {
                that.layerState.setHoverState({});
            });
        }

    });

    return TopTopicsAperture;
});
