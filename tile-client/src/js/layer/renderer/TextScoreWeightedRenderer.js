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
        MAX_WORDS_DISPLAYED = 5,
        MIN_BAR_WIDTH = 30,
        MAX_BAR_WIDTH = 110,
        injectCss;

    injectCss = function( spec ) {
        var i, j;
        if ( spec.text.themes ) {
            for ( i=0; i<spec.text.themes.length; i++ ) {
                spec.text.themes[i].injectTheme({
                    selector: ".text-score-label",
                    parentSelector: ".text-score-entry"
                });
                spec.text.themes[i].injectTheme({
                    selector: ".text-score-entry-count",
                    parentSelector: ".text-score-entry"
                });
            }
            for ( i=0; i<spec.weights.length; i++ ) {
                for ( j=0; j<spec.weights[i].themes.length; j++ ) {
                    spec.weights[i].themes[j].injectTheme( {
                        selector: ".text-score-weight-" + i,
                        parentSelector: ".text-score-entry"
                    });
                }
            }
        }
    };

    /**
     * Instantiate a TextScoreRenderer object.
     * @class TextScoreRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a text label scaled by its
     * frequency.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     text: {
     *         textKey  {String|Function} - The attribute for the text in the data entry.
     *         countKey {String|Function} - The attribute for the count in the data entry.
     *         themes   {Array}  - The array of RenderThemes to be attached to this component.
     *     },
     *     weights: [{
     *         weightKey {Array|Function} - The attributes for the weights in the data entry
     *         themes    {Array}  - The array of RenderThemes to be attached to this component.
     *     }],
     *     threshold: {number} - The count threshold at which to de-saturate the colors.
     * }
     * </pre>
     */
    function TextScoreWeightedRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values[0].value";
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    TextScoreWeightedRenderer.prototype = Object.create( Renderer.prototype );

    /**
     * Returns the entry selector unique to this Renderer Implementation.
     * @memberof TextScoreRenderer
     * @private
     *
     * @returns {String} The entry DOM selector.
     */
    TextScoreWeightedRenderer.prototype.getEntrySelector = function() {
        return ".text-score-entry";
    };

    /**
     * Implementation specific rendering function.
     * @memberof TextScoreRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    TextScoreWeightedRenderer.prototype.render = function( data ) {

        var ENTRY_HEIGHT = 30,
            BAR_HEIGHT = 6,
            BOTTOM_OFFSET = ENTRY_HEIGHT - BAR_HEIGHT - 2,
            text = this.spec.text,
            weights = this.spec.weights,
            threshold = this.spec.threshold || 5,
            textKey = text.textKey,
            countKey = text.countKey,
            values = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            meta = this.meta[ this.map.getZoom() ],
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            minFontSize = 12,
            maxFontSize = 22,
            entries = [],
            html = '',
            desaturate,
            percentLabel,
            middleWeightIndex,
            middleWeight,
            weightCounts,
            weightTotal,
            weightPercent,
            weight,
            barWidth,
            yOffset,
            value,
            textEntry,
            fontSize,
            textCount,
            i,
            j;

        yOffset = RendererUtil.getYOffset( numEntries, 36, 122 );

        for (i=0; i<numEntries; i++) {

            value = values[i];
            entries.push( value );
            textEntry = RendererUtil.getAttributeValue( value, textKey );
            textCount = RendererUtil.getAttributeValue( value, countKey );
            desaturate = ( textCount < threshold ) ? "de-saturate" : "";
            fontSize = RendererUtil.getFontSize(
                textCount,
                meta.minimum[ countKey ],
                meta.maximum[ countKey ],
                {
                    minFontSize: minFontSize,
                    maxFontSize: maxFontSize,
                    type: "log"
            });
            weightPercent = (fontSize-minFontSize) / (maxFontSize-minFontSize);

            // parent
            html += '<div class="text-score-entry-parent" style="top:' + yOffset + 'px;">';

            // create entry
            html += '<div class="text-score-entry" style="height:'+ENTRY_HEIGHT+'px;">';

            html += '<div class="text-score-entry-count">'+ textCount +'</div>';

            // create label
            percentLabel = Math.round( (weightPercent*100) / 10 ) * 10;
            html += '<div class="text-score-label text-score-label-'+percentLabel+' '+desaturate+'" style="'
                + 'font-size:'+ fontSize +'px;'
                + 'line-height:'+ fontSize +'px;'
                + 'bottom:'+-(BOTTOM_OFFSET-fontSize)+'px;">'+textEntry+'</div>';

            if ( textCount > 0 ) {
                // create weights
                weightCounts = [];
                weightTotal = 0;
                for ( j=0; j<weights.length; j++ ) {
                    weight = RendererUtil.getAttributeValue( value, weights[j].weightKey );
                    weightTotal += weight;
                    weightCounts.push( weight );
                }
                // get the index of the middle weight
                middleWeightIndex = Math.floor( (weights.length-1) / 2 );
                // sum the amount of weight to centre the bar on the middle weight
                if ( weights.length % 2 === 0 ) {
                    middleWeight = weightCounts[ middleWeightIndex ];
                } else {
                    middleWeight = weightCounts[ middleWeightIndex ] / 2;
                }
                for ( j=middleWeightIndex-1; j>=0; j-- ) {
                    middleWeight += weightCounts[j];
                }
                barWidth = MAX_BAR_WIDTH; //MIN_BAR_WIDTH + ( MAX_BAR_WIDTH - MIN_BAR_WIDTH ) * weightPercent;
                // create bar container
                html += '<div class="text-score-weight-bar" style="'
                    + 'width:' + barWidth + 'px;'
                    + 'height:' + BAR_HEIGHT + 'px;'
                    + 'left:' + (-barWidth*(middleWeight/weightTotal)) + 'px;">';
                for ( j=0; j<weights.length; j++ ) {
                    // create bar
                    html += '<div class="text-score-weight text-score-weight-'+ j +' '+desaturate+'" style="'
                        + 'width:'+((weightCounts[j]/weightTotal)*100)+'%;"></div>';
                }
                html += '</div>';
            }

            html += '</div>';
            html += '</div>';
            html += '<div class="clear"></div>';
        }

        return {
            html: html,
            entries: entries
        };
    };

    module.exports = TextScoreWeightedRenderer;
}());
