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

define( function( require ) {
    "use strict";

    var Renderer = require('./Renderer'),
        RendererUtil = require('./RendererUtil'),
        MAX_WORDS_DISPLAYED = 8,
        injectCss;

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

    function TextByFrequencyRenderer( spec ) {
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    TextByFrequencyRenderer.prototype = Object.create( Renderer.prototype );

    TextByFrequencyRenderer.prototype.render = function( data ) {

        var textKey = this.spec.text.textKey,
            countKey = this.spec.frequency.countKey,
            values = data.tile.values[0].value,
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            html = '',
            entries = [],
            value,
            entryText,
            maxPercentage,
            relativePercent,
            visibility,
            chartSize,
            i, j;

        /*
            Utility function for positioning the labels
        */
        function getYOffset( index, numEntries ) {
            var SPACING = 20;
            return 118 - ( (( numEntries - 1) / 2 ) - index ) * SPACING;
        }

        function getChartSize( value, countKey ) {
            return value[ countKey ].length;
        }

        /*
            Returns the total count for single value
        */
        function getCount( value, index, countKey ) {
            return RendererUtil.getAttributeValue( value, countKey )[index];
        }

        /*
            Returns the total sum count
        */
        function getCountArraySum( value, countKey ) {
            var sum = 0, i;
            for ( i=0; i<value[ countKey ].length; i++ ) {
                sum += value[ countKey ][i];
            }
            return sum;
        }

        /*
            Returns the percentage count
        */
        function getPercentage( value, index, countKey ) {
            return ( getCount( value, index, countKey ) / getCountArraySum( value, countKey ) ) || 0;
        }

        /*
            Returns the maximum percentage count
        */
        function getMaxPercentage( value, countKey ) {
            var i,
                percent,
                chartSize = getChartSize( value, countKey ),
                maxPercent = 0,
                count = getCountArraySum( value, countKey );

            if (count === 0) {
                return 0;
            }

            for (i=0; i<chartSize; i++) {
                // get maximum percent
                percent = getCount( value, i, countKey ) / count;
                if (percent > maxPercent) {
                    maxPercent = percent;
                }
            }
            return maxPercent;
        }

        for (i=0; i<numEntries; i++) {

            value = values[i];
            entries.push( value );
            entryText = RendererUtil.getAttributeValue( value, textKey );
            chartSize = getChartSize( value, countKey );
            maxPercentage = getMaxPercentage( value, countKey );

            html += '<div class="text-by-frequency-entry" style="'
                  + 'top:' +  getYOffset( i, numEntries ) + 'px;">';

            // create chart
            html += '<div class="text-by-frequency-left">';
            for (j=0; j<chartSize; j++) {
                relativePercent = ( getPercentage( value, j, countKey ) / maxPercentage ) * 100;
                visibility = (relativePercent > 0) ? '' : 'hidden';
                relativePercent = Math.max( relativePercent, 20 );
                // create bar
                html += '<div class="text-by-frequency-bar" style="'
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

    return TextByFrequencyRenderer;
});