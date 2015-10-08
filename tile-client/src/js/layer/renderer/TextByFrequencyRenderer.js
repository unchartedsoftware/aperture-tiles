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

( function() {

    "use strict";

    var Renderer = require('./Renderer'),
        RendererUtil = require('./RendererUtil'),
        MAX_WORDS_DISPLAYED = 8,
        injectCss,
        getYOffset,
        getHighestCount;

    injectCss = function( spec ) {
        var i;
        if ( spec.text.themes ) {
            for (i = 0; i < spec.text.themes.length; i++) {
                spec.text.themes[i].injectTheme({
                    selector: ".text-by-frequency-label",
                    parentSelector: ".text-by-frequency-entry"
                });
            }
        }
        if ( spec.frequency.themes ) {
            for (i = 0; i < spec.frequency.themes.length; i++) {
                spec.frequency.themes[i].injectTheme({
                    selector: ".text-by-frequency-bar",
                    parentSelector: ".text-by-frequency-entry"
                });
            }
        }
    };

    /**
     * Utility function for positioning the labels
     */
    getYOffset = function( index, numEntries, spacing ) {
        return 118 - ( (( numEntries - 1) / 2 ) - index ) * spacing;
    };

    /**
     * Utility function to get the highest count for a topic in the tile
     */
    getHighestCount = function( values, countKey ) {
        // get the highest single count
        var highestCount = 0,
            counts = RendererUtil.getAttributeValue( values, countKey ),
            j;
        for ( j=0; j<counts.length; j++ ) {
            // get highest count
            highestCount = Math.max( highestCount, counts[j] );
        }
        return highestCount;
    };

    /**
     * Instantiate a TextByFrequencyRenderer object.
     * @class TextByFrequencyRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a histogram of the frequency of
     * a particular topic over time, with the topic text next to it.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     text: {
     *         textKey  {String|Function} - The attribute for the text in the data entry.
     *         themes   {Array}  - The array of RenderThemes to be attached to this component.
     *     },
     *     frequency: {
     *         countKey {String|Function} - The attribute for the count in the data entry.
     *         themes   {Array}  - The array of RenderThemes to be attached to this component.
     *         invertOrder {Boolean} - The boolean to determine order of chart values.  Defaults to false if not present
     *     }
     * }
     * </pre>
     */
    function TextByFrequencyRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.meta.aggregated";
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    TextByFrequencyRenderer.prototype = Object.create( Renderer.prototype );

	TextByFrequencyRenderer.prototype.getEntrySelector = function() {
		return ".text-by-frequency-label";
	};

    /**
     * Implementation specific rendering function.
     * @memberof TextByFrequencyRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    TextByFrequencyRenderer.prototype.render = function( data ) {

        var minFontSize = 14,
            maxFontSize = 24,
            spacing = 20,
            textKey = this.spec.text.textKey,
            frequency = this.spec.frequency,
            countKey = frequency.countKey,
            values = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            levelMinMax = this.parent.getLevelMinMax(),
            $html = $("<div></div>"),
            entries = [];

		var $label = $('<div class="count-summary"></div>');
		$html = $html.append( $label );

		values = values.slice( 0, numEntries );
        values.forEach( function( value, index ) {
            entries.push( value );
            var counts = RendererUtil.getAttributeValue( value, countKey );
            var text = RendererUtil.getAttributeValue( value, textKey );
            var chartSize = counts.length;
            // highest count for the topic
            var highestCount = getHighestCount( value, countKey );
            // scale the height based on level min / max
            var height = RendererUtil.getFontSize(
                highestCount,
                0,
                getHighestCount( levelMinMax.maximum, countKey ),
                {
                    minFontSize: minFontSize,
                    maxFontSize: maxFontSize,
                    type: "log"
                });

			// create container 'entry' for chart and hashtag
			var html = '<div class="text-by-frequency-entry" style="'
                // ensure constant spacing independent of height
                  + 'top:' + ( getYOffset( index, numEntries, spacing ) + ( maxFontSize - height ) ) + 'px;'
                  + 'height:' + height + 'px"></div>';
			var $entry = $(html);

			$entry.mouseover(function() {
				$label.show(); // show label
				$label.text( value.count );
			});
			$entry.mouseout(function() {
				$label.hide(); // hide label
			});

            // create chart
			var $chart = $('<div class="text-by-frequency-left"></div>');
            counts.forEach( function( count ) {
                // get the percent relative to the highest count in the tile
                var relativePercent = ( count / highestCount ) * 100;
                // if percent === 0, hide bar
                var visibility = ( relativePercent > 0 ) ? '' : 'hidden';
                // class percent in increments of 10
                var percentLabel = Math.round( relativePercent / 10 ) * 10;
                // set minimum bar length
                relativePercent = Math.max( relativePercent, 20 );
                // get width
                var width = Math.floor( (105+chartSize)/chartSize );
                var borderStr = ( width < 3 ) ? 'border: none;' : '';
                // create bar
				var bar_string = '<div class="text-by-frequency-bar text-by-frequency-bar-'+percentLabel+'" style="'
                    + 'visibility:'+visibility+';'
                    + 'height:'+relativePercent+'%;'
                    + 'width:'+ width +'px;'
                    + borderStr
                    + 'top:'+(100-relativePercent)+'%;"></div>';
				var $chartBar = $(bar_string);
				$chart.append($chartBar);
            });
			$entry.append( $chart );
            // create tag label
			var $labelTag = $('<div class="text-by-frequency-right"></div>');
			var label = '<div class="text-by-frequency-label" style="' +
                'font-size:'+height+'px;' +
                'line-height:'+height+'px;' +
                'height:'+height+'px">'+text+'</div>';
			var $labelText = $(label);
			$labelTag.append( $labelText );
			$entry.append( $labelTag );
			$html.append($entry);
        });

        return {
            html: $html,
            entries: entries
        };
    };

    module.exports = TextByFrequencyRenderer;
}());
