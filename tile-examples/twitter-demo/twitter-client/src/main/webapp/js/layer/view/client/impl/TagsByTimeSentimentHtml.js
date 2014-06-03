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
        TagsByTimeSentimentHtml;



    TagsByTimeSentimentHtml = HtmlRenderer.extend({
        ClassName: "TagsByTimeSentimentHtml",

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
                $(".tags-by-time-sentiment").removeClass('greyed clicked');
                DetailsOnDemand.destroy();
                that.clientState.removeClickState('tag');
            });
        },

        createLayer : function() {

            var that = this;

            function getYOffset( value, index ) {
                var SPACING = 18;
                return 65 - ( (( TwitterUtil.getTagCount( value ) - 1) / 2 ) - index ) * SPACING;
            }

            function onClick() {
                var tag = $(this).find(".tags-by-time-label").text();

                $(".tags-by-time-sentiment").filter( function() {
                    return $(this).find(".tags-by-time-label").text() !== tag;
                }).addClass('greyed').removeClass('clicked');

                $(".tags-by-time-sentiment").filter( function() {
                    return $(this).find(".tags-by-time-label").text() === tag;
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
                        maxPercentage, relativePerc,
                        yOffset,
                        i, j,
                        tag,
                        count = TwitterUtil.getTagCount( values, 10 );

                    // create count summaries
                    $summaries = TwitterUtil.createTweetSummaries();

                    $html = $html.add( $summaries );

                    for (i=0; i<count; i++) {

                        value = values[i];
                        tag = TwitterUtil.trimLabelText( value.tag );
                        maxPercentage = TwitterUtil.getMaxCountByTimePercentage( value );

                        html = '<div class="tags-by-time-sentiment">'; // style="top:' +  getYOffset( value, i ) + 'px;">';

                        // create count chart
                        html += '<div class="tags-by-time-left">'

                        for (j=0; j<24; j++) {
                            relativePerc = ( TwitterUtil.getCountByTimePercentage( value, j ) / maxPercentage ) * 100;
                            //if (relativePerc > 0) {
                                html += '<div class="tags-by-time-bar" style=" height:'+relativePerc+'%;"></div>';
                            //}
                        }


                        html += '</div>';

                        // create tag label
                        html += '<div class="tags-by-time-right">'

                        html +=     '<div class="tags-by-time-label">'+tag+'</div>';

                        html += '</div>';

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

        },

        redraw: function( data ) {
            this.nodeLayer.all( data ).redraw();
        }


    });

    return TagsByTimeSentimentHtml;
});