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



    var GenericHtmlRenderer = require('./GenericHtmlRenderer'),
        MAX_WORDS_DISPLAYED = 10,
        MAX_LETTERS_IN_WORD = 20,
        MAX_FONT_SIZE = 30,
        MIN_FONT_SIZE = 12,
        FONT_RANGE = MAX_FONT_SIZE - MIN_FONT_SIZE,
        HORIZONTAL_OFFSET = 10,
        VERTICAL_OFFSET = 24,
        trimLabelText,
        getFontSize,
        spiralPosition,
        boxTest,
        overlapTest,
        intersectWord,
        getWordDimensions,
        WordCloudRenderer;


    trimLabelText = function( str ) {
        if ( str.length > MAX_LETTERS_IN_WORD ) {
            str = str.substr( 0, MAX_LETTERS_IN_WORD ) + "...";
        }
        return str;
    };


    /**
     * Scale font size based on percentage of total count
     */
    getFontSize = function( count, totalCount ) {
        var percentage = ( count / totalCount ) || 0,
            size = ( percentage * FONT_RANGE  ) +  MIN_FONT_SIZE;
        return Math.min( Math.max( size, MIN_FONT_SIZE ), MAX_FONT_SIZE );
    };


    /**
     * Given an initial position, return a new position, incrementally spiralled
     * outwards.
     */
    spiralPosition = function( pos ) {

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
    };


    /**
     *  Returns true if bounding box a intersects bounding box b
     */
    boxTest = function( a, b ) {
        return (Math.abs(a.x - b.x) * 2 < (a.width + b.width)) &&
               (Math.abs(a.y - b.y) * 2 < (a.height + b.height));
    };


    /**
     *  Returns true if bounding box a is not fully contained inside bounding box b
     */
    overlapTest = function( a, b ) {
        return ( a.x + a.width/2 > b.x+b.width/2 ||
                 a.x - a.width/2 < b.x-b.width/2 ||
                 a.y + a.height/2 > b.y+b.height/2 ||
                 a.y - a.height/2 < b.y-b.height/2 );
    };


    /**
     * Check if a word intersects another word, or is not fully contained in the
     * tile bounding box
     */
    intersectWord = function( position, dimensions, cloud, bb ) {
        var i,
            box = {
                x: position.x,
                y: position.y,
                height: dimensions.height,
                width: dimensions.width
            };

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

        return { result : false };
    };

    /**
     * Returns the pixel dimensions of the label
     */
    getWordDimensions = function( str, fontSize ) {
        var $temp,
            dimension = {};
        $temp = $('<div class="word-cloud-word-temp" style="visibility:hidden; font-size:'+fontSize+'px;">'+str+'</div>');
        $('body').append( $temp );
        dimension.width = $temp.outerWidth();
        dimension.height = $temp.outerHeight();
        $temp.remove();
        return dimension;
    };


    WordCloudRenderer = GenericHtmlRenderer.extend({
        ClassName: "WordCloudRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {
            spec.text.textKey = spec.text.textKey || "text";
            spec.text = this.parseCountSpec( spec.text );
            spec.text = this.parseStyleSpec( spec.text );
            return spec;
        },


        getSelectableElement: function() {
            return 'word-cloud-entry';
        },


        createStyles: function() {

            var spec = this.spec,
                css;

            css = '<style id="generic-word-cloud-renderer-css-'+this.id+'" type="text/css">';

            if ( spec.text.color ) {
                css += this.generateBlendedCss( spec.text, "word-cloud-word", "color" );
            } else if ( spec.text.themes ) {
                css += this.generateThemedCss( spec.text, "word-cloud-word", "color" );
            }

            css += '</style>';

            $( document.body ).prepend( css );
        },


        createWordCloud: function( wordCounts, maxFrequency, boundingBox ) {

            var cloud = [],
                borderCollisions = 0,
                i, word, count, dim,
                fontSize, pos,
                intersection;

            wordCounts.sort( function( a, b ) {
                return b.count - a.count;
            });

            // Assemble word cloud
            for ( i=0; i<wordCounts.length; i++ ) {

                word = wordCounts[i].word;
                count = wordCounts[i].count;

                // get font size based on font size function
                fontSize = getFontSize( count, maxFrequency );

                dim = getWordDimensions( word, fontSize );

                // starting spiral position
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
                    intersection = intersectWord( pos, dim, cloud, boundingBox );

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
                            if ( borderCollisions > 10 ) {
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

            var that = this,
                meta = this.meta[ this.map.getZoom() ],
                spec = this.spec,
                textKey = spec.text.textKey,
                text = spec.text,
                tilekey = data.tilekey,
                values = data.values,
                $html = $([]),
                wordCounts = [],
                maxCount,
                $elem,
                value,
                word,
                wordClass,
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
                for ( i=0; i<subSpec.countKey.length; i++ ) {
                    count += that.getAttributeValue( value, subSpec.countKey[i] );
                }
                return count;
            }

            if (spec.title) {
                $html = $html.add('<div class="aperture-tile-title">'+spec.title+'</div>');
            }

            for (i=0; i<count; i++) {
                value = values[i];
                wordCounts.push({
                    word: trimLabelText( this.getAttributeValue( value, textKey ) ),
                    count: getCount( value, text )
                });
            }

            // get maximum count for layer if it exists in meta data
            maxCount = meta.minMax.max[ text.countKey ];

            cloud = this.createWordCloud( wordCounts, maxCount, boundingBox );

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

    return WordCloudRenderer;
});