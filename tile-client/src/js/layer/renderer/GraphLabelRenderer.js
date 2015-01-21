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
        if ( spec.text.themes ) {
            for ( i=0; i<spec.text.themes.length; i++ ) {
                spec.text.themes[i].injectTheme({
                    selector: ".node-label"
                });
            }
        }
    };

    /**
     * Instantiate a GraphLabelRenderer object.
     * @class GraphLabelRenderer
     * @augments Renderer
     * @classDesc A Renderer implementation that renders a set of graph labels.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     text: {
     *         xKey   {String} - The attribute for the x coordinate.
     *         yKey   {String} - The attribute for the y coordinate.
     *         themes {Array}  - The array of RenderThemes to be attached to this component.
     *     }
     * }
     * </pre>
     */
    function GraphLabelRenderer( spec ) {
        spec.rootKey = spec.rootKey || "tile.values[0].value[0].communities";
        Renderer.call( this, spec );
        injectCss( this.spec );
    }

    GraphLabelRenderer.prototype = Object.create( Renderer.prototype );

    /**
     * Implementation specific rendering function.
     * @memberof GraphLabelRenderer
     * @private
     *
     * @param {Object} data - The raw data for a tile to be rendered.
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    GraphLabelRenderer.prototype.render = function( data ) {

        var GRAPH_COORD_RANGE = 256,
            text = this.spec.text,
            meta = this.meta[ this.map.getZoom() ],
            communities = RendererUtil.getAttributeValue( data, this.spec.rootKey ),
            scale = Math.pow( 2, this.map.getZoom() ),
            range =  GRAPH_COORD_RANGE / scale,
            labelIndex = ( text.labelIndex !== undefined ) ? text.labelIndex : 0,
            metaCommunities = meta.minMax.max.communities[0],
            sizeMultiplier,
            community,
            html = "",
            fontScale,
            fontSize,
            split,
            label,
            countNorm,
            percent,
            hierLevel,
            parentIDarray = [],
            entries = [],
            x, y, i;

        function capitalize( str ) {
            return str.replace(/(?:^|,|\s)\S/g, function( a ) {
                return a.toUpperCase();
            });
        }
        // get labelSizeMultiplier value -- value between 0.001 and 1 that controls label font size scaling.
        // (Lower value caps font size for very large communities, and is better for graphs with many small communities and a few very large ones)
        sizeMultiplier = ( text.sizeMultiplier )
            ? Math.min( Math.max( text.sizeMultiplier, 0.001 ), 1.0 )
            : 0.5;

        // get graph hierarchy level for this zoom level
        // assumes same hierarchy level for all tiles at a given zoom level
        hierLevel = metaCommunities.hierLevel;

        // if hierLevel = 0, normalize label attributes by community degree
        // else normalize label attributes by num internal nodes
        countNorm = ( hierLevel === 0 )
            ? metaCommunities.degree * sizeMultiplier
            : metaCommunities.numNodes * sizeMultiplier;

        for ( i=0; i<communities.length; i++ ) {

            community = communities[i];
            entries.push( community );

            // capitalize label array, split by comma
            split = community.metadata.split(",");
            // don't draw labels for isolated communities
            // and don't render if label string is empty
            // only draw one label per parent community per tile
            // only draw up to 5 labels per tile
            if ( community.degree === 0 ||
                 split[ labelIndex ] === "" ||
                 parentIDarray.length >= 5 ||
                 parentIDarray.indexOf( community.parentID ) !== -1 ) {
                // Skip this label entry
                continue;
            }
            else {
                // add this parent ID to the list, and draw the label (below
                parentIDarray.push( community.parentID );
            }

            // get label position
            x = ( community[ text.xKey ] % range ) * scale;
            y = ( community[ text.yKey ] % range ) * scale;

            // capitalize label
            label = capitalize( split[ labelIndex ].toLowerCase() );

            // get font scale based on hierarchy level
            fontScale = ( hierLevel === 0 ) ? community.degree : community.numNodes;
            fontSize = RendererUtil.getFontSize( fontScale, countNorm );

            //
            percent = Math.min( 1, fontScale / countNorm ) + 0.5;

            html += '<div class="node-label" style="'
                  + 'left:'+x+'px;'
                  + 'bottom:'+y+'px;'
                  + 'font-size:' + fontSize + 'px;'
                  + 'line-height:' + fontSize + 'px;'
                  + 'margin-top:' + (-fontSize/2) + 'px;'
                  + 'height:' + fontSize + 'px;'
                  + 'opacity:' + percent + ';'
                  + 'z-index:' + Math.floor( fontSize ) + ';'
                  + '">'+label+'</div>';
        }
        return {
            html: html,
            entries: entries
        };
    };

    module.exports = GraphLabelRenderer;
}());