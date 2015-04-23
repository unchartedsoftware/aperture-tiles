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
    getYOffset = function( index, numEntries ) {
        var SPACING = 20;
        return 118 - ( (( numEntries - 1) / 2 ) - index ) * SPACING;
    };

    /**
     * Utility function to get the highest count in the tile
     */
    getHighestCount = function( numEntries, values, countKey ) {
        // get the highest single count
        var highestCount = 0,
            value,
            counts,
            i, j;
        for ( i=0; i<numEntries; i++ ) {
            value = values[i];
            counts = value[countKey];
            for ( j=0; j<counts.length; j++ ) {
                // get highest count
                highestCount = Math.max( highestCount, counts[j] );
            }
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
        spec.frequency.invertOrder = spec.frequency.invertOrder || false;
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    TextByFrequencyRenderer.prototype = Object.create( Renderer.prototype );

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

        var textKey = this.spec.text.textKey,
            frequency = this.spec.frequency,
            countKey = frequency.countKey,
            invertOrder = frequency.invertOrder,
            values = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            percentLabel,
            html = '',
            entries = [],
            value,
            entryText,
            highestCount,
            counts,
            relativePercent,
            visibility,
            chartSize,
            index,
            i, j;

        highestCount = getHighestCount( numEntries, values, countKey );

        for ( i=0; i<numEntries; i++ ) {

            value = values[i];
            entries.push( value );
            counts = value[ countKey ];
            chartSize = counts.length;
            entryText = RendererUtil.getAttributeValue( value, textKey );

            html += '<div class="text-by-frequency-entry" style="'
                  + 'top:' + getYOffset( i, numEntries ) + 'px;">';

            // create chart
            html += '<div class="text-by-frequency-left">';
            for (j=0; j<chartSize; j++) {
                // if invertOrder is true, invert the order of iteration
                index = ( invertOrder ) ? chartSize - j - 1 : j;
                // get the percent relative to the highest count in the tile
                relativePercent = ( counts[index] / highestCount ) * 100;
                // if percent === 0, hide bar
                visibility = ( relativePercent > 0 ) ? '' : 'hidden';
                // class percent in increments of 10
                percentLabel = Math.round( relativePercent / 10 ) * 10;
                // set minimum bar length
                relativePercent = Math.max( relativePercent, 20 );
                // create bar
                html += '<div class="text-by-frequency-bar text-by-frequency-bar-'+percentLabel+'" style="'
                    + 'visibility:'+visibility+';'
                    + 'height:'+relativePercent+'%;'
                    + 'width:'+ Math.floor( (105+chartSize)/chartSize ) +'px;'
                    + 'top:'+(100-relativePercent)+'%;"></div>';
            }
            html += '</div>';

            // create tag label
            html += '<div class="text-by-frequency-right">';
            html += '<div class="text-by-frequency-label">'+entryText+'</div>';
            html += '</div>';
            html += '</div>';
        }

        return {
            html: html,
            entries: entries
        };
    };

    module.exports = TextByFrequencyRenderer;
}());
