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

    /**
     * Instantiate a PointRenderer object.
     * @class PointRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a circular point for each data
     * value.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     point: {
     *         xKey   {String|Function} - The attribute for the x coordinate.
     *         yKey   {String|Function} - The attribute for the y coordinate.
     *         themes {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function PointRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values";
        spec.point = spec.point || {};
        spec.point.xKey = spec.point.xKey || 'x';
        spec.point.yKey = spec.point.yKey || 'y';
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    PointRenderer.prototype = Object.create( Renderer.prototype );

    /**
     * Implementation specific rendering function.
     * @memberof PointRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    PointRenderer.prototype.render = function( data ) {

        var spec = this.spec,
            map = this.parent.map,
            values = RendererUtil.getAttributeValue( data, spec.rootKey ),
            point = spec.point,
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

        // get tilekey
        tilekey = data.index.level + "," + data.index.xIndex + "," + data.index.yIndex;
        // get tile pos
        tilePos = MapUtil.getTopLeftViewportPixelForTile( map, tilekey );

        // for each bin
        for ( i=0; i<values.length; i++ ) {

            value = values[i].value;

            if ( value.length === 0 ) {
                continue;
            }

            for ( j=0; j<value.length; j++ ) {

                entries.push( value[j] );

                // get position in viewport space
                position = MapUtil.getViewportPixelFromCoord( map, value[j][point.xKey], value[j][point.yKey] );
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
                          + 'border-width: 2px"';
                    if (value[j].data.labels) {
                        html += ' title="' + value[j].data.labels + '"';
                    }
                    html += '></div>';
                }
            }
        }
        return {
            html: html,
            entries: entries
        };
    };

    module.exports = PointRenderer;
}());
