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

/**
 * A word cloud renderer. Uses a set of words and their counts to generate a word cloud.
 */
( function() {

    "use strict";

    var Renderer = require('./Renderer'),
        RendererUtil = require('./RendererUtil'),
        MAX_WORDS_DISPLAYED = 10,
        HORIZONTAL_OFFSET = 10,
        VERTICAL_OFFSET = 24,
        spiralPosition,
        intersectTest,
        overlapTest,
        intersectWord,
        getWordDimensions,
        createWordCloud,
        injectCss;

    injectCss = function( spec ) {
        var i;
        if ( spec.text.themes ) {
            for ( i = 0; i < spec.text.themes.length; i++ ) {
                spec.text.themes[i].injectTheme({
                    selector: ".word-cloud-label"
                });
            }
        }
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
    intersectTest = function( a, b ) {
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
        var box = {
                x: position.x,
                y: position.y,
                height: dimensions.height,
                width: dimensions.width
            },
            i;
        for ( i=0; i<cloud.length; i++ ) {
            if ( intersectTest( box, cloud[i] ) ) {
                return true;
            }
        }
        // make sure it doesn't intersect the border;
        if ( overlapTest( box, bb ) ) {
            // if it hits a border, increment collision count
            // and extend arc length
            position.collisions++;
            position.arcLength = position.radius;
            return true;
        }
        return false;
    };

    /**
     * Returns the pixel dimensions of the label
     */
    getWordDimensions = function( str, fontSize ) {
        var $temp,
            dimension = {};
        $temp = $('<div class="word-cloud-label-temp" style="font-size:'+fontSize+'px;">'+str+'</div>');
        $('body').append( $temp );
        dimension.width = $temp.outerWidth();
        dimension.height = $temp.outerHeight();
        $temp.remove();
        return dimension;
    };

    /**
     * Returns the word cloud words containing font size and x and y coordinates
     */
    createWordCloud = function( wordCounts, maxCount ) {
        var boundingBox = {
                width: 256 - HORIZONTAL_OFFSET*2,
                height: 256 - VERTICAL_OFFSET*2,
                x: 0,
                y: 0
            },
            cloud = [],
            i, word, count, dim,
            fontSize, pos;
        // sort words by frequency
        wordCounts.sort( function( a, b ) {
            return b.count - a.count;
        });
        // assemble word cloud
        for ( i=0; i<wordCounts.length; i++ ) {
            word = wordCounts[i].word;
            count = wordCounts[i].count;
            // get font size based on font size function
            fontSize = RendererUtil.getFontSize( count, maxCount, {
                maxFontSize: 30,
                minFontSize: 12,
                bias: -i/2
            });
            // get dimensions of word
            dim = getWordDimensions( word, fontSize );
            // starting spiral position
            pos = {
                radius : 1,
                radiusInc : 5,
                arcLength : 5,
                x : 0,
                y : 0,
                t : 0,
                collisions : 0
            };

            while( pos.collisions < 10 ) {
                // increment position in a spiral
                pos = spiralPosition( pos );
                // test for intersection
                if ( !intersectWord( pos, dim, cloud, boundingBox ) ) {
                    cloud.push({
                        word: word,
                        fontSize: fontSize,
                        x:pos.x,
                        y:pos.y,
                        width: dim.width,
                        height: dim.height
                    });
                    break;
                }
            }
        }
        return cloud;
    };

    /**
     * Instantiate a WordCloudRenderer object.
     *
     * @param spec {Object} The specification object.
     * {
     *     text: {
     *         textKey  {String} The attribute for the text in the data entry.
     *         countKey {String} The attribute for the count in the data entry.
     *         themes   {Array}  The array of RenderThemes to be attached to this component.
     *     }
     * }
     */
    function WordCloudRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values[0].value";
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    WordCloudRenderer.prototype = Object.create( Renderer.prototype );

    WordCloudRenderer.prototype.render = function( data ) {

        var text = this.spec.text,
            textKey = text.textKey,
            countKey = text.countKey,
            values = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            meta = this.meta[ this.map.getZoom() ],
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED),
            html = '',
            wordCounts = [],
            entries = [],
            maxCount,
            value,
            word,
            i,
            cloud;

         // get maximum count for layer if it exists in meta data
        maxCount = meta.minMax.max[ countKey ];

        for (i=0; i<numEntries; i++) {
            value = values[i];
            entries.push( value );
            wordCounts.push({
                word: RendererUtil.getAttributeValue( value, textKey ),
                count: RendererUtil.getAttributeValue( value, countKey )
            });
        }

        cloud = createWordCloud( wordCounts, maxCount );

        for ( i=0; i<cloud.length; i++ ) {

            word = cloud[i];
            value = values[i];

            html += '<div class="word-cloud-label" style="'
                    + 'font-size:'+word.fontSize+'px;'
                    + 'left:'+(128+word.x-(word.width/2))+'px;'
                    + 'top:'+(128+word.y-(word.height/2))+'px;'
                    + 'width:'+word.width+'px;'
                    + 'height:'+word.height+'px;">'+word.word+'</div>';
        }

        return {
            html: html,
            entries: entries
        };
    };

    module.exports = WordCloudRenderer;
}());