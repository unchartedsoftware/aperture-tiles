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

define( function() {
    "use strict";

    function GraphNodeRenderer( spec ) {
        var i;
        this.spec = spec;
        for ( i=0; i<spec.node.themes.length; i++ ) {
            spec.node.themes[i].injectTheme({
                elemClass: "community-node",
                attribute: "background-color"
            });
        }
        for ( i=0; i<spec.criticalNode.themes.length; i++ ) {
            spec.criticalNode.themes[i].injectTheme({
                elemClass: "community-node community-critical-node",
                attribute: "background-color"
            });
        }
        for ( i=0; i<spec.parentNode.themes.length; i++ ) {
            spec.parentNode.themes[i].injectTheme({
                elemClass: "community-parent-node",
                attribute: "background-color"
            });
        }
    }

    GraphNodeRenderer.prototype.createHtml = function( data ) {

        var GRAPH_COORD_RANGE = 256,
            BORDER_WIDTH = 2,
            spec = this.spec,
            communities = data.tile.values[0].value[0].communities,
            scale = Math.pow( 2, this.map.getZoom() ),
            range =  GRAPH_COORD_RANGE / scale,
            className,
            community,
            radius,
            diameter,
            parentRadius,
            parentDiameter,
            html = '',
            i, x, y, px, py;

        for (i=0; i<communities.length; i++) {

            community = communities[i];

            // get node position, radius, and diameter
            x = ( community[ spec.node.x ] % range ) * scale;
            y = ( community[ spec.node.y ] % range ) * scale;
            radius = community[ spec.node.radius ] * scale;
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
                px = ( community[ spec.parentNode.x ] % range ) * scale;
                py = ( community[ spec.parentNode.y ] % range ) * scale;
                parentRadius = community[ spec.parentNode.radius ] * scale;
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

        return html;
    };

    return GraphNodeRenderer;
});