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
        SIZE_FUNCTION = 'log',
        MIN_FONT_SIZE = 13,
        MAX_FONT_SIZE = 28,
        spiralPosition,
        intersectTest,
        overlapTest,
        intersectWord,
        getWordDimensions,
        createWordCloud;

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
    createWordCloud = function( wordCounts, min, max, sizeFunction, minFontSize, maxFontSize ) {
        var boundingBox = {
                width: 256 - HORIZONTAL_OFFSET * 2,
                height: 256 - VERTICAL_OFFSET * 2,
                x: 0,
                y: 0
            },
            cloud = [],
            percent,
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
            fontSize = RendererUtil.getFontSize( count, min, max, {
                maxFontSize: maxFontSize,
                minFontSize: minFontSize,
                type: sizeFunction
            });
            // frequency percent
            percent = ((fontSize-minFontSize) / (maxFontSize-minFontSize))*100;
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

            var numWords = wordCounts.length;
            while( pos.collisions < numWords ) {
                // increment position in a spiral
                pos = spiralPosition( pos );
                // test for intersection
                if ( !intersectWord( pos, dim, cloud, boundingBox ) ) {
                    cloud.push({
                        word: word,
                        entry: wordCounts[i].entry,
                        fontSize: fontSize,
                        percentLabel: Math.round( percent / 10 ) * 10, // round to nearest 10
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
     * @class WordCloudRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a word cloud.
     *
     * @param spec {Object} The specification object.
     * <pre>
     *     text: {
     *         textKey  {String|Function} - The attribute for the text in the data entry.
     *         countKey {String|Function} - The attribute for the count in the data entry.
     *         themes   {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function WordCloudRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.meta.aggregated";
        spec.text = spec.text || {};
        Renderer.call( this, spec );
        this.injectCss( this.spec );
    }

    WordCloudRenderer.prototype = Object.create( Renderer.prototype );

    WordCloudRenderer.prototype.getEntrySelector = function() {
        return ".word-cloud-label";
    };

    WordCloudRenderer.prototype.injectCss = function( spec ) {
        var i;
        if ( spec.text.themes ) {
            for ( i = 0; i < spec.text.themes.length; i++ ) {
                spec.text.themes[i].injectTheme({
                    selector: ".word-cloud-label"
                });
                spec.text.themes[i].injectTheme({
                    selector: ".count-summary"
                });
            }
        }
    };

    WordCloudRenderer.prototype.getWordStyleClass = function ( wordProps ) {
        return 'word-cloud-label word-cloud-label-' + wordProps.percentLabel;
    };

    /**
     * Implementation specific rendering function.
     * @memberof WordCloudRenderer
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    WordCloudRenderer.prototype.render = function( data ) {

        var self = this,
            text = this.spec.text,
            textKey = text.textKey,
            countKey = text.countKey,
            values = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            numEntries = Math.min( values.length, text.maxWords || MAX_WORDS_DISPLAYED ),
            levelMinMax = this.parent.getLevelMinMax(),
            sizeFunction = text.sizeFunction || SIZE_FUNCTION,
            minFontSize = text.minFontSize || MIN_FONT_SIZE,
            maxFontSize = text.maxFontSize || MAX_FONT_SIZE,
            min = Number.MAX_VALUE,
            max = 0,
			$html = $("<div></div>"),
            wordCounts = [],
            entries = [],
            value,
            word,
            count,
            i,
            cloud;

        for ( i=0; i<numEntries; i++ ) {
            value = values[i];
            word = RendererUtil.getAttributeValue( value, textKey );
            count = RendererUtil.getAttributeValue( value, countKey );
            min = Math.min( min, count );
            max = Math.max( max, count );
            wordCounts.push({
                word: word,
                count: count,
                entry: value
            });
        }

        // if the min for the zoom level is specified in the meta, use it
        if ( levelMinMax.minimum ) {
            min = RendererUtil.getAttributeValue( levelMinMax.minimum, countKey );
        }
        // if the max for the zoom level is specified in the meta, use it
        if ( levelMinMax.maximum ) {
            max = RendererUtil.getAttributeValue( levelMinMax.maximum, countKey );
        }

        cloud = createWordCloud(
            wordCounts,
            min,
            max,
            sizeFunction,
            minFontSize,
            maxFontSize);

		var $label = $('<div class="count-summary"></div>');
		$html = $html.append( $label );

    cloud.forEach( function( word ) {
			entries.push( word.entry );
            var $wordLabel = $('<div '
                    + 'class="' + self.getWordStyleClass(word) + '" '
                    + 'style="'
                    + 'font-size:'+word.fontSize+'px;'
                    + 'left:'+(128+word.x-(word.width/2))+'px;'
                    + 'top:'+(128+word.y-(word.height/2))+'px;'
                    + 'width:'+word.width+'px;'
                    + 'height:'+word.height+'px;">'+word.word+'</div>');
			$wordLabel.mouseover(function() {
				$label.show(); // show label
				$label.text( RendererUtil.getAttributeValue( word.entry, countKey ) );
			});
			$wordLabel.mouseout(function() {
				$label.hide(); // hide label
			});
			// add it to the group
			$html = $html.append( $wordLabel );
    });

    return {
        html: $html,
        entries: entries
    };
  };

  module.exports = WordCloudRenderer;
}());
