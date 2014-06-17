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



    var ClientNodeLayer = require('../ClientNodeLayer'),
        HtmlLayer = require('../HtmlLayer'),
        TwitterUtil = require('./TwitterUtil'),
        TwitterHtmlRenderer = require('./TwitterHtmlRenderer'),
        NUM_TAGS_DISPLAYED = 8,
        TagsByTimeSentimentHtml;



    TagsByTimeSentimentHtml = TwitterHtmlRenderer.extend({
        ClassName: "TagsByTimeSentimentHtml",

        init: function( map) {

            this._super( map );

            this.nodeLayer = new ClientNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });

            this.createLayer();
        },


        addClickStateClassesGlobal: function() {

            var selectedTag = this.layerState.getClickState().tag,
                selectedTagEnglish = this.layerState.getClickState().translatedTag,
                $elements = $(".tags-by-time");

            // top text sentiments
            $elements.filter( function() {
                return $(this).text() !== selectedTag &&
                       $(this).text() !== selectedTagEnglish;
            }).addClass('greyed').removeClass('clicked');

            $elements.filter( function() {
                return $(this).text() === selectedTag ||
                       $(this).text() === selectedTagEnglish;
            }).removeClass('greyed').addClass('clicked');

        },


        removeClickStateClassesGlobal: function() {

            $(".tags-by-time").removeClass('greyed clicked');
        },


        createLayer : function() {

            var that = this;

            function getYOffset( values, index ) {
                var SPACING = 20;
                return 113 - ( (( TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED ) - 1) / 2 ) - index ) * SPACING;
            }

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var tilekey = this.tilekey,
                        html = '',
                        $html = $('<div id="'+tilekey+'" class="aperture-tile"></div>'),
                        $elem,
                        $translate,
                        values = this.bin.value,
                        value,
                        maxPercentage, relativePercent,
                        visibility,
                        i, j,
                        tag,
                        count = TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED );

                    // create translate button
                    $translate = that.createTranslateLabel( tilekey );

                    $html.append( $translate );

                    for (i=0; i<count; i++) {

                        value = values[i];
                        tag = TwitterUtil.trimLabelText( that.getTopic( value, tilekey ), 10 );
                        maxPercentage = TwitterUtil.getMaxPercentageByType( value, 'PerHour' );

                        html = '<div class="tags-by-time" style="top:' +  getYOffset( values, i ) + 'px;">';

                        // create count chart
                        html += '<div class="tags-by-time-left">';
                        for (j=0; j<24; j++) {
                            relativePercent = ( TwitterUtil.getPercentageByType( value, j, 'PerHour' ) / maxPercentage ) * 100;
                            visibility = (relativePercent > 0) ? '' : 'hidden';
                            html += '<div class="tags-by-time-bar" style="visibility:'+visibility+';height:'+relativePercent+'%; top:'+(100-relativePercent)+'%;"></div>';
                        }
                        html += '</div>';

                        // create tag label
                        html += '<div class="tags-by-time-right">';
                        html +=     '<div class="tags-by-time-label">'+tag+'</div>';
                        html += '</div>';

                        html += '</div>';

                        $elem = $(html);

                        that.setMouseEventCallbacks( $elem, this, value );
                        that.addClickStateClasses( $elem, value.topic );

                        $html.append( $elem );
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