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
        MapUtil = require('../../map/MapUtil'),
        injectCss;

    injectCss = function( spec ) {
        var i;
        if ( spec.point.themes ) {
            for (i = 0; i < spec.point.themes.length; i++) {
                spec.point.themes[i].injectTheme({
                    selector: ".point-annotation"
                });
            }
        }
    };

    function PointRenderer( spec ) {
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    PointRenderer.prototype = Object.create( Renderer.prototype );

    PointRenderer.prototype.render = function( data ) {

        var //spec = this.spec,
            //meta = this.meta[ this.map.getZoom() ],
            values = data.tile.values,
            entries = [],
            positionMap = {},
            positionKey,
            tilekey,
            tilePos,
            html = '',
            position,
            offset,
            value,
            i, j;

        // for each bin
        for ( i=0; i<values.length; i++ ) {

            value = values[i].value;

            if ( value.length === 0 ) {
                continue;
            }

            for ( j=0; j<value.length; j++ ) {

                entries.push( value[j] );

                // get annotations position in viewport space
                tilekey = data.index.level + "," + data.index.xIndex + "," + data.index.yIndex;
                tilePos = MapUtil.getTopLeftViewportPixelForTile( this.map, tilekey );
                position = MapUtil.getViewportPixelFromCoord( this.map, value[j].x, value[j].y );
                // get relative position from tile top left
                offset = {
                    x: position.x - tilePos.x,
                    y: position.y - tilePos.y
                };
                // prevent creating two annotations on the exact same pixel
                positionKey = Math.floor( offset.x ) + "," + Math.floor( offset.y );
                if ( !positionMap[ positionKey ] ) {
                    positionMap[ positionKey ] = true;
                    html += '<div class="point-annotation point-annotation-single" style="'
                          + 'left:' + offset.x + 'px;'
                          + 'top:' + offset.y + 'px;'
                          + 'border-width: 2px"></div>';
                }
            }
        }
        return {
            html: html,
            entries: entries
        };
    };

    return PointRenderer;
});