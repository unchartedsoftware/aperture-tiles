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



    var //Util = require('../../../util/Util'),
        GenericHtmlRenderer = require('./GenericHtmlRenderer'),
        MAX_WORDS_DISPLAYED = 10,
        MAX_LETTERS_IN_WORD = 20,
        DEFAULT_COLOR = '#FFFFFF',
        DEFAULT_HOVER_COLOR = '#7FFF00',
        trimLabelText,
        GenericWordCloudHtml;


    trimLabelText = function( str ) {
        if (str.length > MAX_LETTERS_IN_WORD) {
            str = str.substr( 0, MAX_LETTERS_IN_WORD ) + "...";
        }
        return str;
    };


    GenericWordCloudHtml = GenericHtmlRenderer.extend({
        ClassName: "GenericWordCloudHtml",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {

            var i;

            spec.text.textKey = spec.text.textKey || "text";
            if ( !spec.text.blend ) {
                spec.text.blend = [{
                    countKey : spec.text.countKey,
                    color : spec.text.color,
                    hoverColor : spec.text.hoverColor
                }];
            }
            for ( i=0; i<spec.text.blend.length; i++ ) {
                spec.text.blend[i].color = spec.text.blend[i].color || DEFAULT_COLOR;
                spec.text.blend[i].hoverColor = spec.text.blend[i].hoverColor || DEFAULT_HOVER_COLOR;
                spec.text.blend[i].countKey = spec.text.blend[i].countKey || "count";
            }

            if ( spec.summary ) {
                if ( !$.isArray( spec.summary ) ) {
                    spec.summary = [ spec.summary ];
                }
                for ( i=0; i<spec.summary.length; i++ ) {
                    spec.summary[i].countKey = spec.summary[i].countKey || "count";
                    spec.summary[i].color = spec.summary[i].color || DEFAULT_COLOR;
                    spec.summary[i].prefix = spec.summary[i].prefix || "";
                }
            }

            return spec;
        },


        getSelectableElement: function() {
            return 'word-cloud-entry';
        },


        createStyles: function() {

            var spec = this.spec,
                css;

            css = '<style id="generic-word-cloud-renderer-css-'+this.id+'" type="text/css">';

            // generate text css
            css += this.generateBlendedCss( spec.text.blend, "word-cloud-word", "color" );

            css += '</style>';

            $( document.body ).prepend( css );
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
                $temp = $('<div class="word-cloud-word-temp" style="visibility:hidden; font-size:'+fontSize+'px;">'+str+'</div>');
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
                //dim.height -= dim.height * 0.20;
                pos = {
                    radius : 1,
                    radiusInc : 5,
                    arcLength : 5,
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


        createHtml : function( data ) {

            var MAX_FONT_SIZE = 30,
                MIN_FONT_SIZE = 12,
                HORIZONTAL_OFFSET = 10,
                VERTICAL_OFFSET = 24,
                spec = this.spec,
                textKey = spec.text.textKey,
                text = spec.text.blend || [ spec.text ],
                tilekey = data.tilekey,
                values = data.values,
                $html = $([]),
                $elem,
                value,
                words = [],
                word,
                wordClass,
                frequencies = [],
                i,
                cloud,
                boundingBox = {
                    width:256 - HORIZONTAL_OFFSET*2,
                    height:256 - VERTICAL_OFFSET*2,
                    x:0,
                    y:0
                },
                count = Math.min( values.length, MAX_WORDS_DISPLAYED );

            /*
                Returns the total count for single value
            */
            function getCount( value, subSpec ) {
                var i, count = 0;
                for ( i=0; i<subSpec.length; i++ ) {
                    count += value[subSpec[i].countKey];
                }
                return count;
            }

            if (spec.title) {
                $html = $html.add('<div class="word-cloud-title">'+spec.title+'</div>');
            }

            $html = $html.add('<div class="count-summary"></div>');

            for (i=0; i<count; i++) {
                value = values[i];
                words.push( trimLabelText( value[ textKey ] ) );
                frequencies.push( getCount( value, text ) );
            }

            cloud = this.createWordCloud( words, frequencies, MIN_FONT_SIZE, MAX_FONT_SIZE, boundingBox );

            for (i=cloud.length-1; i>=0; i--) {

                if ( !cloud[i] ) {
                    // words may not fit and will be null
                    continue;
                }

                word = cloud[i];
                value = values[i];
                wordClass = this.generateBlendedClass( "word-cloud-word", value, text ) + "-" + this.id;

                $elem = $('<div class="word-cloud-entry"><div class="word-cloud-word '+wordClass+'" style="'
                        + 'font-size:'+word.fontSize+'px;'
                        + 'left:'+(128+word.x-(word.width/2))+'px;'
                        + 'top:'+(128+word.y-(word.height/2))+'px;'
                        + 'width:'+word.width+'px;'
                        + 'height:'+word.height+'px;">'+word.word+'</div></div>');

                this.setMouseEventCallbacks( $elem, data, value );
                this.addClickStateClassesLocal( $elem, value, tilekey );

                $html = $html.add( $elem );
            }

            return $html;

        }


    });

    return GenericWordCloudHtml;
});