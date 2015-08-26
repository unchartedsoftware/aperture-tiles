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
        WordCloudRenderer = require('./WordCloudRenderer'),
        MAX_WORDS_DISPLAYED = 10,
        HORIZONTAL_OFFSET = 10,
        VERTICAL_OFFSET = 24,
        SIZE_FUNCTION = 'log',
        MIN_FONT_SIZE = 13,
        MAX_FONT_SIZE = 28,
        spiralPosition = WordCloudRenderer.spiralPosition,
        intersectWord = WordCloudRenderer.intersectWord,
        getWordDimensions = WordCloudRenderer.getWordDimensions,
        createWordCloud;

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
            sentiment, color,
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

            // get color based on sentiment
            sentiment = wordCounts[i].sentiment;
            switch (sentiment) {
                case "positive":
                    color = "#ADFF00";
                    break;
                case "negative":
                    color = "#FF00C2";
                    break;
                case "neutral":
                    color = "rgba(190, 190, 190, 0.85)";
                    break;
                default:
                    // no sentiment associated with this word
                    break;
            }

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
                        height: dim.height,
                        color: color
                    });
                    break;
                }
            }
        }
        return cloud;
    };

    /**
     * Instantiate a SentimentWordCloudRenderer object.
     * @class SentimentWordCloudRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a word cloud.
     *
     * @param spec {Object} The specification object.
     * <pre>
     *     text: {
     *         textKey  {String|Function} - The attribute for the text in the data entry.
     *         countKey {String|Function} - The attribute for the count in the data entry.
     *         sentimentKey {String|Function} - The attribute for the sentiment in the data entry.
     *         themes   {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function SentimentWordCloudRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.meta.aggregated";
        spec.text = spec.text || {};
        Renderer.call( this, spec );
    }

    SentimentWordCloudRenderer.prototype = Object.create( WordCloudRenderer.prototype );

    SentimentWordCloudRenderer.prototype.getEntrySelector = function() {
		return ".word-cloud-label";
	};

    /**
     * Implementation specific rendering function.
     * @memberof SentimentWordCloudRenderer
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
     SentimentWordCloudRenderer.prototype.render = function( data ) {

        var text = this.spec.text,
            textKey = text.textKey,
            countKey = text.countKey,
            sentimentKey = text.sentimentKey,
            values = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            numEntries = Math.min( values.length, text.maxWords || MAX_WORDS_DISPLAYED ),
            levelMinMax = this.parent.getLevelMinMax(),
            sizeFunction = text.sizeFunction || SIZE_FUNCTION,
            minFontSize = text.minFontSize || MIN_FONT_SIZE,
            maxFontSize = text.maxFontSize || MAX_FONT_SIZE,
            $html = $("<div></div>"),
            wordCounts = [],
            entries = [],
            value,
            min,
            max,
            i,
            cloud;

        for ( i=0; i<numEntries; i++ ) {
            value = values[i];
            wordCounts.push({
                word: RendererUtil.getAttributeValue( value, textKey ),
                count: RendererUtil.getAttributeValue( value, countKey ),
                sentiment: RendererUtil.getAttributeValue( value, sentimentKey ),
                entry: value
            });
        }

        min = RendererUtil.getAttributeValue( levelMinMax.minimum, countKey );
        max = RendererUtil.getAttributeValue( levelMinMax.maximum, countKey );

        cloud = createWordCloud(
            wordCounts,
            min,
            max,
            sizeFunction,
            minFontSize,
            maxFontSize
        );

        var $label = $('<div class="count-summary"></div>');
        $html = $html.append( $label );

        cloud.forEach( function( word ) {
            entries.push( word.entry );

            var $wordLabel = $('<div class="word-cloud-label word-cloud-label-'+word.percentLabel+'" style="'
                    + 'font-size:'+word.fontSize+'px;'
                    + 'left:'+(128+word.x-(word.width/2))+'px;'
                    + 'top:'+(128+word.y-(word.height/2))+'px;'
                    + 'width:'+word.width+'px;'
                    + 'color:'+word.color+';'
                    + 'height:'+word.height+'px;">'+word.word
                    + '</div>');

            $wordLabel.mouseover(function() {
                $label.show(); // show label
                $label.text( word.entry.total ); // 'word.entry.count' when using TopicCountAggregator
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

    module.exports = SentimentWordCloudRenderer;
}());
