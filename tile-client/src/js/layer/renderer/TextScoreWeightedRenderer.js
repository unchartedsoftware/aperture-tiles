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
        spec.rootKey = spec.rootKey || "tile.meta.aggregated";
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
            spec = this.spec,
            weights = spec.weights,
            threshold = spec.threshold || 5,
            textKey = spec.text.textKey,
            countKey = spec.text.countKey,
            values = RendererUtil.getAttributeValue( data, spec.rootKey ),
            levelMinMax = this.parent.getLevelMinMax(),
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            minFontSize = 13,
            maxFontSize = 18,
            min = Number.MAX_VALUE,
            max = 0,
            entries = [],
            html = '',
            desaturate,
            percentLabel,
            middleWeightIndex,
            middleWeight,
            weightCounts,
            weightPercent,
            weight,
            barWidth,
            yOffset,
            value,
            text,
            count,
            fontSize,
            i,
            j;

        yOffset = RendererUtil.getYOffset( numEntries, ENTRY_HEIGHT+BAR_HEIGHT, 122 );

        // if the min for the zoom level is specified in the meta, use it
        if ( levelMinMax.minimum ) {
            min = RendererUtil.getAttributeValue( levelMinMax.minimum, countKey );
        } else {
            values.forEach( function( value ) {
                min = Math.min( min, RendererUtil.getAttributeValue( value, countKey ) );
            });
        }

        // if the max for the zoom level is specified in the meta, use it
        if ( levelMinMax.maximum ) {
            max = RendererUtil.getAttributeValue( levelMinMax.maximum, countKey );
        } else {
            values.forEach( function( value ) {
                max = Math.max( max, RendererUtil.getAttributeValue( value, countKey ) );
            });
        }

        for (i=0; i<numEntries; i++) {
            value = values[i];
            entries.push( value );
            text = RendererUtil.getAttributeValue( value, textKey );
            count = RendererUtil.getAttributeValue( value, countKey );
            desaturate = ( count < threshold ) ? "de-saturate" : "";
            fontSize = RendererUtil.getFontSize(
                count,
                min,
                max,
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

            html += '<div class="text-score-entry-count">'+ count +'</div>';

            // create label
            percentLabel = Math.round( (weightPercent*100) / 10 ) * 10;
            html += '<div class="text-score-label text-score-label-'
                + percentLabel+' '+desaturate+'" style="'
                + 'font-size:'+ fontSize +'px;'
                + 'line-height:'+ fontSize +'px;'
                + 'bottom:'+-(BOTTOM_OFFSET-fontSize)+'px;">'+text+'</div>';

            if ( count > 0 ) {
                // create weights
                weightCounts = [];
                for ( j=0; j<weights.length; j++ ) {
                    weight = RendererUtil.getAttributeValue( value, weights[j].weightKey );
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
                barWidth = MAX_BAR_WIDTH;
                // create bar container
                html += '<div class="text-score-weight-bar" style="'
                    + 'width:' + barWidth + 'px;'
                    + 'height:' + BAR_HEIGHT + 'px;'
                    + 'left:' + (-barWidth*(middleWeight/count)) + 'px;">';
                for ( j=0; j<weights.length; j++ ) {
                    // create bar
                    html += '<div class="text-score-weight text-score-weight-'
                        + j +' '+desaturate+'" style="'
                        + 'width:'+((weightCounts[j]/count)*100)+'%;"></div>';
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
