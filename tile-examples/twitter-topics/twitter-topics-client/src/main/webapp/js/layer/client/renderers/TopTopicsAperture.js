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
        PubSub = require('../../../util/PubSub'),
        TopTopicsAperture;



    TopTopicsAperture = ApertureRenderer.extend({
        ClassName: "TopTopicsAperture",

        init: function( map ) {

            this._super( map );
            this.createNodeLayer(); // instantiate the node layer data object
            this.createLayer();     // instantiate the html visualization layer
        },

        subscribeRenderer: function() {

            var that = this; // preserve 'this' context

            PubSub.subscribe( this.parent.getChannel(), function( message, path ) {

                var field = message.field,
                    value = message.value;

                switch ( field ) {

                    case "click":

                        // if a click occurs, lets redraw all nodes to ensure that any click change is refreshed
                        that.nodeLayer.all().redraw();
                        break;

                    case "hover":

                        if ( value ) {
                            // if a hover occurs, only redraw the relevant tile
                           that.nodeLayer.all().where('tilekey', value.tilekey ).redraw();
                        }
                        break;
                }
            });

            PubSub.subscribe( 'layer', function( message, path ) {

                var field = message.field;

                if ( field === "baseLayerIndex" ) {
                    that.nodeLayer.all().redraw();
                }
            });

        },


        createNodeLayer: function() {

            /*
                 Instantiate the aperture map node layer. This holds the tile data as it comes in from the tile service.
                 Here we set the longitude and latitude coordinate mappings that are used to position the individual
                 nodes on the map.
             */
            this.nodeLayer = this.map.addApertureLayer( aperture.geo.MapNodeLayer );
            this.nodeLayer.map('latitude').from('latitude');
            this.nodeLayer.map('longitude').from('longitude');
        },


        createLayer: function() {

            var that = this,
                MAX_TOPICS = 5;

            /*
                Utility function for positioning the labels
            */
            function getYOffset( numTopics, index ) {
                var SPACING =  36;
                return 116 - ( (( numTopics - 1) / 2 ) - index ) * SPACING;
            }

            /*
                Here we create and attach an individual aperture label layer to the map node layer. For every individual
                node of data in the node layer, the follow mappings will be executed with the 'this' context that of
                the node.
             */
            this.tagLabels = this.nodeLayer.addLayer( aperture.LabelLayer );
            this.tagLabels.map('offset-x').asValue( this.Y_CENTRE_OFFSET );
            this.tagLabels.map('text-anchor').asValue( 'middle' );      // center text horizontally
            this.tagLabels.map('font-outline').from( function() {
                return ( that.parent.map.getTheme() === "dark" ) ? "black" : "white";
            });
            this.tagLabels.map('font-outline-width').asValue( 2 );      // outline width
            this.tagLabels.map('fill').from( function( index ) {
                // change the fill colour dynamically based on the click state
                var click = that.parent.getClick(),
                    hover = that.parent.getHover();
                if ( click && this.tilekey === click.tilekey && index === click.index ) {
                    return "#09CFFF";
                }
                if ( hover && this.tilekey === hover.tilekey && index === hover.index ) {
                    return "#D33CFF";
                }
                return ( that.parent.map.getTheme() === "dark" ) ? "white" : "black";
            });
            this.tagLabels.map('font-size').asValue(28);

            // set the visibility and opacity mappings to that of the base ApertureRenderer attributes
            this.tagLabels.map('visible').from( function() {
                return that.visibility;
            });
            this.tagLabels.map('opacity').from( function() {
                return that.opacity;
            });

            // set the number of labels that will be rendered
            this.tagLabels.map('label-count').from( function() {
                return Math.min( this.values.length, MAX_TOPICS );
            });
            // set the text of the individual labels based on the index
            this.tagLabels.map('text').from( function( index ) {
                return this.values[index].topic;
            });
            // set the y offset based on the index of the label
            this.tagLabels.map('offset-y').from( function( index ) {
                return getYOffset( Math.min( this.values.length, MAX_TOPICS ), index );
            });

            // set the mouse event callbacks
            this.tagLabels.on('click', function(event) {
                var click = {
                    tilekey: event.data.tilekey,
                    index: event.index[0]
                };
                that.parent.setClick( click );
                return true; // swallow event
            });
            this.tagLabels.on('mouseover', function(event) {
                var hover = {
                    tilekey: event.data.tilekey,
                    index: event.index[0]
                };
                that.parent.setHover( hover );
            });
            this.tagLabels.on('mouseout', function(event) {
                that.parent.setHover( { tilekey: event.data.tilekey } );
            });
        }

    });

    return TopTopicsAperture;
});
