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
        injectCss;

    injectCss = function( spec ) {
        var i;
        if ( spec.text.themes ) {
            for ( i=0; i<spec.text.themes.length; i++ ) {
                spec.text.themes[i].injectTheme({
                    selector: ".text-score-label",
                    parentSelector: ".text-score-entry"
                });
            }
        }
    };

    function TextScoreRenderer( spec ) {
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    TextScoreRenderer.prototype = Object.create( Renderer.prototype );

    TextScoreRenderer.prototype.getEntrySelector = function() {
        return ".text-score-entry";
    };

    TextScoreRenderer.prototype.render = function( data ) {

        var text = this.spec.text,
            textKey = text.textKey,
            countKey = text.countKey,
            meta = this.meta[ this.map.getZoom() ],
            values = data.tile.values[0].value,
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            entries = [],
            totalCount,
            yOffset,
            html = '',
            value,
            textEntry,
            fontSize,
            textCount,
            i;

         // get maximum count for layer if it exists in meta data
        totalCount = meta.minMax.max[ countKey ];
        yOffset = RendererUtil.getYOffset( numEntries, 36, 122 );

        for (i=0; i<numEntries; i++) {

            value = values[i];
            entries.push( value );
            textEntry = RendererUtil.getAttributeValue( value, textKey );
            textCount = RendererUtil.getAttributeValue( value, countKey );
            fontSize = RendererUtil.getFontSize( textCount, totalCount );

            // parent
            html += '<div class="text-score-entry-parent" style="top:' + yOffset + 'px;">';

            // create entry
            html += '<div class="text-score-entry">';
            // create label
            html += '<div class="text-score-label" style="'
                  + 'font-size:'+ fontSize +'px;'
                  + 'line-height:'+ fontSize +'px;">'+textEntry+'</div>';
            html += '</div>';
            html += '</div>';

            html += '<div class="clear"></div>';
        }

        return {
            html: html,
            entries: entries
        };
    };

    module.exports = TextScoreRenderer;
}());