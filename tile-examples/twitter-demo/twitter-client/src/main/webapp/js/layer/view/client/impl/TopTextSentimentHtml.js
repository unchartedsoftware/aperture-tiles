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



    var HtmlRenderer = require('../HtmlRenderer'),
        ClientNodeLayer = require('../ClientNodeLayer'),
        HtmlLayer = require('../HtmlLayer'),
        TwitterUtil = require('./TwitterUtil'),
        DetailsOnDemand = require('./DetailsOnDemandHtml'),
        TopTextSentimentHtml;



    TopTextSentimentHtml = HtmlRenderer.extend({
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
                $(".top-text-sentiments").removeClass('greyed clicked');
                TwitterUtil.destroyDetailsOnDemand( DetailsOnDemand );
                that.clientState.removeClickState('tag');
            });
        },

        createLayer : function() {

            var that = this;

            function getYOffset( values, index ) {
                var SPACING =  36;
                return 95 - ( (( TwitterUtil.getTagCount( values ) - 1) / 2 ) - index ) * SPACING;
            }

            function onClick() {

                var tag = $(this).find(".sentiment-labels").text();

                $(".top-text-sentiments").filter( function() {
                    return $(this).find(".sentiment-labels").text() !== tag;
                }).addClass('greyed').removeClass('clicked');

                $(".top-text-sentiments").filter( function() {
                    return $(this).find(".sentiment-labels").text() === tag;
                }).removeClass('greyed').addClass('clicked');

                that.clientState.setClickState('tag', tag );
            }

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var html = '',
                        $html = $(''),
                        $elem,
                        $summaries,
                        values = this.bin.value,
                        value,
                        i,
                        tag,
                        percentages,
                        count = TwitterUtil.getTagCount( values );

                    // create count summaries
                    $summaries = TwitterUtil.createTweetSummaries();

                    $html = $html.add( $summaries );

                    for (i=0; i<count; i++) {

                        value = values[i];
                        tag = TwitterUtil.trimLabelText( values[i].tag );
                        percentages = TwitterUtil.getSentimentPercentages( value );

                        html = '<div class="top-text-sentiments" style=" top:' +  getYOffset( values, i ) + 'px;">';

                        // create sentiment bars
                        html += '<div class="sentiment-bars">';
                        html +=     '<div class="sentiment-bars-negative" style="width:'+percentages.negative+'%;"></div>';
                        html +=     '<div class="sentiment-bars-neutral"  style="width:'+percentages.neutral+'%;"></div>';
                        html +=     '<div class="sentiment-bars-positive" style="width:'+percentages.positive+'%;"></div>';
                        html += "</div>";

                        // create tag label
                        html += '<div class="sentiment-labels" style="font-size:' + TwitterUtil.getFontSize( values, i ) +'px; ">'+tag+'</div>';

                        html += '</div>';

                        $elem = $(html);

                        // set event handlers
                        TwitterUtil.setMouseEventCallbacks( that.map, $elem, $summaries, this, i, onClick, DetailsOnDemand );

                        TwitterUtil.injectClickStateClasses( $elem, tag, that.clientState.getClickState('tag') );

                        $html = $html.add( $elem );
                    }

                    return $html;
                }
            }));

        }


    });

    return TopTextSentimentHtml;
});