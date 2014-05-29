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
        HtmlLayer = require('../HtmlLayer'),
        ClientNodeLayer = require('../ClientNodeLayer'),
        HtmlRenderer;



    HtmlRenderer = ClientRenderer.extend({
        ClassName: "HtmlRenderer",

        /**
         * Constructs a client render layer object
         * @param id the id string for the render layer
         */
        init: function( map) {

            var that = this,
                SCALE = 1.5;

            this._super(map);

            this.nodeLayer = new ClientNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });

            function getCount(data) {
                return Math.min( data.bin.value.length, 5 );
            }

            function getSentimentPercentage(data, index, type) {
                return (data.bin.value[index][type] / data.bin.value[index].count) || 0;
            }

            function getTotalCount(data, index) {
                var i,
                    sum = 0,
                    n = getCount(data);
                for (i=0; i<n; i++) {
                    sum += data.bin.value[i].count;
                }
                return sum;
            }

            function getTotalCountPercentage(data, index) {
                var i,
                    sum = 0,
                    n = getCount(data);
                for (i=0; i<n; i++) {
                    sum += data.bin.value[i].count;
                }
                return (data.bin.value[index].count/sum) || 0;
            }

            function getFontSize( data, index ) {
                var MAX_FONT_SIZE = 28 * SCALE,
                    MIN_FONT_SIZE = 12 * SCALE,
                    FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                    sum = getTotalCount(data, index),
                    perc = getTotalCountPercentage(data, index),
                    scale = Math.log(sum),
                    size = ( perc * FONT_RANGE * scale) + (MIN_FONT_SIZE * perc);

                return Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
            }

            function getYOffset(data, index) {
                return 98 - 36 * (((getCount(data) - 1) / 2) - index);
            }

            function trimLabelText(data, index) {
                var MAX_LABEL_CHAR_COUNT = 9,
                    str = data.bin.value[index].tag;
                if (str.length > MAX_LABEL_CHAR_COUNT) {
                    str = str.substr(0, MAX_LABEL_CHAR_COUNT) + "...";
                }
                return str;
            }


            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var html = '',
                        $html, // $('<div></div>'),
                        i,
                        positivePerc, neutralPerc, negativePerc,
                        count = getCount( this );

                    for (i=0; i<count; i++) {

                        html += '<div class="top-text-sentiments" style=" top:' +  getYOffset(this, i) + 'px">';

                        positivePerc = getSentimentPercentage( this, i, 'positive')*100;
                        neutralPerc = getSentimentPercentage( this, i, 'neutral')*100;
                        negativePerc = getSentimentPercentage( this, i, 'negative')*100;

                        // bar
                        html += '<div class="sentiment-bars">';
                        html +=     '<div class="sentiment-bars-negative" style="width:'+negativePerc+'%;"></div>';
                        html +=     '<div class="sentiment-bars-neutral"  style="width:'+neutralPerc+'%;"></div>';
                        html +=     '<div class="sentiment-bars-positive" style="width:'+positivePerc+'%;"></div>';
                        html += "</div>";

                        // label
                        html += '<div class="sentiment-labels"';
                        html += 'style="font-size:' + getFontSize(this, i) +'px; ">';
                        html += trimLabelText( this, i );
                        html += "</div>";

                        html += '</div>';

                        //$html.append(html);

                        if (!$html) {
                            $html = $(html);
                        } else {
                            $html.after(html);
                        }
                        //*/
                    }

                    $html = $(html);


                    $html.click( function(event) {
                        var label = $(this).find(".sentiment-labels").text();

                        $(".top-text-sentiments").filter( function() {
                            return $(this).find(".sentiment-labels").text() !== label;
                        }).addClass('greyed');

                        $(".top-text-sentiments").filter( function() {
                            return $(this).find(".sentiment-labels").text() === label;
                        }).removeClass('greyed');

                        event.stopPropagation();
                    });

                    that.map.on( 'click', function() {
                        $(".top-text-sentiments").removeClass('greyed');
                    });

                    return $html;
                },
                css: {
                    'z-index' : 749
                }
            }));

        },

        redraw: function( allData, tilekeys ) {

            this.nodeLayer.all( allData ).where( tilekeys ).redraw();
        }


    });

    return HtmlRenderer;
});
