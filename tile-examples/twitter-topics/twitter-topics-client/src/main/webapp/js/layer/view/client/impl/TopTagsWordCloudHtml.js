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
        NUM_TAGS_DISPLAYED = 10,
        TopTextSentimentHtml;



    TopTextSentimentHtml = TwitterHtmlRenderer.extend({
        ClassName: "TopTextSentimentHtml",

        init: function( map) {

            this._super(map);

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
                $elements = $(".top-tags-cloud-word");

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

            $(".top-tags-cloud-word").removeClass('greyed clicked');
        },


        createWordCloud: function( words, frequencies, minFontSize, maxFontSize, boundingBox ) {

            var wordCounts = [],
                cloud = [],
                sum = 0, i,
                word, count, dim,
                fontSize, pos, scale,
                fontRange, borderCollisions = 0,
                intersection;

            function spiralPosition( pos ) {

                var pi2 = 2 * Math.PI,
                    circ = pi2 * pos.radius,
                    inc = ( pos.arcLength > circ/10) ? circ/10 : pos.arcLength,
                    da = inc / pos.radius,
                    nt = (pos.t+da);

                if (nt > pi2) {
                    nt = nt % pi2;
                    pos.radius = pos.radius + pos.radiusInc;
                }

                pos.t = nt;
                pos.x = pos.radius * Math.cos(nt);
                pos.y = pos.radius * Math.sin(nt);
                return pos;
            }


            function intersectsWord( position, dimensions, cloud, bb ) {
                var i,
                    box = {
                        x: position.x,
                        y: position.y,
                        height: dimensions.height,
                        width: dimensions.width
                    };

                function boxTest( a, b ) {
                    return (Math.abs(a.x - b.x) * 2 < (a.width + b.width)) &&
                           (Math.abs(a.y - b.y) * 2 < (a.height + b.height));
                }

                function overlapTest( a, b ) {
                    return ( a.x + a.width/2 > b.x+b.width/2 ||
                             a.x - a.width/2 < b.x-b.width/2 ||
                             a.y + a.height/2 > b.y+b.height/2 ||
                             a.y - a.height/2 < b.y-b.height/2 );
                }

                for (i=0; i<cloud.length; i++) {
                    if ( cloud[i] !== null && boxTest( box, cloud[i] ) ) {
                        return {
                            result : true,
                            type: 'word'
                        };
                    }
                }

                // make sure it doesn't intersect the border;
                if ( overlapTest( box, bb ) ) {
                    return {
                        result : true,
                        type: 'border'
                    };
                }

                return {
                    result : false
                };
            }

            function getWordDimensions( str, fontSize ) {

                var $temp,
                    dimension = {};

                $temp = $('<div class="top-tags-cloud-word-temp" style="visibility:hidden; font-size:'+fontSize+'px; line-height:'+fontSize+'px;">'+str+'</div>');
                $('body').append( $temp );

                dimension.width = $temp.outerWidth();
                dimension.height = $temp.outerHeight();
                $temp.remove();

                return dimension;
            }


            for (i=0; i<words.length; i++) {
                wordCounts.push({
                    word: words[i],
                    count: frequencies[i]
                });
            }

            // Get mean
            for (i=0; i<wordCounts.length; i++) {
                sum += frequencies[i];
            }

            wordCounts.sort(function(a, b) {
                return b.count-a.count;
            });

            // Assemble word cloud
            scale = Math.log(sum);
            fontRange = (maxFontSize - minFontSize);

            // assemble words in cloud
            for (i=0; i<wordCounts.length; i++) {

                word = wordCounts[i].word;
                count = wordCounts[i].count;

                fontSize = ( (count/sum) * fontRange * scale) + (minFontSize * (count / sum));
                fontSize = Math.min( Math.max( fontSize, minFontSize), maxFontSize );

                dim = getWordDimensions( word, fontSize );
                dim.height -= dim.height * 0.10;
                pos = {
                    radius : 1,
                    radiusInc : 5,
                    arcLength : 15,
                    x : 0,
                    y : 0,
                    t : 0
                };


                while( true ) {
                    // increment spiral
                    pos = spiralPosition(pos);
                    // test for intersection
                    intersection = intersectsWord( pos, dim, cloud, boundingBox );

                    if ( intersection.result === false ) {

                        cloud[i] = {
                            word: word,
                            fontSize: fontSize,
                            x:pos.x,
                            y:pos.y,
                            width: dim.width,
                            height: dim.height
                        };
                        break;

                    } else {

                        if ( intersection.type === 'border' ) {
                            // if we hit border, extend arc length
                            pos.arcLength = pos.radius;
                            borderCollisions++;
                            if ( borderCollisions > 20 ) {
                                // bail
                                cloud[i] = null;
                                break;
                            }

                        }
                    }
                }
            }

            return cloud;

        },


        createLayer : function() {

            var that = this;

            this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var DOWNSCALE_OFFSET = 1.5,
                        MAX_FONT_SIZE = 24 * DOWNSCALE_OFFSET,
                        MIN_FONT_SIZE = 8 * DOWNSCALE_OFFSET,
                        HORIZONTAL_OFFSET = 10,
                        VERTICAL_OFFSET = 14,
                        tilekey = this.tilekey,
                        $html = $('<div id="'+tilekey+'" class="aperture-tile"></div>'),
                        $elem,
                        $translate,
                        values = this.bin.value,
                        value,
                        words = [],
                        frequencies = [],
                        i,
                        cloud, cloudWord,
                        boundingBox = {
                            width:256 - HORIZONTAL_OFFSET*2,
                            height:256 - VERTICAL_OFFSET*2,
                            x:0,
                            y:0
                        },
                        count = TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED );

                    // create translate button
                    $translate = that.createTranslateLabel( tilekey );

                    $html.append( $translate );

                    for (i=0; i<count; i++) {
                        value = values[i];
                        words.push( TwitterUtil.trimLabelText( that.getTopic( value, tilekey ), 10 ) );
                        frequencies.push( value.countMonthly );
                    }

                    cloud = that.createWordCloud( words, frequencies, MIN_FONT_SIZE, MAX_FONT_SIZE, boundingBox );

                    for (i=cloud.length-1; i>=0; i--) {

                        cloudWord = cloud[i];

                        if (!cloudWord) {
                            continue;
                        }

                        value = values[i];

                        $elem = $('<div class="top-tags-cloud-word" style="'
                            + 'font-size:'+cloudWord.fontSize+'px;'
                            + 'line-height:'+cloudWord.fontSize+'px;'
                            + 'left:'+(128+cloudWord.x-(cloudWord.width/2))+'px;'
                            + 'top:'+(128+cloudWord.y-(cloudWord.height/2))+'px;'
                            + 'width:'+cloudWord.width+'px;'
                            + 'height:'+cloudWord.height+'px;">'+cloudWord.word+'</div>');

                        that.setMouseEventCallbacks( $elem, this, value );
                        that.addClickStateClasses( $elem, value.topic );
                        $html.append( $elem );
                    }

                    return $html;
                }
            }));

        }


    });

    return TopTextSentimentHtml;
});