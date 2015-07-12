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
		createTopicWordsArrays,
        injectCss;

    injectCss = function( spec ) {
        var i, j;
        if ( spec.text.themes ) {
			// for each pair of light and dark themes, inject theme
            for ( i = 0; i < spec.text.themes.length; i++ ) {
				var themePair = spec.text.themes[ i ];
				for ( j = 0; j < themePair.length; j++ ) {
					themePair[j].injectTheme({
						selector: '.text-topic-' + i + '-label'
					});
				}
			}
        }
    };

	/**
     * Create an array of text words for each topic based on the entry's topic number
     */
	createTopicWordsArrays = function( entries, numTopics ) {
		var topicWordsArray = [];
		
		for ( var i = 0; i < numTopics; i++ ) {
			topicWordsArray[i] = [];
		}

		entries.forEach( function( entry ) {
			topicWordsArray[ entry.topicNumber ].push( entry.topic );
		});	
		
		return topicWordsArray;
	};

    /**
     * Instantiate a TextByTopicRenderer object.
     * @class TextByTopicRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a word cloud.
     *
     * @param spec {Object} The specification object.
     * <pre>
     *     text: {
     *         textKey   {String|Function} - The attribute for the text in the data entry.
     *         themes    {Array}  - The array of RenderThemes to be attached to this component.
	 *         numTopics {integer} - number of topics to bin the text words under
     *     }
     * }
     * </pre>
     */
    function TextByTopicRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.meta.raw";
        spec.text = spec.text || {};
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    TextByTopicRenderer.prototype = Object.create( Renderer.prototype );

	TextByTopicRenderer.prototype.getEntrySelector = function() {
		return ".text-by-topic-label";
	};

    /**
     * Implementation specific rendering function.
     * @memberof TextByTopicRenderer
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    TextByTopicRenderer.prototype.render = function( data ) {

        var text = this.spec.text,
            textKey = text.textKey,
			numTopics = text.numTopics ? text.numTopics : 3,
            entries = data.tile.meta.raw[0],
			$html = $('<div class="text-by-topic-box"></div>'),
			topicWords = [],
			index = 1,
            i, j;
	
		// need to get the first non-null array in the raw data
		while ( entries === null && index < data.tile.meta.raw.length ) {
			entries = data.tile.meta.raw[index];
			index++;
		}
        topicWords = createTopicWordsArrays( entries, numTopics, textKey );

		for ( i = 0; i < numTopics; i++ ) {
			var words = topicWords[i],
				wordsString = '';
				
			for ( j = 0; j < words.length; j++ ) {
				wordsString += words[j] + ' ';
			}
			var $words_html = $('<div class="text-topic-' + i + '-label text-topic-label">' + wordsString + '</div>');

			// add it to the group
			$html = $html.append( $words_html );
        }

        return {
            html: $html,
            entries: entries
        };
    };

    module.exports = TextByTopicRenderer;
}());
