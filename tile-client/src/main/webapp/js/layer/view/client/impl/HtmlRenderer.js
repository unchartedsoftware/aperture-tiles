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
                var MAX_FONT_SIZE = 48,
                    MIN_FONT_SIZE = 32,
                    FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                    sum = getTotalCount(data, index),
                    perc = getTotalCountPercentage(data, index),
                    scale = Math.log(sum),
                    size = ( perc * FONT_RANGE * scale) + (MIN_FONT_SIZE * perc);

                return Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
            }

            function getYOffset(data, index) {
                return 108 - 36 * (((getCount(data) - 1) / 2) - index);
            }

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    /*
                    var html,
                        $html = '<div></div>',
                        $bar,
                        $label,
                        i,
                        count = getCount( this );

                    for (i=0; i<count; i++) {

                        // bar
                        html = '<div class="sentiment-bars"';
                        html += 'style=" position:absolute; top:' +  (getYOffset(i) + 40) + 'px;';
                        html += 'font-size:' + getFontSize(this, i) +'px; ">';
                        html += "</div>";

                        $bar = $(html);

                        // label
                        html = '<div class="sentiment-labels"';
                        html += 'style=" position:absolute; top:' +  getYOffset(i) + 'px;';
                        html += 'font-size:' + getFontSize(this, i) +'px; ">';
                        html += this.bin.value[i].tag;
                        html += "</div>";

                        $label = $(html);

                        $bar.hover( function() {
                            $bar.addClass('hover');
                            $label.addClass('hover');
                        }, function() {
                            $bar.removeClass('hover');
                            $label.removeClass('hover');
                        });
                    }
                    */


                    var html = '',
                        i,
                        count = getCount( this );

                    for (i=0; i<count; i++) {

                        html += '<div class="sentiment-bars"';
                        html += 'style=" position:absolute; top:' +  (getYOffset(this, i) + 40) + 'px;';
                        html += 'font-size:' + getFontSize(this, i) +'px; ">';
                        html += "</div>";
                    }

                    for (i=0; i<count; i++) {
                        html += '<div class="sentiment-labels"';
                        html += 'style=" position:absolute; top:' +  getYOffset(this, i) + 'px;';
                        html += 'font-size:' + getFontSize(this, i) +'px; ">';
                        html += this.bin.value[i].tag;
                        html += "</div>";
                    }

                    return html;

                },
                css: {
                    'z-index' : 1000
                }
            }));

        },

        redraw: function( allData, tilekeys ) {

            this.nodeLayer.all( allData ).where( tilekeys ).redraw();
        }


    });

    return HtmlRenderer;
});
