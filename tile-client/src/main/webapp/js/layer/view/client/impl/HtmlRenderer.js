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

            /*
                Return the count of node entries, clamped at 5
            */
            function getCount( value ) {
                return Math.min( value.length, 5 );
            }


            /*
                Return the relative percentages of positive, neutral, and negative tweets
            */
            function getSentimentPercentages( value, index ) {
                return {
                    positive : ( value[index].positive / value[index].count )*100 || 0,
                    neutral : ( value[index].neutral / value[index].count )*100 || 0,
                    negative : ( value[index].negative / value[index].count )*100 || 0
                };
            }

            /*
                Returns the total count of all tweets in a node
            */
            function getTotalCount(value, index) {
                var i,
                    sum = 0,
                    n = getCount( value );
                for (i=0; i<n; i++) {
                    sum += value[i].count;
                }
                return sum;
            }

            /*
                Returns the percentage of tweets in a node for the respective tag
            */
            function getTotalCountPercentage( value, index ) {
                return ( value[index].count / getTotalCount( value, index ) ) || 0;
            }

            /*
                Returns a font size based on the percentage of tweets relative to the total count
            */
            function getFontSize( value, index ) {
                var MAX_FONT_SIZE = 28 * SCALE,
                    MIN_FONT_SIZE = 12 * SCALE,
                    FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
                    sum = getTotalCount( value, index ),
                    percentage = getTotalCountPercentage( value, index ),
                    scale = Math.log( sum ),
                    size = ( percentage * FONT_RANGE * scale ) + ( MIN_FONT_SIZE * percentage );
                return Math.min( Math.max( size, MIN_FONT_SIZE), MAX_FONT_SIZE );
            }

            /*
                Returns a y offset to position tag entry relative to centre of tile
            */
            function getYOffset( value, index ) {
                return 98 - ( (( getCount( value ) - 1) / 2 ) - index ) * 36;
            }

            /*
                Returns a trimmed string based on character limit
            */
            function trimLabelText( value, index ) {
                var MAX_LABEL_CHAR_COUNT = 9,
                    str = value[index].tag;
                if (str.length > MAX_LABEL_CHAR_COUNT) {
                    str = str.substr(0, MAX_LABEL_CHAR_COUNT) + "...";
                }
                return str;
            }

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

            function onClick( data ) {
                return function( event ) {
                    var tag = $(this).find(".sentiment-labels").text();

                    $(".top-text-sentiments").filter( function() {
                        return $(this).find(".sentiment-labels").text() !== tag;
                    }).addClass('greyed').removeClass('clicked');

                    $(".top-text-sentiments").filter( function() {
                        return $(this).find(".sentiment-labels").text() === tag;
                    }).removeClass('greyed').addClass('clicked');

                    that.clientState.setClickState('tag', tag );

                    that.map.panToCoord( data.longitude, data.latitude  );

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

            function onMousedown( $elem, data ) {
                return function( event ) {
                    $elem.click( onClick( data ) );
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
                        count = getCount( value );

                    // create count summaries
                    html = '<div class="sentiment-summaries">';
                    html +=     '<div class="positive-summaries"></div>';
                    html +=     '<div class="neutral-summaries"></div>';
                    html +=     '<div class="negative-summaries"></div>';
                    html += '</div>';
                    $summaries = $(html);

                    $html = $html.add( $summaries );

                    for (i=0; i<count; i++) {

                        tag = trimLabelText( value, i );
                        percentages = getSentimentPercentages( value, i );

                        html = '<div class="top-text-sentiments" style=" top:' +  getYOffset( value, i ) + 'px;">';

                        // create sentiment bars
                        html += '<div class="sentiment-bars">';
                        html +=     '<div class="sentiment-bars-negative" style="width:'+percentages.negative+'%;"></div>';
                        html +=     '<div class="sentiment-bars-neutral"  style="width:'+percentages.neutral+'%;"></div>';
                        html +=     '<div class="sentiment-bars-positive" style="width:'+percentages.positive+'%;"></div>';
                        html += "</div>";

                        // create tag label
                        html += '<div class="sentiment-labels" style="font-size:' + getFontSize( value, i ) +'px; ">'+tag+'</div>';

                        html += '</div>';

                        $elem = $(html);

                        // bind event handlers
                        $elem.mouseover( onMouseover( $summaries, value, i ) );
                        $elem.mouseout( onMouseout( $elem, $summaries ) );
                        $elem.mousedown( onMousedown( $elem, this ) );
                        $elem.mousemove( onMousemove( $elem ) );

                        injectClasses( $elem, tag );

                        $html = $html.add( $elem );
                    }

                    that.map.on( 'click', function() {
                        $(".top-text-sentiments").removeClass('greyed');
                        $(".top-text-sentiments").removeClass('clicked');
                        that.clientState.removeClickState('tag');
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