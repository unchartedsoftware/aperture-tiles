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
            for ( i = 0; i < spec.point.themes.length; i++ ) {
                spec.point.themes[i].injectTheme({
                    selector: ".point-annotation-fill",
                    parentSelector: ".point-annotation-aggregate"
                });
            }
        }
        if ( spec.aggregate.themes ) {
            for ( i = 0; i < spec.aggregate.themes.length; i++ ) {
                spec.aggregate.themes[i].injectTheme({
                    selector: ".point-annotation-border",
                    parentSelector: ".point-annotation-aggregate"
                });
            }
        }
    };

    /**
     * Instantiate a PointAggregateRenderer object.
     * @class PointAggregateRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders clusters of circular points for each
     * data value group.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     point: {
     *         xKey   {String|Function} - The attribute for the x coordinate.
     *         yKey   {String|Function} - The attribute for the y coordinate.
     *         themes {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     *     aggregate: {
     *         themes {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function PointAggregateRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values";
        spec.point = spec.point || {};
        spec.point.xKey = spec.point.xKey || 'x';
        spec.point.yKey = spec.point.yKey || 'y';
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    PointAggregateRenderer.prototype = Object.create( Renderer.prototype );

    /**
     * Implementation specific rendering function.
     * @memberof PointAggregateRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    PointAggregateRenderer.prototype.render = function( data ) {

        var spec = this.spec,
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
        tilePos = MapUtil.getTopLeftViewportPixelForTile( this.map, tilekey );

        // for each bin
        for ( i=0; i<values.length; i++ ) {

            value = values[i].value;

            if ( value.length === 0 ) {
                continue;
            }
            entries.push( value );

            html += '<div class="point-annotation-aggregate">';

            for ( j=0; j<value.length; j++ ) {

                // get position in viewport space
                position = MapUtil.getViewportPixelFromCoord( this.map, value[j][point.xKey], value[j][point.yKey] );
                // get relative position from tile top left
                offset = {
                    x: position.x - tilePos.x,
                    y: position.y - tilePos.y
                };
                // prevent creating two annotations on the exact same pixel
                positionKey = Math.floor( offset.x ) + "," + Math.floor( offset.y );
                if ( !positionMap[ positionKey ] ) {
                    positionMap[ positionKey ] = true;
                    html += '<div class="point-annotation point-annotation-fill" style="'
                          + 'left:' + offset.x + 'px;'
                          + 'top:' + offset.y + 'px;'
                          + 'border-width: 2px;"></div>'
                          + '<div class="point-annotation point-annotation-border" style="'
                          + 'left:' + offset.x + 'px;'
                          + 'top:' + offset.y + 'px;'
                          + 'border-width: 2px"></div>';
                }
            }

            html += '</div>';
        }
        return {
            html: html,
            entries: entries
        };
    };

    module.exports = PointAggregateRenderer;
}());