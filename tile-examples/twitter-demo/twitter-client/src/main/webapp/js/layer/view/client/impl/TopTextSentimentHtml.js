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
 * This module defines the base class for a client render layer. Must be
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";



    var ClientRenderer = require('../ClientRenderer'),
        ClientNodeLayer = require('../ClientNodeLayer'),
        HtmlLayer = require('../HtmlLayer'),
        TwitterUtil = require('./TwitterUtil'),
        DetailsOnDemand = require('./DetailsOnDemandHtml'),
        TopTextSentimentHtml;



    TopTextSentimentHtml = ClientRenderer.extend({
        ClassName: "TopTextSentimentHtml",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function( map) {

            var that = this;

            this._super(map);

            this.nodeLayer = new ClientNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });

            this.createLayer();

            this.map.on( 'click', function() {
                $(".top-text-sentiments").removeClass('greyed');
                $(".top-text-sentiments").removeClass('clicked');
                DetailsOnDemand.destroy();
                that.clientState.removeClickState('tag');
            });
        },

        createLayer : function() {

            var that = this;
            /*
                if user has clicked a tag entry, ensure newly created nodes are styled accordingly
            */
            function injectClasses( $elem, tag ) {
                var selectedTag = that.clientState.getClickState('tag');
                if ( selectedTag ) {
                    if ( selectedTag !== tag ) {
                        $elem.addClass('greyed');
                    } else {
                        $elem.addClass('clicked');
                    }
                }
            }

            function centreForDetails( data ) {
                var viewportPixel = that.map.getViewportPixelFromCoord( data.longitude, data.latitude ),
                    panCoord = that.map.getCoordFromViewportPixel( viewportPixel.x + that.map.getTileSize(),
                                                                   viewportPixel.y + that.map.getTileSize() );
                that.map.panToCoord( panCoord.x, panCoord.y );
            }

            function onClick( data, index ) {
                return function( event ) {

                    var tag = $(this).find(".sentiment-labels").text();

                    $(".top-text-sentiments").filter( function() {
                        return $(this).find(".sentiment-labels").text() !== tag;
                    }).addClass('greyed').removeClass('clicked');

                    $(".top-text-sentiments").filter( function() {
                        return $(this).find(".sentiment-labels").text() === tag;
                    }).removeClass('greyed').addClass('clicked');

                    that.clientState.setClickState('tag', tag );

                    centreForDetails( data );

                    var pos = that.map.getMapPixelFromCoord( data.longitude, data.latitude ),

                    $details = $('<div class="details-on-demand" style="left:'+(pos.x + 256)+'px; top:'+(that.map.getMapHeight() - pos.y)+'px;"></div>');
                    $details.append( DetailsOnDemand.create( data.bin.value[index] ) );
                    $details.draggable();

                    that.map.enableEventToMapPropagation( $details, ['onmousemove'] );
                    that.map.getRootElement().append( $details );

                    event.stopPropagation();
                };
            }

            function onMouseover( $summaries, value, index ) {
                return function( event ) {
                    $summaries.find(".positive-summaries").text( "+" + value[index].positive );
                    $summaries.find(".neutral-summaries").text( value[index].neutral );
                    $summaries.find(".negative-summaries").text("-" + value[index].negative );
                };
            }

            function onMouseout( $elem, $summaries ) {
                return function( event ) {
                    $summaries.find(".positive-summaries").text( "" );
                    $summaries.find(".neutral-summaries").text( "" );
                    $summaries.find(".negative-summaries").text( "" );
                    $elem.off('click');
                };
            }

            function onMousedown( $elem, data, index ) {
                return function( event ) {
                    $elem.click( onClick( data, index ) );
                };
            }

            function onMousemove( $elem ) {
                return function( event ) {
                    $elem.off('click');
                };
            }

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var html = '',
                        $html = $(''),
                        $elem,
                        $summaries,
                        value = this.bin.value,
                        i,
                        tag,
                        percentages,
                        count = TwitterUtil.getTagCount( value );

                    // create count summaries
                    html = '<div class="sentiment-summaries">';
                    html +=     '<div class="positive-summaries"></div>';
                    html +=     '<div class="neutral-summaries"></div>';
                    html +=     '<div class="negative-summaries"></div>';
                    html += '</div>';
                    $summaries = $(html);

                    $html = $html.add( $summaries );

                    for (i=0; i<count; i++) {

                        tag = TwitterUtil.trimLabelText( value[i].tag );
                        percentages = TwitterUtil.getSentimentPercentages( value, i );

                        html = '<div class="top-text-sentiments" style=" top:' +  TwitterUtil.getYOffset( value, i ) + 'px;">';

                        // create sentiment bars
                        html += '<div class="sentiment-bars">';
                        html +=     '<div class="sentiment-bars-negative" style="width:'+percentages.negative+'%;"></div>';
                        html +=     '<div class="sentiment-bars-neutral"  style="width:'+percentages.neutral+'%;"></div>';
                        html +=     '<div class="sentiment-bars-positive" style="width:'+percentages.positive+'%;"></div>';
                        html += "</div>";

                        // create tag label
                        html += '<div class="sentiment-labels" style="font-size:' + TwitterUtil.getFontSize( value, i ) +'px; ">'+tag+'</div>';

                        html += '</div>';

                        $elem = $(html);

                        // bind event handlers
                        $elem.mouseover( onMouseover( $summaries, value, i ) );
                        $elem.mouseout( onMouseout( $elem, $summaries ) );
                        $elem.mousedown( onMousedown( $elem, this, i ) );
                        $elem.mousemove( onMousemove( $elem ) );

                        injectClasses( $elem, tag );

                        $html = $html.add( $elem );
                    }

                    return $html;
                }
            }));

        },

        redraw: function( data ) {
            this.nodeLayer.all( data ).redraw();
        }


    });

    return TopTextSentimentHtml;
});