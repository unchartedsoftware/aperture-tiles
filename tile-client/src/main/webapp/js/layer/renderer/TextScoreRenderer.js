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

    var RendererUtil = require('./RendererUtil'),
        MAX_WORDS_DISPLAYED = 5;

    function TextScoreRenderer( spec ) {
        var i;
        this.spec = spec;
        if ( spec.themes ) {
            for ( i=0; i<spec.themes.length; i++ ) {
                spec.themes[i].injectTheme({
                    elemClass: "text-score-label",
                    parentClass: "text-score-entry",
                    attribute: "color"
                });
            }
        }
    }

    TextScoreRenderer.prototype.createHtml = function( data ) {

        var spec = this.spec,
            meta = this.meta[ this.map.getZoom() ],
            values = data.tile.values[0].value,
            numEntries = Math.min( values.length, MAX_WORDS_DISPLAYED ),
            totalCount,
            yOffset,
            html,
            value,
            textEntry,
            fontSize,
            textCount,
            labelClass,
            i;

         // get maximum count for layer if it exists in meta data
        totalCount = meta.minMax.max[ spec.countKey ];
        yOffset = RendererUtil.getYOffset( numEntries, 36, 122 );

        html = '<div>';

        for (i=0; i<numEntries; i++) {

            value = values[i];
            textEntry = RendererUtil.getAttributeValue( value, spec.textKey );
            textCount = RendererUtil.getAttributeValue( value, spec.countKey );
            fontSize = RendererUtil.getFontSize( textCount, totalCount );
            labelClass = "text-score-label";

            // parent
            html += '<div class="text-score-entry-parent" style="top:' + yOffset + 'px;">';

            // create entry
            html += '<div class="text-score-entry">';
            // create label
            html += '<div class="text-score-label '+labelClass+'" style="'
                  + 'font-size:'+ fontSize +'px;'
                  + 'line-height:'+ fontSize +'px;">'+textEntry+'</div>';
            html += '</div>';
            html += '</div>';

            html += '<div class="clear"></div>';

        }

        return html;
    };

    return TextScoreRenderer;
});