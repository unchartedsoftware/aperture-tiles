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

    var RendererUtil = require('./RendererUtil');

    function PointRenderer( spec ) {
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

    PointRenderer.prototype.createHtml = function( data ) {

        var spec = this.spec,
            meta = this.meta[ this.map.getZoom() ],
            values = data.tile.values,
            positionMap = {},
            positionKey,
            html = '',
            position,
            offset,
            value,
            i, j;

        // for each bin
        for ( i=0; i<values.length; i++ ) {
            value = values;

            html += '<div class="point-annotation-aggregate">';

            for ( j=0; j<value.length; j++ ) {

                // get annotations position in viewport space
                position = this.map.getViewportPixelFromCoord( value[i].x, value[i].y );
                // get relative position from tile top left
                offset = {
                    x: position.x,
                    y: this.map.getMapHeight() - position.y
                };
                // prevent creating two annotations on the exact same pixel
                positionKey = Math.floor( offset.x ) + "," + Math.floor( offset.y );
                if ( !positionMap[ positionKey ] ) {
                    positionMap[ positionKey ] = true;
                    html += '<div class="point-annotation point-annotation-fill" style="'
                          + 'left:' + offset.x + 'px;'
                          + 'top:' + offset.y + 'px;"></div>'
                          + '<div class="point-annotation point-annotation-border" style="'
                          + 'left:' + offset.x + 'px;'
                          + 'top:' + offset.y + 'px;"></div>';
                }
            }

            html += '</div>';
        }
        return html;
    };

    return PointRenderer;
});