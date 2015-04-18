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
        injectCss;

    injectCss = function( spec ) {
        var i;
        if ( spec.node.themes ) {
            for (i = 0; i < spec.node.themes.length; i++) {
                spec.node.themes[i].injectTheme({
                    selector: ".community-node"
                });
            }
        }
        if ( spec.criticalNode.themes ) {
            for (i = 0; i < spec.criticalNode.themes.length; i++) {
                spec.criticalNode.themes[i].injectTheme({
                    selector: ".community-node.community-critical-node"
                });
            }
        }
        if ( spec.parentNode.themes ) {
            for (i = 0; i < spec.parentNode.themes.length; i++) {
                spec.parentNode.themes[i].injectTheme({
                    selector: ".community-parent-node"
                });
            }
        }
    };

    /**
     * Instantiate a GraphNodeRenderer object.
     * @class GraphNodeRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a set of graph nodes.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     node: {
     *         xKey      {String|Function} - The attribute for the x coordinate.
     *         yKey      {String|Function} - The attribute for the y coordinate.
     *         radiusKey {String|Function} - The attribute for the node radius.
     *         themes    {Array}   The array of RenderThemes to be attached to this component.
     *     },
     *     criticalNode: {
     *         flag     {String|Function} - The boolean attribute to designate critical nodes.
     *         themes   {Array}  - The array of RenderThemes to be attached to this component.
     *     },
     *     parentNode: {
     *         xKey      {String|Function} - The attribute for the parent node x coordinate.
     *         yKey      {String|Function} - The attribute for the parent node y coordinate.
     *         radiusKey {String|Function} - The attribute for the node radius.
     *         themes    {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function GraphNodeRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values[0].value[0].communities";
        spec.node = spec.node || {};
        spec.node.xKey = spec.node.xKey || 'x';
        spec.node.yKey = spec.node.yKey || 'y';
        spec.node.radiusKey = spec.node.radiusKey || 'r';
        spec.parentNode = spec.parentNode || {};
        spec.parentNode.xKey = spec.parentNode.xKey || 'parentX';
        spec.parentNode.yKey = spec.parentNode.yKey || 'parentY';
        spec.parentNode.radiusKey = spec.parentNode.radiusKey || 'parentR';
        spec.criticalNode = spec.criticalNode || {};
        spec.criticalNode.flag = spec.criticalNode.flag || 'isPrimaryNode';
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    GraphNodeRenderer.prototype = Object.create( Renderer.prototype );

    /**
     * Implementation specific rendering function.
     * @memberof GraphNodeRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    GraphNodeRenderer.prototype.render = function( data ) {

        var GRAPH_COORD_RANGE = 256,
            BORDER_WIDTH = 2,
            spec = this.spec,
            communities = RendererUtil.getAttributeValue( data, spec.rootKey ),
            scale = Math.pow( 2, this.parent.map.getZoom() ),
            range =  GRAPH_COORD_RANGE / scale,
            className,
            community,
            radius,
            diameter,
            parentRadius,
            parentDiameter,
            html = '',
            entries = [],
            i, x, y, px, py;

        for ( i=0; i<communities.length; i++ ) {

            community = communities[i];
            entries.push( community );

            // get node position, radius, and diameter
            x = ( community[ spec.node.xKey ] % range ) * scale;
            y = ( community[ spec.node.yKey ] % range ) * scale;
            radius = community[ spec.node.radiusKey ] * scale;
            diameter = radius*2;

            // don't draw node if radius < 1
            // or if it is an isolated community
            if ( radius < 1 || community.degree === 0 ) {
                continue;
            }

            if ( community[ spec.criticalNode.flag ] ) {
                className = 'community-node community-critical-node';
            } else {
                className = 'community-node';
            }

            // draw node
            html += '<div class="'+className+'" style="'
                  + 'height:'+diameter+'px;'
                  + 'width:'+diameter+'px;'
                  + 'border-radius:'+diameter+'px;'
                  + 'left:'+(x - radius - BORDER_WIDTH)+'px;'
                  + 'bottom:'+(y - radius - BORDER_WIDTH)+'px;'
                  + 'border-width:' + BORDER_WIDTH + 'px;'
                  + '"></div>';

            // only have the critical node draw the parent
            // assumes there is a critical node present in all communities
            if ( community[ spec.criticalNode.flag ] ) {

                // get parent node position, radius, and diameter
                px = ( community[ spec.parentNode.xKey ] % range ) * scale;
                py = ( community[ spec.parentNode.yKey ] % range ) * scale;
                parentRadius = community[ spec.parentNode.radiusKey ] * scale;
                parentDiameter = parentRadius * 2;

                // draw parent node
                html += '<div class="community-parent-node" style="'
                      + 'height:'+parentDiameter+'px;'
                      + 'width:'+parentDiameter+'px;'
                      + 'border-radius:'+parentDiameter+'px;'
                      + 'left:'+(px - parentRadius - BORDER_WIDTH)+'px;'
                      + 'bottom:'+(py - parentRadius - BORDER_WIDTH)+'px;'
                      + 'border-width:' + BORDER_WIDTH + 'px;'
                      + '"></div>';
            }
        }
        return {
            html: html,
            entries: entries
        };
    };

    module.exports = GraphNodeRenderer;
}());
